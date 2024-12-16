/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.email.mgt.store.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.identity.core.util.JdbcUtils;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.List;

import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.BODY;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.CHANNEL;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.CONTENT;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.CONTENT_TYPE;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.CREATED_AT;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.FOOTER;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.ID;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.LOCALE;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.ORG_TEMPLATE_SCHEMA_VERSION;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.SUBJECT;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.TEMPLATE_KEY;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.TENANT_ID;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.TYPE_ID;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.TYPE_KEY;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.UPDATED_AT;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.VERSION;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.DELETE_ORG_NOTIFICATION_TEMPLATES_BY_TYPE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.DELETE_ORG_NOTIFICATION_TEMPLATE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.GET_NOTIFICATION_TYPE_ID_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.GET_ORG_NOTIFICATION_TEMPLATE_HYBRID_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.GET_ORG_NOTIFICATION_TEMPLATE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.GET_ORG_NOTIFICATION_TEMPLATE_WITHOUT_UNICODE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.INSERT_ORG_NOTIFICATION_TEMPLATE_HYBRID_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.INSERT_ORG_NOTIFICATION_TEMPLATE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.INSERT_ORG_NOTIFICATION_TEMPLATE_WITHOUT_UNICODE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.IS_ORG_NOTIFICATION_TEMPLATE_EXISTS_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.LIST_ORG_NOTIFICATION_TEMPLATES_BY_TYPE_HYBRID_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.LIST_ORG_NOTIFICATION_TEMPLATES_BY_TYPE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.LIST_ORG_NOTIFICATION_TEMPLATES_BY_TYPE_WITHOUT_UNICODE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.UPDATE_ORG_NOTIFICATION_TEMPLATE_HYBRID_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.UPDATE_ORG_NOTIFICATION_TEMPLATE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.UPDATE_ORG_NOTIFICATION_TEMPLATE_WITHOUT_UNICODE_SQL;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.CALENDER;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.getContentByteArray;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.getCurrentTime;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.setContent;

/**
 * This class is to perform CRUD operations for Org NotificationTemplates.
 */
public class OrgNotificationTemplateDAO {

    private static final Log log = LogFactory.getLog(OrgNotificationTemplateDAO.class);
    private boolean isUnicodeSupported = I18nMgtDataHolder.getInstance().isUnicodeSupported();
    private boolean isHybrid = I18nMgtDataHolder.getInstance().isHybrid();
    private List<String> debugTenants = I18nMgtDataHolder.getInstance().getDebugTenants();

    public void addNotificationTemplate(NotificationTemplate notificationTemplate, int tenantId)
            throws NotificationTemplateManagerServerException {

        String displayName = notificationTemplate.getDisplayName();
        String locale = notificationTemplate.getLocale();
        String channelName = notificationTemplate.getNotificationChannel();

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        byte[] contentByteArray = getContentByteArray(notificationTemplate);
        int contentLength = contentByteArray.length;
        try (InputStream contentStream = new ByteArrayInputStream(contentByteArray)) {
            String insertOrgNotificationTemplateSql = isUnicodeSupported ? INSERT_ORG_NOTIFICATION_TEMPLATE_SQL:
                    isHybrid ? INSERT_ORG_NOTIFICATION_TEMPLATE_HYBRID_SQL :
                            INSERT_ORG_NOTIFICATION_TEMPLATE_WITHOUT_UNICODE_SQL;
            namedJdbcTemplate.executeInsert(insertOrgNotificationTemplateSql, (preparedStatement -> {
                preparedStatement.setString(TEMPLATE_KEY, locale.toLowerCase());
                preparedStatement.setString(LOCALE, locale);
                if (isUnicodeSupported) {
                    preparedStatement.setBinaryStream(CONTENT, contentStream, contentLength);
                    Timestamp currentTime = getCurrentTime();
                    preparedStatement.setTimeStamp(CREATED_AT, currentTime, CALENDER);
                    preparedStatement.setTimeStamp(UPDATED_AT, currentTime, CALENDER);
                    preparedStatement.setString(VERSION, ORG_TEMPLATE_SCHEMA_VERSION);
                } else if (isHybrid) {
                    preparedStatement.setBinaryStream(CONTENT, contentStream, contentLength);
                    preparedStatement.setString(SUBJECT, notificationTemplate.getSubject());
                    preparedStatement.setString(BODY, notificationTemplate.getBody());
                    preparedStatement.setString(FOOTER, notificationTemplate.getFooter());
                } else {
                    preparedStatement.setString(SUBJECT, notificationTemplate.getSubject());
                    preparedStatement.setString(BODY, notificationTemplate.getBody());
                    preparedStatement.setString(FOOTER, notificationTemplate.getFooter());
                }
                preparedStatement.setString(CONTENT_TYPE, notificationTemplate.getContentType());
                preparedStatement.setString(TYPE_KEY, displayName.toLowerCase());
                preparedStatement.setString(CHANNEL, channelName);
                preparedStatement.setInt(TENANT_ID, tenantId);
                preparedStatement.setInt(TENANT_ID, tenantId);
            }), notificationTemplate, false);
        } catch (DataAccessException e) {
            String error =
                    String.format("Error while adding %s template %s of type %s to %s tenant.", channelName,
                            locale, displayName, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        } catch (IOException e) {
            throw new NotificationTemplateManagerServerException("Error while processing content stream.", e);
        }

        if (debugTenants.contains(String.valueOf(tenantId))) {
            log.info("[" + tenantId + "][NotificationTemplate][ADD] {"
                    + "subject: " + notificationTemplate.getSubject()
                    + ", footer: " + notificationTemplate.getFooter()
                    + ", body: " + notificationTemplate.getBody()
                    + "}");
        }
    }

    public NotificationTemplate getNotificationTemplate(String locale, String templateType, String channelName,
                                                        int tenantId)
            throws NotificationTemplateManagerServerException {

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        NotificationTemplate notificationTemplate;

        try {
            String getOrgNotificationTemplateSql = isUnicodeSupported ? GET_ORG_NOTIFICATION_TEMPLATE_SQL :
                    isHybrid ? GET_ORG_NOTIFICATION_TEMPLATE_HYBRID_SQL :
                            GET_ORG_NOTIFICATION_TEMPLATE_WITHOUT_UNICODE_SQL;
            notificationTemplate = namedJdbcTemplate.fetchSingleRecord(getOrgNotificationTemplateSql,
                    (resultSet, rowNumber) -> {
                        NotificationTemplate notificationTemplateResult = new NotificationTemplate();
                        if (isUnicodeSupported) {
                            setContent(resultSet.getBinaryStream(CONTENT), notificationTemplateResult);
                        } else if (isHybrid) {
                            setContent(resultSet.getBinaryStream(CONTENT), notificationTemplateResult);
                            if (notificationTemplateResult.getSubject() == null
                                    && notificationTemplateResult.getBody() == null
                                    && notificationTemplateResult.getFooter() == null) {
                                notificationTemplateResult.setSubject(resultSet.getString(SUBJECT));
                                notificationTemplateResult.setBody(resultSet.getString(BODY));
                                notificationTemplateResult.setFooter(resultSet.getString(FOOTER));
                            }
                        } else {
                            notificationTemplateResult.setSubject(resultSet.getString(SUBJECT));
                            notificationTemplateResult.setBody(resultSet.getString(BODY));
                            notificationTemplateResult.setFooter(resultSet.getString(FOOTER));
                        }
                        notificationTemplateResult.setContentType(resultSet.getString(CONTENT_TYPE));
                        notificationTemplateResult.setLocale(locale);
                        notificationTemplateResult.setType(templateType);
                        notificationTemplateResult.setDisplayName(templateType);
                        return notificationTemplateResult;
                    },
                    preparedStatement -> {
                        preparedStatement.setString(TEMPLATE_KEY, locale.toLowerCase());
                        preparedStatement.setString(TYPE_KEY, templateType.toLowerCase());
                        preparedStatement.setString(CHANNEL, channelName);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            String error =
                    String.format("Error while retrieving %s template %s of type %s from %s tenant.", channelName,
                            locale, templateType, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        }

        if (debugTenants.contains(String.valueOf(tenantId))) {
            if (notificationTemplate != null) {
                log.info("[" + tenantId + "][NotificationTemplate][GET] {"
                        + "subject: " + notificationTemplate.getSubject()
                        + ", footer: " + notificationTemplate.getFooter()
                        + ", body: " + notificationTemplate.getBody()
                        + "}");
            } else {
                log.info("[" + tenantId + "][NotificationTemplate][GET] {template not found}");
            }
        }

        return notificationTemplate;
    }

    public boolean isNotificationTemplateExists(String locale, String templateType, String channelName,
                                                        int tenantId)
            throws NotificationTemplateManagerServerException {

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();

        try {
            Integer typeId = namedJdbcTemplate.fetchSingleRecord(GET_NOTIFICATION_TYPE_ID_SQL,
                    (resultSet, rowNumber) -> resultSet.getInt(ID),
                    preparedStatement -> {
                        preparedStatement.setString(TYPE_KEY, templateType.toLowerCase());
                        preparedStatement.setString(CHANNEL, channelName);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });

            if (typeId == null) {
                return false;
            }

            Integer templateId = namedJdbcTemplate.fetchSingleRecord(IS_ORG_NOTIFICATION_TEMPLATE_EXISTS_SQL,
                    (resultSet, rowNumber) -> resultSet.getInt(ID),
                    preparedStatement -> {
                        preparedStatement.setString(TEMPLATE_KEY, locale.toLowerCase());
                        preparedStatement.setInt(TYPE_ID, typeId);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });

            return templateId != null;
        } catch (DataAccessException e) {
            String error =
                    String.format("Error while checking the existence of %s template %s of type %s from %s tenant.",
                            channelName, locale, templateType, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        }
    }

    public List<NotificationTemplate> listNotificationTemplates(String templateType, String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        List<NotificationTemplate> notificationTemplates;

        try {
            String listOrgNotificationTemplatesByTypeSql =
                    isUnicodeSupported ? LIST_ORG_NOTIFICATION_TEMPLATES_BY_TYPE_SQL :
                            isHybrid ? LIST_ORG_NOTIFICATION_TEMPLATES_BY_TYPE_HYBRID_SQL :
                                    LIST_ORG_NOTIFICATION_TEMPLATES_BY_TYPE_WITHOUT_UNICODE_SQL;
            notificationTemplates = namedJdbcTemplate.executeQuery(listOrgNotificationTemplatesByTypeSql,
                    (resultSet, rowNumber) -> {
                        NotificationTemplate notificationTemplateResult = new NotificationTemplate();
                        if (isUnicodeSupported) {
                            setContent(resultSet.getBinaryStream(CONTENT), notificationTemplateResult);
                        } else if (isHybrid) {
                            setContent(resultSet.getBinaryStream(CONTENT), notificationTemplateResult);
                            if (notificationTemplateResult.getSubject() == null
                                    && notificationTemplateResult.getBody() == null
                                    && notificationTemplateResult.getFooter() == null) {
                                notificationTemplateResult.setSubject(resultSet.getString(SUBJECT));
                                notificationTemplateResult.setBody(resultSet.getString(BODY));
                                notificationTemplateResult.setFooter(resultSet.getString(FOOTER));
                            }
                        } else {
                            notificationTemplateResult.setSubject(resultSet.getString(SUBJECT));
                            notificationTemplateResult.setBody(resultSet.getString(BODY));
                            notificationTemplateResult.setFooter(resultSet.getString(FOOTER));
                        }
                        notificationTemplateResult.setContentType(resultSet.getString(CONTENT_TYPE));
                        notificationTemplateResult.setLocale(resultSet.getString(LOCALE));
                        notificationTemplateResult.setType(templateType.toLowerCase());
                        notificationTemplateResult.setDisplayName(templateType);
                        return notificationTemplateResult;
                    },
                    preparedStatement -> {
                        preparedStatement.setString(TYPE_KEY, templateType.toLowerCase());
                        preparedStatement.setString(CHANNEL, channelName);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            String error =
                    String.format("Error while listing %s templates of type %s from %s tenant.", channelName,
                            templateType, tenantId);
            throw new NotificationTemplateManagerServerException(String.format(error, templateType, tenantId), e);
        }

        return notificationTemplates;
    }

    public void updateNotificationTemplate(NotificationTemplate notificationTemplate, int tenantId)
            throws NotificationTemplateManagerServerException {

        String displayName = notificationTemplate.getDisplayName();
        String locale = notificationTemplate.getLocale();
        String channelName = notificationTemplate.getNotificationChannel();

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        byte[] contentByteArray = getContentByteArray(notificationTemplate);
        int contentLength = contentByteArray.length;
        try (InputStream contentStream = new ByteArrayInputStream(contentByteArray)) {
            String updateOrgNotificationTemplateSql = isUnicodeSupported ? UPDATE_ORG_NOTIFICATION_TEMPLATE_SQL :
                    isHybrid ? UPDATE_ORG_NOTIFICATION_TEMPLATE_HYBRID_SQL :
                            UPDATE_ORG_NOTIFICATION_TEMPLATE_WITHOUT_UNICODE_SQL;
            namedJdbcTemplate.executeUpdate(updateOrgNotificationTemplateSql,
                    preparedStatement -> {
                        if (isUnicodeSupported) {
                            preparedStatement.setBinaryStream(CONTENT, contentStream, contentLength);
                            preparedStatement.setTimeStamp(UPDATED_AT, getCurrentTime(), CALENDER);
                        } else if (isHybrid) {
                            preparedStatement.setBinaryStream(CONTENT, contentStream, contentLength);
                            preparedStatement.setString(SUBJECT, notificationTemplate.getSubject());
                            preparedStatement.setString(BODY, notificationTemplate.getBody());
                            preparedStatement.setString(FOOTER, notificationTemplate.getFooter());
                        } else {
                            preparedStatement.setString(SUBJECT, notificationTemplate.getSubject());
                            preparedStatement.setString(BODY, notificationTemplate.getBody());
                            preparedStatement.setString(FOOTER, notificationTemplate.getFooter());
                        }
                        preparedStatement.setString(CONTENT_TYPE, notificationTemplate.getContentType());
                        preparedStatement.setString(TEMPLATE_KEY, locale.toLowerCase());
                        preparedStatement.setString(TYPE_KEY, displayName.toLowerCase());
                        preparedStatement.setString(CHANNEL, channelName);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                        preparedStatement.setInt(TENANT_ID, tenantId);

                        Timestamp currentTime = getCurrentTime();
                        preparedStatement.setTimeStamp(UPDATED_AT, currentTime, CALENDER);
                    });
        } catch (DataAccessException e) {
            String error =
                    String.format("Error while updating %s template %s of type %s from %s tenant.", channelName, locale,
                            displayName, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        } catch (IOException e) {
            throw new NotificationTemplateManagerServerException("Error while processing content stream.", e);
        }

        if (debugTenants.contains(String.valueOf(tenantId))) {
            log.info("[" + tenantId + "][NotificationTemplate][UPDATE] {"
                    + "subject: " + notificationTemplate.getSubject()
                    + ", footer: " + notificationTemplate.getFooter()
                    + ", body: " + notificationTemplate.getBody()
                    + "}");
        }

    }

    public void removeNotificationTemplate(String locale, String templateType, String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_ORG_NOTIFICATION_TEMPLATE_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(TEMPLATE_KEY, locale.toLowerCase());
                        preparedStatement.setString(TYPE_KEY, templateType.toLowerCase());
                        preparedStatement.setString(CHANNEL, channelName);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            String error =
                    String.format("Error while deleting %s template %s of type %s from %s tenant.", channelName, locale,
                            templateType, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        }
    }

    public void removeNotificationTemplates(String templateType, String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_ORG_NOTIFICATION_TEMPLATES_BY_TYPE_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(TYPE_KEY, templateType.toLowerCase());
                        preparedStatement.setString(CHANNEL, channelName);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            String error =
                    String.format("Error while deleting %s templates of type %s from %s tenant.", channelName,
                            templateType, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        }
    }
}
