/*
 * Copyright (c) 2016-2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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

import org.wso2.carbon.utils.CarbonUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * i18n management related constants
 */
public class I18nMgtConstants {

    private I18nMgtConstants() {}

    public static final String EMAIL_TEMPLATE_PATH = "/identity/email";
    public static final String APP_TEMPLATE_PATH = "/apps";
    public static final String SMS_TEMPLATE_PATH = "/identity/sms";
    public static final String EMAIL_CONF_DIRECTORY = "email";
    public static final String SMS_CONF_DIRECTORY = "sms";
    public static final String EMAIL_ADMIN_CONF_FILE = "email-admin-config.xml";
    public static final String SMS_TEMPLAE_ADMIN_CONF_FILE = "sms-templates-admin-config.xml";
    public static final String DEFAULT_EMAIL_LOCALE = "en_us";
    public static final String DEFAULT_SMS_NOTIFICATION_LOCALE = "en_us";

    public static final String EMAIL_TEMPLATE_NAME = "templateName";
    public static final String EMAIL_TEMPLATE_TYPE_DISPLAY_NAME = "templateDisplayName";

    // Constants related to email template meta data properties.
    public static final String TEMPLATE_TYPE = "type";
    public static final String TEMPLATE_TYPE_DISPLAY_NAME = "display";
    public static final String TEMPLATE_LOCALE = "locale";
    public static final String TEMPLATE_CONTENT_TYPE = "emailContentType";

    public static final String TEMPLATE_SUBJECT = "subject";
    public static final String TEMPLATE_BODY = "body";
    public static final String TEMPLATE_FOOTER = "footer";

    public static final String EMAIL_TEMPLATE_TYPE_REGEX = "[a-zA-Z0-9\\s]+";
    public static final String ERROR_CODE_DELIMITER = "-";

    public static final String SMS_PROVIDER_POST_BODY_TEMPLATES_FILE = "sms-providers-api-body-templates.xml";
    public static final Path
            SMS_PROVIDER_POST_BODY_TEMPLATES_DIR_PATH = Paths.get(CarbonUtils.getCarbonHome(), "repository",
            "conf", "sms",SMS_PROVIDER_POST_BODY_TEMPLATES_FILE);
    public static final String SMS_PROVIDER= "provider";

    public static class ErrorMsg {
        private ErrorMsg() {

        }

        public static final String DUPLICATE_TEMPLATE_TYPE = "Email template type '%s' already exists in %s " +
                "tenant registry.";
    }

    public static class ErrorCodes {

        public static final String EMAIL_TEMPLATE_TYPE_NODE_FOUND = "10001";
        public static final String EMAIL_TEMPLATE_TYPE_ALREADY_EXISTS = "10002";
        public static final String EMAIL_TEMPLATE_TYPE_NOT_FOUND = "10003";
    }

    /**
     * Class which contains the error scenarios.
     */
    public static class ErrorScenarios {

        // ETM - EMAIL TEMPLATE MANAGER
        public static final String EMAIL_TEMPLATE_MANAGER = "ETM";
    }

    /**
     * Enum which contains error codes and corresponding error messages.
     */
    public enum ErrorMessages {

        ERROR_CODE_NULL_TEMPLATE_OBJECT("60001", "Notification template is not provided."),
        ERROR_CODE_EMPTY_TEMPLATE_NAME("60002", "Notification template name cannot be empty."),
        ERROR_CODE_INVALID_CHARACTERS_IN_TEMPLATE_NAME("60003", "Invalid characters exists in the " +
                "notification template display name : %s"),
        ERROR_CODE_EMPTY_LOCALE("60004", "Locale code cannot be empty"),
        ERROR_CODE_INVALID_CHARACTERS_IN_LOCALE("60005", "Locale contains invalid characters : %s"),
        ERROR_CODE_INVALID_EMAIL_TEMPLATE("60006", "Subject/Body/Footer sections of an email template " +
                "cannot be empty."),
        ERROR_CODE_INVALID_SMS_TEMPLATE("60007", "Body of a SMS template cannot be empty."),
        ERROR_CODE_DUPLICATE_TEMPLATE_TYPE("60008", "Notification template type : %s already exists " +
                "in tenant registry: %s"),
        ERROR_CODE_INVALID_SMS_TEMPLATE_CONTENT("60009", "SMS template cannot have a subject or footer"),
        ERROR_CODE_EMPTY_TEMPLATE_CHANNEL("60010", "Notification template channel cannot be empty"),
        ERROR_CODE_ERROR_CREATING_REGISTRY_RESOURCE("65001", "Error creating a registry resource " +
                "from template : %s in locale : %s"),
        ERROR_CODE_ERROR_ADDING_TEMPLATE("65002", "Error when adding template : %s to tenant : %s"),
        ERROR_CODE_ERROR_ERROR_ADDING_TEMPLATE("65003", "Error when adding notification template " +
                "of type : %s, in locale : %s to the tenant registry :  %s");
        private final String code;
        private final String message;

        ErrorMessages(String code, String message) {

            this.code = code;
            this.message = message;
        }

        public String getCode() {

            return code;
        }

        public String getMessage() {

            return message;
        }

        @Override
        public String toString() {

            return code + " - " + message;
        }
    }

    /**
     * Grouping of constants related to database table names.
     */
    public static class NotificationTableColumns {

        public static final String TYPE_KEY = "TYPE_KEY";
        public static final String NAME = "NAME";
        public static final String CHANNEL = "CHANNEL";
        public static final String TENANT_ID = "TENANT_ID";
        public static final String TEMPLATE_KEY = "TEMPLATE_KEY";
        public static final String LOCALE = "LOCALE";
        public static final String SUBJECT = "SUBJECT";
        public static final String BODY = "BODY";
        public static final String FOOTER = "FOOTER";
        public static final String CONTENT_TYPE = "CONTENT_TYPE";
        public static final String APP_ID = "APP_ID";
    }
}
