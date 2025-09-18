package com.ethlo.mvc.fastpath;

import com.ethlo.mvc.MvcAccelerator;
import com.ethlo.mvc.config.MvcAcceleratorConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

import java.util.List;

public class MvcAcceleratorHandlerMapping extends AbstractHandlerMapping {

    private static final Logger logger = LoggerFactory.getLogger(MvcAcceleratorHandlerMapping.class);
    private final List<FastEntry> entries;
    private final MvcAcceleratorConfig mvcAcceleratorConfig;

    public MvcAcceleratorHandlerMapping(MvcAcceleratorConfig mvcAcceleratorConfig, final List<FastEntry> entries) {
        this.mvcAcceleratorConfig = mvcAcceleratorConfig;
        logger.info("@{} handlers registered for {}", MvcAccelerator.class.getSimpleName(), entries.stream().map(e -> "%s %s".formatted(e.requestMethod(), e.pattern())).toList());
        this.entries = entries;
        setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    @Override
    @Nullable
    protected Object getHandlerInternal(@NonNull HttpServletRequest request) {
        return RequestHandlerMatcherUtil.getHandlerInternal(mvcAcceleratorConfig.getFastPath().getMode(), request, entries, getPathMatcher());
    }
}
