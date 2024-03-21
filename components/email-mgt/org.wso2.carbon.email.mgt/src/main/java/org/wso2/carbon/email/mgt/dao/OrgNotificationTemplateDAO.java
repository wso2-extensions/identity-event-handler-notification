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
import org.wso2.carbon.identity.core.util.JdbcUtils;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;

import java.util.List;

import static org.wso2.carbon.email.mgt.constants.SQLConstants.*;

/**
 * This class is to perform CRUD operations for Org NotificationTemplates.
 */
public class OrgNotificationTemplateDAO {

    public void addNotificationTemplate(NotificationTemplate notificationTemplate, String channelName, int tenantId) throws Exception {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeInsert(INSERT_ORG_NOTIFICATION_TEMPLATE_SQL, (preparedStatement -> {
                preparedStatement.setString(1, notificationTemplate.getLocale());
                preparedStatement.setString(2, notificationTemplate.getSubject());
                preparedStatement.setString(3, notificationTemplate.getBody());
                preparedStatement.setString(4, notificationTemplate.getFooter());
                preparedStatement.setString(5, notificationTemplate.getContentType());
                preparedStatement.setString(6, notificationTemplate.getDisplayName());
                preparedStatement.setString(7, channelName);
                preparedStatement.setInt(8, tenantId);
                preparedStatement.setInt(9, tenantId);
            }), notificationTemplate, false);
        } catch (DataAccessException e) {
            // todo: handle exception
            throw new Exception("Error while adding notification template", e);
        }
    }

    public NotificationTemplate getNotificationTemplate(String locale, String scenarioName, String channelName, int tenantId) throws Exception {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        NotificationTemplate notificationTemplate;

        try {
            notificationTemplate = jdbcTemplate.fetchSingleRecord(GET_ORG_NOTIFICATION_TEMPLATE_SQL,
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
                        preparedStatement.setInt(5, tenantId);
                    });
        } catch (DataAccessException e) {
            // todo: handle exception
            throw new Exception("Error while retrieving notification template", e);
        }

        return notificationTemplate;
    }

    public List<NotificationTemplate> listNotificationTemplates(String scenarioName, String channelName, int tenantId) throws Exception {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        List<NotificationTemplate> notificationTemplates;

        try {
            notificationTemplates = jdbcTemplate.executeQuery(LIST_ORG_NOTIFICATION_TEMPLATES_BY_SCENARIO_SQL,
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
                        preparedStatement.setInt(4, tenantId);
                    });
        } catch (DataAccessException e) {
            // todo: handle exception
            throw new Exception("Error while listing notification scenario", e);
        }

        return notificationTemplates;
    }

    public void updateNotificationTemplate(NotificationTemplate notificationTemplate, String channelName, int tenantId) throws Exception {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(UPDATE_ORG_NOTIFICATION_TEMPLATE_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(1, notificationTemplate.getSubject());
                        preparedStatement.setString(2, notificationTemplate.getBody());
                        preparedStatement.setString(3, notificationTemplate.getFooter());
                        preparedStatement.setString(4, notificationTemplate.getContentType());
                        preparedStatement.setString(5, notificationTemplate.getLocale());
                        preparedStatement.setString(6, notificationTemplate.getDisplayName());
                        preparedStatement.setString(7, channelName);
                        preparedStatement.setInt(8, tenantId);
                        preparedStatement.setInt(9, tenantId);
                    });
        } catch (DataAccessException e) {
            // todo: handle exception
            throw new Exception("Error while update notification template", e);
        }

    }

    public void removeNotificationTemplate(String locale, String scenarioName, String channelName, int tenantId) throws Exception {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(DELETE_ORG_NOTIFICATION_TEMPLATE_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(1, locale);
                        preparedStatement.setString(2, scenarioName);
                        preparedStatement.setString(3, channelName);
                        preparedStatement.setInt(4, tenantId);
                        preparedStatement.setInt(5, tenantId);
                    });
        } catch (DataAccessException e) {
            // todo: handle exception
            throw new Exception("Error while delete notification template", e);
        }
    }

    public void removeNotificationTemplates(String scenarioName, String channelName, int tenantId) throws Exception {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(DELETE_ORG_NOTIFICATION_TEMPLATES_BY_SCENARIO_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(1, scenarioName);
                        preparedStatement.setString(2, channelName);
                        preparedStatement.setInt(3, tenantId);
                        preparedStatement.setInt(4, tenantId);
                    });
        } catch (DataAccessException e) {
            // todo: handle exception
            throw new Exception("Error while delete notification templates for scenario", e);
        }
    }
}
