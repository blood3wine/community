package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.service.ElasticsearchService;
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

@Controller
public class SearchController implements CommunityConstant {
    @Autowired
    private ElasticsearchService elasticsearchService;

    //搜到帖子后展现帖子作者
    @Autowired
    private UserService userService;

    //展现帖子点赞数量
    @Autowired
    private LikeService likeService;

    //   search?keyword=xxx
    @RequestMapping(path = "/search",method = RequestMethod.GET)
    public String search(String keyword, Page page, Model model){
        //搜索帖子
        org.springframework.data.domain.Page<DiscussPost> searchResult=
        elasticsearchService.searchDiscussPost(keyword, page.getCurrent()-1, page.getLimit() );
        //查user
        List<Map<String,Object>> discussPosts=new ArrayList<>();
        if(searchResult!=null){
            for(DiscussPost post:searchResult){
                Map<String,Object> map=new HashMap<>();
                //先存帖子
                map.put("post",post);
                //存作者
                map.put("user",userService.findUserById(post.getUserId()));
                //得到点赞数量
                map.put("likeCount",likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));

                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts",discussPosts);
        //在链接里显示默认字
        model.addAttribute("keyword",keyword);

        //设置分页信息
        page.setPath("/search?keyword="+keyword);
        page.setRows(searchResult==null?0: (int) searchResult.getTotalElements());

        return "/site/search";


    }

}
