package com.ethlo.mvc.security;

import org.springframework.lang.Nullable;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.List;
import java.util.function.Supplier;

public class DelegatingAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final List<AuthorizationManager<RequestAuthorizationContext>> delegates;

    public DelegatingAuthorizationManager(List<AuthorizationManager<RequestAuthorizationContext>> delegates) {
        this.delegates = delegates;
    }

    @Nullable
    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        for (AuthorizationManager<RequestAuthorizationContext> delegate : delegates) {
            AuthorizationDecision decision = delegate.check(authentication, context);
            if (decision != null && decision.isGranted()) {
                return decision; // short-circuit: allow if any delegate allows
            }
        }
        return null;
    }
}
