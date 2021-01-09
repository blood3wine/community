package com.nowcoder.community.service;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {
    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;

    public void follow(int userId,int entityType,int entityId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey= RedisKeyUtil.getFolloweeKey(userId,entityType);
                String followerKey=RedisKeyUtil.getFollowerKey(entityType,entityId);

                //在这中间做两次存储的 操作
                operations.multi();
                //有序集合  userId是当前用户，entityId是那个实体的id
                // followee:userId:entityType -> zset(entityId,now)
                operations.opsForZSet().add(followeeKey,entityId,System.currentTimeMillis());
                //// follower:entityType:entityId -> zset(userId,now)
                operations.opsForZSet().add(followerKey,userId,System.currentTimeMillis());


                return operations.exec();
            }
        });
    }

    public void unfollow(int userId,int entityType,int entityId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey= RedisKeyUtil.getFolloweeKey(userId,entityType);
                String followerKey=RedisKeyUtil.getFollowerKey(entityType,entityId);

                //在这中间做两次存储的 操作
                operations.multi();
                operations.opsForZSet().remove(followeeKey,entityId);
                operations.opsForZSet().remove(followerKey,userId);

                return operations.exec();
            }
        });
    }
    //查询某用户关注的实体的数量
    public long findFolloweeCount(int userId,int entityType){
        String followeeKey= RedisKeyUtil.getFolloweeKey(userId,entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    //查询某实体的粉丝的数量
    public long findFollowerCount(int entityType,int entityId){
        String followerKey=RedisKeyUtil.getFollowerKey(entityType,entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    //查询当前用户是否已关注该实体
    public boolean hasFollowed(int userId,int entityType,int entityId){
        String followeeKey= RedisKeyUtil.getFolloweeKey(userId,entityType);
        return redisTemplate.opsForZSet().score(followeeKey,entityId)!=null;
    }

    //查询某个用户关注的人[User对象和关注时间整合
    public List<Map<String,Object>> findFollowees(int userId,int offset,int limit){
        String followeeKey= RedisKeyUtil.getFolloweeKey(userId,ENTITY_TYPE_USER);
        Set<Integer> targetIds=redisTemplate.opsForZSet().reverseRange(followeeKey,offset,offset+limit-1);

        if(targetIds==null){
            return null;
        }
        List<Map<String,Object>> list=new ArrayList<>();
        //根据id查到对应用户封装到map里
        for(Integer targetId:targetIds){
            Map<String,Object> map=new HashMap<>();
            User user=userService.findUserById(targetId);
            map.put("user",user);
            //查targetId对应的分数
            Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
            map.put("followTime",new Date(score.longValue()));
            list.add(map);
        }

        return list;
    }

    //查询某用户的粉丝
    public List<Map<String,Object>> findFollowers(int userId,int offset,int limit){
        String followerKey=RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER,userId);
        Set<Integer> targetIds=redisTemplate.opsForZSet().reverseRange(followerKey,offset,offset+limit-1);
        if(targetIds==null){
            return null;
        }
        List<Map<String,Object>> list=new ArrayList<>();
        for(Integer targetId:targetIds){
            Map<String,Object> map=new HashMap<>();
            User user=userService.findUserById(targetId);
            map.put("user",user);
            Double score = redisTemplate.opsForZSet().score(followerKey,targetId);
            map.put("followTime",new Date(score.longValue()));
            list.add(map);
        }

        return list;
    }

}
