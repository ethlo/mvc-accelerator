package com.ethlo.mvc.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.web.access.intercept.RequestMatcherDelegatingAuthorizationManager;
import org.springframework.security.web.util.matcher.RequestMatcherEntry;

import java.util.List;

public class CachedMatcherPostProcessor implements ObjectPostProcessor<Object> {

    private static final Logger logger = LoggerFactory.getLogger(CachedMatcherPostProcessor.class);

    @Override
    public <O> O postProcess(O object) {
        if (object instanceof RequestMatcherDelegatingAuthorizationManager authorizationManager) {
            final List<RequestMatcherEntry<?>> mappings = RequestMatcherHelper.getMappings(authorizationManager);
            final List<? extends RequestMatcherEntry<?>> replacement = mappings.stream().map(requestMatcherEntry ->
            {
                if (requestMatcherEntry.getRequestMatcher() instanceof CachedPathPatternRequestMatcher) {
                    // Avoid double cache layers
                    return requestMatcherEntry;
                }
                return new RequestMatcherEntry<>(new CachedPathPatternRequestMatcher(requestMatcherEntry.getRequestMatcher()), requestMatcherEntry.getEntry());
            }).toList();
            mappings.clear();
            mappings.addAll(replacement);
            logger.info("Wrapped {} with cache", replacement.size());
        }
        return object;
    }
}
