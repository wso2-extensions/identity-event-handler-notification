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

import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.APP_ID;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.APP_TEMPLATE_SCHEMA_VERSION;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.CHANNEL;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.CONTENT;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.CONTENT_TYPE;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.CREATED_AT;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.ID;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.LOCALE;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.TEMPLATE_KEY;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.TENANT_ID;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.TYPE_ID;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.TYPE_KEY;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.UPDATED_AT;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.VERSION;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.DELETE_ALL_APP_NOTIFICATION_TEMPLATES_BY_TYPE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.DELETE_APP_NOTIFICATION_TEMPLATES_BY_TYPE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.DELETE_APP_NOTIFICATION_TEMPLATE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.GET_APP_NOTIFICATION_TEMPLATE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.GET_NOTIFICATION_TYPE_ID_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.INSERT_APP_NOTIFICATION_TEMPLATE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.IS_APP_NOTIFICATION_TEMPLATE_EXISTS_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.LIST_APP_NOTIFICATION_TEMPLATES_BY_APP_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.UPDATE_APP_NOTIFICATION_TEMPLATE_SQL;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.CALENDER;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.getContentByteArray;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.getCurrentTime;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.setContent;

/**
 * This class is to perform CRUD operations for Application NotificationTemplates.
 */
public class AppNotificationTemplateDAO {

    public void addNotificationTemplate(NotificationTemplate notificationTemplate, String applicationUuid, int tenantId)
            throws NotificationTemplateManagerServerException {

        String templateType = notificationTemplate.getType();
        String locale = notificationTemplate.getLocale();
        String channelName = notificationTemplate.getNotificationChannel();

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        byte[] contentByteArray = getContentByteArray(notificationTemplate);
        int contentLength = contentByteArray.length;
        try (InputStream contentStream = new ByteArrayInputStream(contentByteArray)) {
            namedJdbcTemplate.executeInsert(INSERT_APP_NOTIFICATION_TEMPLATE_SQL, (preparedStatement -> {
                preparedStatement.setString(TEMPLATE_KEY, locale.toLowerCase());
                preparedStatement.setString(LOCALE, locale);
                preparedStatement.setBinaryStream(CONTENT, contentStream, contentLength);
                preparedStatement.setString(CONTENT_TYPE, notificationTemplate.getContentType());
                preparedStatement.setString(TYPE_KEY, templateType.toLowerCase());
                preparedStatement.setString(CHANNEL, channelName);
                preparedStatement.setInt(TENANT_ID, tenantId);
                preparedStatement.setString(APP_ID, applicationUuid);
                preparedStatement.setInt(TENANT_ID, tenantId);

                Timestamp currentTime = getCurrentTime();
                preparedStatement.setTimeStamp(CREATED_AT, currentTime, CALENDER);
                preparedStatement.setTimeStamp(UPDATED_AT, currentTime, CALENDER);
                preparedStatement.setString(VERSION, APP_TEMPLATE_SCHEMA_VERSION);
            }), notificationTemplate, false);
        } catch (DataAccessException e) {
            String error =
                    String.format("Error while adding %s template %s of type %s to application %s in %s tenant.",
                            channelName, locale, templateType, applicationUuid, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        } catch (IOException e) {
            throw new NotificationTemplateManagerServerException("Error while processing content stream.", e);
        }
    }

    public NotificationTemplate getNotificationTemplate(String locale, String templateType, String channelName,
                                                        String applicationUuid, int tenantId)
            throws NotificationTemplateManagerServerException {

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        NotificationTemplate notificationTemplate;

        try {
            notificationTemplate = namedJdbcTemplate.fetchSingleRecord(GET_APP_NOTIFICATION_TEMPLATE_SQL,
                    (resultSet, rowNumber) -> {
                        NotificationTemplate notificationTemplateResult = new NotificationTemplate();
                        setContent(resultSet.getBinaryStream(CONTENT), notificationTemplateResult);
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
                        preparedStatement.setString(APP_ID, applicationUuid);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            String error =
                    String.format("Error while retrieving %s template %s of type %s from application %s in %s tenant.",
                            channelName, locale, templateType, applicationUuid, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        }

        return notificationTemplate;
    }

    public boolean isNotificationTemplateExists(String locale, String templateType, String channelName,
                                                        String applicationUuid, int tenantId)
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

            Integer templateId = namedJdbcTemplate.fetchSingleRecord(IS_APP_NOTIFICATION_TEMPLATE_EXISTS_SQL,
                    (resultSet, rowNumber) -> resultSet.getInt(ID),
                    preparedStatement -> {
                        preparedStatement.setString(TEMPLATE_KEY, locale.toLowerCase());
                        preparedStatement.setInt(TYPE_ID, typeId);
                        preparedStatement.setString(APP_ID, applicationUuid);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });
            return templateId != null;
        } catch (DataAccessException e) {
            String error =
                    String.format(
                            "Error while checking the existence of %s template %s of type %s from application %s " +
                                    "in %s tenant.",
                            channelName, locale, templateType, applicationUuid, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        }
    }

    public List<NotificationTemplate> listNotificationTemplates(String templateType, String channelName,
                                                                String applicationUuid, int tenantId)
            throws NotificationTemplateManagerServerException {

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        List<NotificationTemplate> notificationTemplates;

        try {
            notificationTemplates = namedJdbcTemplate.executeQuery(LIST_APP_NOTIFICATION_TEMPLATES_BY_APP_SQL,
                    (resultSet, rowNumber) -> {
                        NotificationTemplate notificationTemplateResult = new NotificationTemplate();
                        setContent(resultSet.getBinaryStream(CONTENT), notificationTemplateResult);
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
                        preparedStatement.setString(APP_ID, applicationUuid);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            String error =
                    String.format("Error while listing %s templates of type %s from application %s in %s tenant.",
                            channelName, templateType, applicationUuid, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        }

        return notificationTemplates;
    }

    public void updateNotificationTemplate(NotificationTemplate notificationTemplate, String applicationUuid,
                                           int tenantId) throws NotificationTemplateManagerServerException {

        String templateType = notificationTemplate.getType();
        String locale = notificationTemplate.getLocale();
        String channelName = notificationTemplate.getNotificationChannel();

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        byte[] contentByteArray = getContentByteArray(notificationTemplate);
        int contentLength = contentByteArray.length;
        try (InputStream contentStream = new ByteArrayInputStream(contentByteArray)) {
            namedJdbcTemplate.executeUpdate(UPDATE_APP_NOTIFICATION_TEMPLATE_SQL,
                    preparedStatement -> {
                        preparedStatement.setBinaryStream(CONTENT, contentStream, contentLength);
                        preparedStatement.setString(CONTENT_TYPE, notificationTemplate.getContentType());
                        preparedStatement.setString(TEMPLATE_KEY, locale.toLowerCase());
                        preparedStatement.setString(TYPE_KEY, templateType.toLowerCase());
                        preparedStatement.setString(CHANNEL, channelName);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                        preparedStatement.setString(APP_ID, applicationUuid);
                        preparedStatement.setInt(TENANT_ID, tenantId);

                        preparedStatement.setTimeStamp(UPDATED_AT, getCurrentTime(), CALENDER);
                    });
        } catch (DataAccessException e) {
            String error =
                    String.format("Error while updating %s template %s of type %s from application %s in %s tenant.",
                            channelName, locale, templateType, applicationUuid, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        } catch (IOException e) {
            throw new NotificationTemplateManagerServerException("Error while processing content stream.", e);
        }

    }

    public void removeNotificationTemplate(String locale, String templateType, String channelName,
                                           String applicationUuid, int tenantId)
            throws NotificationTemplateManagerServerException {

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_APP_NOTIFICATION_TEMPLATE_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(TEMPLATE_KEY, locale.toLowerCase());
                        preparedStatement.setString(TYPE_KEY, templateType.toLowerCase());
                        preparedStatement.setString(CHANNEL, channelName);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                        preparedStatement.setString(APP_ID, applicationUuid);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            String error =
                    String.format("Error while deleting %s template %s of type %s from application %s in %s tenant.",
                            channelName, locale, templateType, applicationUuid, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        }
    }

    public void removeNotificationTemplates(String templateType, String channelName, String applicationUuid,
                                            int tenantId) throws NotificationTemplateManagerServerException {

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_APP_NOTIFICATION_TEMPLATES_BY_TYPE_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(TYPE_KEY, templateType.toLowerCase());
                        preparedStatement.setString(CHANNEL, channelName);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                        preparedStatement.setString(APP_ID, applicationUuid);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            String error =
                    String.format("Error while deleting %s templates of type %s from application %s in %s tenant.",
                            channelName, templateType, applicationUuid, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        }
    }

    public void removeAllNotificationTemplates(String templateType, String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_ALL_APP_NOTIFICATION_TEMPLATES_BY_TYPE_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(TYPE_KEY, templateType.toLowerCase());
                        preparedStatement.setString(CHANNEL, channelName);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            String error =
                    String.format("Error while deleting %s templates of type %s from all applications in %s tenant.",
                            channelName, templateType, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        }
    }
}
