# MVC Accelerator

**High-performance routing for Spring MVC / Spring Boot**

Spring MVC Accelerator provides a minimally invasive way to dramatically improve throughput for high-traffic endpoints.  
It introduces a *fast path* through Spring MVC and Spring Security, removing unnecessary overhead while preserving compatibility.

## Why

Most applications only have a handful of truly performance-critical endpoints, but the full Spring MVC + Security stack applies equally to every request.  
For lightweight endpoints, this overhead dominates request cost. MVC Accelerator addresses this by:

- Prioritizing hot endpoints
- Skipping unnecessary filter-chain steps
- Avoiding repeated path parsing

The result: **up to 5–10x higher throughput** for high-frequency APIs such as highly cached or small payload ingestion end-points.

---

## Features

- **`@MvcAccelerator` annotation**  
  Mark high-throughput endpoints to bypass the normal DispatcherServlet and take the *fast path*.

- **Fast-path filter chain**  
  Configure a minimal subset of Spring Security / custom filters to run before your handler.  
  All other filters are skipped for fast endpoints, while the correct execution order is preserved.

- **Configurable via `application.properties`**  
  Enable/disable features, define included filters by FQN, and choose whether to fail-fast if required filters are missing.

- **`CachedPathPatternRequestMatcher`**  
  Drop-in replacement for Spring Security’s `PathPatternRequestMatcher` that caches results per-request.  
  Eliminates repeated parsing when many patterns or actuator endpoints are active.  
  ⚡ Not auto-wired; explicitly use it in your security config.

- **Spring Boot starter**  
  Plug-and-play with minimal application changes.

---

## Configuration

```properties
# Enable fast-path for endpoints annotated with @MvcAccelerator
mvc.accelerator.fast-path.enabled=true

# Enable fast filter-chain mode
mvc.accelerator.fast-filter-chain.enabled=true

# Fail startup if any required filter is missing (default: true → halt starup, otherwise log warning)
mvc.accelerator.fast-filter-chain.fail-if-filter-missing=true

# Include all filters
mvc.accelerator.fast-filter-chain.included-filters=*

# Fully qualified class names of filters to include in the fast-path
mvc.accelerator.fast-filter-chain.included-filters=\
  org.springframework.boot.web.servlet.filter.OrderedRequestContextFilter,\
  org.springframework.security.web.authentication.www.BasicAuthenticationFilter,\
  org.springframework.security.web.access.ExceptionTranslationFilter
