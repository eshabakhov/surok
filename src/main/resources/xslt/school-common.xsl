<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:import href="federal-common.xsl"/>
    <xsl:variable name="school" select="/surok/school"/>
    <xsl:variable name="building" select="/surok/building | /surok/buildings/building[$view = 'details' or $view = 'edit']"/>
    <xsl:variable name="cabinet" select="/surok/cabinets/cabinet[$view = 'details' or $view = 'edit']"/>
    <xsl:variable name="clazz" select="/surok/classes/school-class[not(/surok/loads) and ($view = 'details' or $view = 'edit')]"/>
    <xsl:variable name="teacher" select="/surok/teachers/teacher[$view = 'details' or $view = 'edit']"/>
    <xsl:variable name="subject" select="/surok/subjects/subject[not(/surok/loads) and ($view = 'details' or $view = 'edit')]"/>
    <xsl:variable name="school-path" select="concat('/schools/', $school/id)"/>

    <xsl:template name="school-navigation">
        <xsl:if test="$school/id">
        <div class="sidebar-section">
            <a class="sidebar-school-link" href="{$school-path}" title="Перейти к школе">
                <xsl:value-of select="$school/name"/>
            </a>
        </div>
        <a class="sidebar-link"
                       href="{$school-path}/schedules"
                       title="Расписания">
                        <svg fill="none"
                             stroke="currentColor"
                             stroke-width="2"
                             viewBox="0 0 24 24"
                             xmlns="http://www.w3.org/2000/svg">
                            <rect height="18" rx="2" ry="2" width="18" x="3" y="4"/>
                            <line x1="16" x2="16" y1="2" y2="6"/>
                            <line x1="8" x2="8" y1="2" y2="6"/>
                            <line x1="3" x2="21" y1="10" y2="10"/>
                        </svg>
                        <span class="sidebar-link-label">Расписания</span>
                    </a>
        <xsl:if test="$school/schedules/schedule">
            <div class="sidebar-subnav">
                <xsl:for-each select="$school/schedules/schedule">
                    <a class="sidebar-schedule-link"
                       href="{$school-path}/schedules/{id}" title="{name}">
                        <xsl:if test="id = /surok/schedule/id">
                            <xsl:attribute name="class">sidebar-schedule-link active</xsl:attribute>
                        </xsl:if>
                        <span><xsl:value-of select="name"/></span>
                    </a>
                    <xsl:if test="id = /surok/schedule/id">
                        <a class="sidebar-subnav-link active" title="Учебная нагрузка"
                           href="{$school-path}/schedules/{id}/loads">
                            <xsl:if test="not(/surok/loads) and not(/surok/saved)">
                                <xsl:attribute name="class">sidebar-subnav-link</xsl:attribute>
                            </xsl:if>
                            <xsl:text>Учебная нагрузка</xsl:text>
                        </a>
                    </xsl:if>
                </xsl:for-each>
            </div>
        </xsl:if>
        <a class="sidebar-link"
                       href="{$school-path}/teachers"
                       title="Учителя">
                        <svg fill="none"
                             stroke="currentColor"
                             stroke-width="2"
                             viewBox="0 0 24 24"
                             xmlns="http://www.w3.org/2000/svg">
                            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                            <circle cx="9" cy="7" r="4"/>
                            <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>
                        </svg>
                        <span class="sidebar-link-label">Учителя</span>
                    </a>
        <xsl:if test="$teacher/id">
            <div class="sidebar-subnav">
                <a class="sidebar-subnav-link active" href="{$school-path}/teachers/{$teacher/id}"
                   title="Перейти к учителю"><xsl:value-of select="$teacher/name"/></a>
            </div>
        </xsl:if>
        <a class="sidebar-link"
                       href="{$school-path}/classes"
                       title="Классы">
                        <svg fill="none"
                             stroke="currentColor"
                             stroke-width="2"
                             viewBox="0 0 24 24"
                             xmlns="http://www.w3.org/2000/svg">
                            <path d="M22 10v6M2 10l10-5 10 5-10 5z"/>
                            <path d="M6 12v5c3 3 9 3 12 0v-5"/>
                        </svg>
                        <span class="sidebar-link-label">Классы</span>
                    </a>
        <xsl:if test="$clazz/id">
            <div class="sidebar-subnav">
                <a class="sidebar-subnav-link active" href="{$school-path}/classes/{$clazz/id}"
                   title="Перейти к классу"><xsl:value-of select="$clazz/name"/></a>
            </div>
        </xsl:if>
        <a class="sidebar-link"
                       href="{$school-path}/subjects"
                       title="Предметы">
                        <svg fill="none"
                             stroke="currentColor"
                             stroke-width="2"
                             viewBox="0 0 24 24"
                             xmlns="http://www.w3.org/2000/svg">
                            <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
                            <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
                        </svg>
                        <span class="sidebar-link-label">Предметы</span>
                    </a>
        <xsl:if test="$subject/id">
            <div class="sidebar-subnav">
                <a class="sidebar-subnav-link active" href="{$school-path}/subjects/{$subject/id}"
                   title="Перейти к предмету"><xsl:value-of select="$subject/name"/></a>
            </div>
        </xsl:if>
        <a class="sidebar-link"
                       href="{$school-path}/buildings"
                       title="Корпусы">
                        <svg fill="none"
                             stroke="currentColor"
                             stroke-width="2"
                             viewBox="0 0 24 24"
                             xmlns="http://www.w3.org/2000/svg">
                            <rect height="18" rx="2" width="20" x="2" y="3"/>
                            <line x1="2" x2="22" y1="9" y2="9"/>
                            <line x1="9" x2="9" y1="21" y2="9"/>
                        </svg>
                        <span class="sidebar-link-label">Корпусы</span>
                    </a>
        <xsl:if test="$building/id">
            <div class="sidebar-subnav">
                <a class="sidebar-subnav-link active" href="{$school-path}/buildings/{$building/id}"
                   title="Перейти к корпусу"><xsl:value-of select="$building/name"/></a>
                <a class="sidebar-link" href="{$school-path}/buildings/{$building/id}/cabinets" title="Кабинеты">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor"
                         stroke-width="2" viewBox="0 0 24 24">
                        <rect height="18" rx="1" width="12" x="6" y="3"/>
                        <circle cx="15" cy="12" r="0.8"/>
                    </svg>
                    <span class="sidebar-link-label">Кабинеты</span>
                </a>
                <xsl:if test="$cabinet/id">
                    <div class="sidebar-subnav">
                        <a class="sidebar-subnav-link active" title="Перейти к кабинету"
                           href="{$school-path}/buildings/{$building/id}/cabinets/{$cabinet/id}">
                            <xsl:value-of select="$cabinet/name"/>
                        </a>
                    </div>
                </xsl:if>
            </div>
        </xsl:if>
        </xsl:if>
    </xsl:template>
    <xsl:template name="name-sort">
        <xsl:param name="prefix"/>
        <xsl:param name="direction"/>
        <xsl:param name="label" select="'Название'"/>
        <xsl:param name="title" select="'Сортировать по названию'"/>
        <button type="button" title="{$title}" data-sort="name"
                aria-pressed="{$direction = 'ASC' or $direction = 'DESC'}">
            <xsl:attribute name="class">
                <xsl:value-of select="concat($prefix, '-sort')"/>
                <xsl:if test="$direction = 'ASC' or $direction = 'DESC'">
                    <xsl:text> active</xsl:text>
                </xsl:if>
            </xsl:attribute>
            <span><xsl:value-of select="$label"/></span>
            <span class="{$prefix}-sort-icon">
                <xsl:choose>
                    <xsl:when test="$direction = 'ASC'">↑</xsl:when>
                    <xsl:when test="$direction = 'DESC'">↓</xsl:when>
                    <xsl:otherwise>↕</xsl:otherwise>
                </xsl:choose>
            </span>
        </button>
    </xsl:template>
</xsl:stylesheet>
