package com.atguigu.gmall.common.exception;

import org.springframework.stereotype.Component;

public class OrderException extends  RuntimeException{
    public OrderException() {
        super();
    }

    public OrderException(String message) {
        super(message);
    }
}
