package com.ethlo.mvc.security;

import org.springframework.security.web.access.intercept.RequestMatcherDelegatingAuthorizationManager;
import org.springframework.security.web.util.matcher.RequestMatcherEntry;

import java.lang.reflect.Field;
import java.util.List;

@SuppressWarnings("unchecked")
public class RequestMatcherHelper {

    private static final Field field;

    static {
        try {
            field = RequestMatcherDelegatingAuthorizationManager.class.getDeclaredField("mappings");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Cannot find mappings field", e);
        }
        field.setAccessible(true);
    }

    private RequestMatcherHelper() {

    }

    public static List<RequestMatcherEntry<?>> getMappings(RequestMatcherDelegatingAuthorizationManager manager) {
        try {
            return (List<RequestMatcherEntry<?>>) field.get(manager);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access mappings field", e);
        }
    }
}
