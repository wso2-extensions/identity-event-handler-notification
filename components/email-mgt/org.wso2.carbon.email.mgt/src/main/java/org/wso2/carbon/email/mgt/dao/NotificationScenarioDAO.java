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

import java.util.List;

import static org.wso2.carbon.email.mgt.constants.SQLConstants.*;

/**
 * This class is to perform CRUD operations for NotificationScenario.
 */
public class NotificationScenarioDAO {

    public void addNotificationScenario(String UUID, String scenarioName, String channelName, int tenantId) throws Exception {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeInsert(INSERT_NOTIFICATION_SCENARIO_SQL, (preparedStatement -> {
                preparedStatement.setString(1, UUID);
                preparedStatement.setString(2, scenarioName);
                preparedStatement.setString(3, channelName);
                preparedStatement.setInt(4, tenantId);
            }), scenarioName, false);
        } catch (DataAccessException e) {
            // todo: handle exception
            throw new Exception("Error while adding notification scenario", e);
        }
    }

    public String getNotificationScenario(String scenarioId, String channelName, int tenantId) throws Exception {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        String scenarioName;

        try {
            scenarioName = jdbcTemplate.fetchSingleRecord(GET_NOTIFICATION_SCENARIO_SQL,
                    (resultSet, rowNumber) -> resultSet.getString(1),
                    preparedStatement -> {
                        preparedStatement.setString(1, scenarioId);
                        preparedStatement.setString(2, channelName);
                        preparedStatement.setInt(3, tenantId);
                    });
        } catch (DataAccessException e) {
            // todo: handle exception
            throw new Exception("Error while retrieving notification scenario", e);
        }

        return scenarioName;
    }

    public List<String> listNotificationScenarios(String channelName, int tenantId) throws Exception {

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
            // todo: handle exception
            throw new Exception("Error while listing notification scenario", e);
        }

        return scenarioNames;
    }

    public void removeNotificationScenario(String uuid, String channelName, int tenantId) throws Exception {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(DELETE_NOTIFICATION_SCENARIO_BY_ID_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(1, uuid);
                        preparedStatement.setString(2, channelName);
                        preparedStatement.setInt(3, tenantId);
                    });
        } catch (DataAccessException e) {
            // todo: handle exception
            throw new Exception("Error while delete notification scenario", e);
        }
    }
}
