package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

//    @Autowired
//    private LoginTicketMapper loginTicketMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    //模板引擎
    @Autowired
    private TemplateEngine templateEngine;

    //发邮件时有一个激活码 里面有域名、项目名
    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    //根据用户id查询用户
    public User findUserById(int id){
        //return userMapper.selectById(id);
        //首先去cache里查
        User user=getCache(id);
        if(user==null){
            user=initCache(id);
        }
        return user;
    }

    public Map<String,Object> register(User user){
        Map<String, Object> map=new HashMap<>();
        //空值处理
        if(user==null){
            throw new IllegalArgumentException("参数不能为空");
        }
        //用户名为空不是程序错误 不抛异常 放进map里返回
        if(StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg","账号不能为空");
            return map;
        }
        if(StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg","密码不能为空");
            return map;
        }
        if(StringUtils.isBlank(user.getEmail())){
            map.put("emailMsg","游戏不能为空");
            return map;
        }

        //验证账号
        User u=userMapper.selectByName(user.getUsername());
        if(u!=null){
            map.put("usernameMsg","该账号已存在");
            return map;
        }
        //验证邮箱
        u=userMapper.selectByEmail(user.getEmail());
        if(u!=null){
            map.put("emailMsg","该邮箱已被注册");
            return map;
        }

        //注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        //format里会用数字来替代%d
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //给用户发个激活邮件
        Context context=new Context();

        //给模板传参
        context.setVariable("email",user.getEmail());
        //http://localhost:8080/community/activation/101/code
        //mybatis.configuration.useGeneratedKeys=true 配置文件里自动生成id
        String url=domain+contextPath+"/activation/"+user.getId()+"/"+user.getActivationCode();
        context.setVariable("url",url);

        //模板引擎的process生成动态网页
        String content= templateEngine.process("/mail/activation",context);

        mailClient.sendMail(user.getEmail(), "激活账号",content);


        return map;
    }

    public int activation(int userId,String code){
        User user=userMapper.selectById(userId);
        //重复激活
        if(user.getStatus()==1){
            return ACTIVATION_REPEAT;
        }else if(user.getActivationCode().equals(code)){
            //激活码相等 修改状态为1
            userMapper.updateStatus(userId,1);
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        }else{
            //不等就gg
            return ACTIVATION_FAILURE;
        }
    }

    public Map<String,Object> login(String username,String password,int expiredSeconds){

        Map<String,Object> map=new HashMap<>();

        //空值处理
        if(StringUtils.isBlank(username)){
            map.put("usernameMsg","账号不能为空");
            return map;
        }
        if(StringUtils.isBlank(password)){
            map.put("passwordMsg","密码不能为空");
            return map;
        }

        //验证账号
        User user=userMapper.selectByName(username);
        if(user==null){
            map.put("usernameMsg","该账号不存在");
            return map;
        }
        //验证状态
        if(user.getStatus()==0){
            map.put("usernameMsg","该账号未激活");
            return map;
        }
        //验证密码
        password=CommunityUtil.md5(password+user.getSalt());
        if(!user.getPassword().equals(password)){
            map.put("passwordMsg","密码不正确");
            return map;
        }

        //生成登录凭证
        LoginTicket loginTicket=new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + 1000 *expiredSeconds));
        //loginTicketMapper.insertLoginTicket(loginTicket);

        String redisKey= RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        //每个人有个自己的ticket，每个人单独存一份
        //redis会把ticket序列化成json对象的字符串
        redisTemplate.opsForValue().set(redisKey,loginTicket);


        map.put("ticket",loginTicket.getTicket());
        return map;
    }

    //忘记密码
    public Map<String,Object> resetPassword(String email,String password){
        Map<String,Object> map=new HashMap<>();

        //空值处理
        if(StringUtils.isBlank(email)){
            map.put("emailMsg","邮箱不能为空");
            return map;
        }
        if(StringUtils.isBlank(password)){
            map.put("passwordMsg","密码不能为空");
            return map;
        }

        //验证邮箱
        User user=userMapper.selectByEmail(email);

        if(user==null){
            map.put("emailMsg","邮箱错误");
            return map;
        }

        //重置密码
        password=CommunityUtil.md5(password+user.getSalt());

        userMapper.updatePassword(user.getId(),password);
        clearCache(user.getId());

        map.put("user",user);
        return map;

    }

    public void logout(String ticket){
        //loginTicketMapper.updateStatus(ticket,1);
        String redisKey= RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket= (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey,loginTicket);
    }

    public LoginTicket findLoginTicket(String ticket) {
        //return loginTicketMapper.selectByTicket(ticket);
        String redisKey= RedisKeyUtil.getTicketKey(ticket);
       return (LoginTicket) redisTemplate.opsForValue().get(redisKey);

    }

    //更新修改头像的路径
    public int updateHeader(int userId,String headerUrl){
        //return userMapper.updateHeader(userId,headerUrl);
        //访问redis和访问mysql不能放到一个事务之内，防止更新失败，更新之后再清理缓存
        int rows=userMapper.updateHeader(userId,headerUrl);;
        clearCache(userId);
        return rows;
    }

    //更改密码
    public Map<String,Object> updatePassword(int userId,String oldPassword,String newPassword){
        Map<String,Object> map=new HashMap<>();
        if(StringUtils.isBlank(oldPassword)){
            map.put("oldPasswordMsg","原密码不能为空");
            return map;
        }
        if(StringUtils.isBlank(newPassword)){
            map.put("newPasswordMsg","新密码不能为空");
            return map;
        }
        //验证原始密码
        User user=userMapper.selectById(userId);
        oldPassword=CommunityUtil.md5(oldPassword+user.getSalt());
        if(!oldPassword.equals(user.getPassword())){
            map.put("oldPasswordMsg","原密码错误");
            return map;
        }

        newPassword=CommunityUtil.md5(newPassword+user.getSalt());
        userMapper.updatePassword(user.getId(), newPassword);
        clearCache(userId);

        return map;
    }

    public User findUserByName(String username) {
        return userMapper.selectByName(username);
    }

    //查询用户缓存
    // 1.优先从缓存中取值
    private User getCache(int userId){
        String redisKey=RedisKeyUtil.getUserKey(userId);
       return (User) redisTemplate.opsForValue().get(redisKey);

    }


    // 2.取不到时初始化缓存数据
    private User initCache(int userId){
        //从mysql里查到
        User user=userMapper.selectById(userId);
        String redisKey=RedisKeyUtil.getUserKey(userId);
        //给个过期时间
        redisTemplate.opsForValue().set(redisKey,user,3600, TimeUnit.SECONDS);
        return user;

    }

    // 3.数据变更时清除缓存数据【改数据后更新缓存的方法：复杂 并且可能会有并发的问题
    private void clearCache(int userId){
        String redisKey=RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);

    }

}
