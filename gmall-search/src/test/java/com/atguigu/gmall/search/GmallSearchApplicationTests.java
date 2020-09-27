package com.atguigu.gmall.search;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValueVo;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.api.GmallWmsApi;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {
    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private RestHighLevelClient highLevelClient;

    @Autowired
    private GmallPmsApi pmsApi;

    @Autowired
    private GmallWmsApi wmsApi;

    @Test
    void contextLoads() {
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);
    }

    @Test
    void addData() {
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);
        //分页插入
        Integer pageNum = 1;
        Integer pageSize = 100;

        do {
            //1、获得goods集合
            //1.1.获得sku相关的信息
            PageParamVo pageParamVo = new PageParamVo(pageNum, pageSize, null);
            ResponseVo<List<SpuEntity>> spuEntitiesResponseVo = pmsApi.querySpuByPageJson(pageParamVo);
            List<SpuEntity> spuEntities = spuEntitiesResponseVo.getData();
            if (CollectionUtils.isEmpty(spuEntities)) {
                continue;
            }
            //遍历所有的spu里面的所有sku，转换成goods，然后封装goods集合

            spuEntities.forEach(spuEntity -> {

                //根据当前的spuid查询出下面的所有的sku
                ResponseVo<List<SkuEntity>> skuEntitiesResponseVo = pmsApi.querySkuBySpuId(spuEntity.getId());
                List<SkuEntity> skuEntities = skuEntitiesResponseVo.getData();
                if (!CollectionUtils.isEmpty(skuEntities)) {
                    //使用流把sku集合转化成goods集合
                    List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                        Goods goods = new Goods();
                        //1、设置sku相关信息
                        goods.setSkuId(skuEntity.getId());
                        goods.setDefaultImage(skuEntity.getDefaultImage());
                        goods.setTitle(skuEntity.getTitle());
                        goods.setSubtitle(skuEntity.getSubtitle());
                        goods.setPrice(skuEntity.getPrice().doubleValue());
                        //2、根据当前的spu获得品牌信息
                        ResponseVo<BrandEntity> brandEntityResponseVo = pmsApi.queryBrandById(spuEntity.getBrandId());
                        BrandEntity brandEntity = brandEntityResponseVo.getData();
                        if (brandEntity != null) {
                            goods.setBrandId(brandEntity.getId());
                            goods.setBrandName(brandEntity.getName());
                            goods.setLogo(brandEntity.getLogo());
                        }
                        //3、根据spu获得当前的分类信息
                        ResponseVo<CategoryEntity> categoryEntityResponseVo = pmsApi.queryCategoryById(spuEntity.getCategoryId());
                        CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
                        if (categoryEntity != null) {
                            goods.setCategoryId(categoryEntity.getId());
                            goods.setCategoryName(categoryEntity.getName());
                        }
                        //4、设置spu相关信息
                        goods.setCreateTime(spuEntity.getCreateTime());
                        //5、设置库存相关信息
                        ResponseVo<List<WareSkuEntity>> wareSkuEntitiesVo = wmsApi.queryWareSkuBySkuId(skuEntity.getId());
                        List<WareSkuEntity> wareSkuEntities = wareSkuEntitiesVo.getData();
                        if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                            //使用流判断是否有货
                            goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                            //使用流计算合计的销量
                            goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a, b) -> a + b).get());
                        }
                        //6、设置搜索参数集合
                        ArrayList<SearchAttrValueVo> searchAttrValueVos = new ArrayList<>();
                        //6.1. 设置spu的搜索参数到搜索参数集合中
                        ResponseVo<List<SpuAttrValueEntity>> spuAttrValueEntitiesVo = pmsApi.querySearchSpuAttrValuesByCidAndSpuId(spuEntity.getCategoryId(), spuEntity.getId());
                        List<SpuAttrValueEntity> spuAttrValueEntities = spuAttrValueEntitiesVo.getData();
                        if (!CollectionUtils.isEmpty(spuAttrValueEntities)) {
                            List<SearchAttrValueVo> searchSpuAttrValues = spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                                SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                                BeanUtils.copyProperties(spuAttrValueEntity, searchAttrValueVo);
                                return searchAttrValueVo;
                            }).collect(Collectors.toList());
                            //加入到搜索参数集合中
                            searchAttrValueVos.addAll(searchSpuAttrValues);
                        }
                        //6.2。 设置sku的搜索参数到搜索参数集合中
                        ResponseVo<List<SkuAttrValueEntity>> skuAttrValueEntitiesVo = pmsApi.querySearchSkuAttrValuesByPaCidAndSkuId(spuEntity.getCategoryId(), skuEntity.getId());
                        List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueEntitiesVo.getData();
                        if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                            List<SearchAttrValueVo> searchSkuAttrValues = skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                                SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                                BeanUtils.copyProperties(skuAttrValueEntity, searchAttrValueVo);
                                return searchAttrValueVo;
                            }).collect(Collectors.toList());
                            searchAttrValueVos.addAll(searchSkuAttrValues);
                        }
                        goods.setSearchAttrs(searchAttrValueVos);//最终设置给goods

                        return goods;
                    }).collect(Collectors.toList());
                    //2、向es输入数据
                    goodsRepository.saveAll(goodsList);
                }

            });
            //3、设置循环条件
            pageSize = spuEntities.size();
            pageNum++;
        } while (pageSize == 100);
    }
}
