package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuEntity;

import java.util.List;

public class SpuVo extends SpuEntity {
    //图片信息
    private List<String> spuImages;
    //基本信息
    private List<?> baseAttrs;
    //sku信息
    private List<?> skus;
}
