<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:import href="school-common.xsl"/>
    <xsl:output method="html" encoding="UTF-8" omit-xml-declaration="yes"
                doctype-system="about:legacy-compat"/>
    <xsl:param name="view"/>
    <xsl:variable name="entries" select="/surok/teachers"/>
    <xsl:variable name="selected" select="$entries/teacher[$view = 'details' or $view = 'edit']"/>
    <xsl:variable name="base" select="concat($school-path, '/teachers')"/>

    <xsl:template match="/">
        <xsl:choose>
            <xsl:when test="$view = 'list-fragment'"><xsl:call-template name="results"/></xsl:when>
            <xsl:otherwise><xsl:call-template name="page"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="page-title">
        <xsl:choose>
            <xsl:when test="$view = 'create'">Новый учитель</xsl:when>
            <xsl:when test="$view = 'edit'">Редактировать учителя</xsl:when>
            <xsl:when test="$view = 'details'"><xsl:value-of select="$selected/name"/></xsl:when>
            <xsl:otherwise><xsl:value-of select="concat($school/name, ' - учителя')"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="styles">
        <link rel="stylesheet" href="/css/school/teachers.css"/>
    </xsl:template>

    <xsl:template name="content">
        <xsl:choose>
            <xsl:when test="$view = 'create' or $view = 'edit'"><xsl:call-template name="form"/></xsl:when>
            <xsl:when test="$view = 'details'"><xsl:call-template name="details"/></xsl:when>
            <xsl:otherwise>
                <div class="page-title-row">
                    <div><h1>Учителя</h1><p class="page-subtitle">Управление учителями школы</p></div>
                    <a class="btn btn-primary teachers-add"
                       href="{$base}/create"
                       title="Добавить учителя в школу">
                        <svg xmlns="http://www.w3.org/2000/svg"
                             fill="none"
                             stroke="currentColor"
                             stroke-width="2"
                             viewBox="0 0 24 24">
                            <line x1="12" x2="12" y1="5" y2="19"/>
                            <line x1="5" x2="19" y1="12" y2="12"/>
                        </svg>
                        <xsl:text>Добавить учителя</xsl:text>
                    </a>
                </div>
                <div class="teachers-content">
                    <div class="search-wrap teachers-search">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor"
                             stroke-width="2" viewBox="0 0 24 24">
                            <circle cx="11" cy="11" r="8"/><line x1="21" x2="16.65" y1="21" y2="16.65"/>
                        </svg>
                        <label for="teacher-search">
                            <input id="teacher-search" type="text" placeholder="Поиск учителя..."
                                   value="{$entries/filters/item[name='name' or name='name_ct']/value}"/>
                        </label>
                    </div>
                    <xsl:call-template name="results"/>
                </div>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="results">
        <div id="teachers-results" class="teachers-results">
            <div class="teachers-sort-controls">
                <xsl:call-template name="name-sort">
                    <xsl:with-param name="prefix" select="'teachers'"/>
                    <xsl:with-param name="label" select="'ФИО'"/>
                    <xsl:with-param name="title" select="'Сортировать по ФИО'"/>
                    <xsl:with-param name="direction" select="$entries/sorts/item[name = 'name']/direction"/>
                </xsl:call-template>
            </div>
            <div class="card-grid" id="teachers-container">
                <xsl:if test="not($entries/teacher)">
                    <div class="teachers-empty">Учителя не найдены.</div>
                </xsl:if>
                <xsl:for-each select="$entries/teacher">
                    <div class="card-item teacher-row teacher-card" data-id="{id}" data-name="{name}">
                        <div class="card-item-header"><div class="card-item-title-row"><div class="card-item-icon">
                            <svg fill="none"
                                     stroke="currentColor"
                                     stroke-width="2"
                                     viewBox="0 0 24 24"
                                     xmlns="http://www.w3.org/2000/svg">
                                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                                    <circle cx="9" cy="7" r="4"/>
                                    <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>
                                </svg>
                        </div></div></div>
                        <div class="card-item-period">
                            <a class="card-item-name" href="{$base}/{id}" title="Открыть"><xsl:value-of select="name"/></a>
                            <div class="card-item-period-label">ID: <span class="td-mono"><xsl:value-of select="id"/></span></div>
                        </div>
                    </div>
                </xsl:for-each>
            </div>
            <xsl:call-template name="pagination">
                <xsl:with-param name="page" select="$entries/page"/>
                <xsl:with-param name="prefix" select="'teachers'"/>
                <xsl:with-param name="ellipsis" select="'…'"/>
            </xsl:call-template>
        </div>
    </xsl:template>

    <xsl:template name="details">
        <div class="page-title-row teacher-details-heading">
            <div>
                <h1 class="title-with-action">
                    <span><xsl:value-of select="$selected/name"/></span>
                    <a class="btn-icon-edit" href="{$base}/{$selected/id}/edit"
                       title="Редактировать" aria-label="Редактировать">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" height="15" width="15"
                             stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/>
                        </svg>
                    </a>
                </h1>
                <p class="page-subtitle">Информация об учителе</p>
            </div>
        </div>
        <div class="school-info">
            <div class="info-row"><span class="info-label">ID:</span><span class="info-value"><xsl:value-of select="$selected/id"/></span></div>
            <div class="info-row"><span class="info-label">ФИО:</span><span class="info-value"><xsl:value-of select="$selected/name"/></span></div>
        </div>
        <div class="details-footer-actions">
            <button class="btn-outline-action btn-delete-row teacher-delete" type="button"
                    data-name="{$selected/name}" data-uri="{$base}/{$selected/id}" data-redirect="{$base}">Удалить</button>
        </div>
    </xsl:template>

    <xsl:template name="form">
        <div class="page-title-row teacher-form-heading">
            <div class="teacher-form-title">
                <div>
                    <h1>
                        <xsl:choose>
                            <xsl:when test="$view = 'edit'">Редактировать</xsl:when>
                            <xsl:otherwise><xsl:call-template name="page-title"/></xsl:otherwise>
                        </xsl:choose>
                    </h1>
                    <xsl:if test="$view = 'create'">
                        <p class="page-subtitle">Заполните данные для создания учителя</p>
                    </xsl:if>
                </div>
            </div>
        </div>
        <xsl:if test="$view = 'edit' or /surok/error = 'empty' or /surok/error = 'duplicate'">
            <div id="teacher-form-error" class="error-message" role="alert">
                <xsl:choose>
                    <xsl:when test="/surok/error = 'empty'">Имя не может быть пустым</xsl:when>
                    <xsl:when test="/surok/error = 'duplicate'">Учитель с таким именем уже существует</xsl:when>
                    <xsl:otherwise><xsl:attribute name="hidden">hidden</xsl:attribute></xsl:otherwise>
                </xsl:choose>
            </div>
        </xsl:if>
        <xsl:if test="$view = 'edit'">
            <noscript><p class="error-message">Включите JavaScript, чтобы сохранить изменения.</p></noscript>
        </xsl:if>
        <div class="teacher-form-wrap">
            <form method="post" class="teacher-form">
                <xsl:if test="$view = 'edit'"><xsl:attribute name="id">teacher-edit-form</xsl:attribute></xsl:if>
                <xsl:attribute name="action">
                    <xsl:choose>
                        <xsl:when test="$view = 'edit'"><xsl:value-of select="concat($base, '/', $selected/id)"/></xsl:when>
                        <xsl:otherwise><xsl:value-of select="concat($base, '/create')"/></xsl:otherwise>
                    </xsl:choose>
                </xsl:attribute>
                <xsl:if test="$view = 'create'"><xsl:call-template name="csrf-input"/></xsl:if>
                <label for="teacher-name" class="teacher-form-field">
                    ФИО учителя
                    <input id="teacher-name" name="name" type="text" autofocus="autofocus"
                           required="required" value="{$selected/name}">
                        <xsl:if test="$view = 'create'">
                            <xsl:attribute name="placeholder">Например: Иванов Иван Иванович</xsl:attribute>
                        </xsl:if>
                    </input>
                </label>
                <div class="teacher-form-actions">
                    <a class="btn-outline-action">
                        <xsl:attribute name="title">
                            <xsl:choose>
                                <xsl:when test="$view = 'edit'">Отменить редактирование учителя</xsl:when>
                                <xsl:otherwise>Отменить создание учителя</xsl:otherwise>
                            </xsl:choose>
                        </xsl:attribute>
                        <xsl:attribute name="href">
                            <xsl:choose>
                                <xsl:when test="$view = 'edit'"><xsl:value-of select="concat($base, '/', $selected/id)"/></xsl:when>
                                <xsl:otherwise><xsl:value-of select="$base"/></xsl:otherwise>
                            </xsl:choose>
                        </xsl:attribute>
                        <xsl:text>Отмена</xsl:text>
                    </a>
                    <button class="btn-primary" type="submit">
                        <xsl:choose>
                            <xsl:when test="$view = 'edit'">
                                <xsl:attribute name="disabled">disabled</xsl:attribute>Сохранить
                            </xsl:when>
                            <xsl:otherwise>Создать</xsl:otherwise>
                        </xsl:choose>
                    </button>
                </div>
            </form>
        </div>
    </xsl:template>

    <xsl:template name="scripts">
        <xsl:choose>
            <xsl:when test="$view = 'edit'"><script type="module" src="/js/school/teacher-edit.js"></script></xsl:when>
            <xsl:otherwise><script type="module" src="/js/school/teachers.js"></script></xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
