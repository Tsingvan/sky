package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解 用于标识某个方法需要进行字段自动填充处理
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME) //注解不仅被保存到class文件中，jvm加载class文件之后，仍然存在
public @interface AutoFill {
    //数据库操作类型 UPDATA INSERT
    OperationType value();//枚举类的方法 返回 enum 实例的数组，而且该数组中的元素严格保持在 enum 中声明时的顺序
}
