package com.ethlo.mvc.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Arrays;

public class OnPropertyEnumCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        MergedAnnotations annotations = metadata.getAnnotations();
        MergedAnnotation<ConditionalOnEnumProperty> annotation =
                annotations.get(ConditionalOnEnumProperty.class);

        if (!annotation.isPresent()) {
            return false;
        }

        String propertyName = annotation.getString("name");
        MvcAcceleratorConfig.Mode[] accepted = annotation.getEnumArray("havingValues", MvcAcceleratorConfig.Mode.class);

        String rawValue = context.getEnvironment().getProperty(propertyName);
        if (rawValue == null) {
            return false;
        }

        try {
            MvcAcceleratorConfig.Mode mode = MvcAcceleratorConfig.Mode.valueOf(rawValue.trim().toUpperCase());
            return Arrays.asList(accepted).contains(mode);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
