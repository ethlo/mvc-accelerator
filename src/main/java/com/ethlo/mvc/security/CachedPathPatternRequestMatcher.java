package com.ethlo.mvc.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.util.ServletRequestPathUtils;

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

public final class CachedPathPatternRequestMatcher implements RequestMatcher {
    private static final String ATTR = CachedPathPatternRequestMatcher.class.getName() + ".PATH_CONTAINER";

    private final RequestMatcher delegate;

    /**
     * Wrap any RequestMatcher
     */
    public CachedPathPatternRequestMatcher(RequestMatcher delegate) {
        this.delegate = delegate;
    }

    /**
     * Build a delegate matcher from method + pattern
     */
    public CachedPathPatternRequestMatcher(HttpMethod method, String pattern) {
        this.delegate = PathPatternRequestMatcher.withDefaults().matcher(method, pattern);
    }

    /**
     * Build a delegate matcher from pattern only
     */
    public CachedPathPatternRequestMatcher(String pattern) {
        this.delegate = PathPatternRequestMatcher.withDefaults().matcher(pattern);
    }

    @Override
    public MatchResult matcher(HttpServletRequest request) {
        // Ensure the RequestPath and subPath are cached
        getCachedPathContainer(request);
        return delegate.matcher(request);
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return matcher(request).isMatch();
    }

    private PathContainer getCachedPathContainer(HttpServletRequest request) {
        PathContainer cached = (PathContainer) request.getAttribute(ATTR);
        if (cached != null)
        {
            return cached;
        }

        RequestPath path = ServletRequestPathUtils.hasParsedRequestPath(request)
                ? ServletRequestPathUtils.getParsedRequestPath(request)
                : ServletRequestPathUtils.parseAndCache(request);

        PathContainer contextPath = path.contextPath();
        PathContainer sub = path.subPath(contextPath.elements().size());

        request.setAttribute(ATTR, sub);
        return sub;
    }

    @Override
    public String toString() {
        return "Cached(" + delegate + ")";
    }
}
