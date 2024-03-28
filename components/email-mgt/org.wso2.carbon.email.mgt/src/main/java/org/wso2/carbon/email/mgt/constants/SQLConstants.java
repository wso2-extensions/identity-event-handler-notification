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

public class SQLConstants {

    // sql constants for notification scenario
    public static final String INSERT_NOTIFICATION_SCENARIO_SQL =
            "INSERT INTO IDN_NOTIFICATION_SCENARIO " +
                    "(UUID, NAME, CHANNEL, TENANT_ID) VALUES (:UUID;, :NAME;, :CHANNEL;, :TENANT_ID;)";
    public static final String GET_NOTIFICATION_SCENARIO_SQL =
            "SELECT NAME FROM IDN_NOTIFICATION_SCENARIO " +
                    "WHERE UUID = :UUID; AND CHANNEL = :CHANNEL; AND TENANT_ID = :TENANT_ID;";
    public static final String LIST_NOTIFICATION_SCENARIOS_SQL =
            "SELECT UUID, NAME, CHANNEL FROM IDN_NOTIFICATION_SCENARIO WHERE CHANNEL = :CHANNEL; AND TENANT_ID = :TENANT_ID;";
    public static final String DELETE_NOTIFICATION_SCENARIO_BY_ID_SQL =
            "DELETE FROM IDN_NOTIFICATION_SCENARIO " +
                    "WHERE UUID = :UUID; AND CHANNEL = :CHANNEL; AND TENANT_ID = :TENANT_ID;";
    public static final String GET_NOTIFICATION_SCENARIO_ID_SQL =
            "SELECT ID FROM IDN_NOTIFICATION_SCENARIO " +
                    "WHERE UUID = :UUID; AND CHANNEL = :CHANNEL; AND TENANT_ID = :TENANT_ID;";

    // sql constants for org notification template
    public static final String INSERT_ORG_NOTIFICATION_TEMPLATE_SQL =
            "INSERT INTO IDN_ORG_NOTIFICATION_TEMPLATE " +
                    "(LOCALE, SUBJECT, BODY, FOOTER, CONTENT_TYPE, SCENARIO_ID, TENANT_ID) " +
                    "VALUES (:LOCALE;, :SUBJECT;, :BODY;, :FOOTER;, :CONTENT_TYPE;, (" +
                    GET_NOTIFICATION_SCENARIO_ID_SQL + "), :TENANT_ID;)";
    public static final String GET_ORG_NOTIFICATION_TEMPLATE_SQL =
            "SELECT SUBJECT, BODY, FOOTER, CONTENT_TYPE FROM IDN_ORG_NOTIFICATION_TEMPLATE " +
                    "WHERE LOCALE = :LOCALE; AND SCENARIO_ID = (" + GET_NOTIFICATION_SCENARIO_ID_SQL +
                    ") AND TENANT_ID = :TENANT_ID;";
    public static final String LIST_ORG_NOTIFICATION_TEMPLATES_BY_SCENARIO_SQL =
            "SELECT SUBJECT, BODY, FOOTER, CONTENT_TYPE, LOCALE FROM IDN_ORG_NOTIFICATION_TEMPLATE " +
                    "WHERE SCENARIO_ID = (" + GET_NOTIFICATION_SCENARIO_ID_SQL + ") AND TENANT_ID = :TENANT_ID;";
    public static final String UPDATE_ORG_NOTIFICATION_TEMPLATE_SQL =
            "UPDATE IDN_ORG_NOTIFICATION_TEMPLATE " +
                    "SET SUBJECT = :SUBJECT;, BODY = :BODY;, FOOTER = :FOOTER;, CONTENT_TYPE = :CONTENT_TYPE; " +
                    "WHERE LOCALE = :LOCALE; AND SCENARIO_ID = (" + GET_NOTIFICATION_SCENARIO_ID_SQL +
                    ") AND TENANT_ID = :TENANT_ID;";
    public static final String DELETE_ORG_NOTIFICATION_TEMPLATE_SQL =
            "DELETE FROM IDN_ORG_NOTIFICATION_TEMPLATE WHERE LOCALE = :LOCALE; AND SCENARIO_ID = (" +
                    GET_NOTIFICATION_SCENARIO_ID_SQL + ") AND TENANT_ID = :TENANT_ID;";
    public static final String DELETE_ORG_NOTIFICATION_TEMPLATES_BY_SCENARIO_SQL =
            "DELETE FROM IDN_ORG_NOTIFICATION_TEMPLATE WHERE SCENARIO_ID = (" + GET_NOTIFICATION_SCENARIO_ID_SQL +
                    ") AND TENANT_ID = :TENANT_ID;";

    // sql constants for app notification template
    public static final String INSERT_APP_NOTIFICATION_TEMPLATE_SQL =
            "INSERT INTO IDN_APP_NOTIFICATION_TEMPLATE " +
                    "(LOCALE, SUBJECT, BODY, FOOTER, CONTENT_TYPE, SCENARIO_ID, APP_ID, TENANT_ID) " +
                    "VALUES (:LOCALE;, :SUBJECT;, :BODY;, :FOOTER;, :CONTENT_TYPE;, (" +
                    GET_NOTIFICATION_SCENARIO_ID_SQL + "), :APP_ID;, :TENANT_ID;)";
    public static final String GET_APP_NOTIFICATION_TEMPLATE_SQL =
            "SELECT SUBJECT, BODY, FOOTER, CONTENT_TYPE FROM IDN_APP_NOTIFICATION_TEMPLATE " +
                    "WHERE LOCALE = :LOCALE; AND SCENARIO_ID = (" + GET_NOTIFICATION_SCENARIO_ID_SQL +
                    ") AND APP_ID = :APP_ID; AND TENANT_ID = :TENANT_ID;";
    public static final String LIST_APP_NOTIFICATION_TEMPLATES_BY_APP_SQL =
            "SELECT SUBJECT, BODY, FOOTER, CONTENT_TYPE, LOCALE FROM IDN_APP_NOTIFICATION_TEMPLATE " +
                    "WHERE SCENARIO_ID = (" + GET_NOTIFICATION_SCENARIO_ID_SQL +
                    ") AND APP_ID = :APP_ID; AND TENANT_ID = :TENANT_ID;";
    public static final String UPDATE_APP_NOTIFICATION_TEMPLATE_SQL =
            "UPDATE IDN_APP_NOTIFICATION_TEMPLATE " +
                    "SET SUBJECT = :SUBJECT;, BODY = :BODY;, FOOTER = :FOOTER;, CONTENT_TYPE = :CONTENT_TYPE; " +
                    "WHERE LOCALE = :LOCALE; AND SCENARIO_ID = (" + GET_NOTIFICATION_SCENARIO_ID_SQL +
                    ") AND APP_ID = :APP_ID; AND TENANT_ID = :TENANT_ID;";
    public static final String DELETE_APP_NOTIFICATION_TEMPLATE_SQL =
            "DELETE FROM IDN_APP_NOTIFICATION_TEMPLATE WHERE LOCALE = :LOCALE; AND SCENARIO_ID = (" +
                    GET_NOTIFICATION_SCENARIO_ID_SQL + ") AND APP_ID = :APP_ID; AND TENANT_ID = :TENANT_ID;";
    public static final String DELETE_APP_NOTIFICATION_TEMPLATES_BY_SCENARIO_SQL =
            "DELETE FROM IDN_ORG_NOTIFICATION_TEMPLATE WHERE SCENARIO_ID = (" + GET_NOTIFICATION_SCENARIO_ID_SQL +
                    ") AND APP_ID = :APP_ID; AND TENANT_ID = :TENANT_ID;";

}
