<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template name="school-form">
        <xsl:param name="editing"/>
        <xsl:variable name="selected" select="/surok/schools/school"/>
        <div class="page-title-row school-form-heading"><div><h1>
            <xsl:choose><xsl:when test="$editing">Редактировать школу</xsl:when>
                <xsl:otherwise>Новая школа</xsl:otherwise></xsl:choose>
        </h1></div></div>
        <xsl:if test="/surok/error = 'true'">
            <div class="error-message">Название не может быть пустым</div>
        </xsl:if>
        <div class="school-form-wrap">
            <form class="school-form" method="post">
                <xsl:attribute name="action"><xsl:choose>
                    <xsl:when test="$editing"><xsl:value-of select="concat('/schools/', $selected/id, '/edit')"/></xsl:when>
                    <xsl:otherwise>/schools</xsl:otherwise>
                </xsl:choose></xsl:attribute>
                <xsl:call-template name="csrf-input"/>
                <label class="school-form-field" for="school-name">Название школы
                    <input id="school-name" name="name" type="text" autofocus="autofocus"
                           required="required" placeholder="Например: Школа №42" value="{$selected/name}"/>
                </label>
                <div class="school-form-actions">
                    <a class="btn-outline-action" title="Отменить изменение школы">
                        <xsl:attribute name="href"><xsl:choose>
                            <xsl:when test="$editing"><xsl:value-of select="concat('/schools/', $selected/id)"/></xsl:when>
                            <xsl:otherwise>/schools</xsl:otherwise>
                        </xsl:choose></xsl:attribute>Отмена
                    </a>
                    <button class="btn-primary" type="submit"><xsl:choose>
                        <xsl:when test="$editing">Сохранить</xsl:when><xsl:otherwise>Создать</xsl:otherwise>
                    </xsl:choose></button>
                </div>
            </form>
        </div>
    </xsl:template>
</xsl:stylesheet>
