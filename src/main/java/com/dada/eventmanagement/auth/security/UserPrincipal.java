package com.dada.eventmanagement.auth.security;

import com.dada.eventmanagement.common.enums.Role;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class UserPrincipal implements UserDetails {
    private final Long id;
    private final Long companyId;
    private final String fullName;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean active;

    public UserPrincipal(Long id, Long companyId, String fullName, String email, String passwordHash, Role role, boolean active) {
        this.id = id;
        this.companyId = companyId;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
