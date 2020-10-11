package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.aspect.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.utils.DistributedLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "index:cate:";

    //远程查询一级分类
    public List<CategoryEntity> queryLv1Categories() {
        ResponseVo<List<CategoryEntity>> listResponseVo = pmsClient.queryCategoryByPid(0L);
        return listResponseVo.getData();
    }


    //远程查询二级分类,使用注解添加缓存和加分布式锁
    @GmallCache(prefix = KEY_PREFIX, timeOut = 129600, random = 7200)
    public List<CategoryEntity> queryCategoryLv2WithSubsByPid(Long pid) {

        System.out.println("这个是目标方法");
        ResponseVo<List<CategoryEntity>> listResponseVo = pmsClient.queryCategoryLv2WithSubsByPid(pid);
        return listResponseVo.getData();
    }

    //远程查询二级分类,自己编写代码实现的缓存和加分布式锁
    public List<CategoryEntity> queryCategoryLv2WithSubsByPidBySelf(Long pid) {
        //缓存命中返回

        String json = redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(json)) {
            return JSON.parseArray(json, CategoryEntity.class);
        }

        RLock lock = this.redissonClient.getLock("lock:" + pid);
        //缓存未命中的话查询数据库返回并添加缓存
        lock.lock();
        List<CategoryEntity> categoryEntities;
        try {
            //可能一个请求查询后存入缓存，而其他一起的请求也还是要访问数据库，所以还是要再判断一下
            String json2 = redisTemplate.opsForValue().get(KEY_PREFIX + pid);
            if (StringUtils.isNotBlank(json)) {
                return JSON.parseArray(json, CategoryEntity.class);
            }

            ResponseVo<List<CategoryEntity>> listResponseVo = pmsClient.queryCategoryLv2WithSubsByPid(pid);
            categoryEntities = listResponseVo.getData();
            if (CollectionUtils.isEmpty(categoryEntities)) {
                //解决穿透，设置null
                redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 5, TimeUnit.MINUTES);
            } else {
                //解决缓存雪崩，给过期时间添加随机数
                redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 90 + new Random().nextInt(5), TimeUnit.DAYS);
            }
            return categoryEntities;
        } finally {

            lock.unlock();
        }
    }


    //使用redisson客户端加解锁
    public void testLock() throws InterruptedException {

        String uuid = UUID.randomUUID().toString();
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock();
        //获取锁成功的话执行业务逻辑，完成后释放锁
        String countString = redisTemplate.opsForValue().get("count");
        if (StringUtils.isBlank(countString)) {
            redisTemplate.opsForValue().set("count", "1");
        }
        int count = Integer.parseInt(countString);
        redisTemplate.opsForValue().set("count", String.valueOf(++count));
        lock.unlock();

    }

    //实现了可重入锁的方案
    public void testLock2() throws InterruptedException {

        String uuid = UUID.randomUUID().toString();
        //可重入锁加锁
        Boolean lock = distributedLock.lock("lock", uuid, 9L);
        try {
            if (lock) {
                //测试锁的自动续期,这里故意让当前线程执行时间过长
                TimeUnit.SECONDS.sleep(40);

                this.testSubLock("lock", uuid, 9L);
                //获取锁成功的话执行业务逻辑，完成后释放锁
                String countString = redisTemplate.opsForValue().get("count");
                if (StringUtils.isBlank(countString)) {
                    redisTemplate.opsForValue().set("count", "1");
                }
                int count = Integer.parseInt(countString);
                redisTemplate.opsForValue().set("count", String.valueOf(++count));
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } finally {
            //可重入锁解锁
            this.distributedLock.unlock("lock", uuid);
        }

    }

    //测试可重入的方法
    public void testSubLock(String lockName, String uuid, Long timeOut) throws InterruptedException {
        distributedLock.lock(lockName, uuid, timeOut);
        System.out.println("我是内层的获得锁的方法");
        distributedLock.unlock(lockName, uuid);
    }

    //没有实现可重入的锁的方法
    public void testLock1() throws InterruptedException {
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS);
        if (!lock) {
            //获取锁失败的话就重试
            Thread.sleep(20);
            this.testLock();
        } else {
            //获取锁成功的话执行业务逻辑，完成后释放锁
            String countString = redisTemplate.opsForValue().get("count");
            if (StringUtils.isBlank(countString)) {
                redisTemplate.opsForValue().set("count", "1");
            }
            int count = Integer.parseInt(countString);
            redisTemplate.opsForValue().set("count", String.valueOf(++count));
            //判断一下这个锁是不是由自己来释放，判断加删除可以使用lua脚本进行封装
            /*if (StringUtils.equals(uuid, redisTemplate.opsForValue().get("lock"))) {
                //判断完成之后，刚好过期，导致锁释放，误删其他请求获得的锁
                redisTemplate.delete("lock");
            }*/
            String script = "if(ARGV[1]==redis.call('get',KEYS[1])) then return redis.call('del',KEYS[1]) else return 0  end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList("lock"), uuid);

        }

    }

    //加读锁的方法
    public void testRead() {
        RLock rLock = redissonClient.getReadWriteLock("rwLock").readLock();
        rLock.lock(10, TimeUnit.SECONDS);
        System.out.println("这个是读的方法");
//        rLock.unlock();
    }

    //加写锁的方法
    public void testWrite() {
        RLock wLock = redissonClient.getReadWriteLock("rwLock").writeLock();
        wLock.lock(10, TimeUnit.SECONDS);
        System.out.println("这个是写的方法");
//        wLock.unlock();
    }


    public String testLatch() {
        RCountDownLatch latch = redissonClient.getCountDownLatch("latch");
        latch.trySetCount(6);
        System.out.println("班长要锁门了");
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "班长锁门";
    }

    public String countDown() {
        RCountDownLatch latch = redissonClient.getCountDownLatch("latch");
        latch.countDown();
        return "出来了一味同学";
    }
}
