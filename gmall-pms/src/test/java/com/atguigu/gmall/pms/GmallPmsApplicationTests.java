package com.atguigu.gmall.pms;

import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GmallPmsApplicationTests {
    @Autowired
    private SkuAttrValueMapper attrValueMapper;

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @Test
    void contextLoads() {
    }
    @Test
    void testQuerySkuIdMappingSaleAttrValueBySpuId() {
        System.out.println(attrValueMapper.querySkuIdMappingSaleAttrValueBySpuId(7L));
    }

    @Test
    void testServiceQuerySkuIdMappingSaleAttrValueBySpuId() {
        System.out.println(skuAttrValueService.querySkuIdMappingSaleAttrValueBySpuId(7L));
    }
}
