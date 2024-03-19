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
 * This class is to perform CRUD operations for OrgNotificationTemplate.
 */
public class OrgNotificationTemplateDAO {

    public void addNotificationTemplate(NotificationTemplate notificationTemplate, int tenantId) throws Exception {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeInsert(INSERT_ORG_NOTIFICATION_TEMPLATE_SQL, (preparedStatement -> {
                preparedStatement.setString(1, notificationTemplate.getLocale());
                preparedStatement.setString(2, notificationTemplate.getSubject());
                preparedStatement.setString(3, notificationTemplate.getBody());
                preparedStatement.setString(4, notificationTemplate.getFooter());
                preparedStatement.setString(5, notificationTemplate.getContentType());
                preparedStatement.setInt(6, resolveScenarioId(notificationTemplate.getScenarioType()));
                preparedStatement.setInt(7, tenantId);
            }), notificationTemplate, false);
        } catch (DataAccessException e) {
            // todo: handle exception
            throw new Exception("Error while adding notification template", e);
        }
    }

    public NotificationTemplate getNotificationTemplate(String locale, String scenarioName, int tenantId) throws Exception {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        NotificationTemplate notificationTemplate;

        try {
            notificationTemplate = jdbcTemplate.fetchSingleRecord(GET_ORG_NOTIFICATION_TEMPLATE_SQL,
                    (resultSet, rowNumber) -> new NotificationTemplate(resultSet.getString(1),
                            resultSet.getString(2), resultSet.getString(3),
                            resultSet.getString(4), locale, scenarioName),
                    preparedStatement -> {
                        preparedStatement.setString(1, locale);
                        preparedStatement.setInt(2, resolveScenarioId(scenarioName));
                        preparedStatement.setInt(3, tenantId);
                    });
        } catch (DataAccessException e) {
            // todo: handle exception
            throw new Exception("Error while retrieving notification template", e);
        }

        return notificationTemplate;
    }

    public List<NotificationTemplate> listNotificationTemplates(String scenarioName, int tenantId) throws Exception {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        List<NotificationTemplate> notificationTemplates;

        try {
            notificationTemplates = jdbcTemplate.executeQuery(LIST_ORG_NOTIFICATION_TEMPLATES_BY_SCENARIO_SQL,
                    (resultSet, rowNumber) -> new NotificationTemplate(resultSet.getString(1),
                            resultSet.getString(2), resultSet.getString(3),
                            resultSet.getString(4), resultSet.getString(5), scenarioName),
                    preparedStatement -> {
                        preparedStatement.setInt(1, resolveScenarioId(scenarioName));
                        preparedStatement.setInt(2, tenantId);
                    });
        } catch (DataAccessException e) {
            // todo: handle exception
            throw new Exception("Error while listing notification scenario", e);
        }

        return notificationTemplates;
    }

    public void updateNotificationTemplate(NotificationTemplate notificationTemplate, String scenarioName, int tenantId) throws Exception {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(UPDATE_ORG_NOTIFICATION_TEMPLATE_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(1, notificationTemplate.getSubject());
                        preparedStatement.setString(2, notificationTemplate.getBody());
                        preparedStatement.setString(3, notificationTemplate.getFooter());
                        preparedStatement.setString(4, notificationTemplate.getContentType());
                        preparedStatement.setString(5, notificationTemplate.getLocale());
                        preparedStatement.setInt(6, resolveScenarioId(notificationTemplate.getScenarioType()));
                        preparedStatement.setInt(7, tenantId);
                    });
        } catch (DataAccessException e) {
            // todo: handle exception
            throw new Exception("Error while update notification template", e);
        }

    }

    public void removeNotificationTemplate(String locale, String scenarioName, int tenantId) throws Exception {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(DELETE_ORG_NOTIFICATION_TEMPLATE_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(1, locale);
                        preparedStatement.setInt(1, resolveScenarioId(scenarioName));
                        preparedStatement.setInt(2, tenantId);
                    });
        } catch (DataAccessException e) {
            // todo: handle exception
            throw new Exception("Error while delete notification template", e);
        }
    }

    private int resolveScenarioId(String scenarioType) {
        return -1234;
    }
}
