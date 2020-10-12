package com.atguigu.gmall.index;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;

import java.util.List;

@SpringBootTest
class GmallIndexApplicationTests {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RBloomFilter bloomFilter;

    @Autowired
    private GmallPmsClient pmsClient;

    @Test
    void contextLoads() {
        redisTemplate.opsForValue().set("aa:bb:man", "xiaoliu");
        System.out.println(redisTemplate.opsForValue().get("aa:bb:man"));

    }

    @Test
    void testBloomGuava() {

        BloomFilter<CharSequence> filter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), 10l, 0.3);
        filter.put("1");
        filter.put("2");
        filter.put("3");
        filter.put("4");
        filter.put("5");
        System.out.println(filter.mightContain("1"));
        System.out.println(filter.mightContain("2"));
        System.out.println(filter.mightContain("3"));
        System.out.println(filter.mightContain("6"));
        System.out.println(filter.mightContain("7"));
        System.out.println(filter.mightContain("19"));
        System.out.println(filter.mightContain("198"));
        System.out.println(filter.mightContain("16"));
        System.out.println(filter.mightContain("156"));
        System.out.println(filter.mightContain("15643"));
    }

    @Test
    void testBloomRedission() {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("bloomFilter");
        bloomFilter.tryInit(10l, 0.03);
        bloomFilter.add("1");
        bloomFilter.add("2");
        bloomFilter.add("3");
        bloomFilter.add("4");
        bloomFilter.add("5");
        System.out.println(bloomFilter.contains("1"));
        System.out.println(bloomFilter.contains("2"));
        System.out.println(bloomFilter.contains("3"));
        System.out.println(bloomFilter.contains("6"));
        System.out.println(bloomFilter.contains("7"));
        System.out.println(bloomFilter.contains("8"));
        System.out.println(bloomFilter.contains("9"));
        System.out.println(bloomFilter.contains("10"));
        System.out.println(bloomFilter.contains("11"));
        System.out.println(bloomFilter.contains("12"));
        System.out.println(bloomFilter.contains("13"));
        System.out.println(bloomFilter.contains("14"));

    }

    @Test
    void testloadDataRBloom() {
        //查询分类放入bloom过滤器中
        ResponseVo<List<CategoryEntity>> listResponseVo = pmsClient.queryCategoryByPid(0L);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        if (!CollectionUtils.isEmpty(categoryEntities)) {
            categoryEntities.forEach(categoryEntity -> {
                bloomFilter.add(categoryEntity.getId().toString());
            });
        }
    }

}
