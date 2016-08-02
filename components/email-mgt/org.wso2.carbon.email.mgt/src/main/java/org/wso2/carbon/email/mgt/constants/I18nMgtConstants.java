/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.identity.base.IdentityValidationUtil;

/**
 *  i18n management related constants
 */
public class I18nMgtConstants {

    private I18nMgtConstants() {
    }

    public static final String EMAIL_TEMPLATE_PATH = "/identity/Email";
    public static final String EMAIL_CONF_DIRECTORY = "email";
    public static final String EMAIL_ADMIN_CONF_FILE = "email-admin-config.xml";
    public static final String EMAIL_LOCALE_SEPARATOR = ".";
    public static final String EMAIL_FOLDER_SEPARATOR = "/";
    public static final String DEFAULT_EMAIL_LOCALE = "en";
    public static final String EMAIL_MEDIA_TYPE = "text/plain";

    public static final String EMAIL_TEMPLATE_NAME = "templateName";
    public static final String EMAIL_TEMPLATE_TYPE_DISPLAY_NAME = "templateDisplayName";

    // constants related to email template meta data properties
    public static final String TEMPLATE_TYPE = "type";
    public static final String TEMPLATE_TYPE_DISPLAY_NAME = "display";
    public static final String TEMPLATE_LOCALE = "locale";
    public static final String TEMPLATE_CONTENT_TYPE = "emailContentType";

    public static final String TEMPLATE_SUBJECT = "subject";
    public static final String TEMPLATE_BODY = "body";
    public static final String TEMPLATE_FOOTER = "footer";

    public static final String EMAIL_TEMPLATE_TYPE_REGEX = "[a-zA-Z0-9\\s]+";


    public static class ErrorMsg {
        private ErrorMsg() {}

        public static final String DUPLICATE_TEMPLATE_TYPE =
                "Email template type '%s' already exists in %s tenant registry.";

    }

}
