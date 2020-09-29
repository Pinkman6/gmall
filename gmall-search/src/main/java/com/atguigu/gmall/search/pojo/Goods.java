package com.atguigu.gmall.search.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;

/**
 * 一个goods对应一个sku
 */
@Data
@Document(indexName = "goods",type = "info",shards = 3,replicas = 2)
public class Goods {

    //sku相关信息
    @Id
    @Field(type = FieldType.Long)
    private Long skuId;
    @Field(type = FieldType.Keyword,index = false)
    private String defaultImage;
    @Field(type = FieldType.Double)
    private Double price;
    @Field(type = FieldType.Text,analyzer = "ik_max_word")
    private String title;
    @Field(type = FieldType.Keyword,index = false)
    private String subTitle;

    //品牌相关信息
    @Field(type = FieldType.Long)
    private Long brandId;
    @Field(type = FieldType.Keyword)
    private String brandName;
    @Field(type = FieldType.Keyword)
    private String logo;

    //分类相关信息
    @Field(type = FieldType.Long)
    private Long categoryId;
    @Field(type = FieldType.Keyword)
    private String categoryName;

    //搜索规格参数
    @Field(type = FieldType.Nested)
    private List<SearchAttrValueVo> searchAttrs;


    //排序相关信息
    @Field(type = FieldType.Long)
    private Date createTime; //新品
    @Field(type = FieldType.Date)
    private Long sales; //销量
    @Field(type = FieldType.Boolean)
    private Boolean store= false; //有货


}
