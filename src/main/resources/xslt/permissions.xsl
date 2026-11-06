<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:import href="federal-common.xsl"/>
    <xsl:output method="html" encoding="UTF-8" doctype-system="about:legacy-compat"/>
    <xsl:param name="view" select="'permissions'"/>
    <xsl:key name="resources" match="permissions/operations/operation" use="resource"/>
    <xsl:template match="/"><xsl:call-template name="page"/></xsl:template>
    <xsl:template name="page-title">Права доступа</xsl:template>
    <xsl:template name="styles"><link rel="stylesheet" href="/css/permissions.css"/></xsl:template>
    <xsl:template name="scripts"><script type="module" src="/js/permissions.js"></script></xsl:template>
    <xsl:template name="content">
        <div class="permissions-page" id="permissions">
            <div class="page-title-row">
                <div>
                    <h2>Права доступа</h2>
                    <p class="page-subtitle">Какие действия доступны каждой роли</p>
                </div>
                <span class="permissions-badge">Только для администратора</span>
            </div>
            <p class="permissions-intro">
                Основные маршруты ресурсов приложения и их правила доступа.
                Каждая роль показана отдельно; дополнительные условия указаны под действием.
                Ссылки на HTML-формы, фрагменты страниц и технические маршруты не включены.
            </p>
            <div class="permissions-filters" id="permission-filters" hidden="hidden">
                <label for="permission-search">
                    <span>Поиск</span>
                    <input type="search" id="permission-search" class="shadcn-input"
                           placeholder="Найти действие или эндпоинт" autocomplete="off"/>
                </label>
                <label for="permission-resource">
                    <span>Ресурс</span>
                    <select id="permission-resource" class="shadcn-select">
                        <option value="">Все ресурсы</option>
                        <xsl:for-each select="/surok/permissions/operations/operation[generate-id() = generate-id(key('resources', resource)[1])]">
                            <xsl:sort select="resource"/>
                            <option value="{resource}"><xsl:value-of select="resource"/></option>
                        </xsl:for-each>
                    </select>
                </label>
                <p class="permissions-count" id="permission-count" role="status" aria-live="polite"></p>
            </div>
            <ul class="permissions-legend" aria-label="Обозначения">
                <li><span class="permission-allowed" aria-hidden="true">✓</span> Доступ разрешён</li>
                <li><span class="permission-allowed" aria-hidden="true">✓*</span> Только своя школа</li>
                <li><span class="permission-denied" aria-hidden="true">—</span> Нет доступа</li>
            </ul>
            <p class="permissions-hint" id="permission-hint">
                Если таблица не помещается, прокрутите её по горизонтали, чтобы увидеть все роли.
                Проверки существования ресурса, данных запроса и CSRF выполняются дополнительно.
            </p>
            <xsl:for-each select="/surok/permissions/operations/operation[generate-id() = generate-id(key('resources', resource)[1])]">
                <xsl:sort select="resource"/>
                <section class="permissions-resource" data-resource="{resource}" aria-labelledby="resource-{position()}">
                    <h3 id="resource-{position()}"><xsl:value-of select="resource"/></h3>
                    <div class="permissions-scroll" tabindex="0" role="region"
                         aria-labelledby="resource-{position()}" aria-describedby="permission-hint">
                        <table class="data-table permissions-table">
                            <caption class="sr-only">Права доступа: <xsl:value-of select="resource"/></caption>
                            <thead>
                                <tr>
                                    <th scope="col" class="permission-action">Действие</th>
                                    <th scope="col" class="permission-endpoint">Эндпоинт</th>
                                    <xsl:for-each select="access/grant">
                                        <th scope="col" class="permission-role" title="{role}">
                                            <xsl:value-of select="title"/>
                                        </th>
                                    </xsl:for-each>
                                </tr>
                            </thead>
                            <tbody>
                                <xsl:for-each select="key('resources', resource)">
                                    <xsl:sort select="path"/>
                                    <xsl:sort select="method"/>
                                    <tr class="permission-row">
                                        <th scope="row" class="permission-action">
                                            <span><xsl:value-of select="action"/></span>
                                            <xsl:if test="string-length(condition) &gt; 0">
                                                <small class="permission-condition"><xsl:value-of select="condition"/></small>
                                            </xsl:if>
                                        </th>
                                        <td class="permission-endpoint">
                                            <span class="permission-method" data-method="{method}"><xsl:value-of select="method"/></span>
                                            <code><xsl:value-of select="path"/></code>
                                            <xsl:if test="string-length(consumes) &gt; 0">
                                                <small class="permission-input">Тело запроса: <xsl:value-of select="consumes"/></small>
                                            </xsl:if>
                                        </td>
                                        <xsl:for-each select="access/grant">
                                            <td class="permission-role" data-role="{role}" data-scope="{scope}">
                                                <xsl:choose>
                                                    <xsl:when test="scope = 'all'">
                                                        <span class="permission-allowed" title="Доступ разрешён" aria-label="Доступ разрешён">✓</span>
                                                    </xsl:when>
                                                    <xsl:when test="scope = 'school'">
                                                        <span class="permission-allowed" title="Только своя школа" aria-label="Только своя школа">✓*</span>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <span class="permission-denied" title="Нет доступа" aria-label="Нет доступа">—</span>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </td>
                                        </xsl:for-each>
                                    </tr>
                                </xsl:for-each>
                            </tbody>
                        </table>
                    </div>
                </section>
            </xsl:for-each>
            <p class="permissions-empty" id="permission-empty" hidden="hidden">
                Ничего не найдено. Измените поисковый запрос или выберите другой ресурс.
            </p>
        </div>
    </xsl:template>
</xsl:stylesheet>
