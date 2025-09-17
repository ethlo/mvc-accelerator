package com.ethlo.mvc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for the MVC Accelerator.
 */
@Configuration
@ConfigurationProperties(prefix = "mvc.accelerator")
public class MvcAcceleratorConfig {

    private final FastPath fastPath = new FastPath();
    private final FilterChain fastFilterChain = new FilterChain();
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public FastPath getFastPath() {
        return fastPath;
    }

    public FilterChain getFastFilterChain() {
        return fastFilterChain;
    }

    public enum Mode {
        ALL, ANNOTATED, NONE
    }

    public static class FastPath extends BaseMode {
    }

    public static class FilterChain extends BaseMode {
        private boolean failIfFilterMissing = true;
        /**
         * Fully qualified class names of filters to include in the fast-path.
         */
        private List<String> includedFilters = List.of();

        public List<String> getIncludedFilters() {
            return includedFilters;
        }

        public void setIncludedFilters(List<String> includedFilters) {
            this.includedFilters = includedFilters;
        }

        public boolean isFailIfFilterMissing() {
            return failIfFilterMissing;
        }

        public FilterChain setFailIfFilterMissing(final boolean failIfFilterMissing) {
            this.failIfFilterMissing = failIfFilterMissing;
            return this;
        }
    }

    public static class BaseMode {
        private Mode mode = Mode.ANNOTATED;

        public Mode getMode() {
            return mode;
        }

        public BaseMode setMode(Mode mode) {
            this.mode = mode;
            return this;
        }
    }
}
