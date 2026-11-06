/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.Users;
import com.eshabakhov.surok.sql.ClasspathSql;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * PostgreSQL users implementation.
 *
 * @since 0.0.1
 */
public final class UrsPostgres implements Users {

    /** Database context. */
    private final DSLContext ctx;

    /** School ID. */
    private final Long sid;

    /**
     * New users collection.
     *
     * @param ctx Database context
     */
    public UrsPostgres(final DSLContext ctx) {
        this(ctx, null);
    }

    /**
     * New users collection scoped to a school.
     *
     * @param ctx Database context
     * @param sid School identifier
     */
    public UrsPostgres(final DSLContext ctx, final Long sid) {
        this.ctx = ctx;
        this.sid = sid;
    }

    @Override
    public boolean contains(final User user) {
        final boolean found;
        if (this.sid == null) {
            found = this.ctx.resultQuery(
                new ClasspathSql("user/find-by-id.sql").text(), user.uid()
            ).fetchOne() != null;
        } else {
            found = this.ctx.resultQuery(
                new ClasspathSql("school-user/contains.sql").text(), this.sid, user.uid()
            ).fetchOneInto(Boolean.class);
        }
        return found;
    }

    @Override
    public User add(final User user) throws Exception {
        if (this.sid == null) {
            throw new IllegalStateException("A school is required to add a user");
        }
        final Record added = this.ctx.resultQuery(
            new ClasspathSql("school-user/add.sql").text(), this.sid, user.uid()
        ).fetchOne();
        if (added == null) {
            throw new UserNotFoundException("User or school not found");
        }
        return user;
    }

    @Override
    public Roles roles(final User user) {
        final Roles selected;
        if (this.sid == null) {
            selected = user.roles();
        } else {
            selected = new RlsSchool(this.ctx, this.sid, user.uid());
        }
        return selected;
    }

    @Override
    public Iterable<User> iterate() {
        final String sql;
        final Object[] args;
        if (this.sid == null) {
            sql = "user/list.sql";
            args = new Object[0];
        } else {
            sql = "school-user/list.sql";
            args = new Object[]{this.sid};
        }
        return this.ctx.resultQuery(new ClasspathSql(sql).text(), args).fetch(
            row -> new UrPostgres(this.ctx, row.get("id", Long.class))
        );
    }

    @Override
    public User register(
        final String username,
        final String password,
        final String email,
        final Boolean corporate
    ) throws Exception {
        validEmail(email);
        validPassword(password);
        return this.ctx.transactionResult(
            conf -> {
                final DSLContext ttx = conf.dsl();
                final Record inserted = ttx.resultQuery(
                    new ClasspathSql("user/create.sql").text(),
                    username, new BCryptPasswordEncoder().encode(password),
                    email, corporate
                ).fetchOne();
                if (inserted == null) {
                    throw new UserCreationException("Failed to create user");
                }
                final User user = new UrPostgres(ttx, inserted.get("id", Long.class));
                user.roles().grant(Roles.RoleEnum.BASIC_MAKER);
                if (this.sid != null) {
                    final Users members = new UrsPostgres(ttx, this.sid);
                    members.add(user);
                    members.roles(user).grant(Roles.RoleEnum.STUDENT);
                }
                return user;
            }
        );
    }

    @Override
    public User identification(final String login) throws Exception {
        final Record selected = this.ctx.resultQuery(
            new ClasspathSql("user/find-by-login.sql").text(),
            login,
            login
        ).fetchOne();
        if (selected == null) {
            throw new UserNotFoundException(String.format("User with login '%s' not found", login));
        }
        final User found = new UrPostgres(this.ctx, selected.get("id", Long.class));
        if (this.sid != null && !this.contains(found)) {
            throw new UserNotFoundException(String.format("User with login '%s' not found", login));
        }
        return found;
    }

    @Override
    public User identification(final Long uid) throws Exception {
        final Record selected = this.ctx.resultQuery(
            new ClasspathSql("user/find-by-id.sql").text(),
            uid
        ).fetchOne();
        if (selected == null) {
            throw new UserNotFoundException(String.format("User with id '%s' not found", uid));
        }
        final User found = new UrPostgres(this.ctx, selected.get("id", Long.class));
        if (this.sid != null && !this.contains(found)) {
            throw new UserNotFoundException(String.format("User with id '%s' not found", uid));
        }
        return found;
    }

    @Override
    public User authentication(final String login) throws Exception {
        final User user = this.identification(login);
        final OffsetDateTime time = Instant.now().atOffset(ZoneOffset.UTC);
        final Result<Record> selected = this.ctx.resultQuery(
            new ClasspathSql("login-attempt/find-recent-failures.sql").text(),
            user.uid(),
            time.minus(Duration.ofMinutes(5)),
            time,
            false
        ).fetch();
        if (selected.size() > 5) {
            throw new UserLockedException(String.format("User %s is locked", login));
        }
        return user;
    }

    @Override
    public void remove(final Long uid) throws Exception {
        if (this.sid == null) {
            this.ctx.resultQuery(
                new ClasspathSql("user/soft-delete.sql").text(),
                Instant.now().atOffset(ZoneOffset.UTC), uid
            ).execute();
        } else {
            this.ctx.resultQuery(
                new ClasspathSql("school-user/remove.sql").text(), this.sid, uid
            ).execute();
        }
    }

    private static void validEmail(final String email) throws UserCreationException {
        if (email == null) {
            throw new UserCreationException("Получен пустой email");
        }
        if (!email.matches("^[\\w-.]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            throw new UserCreationException("Email введен некорректно");
        }
    }

    private static void validPassword(final String password) throws UserCreationException {
        if (password == null) {
            throw new UserCreationException("Получен пустой пароль");
        }
        if (password.length() < 8) {
            throw new UserCreationException("Длина пароля меньше 8 символов");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            throw new UserCreationException("Пароль должен содержать хотя бы одну цифру");
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            throw new UserCreationException("Пароль должен содержать хотя бы одну строчную букву");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            throw new UserCreationException("Пароль должен содержать хотя бы одну заглавную букву");
        }
        if (password.chars().noneMatch(
            c -> "!@#$%^&*()_+-=[]{};':\"\\|,.<>/?".indexOf(c) >= 0
        )) {
            throw new UserCreationException("Пароль должен содержать хотя бы один спец. символ");
        }
    }
}
