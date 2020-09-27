package com.atguigu.gmall.search.service;

import com.atguigu.gmall.search.pojo.SearchParamVo;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SearchService {
    @Autowired
    private RestHighLevelClient highLevelClient;
    public void search(SearchParamVo searchParamVo) {
        try {
            //创建查询请求
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"},this.buildDsl(searchParamVo));
            //client发送请求获得响应
            SearchResponse searchResponse = highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            //解析响应返回数据
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private SearchSourceBuilder buildDsl(SearchParamVo searchParamVo){
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //构建搜索条件
        //1、构建bool查询
        QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("title", "手机"));
//        searchSourceBuilder.query()
        //1.1.must查询条件
        //1.2.filter过滤条件
        //1.2.1.过滤之品牌过滤
        //1.2.2.过滤之分类过滤
        //1.2.3.过滤之价格范围过滤
        //1.2.4.过滤之有货过滤
        //1.2.5.过滤之规格参数的嵌套过滤
        //构建排序条件

        //构建分页条件
        //构建高亮
        //构建聚合

        System.out.println("searchSourceBuilder = " + searchSourceBuilder);
        return searchSourceBuilder;
    }
}
