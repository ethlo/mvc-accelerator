# MVC Accelerator

**High-performance routing for Spring MVC / Spring Boot 3.5+**

Spring MVC Accelerator provides a minimally invasive way to dramatically improve throughput for high-traffic endpoints. It focuses on:

- Fast handler resolution for “hot” endpoints
- Caching path parsing for repeated security checks
- Minimal changes to existing Spring Boot applications
- Seamless integration with Spring Security

## Why

Many projects accumulate a large number of endpoints over time, but only a few are truly performance-critical. The Spring MVC stack can be framework-heavy, and the CPU overhead for lightweight endpoints is noticeable. MVC Accelerator addresses this by prioritizing hot paths and reducing repeated computations.

## Features

- `@HighRps` annotation to mark high-throughput endpoints. These endpoints are prioritized when evaluating handler matches.
- `CachedPathPatternRequestMatcher` for per-request caching of path parsing. Useful when many security patterns or actuator endpoints are active, avoiding repeated evaluation.
- Preserves existing Spring Security filters and Spring MVC functionality.
- Fully compatible with Spring Boot 3.5+ and Spring Framework 7.
- Supports standard request mappings and path variables.

## Example Usage

```java
@RestController
@RequestMapping("/demo/performance")
public class PerformanceDemoController {

    @HighRps
    @GetMapping("/fast/{var1}/{var2}")
    public String fastPath(@PathVariable String var1,
                           @PathVariable String var2) {
        return var1 + " - " + var2;
    }

    @GetMapping("/normal/{var1}/{var2}")
    public String normalPath(@PathVariable String var1,
                             @PathVariable String var2) {
        return var1 + " - " + var2;
    }
}