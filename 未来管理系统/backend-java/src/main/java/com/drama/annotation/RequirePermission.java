package com.drama.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /** 需要的权限代码，如 "drama:delete" */
    String value();

    /** 权限描述，用于错误提示 */
    String description() default "";
}
