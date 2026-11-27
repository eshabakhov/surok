<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:import href="web-common.xsl"/>
    <xsl:output method="html" encoding="UTF-8" omit-xml-declaration="yes" doctype-system="about:legacy-compat"/>
    <xsl:param name="view"/>
    <xsl:template match="/"><xsl:call-template name="page"/></xsl:template>
    <xsl:template name="page-title">Подписка</xsl:template>
    <xsl:template name="styles"><link rel="stylesheet" href="/css/user/subscription.css"/></xsl:template>
    <xsl:variable name="plan" select="/surok/subscription/plan"/>
    <xsl:template name="content">
<div>
    <xsl:call-template name="in-development-banner" />
    <div class="sub-wrap">
        <div class="page-title-row">
            <div>
                <h1>Подписка</h1>
                <p class="page-subtitle">Управление тарифным планом</p>
            </div>
        </div>
        <xsl:if test="/surok/activated = 'true'">
            <div class="sub-alert ok">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                    <polyline points="22 4 12 14.01 9 11.01" />
                </svg>
            Подписка активирована на 30 дней. Роли обновлены.
        </div>
        </xsl:if>
        <xsl:if test="/surok/cancelled = 'true'">
            <div class="sub-alert ok">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                    <polyline points="22 4 12 14.01 9 11.01" />
                </svg>
            Подписка отменена. Роли сброшены до набора Базового плана.
        </div>
        </xsl:if>
        <xsl:if test="/surok/error = 'true'">
            <div class="sub-alert err">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="15" x2="9" y1="9" y2="15" />
                    <line x1="9" x2="15" y1="9" y2="15" />
                </svg>
            Произошла ошибка. Попробуйте ещё раз.
        </div>
        </xsl:if>
        <div class="sub-banner">
            <div class="sub-banner-left">
                <div>
                    <xsl:attribute name="class">sub-icon <xsl:value-of select="/surok/subscription/@style" />
                    </xsl:attribute>
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
                    </svg>
                </div>
                <div>
                    <div class="sub-name">
                        <xsl:value-of select="/surok/subscription/label" />
                    </div>
                    <div class="sub-label">Текущий тарифный план</div>
                </div>
            </div>
            <xsl:if test="$plan = 'BASIC'">
                <span class="sub-chip chip-free">
                Бессрочно · Бесплатно
            </span>
            </xsl:if>
            <xsl:if test="$plan != 'BASIC' and /surok/subscription/valid = 'true'">
                <span class="sub-chip chip-ok">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <circle cx="12" cy="12" r="10" />
                        <polyline points="12 6 12 12 16 14" />
                    </svg>
                Активна до <span>
                        <xsl:value-of select="/surok/subscription/expiry" />
                    </span>
                </span>
            </xsl:if>
            <xsl:if test="$plan != 'BASIC' and /surok/subscription/valid != 'true'">
                <span class="sub-chip chip-warn">Истекла</span>
            </xsl:if>
        </div>
        <div class="plans-title">Доступные планы</div>
        <div class="plans-grid">
            <div>
                <xsl:attribute name="class"><xsl:text>plan-card</xsl:text><xsl:choose>
                        <xsl:when test="$plan = 'BASIC'"><xsl:text> is-current</xsl:text></xsl:when>
                    </xsl:choose>
                </xsl:attribute>
                <xsl:if test="$plan = 'BASIC'">
                    <span class="plan-pill pill-curr">Текущий</span>
                </xsl:if>
                <div>
                    <div class="plan-card-name">Базовый</div>
                    <div class="plan-card-price">
                        <strong>0 ₽</strong> / бессрочно</div>
                </div>
                <div class="plan-features">
                    <div class="plan-feat">
                        <svg xmlns="http://www.w3.org/2000/svg" class="feat-ok" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                            <polyline points="20 6 9 17 4 12" />
                        </svg>
                        Просмотр расписания
                    </div>
                    <div class="plan-feat">
                        <svg xmlns="http://www.w3.org/2000/svg" class="feat-ok" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                            <polyline points="20 6 9 17 4 12" />
                        </svg>
                        Доступ к учебному плану
                    </div>
                    <div class="plan-feat">
                        <svg xmlns="http://www.w3.org/2000/svg" class="feat-ok" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                            <polyline points="20 6 9 17 4 12" />
                        </svg>
                        До 1 расписания
                    </div>
                </div>
            </div>
            <div>
                <xsl:attribute name="class"><xsl:text>plan-card</xsl:text><xsl:choose>
                        <xsl:when test="$plan = 'ADVANCED'"><xsl:text> is-current</xsl:text></xsl:when>
                    </xsl:choose>
                </xsl:attribute>
                <xsl:if test="$plan = 'ADVANCED'">
                    <span class="plan-pill pill-curr">Текущий</span>
                </xsl:if>
                <div>
                    <div class="plan-card-name">Продвинутый</div>
                    <div class="plan-card-price">
                        <strong>0 ₽</strong> / мес</div>
                </div>
                <div class="plan-features">
                    <div class="plan-feat">
                        <svg xmlns="http://www.w3.org/2000/svg" class="feat-ok" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                            <polyline points="20 6 9 17 4 12" />
                        </svg>
                        Всё из Базового
                    </div>
                    <div class="plan-feat">
                        <svg xmlns="http://www.w3.org/2000/svg" class="feat-ok" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                            <polyline points="20 6 9 17 4 12" />
                        </svg>
                        Создание расписаний
                    </div>
                    <div class="plan-feat">
                        <svg xmlns="http://www.w3.org/2000/svg" class="feat-ok" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                            <polyline points="20 6 9 17 4 12" />
                        </svg>
                        Управление учебным планом
                    </div>
                    <div class="plan-feat">
                        <svg xmlns="http://www.w3.org/2000/svg" class="feat-ok" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                            <polyline points="20 6 9 17 4 12" />
                        </svg>
                        До 5 расписаний
                    </div>
                    <div class="plan-feat soon">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" class="plan-soon-icon">
                            <circle cx="12" cy="12" r="10" />
                            <line x1="12" x2="12" y1="8" y2="12" />
                            <line x1="12" x2="12.01" y1="16" y2="16" />
                        </svg>
                        Экспорт PDF - скоро
                    </div>
                </div>
                <xsl:if test="$plan != 'ADVANCED'">
                    <div class="plan-card-actions">
                        <form method="post" action="/users/subscription/checkout">
                            <xsl:call-template name="csrf-input" />
                            <input name="plan" type="hidden" value="ADVANCED" />
                            <button class="btn-plan-checkout" type="submit">Перейти</button>
                        </form>
                        <p class="plan-stub-note">Тестовый режим - 30 дней без списания</p>
                    </div>
                </xsl:if>
            </div>
            <div>
                <xsl:attribute name="class"><xsl:text>plan-card</xsl:text><xsl:choose>
                        <xsl:when test="$plan = 'PRO'"><xsl:text> is-current</xsl:text></xsl:when>
                        <xsl:otherwise><xsl:text> is-popular</xsl:text></xsl:otherwise>
                    </xsl:choose>
                </xsl:attribute>
                <xsl:if test="$plan != 'PRO'">
                    <span class="plan-pill pill-pop">Популярный</span>
                </xsl:if>
                <xsl:if test="$plan = 'PRO'">
                    <span class="plan-pill pill-curr">Текущий</span>
                </xsl:if>
                <div>
                    <div class="plan-card-name">Профессиональный</div>
                    <div class="plan-card-price">
                        <strong>0 ₽</strong> / мес</div>
                </div>
                <div class="plan-features">
                    <div class="plan-feat">
                        <svg xmlns="http://www.w3.org/2000/svg" class="feat-ok" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                            <polyline points="20 6 9 17 4 12" />
                        </svg>
                        Всё из Продвинутого
                    </div>
                    <div class="plan-feat">
                        <svg xmlns="http://www.w3.org/2000/svg" class="feat-ok" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                            <polyline points="20 6 9 17 4 12" />
                        </svg>
                        Неограниченные расписания
                    </div>
                    <div class="plan-feat">
                        <svg xmlns="http://www.w3.org/2000/svg" class="feat-ok" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                            <polyline points="20 6 9 17 4 12" />
                        </svg>
                        Расширенный учебный план
                    </div>
                    <div class="plan-feat">
                        <svg xmlns="http://www.w3.org/2000/svg" class="feat-ok" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                            <polyline points="20 6 9 17 4 12" />
                        </svg>
                        Будущие отчёты
                    </div>
                    <div class="plan-feat">
                        <svg xmlns="http://www.w3.org/2000/svg" class="feat-ok" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                            <polyline points="20 6 9 17 4 12" />
                        </svg>
                        Приоритетная поддержка
                    </div>
                    <div class="plan-feat soon">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" class="plan-soon-icon">
                            <circle cx="12" cy="12" r="10" />
                            <line x1="12" x2="12" y1="8" y2="12" />
                            <line x1="12" x2="12.01" y1="16" y2="16" />
                        </svg>
                        API-доступ - скоро
                    </div>
                </div>
                <xsl:if test="$plan != 'PRO'">
                    <div class="plan-card-actions">
                        <form method="post" action="/users/subscription/checkout">
                            <xsl:call-template name="csrf-input" />
                            <input name="plan" type="hidden" value="PRO" />
                            <button class="btn-plan-checkout" type="submit">Перейти</button>
                        </form>
                        <p class="plan-stub-note">Тестовый режим - 30 дней без списания</p>
                    </div>
                </xsl:if>
            </div>
            <div>
                <xsl:attribute name="class"><xsl:text>plan-card</xsl:text><xsl:choose>
                        <xsl:when test="$plan = 'VIEWER'"><xsl:text> is-current</xsl:text></xsl:when>
                    </xsl:choose>
                </xsl:attribute>
                <xsl:if test="$plan = 'VIEWER'">
                    <span class="plan-pill pill-curr">Текущий</span>
                </xsl:if>
                <div>
                    <div class="plan-card-name">Просмотрщик</div>
                    <div class="plan-card-price">
                        <strong>0 ₽</strong> / мес</div>
                </div>
                <div class="plan-features">
                    <div class="plan-feat">
                        <svg xmlns="http://www.w3.org/2000/svg" class="feat-ok" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                            <polyline points="20 6 9 17 4 12" />
                        </svg>
                        Просмотр готового расписания
                    </div>
                    <div class="plan-feat">
                        <svg xmlns="http://www.w3.org/2000/svg" class="feat-ok" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                            <polyline points="20 6 9 17 4 12" />
                        </svg>
                        Фильтрация по классу и учителю
                    </div>
                    <div class="plan-feat soon">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" class="plan-soon-icon">
                            <circle cx="12" cy="12" r="10" />
                            <line x1="12" x2="12" y1="8" y2="12" />
                            <line x1="12" x2="12.01" y1="16" y2="16" />
                        </svg>
                        Уведомления - скоро
                    </div>
                </div>
                <xsl:if test="$plan != 'VIEWER'">
                    <div class="plan-card-actions">
                        <form method="post" action="/users/subscription/checkout">
                            <xsl:call-template name="csrf-input" />
                            <input name="plan" type="hidden" value="VIEWER" />
                            <button class="btn-plan-checkout" type="submit">Перейти</button>
                        </form>
                        <p class="plan-stub-note">Тестовый режим - 30 дней без списания</p>
                    </div>
                </xsl:if>
            </div>
        </div>
        <xsl:if test="$plan != 'BASIC'">
            <div class="cancel-card">
                <div>
                    <div class="cancel-title">Отменить подписку</div>
                    <div class="cancel-desc">Переход на Базовый план. Роли пересчитаются автоматически.</div>
                </div>
                <form method="post" action="/users/subscription/cancel" id="subscription-cancel">
                    <xsl:call-template name="csrf-input" />
                    <button class="btn-cancel-sub" type="submit">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <circle cx="12" cy="12" r="10" />
                            <line x1="15" x2="9" y1="9" y2="15" />
                            <line x1="9" x2="15" y1="9" y2="15" />
                        </svg>
                    Отменить подписку
                </button>
                </form>
            </div>
        </xsl:if>
    </div>
</div>

    </xsl:template>
    <xsl:template name="scripts"><script type="module" src="/js/user/subscription.js"></script></xsl:template>
</xsl:stylesheet>
