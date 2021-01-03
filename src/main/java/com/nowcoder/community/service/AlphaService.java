package com.nowcoder.community.service;

import com.nowcoder.community.dao.AlphaDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
//@Scope("prototype")//bean的作用范围 默认是singleton 单例
public class AlphaService {

    @Autowired
    private AlphaDao alphaDao;

    public  AlphaService() {
        System.out.println("实例化AlphaService");
    }

    @PostConstruct
    //这个方法会在构造器之后调用
    public void init(){
        System.out.println("初始化AlphaService");
    }

    @PreDestroy
    //销毁对象之前调用它
    //可以在这释放某些资源
    public void destroy(){
        System.out.println("销毁AlphaService");
    }

    public String find(){
        return alphaDao.select();
    }


}
