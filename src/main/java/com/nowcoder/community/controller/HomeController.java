package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Controller没有访问路径 直接访问方法
@Controller
public class HomeController implements CommunityConstant {
    @Autowired
    private DiscussPostService discussPostService;
    //查到的只是userid 再把UserService注入进来 根据id查user

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;



    //响应的是请求
    //返回类型可以是MAV也可以是String 就是视图的路径
    @RequestMapping(path="/index",method = RequestMethod.GET)
    public String getIndexPage(Model model, Page page){
        //方法调用之前 SpringMVC会自动实例化model和page 并且将page注入给model
        //所以在thymeleaf中可以直接访问Page对象中的数据，不用model再add一次
        page.setRows(discussPostService.findDiscussPostRows(0));
        page.setPath("/index");

        List<DiscussPost>list= discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit());
        List<Map<String,Object>> discussPosts=new ArrayList<>();
        for(DiscussPost post:list){
            if(list!=null){
                Map<String,Object>map=new HashMap<>();
                map.put("post",post);

                User user=userService.findUserById(post.getUserId());
                map.put("user",user);

                //增加 首页点赞数量 功能
                //帖子类型，帖子id
                long likeCount =likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount",likeCount);

               discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts",discussPosts);
        return "/index";
    }

    @RequestMapping(path = "/error",method = RequestMethod.GET)
    public String getErrorPage(){
        return "/error/500";

    }
}


