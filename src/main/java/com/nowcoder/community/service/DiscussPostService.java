package com.nowcoder.community.service;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiscussPostService {
    @Autowired
    private DiscussPostMapper discussPostMapper;

    //网页上一般显示的不是userid，是用户名
    // 1.写sql的时候关联查询用户，然后一起查到，处理
    //2.得到数据后，单独的针对每一个DiscussPost，单独的查user，然后组合在一起返回给页面
    //采用后者 使用redis缓存数据的时候比较方便 代码也比较直观
    // 所以需要再提供一个方法 能够根据userid查到user 在UserService里面添加这个方法

    public List<DiscussPost> findDiscussPosts(int userId,int offset,int limit){
        return discussPostMapper.selectDiscussPosts(userId, offset, limit);
    }

    public int findDiscussPostRows(int userid){
        return discussPostMapper.selectDiscussPostRows(userid);
    }
}
