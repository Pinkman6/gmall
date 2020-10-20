package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.GmallPmsClient;
import com.atguigu.gmall.oms.feign.GmallUmsClient;
import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.atguigu.gmall.oms.service.OrderItemService;
import com.atguigu.gmall.oms.service.OrderService;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements OrderService {
    @Autowired
    private GmallUmsClient umsClient;
    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<OrderEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<OrderEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public OrderEntity saveOrder(OrderSubmitVo submitVo, Long userId) {
        //1、保存订单表
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setUserId(userId);
        ResponseVo<UserEntity> userEntityResponseVo = umsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
        if (userEntity != null) {
            orderEntity.setUsername(userEntity.getUsername());//设置订单里面的用户姓名
        }
        orderEntity.setOrderSn(submitVo.getOrderToken());//设置订单编号
        orderEntity.setCreateTime(new Date());//设置订单创建时间
        orderEntity.setTotalAmount(submitVo.getTotalPrice());//设置总价格
        //TODO: 各种优惠抵扣金额
        orderEntity.setPayType(submitVo.getPayType());//设置支付方式
        orderEntity.setSourceType(1);//下单的来源
        orderEntity.setStatus(0);//订单状态，默认待付款
        orderEntity.setDeliveryCompany(submitVo.getDeliveryCompany());//选择快递公司
        UserAddressEntity address = submitVo.getAddress();
        if (address != null) {
            orderEntity.setReceiverName(address.getName());
            orderEntity.setReceiverPhone(address.getPhone());
            orderEntity.setReceiverCity(address.getCity());
            orderEntity.setReceiverPostCode(address.getPostCode());
            orderEntity.setReceiverProvince(address.getProvince());
            orderEntity.setReceiverRegion(address.getRegion());
            orderEntity.setReceiverAddress(address.getAddress());
        }
        orderEntity.setConfirmStatus(0);//设置收货确认状态，默认未收货
        orderEntity.setDeleteStatus(0);//设置订单删除状态，默认未删除
        orderEntity.setUseIntegration(submitVo.getBounds());//设置使用积分
        this.save(orderEntity);
        Long id = orderEntity.getId();//主键回写获得订单id
        //2、保存订单详情表

        List<OrderItemVo> items = submitVo.getItems();//获得订单详情
        if (!CollectionUtils.isEmpty(items)) {
            List<OrderItemEntity> itemEntities = items.stream().map(item -> {
                OrderItemEntity itemEntity = new OrderItemEntity();

                itemEntity.setOrderId(id);
                itemEntity.setOrderSn(submitVo.getOrderToken());

                //根据skuId获取sku的信息
                ResponseVo<SkuEntity> skuEntityResponseVo = pmsClient.querySkuById(item.getSkuId());
                SkuEntity skuEntity = skuEntityResponseVo.getData();
                if (skuEntity != null) {
                    itemEntity.setSkuId(skuEntity.getId());
                    itemEntity.setSkuName(skuEntity.getName());
                    itemEntity.setSkuQuantity(item.getCount().intValue());
                    itemEntity.setSkuPrice(skuEntity.getPrice());
                    itemEntity.setSkuPic(skuEntity.getDefaultImage());
                    ResponseVo<List<SkuAttrValueEntity>> listResponseVo = this.pmsClient.querySkuAttrValueBySkuId(item.getSkuId());
                    List<SkuAttrValueEntity> skuAttrValueEntities = listResponseVo.getData();
                    itemEntity.setSkuAttrsVals(JSON.toJSONString(skuAttrValueEntities));
                    itemEntity.setCategoryId(skuEntity.getCatagoryId());

                    ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
                    BrandEntity brandEntity = brandEntityResponseVo.getData();
                    if (brandEntity != null) {
                        itemEntity.setSpuBrand(brandEntity.getName());
                    }

                    //根据spuId获取spu信息
                    ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
                    SpuEntity spuEntity = spuEntityResponseVo.getData();
                    if (spuEntity != null) {
                        itemEntity.setSpuId(spuEntity.getId());
                        itemEntity.setSpuName(spuEntity.getName());
                    }

                    ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
                    SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
                    if (spuDescEntity != null) {
                        itemEntity.setSpuPic(spuDescEntity.getDecript());
                    }

                    // TODO：根据skuId查询积分优惠
                }


                return itemEntity;
            }).collect(Collectors.toList());
            orderItemService.saveBatch(itemEntities);
        }

        //发送定时关单的消息到延时队列中
        rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.ttl", submitVo.getOrderToken());

        return orderEntity;
    }

}