package com.atguigu.gmall.scheduling.job;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.scheduling.mapper.CartMapper;
import com.atguigu.gmall.scheduling.pojo.Cart;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class CartJobHandler {
    private static final String KEY = "cart:async:exception";
    private static final String KEY_PREFIX = "cart:info:";

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;
    //同步出现异常的用户id的购物车信息
    @XxlJob(value = "AsyncExceptionJobHandler")
    public ReturnT<String> cartJobHandler(String arg) {
        //获得用户的id
        BoundListOperations<String, String> listOps = redisTemplate.boundListOps(KEY);
        String userId = listOps.rightPop();

        while (!StringUtils.isBlank(userId)) {
            //再通过用户的这个id获得用户的购物车信息
            BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
            List<Object> cartJsons = hashOps.values();
            //如果通过这个发生异常的异步同步数据的方法的用户id查询到的购物车里面没有信息的话，那么表示他已经全部删除了
            //  我们需要把MySQL的购物车数据清空
            //如果这个用户的购物车信息不为空的话，那么也是先删除MySQL
            this.cartMapper.delete(new QueryWrapper<Cart>().eq("user_id", userId));
            //如果不为空的话 就新增数据
            if (!CollectionUtils.isEmpty(cartJsons)) {
                cartJsons.forEach(cartJson->{
                    Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                    cartMapper.insert(cart);
                });
            }
            userId = listOps.rightPop();
        }
        return ReturnT.SUCCESS;
    }

}
