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
import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.identity.governance.IdentityMgtConstants;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.core.util.JdbcUtils;

import java.util.List;

import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.APP_ID;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.BODY;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.CHANNEL;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.CONTENT_TYPE;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.FOOTER;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.LOCALE;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.NAME;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.SUBJECT;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.TENANT_ID;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.DELETE_APP_NOTIFICATION_TEMPLATES_BY_SCENARIO_SQL;
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
                preparedStatement.setString(LOCALE, locale);
                preparedStatement.setString(SUBJECT, notificationTemplate.getSubject());
                preparedStatement.setString(BODY, notificationTemplate.getBody());
                preparedStatement.setString(FOOTER, notificationTemplate.getFooter());
                preparedStatement.setString(CONTENT_TYPE, notificationTemplate.getContentType());
                preparedStatement.setString(NAME, displayName);
                preparedStatement.setString(CHANNEL, channelName);
                preparedStatement.setInt(TENANT_ID, tenantId);
                preparedStatement.setString(APP_ID, applicationUuid);
                preparedStatement.setInt(TENANT_ID, tenantId);
            }), notificationTemplate, false);
        } catch (DataAccessException e) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                    I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ERROR_ADDING_TEMPLATE.getCode(),
                    I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            String message =
                    String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ERROR_ADDING_TEMPLATE.getMessage(),
                            displayName, locale, tenantId);
            throw new NotificationTemplateManagerServerException(code, message, e);
        }
    }

    public NotificationTemplate getNotificationTemplate(String locale, String scenarioName, String channelName,
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
                        notificationTemplateResult.setType(scenarioName);
                        notificationTemplateResult.setDisplayName(scenarioName);
                        return notificationTemplateResult;
                    },
                    preparedStatement -> {
                        preparedStatement.setString(LOCALE, locale);
                        preparedStatement.setString(NAME, scenarioName);
                        preparedStatement.setString(CHANNEL, channelName);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                        preparedStatement.setString(APP_ID, applicationUuid);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            String error = String.format(
                    IdentityMgtConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_TEMPLATE_FROM_REGISTRY.getMessage(),
                    scenarioName, locale, tenantId);
            throw new NotificationTemplateManagerServerException(
                    IdentityMgtConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_TEMPLATE_FROM_REGISTRY.getCode(),
                    error, e);
        }

        return notificationTemplate;
    }

    public List<NotificationTemplate> listNotificationTemplates(String scenarioName, String channelName,
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
                        notificationTemplateResult.setType(scenarioName);
                        notificationTemplateResult.setDisplayName(scenarioName);
                        return notificationTemplateResult;
                    },
                    preparedStatement -> {
                        preparedStatement.setString(NAME, scenarioName);
                        preparedStatement.setString(CHANNEL, channelName);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                        preparedStatement.setString(APP_ID, applicationUuid);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            String error = "Error when retrieving '%s' template type from %s tenant registry.";
            throw new NotificationTemplateManagerServerException(String.format(error, scenarioName, tenantId), e);
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
                        preparedStatement.setString(LOCALE, locale);
                        preparedStatement.setString(NAME, displayName);
                        preparedStatement.setString(CHANNEL, channelName);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                        preparedStatement.setString(APP_ID, applicationUuid);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            // TODO: Verify the error code (kept add error code due to backward compatibility)
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                    I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ERROR_ADDING_TEMPLATE.getCode(),
                    I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            String message =
                    String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ERROR_ADDING_TEMPLATE.getMessage(),
                            displayName, locale, tenantId);
            throw new NotificationTemplateManagerServerException(code, message, e);
        }

    }

    public void removeNotificationTemplate(String locale, String scenarioName, String channelName,
                                           String applicationUuid, int tenantId)
            throws NotificationTemplateManagerServerException {

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_APP_NOTIFICATION_TEMPLATE_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(LOCALE, locale);
                        preparedStatement.setString(NAME, scenarioName);
                        preparedStatement.setString(CHANNEL, channelName);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                        preparedStatement.setString(APP_ID, applicationUuid);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            String error = String.format("Error deleting %s:%s template from %s tenant registry.", scenarioName, locale,
                    tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        }
    }

    public void removeNotificationTemplates(String scenarioName, String channelName, String applicationUuid,
                                            int tenantId) throws NotificationTemplateManagerServerException {

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_APP_NOTIFICATION_TEMPLATES_BY_SCENARIO_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(NAME, scenarioName);
                        preparedStatement.setString(CHANNEL, channelName);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                        preparedStatement.setString(APP_ID, applicationUuid);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            String error =
                    String.format("Error deleting email template type %s from %s tenant.", scenarioName, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        }
    }
}
