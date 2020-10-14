package com.atguigu.gmall.auth.service;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.common.exception.UserException;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IpUtil;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {
    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private JwtProperties jwtProperties;

    public void login(String loginName, String password, HttpServletRequest request, HttpServletResponse response) {
        //1、 查询用户
        UserEntity userEntity = this.umsClient.queryUser(loginName, password).getData();
        if (userEntity == null) {
            throw new UserException("用户名或密码错误");
        }
        //2、如果用户存在的话，那么就封装载荷信息

        HashMap<String, Object> map = new HashMap<>();
        map.put("userId", userEntity.getId());
        map.put("userName", userEntity.getUsername());
        //3、可能存在token被盗用的请狂，所以需要加入一个ip来确认，这里是微服务所以是servlet
        String ip = IpUtil.getIpAddressAtService(request);
        map.put("ip", ip);
        try {
            //4、生成token
            String token = JwtUtils.generateToken(map, jwtProperties.getPrivateKey(), jwtProperties.getExpire());
            //5、放入cookie中去
            CookieUtils.setCookie(request, response, jwtProperties.getCookieName(), token ,jwtProperties.getExpire()*60);
            //6、登陆成功后放入用户昵称
            CookieUtils.setCookie(request, response, jwtProperties.getNickName(), userEntity.getNickname(), jwtProperties.getExpire() * 60);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
