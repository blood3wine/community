package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    //存到redis 注入RedisTemplate
    @Autowired
    private RedisTemplate redisTemplate;


    // 点赞
    //entityId是点赞的人 entityUserId是被赞的人
    public void like(int userId, int entityType, int entityId,int entityUserId) {

        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                //这里是被赞的人
                String entityUserKey=RedisKeyUtil.getUserLikeKey(entityUserId);

                //这是查询 一定要放在事务过程之外
                boolean isMember=operations.opsForSet().isMember(entityLikeKey,userId);
                operations.multi();

                //如果已经赞了
                if(isMember){
                    operations.opsForSet().remove(entityLikeKey,userId);
                    //这是普通字符串 用-1就行了
                    operations.opsForValue().decrement(entityUserKey);
                }else{
                    operations.opsForSet().add(entityLikeKey,userId);
                    //这是普通字符串 用+1就行了
                    operations.opsForValue().increment(entityUserKey);
                }


                return operations.exec();
            }
        });
    }

    // 查询某实体点赞的数量
    public long findEntityLikeCount(int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    // 查询某人对某实体的点赞状态
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        //0无 1点赞 -1踩【以后再说】
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }

    //查询某个用户获得的赞数量
    public int findUserLikeCount(int userId){
        String entityUserKey=RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer)redisTemplate.opsForValue().get(entityUserKey);

        //不为null显示数据的整数形式
        return count==null?0:count.intValue();

    }

}
