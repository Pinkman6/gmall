package com.atguigu.gmall.cart.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
@Slf4j
public class UncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {
    //异常的信息的key的固定名
    private static final String KEY = "cart:async:exception";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
        log.error("异步调用方法出现异常，方法：{},参数：{}，异常信息：{}",method,objects,throwable.getMessage());
        //发生异常后，信息保存到数据库中,记录的是userId
        BoundListOperations<String, String> listOps = redisTemplate.boundListOps(KEY);
        listOps.leftPush(objects[0].toString());

    }
}
