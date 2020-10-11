package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class IndexController {
    @Autowired
    private IndexService indexService;

    @GetMapping({"index/cates/{pid}"})
    @ResponseBody
    public ResponseVo<List<CategoryEntity>> queryCategoryLv2WithSubsByPid(@PathVariable("pid")Long pid) {
        List<CategoryEntity> categoryEntities = indexService.queryCategoryLv2WithSubsByPid(pid);
        return ResponseVo.ok(categoryEntities);
    }


    @GetMapping({"/","index"})
    public String toIndex(Model model) {
        //查询一级分类
        List<CategoryEntity> categoryEntities = indexService.queryLv1Categories();
        model.addAttribute("categories", categoryEntities);
        return "index";
    }

    //测试分布式锁的案例
    @GetMapping({"index/test/lock"})
    @ResponseBody
    public ResponseVo testLock(Model model) throws InterruptedException {
        indexService.testLock();
        return ResponseVo.ok();
    }

    //测试分布式读写锁的读
    @GetMapping({"index/test/read"})
    @ResponseBody
    public ResponseVo testRead(){
        this.indexService.testRead();
        return ResponseVo.ok();
    }


    //测试分布式读写锁的写
    @GetMapping({"index/test/write"})
    @ResponseBody
    public ResponseVo testWrite(){
        this.indexService.testWrite();
        return ResponseVo.ok();
    }


    //这个是latch方法，等count计数完毕执行
    @GetMapping({"index/test/latch"})
    @ResponseBody
    public ResponseVo testLatch(){
        String msg = this.indexService.testLatch();
        return ResponseVo.ok(msg);
    }


    //这个是倒计数当大，执行一次count减1
    @GetMapping({"index/test/countDown"})
    @ResponseBody
    public ResponseVo countDown(){
        String msg = this.indexService.countDown();
        return ResponseVo.ok(msg);
    }
}
