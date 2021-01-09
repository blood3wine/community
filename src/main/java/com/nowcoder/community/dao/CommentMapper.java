package com.nowcoder.community.dao;


import com.nowcoder.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {
    //查询帖子评论/评论的评论/课程评论
    //offset和limit是分页条件
    List<Comment> selectCommentsByEntity(int entityType,int entityId,int offset,int limit);

    List<Comment> selectCommentsByUserId(int entityType,int userId,int offset,int limit);

    //查询数据条目数
    int selectCountByEntity(int entityType,int entityId);

    //增加评论
    int insertComment(Comment comment);

    //根据用户id 查发了多少评论
    int selectCountByUserId(int entityType,int userId);
}
