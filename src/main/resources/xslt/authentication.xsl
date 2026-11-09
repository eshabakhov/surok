<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:import href="web-common.xsl"/>
    <xsl:output method="html" encoding="UTF-8" omit-xml-declaration="yes" doctype-system="about:legacy-compat"/>
    <xsl:param name="view"/>
    <xsl:template match="/"><xsl:call-template name="page"/></xsl:template>
    <xsl:template name="page-title">
        <xsl:choose><xsl:when test="$view = 'register'">Surok - Регистрация</xsl:when><xsl:otherwise>Surok - Вход</xsl:otherwise></xsl:choose>
    </xsl:template>
    <xsl:template name="page-description">
        <xsl:choose><xsl:when test="$view = 'register'">Регистрация на платформе</xsl:when><xsl:otherwise>Вход на платформу</xsl:otherwise></xsl:choose>
    </xsl:template>
    <xsl:template name="canonical"><link rel="canonical" href="https://surok.com/users/{$view}"/></xsl:template>
    <xsl:template name="styles"><link rel="stylesheet" href="/css/login.css"/></xsl:template>
    <xsl:template name="page-body">
        <xsl:choose><xsl:when test="$view = 'register'"><xsl:call-template name="register"/></xsl:when><xsl:otherwise><xsl:call-template name="login"/></xsl:otherwise></xsl:choose>
        <xsl:call-template name="web-scripts"/>
    </xsl:template>
    <xsl:template name="scripts"><script type="module" src="/js/authentication.js"></script></xsl:template>
    <xsl:template name="login">
    <main>
        <div class="page-container">
            <div class="login-card">
                <div class="card-header">
                    <div class="logo-wrap">
                        <xsl:call-template name="brand-mark"/>
                    </div>
                    <h1 class="card-title">Surok - Вход</h1>
                    <div class="card-desc">Платформа управления школьными расписаниями</div>
                </div>
                <a class="btn-yandex" href="/oauth2/authorization/yandex" title="Авторизоваться в системе через Яндекс">
                    <svg fill="none"
                         height="44"
                         viewBox="0 0 44 44"
                         width="44"
                         xmlns="http://www.w3.org/2000/svg">
                        <rect fill="#FC3F1D" height="44" rx="22" width="44"></rect>
                        <path d="M25.2438 12.3208H23.0173C19.2005 12.3208 17.292 14.2292 17.292 17.0919C17.292 20.2726 18.5643 21.863 21.427 23.7714L23.6535 25.3618L17.292 35.222H12.2029L18.2463 26.316C14.7475 23.7714 12.839 21.5449 12.839 17.41C12.839 12.3208 16.3378 8.82202 23.0173 8.82202H29.6969V35.222H25.2438V12.3208Z"
                              fill="white">
                        </path>
                    </svg>
                    Войти через Яндекс
                </a>
                <xsl:if test="/surok/error = 'oauth_no_email'"><div class="alert error">
                    Яндекс не передал email. Попробуйте войти по логину.
                </div></xsl:if>
                <div class="divider"><span>или</span></div>
                <form class="login-form" method="post" action="/login">
                    <xsl:call-template name="csrf-input"/>
                    <div class="field">
                        <label for="username">Логин</label>
                        <input id="username"
                               name="username"
                               type="text"
                               autofocus="autofocus"
                               placeholder="Введите логин"
                               required="required"/>
                    </div>
                    <div class="field">
                        <label for="password">Пароль</label>
                        <div class="input-wrap">
                            <input id="password"
                                   name="password"
                                   type="password"
                                   placeholder="••••••••"
                                   required="required"/>
                            <button class="eye-btn" type="button" aria-label="Показать пароль" data-target="password">
                                <svg class="eye-icon eye-off"
                                     fill="none"
                                     stroke="currentColor"
                                     stroke-linecap="round"
                                     stroke-linejoin="round"
                                     stroke-width="2"
                                     viewBox="0 0 24 24"
                                     xmlns="http://www.w3.org/2000/svg">
                                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                                    <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                                    <line x1="1" x2="23" y1="1" y2="23"/>
                                </svg>
                                <svg class="eye-icon eye-on is-hidden"
                                     fill="none"
                                     stroke="currentColor"
                                     stroke-linecap="round"
                                     stroke-linejoin="round"
                                     stroke-width="2"
                                     
                                     viewBox="0 0 24 24"
                                     xmlns="http://www.w3.org/2000/svg">
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                                    <circle cx="12" cy="12" r="3"/>
                                </svg>
                            </button>
                        </div>
                    </div>
                    <button class="btn-submit" type="submit">Войти</button>
                </form>
                <xsl:if test="/surok/error = 'bad_credentials'"><div class="alert error">
                    Неверный логин или пароль
                </div></xsl:if>
                <xsl:if test="/surok/error = 'user_not_found'"><div class="alert error">
                    Пользователь не найден
                </div></xsl:if>
                <xsl:if test="/surok/logout = 'true'"><div class="alert success">
                    Вы вышли из системы
                </div></xsl:if>
                <xsl:if test="/surok/registered = 'true'"><div class="alert success">
                    Регистрация успешна! Войдите в систему.
                </div></xsl:if>

                <div class="divider"><span>или</span></div>
                <div class="register-link">
                    Нет аккаунта?
                    <a href="/users/register" title="Зарегистрироваться в системе">
                        Зарегистрироваться
                    </a>
                </div>
            </div>
        </div>
    </main>

    </xsl:template>
    <xsl:template name="register">
    <main>
        <div class="page-container">
            <div class="login-card">
                <div class="card-header">
                    <div class="logo-wrap">
                        <xsl:call-template name="brand-mark"/>
                    </div>
                    <h1 class="card-title">Surok - Регистрация</h1>
                    <div class="card-desc">Создайте аккаунт для доступа к платформе</div>
                </div>
                <form class="login-form" id="register-form" method="post" action="/users">
                    <xsl:call-template name="csrf-input"/>
                    <div class="field">
                        <label for="username">Логин</label>
                        <input id="username"
                               name="username" value="{/surok/username}"
                               type="text"
                               autofocus="autofocus"
                               minlength="3"
                               placeholder="Минимум 3 символа"
                               required="required"/>
                    </div>
                    <div class="field">
                        <label for="email">Email</label>
                        <input id="email"
                               name="email" value="{/surok/email}"
                               type="email"
                               placeholder="email@example.com"
                               required="required"/>
                    </div>
                    <div class="field">
                        <label for="password">Пароль</label>
                        <div class="input-wrap">
                            <input id="password"
                                   name="password"
                                   type="password"
                                   minlength="8"
                                   placeholder="••••••••"
                                   required="required"/>
                            <button class="eye-btn"
                                    type="button"
                                    aria-label="Показать пароль"
                                    data-target="password">
                                <svg class="eye-icon eye-off"
                                     fill="none"
                                     stroke="currentColor"
                                     stroke-linecap="round"
                                     stroke-linejoin="round"
                                     stroke-width="2"
                                     viewBox="0 0 24 24"
                                     xmlns="http://www.w3.org/2000/svg">
                                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                                    <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                                    <line x1="1" x2="23" y1="1" y2="23"/>
                                </svg>
                                <svg class="eye-icon eye-on is-hidden"
                                     fill="none"
                                     stroke="currentColor"
                                     stroke-linecap="round"
                                     stroke-linejoin="round"
                                     stroke-width="2"
                                     
                                     viewBox="0 0 24 24"
                                     xmlns="http://www.w3.org/2000/svg">
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                                    <circle cx="12" cy="12" r="3"/>
                                </svg>
                            </button>
                        </div>
                    </div>
                    <div class="field">
                        <label for="confirm-password">Подтверждение пароля</label>
                        <div class="input-wrap">
                            <input id="confirm-password"
                                   name="confirmPassword"
                                   type="password"
                                   minlength="8"
                                   placeholder="••••••••"
                                   required="required"/>
                            <button class="eye-btn"
                                    type="button"
                                    aria-label="Показать пароль"
                                    data-target="confirm-password">
                                <svg class="eye-icon eye-off"
                                     fill="none"
                                     stroke="currentColor"
                                     stroke-linecap="round"
                                     stroke-linejoin="round"
                                     stroke-width="2"
                                     viewBox="0 0 24 24"
                                     xmlns="http://www.w3.org/2000/svg">
                                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                                    <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                                    <line x1="1" x2="23" y1="1" y2="23"/>
                                </svg>
                                <svg class="eye-icon eye-on is-hidden"
                                     fill="none"
                                     stroke="currentColor"
                                     stroke-linecap="round"
                                     stroke-linejoin="round"
                                     stroke-width="2"
                                     
                                     viewBox="0 0 24 24"
                                     xmlns="http://www.w3.org/2000/svg">
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                                    <circle cx="12" cy="12" r="3"/>
                                </svg>
                            </button>
                        </div>
                    </div>
                    <button class="btn-submit" type="submit">Зарегистрироваться</button>
                </form>
                <div class="alert error" id="error-message" role="alert">
                    <xsl:if test="not(/surok/message) or /surok/message = ''"><xsl:attribute name="hidden">hidden</xsl:attribute></xsl:if>
                    <xsl:value-of select="/surok/message"/>
                </div>
                <div class="divider"><span>или</span></div>
                <div class="register-link">
                    Уже есть аккаунт?
                    <a href="/users/login" title="Войти в систему">
                        Войти
                    </a>
                </div>
            </div>
        </div>
    </main>

    </xsl:template>
</xsl:stylesheet>
