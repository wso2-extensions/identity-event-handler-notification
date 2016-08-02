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

package org.wso2.carbon.identity.event.handler.email;

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

        public static final String STREAM_NAME = "id_gov_notify_stream";
        public static final String STREAM_VERSION = "1.0.0";
        public static final String STREAM_ID = "id_gov_notify_stream:1.0.0";

        public static final String EVENT_PUBLISHER_NAME = "EmailPublisher";
        public static final String OUTPUT_ADAPTOR_NAME = "Email Publisher";
        public static final String OUTPUT_ADAPTOR_MESSAGE_FORMAT = "text";
        public static final String OUTPUT_ADAPTOR_TYPE = "email";
        public static final String OUTPUT_MAPPING_TEXT = "{{body}}{{footer}}";
        public static final String OUTPUT_ADAPTOR_DYNAMIC_EMAIL_ADD_PROPERTY = "email.address";
        public static final String OUTPUT_ADAPTOR_DYNAMIC_EMAIL_ADD_VALUE = "{{send-to}}";
        public static final String OUTPUT_ADAPTOR_DYNAMIC_EMAIL_TYPE_PROPERTY = "email.type";
        public static final String OUTPUT_ADAPTOR_DYNAMIC_EMAIL_TYPE_VALUE = "text/plain";
        public static final String OUTPUT_ADAPTOR_DYNAMIC_EMAIL_SUBJECT_PROPERTY = "email.subject";
        public static final String OUTPUT_ADAPTOR_DYNAMIC_EMAIL_SUBJECT_VALUE = "{{subject}}";

        public static final String ARBITRARY_EVENT_TYPE = "notification-event";
        public static final String ARBITRARY_SEND_FROM = "send-from";
        public static final String ARBITRARY_SUBJECT_TEMPLATE = "subject-template";
        public static final String ARBITRARY_BODY_TEMPLATE = "body-template";
        public static final String ARBITRARY_FOOTER_TEMPLATE = "footer-template";
        public static final String ARBITRARY_LOCALE = "locale";
        public static final String ARBITRARY_CONTENT_TYPE = "content-type";
        public static final String ARBITRARY_SEND_TO = "send-to";
        public static final String ARBITRARY_SUBJECT = "subject";
        public static final String ARBITRARY_BODY = "body";
        public static final String ARBITRARY_FOOTER = "footer";


    }
}
