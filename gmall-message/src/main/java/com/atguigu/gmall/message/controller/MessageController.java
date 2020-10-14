package com.atguigu.gmall.message.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.message.service.MsgService;
import com.atguigu.gmall.message.utils.RandomUtil;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController("message")
public class MessageController {
    @Autowired
    private MsgService msmService;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;


    @ApiOperation(value = "发送验证码短信")
    @GetMapping("sendMsgPhone/{phone}")
    public ResponseVo<Object> sendMsmPhone(@PathVariable String phone){

        //1根据手机号查询redis，有没有想关验证码
        String rPhone = redisTemplate.opsForValue().get(phone);
        if(!StringUtils.isEmpty(rPhone)){
            return ResponseVo.ok();
        }
        //2生成验证码
        String code = RandomUtil.getFourBitRandom();
        Map<String,String> map = new HashMap<>();
        map.put("code",code);
        //3调用接口发送验证码
        Boolean isSend = msmService.sendMsgPhone(phone,map);
        //4验证发送成功，验证码存入redis
        if(isSend){
            redisTemplate.opsForValue().set(phone,code,5, TimeUnit.MINUTES);
            return ResponseVo.ok();
        }else{
            return ResponseVo.fail("发送失败");
        }
    }
}
