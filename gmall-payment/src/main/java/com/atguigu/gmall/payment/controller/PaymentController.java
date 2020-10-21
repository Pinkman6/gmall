package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.config.AlipayTemplate;
import com.atguigu.gmall.payment.entity.PaymentInfoEntity;
import com.atguigu.gmall.payment.interceptor.LoginInterceptor;
import com.atguigu.gmall.payment.service.PaymentService;
import com.atguigu.gmall.payment.vo.PayAsyncVo;
import com.atguigu.gmall.payment.vo.PayVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;

@Controller
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    //跳转支付页面
    @GetMapping("pay.html")
    public String toPay(@RequestParam("orderToken")String orderToken, Model model) {
        OrderEntity orderEntity = paymentService.queryOrderByToken(orderToken);
        Long userId = LoginInterceptor.getUserInfo().getUserId();
        //合法检查
        //只有当前订单不为空且是当前用户的订单且当前订单状态是待支付状态才能支付
        if (orderToken==null || userId!=orderEntity.getUserId() || orderEntity.getStatus()!=0) {
            throw new OrderException("非法参数！");
        }
        //调用支付宝接口跳转支付宝页面，获得支付表单
        model.addAttribute("orderEntity", orderEntity);
        return "pay";
    }

    //点击支付后跳转到支付宝支付页面
    @GetMapping("alipay.html")
    @ResponseBody
    public Object toAlipay(@RequestParam("orderToken")String orderToken) {
        OrderEntity orderEntity = paymentService.queryOrderByToken(orderToken);
        Long userId = LoginInterceptor.getUserInfo().getUserId();
        //合法检查
        //只有当前订单不为空且是当前用户的订单且当前订单状态是待支付状态才能支付
        if (orderToken==null || userId!=orderEntity.getUserId() || orderEntity.getStatus()!=0) {
            throw new OrderException("非法参数！");
        }
        try {
            PayVo payVo = new PayVo();
            payVo.setOut_trade_no(orderEntity.getOrderSn());
            payVo.setSubject("谷粒商城支付订单");
            payVo.setTotal_amount("0.01");
            //支付时生成支付记录
            Long id = this.paymentService.savePaymentInfo(orderEntity);
            //传递同样的可回传的参数，在对账记录生成后可以获得对账记录的id
            payVo.setPassback_params(id.toString());

            return alipayTemplate.pay(payVo);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    //支付宝同步调用接口，如果成功的话，跳转到支付成功页面
    @GetMapping("pay/ok")
    public String payOk() {
        return "paysuccess";
    }

    //支付宝异步回调接口
    @PostMapping("pay/success")
    @ResponseBody
    public String paysuccess(PayAsyncVo payAsyncVo) {
        System.out.println("异步回调成功============================");
        //回调的时候更新订单状态以及异步更新库存记录

        //1.验签
        Boolean flag = this.alipayTemplate.checkSignature(payAsyncVo);
        if (!flag) {
            //验签失败返回结果
            return "failure";
        }
        //2.验证业务参数和数据库中的对账记录数据比较
        String app_id = payAsyncVo.getApp_id();
        String out_trade_no = payAsyncVo.getOut_trade_no();
        String total_amount = payAsyncVo.getTotal_amount();
        String payId = payAsyncVo.getPassback_params();
        PaymentInfoEntity paymentInfoEntity = this.paymentService.queryById(payId);
        if (!StringUtils.equals(app_id,alipayTemplate.getApp_id())||
                !StringUtils.equals(out_trade_no,paymentInfoEntity.getOutTradeNo())||
                paymentInfoEntity.getTotalAmount().compareTo(new BigDecimal(total_amount))!=0
        ){
            return "failure";
        }
        //3.校验支付状态码
        if (!StringUtils.equals("TRADE_SUCCESS", payAsyncVo.getTrade_status())) {
            return "failure";
        }
        //4.更新对账记录的支付状态
        if (this.paymentService.update(payAsyncVo)==1){
            //5/更新对账记录成功后发送消息给oms更新订单状态
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE","order.success",out_trade_no);

        }
        //6.告诉支付宝已经成功支付了
        return "success";
    }

}
