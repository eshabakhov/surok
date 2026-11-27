<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:import href="web-common.xsl"/>
    <xsl:output method="html" encoding="UTF-8" omit-xml-declaration="yes" doctype-system="about:legacy-compat"/>
    <xsl:template match="/"><xsl:call-template name="page"/></xsl:template>
    <xsl:template name="page-title"><xsl:value-of select="/surok/@status"/> - Surok</xsl:template>
    <xsl:template name="page-description"><xsl:value-of select="/surok/title"/></xsl:template>
    <xsl:template name="styles"><link rel="stylesheet" href="/css/error.css"/></xsl:template>
    <xsl:template name="page-body">
        <main>
            <div class="error-page">
                <div class="error-card">
                    <span class="error-code"><xsl:value-of select="/surok/@status"/></span>
                    <h1 class="error-title"><xsl:value-of select="/surok/title"/></h1>
                    <p class="error-desc"><xsl:value-of select="/surok/message"/></p>
                    <xsl:choose>
                        <xsl:when test="/surok/@status = 401">
                            <a class="btn btn-primary" href="/users/login" title="Войти в систему">Войти</a>
                        </xsl:when>
                        <xsl:otherwise>
                            <a class="btn btn-outline-custom" id="error-back" href="/" title="Вернуться на предыдущую страницу">Назад</a>
                        </xsl:otherwise>
                    </xsl:choose>
                </div>
            </div>
        </main>
        <footer class="error-footer">
            <span class="error-footer-logo">Surok</span>
            <span class="error-footer-copy">© 2026</span>
        </footer>
        <script type="module" src="/js/error.js"></script>
    </xsl:template>
</xsl:stylesheet>
