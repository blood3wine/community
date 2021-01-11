package com.nowcoder.community;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class CommunityApplication {
	//管理bean生命周期【初始化方法
	//修饰的方法会在构造器调用完后被执行
	@PostConstruct
	public void init(){
		// 解决netty启动冲突问题
		// see Netty4Utils.setAvailableProcessors()
		System.setProperty("es.set.netty.runtime.available.processors", "false");
	}


	public static void main(String[] args) {
		SpringApplication.run(CommunityApplication.class, args);
	}

}
