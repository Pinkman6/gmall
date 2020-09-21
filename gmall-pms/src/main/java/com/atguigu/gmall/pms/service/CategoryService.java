package com.atguigu.gmall.pms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.CategoryEntity;

import java.util.Map;

/**
 * 商品三级分类
 *
 * @author xiaoliu
 * @email xiaoliu@atguigu.com
 * @date 2020-09-21 19:17:02
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}
