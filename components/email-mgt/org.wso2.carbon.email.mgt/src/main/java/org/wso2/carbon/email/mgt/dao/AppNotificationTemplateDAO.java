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

package org.wso2.carbon.email.mgt.dao;

import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.core.util.JdbcUtils;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;

import java.util.List;

import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.APP_ID;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.BODY;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.CHANNEL;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.CONTENT_TYPE;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.FOOTER;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.LOCALE;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.TEMPLATE_KEY;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.SUBJECT;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.TENANT_ID;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.TYPE_KEY;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.DELETE_APP_NOTIFICATION_TEMPLATES_BY_TYPE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.DELETE_APP_NOTIFICATION_TEMPLATE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.GET_APP_NOTIFICATION_TEMPLATE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.INSERT_APP_NOTIFICATION_TEMPLATE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.LIST_APP_NOTIFICATION_TEMPLATES_BY_APP_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.UPDATE_APP_NOTIFICATION_TEMPLATE_SQL;

/**
 * This class is to perform CRUD operations for Application NotificationTemplates.
 */
public class AppNotificationTemplateDAO {

    public void addNotificationTemplate(NotificationTemplate notificationTemplate, String applicationUuid, int tenantId)
            throws NotificationTemplateManagerServerException {

        String displayName = notificationTemplate.getDisplayName();
        String locale = notificationTemplate.getLocale();
        String channelName = notificationTemplate.getNotificationChannel();

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.executeInsert(INSERT_APP_NOTIFICATION_TEMPLATE_SQL, (preparedStatement -> {
                preparedStatement.setString(TEMPLATE_KEY, locale.toLowerCase());
                preparedStatement.setString(LOCALE, locale);
                preparedStatement.setString(SUBJECT, notificationTemplate.getSubject());
                preparedStatement.setString(BODY, notificationTemplate.getBody());
                preparedStatement.setString(FOOTER, notificationTemplate.getFooter());
                preparedStatement.setString(CONTENT_TYPE, notificationTemplate.getContentType());
                preparedStatement.setString(TYPE_KEY, displayName.toLowerCase());
                preparedStatement.setString(CHANNEL, channelName);
                preparedStatement.setInt(TENANT_ID, tenantId);
                preparedStatement.setString(APP_ID, applicationUuid);
                preparedStatement.setInt(TENANT_ID, tenantId);
            }), notificationTemplate, false);
        } catch (DataAccessException e) {
            String error =
                    String.format("Error while adding %s template %s of type %s to application %s in %s tenant.",
                            channelName, locale, displayName, applicationUuid, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
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
                        notificationTemplateResult.setSubject(resultSet.getString(SUBJECT));
                        notificationTemplateResult.setBody(resultSet.getString(BODY));
                        notificationTemplateResult.setFooter(resultSet.getString(FOOTER));
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

    public List<NotificationTemplate> listNotificationTemplates(String templateType, String channelName,
                                                                String applicationUuid, int tenantId)
            throws NotificationTemplateManagerServerException {

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        List<NotificationTemplate> notificationTemplates;

        try {
            notificationTemplates = namedJdbcTemplate.executeQuery(LIST_APP_NOTIFICATION_TEMPLATES_BY_APP_SQL,
                    (resultSet, rowNumber) -> {
                        NotificationTemplate notificationTemplateResult = new NotificationTemplate();
                        notificationTemplateResult.setSubject(resultSet.getString(SUBJECT));
                        notificationTemplateResult.setBody(resultSet.getString(BODY));
                        notificationTemplateResult.setFooter(resultSet.getString(FOOTER));
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

        String displayName = notificationTemplate.getDisplayName();
        String locale = notificationTemplate.getLocale();
        String channelName = notificationTemplate.getNotificationChannel();

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.executeUpdate(UPDATE_APP_NOTIFICATION_TEMPLATE_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(SUBJECT, notificationTemplate.getSubject());
                        preparedStatement.setString(BODY, notificationTemplate.getBody());
                        preparedStatement.setString(FOOTER, notificationTemplate.getFooter());
                        preparedStatement.setString(CONTENT_TYPE, notificationTemplate.getContentType());
                        preparedStatement.setString(TEMPLATE_KEY, locale.toLowerCase());
                        preparedStatement.setString(TYPE_KEY, displayName.toLowerCase());
                        preparedStatement.setString(CHANNEL, channelName);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                        preparedStatement.setString(APP_ID, applicationUuid);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            String error =
                    String.format("Error while updating %s template %s of type %s from application %s in %s tenant.",
                            channelName, locale, displayName, applicationUuid, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
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
}
