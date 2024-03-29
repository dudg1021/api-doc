package com.dudg.apidoc.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 功能模块信息
 * 该注解作用于类/接口上，表明该类代表的功能模块的信息
 * 示例：@Api(name = "测试功能模块")
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Api {
    //模块名称，表示该模块的功能
    String value() default "";
}
