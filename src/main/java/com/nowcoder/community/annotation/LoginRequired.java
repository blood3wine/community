package com.nowcoder.community.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//这个注解写在方法之上，可以描述方法
@Target(ElementType.METHOD)
//运行时有效
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequired {



}
