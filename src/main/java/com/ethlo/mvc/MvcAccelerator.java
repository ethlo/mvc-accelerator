package com.ethlo.mvc;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MvcAccelerator {
    int order() default 0;

    boolean shortCircuit() default false;

    // TODO: Consider supporting per endpoint filter config
    //Class<? extends Filter>[] includedFilters() default {};
    //Class<? extends Filter>[] excludedFilters() default {};
}