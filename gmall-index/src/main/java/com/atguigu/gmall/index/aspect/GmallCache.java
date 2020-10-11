package com.atguigu.gmall.index.aspect;


import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GmallCache {
    /**
     * 定义缓存的前缀
     *
     * @return
     */
    String prefix() default "";

    /**
     * 定义分布式锁的前缀
     */
    String lock() default "lock:";

    /**
     * 缓存的过期时间,单位是分钟
     */
    int timeOut() default 5;

    /**
     * 避免缓存雪崩，过期时间需要添加随机值
     */
    int random() default 5;
}
