/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

/**
 * Unauthenticated account of a visitor without login credentials.
 * For example, {@code new AcAnonymous().alive()} cannot enable a visitor for password login.
 *
 * @since 0.0.1
 */
public final class AcAnonymous implements Account {

    /** Visitor description for failures to access account credentials. */
    private final String visitor;

    /**
     * New account of an anonymous visitor.
     */
    public AcAnonymous() {
        this("anonymous visitor");
    }

    /**
     * New anonymous account with a description of its visitor.
     *
     * @param visitor Visitor description
     */
    public AcAnonymous(final String visitor) {
        this.visitor = visitor;
    }

    @Override
    public boolean authenticated() {
        return false;
    }

    @Override
    public String username() {
        throw new IllegalStateException(String.format("The %s has no username", this.visitor));
    }

    @Override
    public String password() {
        throw new IllegalStateException(String.format("The %s has no password", this.visitor));
    }

    @Override
    public boolean corporate() {
        return false;
    }

    @Override
    public boolean alive() {
        return false;
    }
}
