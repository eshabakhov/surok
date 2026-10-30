<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:import href="school-common.xsl"/>
    <xsl:output method="html" encoding="UTF-8" omit-xml-declaration="yes"
                doctype-system="about:legacy-compat"/>
    <xsl:param name="view"/>
    <xsl:variable name="schedule" select="/surok/schedule"/>
    <xsl:variable name="entries" select="/surok/loads"/>
    <xsl:variable name="selected" select="$entries/load[$view = 'details']"/>
    <xsl:variable name="base" select="concat($school-path, '/schedules/', $schedule/id, '/loads')"/>

    <xsl:template match="/">
        <xsl:choose>
            <xsl:when test="$view = 'list-fragment'"><xsl:call-template name="results"/></xsl:when>
            <xsl:otherwise><xsl:call-template name="page"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="page-title">
        <xsl:value-of select="concat($schedule/name, ' - Учебная нагрузка')"/>
    </xsl:template>

    <xsl:template name="styles"><link rel="stylesheet" href="/css/school/class-loads.css"/></xsl:template>

    <xsl:template name="content">
        <nav class="breadcrumbs" aria-label="Навигация">
            <a href="{$school-path}/schedules/{$schedule/id}" title="Перейти к расписанию"><xsl:value-of select="$schedule/name"/></a>
            <span>/</span><a href="{$base}" title="Учебная нагрузка">Учебная нагрузка</a>
        </nav>
        <xsl:choose>
            <xsl:when test="$view = 'details'"><xsl:call-template name="details"/></xsl:when>
            <xsl:otherwise>
                <div class="page-title-row">
                    <div><h1>Учебная нагрузка</h1><p class="page-subtitle"><xsl:value-of select="$schedule/name"/></p></div>
                    <a class="btn btn-primary" href="{$base}/draft" title="Настроить обязательную и вариативную нагрузку">Автозаполнение</a>
                </div>
                <p class="page-subtitle">Обязательная нагрузка заполняется по федеральному плану.
                    Дополнительные предметы учитываются в вариативной нагрузке класса;
                    их часы задаются по параллелям в автозаполнении.</p>
                <xsl:call-template name="results"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="results">
        <div id="loads-results">
            <div class="data-table-wrap loads-table-wrap">
                <table class="shadcn-table loads-table">
                    <thead><tr><th scope="col">Класс</th><th scope="col">Предмет</th><th scope="col">Часов/неделю</th></tr></thead>
                    <tbody>
                        <xsl:if test="not($entries/load)"><tr><td class="table-empty" colspan="3">Нагрузка пока не заполнена. Перейдите в автозаполнение.</td></tr></xsl:if>
                        <xsl:for-each select="$entries/load">
                            <tr data-id="{id}">
                                <td><a href="{$base}/{id}" title="Открыть нагрузку"><xsl:value-of select="schoolClass/name"/></a></td>
                                <td><xsl:value-of select="subject/name"/>
                                    <xsl:if test="subject/origin = 'SCHOOL'">
                                        <span class="load-additional">Вариативная</span>
                                    </xsl:if>
                                </td>
                                <td class="td-mono"><xsl:value-of select="hours"/></td>
                            </tr>
                        </xsl:for-each>
                    </tbody>
                </table>
            </div>
            <xsl:call-template name="pagination">
                <xsl:with-param name="page" select="$entries/page"/>
                <xsl:with-param name="prefix" select="'loads'"/>
                <xsl:with-param name="ellipsis" select="'…'"/>
            </xsl:call-template>
        </div>
    </xsl:template>

    <xsl:template name="details">
        <div class="page-title-row"><div>
            <h1>Нагрузка <xsl:value-of select="$selected/schoolClass/name"/></h1>
            <p class="page-subtitle"><xsl:value-of select="$selected/subject/name"/></p>
        </div><a class="btn-outline-action" href="{$base}/draft" title="Открыть автозаполнение нагрузки">Автозаполнение</a></div>
        <div class="school-info">
            <div class="info-row"><span class="info-label">Класс:</span><span class="info-value"><xsl:value-of select="$selected/schoolClass/name"/></span></div>
            <div class="info-row"><span class="info-label">Предмет:</span><span class="info-value"><xsl:value-of select="$selected/subject/name"/></span></div>
            <div class="info-row"><span class="info-label">Часов/неделю:</span><span class="info-value"><xsl:value-of select="$selected/hours"/></span></div>
        </div>
    </xsl:template>

    <xsl:template name="scripts"><script type="module" src="/js/school/class-loads.js"></script></xsl:template>
</xsl:stylesheet>
