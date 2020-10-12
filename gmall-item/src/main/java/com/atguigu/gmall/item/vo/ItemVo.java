package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ItemVo {
    //三级分类
    private List<CategoryEntity> categories;

    //品牌
    private Long brandId;
    private String brandName;

    //spu信息
    private Long spuId;
    private String spuName;

    //sku信息
    private Long skuId;
    private String title;
    private String subTitle;
    private BigDecimal price;
    private String defaultImage;
    private Integer weight;

    //小图的图片列表
    private List<SkuImagesEntity> images;

    //营销信息
    private List<ItemSaleVo> sales;

    //库存信息,显示是否有货，默认无货
    private Boolean store = false;

    //sku所属的spu下面的所有sku的销售属性 [{attId:8,attName:'内存',attrValues:{'6g','8g'}},{attId:9,attName:'存储',attrValues:{'128g','256g'}}]
    private List<SaleAttrValueVo> saleAttrs;

    //为了使有的销售属性处于选中状态，需要获取自己的sku的销售属性 {8:白色，9： 8g}
    private Map<Long, String> saleAttr;

    //建立属性组合和skuId的映射关系，当选中一组规格参数后可以跳转到对应的sku的详情页面
    // {{8g,128g,白色}:10, {6g,256g,黑色}：11}
    private String skuJsons;

    //商品描述信息
    private List<String> spuImages;

    //组及组下面的规格参数和值
    private List<ItemGroupVo> groups;

}
