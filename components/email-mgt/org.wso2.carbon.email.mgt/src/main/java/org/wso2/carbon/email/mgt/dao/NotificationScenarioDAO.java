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
import org.wso2.carbon.identity.core.util.JdbcUtils;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;

import java.util.List;

import static org.wso2.carbon.email.mgt.constants.SQLConstants.*;

/**
 * This class is to perform CRUD operations for NotificationScenario.
 */
public class NotificationScenarioDAO {

    public void addNotificationScenario(String uuid, String scenarioName, String channelName, int tenantId) throws NotificationTemplateManagerServerException {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeInsert(INSERT_NOTIFICATION_SCENARIO_SQL, (preparedStatement -> {
                preparedStatement.setString(1, uuid);
                preparedStatement.setString(2, scenarioName);
                preparedStatement.setString(3, channelName);
                preparedStatement.setInt(4, tenantId);
            }), scenarioName, false);
        } catch (DataAccessException e) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_TEMPLATE.getCode(), I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            String message = String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_TEMPLATE.getMessage(), uuid, tenantId);
            throw new NotificationTemplateManagerServerException(code, message);
        }
    }

    public String getNotificationScenario(String uuid, String channelName, int tenantId) throws NotificationTemplateManagerServerException {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        String scenarioName;

        try {
            scenarioName = jdbcTemplate.fetchSingleRecord(GET_NOTIFICATION_SCENARIO_SQL,
                    (resultSet, rowNumber) -> resultSet.getString(1),
                    preparedStatement -> {
                        preparedStatement.setString(1, uuid);
                        preparedStatement.setString(2, channelName);
                        preparedStatement.setInt(3, tenantId);
                    });
        } catch (DataAccessException e) {
            String error = String.format("Error while checking the existence of the template type %s for channel %s of %s tenant.", uuid, channelName, tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        }

        return scenarioName;
    }

    public List<String> listNotificationScenarios(String channelName, int tenantId) throws NotificationTemplateManagerServerException {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        List<String> scenarioNames;

        try {
            scenarioNames = jdbcTemplate.executeQuery(LIST_NOTIFICATION_SCENARIOS_SQL,
                    (resultSet, rowNumber) -> resultSet.getString(1),
                    preparedStatement -> {
                        preparedStatement.setString(1, channelName);
                        preparedStatement.setInt(2, tenantId);
                    });
        } catch (DataAccessException e) {
            String errorMsg = String.format("Error when retrieving email template types of %s tenant.", tenantId);
            throw new NotificationTemplateManagerServerException(errorMsg, e);
        }

        return scenarioNames;
    }

    public void removeNotificationScenario(String uuid, String channelName, int tenantId) throws NotificationTemplateManagerServerException {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(DELETE_NOTIFICATION_SCENARIO_BY_ID_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(1, uuid);
                        preparedStatement.setString(2, channelName);
                        preparedStatement.setInt(3, tenantId);
                    });
        } catch (DataAccessException e) {
            String errorMsg = String.format("Error deleting email template type %s for channel %s from %s tenant.", uuid, channelName, tenantId);
            throw new NotificationTemplateManagerServerException(errorMsg, e);
        }
    }
}
