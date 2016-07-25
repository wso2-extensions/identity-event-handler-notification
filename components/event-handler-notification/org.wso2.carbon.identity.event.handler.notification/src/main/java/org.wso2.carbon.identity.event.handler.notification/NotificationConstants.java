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

package org.wso2.carbon.identity.event.handler.notification;

public class NotificationConstants {

    private NotificationConstants() {
    }

    public static class EmailNotification {
        public static final String EMAIL_TEMPLATE_PATH = "identity/Email/";
        public static final String EMAIL_TEMPLATE_TYPE = "TEMPLATE_TYPE";
        public static final String LOCALE_DEFAULT = "en_US";
        public static final String TEMPLATE_CONTENT_TYPE_DEFAULT = "text/plain";
        public static final String CLAIM_URI_LOCALE = "http://wso2.org/claims/locality";
        public static final String CLAIM_URI_EMAIL = "http://wso2.org/claims/emailaddress";
        public static final String IDENTITY_CLAIM_PREFIX = "identity";
        public static final String USER_CLAIM_PREFIX = "user.claim";
        public static final String WSO2_CLAIM_URI = "http://wso2.org/claims/";
    }
}
