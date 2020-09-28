package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParamVo {
    //搜索的关键字
    private String keyword;
    //品牌id集合
    private List<Long> brandId;
    //分类id集合
    private List<Long> cid3;
    //检索的规格参数集合
    private List<String> props;//5:128G-256G-512G
    //价格区间
    private Double priceFrom;
    private Double priceTo;
    //是否有库存
    private Boolean store;
    //排序，1:价格升序，2：价格降序，3：新品排序，4：销量降序
    private Integer sort;
    //分页数据
    private Integer pageSize=20;
    private Integer pageNum=1;
}
