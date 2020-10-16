package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CartAsyncService {

    @Autowired
    private CartMapper cartMapper;
    @Async
    public void updateCart(String userId,Cart cart) {
        //更新到MySQL数据库中
        cartMapper.update(cart, new QueryWrapper<Cart>().eq("user_id", userId).eq("sku_id", cart.getSkuId()));
    }

    @Async
    public void insertCart(Cart cart) {
        //更新到MySQL数据库中
        cartMapper.insert(cart);
    }

    @Async
    public void deleteCartByUserId(String userId) {
        //更新到MySQL数据库中
        cartMapper.delete(new QueryWrapper<Cart>().eq("user_id", userId));

    }


}

