package com.nowcoder.community.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;

//爷想配置第三方的类 可以自己写个配置类 在配置类中通过bean注解进行声明
//然后装配一个第三方的bean
@Configuration//表示这是个配置类
public class AlphaConfig {
    @Bean//bean的名字是以方法命名的 就是simpleDateFormat
    //这个方法返回的对象将被装配到容器里
    public SimpleDateFormat simpleDateFormat(){
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }
}
