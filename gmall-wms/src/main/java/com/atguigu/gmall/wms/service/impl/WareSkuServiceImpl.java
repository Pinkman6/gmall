package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.service.WareSkuService;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuMapper, WareSkuEntity> implements WareSkuService {


    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private WareSkuMapper wareSkuMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String KEY_PREFIX = "stock:lock:";

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public List<SkuLockVo> checkAndLock(List<SkuLockVo> lockVos,String orderToken) {
        //首先判空
        if (CollectionUtils.isEmpty(lockVos)) {
            throw new OrderException("没有选中的商品记录！");
        }
        //不为空的话就遍历验证库存,并锁定库存
        lockVos.forEach(lockVo -> {
            this.checkLock(lockVo);
        });
        //只要有一个库存锁定失败的话，就必须解锁全部商品的库存
        if (lockVos.stream().anyMatch(lockVo -> !lockVo.getLock())) {
            //获取锁定成功的商品列表，然后解锁库存
            List<SkuLockVo> successLockVos = lockVos.stream().filter(SkuLockVo::getLock).collect(Collectors.toList());
            successLockVos.forEach(lockVo -> {
                this.wareSkuMapper.unlock(lockVo.getWareSkuId(), lockVo.getCount());
            });
            return lockVos;//直接返回带锁定状态的集合，就可以知道哪些没锁定成功
        }
        //缓存锁定信息。为了使得可以超时关单,方便将来解锁库存，那么就需要知道哪些被锁定了库存，所以需要在redis中缓存一份数据
        //key 是 orderToken，可以保证唯一性
        redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, JSON.toJSONString(lockVos));

        //发送信息给mq ，定时释放库存，防止锁定库存后宕机引起的库存不释放
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.ttl", orderToken);

        //返回值为null的话，那么就表示库存都锁定成功
        return null;
    }

    //验库存锁库存的原子性，抽取加分布式锁方法，由于是跨服务调用，所以加分布式锁
    private void checkLock(SkuLockVo lockVo) {
        RLock fairLock = redissonClient.getFairLock("stock:" + lockVo.getSkuId());
        fairLock.lock();

        //查库存
        Long skuId = lockVo.getSkuId();
        Integer count = lockVo.getCount();
        List<WareSkuEntity> wareSkuEntities = this.wareSkuMapper.check(skuId, count);
        if (CollectionUtils.isEmpty(wareSkuEntities)) {
            //如果没有一个仓库有库存的话，就设置锁定状态为false，表示锁定失败
            lockVo.setLock(false);
            //释放分布式锁
            fairLock.unlock();
            return;
        }
        //锁库存,取举例比较近的仓库来锁库存
        Long id = wareSkuEntities.get(0).getId();
        if (this.wareSkuMapper.lock(id, count) == 1) {
            //如果影响行数为1的话，那么说明锁库存成功
            lockVo.setLock(true);
            lockVo.setWareSkuId(id);

        }

        fairLock.unlock();

    }
}