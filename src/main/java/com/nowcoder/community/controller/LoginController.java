package com.nowcoder.community.controller;


import com.google.code.kaptcha.Producer;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private MailClient mailClient;

    //创建日志对象
    private static final Logger logger=LoggerFactory.getLogger(LoginController.class);

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private UserService userService;

    //获取注册页面
    @RequestMapping(path = "/register",method = RequestMethod.GET)
    public String getRegisterPage(){
        return "/site/register";
    }

    //获取登录页面
    @RequestMapping(path = "/login",method = RequestMethod.GET)
    public String getLoginPage(){
        return "/site/login";
    }


    //返回的 方法post
    @RequestMapping(path = "/register",method = RequestMethod.POST)
    //往model里存数据 携带给模板
    //注册时传入的三个参数 SpringMVC注入给user对象
    public String register(Model model, User user){
        Map<String,Object>map=userService.register(user);
        if(map==null||map.isEmpty()){
            model.addAttribute("msg","注册成功，我们已经向您的邮箱发送一封激活邮件，请尽快激活");
            model.addAttribute("target","/index");
            return "/site/operate-result";

        }else{
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            model.addAttribute("emailMsg",map.get("emailMsg"));

            return "/site/register";
        }

    }


    //http://localhost:8080/community/activation/101/code    只需要看成功或失败 相当于查询 get就行了
    @RequestMapping(path = "/activation/{userId}/{code}",method = RequestMethod.GET)
    //@PathVariable是从路径中取值
    public String activation(Model model, @PathVariable("userId") int userId,@PathVariable("code") String code){
        int result=userService.activation(userId,code);
        if(result==ACTIVATION_SUCCESS){
            model.addAttribute("msg","激活成功，您的账号已经可以正常使用了");
            model.addAttribute("target","/login");
        }else if(result==ACTIVATION_REPEAT){
            model.addAttribute("msg","无效操作 该账号已经激活过了");
            model.addAttribute("target","/index");
        }else{
            model.addAttribute("msg","激活失败 您提供的激活码不正确");
            model.addAttribute("target","/index");
        }
        return "/site/operate-result";
    }

    //生成验证码方法
    @RequestMapping(path = "/kaptcha",method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session){
        //获取bean 注入
        //生成验证码
        String text=kaptchaProducer.createText();
        BufferedImage image=kaptchaProducer.createImage(text);

        //将验证码存入session
        //session.setAttribute("kaptcha",text);

        //验证码的归属
        String kaptchaOwner=CommunityUtil.generateUUID();
        //发给客户端，客户端用cookie保存
        Cookie cookie=new Cookie("kaptchaOwner",kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        //将验证码存入redis
        String redisKey= RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        //值是验证码,long time,TimeUnit timeunit
        redisTemplate.opsForValue().set(redisKey,text,60, TimeUnit.SECONDS);



        //将图片输出给浏览器
        //返回什么类型 -> 拼接类型的图片
        response.setContentType("image/png");
        try {
            OutputStream os=response.getOutputStream();
            //传什么，格式，用什么流
            // SpringMVC自动会关这个流 不用手动关
            ImageIO.write(image,"png",os);
        } catch (IOException e) {
            logger.error("响应验证码失败"+e.getMessage());
        }
    }

    @RequestMapping(path="/login",method = RequestMethod.POST)
    //boolean：是否记住我
    //还需要从session里把验证码取出来
    //成功后把ticket发放给客户端让他保存 用cookie保存
    public String login(String username, String password, String code,
                        boolean rememberme,Model model,/*HttpSession session,*/HttpServletResponse response,@CookieValue("kaptchaOwner") String kaptchaOwner){
        //检查验证码
        //String kaptcha= (String) session.getAttribute("kaptcha");

        String kaptcha=null;
        //key不为空，没失效
        if(StringUtils.isNotBlank(kaptchaOwner)){
            String redisKey=RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha= (String) redisTemplate.opsForValue().get(redisKey);
        }

        if(StringUtils.isBlank(kaptcha)||StringUtils.isBlank(code)||!kaptcha.equalsIgnoreCase(code)){
            model.addAttribute("codeMsg","验证码不正确");
            return "/site/login";
        }

        //检查账号密码
        int expiredSeconds=rememberme?REMEMBER_EXPIRED_SECONDS:DEFAULT_EXPIRED_SECONDS;
        Map<String,Object>map=userService.login(username,password,expiredSeconds);
        //登录成功ticket会有值，重定向到首页
        if(map.containsKey("ticket")){
            Cookie cookie=new Cookie("ticket",map.get("ticket").toString());
            //有效路径应该包含在整个项目之内
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        }else{
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "/site/login";
        }
    }

    @RequestMapping(path = "/logout",method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        SecurityContextHolder.clearContext();
        return "redirect:/login";


    }

    // 忘记密码页面
    @RequestMapping(path = "/forget", method = RequestMethod.GET)
    public String getForgetPage() {
        return "/site/forget";
    }

    //获取验证码
    @RequestMapping(path = "/forget/code",method = RequestMethod.GET)
    @ResponseBody
    public String getForgetCode(String email, HttpSession session) {

        if (StringUtils.isBlank(email)) {
            //return "邮箱错误";
            return CommunityUtil.getJSONString(1, "邮箱不能为空！");
        }

        // 发送邮件
        Context context=new Context();
        context.setVariable("email",email);
        String code=CommunityUtil.generateUUID().substring(0,4);
        context.setVariable("code",code);

        //给模板传参
        String content=templateEngine.process("/mail/forget",context);
        mailClient.sendMail(email,"忘记密码",content);


        // 保存验证码
        session.setAttribute("code",code);
        //return "发送邮件成功";
        return CommunityUtil.getJSONString(0);

    }

    // 重置密码
    @RequestMapping(path = "/forget/password",method = RequestMethod.POST)
    public String resetPassword(String email,String password,String code,HttpSession session,Model model){
        //验证验证码是否正确
        String correct= (String) session.getAttribute("code");
        if(StringUtils.isBlank(code)||StringUtils.isBlank(correct)||!code.equalsIgnoreCase(correct)){
            model.addAttribute("codeMsg","验证码错误");
            return "/site/forget";
        }
        Map<String, Object> map = userService.resetPassword(email, password);
        if(map.containsKey("user")){
            return "redirect:/login";
        }else{
            model.addAttribute("emailMsg", map.get("emailMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/forget";
        }
    }

}
