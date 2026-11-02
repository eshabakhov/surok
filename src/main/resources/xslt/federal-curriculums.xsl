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
        <xsl:choose>
            <xsl:when test="$view = 'list-fragment'">
                <xsl:call-template name="curriculums-results"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="page"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="page-title">
        <xsl:choose>
            <xsl:when test="$view = 'create'">
                <xsl:text>Новый федеральный учебный план</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>Федеральные учебные планы</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="content">
        <xsl:choose>
            <xsl:when test="$view = 'create'">
                <xsl:call-template name="create"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="curriculums"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="styles">
        <xsl:choose>
            <xsl:when test="$view = 'create'">
                <link rel="stylesheet" href="/css/federal/curriculum.css"/>
            </xsl:when>
            <xsl:otherwise>
                <link rel="stylesheet" href="/css/federal/curriculums.css"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="scripts">
        <xsl:if test="$view = 'list'">
            <script type="module" src="/js/federal/curriculums.js"></script>
        </xsl:if>
    </xsl:template>

    <xsl:template name="curriculums">
        <div class="page-title-row">
            <div>
                <h1>Федеральные учебные планы</h1>
                <p class="page-subtitle">Справочник нормативных учебных планов для школ</p>
            </div>
            <xsl:if test="/surok/user/admin = 'true'">
                <a class="btn btn-primary federal-curriculums-add"
                   href="/federal/curriculums/create"
                   title="Добавить федеральный учебный план в систему">
                    <svg xmlns="http://www.w3.org/2000/svg"
                         fill="none"
                         stroke="currentColor"
                         stroke-width="2"
                         viewBox="0 0 24 24">
                        <line x1="12" x2="12" y1="5" y2="19"/>
                        <line x1="5" x2="19" y1="12" y2="12"/>
                    </svg>
                    <xsl:text>Добавить план</xsl:text>
                </a>
            </xsl:if>
        </div>
        <div class="federal-curriculums-content">
            <div class="search-wrap federal-curriculums-search">
                <svg xmlns="http://www.w3.org/2000/svg"
                     fill="none"
                     stroke="currentColor"
                     stroke-width="2"
                     viewBox="0 0 24 24">
                    <circle cx="11" cy="11" r="8"/>
                    <line x1="21" x2="16.65" y1="21" y2="16.65"/>
                </svg>
                <label for="federal-curriculum-search">
                    <input id="federal-curriculum-search"
                           type="text"
                           placeholder="Поиск плана...">
                        <xsl:attribute name="value">
                            <xsl:value-of select="/surok/curriculums/filters/item[name = 'title']/value"/>
                        </xsl:attribute>
                    </input>
                </label>
            </div>
            <xsl:call-template name="curriculums-results"/>
        </div>
    </xsl:template>

    <xsl:template name="curriculums-results">
        <div class="federal-curriculums-results"
             id="federal-curriculums-results">
            <div class="federal-curriculums-sort-controls">
                <xsl:call-template name="curriculum-sort">
                    <xsl:with-param name="field" select="'title'"/>
                    <xsl:with-param name="label" select="'Название'"/>
                    <xsl:with-param name="title"
                                    select="'Сортировать по названию'"/>
                </xsl:call-template>
                <xsl:call-template name="curriculum-sort">
                    <xsl:with-param name="field" select="'level'"/>
                    <xsl:with-param name="label" select="'Уровень'"/>
                    <xsl:with-param name="title"
                                    select="'Сортировать по уровню образования'"/>
                </xsl:call-template>
                <xsl:call-template name="curriculum-sort">
                    <xsl:with-param name="field" select="'week'"/>
                    <xsl:with-param name="label" select="'Неделя'"/>
                    <xsl:with-param name="title"
                                    select="'Сортировать по учебной неделе'"/>
                </xsl:call-template>
                <xsl:call-template name="curriculum-sort">
                    <xsl:with-param name="field" select="'year'"/>
                    <xsl:with-param name="label" select="'Учебный год'"/>
                    <xsl:with-param name="title"
                                    select="'Сортировать по учебному году'"/>
                </xsl:call-template>
            </div>
            <div class="card-grid" id="federal-curriculums-container">
                <xsl:if test="not(/surok/curriculums/curriculum)">
                    <div class="federal-curriculums-empty">
                        <xsl:text>Федеральные учебные планы не найдены.</xsl:text>
                    </div>
                </xsl:if>
                <xsl:for-each select="/surok/curriculums/curriculum">
                    <div class="card-item federal-curriculum-row federal-curriculum-card">
                        <xsl:attribute name="data-id">
                            <xsl:value-of select="id"/>
                        </xsl:attribute>
                        <div class="card-item-header">
                            <div class="card-item-title-row">
                                <div class="card-item-icon">
                                    <svg xmlns="http://www.w3.org/2000/svg"
                                         fill="none"
                                         stroke="currentColor"
                                         stroke-width="2"
                                         viewBox="0 0 24 24">
                                        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
                                        <path d="M4 4.5A2.5 2.5 0 0 1 6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5z"/>
                                    </svg>
                                </div>
                            </div>
                        </div>
                        <div class="card-item-period">
                            <span class="card-item-name">
                                <xsl:value-of select="title"/>
                            </span>
                            <div class="card-item-period-label">
                                <span><xsl:value-of select="/surok/levels/level[code = current()/level]/title"/></span>
                                <xsl:text> · </xsl:text>
                                <span><xsl:value-of select="/surok/weeks/week[code = current()/week]/title"/></span>
                                <xsl:text> · </xsl:text>
                                <span><xsl:value-of select="year"/></span>
                            </div>
                        </div>
                    </div>
                </xsl:for-each>
            </div>
            <xsl:call-template name="pagination">
                <xsl:with-param name="page"
                                select="/surok/curriculums/page"/>
                <xsl:with-param name="prefix"
                                select="'federal-curriculums'"/>
                <xsl:with-param name="ellipsis" select="'…'"/>
            </xsl:call-template>
        </div>
    </xsl:template>

    <xsl:template name="curriculum-sort">
        <xsl:param name="field"/>
        <xsl:param name="label"/>
        <xsl:param name="title"/>
        <xsl:variable name="direction"
                      select="/surok/curriculums/sorts/item[name = $field]/direction"/>
        <button type="button">
            <xsl:attribute name="class">
                <xsl:text>federal-curriculums-sort</xsl:text>
                <xsl:if test="$direction = 'ASC' or $direction = 'DESC'">
                    <xsl:text> active</xsl:text>
                </xsl:if>
            </xsl:attribute>
            <xsl:attribute name="title"><xsl:value-of select="$title"/></xsl:attribute>
            <xsl:attribute name="data-sort"><xsl:value-of select="$field"/></xsl:attribute>
            <span><xsl:value-of select="$label"/></span>
            <span class="federal-curriculums-sort-icon">
                <xsl:choose>
                    <xsl:when test="$direction = 'ASC'">↑</xsl:when>
                    <xsl:when test="$direction = 'DESC'">↓</xsl:when>
                    <xsl:otherwise>↕</xsl:otherwise>
                </xsl:choose>
            </span>
        </button>
    </xsl:template>

    <xsl:template name="create">
        <xsl:call-template name="curriculum-form">
            <xsl:with-param name="editing" select="false()"/>
        </xsl:call-template>
    </xsl:template>
</xsl:stylesheet>
