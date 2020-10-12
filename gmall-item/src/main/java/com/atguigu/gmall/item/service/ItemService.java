package com.atguigu.gmall.item.service;

import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemService {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallWmsClient wmsClient;

    public ItemVo loadData(Long skuId) {
        ItemVo itemVo = new ItemVo();

        SkuEntity skuEntity = pmsClient.querySkuById(skuId).getData();
        if (skuEntity == null) {
            return null;
        }
        //sku信息
        itemVo.setSkuId(skuId);
        itemVo.setSubTitle(skuEntity.getTitle());
        itemVo.setSubTitle(skuEntity.getSubtitle());
        itemVo.setPrice(skuEntity.getPrice());
        itemVo.setDefaultImage(skuEntity.getDefaultImage());
        itemVo.setWeight(skuEntity.getWeight());

        //三级分类
        List<CategoryEntity> categoryEntities = pmsClient.queryCategoriesByCid3(skuEntity.getCatagoryId()).getData();
        itemVo.setCategories(categoryEntities);

        //品牌
        BrandEntity brandEntity = pmsClient.queryBrandById(skuEntity.getBrandId()).getData();
        if (brandEntity != null) {
            itemVo.setBrandId(brandEntity.getId());
            itemVo.setBrandName(brandEntity.getName());
        }

        //spu信息
        SpuEntity spuEntity = pmsClient.querySpuById(skuEntity.getSpuId()).getData();
        if (spuEntity != null) {
            itemVo.setSpuId(spuEntity.getId());
            itemVo.setSpuName(spuEntity.getName());
        }

        //小图的图片列表
        List<SkuImagesEntity> skuImagesEntities = pmsClient.queryImagesBySkuId(skuId).getData();
        itemVo.setImages(skuImagesEntities);

        //营销信息
        List<ItemSaleVo> itemSaleVos = smsClient.queryItemSalesBySkuId(skuId).getData();
        itemVo.setSales(itemSaleVos);

        //库存信息,显示是否有货，默认无货
        List<WareSkuEntity> wareSkuEntities = wmsClient.queryWareSkusBySkuId(skuId).getData();
        if (!CollectionUtils.isEmpty(wareSkuEntities)) {
            boolean anyMatch = wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0);
            itemVo.setStore(anyMatch);
        }

        //sku所属的spu下面的所有sku的销售属性
        List<SaleAttrValueVo> saleAttrValueVos = pmsClient.querySaleAttrValuesBySpuId(skuEntity.getSpuId()).getData();
        itemVo.setSaleAttrs(saleAttrValueVos);

        //获取自己的sku的销售属性
        List<SkuAttrValueEntity> skuAttrValueEntities = pmsClient.querySkuAttrValueBySkuId(skuId).getData();
        if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {

            itemVo.setSaleAttr(skuAttrValueEntities.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId, SkuAttrValueEntity::getAttrValue)));

        }

        //属性组合和skuId的映射关系
        String json = pmsClient.querySkuIdMappingSaleAttrValueBySpuId(skuEntity.getSpuId()).getData();
        itemVo.setSkuJsons(json);

        //商品描述信息
        SpuDescEntity descEntity = pmsClient.querySpuDescById(skuEntity.getSpuId()).getData();
        if (descEntity != null) {

            //这里的商品描述信息是多个连接组成的字符串，以，分隔
            String[] split = StringUtils.split(descEntity.getDecript(), ",");
            itemVo.setSpuImages(Arrays.asList(split));
        }

        //组及组下面的规格参数和值
        List<ItemGroupVo> itemGroupVos = pmsClient.queryGroupsWithAttrAndValueByCidAndSpuIdAndSkuId(skuEntity.getCatagoryId(), skuId, skuEntity.getSpuId()).getData();
        itemVo.setGroups(itemGroupVos);


        return itemVo;
    }
}
