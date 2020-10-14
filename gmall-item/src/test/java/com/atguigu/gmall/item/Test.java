package com.atguigu.gmall.item;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class Test {
    public static void main(String[] args) throws IOException {
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("=====supplyAsync===a");
            return "supplyAsync";
        });
        CompletableFuture<String> completableFuture1 = completableFuture.thenApplyAsync(s -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("=====thenAcceptAsync===b");
            return "thenApplyAsync";
        });
        CompletableFuture<Void> completableFuture2 = completableFuture.thenAcceptAsync(s -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("=====thenAcceptAsync===c");
        });
        CompletableFuture<Void> completableFuture3 = completableFuture.thenRunAsync(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("=====thenRunAsync===d");
        });
        CompletableFuture.anyOf(completableFuture, completableFuture1, completableFuture2, completableFuture3).join();
        System.out.println("主线程任务执行完毕");
        return;
//        System.in.read();
//        CompletableFuture.allOf(completableFuture1, completableFuture2, completableFuture3).
    }
}
