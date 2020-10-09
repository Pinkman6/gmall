package com.atguigu.gmall.search.listener;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValueVo;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.api.GmallWmsApi;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GoodsListener {
    @Autowired
    private GmallPmsApi pmsApi;

    @Autowired
    private GmallWmsApi wmsApi;

    @Autowired
    private GoodsRepository goodsRepository;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "SEARCH_ADD_QUEUE",durable = "true"),
            exchange = @Exchange(value = "PMS_ITEM_EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"item.insert"}
    ))
    public void listener(Long spuId, Channel channel, Message message) {


            //根据当前的spuid查询出下面的所有的sku
            ResponseVo<List<SkuEntity>> skuEntitiesResponseVo = pmsApi.querySkuBySpuId(spuId);
            List<SkuEntity> skuEntities = skuEntitiesResponseVo.getData();
            if (!CollectionUtils.isEmpty(skuEntities)) {
                //使用流把sku集合转化成goods集合
                List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                    Goods goods = new Goods();
                    //1、设置sku相关信息
                    goods.setSkuId(skuEntity.getId());
                    goods.setDefaultImage(skuEntity.getDefaultImage());
                    goods.setTitle(skuEntity.getTitle());
                    goods.setSubTitle(skuEntity.getSubtitle());
                    goods.setPrice(skuEntity.getPrice().doubleValue());
                    //2、根据当前的spu获得品牌信息
                    SpuEntity spuEntity = this.pmsApi.querySpuById(spuId).getData();
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



    }
}
