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

package org.wso2.carbon.email.mgt.constants;

import org.wso2.carbon.email.mgt.dao.impl.NotificationTemplateDAOImpl;

public class NotificationTemplateConstants {

    private NotificationTemplateConstants() {

    }

    public static final String TENANT_ID = "TENANT_ID";
    public static final String PATH = "PATH";

    /**
     * SQL Queries used in {@link NotificationTemplateDAOImpl}
     */
    public static class SqlQueries {

        private SqlQueries() {

        }

        public static final String GET_NOTIFICATION_TEMPLATE_TYPES = "SELECT MAX(CASE WHEN REG_NAME = " +
                "'templateDisplayName' THEN REG_VALUE END) AS DISPLAY_NAME FROM REG_RESOURCE_PROPERTY INNER JOIN " +
                "REG_PROPERTY ON (REG_RESOURCE_PROPERTY.REG_PROPERTY_ID = REG_PROPERTY.REG_ID AND REG_RESOURCE_PROPERTY.REG_TENANT_ID = REG_PROPERTY.REG_TENANT_ID) WHERE REG_PATH_ID IN (SELECT REG_PATH_ID FROM REG_PATH WHERE REG_PATH_PARENT_ID IN (SELECT REG_PATH_ID FROM REG_PATH WHERE REG_PATH_VALUE = :PATH;)) AND REG_RESOURCE_NAME IS NULL AND REG_PROPERTY.REG_TENANT_ID = :TENANT_ID; GROUP BY REG_PATH_ID, REG_PROPERTY.REG_TENANT_ID";
    }
}