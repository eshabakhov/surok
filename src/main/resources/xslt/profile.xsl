<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:import href="school-common.xsl"/>
    <xsl:output method="html" encoding="UTF-8" omit-xml-declaration="yes" doctype-system="about:legacy-compat"/>
    <xsl:param name="view"/>
    <xsl:template match="/"><xsl:call-template name="page"/></xsl:template>
    <xsl:template name="page-title">Профиль</xsl:template>
    <xsl:template name="styles"><link rel="stylesheet" href="/css/user/profile.css"/></xsl:template>
    <xsl:template name="content">
<div>
    <div class="page-title-row profile-heading">
        <div>
            <h1>Профиль</h1>
            <p class="page-subtitle">Информация об аккаунте</p>
        </div>
    </div>
    <div class="profile-cards">
        <div class="card">
            <div class="card-body profile-identity">
                <div class="profile-avatar">
                    <span>
                        <xsl:value-of select="/surok/account/initial" />
                    </span>
                </div>
                <div class="profile-caption">
                    <div class="profile-username">
                        <xsl:value-of select="/surok/account/username" />
                    </div>
                    <div class="profile-hint">
                        ID: <span class="profile-id">
                            <xsl:value-of select="/surok/account/id" />
                        </span>
                    </div>
                </div>
            </div>
        </div>
        <div class="card">
            <div class="card-header">
                <div class="card-title">Данные аккаунта</div>
            </div>
            <div class="card-body profile-data">
                <div class="info-row profile-row">
                    <span class="info-label">Логин</span>
                    <span class="info-value">
                        <xsl:value-of select="/surok/account/username" />
                    </span>
                </div>
                <div class="info-row profile-row">
                    <span class="info-label">ID</span>
                    <span class="info-value td-mono">
                        <xsl:value-of select="/surok/account/id" />
                    </span>
                </div>
                <xsl:if test="/surok/memberships/membership">
                    <div class="info-row profile-row">
                        <span class="info-label">Школы</span>
                        <div>
                            <xsl:for-each select="/surok/memberships/membership">
                                <div class="profile-membership">
                                    <a class="info-value profile-school" href="/schools/{school/id}" title="Перейти к школе">
                                        <xsl:value-of select="school/name" />
                                    </a>
                                    <xsl:for-each select="roles/role">
                                        <span class="badge badge-secondary">
                                            <xsl:value-of select="name" />
                                        </span>
                                    </xsl:for-each>
                                    <xsl:for-each select="schedules/schedule">
                                        <a class="info-value profile-school" href="/schools/{../../school/id}/schedules/{id}">
                                            <xsl:value-of select="name" />
                                        </a>
                                    </xsl:for-each>
                                </div>
                            </xsl:for-each>
                        </div>
                    </div>
                </xsl:if>
                <div class="info-row">
                    <span class="info-label">Роли</span>
                    <div class="profile-roles">
                        <xsl:for-each select="/surok/account/roles/role">
                            <span class="badge badge-secondary">
                                <xsl:value-of select="name" />
                            </span>
                        </xsl:for-each>
                        <xsl:if test="not(/surok/account/roles/role)">
                            <span class="badge badge-outline">Нет ролей</span>
                        </xsl:if>
                    </div>
                </div>
            </div>
        </div>
        <div class="card">
            <div class="card-body">
                <form method="post" action="/logout" class="profile-logout">
                    <xsl:call-template name="csrf-input" />
                    <div>
                        <div class="profile-logout-title">Выйти из системы</div>
                        <div class="profile-hint">
                            Завершить текущую сессию
                        </div>
                    </div>
                    <button class="logout-button" type="submit">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" class="profile-logout-icon">
                            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                            <polyline points="16 17 21 12 16 7" />
                            <line x1="21" x2="9" y1="12" y2="12" />
                        </svg>
                        Выйти
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>

    </xsl:template>
</xsl:stylesheet>
