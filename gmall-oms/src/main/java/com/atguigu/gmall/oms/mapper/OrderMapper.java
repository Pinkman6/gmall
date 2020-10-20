package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单
 * 
 * @author xiaoliu
 * @email xiaoliu@atguigu.com
 * @date 2020-09-21 20:13:32
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
    public int updateStatus(@Param("orderToken") String orderToken, @Param("expect") Integer expect, @Param("target") Integer target);
}
