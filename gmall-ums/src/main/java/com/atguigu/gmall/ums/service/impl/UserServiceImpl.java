package com.atguigu.gmall.ums.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        switch (type) {
            case 1: wrapper.eq("username",data);break;
            case 2: wrapper.eq("phone",data);break;
            case 3: wrapper.eq("email",data);break;
            default:
                return null;
        }
        return this.count(wrapper)==0;
    }

    @Override
    public void register(UserEntity userEntity, String code) {
        //1、TODO 查询Redis中的验证码和传过来的验证是否相同
        //2、生成随机盐并保存
        String salt = UUID.randomUUID().toString().substring(0, 6);
        userEntity.setSalt(salt);
        
        //3、对密码加盐加密
        userEntity.setPassword(DigestUtils.md5Hex(userEntity.getPassword() + salt));
        //4、把注册用户放入数据库中
        userEntity.setCreateTime(new Date());
        userEntity.setLevelId(1l);
        userEntity.setSourceType(1);
        userEntity.setStatus(1);
        userEntity.setIntegration(1000);
        userEntity.setGrowth(1000);
        this.save(userEntity);

        //5、TODO 删除redis中的短信验证码
    }

    @Override
    public UserEntity queryUser(String loginName, String password) {
        //这里传来的是明文密码，所以需要用户名对应的用户的盐进行加密
        //1、查询数据库中用户名对应的用户
        UserEntity dbUser = this.getOne(new QueryWrapper<UserEntity>().eq("username", loginName).or().eq("phone", loginName).or().eq("email", loginName));
        if (dbUser == null) {
            return null;
        }
        //2、获得盐并对明文密码加密
        String salt = dbUser.getSalt();
        password  = DigestUtils.md5Hex(password + salt);
        //3、判断这个用户登录密码和数据库的是否匹配
        if (!StringUtils.equals(password,dbUser.getPassword())) {
            return null;
        }
        return dbUser;
    }

}