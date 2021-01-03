package com.nowcoder.community.util;


//提供发邮件的功能 把发邮件的事委托给qq邮箱去做 代替了我们的客户端

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

//component交给spring容器管理 通用注解
@Component
public class MailClient {

    private static final Logger logger= LoggerFactory.getLogger(MailClient.class);

    @Autowired
    private JavaMailSender mailSender;

    //发送人固定
    //通过key【spring.mail.username】注入到bean里去
    @Value("${spring.mail.username}")
    private String from;

    public void sendMail(String to, String subject,String content){
        try {
            MimeMessage message=mailSender.createMimeMessage();
            //帮助类
            MimeMessageHelper helper=new MimeMessageHelper(message);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            //true支持html文本
            helper.setText(content,true);
            //从helper里把它构建好的message取到
            mailSender.send(helper.getMimeMessage());
        } catch (MessagingException e) {
            logger.error("发送邮件失败:"+e.getMessage());
        }
    }

}
