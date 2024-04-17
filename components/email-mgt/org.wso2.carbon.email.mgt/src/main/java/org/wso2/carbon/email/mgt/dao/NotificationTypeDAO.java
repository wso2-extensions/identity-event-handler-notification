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

import java.util.List;

import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.CHANNEL;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.NAME;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.TENANT_ID;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.TYPE_KEY;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.DELETE_NOTIFICATION_TYPE_BY_ID_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.GET_NOTIFICATION_TYPE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.INSERT_NOTIFICATION_TYPE_SQL;
import static org.wso2.carbon.email.mgt.constants.SQLConstants.LIST_NOTIFICATION_TYPES_SQL;

/**
 * This class is to perform CRUD operations for Notification Types.
 */
public class NotificationTypeDAO {

    public void addNotificationTemplateType(String type, String displayName, String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.executeInsert(INSERT_NOTIFICATION_TYPE_SQL, (preparedStatement -> {
                preparedStatement.setString(TYPE_KEY, type);
                preparedStatement.setString(NAME, displayName);
                preparedStatement.setString(CHANNEL, channelName);
                preparedStatement.setInt(TENANT_ID, tenantId);
            }), displayName, false);
        } catch (DataAccessException e) {
            String error =
                    String.format("Error while adding the %s template type %s to %s tenant.", channelName, type,
                            tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        }
    }

    public String getNotificationTemplateType(String type, String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        String displayName;

        try {
            displayName = namedJdbcTemplate.fetchSingleRecord(GET_NOTIFICATION_TYPE_SQL,
                    (resultSet, rowNumber) -> resultSet.getString(1),
                    preparedStatement -> {
                        preparedStatement.setString(TYPE_KEY, type);
                        preparedStatement.setString(CHANNEL, channelName);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            String error =
                    String.format("Error while retrieving the %s template type %s from %s tenant.", channelName, type,
                            tenantId);
            throw new NotificationTemplateManagerServerException(error, e);
        }

        return displayName;
    }

    public List<String> listNotificationTemplateTypes(String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        List<String> displayNames;

        try {
            displayNames = namedJdbcTemplate.executeQuery(LIST_NOTIFICATION_TYPES_SQL,
                    (resultSet, rowNumber) -> resultSet.getString(1),
                    preparedStatement -> {
                        preparedStatement.setString(CHANNEL, channelName);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            String errorMsg =
                    String.format("Error while listing %s template types from %s tenant.", channelName, tenantId);
            throw new NotificationTemplateManagerServerException(errorMsg, e);
        }

        return displayNames;
    }

    public void deleteNotificationTemplateType(String type, String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_NOTIFICATION_TYPE_BY_ID_SQL,
                    preparedStatement -> {
                        preparedStatement.setString(TYPE_KEY, type);
                        preparedStatement.setString(CHANNEL, channelName);
                        preparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            String errorMsg =
                    String.format("Error while deleting %s template type %s from %s tenant.", channelName, type,
                            tenantId);
            throw new NotificationTemplateManagerServerException(errorMsg, e);
        }
    }
}
