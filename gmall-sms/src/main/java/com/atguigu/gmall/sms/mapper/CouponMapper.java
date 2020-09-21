package com.atguigu.gmall.sms.mapper;

import com.atguigu.gmall.sms.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author xiaoliu
 * @email xiaoliu@atguigu.com
 * @date 2020-09-21 20:00:16
 */
@Mapper
public interface CouponMapper extends BaseMapper<CouponEntity> {
	
}
