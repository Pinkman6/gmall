package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.mapper.CategoryMapper;
import com.atguigu.gmall.pms.service.CategoryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CategoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<CategoryEntity> queryCategoryByPid(Long parentId) {
        //-1：查询所有，0：查询一级节点
        QueryWrapper<CategoryEntity> wrapper = new QueryWrapper<>();
        //如果不等于-1的话就加上查询条件
        if (parentId != -1) {
            wrapper.eq("parent_id", parentId);
        }
        return  this.list(wrapper);
    }

    @Override
    public List<CategoryEntity> queryCategoryLv2WithSubsByPid(Long pid) {
        return categoryMapper.queryCategoriesByPid(pid);
    }

    @Override
    public List<CategoryEntity> queryCategoriesByCid3(Long cid3) {
        //查询三级分类
        CategoryEntity Lv3categoryEntity = this.categoryMapper.selectById(cid3);
        //查询二级分类
        CategoryEntity lv2categoryEntity = this.categoryMapper.selectById(Lv3categoryEntity.getParentId());
        //查询一级分类
        CategoryEntity lv1categoryEntity = this.categoryMapper.selectById(lv2categoryEntity.getParentId());
        return Arrays.asList(lv1categoryEntity, lv2categoryEntity, Lv3categoryEntity);
    }

}