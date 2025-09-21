package com.yangxj.quartz.controller;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;


/**
 * @author yangxj
 * @date 2019/5/16-21:45
 * 注意：一个任务JOB可以添加多个Trigger 但是一个Trigger只能绑定一个JOB 这点需要注意
 */
@Slf4j
@RestController
public class TestController {

    @RequestMapping("/test")
    public String getTest()  {
        log.info("enter test");
        System.out.println("enter test");
        return getData();
    }
    @RequestMapping("/testlist")
    public List<String> getList() {
        log.info("enter getList");
        System.out.println("enter getList");
//        List<String> a = new ArrayList<>();
//        a.add("11");
//        a.add("test1");
        return getListData();
    }

    @RequestMapping("/getAge")
    public Integer getAge()  {
        log.info("enter getAge");
        System.out.println("enter getAge");
        return 1;
    }
    public String getData(){
        return "1111";
    }
    public List<String> getListData(){
        List<String> a = new ArrayList<>();
        a.add("11");
        a.add("test1");
        return a;
    }
}
