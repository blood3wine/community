package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //拦截到的是一个方法
        if(handler instanceof HandlerMethod){
            HandlerMethod handlerMethod= (HandlerMethod) handler;
            Method method=handlerMethod.getMethod();
            //尝试去取这个类型的注解
            LoginRequired loginRequired= method.getAnnotation(LoginRequired.class);
            //需要登录注解 并且 用户未登录
            if(loginRequired!=null&&hostHolder.getUser()==null){
                //利用response去重定向 给爷去登录【x
                response.sendRedirect(request.getContextPath()+"/login");
                return false;
            }

        }
        return true;
    }
}
