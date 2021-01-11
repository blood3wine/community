package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {
    @Autowired
    private KafkaTemplate kafkaTemplate;

    //处理事件
    public void fireEvent(Event event){
        //将事件发布到指定的主题

        //Message是事件中所有数据，最好方法是把event转换成json
        //消费者得到json字符串后还原成event
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }

}
