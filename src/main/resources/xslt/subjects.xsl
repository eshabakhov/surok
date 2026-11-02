<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:import href="school-common.xsl"/>
    <xsl:output method="html" encoding="UTF-8" omit-xml-declaration="yes"
                doctype-system="about:legacy-compat"/>
    <xsl:param name="view"/>
    <xsl:variable name="entries" select="/surok/subjects"/>
    <xsl:variable name="selected" select="$entries/subject[$view = 'details' or $view = 'edit']"/>
    <xsl:variable name="catalogue" select="not(/surok/school)"/>
    <xsl:variable name="editable" select="($catalogue and /surok/user/admin = 'true') or (not($catalogue) and /surok/editable = 'true')"/>
    <xsl:variable name="base"><xsl:choose>
        <xsl:when test="$catalogue">/federal/subjects</xsl:when>
        <xsl:otherwise><xsl:value-of select="concat($school-path, '/subjects')"/></xsl:otherwise>
    </xsl:choose></xsl:variable>

    <xsl:template match="/">
        <xsl:choose>
            <xsl:when test="$view = 'list-fragment'"><xsl:call-template name="results"/></xsl:when>
            <xsl:otherwise><xsl:call-template name="page"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="page-title">
        <xsl:choose>
            <xsl:when test="$view = 'create'">Добавить предмет</xsl:when>
            <xsl:when test="$view = 'edit'">Редактировать предмет</xsl:when>
            <xsl:when test="$view = 'details'"><xsl:value-of select="$selected/name"/></xsl:when>
            <xsl:when test="$catalogue">Предметы</xsl:when>
            <xsl:otherwise><xsl:value-of select="concat($school/name, ' - предметы')"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="styles">
        <link rel="stylesheet" href="/css/school/subjects.css"/>
    </xsl:template>

    <xsl:template name="content">
        <xsl:choose>
            <xsl:when test="$view = 'create' or $view = 'edit'"><xsl:call-template name="form"/></xsl:when>
            <xsl:when test="$view = 'details'"><xsl:call-template name="details"/></xsl:when>
            <xsl:otherwise>
                <div class="page-title-row">
                    <div><h1>Предметы</h1><p class="page-subtitle"><xsl:choose>
                        <xsl:when test="$catalogue">Общий справочник федеральных предметов</xsl:when>
                        <xsl:otherwise>Федеральные и дополнительные предметы школы</xsl:otherwise>
                    </xsl:choose></p></div>
                    <xsl:if test="$editable">
                    <a class="btn btn-primary subjects-add"
                       href="{$base}/create"
                       title="Добавить предмет">
                        <svg xmlns="http://www.w3.org/2000/svg"
                             fill="none"
                             stroke="currentColor"
                             stroke-width="2"
                             viewBox="0 0 24 24">
                            <line x1="12" x2="12" y1="5" y2="19"/>
                            <line x1="5" x2="19" y1="12" y2="12"/>
                        </svg>
                        <xsl:text>Добавить предмет</xsl:text>
                    </a>
                    </xsl:if>
                </div>
                <div class="subjects-content">
                    <div class="search-wrap subjects-search">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor"
                             stroke-width="2" viewBox="0 0 24 24">
                            <circle cx="11" cy="11" r="8"/><line x1="21" x2="16.65" y1="21" y2="16.65"/>
                        </svg>
                        <label for="subject-search">
                            <input id="subject-search" type="text" placeholder="Поиск предмета..."
                                   value="{$entries/filters/item[name='name' or name='name_ct']/value}"/>
                        </label>
                    </div>
                    <xsl:call-template name="results"/>
                </div>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="results">
        <div id="subjects-results" class="subjects-results">
            <div class="subjects-sort-controls">
                <xsl:call-template name="name-sort">
                    <xsl:with-param name="prefix" select="'subjects'"/>
                    <xsl:with-param name="direction" select="$entries/sorts/item[name = 'name']/direction"/>
                </xsl:call-template>
            </div>
            <div class="card-grid" id="subjects-container">
                <xsl:if test="not($entries/subject)">
                    <div class="subjects-empty">Предметы не найдены.</div>
                </xsl:if>
                <xsl:for-each select="$entries/subject">
                    <div class="card-item subject-row subject-card" data-id="{id}" data-name="{name}">
                        <div class="card-item-header"><div class="card-item-title-row"><div class="card-item-icon">
                            <svg fill="none"
                                     stroke="currentColor"
                                     stroke-width="2"
                                     viewBox="0 0 24 24"
                                     xmlns="http://www.w3.org/2000/svg">
                                    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
                                    <path d="M4 4.5A2.5 2.5 0 0 1 6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5z"/>
                                </svg>
                        </div></div></div>
                        <div class="card-item-period">
                            <a class="card-item-name" href="{$base}/{id}" title="Открыть"><xsl:value-of select="name"/></a>
                            <div class="card-item-period-label">ID: <span class="td-mono"><xsl:value-of select="id"/></span></div>
                            <div class="card-item-period-label"><xsl:choose>
                                <xsl:when test="origin = 'FEDERAL'">Федеральный предмет</xsl:when>
                                <xsl:otherwise>Дополнительный предмет</xsl:otherwise>
                            </xsl:choose></div>
                        </div>
                    </div>
                </xsl:for-each>
            </div>
            <xsl:call-template name="pagination">
                <xsl:with-param name="page" select="$entries/page"/>
                <xsl:with-param name="prefix" select="'subjects'"/>
                <xsl:with-param name="ellipsis" select="'…'"/>
            </xsl:call-template>
        </div>
    </xsl:template>

    <xsl:template name="details">
        <div class="page-title-row subject-details-heading">
            <div>
                <h1 class="title-with-action">
                    <span><xsl:value-of select="$selected/name"/></span>
                    <xsl:if test="$editable and ($catalogue or $selected/origin != 'FEDERAL' or not($selected/origin))"><a class="btn-icon-edit" href="{$base}/{$selected/id}/edit"
                       title="Редактировать" aria-label="Редактировать">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" height="15" width="15"
                             stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/>
                        </svg>
                    </a></xsl:if>
                </h1>
                <p class="page-subtitle">Информация о предмете</p>
            </div>
        </div>
        <div class="school-info">
            <div class="info-row"><span class="info-label">ID:</span><span class="info-value"><xsl:value-of select="$selected/id"/></span></div>
            <div class="info-row"><span class="info-label">Название:</span><span class="info-value"><xsl:value-of select="$selected/name"/></span></div>
        </div>
        <xsl:choose>
            <xsl:when test="not($catalogue) and $selected/origin = 'FEDERAL'">
                <p>Федеральный предмет доступен только для просмотра.
                    <a href="/federal/subjects/{$selected/id}" title="Открыть федеральный предмет">Открыть в федеральном справочнике</a>
                </p>
            </xsl:when>
            <xsl:otherwise><xsl:if test="$editable">
                <div class="details-footer-actions">
                    <button class="btn-outline-action btn-delete-row subject-delete" type="button"
                            data-name="{$selected/name}" data-uri="{$base}/{$selected/id}"
                            data-redirect="{$base}">Удалить</button>
                </div>
            </xsl:if></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="form">
        <div class="page-title-row subject-form-heading">
            <div class="subject-form-title">
                <div>
                    <h1>
                        <xsl:choose>
                            <xsl:when test="$view = 'edit'">Редактировать</xsl:when>
                            <xsl:otherwise><xsl:call-template name="page-title"/></xsl:otherwise>
                        </xsl:choose>
                    </h1>
                    <xsl:if test="$view = 'create'">
                        <p class="page-subtitle">Заполните данные для создания предмета</p>
                    </xsl:if>
                </div>
            </div>
        </div>
        <xsl:choose>
            <xsl:when test="/surok/error = 'empty'"><div class="error-message" role="alert">Название не может быть пустым</div></xsl:when>
            <xsl:when test="/surok/error = 'duplicate'"><div class="error-message" role="alert">Предмет с таким названием уже существует</div></xsl:when>
        </xsl:choose>
        <div class="subject-form-wrap">
            <form method="post" class="subject-form">
                <xsl:attribute name="action">
                    <xsl:choose>
                        <xsl:when test="$view = 'edit'"><xsl:value-of select="concat($base, '/', $selected/id, '/edit')"/></xsl:when>
                        <xsl:otherwise><xsl:value-of select="concat($base, '/create')"/></xsl:otherwise>
                    </xsl:choose>
                </xsl:attribute>
                <xsl:call-template name="csrf-input"/>
                <label for="subject-name" class="subject-form-field">
                    Название предмета
                    <input id="subject-name" name="name" type="text" autofocus="autofocus"
                           required="required" value="{$selected/name}">
                        <xsl:if test="$view = 'create'">
                            <xsl:attribute name="placeholder"><xsl:choose>
                                <xsl:when test="$catalogue">Например: Математика</xsl:when>
                                <xsl:otherwise>Например: Робототехника</xsl:otherwise>
                            </xsl:choose></xsl:attribute>
                        </xsl:if>
                    </input>
                </label>
                <div class="subject-form-actions">
                    <a class="btn-outline-action">
                        <xsl:attribute name="title">
                            <xsl:choose>
                                <xsl:when test="$view = 'edit'">Отменить редактирование предмета</xsl:when>
                                <xsl:otherwise>Отменить создание предмета</xsl:otherwise>
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
                        <xsl:choose><xsl:when test="$view = 'edit'">Сохранить</xsl:when><xsl:otherwise>Добавить</xsl:otherwise></xsl:choose>
                    </button>
                </div>
            </form>
        </div>
    </xsl:template>

    <xsl:template name="scripts"><script type="module" src="/js/school/subjects.js"></script></xsl:template>
</xsl:stylesheet>
