package com.ethlo.mvc.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * A {@link RequestMatcher} wrapper that caches the {@link MatchResult} for a given
 * {@link HttpServletRequest} to avoid repeated path parsing and matching.
 * <p>
 * This is useful for high-throughput endpoints where the underlying matcher (for example,
 * {@link PathPatternRequestMatcher}) is expensive to evaluate multiple times per request
 * due to multiple security filters or other checks.
 * <p>
 * The result of the first {@link #matcher(HttpServletRequest)} invocation is stored as
 * a request attribute, and subsequent calls return the cached {@link MatchResult}.
 * <p>
 * Instances can be created with:
 * <ul>
 *     <li>A custom {@link RequestMatcher} delegate</li>
 *     <li>A path string (defaults to any HTTP handlerMethod)</li>
 *     <li>An HTTP handlerMethod and path string</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * RequestMatcher matcher = new CachedPathPatternRequestMatcher(HttpMethod.GET, "/api/users/{id}");
 * if (matcher.matches(request)) {
 *     // handle matching request
 * }
 * }</pre>
 */
public class CachedPathPatternRequestMatcher implements RequestMatcher {
    private static final String ATTR = CachedPathPatternRequestMatcher.class.getName() + ".MATCH_RESULT";
    private final RequestMatcher delegate;

    public CachedPathPatternRequestMatcher(RequestMatcher delegate) {
        this.delegate = delegate;
    }

    public CachedPathPatternRequestMatcher(final String path) {
        this.delegate = PathPatternRequestMatcher.withDefaults().matcher(path);
    }

    public CachedPathPatternRequestMatcher(HttpMethod method, String path) {
        this.delegate = PathPatternRequestMatcher.withDefaults().matcher(method, path);
    }

    @Override
    public MatchResult matcher(HttpServletRequest request) {
        Object cached = request.getAttribute(ATTR);
        if (cached instanceof MatchResult result) {
            return result;
        }
        MatchResult result = delegate.matcher(request);
        request.setAttribute(ATTR, result);
        return result;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return matcher(request).isMatch();
    }

    @Override
    public String toString() {
        return "Cached" + delegate.toString();
    }
}