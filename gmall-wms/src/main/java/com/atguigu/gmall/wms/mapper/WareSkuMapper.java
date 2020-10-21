package com.atguigu.gmall.wms.mapper;

import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author xiaoliu
 * @email xiaoliu@atguigu.com
 * @date 2020-09-21 20:31:00
 */
@Mapper
public interface WareSkuMapper extends BaseMapper<WareSkuEntity> {
    public List<WareSkuEntity> check(@Param("skuId") Long skuId,@Param("count") Integer count);
    public int lock(@Param("id") Long id,@Param("count") Integer count);
    public int unlock(@Param("id") Long id,@Param("count") Integer count);

    int minus(@Param("id") Long id,@Param("count")Integer count);
}
