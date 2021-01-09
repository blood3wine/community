package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.*;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger= LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.load}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private CommentService commentService;
    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    //返回配置页面
    @LoginRequired
    @RequestMapping(path = "/setting",method = RequestMethod.GET)
    public String getSettingPage(){
        return "/site/setting";
    }

    //处理上传文件的请求
    //SpringMVC通过MultipartFile处理上传文件
    //向页面返回model
    @LoginRequired
    @RequestMapping(path = "/upload",method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model){
        if(headerImage==null){
            model.addAttribute("error","您还没有选择图片");
            return "/site/setting";
        }

        //避免覆盖，生成随机名字，后缀不改

        //先暂存文件后缀
        String fileName=headerImage.getOriginalFilename();
        //最后一个.往后截取
        String suffix=fileName.substring(fileName.lastIndexOf("."));
        //判断后缀合不合理
        if(StringUtils.isBlank(suffix)){
            model.addAttribute("error","文件格式不正确");
            return "/site/setting";
        }

        //生成随机文件名
        fileName= CommunityUtil.generateUUID()+suffix;
        //确定文件存放的路径
        File dest=new File(uploadPath+"/"+fileName);
        try {
            //把当前文件的内容写到目标文件dest里面去 存储文件
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败"+e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常",e);
        }

        //存储头像成功，更新用户头像路径(web访问路径)
        //要先知道当前用户是谁
        User user=hostHolder.getUser();
        //(web访问路径)http://localhost:8080/community/user/header/xxx.png
        String headerUrl=domain+contextPath+"/user/header/"+fileName;
        userService.updateHeader(user.getId(),headerUrl);

        return "redirect:/index";

    }

    //获取头像
    //void -> 通过流手动向服务器去输出，手动调response往外写
    @RequestMapping(path = "/header/{fileName}",method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response){

        //服务器存放路径
        fileName=uploadPath+"/"+fileName;
        //文件后缀
        String suffix=fileName.substring(fileName.lastIndexOf("."));
        //响应图片
        response.setContentType("image/"+suffix);
        //字节流
        try (
                FileInputStream fis=new FileInputStream(fileName);
                OutputStream os=response.getOutputStream();
        ){

            //建立缓冲区
            byte[] buffer=new byte[1024];
            //建立游标
            int b=0;
            while((b=fis.read(buffer))!=-1){
                os.write(buffer,0,b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败"+e.getMessage());
        }

    }

    //修改密码
    @LoginRequired
    @RequestMapping(path = "/updatePassword",method = RequestMethod.POST)
    public String updatePassword(String oldPassword,String newPassword,Model model){
        //从session里面取用户
        User user=hostHolder.getUser();
        Map<String, Object> map = userService.updatePassword(user.getId(), oldPassword, newPassword);
        if(map.isEmpty()){
            return "redirect:/logout";
        }else{
            model.addAttribute("oldPasswordMsg",map.get("oldPasswordMsg"));
            model.addAttribute("newPasswordMsg",map.get("newPasswordMsg"));
            return "/site/setting";
        }

    }

    //个人主页
    //useId是要访问的哪个用户
    @RequestMapping(path = "/profile/{userId}",method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId,Model model){
        User user=userService.findUserById(userId);
        if(user==null){
            throw new RuntimeException("用户不存在");
        }
        //得到关注数量，传给模板
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount",followeeCount);

        //粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER,userId);
        model.addAttribute("followerCount",followerCount);

        //当前用户是否对这个用户关注了
        boolean hasFollowed=false;
        //登录后才有可能关注
        if(hostHolder.getUser()!=null){
            hasFollowed=followService.hasFollowed(hostHolder.getUser().getId(),ENTITY_TYPE_USER,userId);
        }
        model.addAttribute("hasFollowed",hasFollowed);

        //用户
        model.addAttribute("user",user);
        //点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount",likeCount);
        return "/site/profile";
    }


    //我的帖子
   @RequestMapping(path = "/profile/{userId}/myDiscussPost",method = RequestMethod.GET)
    public String getMyDiscussPost(@PathVariable("userId") int userId, Page page,Model model){
        User user=userService.findUserById(userId);
        if(user==null){
            throw new RuntimeException("该用户不存在");
        }
        model.addAttribute("user",user);
        page.setRows(discussPostService.findDiscussPostRows(userId));
        page.setPath("/user/profile/"+userId+"/myDiscussPost");
        page.setLimit(5);
        List<DiscussPost> list = discussPostService.findDiscussPosts(userId, page.getOffset(), page.getLimit());

        List<Map<String,Object>> myDiscussPosts=new ArrayList<>();
        for(DiscussPost post:list){
            if(list!=null){
                Map<String,Object>map=new HashMap<>();
                map.put("post",post);
                long likeCount =likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount",likeCount);

                myDiscussPosts.add(map);
            }
        }
        model.addAttribute("myDiscussPosts",myDiscussPosts);
        return "/site/my-post";
    }

    //我的回复
    @RequestMapping(path = "/profile/{userId}/myReplyPost",method = RequestMethod.GET)
    public String getMyReplyPost(@PathVariable("userId") int userId, Page page,Model model){
        User user=userService.findUserById(userId);
        if(user==null){
            throw new RuntimeException("该用户不存在");
        }
        model.addAttribute("user",user);

        page.setLimit(5);
        page.setPath("/user/profile/"+userId+"/myReplyPost");
        page.setRows(commentService.findCommentCountByUserId(ENTITY_TYPE_POST,userId));

        List<Comment> list = commentService.findCommentsByUserId(ENTITY_TYPE_POST, userId, page.getOffset(), page.getLimit());

        List<Map<String,Object>> myReplyPosts=new ArrayList<>();
        for(Comment comment:list){
            if(list!=null){
                Map<String,Object>map=new HashMap<>();
                map.put("comment",comment);
                DiscussPost post = discussPostService.findDiscussPostById(comment.getEntityId());
                map.put("post",post);
                long likeCount =likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                map.put("likeCount",likeCount);

                myReplyPosts.add(map);
            }
        }




        model.addAttribute("myReplyPosts",myReplyPosts);
        return "/site/my-reply";

    }

}
