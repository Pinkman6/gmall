package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.SpuEntity;
import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.service.SpuService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

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

}