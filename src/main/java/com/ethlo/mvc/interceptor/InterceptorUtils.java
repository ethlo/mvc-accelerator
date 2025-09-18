package com.ethlo.mvc.interceptor;

import com.ethlo.mvc.config.MvcAcceleratorConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;

import java.util.*;
import java.util.Map.Entry;

public final class InterceptorUtils {
    private static final Logger logger = LoggerFactory.getLogger(InterceptorUtils.class);

    private InterceptorUtils() {
    }

    /**
     * Flatten interceptors to a map of matcher -> interceptors
     */
    public static Map<InterceptorRequestMatcher, List<HandlerInterceptor>> mapInterceptorsByMatcher(
            List<HandlerInterceptor> interceptors) {

        Map<InterceptorRequestMatcher, List<HandlerInterceptor>> map = new LinkedHashMap<>();

        for (HandlerInterceptor hi : interceptors) {
            if (hi instanceof MappedInterceptor mi) {
                String[] patterns = mi.getIncludePathPatterns();
                if (patterns != null && patterns.length > 0) {
                    for (String p : patterns) {
                        InterceptorRequestMatcher matcher = new InterceptorRequestMatcher(p);
                        map.computeIfAbsent(matcher, k -> new ArrayList<>()).add(mi.getInterceptor());
                    }
                } else {
                    map.computeIfAbsent(InterceptorRequestMatcher.MATCH_ALL, k -> new ArrayList<>()).add(mi.getInterceptor());
                }
            } else {
                map.computeIfAbsent(InterceptorRequestMatcher.MATCH_ALL, k -> new ArrayList<>()).add(hi);
            }
        }

        return map;
    }

    /**
     * Group interceptors by instance while preserving first-seen order of interceptors
     */
    public static List<Entry<HandlerInterceptor, List<InterceptorRequestMatcher>>> groupMatchersByInterceptorOrdered(
            Map<InterceptorRequestMatcher, List<HandlerInterceptor>> matcherToInterceptorsMap) {

        if (matcherToInterceptorsMap == null) return Collections.emptyList();

        Map<HandlerInterceptor, List<InterceptorRequestMatcher>> ordered = new LinkedHashMap<>();
        for (Entry<InterceptorRequestMatcher, List<HandlerInterceptor>> entry : matcherToInterceptorsMap.entrySet()) {
            InterceptorRequestMatcher matcher = entry.getKey();
            for (HandlerInterceptor hi : entry.getValue()) {
                ordered.computeIfAbsent(hi, k -> new ArrayList<>()).add(matcher);
            }
        }
        return new ArrayList<>(ordered.entrySet());
    }

    /**
     * Return only interceptors whose matchers match the current request
     */
    public static List<Entry<HandlerInterceptor, List<InterceptorRequestMatcher>>> selectInterceptors(
            List<HandlerInterceptor> interceptors, HttpServletRequest request) {

        Map<InterceptorRequestMatcher, List<HandlerInterceptor>> flatMap = mapInterceptorsByMatcher(interceptors);
        Map<HandlerInterceptor, List<InterceptorRequestMatcher>> selected = new LinkedHashMap<>();

        for (Entry<InterceptorRequestMatcher, List<HandlerInterceptor>> entry : flatMap.entrySet()) {
            if (entry.getKey().matches(request)) {
                for (HandlerInterceptor hi : entry.getValue()) {
                    selected.computeIfAbsent(hi, k -> new ArrayList<>()).add(entry.getKey());
                }
            }
        }

        List<Entry<HandlerInterceptor, List<InterceptorRequestMatcher>>> result = new ArrayList<>(selected.entrySet());
        logger.debug("Selected interceptors (in order): {}", result.stream().map(e -> e.getKey().getClass().getName()).toList());
        return result;
    }

    /**
     * Prepare interceptors according to config (like FilterUtils.prepareFilters)
     */
    public static List<Entry<HandlerInterceptor, List<InterceptorRequestMatcher>>> prepareInterceptors(
            MvcAcceleratorConfig config, List<HandlerInterceptor> allInterceptors) {

        List<String> namesToKeep = config.getInterceptors().getIncluded();
        List<Entry<HandlerInterceptor, List<InterceptorRequestMatcher>>> allOrdered =
                groupMatchersByInterceptorOrdered(mapInterceptorsByMatcher(allInterceptors));

        if (namesToKeep.size() == 1 && "*".equals(namesToKeep.get(0))) return allOrdered;

        List<Entry<HandlerInterceptor, List<InterceptorRequestMatcher>>> selectedOrdered = new ArrayList<>();
        for (Entry<HandlerInterceptor, List<InterceptorRequestMatcher>> entry : allOrdered) {
            if (namesToKeep.contains(entry.getKey().getClass().getName())) {
                selectedOrdered.add(entry);
            }
        }

        logger.info("Included interceptors (in order): {}", selectedOrdered.stream().map(e -> e.getKey().getClass().getName()).toList());
        return selectedOrdered;
    }
}
