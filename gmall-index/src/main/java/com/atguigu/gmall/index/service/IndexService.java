package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {
    @Autowired
    private StringRedisTemplate redisTemplate;


    @Autowired
    private GmallPmsClient pmsClient;

    private static final String KEY_PREFIX = "index:cate:";

    public List<CategoryEntity> queryLv1Categories() {
        ResponseVo<List<CategoryEntity>> listResponseVo = pmsClient.queryCategoryByPid(0L);
        return listResponseVo.getData();
    }

    public List<CategoryEntity> queryCategoryLv2WithSubsByPid(Long pid) {
        //缓存命中返回
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(json)) {
            return JSON.parseArray(json, CategoryEntity.class);
        }
        //缓存未命中的话查询数据库返回并添加缓存

        ResponseVo<List<CategoryEntity>> listResponseVo = pmsClient.queryCategoryLv2WithSubsByPid(pid);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        if (CollectionUtils.isEmpty(categoryEntities)) {
            //解决穿透，设置null
            redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 5, TimeUnit.MINUTES);
        }else {
            //解决缓存雪崩，给过期时间添加随机数
            redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 90+new Random().nextInt(5), TimeUnit.DAYS);
        }
        return categoryEntities;
    }

    public  void testLock() throws InterruptedException {
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid,3,TimeUnit.SECONDS);
        if (!lock){
            //获取锁失败的话就重试
            Thread.sleep(20);
            this.testLock();
        }else {
            //获取锁成功的话执行业务逻辑，完成后释放锁
            String countString = redisTemplate.opsForValue().get("count");
            if (StringUtils.isBlank(countString)) {
                redisTemplate.opsForValue().set("count", "1");
            }
            int count = Integer.parseInt(countString);
            redisTemplate.opsForValue().set("count", String.valueOf(++count));
            //判断一下这个锁是不是由自己来释放
            if (StringUtils.equals(uuid, redisTemplate.opsForValue().get("lock"))) {
                //判断完成之后，刚好过期，导致锁释放，误删其他请求获得的锁
                redisTemplate.delete("lock");
            }
        }

    }
}
