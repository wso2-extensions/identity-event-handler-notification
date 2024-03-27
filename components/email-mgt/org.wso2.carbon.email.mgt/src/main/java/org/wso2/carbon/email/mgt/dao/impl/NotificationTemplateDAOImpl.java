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

package org.wso2.carbon.email.mgt.dao.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.database.utils.jdbc.NamedPreparedStatement;
import org.wso2.carbon.email.mgt.constants.NotificationTemplateConstants;
import org.wso2.carbon.email.mgt.dao.NotificationTemplateDAO;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NotificationTemplateDAOImpl implements NotificationTemplateDAO {

    private static final Log log = LogFactory.getLog(NotificationTemplateDAOImpl.class);

    /**
     * @inheritDoc
     */
    @Override
    public List<String> getNotificationTemplateTypes(String path, int tenantID)
            throws NotificationTemplateManagerException {

        List<String> templateTypes = new ArrayList<>();

        try (Connection connection = IdentityDatabaseUtil.getGovernanceDBConnection(false)) {
            try (NamedPreparedStatement statement = new NamedPreparedStatement(connection,
                    NotificationTemplateConstants.SqlQueries.GET_NOTIFICATION_TEMPLATE_TYPES)) {
                statement.setString(NotificationTemplateConstants.PATH, path);
                statement.setInt(NotificationTemplateConstants.TENANT_ID, tenantID);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        templateTypes.add(resultSet.getString(1));
                    }
                }
            } catch (SQLException e) {
                throw new NotificationTemplateManagerException("Error while retrieving notification template types.",
                        e);
            }
        } catch (SQLException e) {
            throw new NotificationTemplateManagerException("Error while retrieving notification template types.", e);
        }

        return templateTypes;
    }
}
