package com.atguigu.gmall.cart.service;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;


    @Autowired
    private CartAsyncService cartAsyncService;

    //设置在Redis中保存数据的key的前缀
    private static final String KEY_PREFIX = "cart:info:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 添加购物车的方法
     * @param cart
     */
    public void addCart(Cart cart) {
        //1.通过ThreadLocal查询出userinfo。然后获得购物车对应的外层key即userId，不管是否登录
        String userId = getUserId();
        //2.通过userId和skuId查询购物车的信息
        String key = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);
        String skuIdString = cart.getSkuId().toString();
        //获得原来传过来的cart的数量
        BigDecimal count = cart.getCount();
        //3.如果有的话就是修改数量
        if (hashOps.hasKey(skuIdString)) {

            //查询Redis数据库中的cart进行反序列化
            String cartJson = hashOps.get(skuIdString).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            //更新数量
            cart.setCount(cart.getCount().add(count));

            //异步新增MySQL
            cartAsyncService.updateCart(userId, cart);

        } else {
            //4.如果没有的话就是新增购物车记录
            //   首先要通过sku的id查询出这个商品的信息
            ResponseVo<SkuEntity> skuEntityResponseVo = pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            //设置商品信息
            cart.setUserId(userId);
            cart.setCheck(true);

            if (skuEntity != null) {
                cart.setPrice(skuEntity.getPrice());
                cart.setDefaultImage(skuEntity.getDefaultImage());
                cart.setTitle(skuEntity.getTitle());
            }
            //设置库存信息
            ResponseVo<List<WareSkuEntity>> listResponseVo = wmsClient.queryWareSkusBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntityList = listResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntityList)) {
                cart.setStore(wareSkuEntityList.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }

            //设置营销信息
            ResponseVo<List<ItemSaleVo>> listResponseVo1 = smsClient.queryItemSalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVoList = listResponseVo1.getData();
            cart.setSales(JSON.toJSONString(itemSaleVoList));

            //设置销售属性
            ResponseVo<List<SkuAttrValueEntity>> listResponseVo2 = pmsClient.querySkuAttrValueBySkuId(cart.getSkuId());
            cart.setSaleAttrs(JSON.toJSONString(listResponseVo2.getData()));

            //然后同步到MySQL数据库中
            cartAsyncService.insertCart(cart);
        }
        //5.//更新/新增到Redis中去
        hashOps.put(skuIdString, JSON.toJSONString(cart));
        //6.
    }

    private String getUserId() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        if (userId == null) {
            return userInfo.getUserKey();
        }
        return userId.toString();
    }

    /**
     * 添加购物车之后成功信息的回显
     * @param skuId
     * @return
     */
    public Cart queryCartBySkuId(Long skuId) {
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;
        //获得Redis内层中的map
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);
        //判断该用户的购物车中是否有改商品
        if (hashOps.hasKey(skuId.toString())) {
            //有的话就获得并反序列化返回
            String cartJson = hashOps.get(skuId.toString()).toString();
            return JSON.parseObject(cartJson, Cart.class);
        }
        //制造异常
        throw  new RuntimeException("该用户购物车没有该商品");
    }

    public List<Cart> queryCarts() {
// 1.获取userKey，查询未登录的购物车
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        String unloginKey = KEY_PREFIX + userKey;
        // 获取未登录用户的购物车内层map
        BoundHashOperations<String, Object, Object> unloginHashOps = this.redisTemplate.boundHashOps(unloginKey);
        List<Object> cartJsons = unloginHashOps.values();
        List<Cart> unloginCarts = null;
        if (!CollectionUtils.isEmpty(cartJsons)){
            unloginCarts = cartJsons.stream().map(cartJson -> JSON.parseObject(cartJson.toString(), Cart.class)).collect(Collectors.toList());
        }

        // 2.获取userId，判断是否登录。未登录则直接返回未登录的购物车
        Long userId = userInfo.getUserId();
        if (userId == null){
            return unloginCarts;
        }

        // 3.判断有没有未登录的购物车，有则合并
        String loginKey = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(loginKey);
        if (!CollectionUtils.isEmpty(unloginCarts)){
            unloginCarts.forEach(cart -> {
                if (loginHashOps.hasKey(cart.getSkuId().toString())){
                    // 更新数量
                    BigDecimal count = cart.getCount();

                    String cartJson = loginHashOps.get(cart.getSkuId().toString()).toString();
                    cart = JSON.parseObject(cartJson, Cart.class);
                    cart.setCount(cart.getCount().add(count));

                    // 写回redis和mysql
                    this.cartAsyncService.updateCart(userId.toString(), cart);
                } else {
                    // 新增购物车记录
                    cart.setUserId(userId.toString());
                    cartAsyncService.insertCart(cart);
                }
                loginHashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
            });
        }

        // 4.删除未登录的购物车
        this.redisTemplate.delete(unloginKey);
        this.cartAsyncService.deleteCartByUserId(userKey);

        // 5.以userId获取登录状态的购物车
        List<Object> loginCartJsons = loginHashOps.values();
        if (!CollectionUtils.isEmpty(loginCartJsons)){
            return loginCartJsons.stream().map(cartJson -> JSON.parseObject(cartJson.toString(), Cart.class)).collect(Collectors.toList());
        }
        return null;
    }

    public void updateNum(Cart cart) {
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);
        if (hashOps.hasKey(cart.getSkuId().toString())) {
            BigDecimal count = cart.getCount();
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(count);
            hashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
            cartAsyncService.updateCart(userId, cart);

        }
    }

    @Async
    public String executor1() throws InterruptedException {
        System.out.println("这是e1方法执行");

            TimeUnit.SECONDS.sleep(5);
            int i = 1 / 0;


        System.out.println("这是e1方法结束");
        return "1111";
    }

    @Async
    public String executor2() throws InterruptedException {

        System.out.println("这是e2方法执行");

        TimeUnit.SECONDS.sleep(4);

        System.out.println("这是e2方法结束");
        return "2222";
    }



}
