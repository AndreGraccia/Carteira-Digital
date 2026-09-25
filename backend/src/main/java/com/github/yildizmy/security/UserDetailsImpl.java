package com.github.yildizmy.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.yildizmy.domain.entity.User;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@EqualsAndHashCode(of = {"username"})
public class UserDetailsImpl implements UserDetails {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    @JsonIgnore
    private String password;

    private String firstName;

    private String lastName;

    private Collection<? extends GrantedAuthority> authorities;

    /** Tokens issued before this are revoked; null when never revoked. */
    private java.time.Instant tokensValidAfter;

    public UserDetailsImpl(Long id, String username, String password, String firstName, String lastName,
                           Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.authorities = authorities;
    }

    public static UserDetailsImpl build(User user) {
        final List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getType().name()))
                .toList();

        UserDetailsImpl details = new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getFirstName(),
                user.getLastName(),
                authorities);
        details.setTokensValidAfter(user.getTokensValidAfter());
        return details;
    }

    void setTokensValidAfter(java.time.Instant tokensValidAfter) {
        this.tokensValidAfter = tokensValidAfter;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
