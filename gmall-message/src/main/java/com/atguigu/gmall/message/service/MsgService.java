package com.atguigu.gmall.message.service;

import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MsgService {


    public Boolean sendMsgPhone(String phone, Map<String, String> map) {
        try {
            //1创建初始化对象
            DefaultProfile profile =
                    DefaultProfile.getProfile("default", "LTAI4GEWg6FyBxaKk3FxEAye", "EGqmv3vnuVmEomPKULOd2xa4dZ5h7F");
            IAcsClient client = new DefaultAcsClient(profile);
            //2创建request对象
            CommonRequest request = new CommonRequest();
            //3向请求对象存入参数
            request.setMethod(MethodType.POST);
            request.setDomain("dysmsapi.aliyuncs.com");
            request.setVersion("2017-05-25");
            request.setAction("SendSms");

            request.putQueryParameter("PhoneNumbers", phone);
            request.putQueryParameter("SignName", "谷粒在线教育网");
            request.putQueryParameter("TemplateCode", "SMS_201470282");
            request.putQueryParameter("TemplateParam", JSONObject.toJSONString(map));

            //4调用初始化对象方法，发送请求，拿到响应
            CommonResponse response = client.getCommonResponse(request);
            System.out.println(response.getData());
            return response.getHttpResponse().isSuccess();
            //5从响应里获取最终结果
        } catch (ClientException e) {
            return false;
        }
    }
}
