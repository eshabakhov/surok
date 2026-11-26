/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.OaYaml;
import com.eshabakhov.surok.security.Operations;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Current application actor adapted to UserDetails and OAuth2User.
 *
 * <p>Implements both {@link UserDetails} and {@link OAuth2User} so that
 * {@code @AuthenticationPrincipal UrAcAuth} resolves correctly regardless
 * of whether the user signed in via form-login or Yandex OAuth2.</p>
 * Usage example:
 * <pre>
 * final UrAccount user = new UrAcAuth(dbUser, operations);
 * user.uid();           // always available
 * user.permit("Buildings.create", school).schools().school(school.uid()).buildings();
 * </pre>
 *
 * @since 0.0.1
 */
public final class UrAcAuth implements UserDetails, OAuth2User, UrAccount {

    /** User. */
    private final UrAccount user;

    /** Application operations, without cached permission decisions. */
    private final Operations operations;

    /**
     * New Spring principal backed by a registered account.
     *
     * @param user User
     * @throws IOException If the operation catalog cannot be read
     */
    public UrAcAuth(final UrAccount user) throws IOException {
        this(user, new OaYaml().operations());
    }

    /**
     * New actor with an explicitly supplied operation catalog.
     *
     * @param user Account backing this actor
     * @param operations Application operations
     */
    public UrAcAuth(final UrAccount user, final Operations operations) {
        this.user = user;
        this.operations = operations;
    }

    @Override
    public UrAccount permit(final String operation) {
        return this.operations.operation(operation).permission(this).permit(this);
    }

    @Override
    public UrAccount permit(final String operation, final School school) {
        return this.operations.operation(operation).permission(this, school).permit(this);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.user.roles().list().stream()
            .map(role -> new SimpleGrantedAuthority(String.format("ROLE_%s", role.name())))
            .toList();
    }

    @Override
    public String getPassword() {
        return this.user.account().password();
    }

    @Override
    public String getUsername() {
        return this.user.account().username();
    }

    @Override
    public boolean isEnabled() {
        return this.user.account().alive();
    }

    @Override
    public String getName() {
        return this.user.account().username();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of();
    }

    @Override
    public long uid() {
        return this.user.uid();
    }

    @Override
    public Account account() {
        return this.user.account();
    }

    @Override
    public Roles roles() {
        return this.user.roles();
    }

    @Override
    public Schools schools() {
        return this.user.schools();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.user.print(media);
    }
}
