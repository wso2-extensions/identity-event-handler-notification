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

import org.wso2.carbon.database.utils.jdbc.JdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.identity.governance.IdentityMgtConstants;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.core.util.JdbcUtils;

import java.util.List;

import static org.wso2.carbon.email.mgt.constants.SQLConstants.*;

/**
 * This class is to perform CRUD operations for Application NotificationTemplates.
 */
public class AppNotificationTemplateDAO {

    public void addNotificationTemplate(NotificationTemplate notificationTemplate, String channelName, String applicationUuid, int tenantId) throws NotificationTemplateManagerServerException {

        String displayName = notificationTemplate.getDisplayName();
        String locale = notificationTemplate.getLocale();

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeInsert(INSERT_APP_NOTIFICATION_TEMPLATE_SQL, (preparedStatement -> {
                preparedStatement.setString(1, locale);
                preparedStatement.setString(2, notificationTemplate.getSubject());
                preparedStatement.setString(3, notificationTemplate.getBody());
                preparedStatement.setString(4, notificationTemplate.getFooter());
                preparedStatement.setString(5, notificationTemplate.getContentType());
                preparedStatement.setString(6, displayName);
                preparedStatement.setString(7, channelName);
                preparedStatement.setInt(8, tenantId);
                preparedStatement.setString(9, applicationUuid);
                preparedStatement.setInt(10, tenantId);
            }), notificationTemplate, false);
        } catch (DataAccessException e) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ERROR_ADDING_TEMPLATE.getCode(), I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            String message = String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ERROR_ADDING_TEMPLATE.getMessage(), displayName, locale, tenantId);
            throw new NotificationTemplateManagerServerException(code, message, e);
        }
    }

    public NotificationTemplate getNotificationTemplate(String locale, String scenarioName, String channelName, String applicationUuid, int tenantId) throws NotificationTemplateManagerServerException {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        NotificationTemplate notificationTemplate;

        try {
            notificationTemplate = jdbcTemplate.fetchSingleRecord(GET_APP_NOTIFICATION_TEMPLATE_SQL,
                    (resultSet, rowNumber) -> {
                        NotificationTemplate notificationTemplateResult = new NotificationTemplate();
                        notificationTemplateResult.setSubject(resultSet.getString(1));
                        notificationTemplateResult.setBody(resultSet.getString(2));
                        notificationTemplateResult.setFooter(resultSet.getString(3));
                        notificationTemplateResult.setContentType(resultSet.getString(4));
                        notificationTemplateResult.setLocale(locale);
                        notificationTemplateResult.setType(scenarioName);
                        notificationTemplateResult.setDisplayName(scenarioName);
                        return notificationTemplateResult;
                    },
                    preparedStatement -> {
                        preparedStatement.setString(1, locale);
                        preparedStatement.setString(2, scenarioName);
                        preparedStatement.setString(3, channelName);
                        preparedStatement.setInt(4, tenantId);
                        preparedStatement.setString(5, applicationUuid);
                        preparedStatement.setInt(6, tenantId);
                    });
        } catch (DataAccessException e) {
            String error = String.format(IdentityMgtConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_TEMPLATE_FROM_REGISTRY.getMessage(), scenarioName, locale, tenantId);
            throw new NotificationTemplateManagerServerException(IdentityMgtConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_TEMPLATE_FROM_REGISTRY.getCode(), error, e);
        }

        return notificationTemplate;
    }

    public List<NotificationTemplate> listNotificationTemplates(String scenarioName, String channelName, String applicationUuid, int tenantId) throws NotificationTemplateManagerServerException {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        List<NotificationTemplate> notificationTemplates;

        try {
            notificationTemplates = jdbcTemplate.executeQuery(LIST_APP_NOTIFICATION_TEMPLATES_BY_APP_SQL,
                    (resultSet, rowNumber) -> {
                        NotificationTemplate notificationTemplateResult = new NotificationTemplate();
                        notificationTemplateResult.setSubject(resultSet.getString(1));
                        notificationTemplateResult.setBody(resultSet.getString(2));
                        notificationTemplateResult.setFooter(resultSet.getString(3));
                        notificationTemplateResult.setContentType(resultSet.getString(4));
                        notificationTemplateResult.setLocale(resultSet.getString(5));
                        notificationTemplateResult.setType(scenarioName);
                        notificationTemplateResult.setDisplayName(scenarioName);
                        return notificationTemplateResult;
                    },
                    preparedStatement -> {
                        preparedStatement.setString(1, scenarioName);
                        preparedStatement.setString(2, channelName);
                        preparedStatement.setInt(3, tenantId);
                        preparedStatement.setString(4, applicationUuid);
                        preparedStatement.setInt(5, tenantId);
                    });
        } catch (DataAccessException e) {
            String error = "Error when retrieving '%s' template type from %s tenant registry.";
            throw new NotificationTemplateManagerServerException(String.format(error, scenarioName, tenantId), e);
        }

        return notificationTemplates;
    }

    public void updateNotificationTemplate(NotificationTemplate notificationTemplate, String channelName, String applicationUuid, int tenantId) throws NotificationTemplateManagerServerException {

        String displayName = notificationTemplate.getDisplayName();
        String locale = notificationTemplate.getLocale();

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(UPDATE_APP_NOTIFICATION_TEMPLATE_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(1, notificationTemplate.getSubject());
                        preparedStatement.setString(2, notificationTemplate.getBody());
                        preparedStatement.setString(3, notificationTemplate.getFooter());
                        preparedStatement.setString(4, notificationTemplate.getContentType());
                        preparedStatement.setString(5, locale);
                        preparedStatement.setString(6, displayName);
                        preparedStatement.setString(7, channelName);
                        preparedStatement.setInt(8, tenantId);
                        preparedStatement.setString(9, applicationUuid);
                        preparedStatement.setInt(10, tenantId);
                    });
        } catch (DataAccessException e) {
            // TODO: Verify the error code (kept add error code due to backward compatibility)
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ERROR_ADDING_TEMPLATE.getCode(), I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            String message = String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ERROR_ADDING_TEMPLATE.getMessage(), displayName, locale, tenantId);
            throw new NotificationTemplateManagerServerException(code, message, e);
        }

    }

    public void removeNotificationTemplate(String locale, String scenarioName, String channelName, String applicationUuid, int tenantId) throws NotificationTemplateManagerServerException {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(DELETE_APP_NOTIFICATION_TEMPLATE_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(1, locale);
                        preparedStatement.setString(2, scenarioName);
                        preparedStatement.setString(3, channelName);
                        preparedStatement.setInt(4, tenantId);
                        preparedStatement.setString(5, applicationUuid);
                        preparedStatement.setInt(6, tenantId);
                    });
        } catch (DataAccessException e) {
            String error = String.format("Error deleting %s:%s template from %s tenant registry.", scenarioName, locale, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        }
    }

    public void removeNotificationTemplates(String scenarioName, String channelName, String applicationUuid, int tenantId) throws NotificationTemplateManagerServerException {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(DELETE_APP_NOTIFICATION_TEMPLATES_BY_SCENARIO_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(1, scenarioName);
                        preparedStatement.setString(2, channelName);
                        preparedStatement.setInt(3, tenantId);
                        preparedStatement.setString(4, applicationUuid);
                        preparedStatement.setInt(5, tenantId);
                    });
        } catch (DataAccessException e) {
            String error = String.format("Error deleting email template type %s from %s tenant.", scenarioName, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        }
    }
}
