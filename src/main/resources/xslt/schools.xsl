<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:import href="federal-common.xsl"/>
    <xsl:import href="school-form.xsl"/>

    <xsl:output method="html"
                encoding="UTF-8"
                omit-xml-declaration="yes"
                doctype-system="about:legacy-compat"/>

    <xsl:param name="view"/>

    <xsl:template match="/">
        <xsl:choose>
            <xsl:when test="$view = 'list-fragment'">
                <xsl:call-template name="schools-results"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="page"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="page-title">
        <xsl:choose>
            <xsl:when test="$view = 'create'">
                <xsl:text>Новая школа</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>Школы</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="content">
        <xsl:choose>
            <xsl:when test="$view = 'create'">
                <xsl:call-template name="create"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="schools"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="styles">
        <xsl:choose>
            <xsl:when test="$view = 'create'">
                <link rel="stylesheet" href="/css/school/school.css"/>
            </xsl:when>
            <xsl:otherwise>
                <link rel="stylesheet" href="/css/school/schools.css"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="scripts">
        <xsl:if test="$view = 'list'">
            <script type="module" src="/js/school/schools.js"></script>
        </xsl:if>
    </xsl:template>

    <xsl:template name="schools">
        <div class="page-title-row">
            <div>
                <h1>Школы</h1>
                <p class="page-subtitle">Управление школами в системе</p>
            </div>
            <a class="btn btn-primary schools-add"
               href="/schools/create"
               title="Добавить школу в систему">
                <svg xmlns="http://www.w3.org/2000/svg"
                     fill="none"
                     stroke="currentColor"
                     stroke-width="2"
                     viewBox="0 0 24 24">
                    <line x1="12" x2="12" y1="5" y2="19"/>
                    <line x1="5" x2="19" y1="12" y2="12"/>
                </svg>
                <xsl:text>Добавить школу</xsl:text>
            </a>
        </div>
        <div class="schools-content">
            <div class="search-wrap schools-search">
                <svg xmlns="http://www.w3.org/2000/svg"
                     fill="none"
                     stroke="currentColor"
                     stroke-width="2"
                     viewBox="0 0 24 24">
                    <circle cx="11" cy="11" r="8"/>
                    <line x1="21" x2="16.65" y1="21" y2="16.65"/>
                </svg>
                <label for="school-search">
                    <input id="school-search"
                           type="text"
                           placeholder="Поиск школы...">
                        <xsl:attribute name="value">
                            <xsl:value-of select="/surok/schools/filters/filter[name = 'name']/value"/>
                        </xsl:attribute>
                    </input>
                </label>
            </div>
            <xsl:call-template name="schools-results"/>
        </div>
    </xsl:template>

    <xsl:template name="schools-results">
        <div class="schools-results"
             id="schools-results">
            <div class="schools-sort-controls">
                <xsl:call-template name="school-sort">
                    <xsl:with-param name="field" select="'name'"/>
                    <xsl:with-param name="label" select="'Название'"/>
                    <xsl:with-param name="title"
                                    select="'Сортировать по названию'"/>
                </xsl:call-template>
            </div>
            <div class="card-grid" id="schools-container">
                <xsl:if test="not(/surok/schools/school)">
                    <div class="schools-empty">
                        <xsl:text>Школы не найдены.</xsl:text>
                    </div>
                </xsl:if>
                <xsl:for-each select="/surok/schools/school">
                    <div class="card-item school-row school-card">
                        <xsl:attribute name="data-id">
                            <xsl:value-of select="@id"/>
                        </xsl:attribute>
                        <div class="card-item-header">
                            <div class="card-item-title-row">
                                <div class="card-item-icon">
                                    <svg xmlns="http://www.w3.org/2000/svg"
                                         fill="none"
                                         stroke="currentColor"
                                         stroke-width="2"
                                         viewBox="0 0 24 24">
                                        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                                        <polyline points="9 22 9 12 15 12 15 22"/>
                                    </svg>
                                </div>
                            </div>
                        </div>
                        <div class="card-item-period">
                            <span class="card-item-name">
                                <xsl:value-of select="name"/>
                            </span>
                            <div class="card-item-period-label">ID: <xsl:value-of select="@id"/></div>
                        </div>
                    </div>
                </xsl:for-each>
            </div>
            <xsl:call-template name="pagination">
                <xsl:with-param name="page"
                                select="/surok/schools"/>
                <xsl:with-param name="prefix"
                                select="'schools'"/>
                <xsl:with-param name="ellipsis" select="'…'"/>
            </xsl:call-template>
        </div>
    </xsl:template>

    <xsl:template name="school-sort">
        <xsl:param name="field"/>
        <xsl:param name="label"/>
        <xsl:param name="title"/>
        <xsl:variable name="direction"
                      select="/surok/schools/sorts/sort[name = $field]/direction"/>
        <button type="button">
            <xsl:attribute name="class">
                <xsl:text>schools-sort</xsl:text>
                <xsl:if test="$direction = 'ASC' or $direction = 'DESC'">
                    <xsl:text> active</xsl:text>
                </xsl:if>
            </xsl:attribute>
            <xsl:attribute name="title"><xsl:value-of select="$title"/></xsl:attribute>
            <xsl:attribute name="data-sort"><xsl:value-of select="$field"/></xsl:attribute>
            <span><xsl:value-of select="$label"/></span>
            <span class="schools-sort-icon">
                <xsl:choose>
                    <xsl:when test="$direction = 'ASC'">↑</xsl:when>
                    <xsl:when test="$direction = 'DESC'">↓</xsl:when>
                    <xsl:otherwise>↕</xsl:otherwise>
                </xsl:choose>
            </span>
        </button>
    </xsl:template>

    <xsl:template name="create">
        <xsl:call-template name="school-form">
            <xsl:with-param name="editing" select="false()"/>
        </xsl:call-template>
    </xsl:template>
</xsl:stylesheet>
