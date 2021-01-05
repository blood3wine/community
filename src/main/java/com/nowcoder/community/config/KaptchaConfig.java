package com.nowcoder.community.config;

import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KaptchaConfig {
    @Bean
    public Producer kaptchaProducer(){
        Properties properties=new Properties();
        properties.setProperty("kaptcha.image.width","100");
        properties.setProperty("kaptcha.image.heigt","40");
        //字号（font是字体）
        properties.setProperty("kaptcha.textproducer.font.size","32");
        properties.setProperty("kaptcha.textproducer.font.color","0,0,0");

        //字符串范围去随机
        properties.setProperty("kaptcha.textproducer.char.string","0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        //生成几个随机字符
        properties.setProperty("kaptcha.textproducer.char.length","4");

        //采用哪个噪声类
        properties.setProperty("kaptcha.noise.impl","com.google.code.kaptcha.impl.NoNoise");







        DefaultKaptcha kaptcha=new DefaultKaptcha();
        //封装到config对象里
        // 所有东西都是config去配的
        // config需要依赖于 Properties 对象
        Config config=new Config(properties);
        kaptcha.setConfig(config);
        return kaptcha;
    }
}
