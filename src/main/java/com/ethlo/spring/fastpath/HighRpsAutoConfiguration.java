package com.ethlo.spring.fastpath;

import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AutoConfiguration
public class HighRpsAutoConfiguration {

    @Bean
    public HighRpsHandlerMapping highRpsHandlerMapping(
            ApplicationContext ctx,
            List<HandlerInterceptor> interceptors) {

        List<FastEntry> entries = new ArrayList<>();

        // Scan all controllers
        Map<String, Object> beans = new HashMap<>();
        beans.putAll(ctx.getBeansWithAnnotation(org.springframework.stereotype.Controller.class));
        beans.putAll(ctx.getBeansWithAnnotation(org.springframework.web.bind.annotation.RestController.class));

        for (Object bean : beans.values()) {
            Class<?> clazz = AopUtils.getTargetClass(bean);
            RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(clazz, RequestMapping.class);
            String classPath = classMapping != null && classMapping.path().length > 0 ? classMapping.path()[0] : "";

            for (var method : clazz.getMethods()) {
                HighRps highRps = method.getAnnotation(HighRps.class);
                if (highRps != null) {
                    var requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                    if (requestMapping != null && requestMapping.path().length > 0) {
                        for (String methodPath : requestMapping.path()) {
                            String fullPath = classPath + methodPath; // combine class + method path
                            HandlerMethod hm = new HandlerMethod(bean, method);

                            // Compute static prefix and variable names
                            int firstVar = fullPath.indexOf('{');
                            String prefix = firstVar >= 0 ? fullPath.substring(0, firstVar) : fullPath;

                            List<String> varNames = new ArrayList<>();
                            int idx = 0;
                            while ((idx = fullPath.indexOf('{', idx)) >= 0) {
                                int end = fullPath.indexOf('}', idx);
                                if (end < 0) break;
                                varNames.add(fullPath.substring(idx + 1, end));
                                idx = end + 1;
                            }

                            entries.add(new FastEntry(prefix, varNames, MediaType.parseMediaType(highRps.produces()), hm));
                        }
                    }
                }
            }
        }

        return new HighRpsHandlerMapping(entries, interceptors);
    }
}
