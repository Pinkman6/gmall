package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuEntity;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Autowired
    private SpuDescMapper descMapper;

    @Autowired
    private SpuDescService descService;

    @Autowired
    private SpuAttrValueService baseAttrService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuAttrValueService skuAttrService;

    @Autowired
    private GmallSmsClient smsClient;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpuByCidPage(Long categoryId, PageParamVo pageParamVo) {
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();
        //判断cid是否是0，如果是0的话就是查全站，不是0的话就是查本类
        if (categoryId != 0) {
            wrapper.eq("category_id", categoryId);
        }
        //查询关键字是否为空，如果不为空的话就加上查询条件
        String key = pageParamVo.getKey();
        if (StringUtils.isNotBlank(key)) {
            wrapper.and(t ->
                    t.eq("id", key).or().like("name", key));
        }
        //然后把封装好的查询，使用mp的分页方法进行分页
        IPage<SpuEntity> page = this.page(
                pageParamVo.getPage(),
                wrapper
        );
        //然后使用pageResultVo的构造方法把分页对象封装成PageResultVo对象返回
        return new PageResultVo(page);
    }

    @Override
    @Transactional()
    public void bigSave(SpuVo spuVo)  {
        //1、保存spu信息，需要携带服务器保存的时间
        Long spuId = saveSpu(spuVo);

        //2、保存spu描述信息(Spu_desc,即海报图片)
        this.descService.saveSpuDesc(spuVo, spuId);


//        try {
//            TimeUnit.SECONDS.sleep(5);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        int i = 1 / 0;
//        new FileInputStream("xxxx");

        //3、保存spu基本属性
        saveBaseAttrs(spuVo, spuId);

        //4、保存sku相关信息
        saveSkus(spuVo, spuId);
    }



    private void saveSkus(SpuVo spuVo, Long spuId) {
        List<SkuVo> skus = spuVo.getSkus();
        if (CollectionUtils.isEmpty(skus)) {
            return;//如果为空就不用保存sku相关信息
        }
        skus.forEach(skuVo -> {
            //保存sku 的信息,附带之前的spuid/cateId/brandId/defaultImge
            skuVo.setSpuId(spuId);
            skuVo.setCatagoryId(spuVo.getBrandId());
            skuVo.setBrandId(spuVo.getBrandId());
            List<String> images = skuVo.getImages();
            if (!CollectionUtils.isEmpty(images)) {
                //如果选择了默认图片就使用默认图片，如果没有的话话就选择第一张
                skuVo.setDefaultImage(StringUtils.isNotBlank(skuVo.getDefaultImage()) ?
                        skuVo.getDefaultImage() : images.get(0));
            }
            skuMapper.insert(skuVo);
            Long skuId = skuVo.getId();//获得回显的skuId


            //保存sku的图片信息
            if (!CollectionUtils.isEmpty(images)) {
                //使用流转换后批量保存sku的图片
                List<SkuImagesEntity> imagesEntities = images.stream().map(image -> {
                    SkuImagesEntity imagesEntity = new SkuImagesEntity();
                    imagesEntity.setUrl(image);
                    imagesEntity.setSkuId(skuId);
                    //判断当前图片是不是选中的图片，如果是的话就设置选中状态
                    if (StringUtils.equals(image, skuVo.getDefaultImage())) {

                        imagesEntity.setDefaultStatus(1);
                    }
                    imagesEntity.setSort(0);
                    return imagesEntity;
                }).collect(Collectors.toList());
                skuImagesService.saveBatch(imagesEntities);
            }


            //保存sku的销售属性
            List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
            if (!CollectionUtils.isEmpty(saleAttrs)) {
                saleAttrs.forEach(saleAttr -> {
                    saleAttr.setSkuId(skuId);
                    saleAttr.setSort(0);
                });
                skuAttrService.saveBatch(saleAttrs);
            }

            //保存sku的优惠信息【需要远程调用sms服务】
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVo, skuSaleVo);
            skuSaleVo.setSkuId(skuId);//必须携带skuId
            smsClient.saveSales(skuSaleVo);


        });
    }

    private void saveBaseAttrs(SpuVo spuVo, Long spuId) {
        List<SpuAttrValueVo> baseAttrs = spuVo.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)) {
            //使用流对象把vo集合转化为实体集合
            List<SpuAttrValueEntity> spuAttrValueEntities = baseAttrs.stream().map(attr -> {
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                BeanUtils.copyProperties(attr, spuAttrValueEntity);
                spuAttrValueEntity.setSort(0);
                //添加spuId
                spuAttrValueEntity.setSpuId(spuId);
                return spuAttrValueEntity;
            }).collect(Collectors.toList());
            //使用service批量加库
            baseAttrService.saveBatch(spuAttrValueEntities);
        }
    }



    private Long saveSpu(SpuVo spuVo) {
        spuVo.setCreateTime(new Date());
        spuVo.setUpdateTime(spuVo.getCreateTime());
        this.save(spuVo);
        return spuVo.getId();
    }


}