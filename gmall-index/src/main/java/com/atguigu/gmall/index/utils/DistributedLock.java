package com.atguigu.gmall.index.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DistributedLock {
    @Autowired
    private StringRedisTemplate redisTemplate;

    private Thread thread;

    //可重入的加锁方法
    public  Boolean  lock(String lockName,String uuid,Long timeOut) throws InterruptedException {
        String lockLua = "if(redis.call('exists',KEYS[1])==0 or redis.call('hexists',KEYS[1],ARGV[1])== 1) " +
                "then redis.call('hincrby',KEYS[1],ARGV[1],1) " +
                "   redis.call('expire',KEYS[1],ARGV[2]) " +
                "   return 1 " +
                "else " +
                "   return 0 " +
                "end";
        Boolean execute = this.redisTemplate.execute(new DefaultRedisScript<>(lockLua, Boolean.class), Arrays.asList(lockName), uuid, timeOut.toString());
        if (!execute) {
            //如果没有获得锁的话，那么隔一段时间重试,
            Thread.sleep(20);
            this.lock(lockName, uuid, timeOut);
        }
        //加锁成功自动续期
        this.reNewExpire(lockName, uuid, timeOut);
        return true;
    }

    //可重入的解锁方法
    public void unlock(String lockName,String uuid) {
        String unLockLua = "if(redis.call('hexists',KEYS[1],ARGV[1])==0) " +
                "then " +
                "return nil " +
                "elseif(redis.call('hincrby',KEYS[1],ARGV[1],-1)>0) " +
                "then " +
                "return 0  " +
                "else  " +
                "redis.call('del',KEYS[1]) " +
                "return 1 end";
        Long execute = this.redisTemplate.execute(new DefaultRedisScript<>(unLockLua, Long.class), Arrays.asList(lockName), uuid);
        //解锁后释放看门狗线程
        this.thread.interrupt();
        if (execute == null) {
            throw new RuntimeException("锁不存在或者这个锁不是你的锁");
        }


    }

    //设置看门狗线程方法
    public void reNewExpire(String lockName,String uuid,Long expireTime) {
        String addTimeLua = "if(redis.call('hexists',KEYS[1],ARGV[1])==1) " +
                "then redis.call('expire',KEYS[1],ARGV[2]) " +
                "return 1 " +
                "else " +
                " return 0  end";
        thread = new Thread(()->{
            //循环加时间
            while (true) {
                try {
                    Thread.sleep(expireTime*1000*2/3);

                    this.redisTemplate.execute(new DefaultRedisScript<>(addTimeLua, Boolean.class), Arrays.asList(lockName), uuid, expireTime.toString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

}
