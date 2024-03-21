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
import org.wso2.carbon.email.mgt.model.NotificationTemplate;
import org.wso2.carbon.identity.core.util.JdbcUtils;

import java.util.List;

import static org.wso2.carbon.email.mgt.constants.SQLConstants.*;

/**
 * This class is to perform CRUD operations for AppNotificationTemplate.
 */
public class AppNotificationTemplateDAO {

    public void addNotificationTemplate(NotificationTemplate notificationTemplate, String channelName, String applicationUuid, int tenantId) throws Exception {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeInsert(INSERT_APP_NOTIFICATION_TEMPLATE_SQL, (preparedStatement -> {
                preparedStatement.setString(1, notificationTemplate.getLocale());
                preparedStatement.setString(2, notificationTemplate.getSubject());
                preparedStatement.setString(3, notificationTemplate.getBody());
                preparedStatement.setString(4, notificationTemplate.getFooter());
                preparedStatement.setString(5, notificationTemplate.getContentType());
                preparedStatement.setString(6, notificationTemplate.getScenarioType());
                preparedStatement.setString(7, channelName);
                preparedStatement.setInt(8, tenantId);
                preparedStatement.setString(9, applicationUuid);
                preparedStatement.setInt(10, tenantId);
            }), notificationTemplate, false);
        } catch (DataAccessException e) {
            // todo: handle exception
            throw new Exception("Error while adding application notification template", e);
        }
    }

    public NotificationTemplate getNotificationTemplate(String locale, String scenarioName, String channelName, String applicationUuid, int tenantId) throws Exception {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        NotificationTemplate notificationTemplate;

        try {
            notificationTemplate = jdbcTemplate.fetchSingleRecord(GET_APP_NOTIFICATION_TEMPLATE_SQL,
                    (resultSet, rowNumber) -> new NotificationTemplate(resultSet.getString(1),
                            resultSet.getString(2), resultSet.getString(3),
                            resultSet.getString(4), locale, scenarioName),
                    preparedStatement -> {
                        preparedStatement.setString(1, locale);
                        preparedStatement.setString(2, scenarioName);
                        preparedStatement.setString(3, channelName);
                        preparedStatement.setInt(4, tenantId);
                        preparedStatement.setString(5, applicationUuid);
                        preparedStatement.setInt(6, tenantId);
                    });
        } catch (DataAccessException e) {
            // todo: handle exception
            throw new Exception("Error while retrieving application notification template", e);
        }

        return notificationTemplate;
    }

    public List<NotificationTemplate> listNotificationTemplates(String scenarioName, String channelName, String applicationUuid, int tenantId) throws Exception {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        List<NotificationTemplate> notificationTemplates;

        try {
            notificationTemplates = jdbcTemplate.executeQuery(LIST_APP_NOTIFICATION_TEMPLATES_BY_APP_SQL,
                    (resultSet, rowNumber) -> new NotificationTemplate(resultSet.getString(1),
                            resultSet.getString(2), resultSet.getString(3),
                            resultSet.getString(4), resultSet.getString(5), scenarioName),
                    preparedStatement -> {
                        preparedStatement.setString(1, scenarioName);
                        preparedStatement.setString(2, channelName);
                        preparedStatement.setInt(3, tenantId);
                        preparedStatement.setString(4, applicationUuid);
                        preparedStatement.setInt(5, tenantId);
                    });
        } catch (DataAccessException e) {
            // todo: handle exception
            throw new Exception("Error while listing application notification scenario", e);
        }

        return notificationTemplates;
    }

    public void updateNotificationTemplate(NotificationTemplate notificationTemplate, String scenarioName, String channelName, String applicationUuid, int tenantId) throws Exception {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(UPDATE_APP_NOTIFICATION_TEMPLATE_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(1, notificationTemplate.getSubject());
                        preparedStatement.setString(2, notificationTemplate.getBody());
                        preparedStatement.setString(3, notificationTemplate.getFooter());
                        preparedStatement.setString(4, notificationTemplate.getContentType());
                        preparedStatement.setString(5, notificationTemplate.getLocale());
                        preparedStatement.setString(6, scenarioName);
                        preparedStatement.setString(7, channelName);
                        preparedStatement.setInt(8, tenantId);
                        preparedStatement.setString(9, applicationUuid);
                        preparedStatement.setInt(10, tenantId);
                    });
        } catch (DataAccessException e) {
            // todo: handle exception
            throw new Exception("Error while update application notification template", e);
        }

    }

    public void removeNotificationTemplate(String locale, String scenarioName, String channelName, String applicationUuid, int tenantId) throws Exception {

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
            // todo: handle exception
            throw new Exception("Error while delete application notification template", e);
        }
    }

    public void removeNotificationTemplates(String scenarioName, String channelName, String applicationUuid, int tenantId) throws Exception {

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
            // todo: handle exception
            throw new Exception("Error while delete notification templates for scenario", e);
        }
    }
}
