<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:import href="federal-common.xsl"/>
    <xsl:import href="federal-curriculum-form.xsl"/>

    <xsl:output method="html"
                encoding="UTF-8"
                omit-xml-declaration="yes"
                doctype-system="about:legacy-compat"/>

    <xsl:param name="view"/>

    <xsl:template match="/">
        <xsl:call-template name="page"/>
    </xsl:template>

    <xsl:template name="page-title">
        <xsl:choose>
            <xsl:when test="$view = 'edit'">
                <xsl:text>Редактировать федеральный учебный план</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="/surok/curriculums/curriculum/title"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="styles">
        <link rel="stylesheet" href="/css/federal/curriculum.css"/>
    </xsl:template>

    <xsl:template name="content">
        <xsl:choose>
            <xsl:when test="$view = 'edit'">
                <xsl:call-template name="edit"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="details"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="scripts">
        <xsl:if test="$view = 'details'">
            <script type="module" src="/js/federal/curriculum.js"></script>
        </xsl:if>
    </xsl:template>

    <xsl:template name="details">
        <xsl:variable name="curriculum"
                      select="/surok/curriculums/curriculum"/>
        <nav class="breadcrumbs" aria-label="Навигационная цепочка">
            <a class="breadcrumb-link"
               href="/federal/curriculums"
               title="Перейти к федеральным учебным планам">
                <xsl:text>Федеральные учебные планы</xsl:text>
            </a>
            <span class="breadcrumb-separator">-&gt;</span>
            <span class="breadcrumb-current">
                <xsl:value-of select="$curriculum/title"/>
            </span>
        </nav>
        <div class="page-title-row">
            <div>
                <h1 class="title-with-action">
                    <span><xsl:value-of select="$curriculum/title"/></span>
                    <xsl:if test="/surok/user/admin = 'true'">
                        <a class="btn-icon-edit"
                           title="Редактировать"
                           aria-label="Редактировать">
                            <xsl:attribute name="href">
                                <xsl:value-of select="concat('/federal/curriculums/', $curriculum/id, '/edit')"/>
                            </xsl:attribute>
                            <svg xmlns="http://www.w3.org/2000/svg"
                                 fill="none"
                                 height="15"
                                 stroke="currentColor"
                                 stroke-width="2"
                                 viewBox="0 0 24 24"
                                 width="15">
                                <path d="M12 20h9"/>
                                <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/>
                            </svg>
                        </a>
                    </xsl:if>
                </h1>
                <p class="page-subtitle">Информация о федеральном учебном плане</p>
            </div>
        </div>
        <div class="school-info">
            <xsl:call-template name="info-row">
                <xsl:with-param name="label" select="'ID:'"/>
                <xsl:with-param name="value" select="$curriculum/id"/>
            </xsl:call-template>
            <xsl:call-template name="info-row">
                <xsl:with-param name="label" select="'Уровень:'"/>
                <xsl:with-param name="value" select="$curriculum/level"/>
            </xsl:call-template>
            <xsl:call-template name="info-row">
                <xsl:with-param name="label" select="'Неделя:'"/>
                <xsl:with-param name="value" select="$curriculum/week"/>
            </xsl:call-template>
            <xsl:call-template name="info-row">
                <xsl:with-param name="label" select="'Версия:'"/>
                <xsl:with-param name="value" select="$curriculum/version"/>
            </xsl:call-template>
            <xsl:call-template name="info-row">
                <xsl:with-param name="label" select="'Учебный год:'"/>
                <xsl:with-param name="value" select="$curriculum/year"/>
            </xsl:call-template>
            <div class="info-row">
                <span class="info-label">Описание:</span>
                <span class="info-value">
                    <xsl:choose>
                        <xsl:when test="normalize-space($curriculum/description)">
                            <xsl:value-of select="$curriculum/description"/>
                        </xsl:when>
                        <xsl:otherwise>-</xsl:otherwise>
                    </xsl:choose>
                </span>
            </div>
            <div class="info-row">
                <span class="info-label">Требования:</span>
                <span class="info-value">
                    <a class="curriculum-requirements-link"
                       title="Перейти к требованиям учебного плана">
                        <xsl:attribute name="href">
                            <xsl:value-of select="concat('/federal/curriculums/', $curriculum/id, '/requirements')"/>
                        </xsl:attribute>
                        <xsl:text>Перейти к требованиям учебного плана</xsl:text>
                    </a>
                </span>
            </div>
        </div>
        <xsl:if test="/surok/user/admin = 'true'">
            <div class="details-footer-actions">
                <button class="btn-outline-action curriculum-delete btn-delete-curriculum"
                        type="submit">
                    <xsl:attribute name="data-id">
                        <xsl:value-of select="$curriculum/id"/>
                    </xsl:attribute>
                    <xsl:attribute name="data-name">
                        <xsl:value-of select="$curriculum/title"/>
                    </xsl:attribute>
                    <xsl:text>Удалить план</xsl:text>
                </button>
            </div>
        </xsl:if>
    </xsl:template>

    <xsl:template name="info-row">
        <xsl:param name="label"/>
        <xsl:param name="value"/>
        <div class="info-row">
            <span class="info-label"><xsl:value-of select="$label"/></span>
            <span class="info-value"><xsl:value-of select="$value"/></span>
        </div>
    </xsl:template>

    <xsl:template name="edit">
        <xsl:call-template name="curriculum-form">
            <xsl:with-param name="editing" select="true()"/>
        </xsl:call-template>
    </xsl:template>
</xsl:stylesheet>
