package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {
    @Autowired
    private CartService cartService;

    @Autowired
    private LoginInterceptor loginInterceptor;

    @GetMapping
    public String addCart(Cart cart) {
        this.cartService.addCart(cart);
        return "redirect:http://cart.gmall.com/addCart.html?skuId=" + cart.getSkuId();
    }

    @GetMapping("addCart.html")
    public String queryCartBySkuId(@RequestParam("skuId") Long skuId, Model model) {
        Cart cart = this.cartService.queryCartBySkuId(skuId);
        model.addAttribute("cart", cart);
        return "addCart";
    }

    //查询购物车信息
    @GetMapping("cart.html")
    public String queryCarts(Model model) {
        List<Cart> carts = this.cartService.queryCarts();
        model.addAttribute("carts", carts);
        return "cart";
    }

    //更新购物车数量信息
    @PostMapping("updateNum")
    @ResponseBody
    public ResponseVo updateNum(@RequestBody Cart cart) {
        this.cartService.updateNum(cart);
        return ResponseVo.ok();
    }

    //根据skuId删除购物车信息
    @GetMapping("deleteCart")
    @ResponseBody
    public ResponseVo deleteCart(@RequestParam("skuId") Long skuId) {
        this.cartService.deleteCart(skuId);
        return ResponseVo.ok();
    }

    //根据userId查询用户选中的购物车信息，主要的是skuId和count
    @GetMapping("checked/{userId}")
    @ResponseBody
    public ResponseVo<List<Cart>> queryCheckedCartsByUserId(@PathVariable("userId")Long userId){
        List<Cart> carts = this.cartService.queryCheckedCartsByUserId(userId);
        return ResponseVo.ok(carts);
    }


    @GetMapping("test")
    @ResponseBody
    public String test(HttpServletRequest request) throws ExecutionException, InterruptedException {
//        System.out.println(LoginInterceptor.getUserInfo());
        long now = System.currentTimeMillis();
        String executor1 = cartService.executor1();
        String executor2 = cartService.executor2();
//        executor1.addCallback(s -> System.out.println("成功了" + s), e -> System.out.println("失败了" + e));
//        executor2.addCallback(s -> System.out.println("成功了" + s), e -> System.out.println("失败了" + e));

        System.out.println(System.currentTimeMillis()-now);
        return "hello intercepter";
    }
}
