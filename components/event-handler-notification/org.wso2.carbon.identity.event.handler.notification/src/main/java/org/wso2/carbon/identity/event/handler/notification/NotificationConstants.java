/*
 * Copyright (c) 2016, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

import org.wso2.carbon.identity.core.util.IdentityUtil;

public class NotificationConstants {

    private NotificationConstants() {
    }

    public static final String TEMPLATE_TYPE = "TEMPLATE_TYPE";
    public static final String CLAIM_URI_LOCALE = IdentityUtil.getClaimUriLocale();
    public static final String ARBITRARY_SEND_TO = "send-to";
    public static final String ARBITRARY_BODY = "body";
    public static final String DEFAULT_NOTIFICATION_LOCALE = "en_US";
    public static final String NOTIFICATION_DEFAULT_LOCALE = "Notification.DefaultLocale";

    public static class EmailNotification {
        public static final String EMAIL_TEMPLATE_PATH = "identity/Email/";
        public static final String EMAIL_TEMPLATE_TYPE = "TEMPLATE_TYPE";
        public static final String LOCALE_DEFAULT = "en_US";
        public static final String TEMPLATE_CONTENT_TYPE_DEFAULT = "text/plain";
        public static final String CLAIM_URI_LOCALE = IdentityUtil.getClaimUriLocale();
        public static final String CLAIM_URI_EMAIL = "http://wso2.org/claims/emailaddress";
        public static final String IDENTITY_CLAIM_PREFIX = "identity";
        public static final String USER_CLAIM_PREFIX = "user.claim";
        public static final String IDENTITY_TEMPLATE_VALUE_PREFIX = "server.placeholder";
        public static final String TEMPLATE_PLACEHOLDERS_ELEM = "EmailTemplatePlaceholders";
        public static final String TEMPLATE_PLACEHOLDER_ELEM = "EmailTemplatePlaceholder";
        public static final String TEMPLATE_PLACEHOLDER_KEY_ATTRIB = "key";
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
        public static final String OUTPUT_ADAPTOR_DYNAMIC_EMAIL_TYPE_VALUE = "{{content-type}}";
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

        public static final String CARBON_PRODUCT_URL_TEMPLATE_PLACEHOLDER = "carbon.product-url";
        public static final String ACCOUNT_RECOVERY_ENDPOINT_PLACEHOLDER = "account.recovery.endpoint-url";
        public static final String AUTHENTICATION_ENDPOINT_PLACEHOLDER = "authentication.endpoint-url";
        public static final String CARBON_PRODUCT_URL_WITH_USER_TENANT_TEMPLATE_PLACEHOLDER
                = "product-url-with-user-tenant";
        public static final String ORGANIZATION_NAME_PLACEHOLDER = "organization-name";

        public static final String ENABLE_ORGANIZATION_LEVEL_EMAIL_BRANDING = "EnableOrganizationLevelEmailBranding";
        public static final String ORGANIZATION_LEVEL_EMAIL_BRANDING_FALLBACKS_ELEM
                =  "OrganizationLevelEmailBrandingFallbacks";
        public static final String ORGANIZATION_LEVEL_EMAIL_BRANDING_FALLBACK_ELEM
                =  "OrganizationLevelEmailBrandingFallback";
        public static final String ORGANIZATION_LEVEL_EMAIL_BRANDING_FALLBACK_KEY_ATTRIBUTE = "key";

        public static final String BRANDING_PREFERENCES_IS_ENABLED_PATH = "/configs/isBrandingEnabled";
        public static final String BRANDING_PREFERENCES_COPYRIGHT_TEXT_PATH = "/organizationDetails/copyrightText";
        public static final String BRANDING_PREFERENCES_SUPPORT_EMAIL_PATH = "/organizationDetails/supportEmail";
        public static final String BRANDING_PREFERENCES_LOGO_URL_PATH = "/images/logo/imgURL";
        public static final String BRANDING_PREFERENCES_LOGO_ALTTEXT_PATH = "/images/logo/altText";
        public static final String BRANDING_PREFERENCES_LIGHT_THEME = "LIGHT";

    }

    public static class SMSNotification {
        public static final String CLAIM_URI_MOBILE = "http://wso2.org/claims/mobile";

        public static final String STREAM_ID = "id_gov_sms_notify_stream:1.0.0";
        public static final String STREAM_NAME = "id_gov_sms_notify_stream";
        public static final String STREAM_VERSION = "1.0.0";
        public static final String DEFAULT_SMS_NOTIFICATION_LOCALE = "en_US";
    }
}
