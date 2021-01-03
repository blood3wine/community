package com.nowcoder.community;

import com.nowcoder.community.util.MailClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


//          在正常情况下测试类是需要@RunWith的，作用是告诉java你这个类通过用什么运行环境运行，
//        例如启动和创建spring的应用上下文。否则你需要为此在启动时写一堆的环境配置代码。
//        你在IDEA里去掉@RunWith仍然能跑是因为在IDEA里识别为一个JUNIT的运行环境，相当于就是一个自识别的RUNWITH环境配置。
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MailTests {

    @Autowired
    private MailClient mailClient;

    //模板引擎
    @Autowired
    private TemplateEngine templateEngine;

    @Test
    public void testTextMail() {
        mailClient.sendMail("1542538428@qq.com", "TEST", "Welcome.");
    }

    @Test
    public void testHtmlMail(){
        //给模板传参
        Context context=new Context();
        context.setVariable("username","sunday");
        //通过process生成动态网页
        String content=templateEngine.process("/mail/demo",context);
        System.out.println(content);

        mailClient.sendMail("1542538428@qq.com", "HTML", content);
    }

}
