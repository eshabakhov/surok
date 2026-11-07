<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:import href="school-common.xsl"/>
    <xsl:import href="school-form.xsl"/>
    <xsl:output method="html" encoding="UTF-8" omit-xml-declaration="yes" doctype-system="about:legacy-compat"/>
    <xsl:param name="view"/>
    <xsl:template match="/"><xsl:call-template name="page"/></xsl:template>
    <xsl:template name="page-title"><xsl:choose>
        <xsl:when test="$view = 'edit'">Редактировать школу</xsl:when>
        <xsl:otherwise><xsl:value-of select="/surok/schools/school/name"/></xsl:otherwise>
    </xsl:choose></xsl:template>
    <xsl:template name="styles"><link rel="stylesheet" href="/css/school/school.css"/></xsl:template>
    <xsl:template name="scripts">
        <xsl:choose>
            <xsl:when test="$view = 'edit'">
                <script type="module" src="/js/school/school-edit.js"></script>
            </xsl:when>
            <xsl:when test="$view = 'details' and /surok/user/admin = 'true'">
                <script type="module" src="/js/school/school.js"></script>
            </xsl:when>
        </xsl:choose>
    </xsl:template>
    <xsl:template name="content"><xsl:choose>
        <xsl:when test="$view = 'edit'"><xsl:call-template name="school-form">
            <xsl:with-param name="editing" select="true()"/>
        </xsl:call-template></xsl:when>
        <xsl:otherwise><xsl:call-template name="details"/></xsl:otherwise>
    </xsl:choose></xsl:template>
    <xsl:template name="details">
        <xsl:variable name="selected" select="/surok/schools/school"/>
        <div class="page-title-row"><div><h1 class="title-with-action">
            <span><xsl:value-of select="$selected/name"/></span>
            <xsl:if test="/surok/user/admin = 'true'">
                <a class="btn-icon-edit" href="/schools/{$selected/id}/edit" title="Редактировать" aria-label="Редактировать">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" height="15" width="15"
                         stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/>
                    </svg>
                </a>
            </xsl:if>
        </h1><p class="page-subtitle">Информация о школе</p></div></div>
        <div class="school-info">
            <div class="info-row"><span class="info-label">ID:</span><span class="info-value"><xsl:value-of select="$selected/id"/></span></div>
            <div class="info-row"><span class="info-label">Название:</span><span class="info-value"><xsl:value-of select="$selected/name"/></span></div>
        </div>
        <xsl:if test="/surok/user/admin = 'true'">
            <div class="details-footer-actions">
                <button class="btn-outline-action school-delete btn-delete-school" type="button"
                        data-id="{$selected/id}" data-name="{$selected/name}">Удалить школу</button>
            </div>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>
