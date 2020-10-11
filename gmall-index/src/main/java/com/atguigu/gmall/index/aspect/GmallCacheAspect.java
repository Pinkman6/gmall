package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(GmallCache)")
    public Object around (ProceedingJoinPoint joinPoint) throws Throwable {
        //需要获得目标方法的参数以及注解里面的前缀参数,以及目标方法的返回类型
        List<Object> args = Arrays.asList(joinPoint.getArgs());
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        GmallCache gmallCache = signature.getMethod().getAnnotation(GmallCache.class);
        String prefix = gmallCache.prefix();
        Class returnType = signature.getReturnType();
        //查询缓存，如果缓存命中直接返回,
        String key = prefix + Arrays.asList(args);
        String json = redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(json)) {
            return JSON.parseObject(json, returnType);
        }

        //缓存中没有的话要加分布式锁
        //--加锁的获得注解里面的锁名的前缀
        String lock = gmallCache.lock();
        RLock rLock = redissonClient.getFairLock(lock + args);
        rLock.lock();

        //双端检测，还需要判断一下缓存中有没有数据
        String json2 = redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(json2)) {
            rLock.unlock();
            return JSON.parseObject(json2, returnType);
        }

        //如果缓存中有的话直接返回，没有的话执行目标方法
        List<CategoryEntity> result = (List<CategoryEntity>)joinPoint.proceed(joinPoint.getArgs());//执行目标方法
        //放入缓存，释放锁
        //---放缓存的获得注解里面的超时时间和随机时间
        int timeOut =  gmallCache.timeOut()+new Random().nextInt(gmallCache.random());
        redisTemplate.opsForValue().set(key,JSON.toJSONString(result),timeOut, TimeUnit.SECONDS);

        rLock.unlock();
        return result;
    }

}
