# 🚀 Spring MVC Accelerator

A minimally invasive library that dramatically improves throughput for your high-traffic Spring Boot endpoints.

---

While the full Spring stack is robust, its overhead can dominate the cost of simple, frequently-hit API calls. This library introduces an optimized **fast path** to reduce this overhead for the specific endpoints you choose, preserving compatibility while boosting performance.

It achieves this by:
* **Prioritizing hot endpoints** using a simple annotation.
* **Skipping unnecessary filter-chain steps** for accelerated requests.
* **Avoiding repeated request path parsing** within the security filter chain.

In benchmarked scenarios, this can result in up to **9× higher throughput**. For example, a minimal echo endpoint on the standard Spring path achieved 5,800 requests per second, but the same endpoint annotated with `@MvcAccelerator` reached **53,000 requests per second**.

> **Note:** Actual performance gains will vary depending on endpoint complexity, underlying hardware, and JVM tuning. Gains are most pronounced for simple endpoints where framework overhead is the primary bottleneck; complex logic or heavy I/O operations will see smaller improvements.

## ⚠️ Disclaimer: Use At Your Own Risk

>
> This is a high-performance library that achieves speed by bypassing parts of the standard Spring Framework execution path, including elements of the security filter chain.
>
> This approach can have **unintended side effects** and may expose your application to security vulnerabilities if not configured and tested correctly. You are solely responsible for understanding the implications and **thoroughly testing** its behavior in your specific environment .
>
> This software is provided "as is" with **no warranty** of any kind.

## ✨ Features

This starter provides several key features to accelerate your Spring MVC application with minimal setup.



### Annotation-Driven Fast Path

You can selectively boost performance by marking high-throughput controllers or methods with the **`@MvcAccelerator`** annotation. This allows specific requests to bypass the standard `DispatcherServlet` and take a highly optimized execution path, significantly reducing overhead. This behavior is the default when using `mode: ANNOTATED` in your configuration.

---

### Customizable Filter Chain

For accelerated endpoints, you can define a minimal, high-speed filter chain. By specifying which filters to include in your configuration, you can ensure that only essential logic (like security checks) is executed. All other configured servlet filters are skipped for these requests, reducing latency while preserving the correct execution order of the filters you've chosen.

---

### Optimized Request Matching

The module includes **`CachedPathPatternRequestMatcher`**, a drop-in replacement for Spring Security’s default `PathPatternRequestMatcher`. It caches the results of pattern matching for the duration of a single request, eliminating redundant parsing work. This is especially effective in applications with numerous security patterns or many actuator endpoints.

**Note:** This component is **not** auto-configured. You must explicitly declare it as a bean in your security configuration to activate it.

---

### Simple Integration

As a **Spring Boot starter**, integration is plug-and-play. All features are easily enabled and controlled through your standard `application.yml` or `application.properties` file, allowing you to fine-tune performance without altering your application's core logic.



## ⚙️ Configuration

You can configure the MVC Accelerator in your `application.yml` or `application.properties` file. All properties are under the `mvc.accelerator` prefix.

---

### Properties

| Property                                                   | Type           | Description                                                                                                                                                             | Default       |
| ---------------------------------------------------------- | -------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------- |
| `mvc.accelerator.enabled`                                  | `boolean`      | Globally enables or disables the MVC Accelerator feature.                                                                                                               | `true`        |
| `mvc.accelerator.fast-path.mode`                           | `Enum`         | Sets the operating mode for the fast-path feature. **Possible values:** `ALL` (applies to all requests), `ANNOTATED` (applies only to annotated controllers), `NONE` (disabled). | `ANNOTATED`   |
| `mvc.accelerator.fast-filter-chain.mode`                   | `Enum`         | Sets the operating mode for the fast-filter-chain feature. **Possible values:** `ALL`, `ANNOTATED`, `NONE`.                                                               | `ANNOTATED`   |
| `mvc.accelerator.fast-filter-chain.fail-if-filter-missing` | `boolean`      | If `true`, the application will fail to start if a filter specified in `included-filters` cannot be found.                                                              | `true`        |
| `mvc.accelerator.fast-filter-chain.included-filters`       | `List<String>` | A list of fully qualified class names for Servlet Filters that should be included in the fast-path filter chain.                                                        | `[]`          |

---

### Example `application.yml`

Here is an example configuration that shows different modes and a custom list of filters.

```yaml
mvc:
  accelerator:
    enabled: true
    fast-path:
      # Apply fast-path processing to ALL requests
      mode: ALL
    fast-filter-chain:
      # Only apply the fast-filter-chain to ANNOTATED endpoints
      mode: ANNOTATED
      fail-if-filter-missing: true
      included-filters:
        - com.example.filters.MyCustomLoggingFilter
        - org.springframework.web.filter.ShallowEtagHeaderFilter