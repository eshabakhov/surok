<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:import href="school-common.xsl"/>
    <xsl:output method="html" encoding="UTF-8" omit-xml-declaration="yes" doctype-system="about:legacy-compat"/>
    <xsl:param name="view"/>
    <xsl:key name="load-subject" match="draft/loads/load/subject" use="id"/>
    <xsl:variable name="draft" select="/surok/draft"/>
    <xsl:variable name="editable" select="/surok/editable = 'true'"/>
    <xsl:variable name="schedule" select="/surok/schedule"/>
    <xsl:variable name="base" select="concat($school-path, '/schedules/', $schedule/id, '/loads/draft')"/>
    <xsl:variable name="chosen"><xsl:choose>
        <xsl:when test="$draft/curriculum/id"><xsl:value-of select="$draft/curriculum/id"/></xsl:when>
        <xsl:otherwise><xsl:value-of select="/surok/offers/curriculum[1]/id"/></xsl:otherwise>
    </xsl:choose></xsl:variable>
    <xsl:variable name="classes" select="/surok/classes/school-class[id = $draft/additions/addition/classes/class/id]"/>
    <xsl:variable name="columns" select="/surok/subjects/subject | $draft/loads/load/subject[not(id = /surok/subjects/subject/id) and generate-id() = generate-id(key('load-subject', id)[1])]"/>

    <xsl:template match="/">
        <xsl:choose>
            <xsl:when test="$view = 'draft-fragment'"><xsl:call-template name="workspace"/></xsl:when>
            <xsl:otherwise><xsl:call-template name="page"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template name="page-title">Автозаполнение нагрузки</xsl:template>
    <xsl:template name="styles"><link rel="stylesheet" href="/css/school/load-draft.css"/></xsl:template>
    <xsl:template name="scripts"><script type="module" src="/js/school/load-draft.js"></script></xsl:template>
    <xsl:template name="content">
        <nav class="breadcrumbs" aria-label="Навигация">
            <a href="{$school-path}/schedules/{$schedule/id}" title="Перейти к расписанию"><xsl:value-of select="$schedule/name"/></a>
            <span>/</span><a href="{$school-path}/schedules/{$schedule/id}/loads" title="Учебная нагрузка">Учебная нагрузка</a>
        </nav>
        <div class="page-title-row"><div><h1>Автозаполнение нагрузки</h1><p class="page-subtitle"><xsl:value-of select="$schedule/name"/></p></div></div>
        <noscript><p class="error-message">Для редактирования и сохранения черновика включите JavaScript.</p></noscript>
        <xsl:call-template name="workspace"/>
    </xsl:template>

    <xsl:template name="workspace">
        <section id="load-draft" class="load-draft" data-uri="{$base}" data-saved="{/surok/saved}" data-editable="{$editable}" data-curriculum="{$draft/curriculum/id}">
            <p>План и дополнительные часы сохраняются в черновике. Фактическая нагрузка изменится после нажатия «Применить нагрузку».</p>
            <xsl:if test="not($editable)"><p class="draft-notice">Доступен только просмотр. Для изменения нагрузки нужны права составителя.</p></xsl:if>
            <div id="draft-status" class="draft-notice draft-status" role="status" aria-live="polite"></div>
            <div id="draft-error" class="error-message" role="alert" hidden="hidden"></div>
            <form id="draft-form" class="draft-form" action="{$base}" method="post">
                <xsl:call-template name="csrf-input"/>
                <section class="draft-section" aria-labelledby="draft-plan-heading">
                    <h2 id="draft-plan-heading">Федеральный учебный план</h2>
                    <xsl:choose>
                        <xsl:when test="/surok/offers/curriculum">
                            <label for="draft-curriculum">Выберите план для всех классов соответствующего уровня</label>
                            <select id="draft-curriculum" class="draft-curriculum" name="curriculumId" required="required">
                                <xsl:for-each select="/surok/offers/curriculum"><option value="{id}">
                                    <xsl:if test="id = string($chosen)"><xsl:attribute name="selected">selected</xsl:attribute></xsl:if>
                                    <xsl:value-of select="concat(title, ' · ', year, ' · версия ', version)"/>
                                </option></xsl:for-each>
                            </select>
                            <xsl:if test="$editable and /surok/saved = 'true'">
                                <div id="draft-switch-notice" class="draft-notice" hidden="hidden">
                                    <p>Вы просматриваете другой план. Обязательные часы и классы будут определены новым планом. Дополнительные часы по параллелям потребуется задать заново.</p>
                                    <button id="draft-switch" class="btn-outline-action" type="button" disabled="disabled">Использовать этот план</button>
                                </div>
                            </xsl:if>
                        </xsl:when>
                        <xsl:otherwise><p class="draft-notice">В справочнике пока нет доступных федеральных планов.</p></xsl:otherwise>
                    </xsl:choose>
                    <xsl:for-each select="/surok/offers/curriculum"><xsl:call-template name="offer"/></xsl:for-each>
                </section>
                <xsl:if test="$draft/unavailable/requirement">
                    <div class="error-message" role="alert">Некоторые обязательные предметы недоступны в федеральном справочнике:
                        <ul><xsl:for-each select="$draft/unavailable/requirement">
                            <li><xsl:value-of select="grade"/> класс — <xsl:value-of select="subject/name"/></li>
                        </xsl:for-each></ul>
                        <a href="/federal/subjects" title="Федеральные предметы">Открыть справочник</a>
                        <p>Будет заполнена доступная часть нагрузки.</p>
                    </div>
                </xsl:if>
                <xsl:call-template name="additions"/>
                <xsl:call-template name="matrix"/>
                <xsl:if test="$editable"><div class="draft-toolbar">
                    <button id="draft-save" type="submit" class="btn-primary" disabled="disabled">Сохранить и пересчитать</button>
                    <button id="draft-apply" type="button" class="btn-outline-action" disabled="disabled">Применить нагрузку</button>
                    <span id="draft-state" class="draft-state">Изменения пока не применены к расписанию</span>
                </div></xsl:if>
            </form>
        </section>
    </xsl:template>

    <xsl:template name="offer">
                <section class="draft-offer" data-plan="{id}">
            <xsl:if test="id != string($chosen)"><xsl:attribute name="hidden">hidden</xsl:attribute></xsl:if>
            <p class="draft-meta"><xsl:value-of select="/surok/levels/level[code = current()/level]/title"/><xsl:text> · </xsl:text><xsl:value-of select="/surok/weeks/week[code = current()/week]/title"/></p>
            <xsl:if test="description != ''"><p><xsl:value-of select="description"/></p></xsl:if>
            <details class="draft-requirements"><summary>Состав плана и часы по классам (<xsl:value-of select="count(requirements/requirement)"/>)</summary>
                <div class="data-table-wrap"><table class="shadcn-table"><caption class="draft-sr-only">Требования федерального плана</caption>
                    <thead><tr><th scope="col">Параллель</th><th scope="col">Предмет</th><th scope="col">Часов в неделю</th><th scope="col">Часть плана</th></tr></thead>
                    <tbody><xsl:for-each select="requirements/requirement"><tr>
                        <td><xsl:value-of select="grade"/></td>
                        <td><xsl:choose>
                            <xsl:when test="partType = 'OPTIONAL'">Все дополнительные предметы</xsl:when>
                            <xsl:otherwise><xsl:value-of select="subject/name"/></xsl:otherwise>
                        </xsl:choose></td>
                        <td><xsl:if test="partType = 'OPTIONAL'">До </xsl:if><xsl:value-of select="hours"/></td>
                        <td><xsl:choose><xsl:when test="partType = 'MANDATORY'">Обязательная</xsl:when><xsl:otherwise>Вариативная</xsl:otherwise></xsl:choose></td>
                    </tr></xsl:for-each></tbody>
                </table></div>
            </details>
        </section>
    </xsl:template>

    <xsl:template name="additions">
        <xsl:if test="$draft">
            <section class="draft-section" aria-labelledby="draft-additions-heading">
                <h2 id="draft-additions-heading">Вариативная нагрузка по параллелям</h2>
                <p>Задайте часы дополнительных предметов вручную. Они применяются к каждому классу параллели, включая новые классы. Индивидуально назначенные дополнительные предметы также учитываются в лимите.</p>
                <xsl:for-each select="$draft/additions/addition">
                    <xsl:variable name="grade" select="grade"/>
                    <xsl:variable name="addition" select="."/>
                    <fieldset class="draft-options" data-settings="true" data-grade="{grade}">
                        <xsl:if test="not($editable)"><xsl:attribute name="disabled">disabled</xsl:attribute></xsl:if>
                        <legend><xsl:value-of select="grade"/>-я параллель</legend>
                        <p>Максимум на каждый класс: <xsl:choose>
                            <xsl:when test="requirements/requirement/hours"><strong><xsl:value-of select="requirements/requirement/hours"/> ч.</strong></xsl:when>
                            <xsl:otherwise><strong>не задан в плане</strong></xsl:otherwise>
                        </xsl:choose></p>
                        <div class="draft-addition-list">
                            <xsl:for-each select="/surok/subjects/subject[origin = 'SCHOOL'] | subjects/subject[not(id = /surok/subjects/subject/id)]">
                                <xsl:variable name="subject" select="id"/>
                                <label class="draft-addition" for="draft-addition-{$grade}-{id}">
                                    <span><xsl:value-of select="name"/></span>
                                    <input id="draft-addition-{$grade}-{id}" type="number" min="0" max="2147483647" step="1" required="required" data-addition="{$grade}" data-subject="{id}">
                                        <xsl:attribute name="value"><xsl:choose>
                                            <xsl:when test="$addition/subjects/subject[id = $subject]"><xsl:value-of select="$addition/subjects/subject[id = $subject]/hours"/></xsl:when>
                                            <xsl:otherwise>0</xsl:otherwise>
                                        </xsl:choose></xsl:attribute>
                                    </input>
                                </label>
                            </xsl:for-each>
                        </div>
                        <p>Ноль исключает предмет из назначения на параллель. Индивидуальную нагрузку можно изменить на <a href="{$school-path}/schedules/{$schedule/id}/loads" title="Открыть учебную нагрузку">странице нагрузки</a>.</p>
                        <ul><xsl:for-each select="classes/class">
                            <li><strong><xsl:value-of select="name"/></strong>: сейчас <xsl:value-of select="hours/current"/> ч., после применения <xsl:value-of select="hours/planned"/> ч.
                                <xsl:if test="hours/planned &gt; $addition/requirements/requirement/hours or (hours/planned &gt; 0 and not($addition/requirements/requirement/hours))">
                                    <span class="draft-difference"> — превышен лимит или максимум не задан</span>
                                </xsl:if>
                            </li>
                        </xsl:for-each></ul>
                    </fieldset>
                </xsl:for-each>
                <xsl:if test="not(/surok/subjects/subject)"><p>Дополнительных предметов пока нет. <a href="{$school-path}/subjects" title="Открыть дополнительные предметы">Добавьте предметы</a>.</p></xsl:if>
            </section>
        </xsl:if>
    </xsl:template>

    <xsl:template name="matrix">
        <section class="draft-section" aria-labelledby="draft-matrix-heading">
            <h2 id="draft-matrix-heading">Планируемая нагрузка</h2>
            <xsl:if test="$draft"><p>Сохранённый план: <strong><xsl:value-of select="$draft/curriculum/title"/></strong></p></xsl:if>
            <xsl:choose>
                <xsl:when test="$classes and $columns">
                    <p>Классы подходящего уровня появляются автоматически. Обязательные часы применяются по плану; дополнительные часы задаются выше для всей параллели. Индивидуальная дополнительная нагрузка учитывается в вариативной части.</p>
                    <fieldset class="draft-matrix-fields" data-settings="true">
                        <xsl:if test="not($editable)"><xsl:attribute name="disabled">disabled</xsl:attribute></xsl:if>
                        <legend class="draft-sr-only">Рассчитанные часы по классам</legend>
                        <div class="data-table-wrap draft-table-wrap" tabindex="0" role="region" aria-label="Нагрузка по классам и предметам">
                            <table class="shadcn-table draft-table"><caption class="draft-sr-only">Черновик учебной нагрузки</caption>
                                <thead><tr><th scope="col">Класс</th><xsl:for-each select="$columns"><th scope="col"><xsl:value-of select="name"/></th></xsl:for-each><th scope="col">Итого</th></tr></thead>
                                <tbody><xsl:for-each select="$classes">
                                    <xsl:variable name="class" select="id"/>
                                    <tr data-row="{id}"><th scope="row"><xsl:value-of select="name"/></th>
                                        <xsl:for-each select="$columns">
                                            <xsl:variable name="subject" select="id"/>
                                            <xsl:call-template name="cell"><xsl:with-param name="class" select="$class"/><xsl:with-param name="subject" select="$subject"/></xsl:call-template>
                                        </xsl:for-each>
                                        <td data-total="true"><xsl:value-of select="sum($draft/loads/load[schoolClass/id = $class]/hours/planned)"/></td>
                                    </tr>
                                </xsl:for-each></tbody>
                            </table>
                        </div>
                    </fieldset>
                </xsl:when>
                <xsl:when test="not(/surok/subjects/subject)"><p>В школе пока нет предметов. <a href="{$school-path}/subjects" title="Предметы школы">Добавьте предметы</a>.</p></xsl:when>
                <xsl:otherwise><p>Сохраните выбранный план: все классы подходящего уровня появятся автоматически.</p></xsl:otherwise>
            </xsl:choose>
        </section>
    </xsl:template>

    <xsl:template name="cell">
        <xsl:param name="class"/><xsl:param name="subject"/>
        <xsl:variable name="load" select="$draft/loads/load[schoolClass/id = $class and subject/id = $subject]"/>
        <xsl:variable name="grade" select="$classes[id = $class]/grade"/>
        <xsl:variable name="additional" select="$load/subject/origin = 'SCHOOL'"/>
        <td><xsl:if test="$load"><div class="draft-cell" data-class="{$class}" data-subject="{$subject}" data-current="{$load/hours/current}" data-proposed="{$load/hours/proposed}">
            <strong><xsl:value-of select="$load/hours/planned"/> ч.</strong>
            <span class="draft-hours-note">Сейчас: <xsl:value-of select="$load/hours/current"/> ч.</span>
            <xsl:choose>
                <xsl:when test="$load/hours/proposed &gt; 0"><span>Обязательная по плану</span></xsl:when>
                <xsl:when test="$additional">
                    <span>Вариативная</span>
                    <xsl:if test="$load/hours/current &gt; 0 and not($draft/additions/addition[grade = $grade]/subjects/subject[id = $subject])">
                        <span class="draft-hours-note">Назначена отдельно классу; учитывается в вариативном лимите</span>
                    </xsl:if>
                </xsl:when>
                <xsl:otherwise><span class="draft-unplanned">Вне обязательной части выбранного плана</span></xsl:otherwise>
            </xsl:choose>
            <xsl:if test="$load/hours/current != $load/hours/planned"><span class="draft-difference">Будет изменено при применении</span></xsl:if>
        </div></xsl:if></td>
    </xsl:template>
</xsl:stylesheet>
