package com.ethlo.mvc.fastpath;

import com.ethlo.mvc.MvcAccelerator;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.util.pattern.PathPattern;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EntryParser {

    private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{([^/]+?)\\}");

    public static List<FastEntry> parseEntry(
            final HandlerMethod handlerMethod,
            final RequestMappingInfo requestMappingInfo) {

        final PathPatternsRequestCondition patternsCondition = requestMappingInfo.getPathPatternsCondition();
        if (patternsCondition == null || patternsCondition.getPatterns().isEmpty()) {
            return Collections.emptyList();
        }

        // --- GATHER ALL COMBINATIONS ---

        // 1. Get all patterns
        final Set<PathPattern> patterns = patternsCondition.getPatterns();

        // 2. Get all HTTP methods. If none, it matches all methods.
        Set<RequestMethod> httpMethods = getRequestMethods(requestMappingInfo);

        // 3. Get all producible media types. If none, treat as a single "any" entry (null).
        final ProducesRequestCondition producesCondition = requestMappingInfo.getProducesCondition();
        Set<MediaType> producibleMediaTypes = new HashSet<>();
        if (producesCondition != null && !producesCondition.getProducibleMediaTypes().isEmpty()) {
            producibleMediaTypes.addAll(producesCondition.getProducibleMediaTypes());
        } else {
            // Add a single null entry to represent "any" media type
            producibleMediaTypes.add(null);
        }

        // Get common properties once
        final MvcAccelerator mvcAccelerator = handlerMethod.getMethodAnnotation(MvcAccelerator.class);
        final int order = determineOrder(mvcAccelerator);
        final boolean shortCircuit = determineShortCircuit(mvcAccelerator);

        // --- FLATTEN ALL COMBINATIONS USING NESTED STREAMS ---
        return patterns.stream()
                .flatMap(pattern -> httpMethods.stream()
                        .flatMap(method -> producibleMediaTypes.stream()
                                .map(mediaType -> {
                                    // For each unique combination, create a FastEntry
                                    final String patternString = pattern.getPatternString();
                                    final int firstVarIndex = patternString.indexOf('{');
                                    final String pathPrefix = (firstVarIndex >= 0) ? patternString.substring(0, firstVarIndex) : patternString;
                                    final List<String> variableNames = new ArrayList<>();
                                    final Matcher matcher = PATH_VARIABLE_PATTERN.matcher(patternString);
                                    while (matcher.find()) {
                                        variableNames.add(matcher.group(1));
                                    }

                                    return new FastEntry(
                                            requestMappingInfo,
                                            patternString,
                                            method, // Each entry is for a single method
                                            pathPrefix,
                                            variableNames,
                                            mediaType, // Each entry is for a single media type
                                            handlerMethod,
                                            order,
                                            shortCircuit
                                            );
                                })
                        )
                )
                .collect(Collectors.toList());
    }

    private static boolean determineShortCircuit(MvcAccelerator mvcAccelerator) {
        return mvcAccelerator != null ? mvcAccelerator.shortCircuit() : false;
    }

    private static Set<RequestMethod> getRequestMethods(RequestMappingInfo requestMappingInfo) {
        final Set<RequestMethod> httpMethods = requestMappingInfo.getMethodsCondition().getMethods();
        if (httpMethods.isEmpty()) {
            return EnumSet.allOf(RequestMethod.class);
        }
        return httpMethods;
    }

    private static int determineOrder(MvcAccelerator mvcAccelerator) {
        return mvcAccelerator != null ? mvcAccelerator.order() : 1;
    }
}