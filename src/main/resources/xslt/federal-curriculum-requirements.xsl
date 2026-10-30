<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:import href="federal-common.xsl"/>

    <xsl:output method="html"
                encoding="UTF-8"
                omit-xml-declaration="yes"
                doctype-system="about:legacy-compat"/>

    <xsl:param name="view"/>
    <xsl:variable name="editable" select="/surok/user/admin = 'true'"/>

    <xsl:template match="/">
        <xsl:choose>
            <xsl:when test="$view = 'list-fragment'">
                <xsl:call-template name="requirements-results"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="page"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="page-title">
        <xsl:text>Требования: </xsl:text>
        <xsl:value-of select="/surok/curriculums/curriculum/title"/>
    </xsl:template>

    <xsl:template name="styles">
        <link rel="stylesheet"
              href="/css/federal/requirements.css"/>
    </xsl:template>

    <xsl:template name="content">
        <xsl:call-template name="requirements"/>
    </xsl:template>

    <xsl:template name="scripts">
        <script type="module"
                src="/js/federal/curriculum-requirements.js"></script>
    </xsl:template>

    <xsl:template name="requirements">
        <xsl:variable name="curriculum"
                      select="/surok/curriculums/curriculum"/>
        <nav class="breadcrumbs" aria-label="Навигационная цепочка">
            <a class="breadcrumb-link"
               href="/federal/curriculums"
               title="Перейти к федеральным учебным планам">
                <xsl:text>Федеральные учебные планы</xsl:text>
            </a>
            <span class="breadcrumb-separator">-&gt;</span>
            <a class="breadcrumb-link"
               title="Перейти к описанию учебного плана">
                <xsl:attribute name="href">
                    <xsl:value-of select="concat('/federal/curriculums/', $curriculum/id)"/>
                </xsl:attribute>
                <xsl:value-of select="$curriculum/title"/>
            </a>
            <span class="breadcrumb-separator">-&gt;</span>
            <span class="breadcrumb-current">Требования</span>
        </nav>
        <div class="page-title-row">
            <div>
                <h1 class="title-with-action">
                    <span>Требования учебного плана</span>
                </h1>
                <p class="page-subtitle">
                    <xsl:value-of select="$curriculum/title"/>
                </p>
            </div>
        </div>
        <xsl:if test="$editable">
            <form class="requirements-add" id="requirements-add" method="post">
                <xsl:attribute name="action">
                    <xsl:value-of select="concat('/federal/curriculums/', $curriculum/id, '/requirements')"/>
                </xsl:attribute>
                <xsl:call-template name="csrf-input"/>
                <fieldset class="requirements-group requirements-group-add">
                    <legend>Добавить требование</legend>
                    <div class="requirements-grid requirements-grid-add">
                        <label class="requirements-field requirements-field-grade"
                               for="grade-number">
                            <select id="grade-number" name="grade" required="required">
                                <option value="">Класс</option>
                                <xsl:call-template name="grade-options"/>
                            </select>
                        </label>
                        <label class="requirements-field" for="subject-name">
                            <select id="subject-name" name="selection" required="required">
                                <option value="">Выберите предмет</option>
                                <option value="OPTIONAL">Дополнительные предметы</option>
                                <xsl:call-template name="subject-options"/>
                            </select>
                        </label>
                        <label class="requirements-field requirements-field-hours"
                               for="requirement-hours">
                            <input id="requirement-hours" name="hours" type="number" min="0"
                                   step="1" placeholder="Часов/нед." required="required"
                                   aria-label="Часов в неделю" aria-describedby="requirement-hours-hint"/>
                        </label>
                        <button class="btn-primary" type="submit">Сохранить</button>
                    </div>
                    <p id="requirement-hours-hint" class="page-subtitle" aria-live="polite">Для предмета — обязательные часы в неделю, для дополнительных предметов — общий максимум на каждый класс параллели.</p>
                </fieldset>
            </form>
        </xsl:if>
        <fieldset class="requirements-group requirements-search">
            <legend>Поиск требований</legend>
            <div class="requirements-grid requirements-grid-search">
                <label class="requirements-field" for="filter-grade">
                    <input id="filter-grade"
                           type="number"
                           max="11"
                           min="1"
                           placeholder="Класс"/>
                </label>
                <label class="requirements-field" for="filter-subject">
                    <select id="filter-subject">
                        <option value="">Все предметы</option>
                        <xsl:call-template name="subject-options"><xsl:with-param name="identifiers" select="false()"/></xsl:call-template>
                    </select>
                </label>
                <select id="filter-part">
                    <option value="">Все части</option>
                    <option value="MANDATORY">Обязательная</option>
                    <option value="OPTIONAL">Вариативная</option>
                </select>
            </div>
        </fieldset>
        <xsl:call-template name="requirements-results"/>
        <p class="page-subtitle">Обязательные часы назначаются по федеральному предмету.
            Вариативное требование задаёт максимум для каждого класса параллели;
            предметы и часы выбирает школа. Ноль запрещает вариативную нагрузку.
            Если требования для параллели нет, лимит не задан.</p>
    </xsl:template>

    <xsl:template name="requirements-results">
        <xsl:variable name="curriculum"
                      select="/surok/curriculums/curriculum"/>
        <xsl:variable name="requirements"
                      select="$curriculum/requirements"/>
        <div id="requirements-results">
            <table class="data-table">
                <thead>
                    <tr>
                        <xsl:call-template name="sort-heading">
                            <xsl:with-param name="field" select="'grade'"/>
                            <xsl:with-param name="label" select="'Класс'"/>
                            <xsl:with-param name="title"
                                            select="'Сортировать по классу'"/>
                            <xsl:with-param name="requirements"
                                            select="$requirements"/>
                        </xsl:call-template>
                        <xsl:call-template name="sort-heading">
                            <xsl:with-param name="field" select="'subject'"/>
                            <xsl:with-param name="label" select="'Предмет'"/>
                            <xsl:with-param name="title"
                                            select="'Сортировать по предмету'"/>
                            <xsl:with-param name="requirements"
                                            select="$requirements"/>
                        </xsl:call-template>
                        <xsl:call-template name="sort-heading">
                            <xsl:with-param name="field" select="'hours'"/>
                            <xsl:with-param name="label" select="'Часов/нед.'"/>
                            <xsl:with-param name="title"
                                            select="'Сортировать по часам в неделю'"/>
                            <xsl:with-param name="requirements"
                                            select="$requirements"/>
                        </xsl:call-template>
                        <xsl:call-template name="sort-heading">
                            <xsl:with-param name="field" select="'part'"/>
                            <xsl:with-param name="label" select="'Часть'"/>
                            <xsl:with-param name="title"
                                            select="'Сортировать по части'"/>
                            <xsl:with-param name="requirements"
                                            select="$requirements"/>
                        </xsl:call-template>
                        <xsl:if test="$editable">
                            <th class="requirements-actions-heading">Действия</th>
                        </xsl:if>
                    </tr>
                </thead>
                <tbody id="requirements-body">
                    <xsl:attribute name="data-search-url">
                        <xsl:value-of select="concat('/federal/curriculums/', $curriculum/id, '/requirements/fragment')"/>
                    </xsl:attribute>
                    <xsl:if test="not($requirements/requirement)">
                        <tr id="requirements-empty">
                            <td class="empty-state">
                                <xsl:attribute name="colspan"><xsl:value-of select="4 + number($editable)"/></xsl:attribute>
                                <xsl:text>Требования пока не добавлены.</xsl:text>
                            </td>
                        </tr>
                    </xsl:if>
                    <xsl:for-each select="$requirements/requirement">
                        <tr class="requirement-row">
                            <xsl:attribute name="data-grade"><xsl:value-of select="grade"/></xsl:attribute>
                            <xsl:attribute name="data-part"><xsl:value-of select="partType"/></xsl:attribute>
                            <xsl:attribute name="data-hours"><xsl:value-of select="hours"/></xsl:attribute>
                            <xsl:attribute name="data-subject"><xsl:value-of select="subject/id"/></xsl:attribute>
                            <td class="requirement-grade">
                                <span class="requirement-value"><xsl:value-of select="grade"/></span>
                                <xsl:if test="$editable">
                                    <label class="requirement-label">
                                        <xsl:attribute name="for"><xsl:value-of select="concat('requirement-', id, '-grade')"/></xsl:attribute>
                                        <xsl:text>Класс</xsl:text>
                                        <select class="requirement-input requirement-grade-input">
                                            <xsl:attribute name="id"><xsl:value-of select="concat('requirement-', id, '-grade')"/></xsl:attribute>
                                            <option value="">Класс</option>
                                            <xsl:call-template name="grade-options">
                                                <xsl:with-param name="selected" select="grade"/>
                                            </xsl:call-template>
                                        </select>
                                    </label>
                                </xsl:if>
                            </td>
                            <td class="requirement-subject">
                                <span>
                                    <xsl:attribute name="class"><xsl:if test="partType = 'MANDATORY'">requirement-value</xsl:if></xsl:attribute>
                                    <xsl:choose>
                                        <xsl:when test="partType = 'OPTIONAL'">Дополнительные предметы</xsl:when>
                                        <xsl:otherwise><xsl:value-of select="subject/name"/></xsl:otherwise>
                                    </xsl:choose>
                                </span>
                                <xsl:if test="$editable and partType = 'MANDATORY'">
                                    <label class="requirement-label">
                                        <xsl:attribute name="for"><xsl:value-of select="concat('requirement-', id, '-subject')"/></xsl:attribute>
                                        <xsl:text>Предмет</xsl:text>
                                        <select class="requirement-input requirement-subject-input">
                                            <xsl:attribute name="id"><xsl:value-of select="concat('requirement-', id, '-subject')"/></xsl:attribute>
                                            <option value="">Выберите предмет</option>
                                            <xsl:call-template name="subject-options">
                                                <xsl:with-param name="selected" select="subject/id"/>
                                            </xsl:call-template>
                                        </select>
                                    </label>
                                </xsl:if>
                            </td>
                            <td class="requirement-hours">
                                <span class="requirement-value"><xsl:if test="partType = 'OPTIONAL'">До </xsl:if><xsl:value-of select="hours"/></span>
                                <xsl:if test="$editable">
                                    <label class="requirement-label">
                                        <xsl:attribute name="for"><xsl:value-of select="concat('requirement-', id, '-hours')"/></xsl:attribute>
                                        <xsl:text>Часов в неделю</xsl:text>
                                        <input class="requirement-input requirement-hours-input"
                                               type="number"
                                               step="1">
                                            <xsl:attribute name="min"><xsl:value-of select="number(partType = 'MANDATORY')"/></xsl:attribute>
                                            <xsl:attribute name="id"><xsl:value-of select="concat('requirement-', id, '-hours')"/></xsl:attribute>
                                            <xsl:attribute name="value"><xsl:value-of select="hours"/></xsl:attribute>
                                        </input>
                                    </label>
                                </xsl:if>
                            </td>
                            <td class="requirement-part">
                                <span class="requirement-value">
                                    <xsl:choose>
                                        <xsl:when test="partType = 'MANDATORY'">Обязательная</xsl:when>
                                        <xsl:otherwise>Вариативная</xsl:otherwise>
                                    </xsl:choose>
                                </span>
                            </td>
                            <xsl:if test="$editable">
                                <td>
                                    <div class="td-actions">
                                        <button class="requirement-action requirement-edit"
                                                type="button"
                                                title="Редактировать">
                                            <xsl:call-template name="edit-icon"/>
                                        </button>
                                        <button class="requirement-action requirement-delete"
                                                type="button"
                                                title="Удалить">
                                            <xsl:attribute name="data-curriculum"><xsl:value-of select="$curriculum/id"/></xsl:attribute>
                                            <xsl:attribute name="data-id"><xsl:value-of select="id"/></xsl:attribute>
                                            <xsl:attribute name="data-name">
                                                <xsl:choose>
                                                    <xsl:when test="partType = 'OPTIONAL'">Вариативная нагрузка, <xsl:value-of select="grade"/> класс</xsl:when>
                                                    <xsl:otherwise><xsl:value-of select="subject/name"/></xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:attribute>
                                            <xsl:call-template name="cross-icon"/>
                                        </button>
                                        <button class="requirement-action requirement-save"
                                                type="button"
                                                title="Сохранить">
                                            <svg xmlns="http://www.w3.org/2000/svg"
                                                 fill="none"
                                                 stroke="currentColor"
                                                 stroke-width="2.5"
                                                 viewBox="0 0 24 24">
                                                <polyline points="20 6 9 17 4 12"/>
                                            </svg>
                                        </button>
                                        <button class="requirement-action requirement-cancel"
                                                type="button"
                                                title="Отмена">
                                            <xsl:call-template name="cross-icon"/>
                                        </button>
                                        <form class="requirement-edit-form"
                                              method="post">
                                            <xsl:attribute name="action">
                                                <xsl:value-of select="concat('/federal/curriculums/', $curriculum/id, '/requirements/', id)"/>
                                            </xsl:attribute>
                                            <xsl:call-template name="csrf-input"/>
                                            <input class="requirement-form-grade"
                                                   name="grade"
                                                   type="hidden"/>
                                            <xsl:if test="partType = 'MANDATORY'">
                                                <input class="requirement-form-subject"
                                                       name="subjectId" type="hidden"/>
                                            </xsl:if>
                                            <input class="requirement-form-hours" name="hours" type="hidden"/>
                                            <input class="requirement-form-part"
                                                   name="partType"
                                                   type="hidden"/>
                                        </form>
                                    </div>
                                </td>
                            </xsl:if>
                        </tr>
                    </xsl:for-each>
                </tbody>
            </table>
            <xsl:call-template name="pagination">
                <xsl:with-param name="page" select="$requirements/page"/>
                <xsl:with-param name="prefix" select="'requirements'"/>
                <xsl:with-param name="ellipsis" select="'...'"/>
            </xsl:call-template>
        </div>
    </xsl:template>

    <xsl:template name="grade-options">
        <xsl:param name="selected" select="''"/>
        <xsl:param name="grade" select="/surok/firstGrade"/>
        <xsl:if test="$grade &lt;= /surok/lastGrade">
            <option>
                <xsl:attribute name="value"><xsl:value-of select="$grade"/></xsl:attribute>
                <xsl:if test="$grade = $selected">
                    <xsl:attribute name="selected">selected</xsl:attribute>
                </xsl:if>
                <xsl:value-of select="$grade"/>
            </option>
            <xsl:call-template name="grade-options">
                <xsl:with-param name="selected" select="$selected"/>
                <xsl:with-param name="grade" select="$grade + 1"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template name="subject-options">
        <xsl:param name="selected" select="''"/>
        <xsl:param name="identifiers" select="true()"/>
        <xsl:for-each select="/surok/subjects/subject">
            <xsl:sort select="name"/>
            <option>
                <xsl:attribute name="value"><xsl:choose>
                    <xsl:when test="$identifiers"><xsl:value-of select="id"/></xsl:when>
                    <xsl:otherwise><xsl:value-of select="name"/></xsl:otherwise>
                </xsl:choose></xsl:attribute>
                <xsl:if test="id = $selected">
                    <xsl:attribute name="selected">selected</xsl:attribute>
                </xsl:if>
                <xsl:value-of select="name"/>
            </option>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="sort-heading">
        <xsl:param name="field"/>
        <xsl:param name="label"/>
        <xsl:param name="title"/>
        <xsl:param name="requirements"/>
        <xsl:variable name="direction"
                      select="$requirements/sorts/item[name = $field]/direction"/>
        <th>
            <button type="button">
                <xsl:attribute name="class">
                    <xsl:text>requirements-sort</xsl:text>
                    <xsl:if test="$direction = 'ASC' or $direction = 'DESC'">
                        <xsl:text> active</xsl:text>
                    </xsl:if>
                </xsl:attribute>
                <xsl:attribute name="title"><xsl:value-of select="$title"/></xsl:attribute>
                <xsl:attribute name="data-sort"><xsl:value-of select="$field"/></xsl:attribute>
                <span><xsl:value-of select="$label"/></span>
                <span class="requirements-sort-icon">
                    <xsl:choose>
                        <xsl:when test="$direction = 'ASC'">↑</xsl:when>
                        <xsl:when test="$direction = 'DESC'">↓</xsl:when>
                        <xsl:otherwise>↕</xsl:otherwise>
                    </xsl:choose>
                </span>
            </button>
        </th>
    </xsl:template>
</xsl:stylesheet>
