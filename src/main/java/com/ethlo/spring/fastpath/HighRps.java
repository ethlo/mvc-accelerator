package com.ethlo.spring.fastpath;

import org.springframework.http.MediaType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HighRps {
    String produces() default MediaType.APPLICATION_JSON_VALUE;
}