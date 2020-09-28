package com.atguigu.gmall.search.pojo;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import lombok.Data;

import java.util.List;

@Data
public class SearchResponseVo {
    //品牌的聚合信息
    private List<BrandEntity> brands;
    //分类的聚合信息
    private List<CategoryEntity> categories;
    //检索参数的聚合信息 [{attrId:4,attrName:"运行内存",attrValues:[8G,16G]},{attrId:5，attrName:"机身存储"，attrValues:[128G,256G,512G]}]
    private List<SearchResponseAttrValueVo> filters;
    //商品集的详情信息
    private List<Goods> goodsList;
    //分页数据
    private Integer pageNum;
    private Integer pageSize;
    private Long total;
}
