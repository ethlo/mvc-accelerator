package com.ethlo.mvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.AntPathMatcher;

public class InterceptorRequestMatcher {
    private final String pattern;
    private final AntPathMatcher matcher = new AntPathMatcher();
    public static final InterceptorRequestMatcher MATCH_ALL = new InterceptorRequestMatcher("/**");

    public InterceptorRequestMatcher(String pattern) {
        this.pattern = pattern;
    }

    public boolean matches(HttpServletRequest request) {
        return matcher.match(pattern, request.getRequestURI());
    }

    @Override
    public String toString() {
        return pattern;
    }
}
