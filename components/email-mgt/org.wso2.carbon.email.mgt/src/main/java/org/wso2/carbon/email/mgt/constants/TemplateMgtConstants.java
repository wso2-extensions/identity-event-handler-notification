/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

public class TemplateMgtConstants {

    private TemplateMgtConstants() {}

    public static final String DEFAULT_EMAIL_NOTIFICATION_LOCALE = "en_us";
    public static final String DEFAULT_SMS_NOTIFICATION_LOCALE = "en_us";

    /**
     * Class which contains the error scenarios.
     */
    public static class ErrorScenarios {

        // NTM - NOTIFICATION TEMPLATE MANAGER
        public static final String NOTIFICATION_TEMPLATE_MANAGER = "NTM";
    }

    /**
     * Class which contains the error codes for template management.
     */
    public static class ErrorCodes {
        public static final String TEMPLATE_TYPE_ALREADY_EXISTS = "65001";
        public static final String TEMPLATE_TYPE_NOT_FOUND = "65002";
        public static final String TEMPLATE_ALREADY_EXISTS = "65003";
        public static final String TEMPLATE_NOT_FOUND = "65004";
        public static final String ERROR_ADDING_TEMPLATE = "65005";
        public static final String ERROR_UPDATING_TEMPLATE = "65006";
        public static final String ERROR_SYSTEM_RESOURCE_DELETION_NOT_ALLOWED = "65007";
    }

    /**
     * Enum which contains error codes and corresponding error messages.
     */
    public enum ErrorMessages {

        ERROR_CODE_EMPTY_TEMPLATE_NAME("60001", "Notification template name cannot be empty."),
        ERROR_CODE_EMPTY_LOCALE("60002", "Locale code cannot be empty"),
        ERROR_CODE_INVALID_LOCALE("60003", "Locale code is invalid."),
        ERROR_CODE_EMPTY_TEMPLATE_CHANNEL("60004", "Notification template channel cannot be empty"),
        ERROR_CODE_INVALID_SMS_TEMPLATE("60005", "Body of a SMS template cannot be empty."),
        ERROR_CODE_INVALID_SMS_TEMPLATE_CONTENT("60006", "SMS template cannot have a subject or footer"),
        ERROR_CODE_INVALID_EMAIL_TEMPLATE("60007", "Subject/Body/Footer sections of an email template " +
                "cannot be empty."),
        ERROR_CODE_INVALID_TEMPLATE_DISPLAY_NAME("60008", "Invalid template display name."),
        ERROR_CODE_NULL_TEMPLATE_OBJECT("60009", "Notification template is not provided."),
        ERROR_CODE_TEMPLATE_TYPE_ALREADY_EXISTS(ErrorCodes.TEMPLATE_TYPE_ALREADY_EXISTS,
                "Notification template type : %s already exists in tenant : %s"),
        ERROR_CODE_TEMPLATE_TYPE_NOT_FOUND(ErrorCodes.TEMPLATE_TYPE_NOT_FOUND, "Notification template type :" +
                " %s doesn't exist in tenant : %s"),
        ERROR_CODE_TEMPLATE_ALREADY_EXISTS(ErrorCodes.TEMPLATE_ALREADY_EXISTS, "Notification template : %s" +
                " already exists in tenant : %s"),
        ERROR_CODE_TEMPLATE_NOT_FOUND(ErrorCodes.TEMPLATE_NOT_FOUND, "Notification template : %s " +
                "doesn't exist in tenant : %s"),
        ERROR_CODE_SYSTEM_TEMPLATE_TYPE_NOT_FOUND(ErrorCodes.TEMPLATE_TYPE_NOT_FOUND,
                "System notification template type : %s doesn't exist."),
        ERROR_CODE_SYSTEM_TEMPLATE_NOT_FOUND(ErrorCodes.TEMPLATE_NOT_FOUND, "System notification " +
                "template : %s doesn't exist"),
        ERROR_CODE_ERROR_ADDING_TEMPLATE(ErrorCodes.ERROR_ADDING_TEMPLATE, "Error when adding template : %s" +
                " to tenant : %s"),
        ERROR_CODE_ERROR_UPDATING_TEMPLATE(ErrorCodes.ERROR_UPDATING_TEMPLATE, "Error when updating " +
                "template : %s on tenant : %s"),
        ERROR_CODE_SYSTEM_RESOURCE_DELETION_NOT_ALLOWED(ErrorCodes.ERROR_SYSTEM_RESOURCE_DELETION_NOT_ALLOWED,
                "System resource deletion not allowed. %S");

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
}
