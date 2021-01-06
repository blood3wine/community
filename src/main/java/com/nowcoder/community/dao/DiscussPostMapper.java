package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {
    //userId为0是所有帖子 用动态sql userid为0时不拼 不为0时拼 为标签是<if>
    //考虑分页 offset是每一页起始页的行号，limit是每一页最多显示多少条数据
    List<DiscussPost> selectDiscussPosts(int userId,int offset,int limit);
    //查询出表里一共有多少条数据 Param是给参数取别名
    //需要写动态sql且只有一个参数时 必须写别名否则报错
    int selectDiscussPostRows(@Param("userId") int userId);


    int insertDiscussPost(DiscussPost discussPost);

    DiscussPost selectDiscussPostById(int id);

    //更新帖子评论数量
    int updateCommentCount(int id, int commentCount);

}
