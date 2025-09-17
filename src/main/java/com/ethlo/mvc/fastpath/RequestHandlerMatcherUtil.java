package com.ethlo.mvc.fastpath;

import com.ethlo.mvc.MvcAccelerator;
import com.ethlo.mvc.MvcAcceleratorConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.PathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.util.ArrayList;
import java.util.List;

import static com.ethlo.mvc.MvcAcceleratorConfig.Mode.ALL;

public class RequestHandlerMatcherUtil {
    public static Object getHandlerInternal(MvcAcceleratorConfig.Mode mode, HttpServletRequest request, List<FastEntry> entries, PathMatcher pathMatcher) {
        String requestPath = request.getServletPath();
        List<Match> potentialMatches = new ArrayList<>();

        // Filter candidates and find all valid matches
        for (FastEntry entry : entries) {
            final MvcAccelerator mvcAccelerator = entry.mvcAccelerator();
            if (mode == ALL || mvcAccelerator != null) {
                // Quick prefix check to eliminate most non-matches
                if (requestPath.startsWith(entry.pathPrefix())) {
                    // Perform the full, correct match against the request
                    RequestMappingInfo matchingCondition = entry.requestMappingInfo().getMatchingCondition(request);

                    // A non-null result means it's a valid match
                    if (matchingCondition != null) {
                        final Match match = new Match(matchingCondition, entry.handlerMethod(), entry.pattern());
                        if (entry.shortCircuit()) {
                            return handleBestMatch(request, pathMatcher, match, requestPath);
                        } else {
                            potentialMatches.add(match);
                        }
                    }
                }
            }
        }

        if (potentialMatches.isEmpty()) {
            return null; // No handlers matched
        }

        // Sort the valid matches to find the single best one
        if (potentialMatches.size() > 1) {
            potentialMatches.sort((match1, match2) ->
                    match1.requestMappingInfo().compareTo(match2.requestMappingInfo(), request)
            );
        }

        // The best match is the first element after sorting
        Match bestMatch = potentialMatches.get(0);
        return handleBestMatch(request, pathMatcher, bestMatch, requestPath);
    }

    private static HandlerExecutionChain handleBestMatch(HttpServletRequest request, PathMatcher pathMatcher, Match bestMatch, String requestPath) {
        final HandlerMethod handler = bestMatch.handlerMethod();
        final String bestPattern = bestMatch.pattern();
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, bestPattern);
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                pathMatcher.extractUriTemplateVariables(bestPattern, requestPath));


        return new HandlerExecutionChain(handler);
    }

    /**
     * A private helper class to hold a successful match, containing the more specific
     * RequestMappingInfo returned by getMatchingCondition.
     */
    private record Match(RequestMappingInfo requestMappingInfo, HandlerMethod handlerMethod, String pattern) {
    }
}
