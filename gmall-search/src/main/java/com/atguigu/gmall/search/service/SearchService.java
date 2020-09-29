package com.atguigu.gmall.search.service;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseAttrValueVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SearchService {
    @Autowired
    private RestHighLevelClient highLevelClient;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public SearchResponseVo search(SearchParamVo searchParamVo) {
        try {
            //创建查询请求
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"}, this.buildDsl(searchParamVo));
            //client发送请求获得响应
            SearchResponse searchResponse = highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            System.out.println("searchResponse = " + searchResponse);
            //解析响应返回数据
            SearchResponseVo searchResponseVo = parseResult(searchResponse);
            //分页数据在响应里面是没有的，需要在外面获取
            searchResponseVo.setPageSize(searchParamVo.getPageSize());
            searchResponseVo.setPageNum(searchParamVo.getPageNum());
            return searchResponseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //解析结果集的方法
    private SearchResponseVo parseResult(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        //1、设置总命中数
        SearchHits hits = searchResponse.getHits();//获得命中结果
        searchResponseVo.setTotal(hits.getTotalHits());
        //2、设置goodsList
        //获得命中的结果集,每一个hit都是一个goods，可以使用stream流对他进行转换成goods集合
        SearchHit[] hitsHits = hits.getHits();
        List<Goods> goodsList = Stream.of(hitsHits).map(hit -> {
            try {
                //获得每个hit下面的_source
                String sourceAsString = hit.getSourceAsString();
                Goods goods = MAPPER.readValue(sourceAsString, Goods.class);
                //替换高亮字段
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                HighlightField field = highlightFields.get("title");
                Text[] fragments = field.getFragments();
                goods.setTitle(fragments[0].toString());
                return goods;
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
        searchResponseVo.setGoodsList(goodsList);
        //获得所有的聚合结果
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        //3、设置品牌的聚合信息
        ParsedLongTerms brandIdAgg = (ParsedLongTerms) aggregationMap.get("brandIdAgg");
        List<? extends Terms.Bucket> buckets = brandIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(buckets)) {
            //如果品牌桶不为空的话那么就把品牌桶集合转换为品牌集合
            List<BrandEntity> brandEntityList = buckets.stream().map(bucket -> {
                BrandEntity brandEntity = new BrandEntity();
                brandEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                //桶里面的名字桶里面获得名字
                Aggregations brandNameAggs = ((Terms.Bucket) bucket).getAggregations();
                ParsedStringTerms brandNameAgg = (ParsedStringTerms) brandNameAggs.get("brandNameAgg");
                List<? extends Terms.Bucket> nameAggBuckets = brandNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(nameAggBuckets)) {
                    brandEntity.setName(nameAggBuckets.get(0).getKeyAsString());
                }
                //id桶里面的logo聚合里面的logo桶
                Aggregations logoAggs = ((Terms.Bucket) bucket).getAggregations();
                ParsedStringTerms logoAgg = (ParsedStringTerms) logoAggs.get("logoAgg");
                List<? extends Terms.Bucket> logoAggBuckets = logoAgg.getBuckets();
                if (!CollectionUtils.isEmpty(logoAggBuckets)) {
                    brandEntity.setLogo(logoAggBuckets.get(0).getKeyAsString());
                }
                return brandEntity;
            }).collect(Collectors.toList());
            searchResponseVo.setBrands(brandEntityList);
        }

        //4、设置分类的聚合信息
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms) aggregationMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryIdAggBuckets = categoryIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(categoryIdAggBuckets)) {
            //cateid聚合里面有桶的话，每个桶就是一个分类,可以使用stream流进行转化成分类集合
            List<CategoryEntity> categoryEntityList = categoryIdAggBuckets.stream().map(bucket -> {
                CategoryEntity categoryEntity = new CategoryEntity();
                categoryEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                //每个桶下面的分类名称的聚合下面的每个桶都是一个分类名称，一般只有一个
                Aggregations nameAggs = ((Terms.Bucket) bucket).getAggregations();
                ParsedStringTerms categoryNameAgg = (ParsedStringTerms) nameAggs.get("categoryNameAgg");
                List<? extends Terms.Bucket> nameAggBuckets = categoryNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(nameAggBuckets)) {
                    categoryEntity.setName(nameAggBuckets.get(0).getKeyAsString());
                }
                return categoryEntity;
            }).collect(Collectors.toList());
            searchResponseVo.setCategories(categoryEntityList);
        }
        //5、设置检索参数的聚合信息
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = (ParsedLongTerms) attrAgg.getAggregations().get("attrIdAgg");
        //获得attrid聚合桶，每一个桶就是一个searchAttrValueVo,使用流转成集合
        List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(attrIdAggBuckets)) {
            List<SearchResponseAttrValueVo> valueVos = attrIdAggBuckets.stream().map(bucket -> {
                SearchResponseAttrValueVo searchResponseAttrValueVo = new SearchResponseAttrValueVo();
                searchResponseAttrValueVo.setAttrId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                //att的参数名实在下一级name聚合桶里面
                ParsedStringTerms attrNameAgg = (ParsedStringTerms) ((Terms.Bucket) bucket).getAggregations().get("attrNameAgg");
                List<? extends Terms.Bucket> nameAggBuckets = attrNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(nameAggBuckets)) {
                    searchResponseAttrValueVo.setAttrName(nameAggBuckets.get(0).getKeyAsString());
                }
                //attr的value集合是在id聚合桶的下一级value聚合桶中
                ParsedStringTerms attrValueAgg = (ParsedStringTerms) ((Terms.Bucket) bucket).getAggregations().get("attrValueAgg");
                List<? extends Terms.Bucket> valueAggBuckets = attrValueAgg.getBuckets();
                if (!CollectionUtils.isEmpty(valueAggBuckets)) {
                    //每个value桶里面都是一个attrvalue值，所以可以使用流把这个value桶集合转化为String集合
                    searchResponseAttrValueVo.setAttrValues(valueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList()));
                }
                return searchResponseAttrValueVo;
            }).collect(Collectors.toList());
            searchResponseVo.setFilters(valueVos);
        }


        //6、返回解析的结果
        return searchResponseVo;
    }

    //构建查询的方法
    private SearchSourceBuilder buildDsl(SearchParamVo searchParamVo) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        String keyword = searchParamVo.getKeyword();
        //如果没有关键字的话那么就不构建搜索条件了;
        if (StringUtils.isBlank(keyword)) {
            return sourceBuilder;
        }
        sourceBuilder.query(boolQuery);
        //0、设置需要查询的字段
        sourceBuilder.fetchSource(new String[]{"skuId", "defaultImage", "title", "subTitle", "price"}, null);
        //1、构建bool查询
        //1.1.must查询条件
        boolQuery.must(QueryBuilders.matchQuery("title", keyword));
        //1.2.filter过滤条件
        //1.2.1.过滤之品牌过滤
        List<Long> brandId = searchParamVo.getBrandId();
        if (!CollectionUtils.isEmpty(brandId)) {
            boolQuery.filter(QueryBuilders.termsQuery("brandId", brandId));
        }
        //1.2.2.过滤之分类过滤
        List<Long> cid3 = searchParamVo.getCid3();
        if (!CollectionUtils.isEmpty(cid3)) {
            boolQuery.filter(QueryBuilders.termsQuery("categoryId", cid3));
        }
        //1.2.3.过滤之价格范围过滤
        Double priceFrom = searchParamVo.getPriceFrom();
        Double priceTo = searchParamVo.getPriceTo();
        if (priceFrom != null || priceTo != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            if (priceFrom != null) {
                rangeQuery.from(priceFrom);
            }
            if (priceTo != null) {
                rangeQuery.to(priceTo);
            }
            boolQuery.filter(rangeQuery);
        }
        //1.2.4.过滤之有货过滤
        Boolean store = searchParamVo.getStore();
        if (store != null) {
            boolQuery.filter(QueryBuilders.termQuery("store", store));
        }
        //1.2.5.过滤之规格参数的嵌套过滤["4:8G-16G","5:128G-256G"]
        List<String> props = searchParamVo.getProps();
        if (!CollectionUtils.isEmpty(props)) {
            props.forEach(attr -> {
                //获得每一个参数然后添加嵌套查询
                String[] split = StringUtils.split(attr, ":");
                if (attr.indexOf(":") > 0 && split.length == 2) {
                    //如果输入合法的话，就获取数据
                    BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
                    String attrId = split[0];
                    boolQueryBuilder.must(QueryBuilders.termQuery("searchAttrs.attrId", attrId));
                    String attValues = split[1];
                    String[] values = StringUtils.split(attValues, "-");
                    boolQueryBuilder.must(QueryBuilders.termsQuery("searchAttrs.attrValue", values));
                    boolQuery.filter(QueryBuilders.nestedQuery("searchAttrs", boolQueryBuilder, ScoreMode.None));
                }
            });
        }
        //构建排序条件        1:价格升序，2：价格降序，3：新品排序，4：销量降序
        Integer sort = searchParamVo.getSort();
        if (sort != null) {

            switch (sort) {
                case 1:
                    sourceBuilder.sort("price", SortOrder.ASC);
                    break;
                case 2:
                    sourceBuilder.sort("price", SortOrder.DESC);
                    break;
                case 3:
                    sourceBuilder.sort("createTime", SortOrder.DESC);
                    break;
                case 4:
                    sourceBuilder.sort("sales", SortOrder.DESC);
                    break;
                default:
                    sourceBuilder.sort("_score", SortOrder.DESC);
                    break;
            }
        }

        //构建分页条件
        Integer pageNum = searchParamVo.getPageNum();
        Integer pageSize = searchParamVo.getPageSize();
        sourceBuilder.from((pageNum - 1) * pageSize);
        sourceBuilder.size(pageSize);
        //构建高亮
        sourceBuilder.highlighter(new HighlightBuilder().field("title").preTags("<font style='color:red'>").postTags("</font>"));
        //构建聚合
        //1、品牌聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo")));
//        //2、分类聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));
//        //3、嵌套的搜索参数的聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))
                )
        );


        System.out.println("searchSourceBuilder = " + sourceBuilder);
        return sourceBuilder;
    }

}
