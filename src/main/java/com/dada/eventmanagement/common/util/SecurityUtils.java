package com.dada.eventmanagement.common.util;

import com.dada.eventmanagement.auth.security.UserPrincipal;
import com.dada.eventmanagement.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static UserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Unauthorized");
        }
        return principal;
    }

    public static Long currentCompanyId() {
        return currentUser().getCompanyId();
    }
}
