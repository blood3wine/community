package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.jws.soap.SOAPBinding;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class MessageInterceptor implements HandlerInterceptor {
    //查当前用户消息数量

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private MessageService messageService;

    //调controller之后，模板之前
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user= hostHolder.getUser();
        if(user!=null&&modelAndView!=null){
            int letterUnreadCount=messageService.findLetterUnreadCount(user.getId(), null);
            int noticeUnreadCount=messageService.findNoticeUnreadCount(user.getId(), null);
            modelAndView.addObject("allUnreadCount",letterUnreadCount+noticeUnreadCount);
        }
    }
}
