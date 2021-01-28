package com.nowcoder.community.config;

import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {

    @Override
    public void configure(WebSecurity web) throws Exception {
        //忽略resource下所有的资源（忽略对静态资源的访问 提高性能
        web.ignoring().antMatchers("/resources/**");
    }


    // 绕过认证


    // 进行授权
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/discuss/add",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "/follow",
                        "/unfollow"
                )
                .hasAnyAuthority(
                        AUTHORITY_USER,
                        AUTHORITY_ADMIN,
                        AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                )
                .hasAnyAuthority(
                        AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/delete",
                        "/data/**"
                )
                .hasAnyAuthority(
                        AUTHORITY_ADMIN
                )
                .anyRequest().permitAll()
                .and().csrf().disable();

        //权限不够时的处理
        http.exceptionHandling()
                // 没登录的处理
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        String xRequestedWith=request.getHeader("x-requested-with");
                        //是异步请求，期待返回xml（现在被json代替）
                        if("XMLHttpRequest".equals(xRequestedWith)){
                            response.setContentType("application/plain;charset=utf-8");
                            // 通过response获得字符流writer向前台输出内容
                            PrintWriter writer=response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403,"你还没有登录"));
                        }else{
                            //不是异步 直接重定向
                            response.sendRedirect(request.getContextPath()+"/login");
                        }
                    }
                })
                // 登录了但权限不足的处理
                .accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestedWith=request.getHeader("x-requested-with");
                        //是异步请求，期待返回xml（现在被json代替）
                        if("XMLHttpRequest".equals(xRequestedWith)){
                            response.setContentType("application/plain;charset=utf-8");
                            // 通过response获得字符流writer向前台输出内容
                            PrintWriter writer=response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403,"你没有访问此功能的权限"));
                        }else{
                            //不是异步 直接重定向
                            response.sendRedirect(request.getContextPath()+"/denied");
                        }
                    }
                });

        // Security底层默认会拦截/logout请求,进行退出处理.
        // 基于filter 肯定在controller之前
        // 覆盖它默认的逻辑,才能执行我们自己的退出代码.
        // 善意的欺骗【不是
        http.logout().logoutUrl("/securitylogout");

    }
}
