package com.nowcoder.community.dao.elasticsearch;

import com.nowcoder.community.entity.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

//Spring提供对数据访问层的注解                                              声明实体类  实体类主键类型
@Repository
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost,Integer> {
}
