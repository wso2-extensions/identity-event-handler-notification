/*
 * Copyright (c) 2016-2026, WSO2 LLC. (http://www.wso2.com).
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
    public static final String TENANT_DOMAIN = "tenant-domain";
    public static final String IS_FEDERATED_USER = "isFederatedUser";
    public static final String FEDERATED_USER_CLAIMS = "federatedUserClaims";
    public static final String IGNORE_IF_TEMPLATE_NOT_FOUND = "ignoreIfTemplateNotFound";
    public static final String FLOW_TYPE = "flowType";
    public static final String REGISTRATION_FLOW = "registration";

    public static class EmailNotification {
        public static final String EMAIL_TEMPLATE_PATH = "identity/Email/";
        public static final String EMAIL_TEMPLATE_TYPE = "TEMPLATE_TYPE";
        public static final String LOCALE_DEFAULT = "en_US";
        public static final String TEMPLATE_CONTENT_TYPE_DEFAULT = "text/plain";
        public static final String CLAIM_URI_LOCALE = IdentityUtil.getClaimUriLocale();
        public static final String CLAIM_URI_EMAIL = "http://wso2.org/claims/emailaddress";
        public static final String OIDC_CLAIM_URI_EMAIL = "email";
        public static final String IDENTITY_CLAIM_PREFIX = "identity";
        public static final String USER_CLAIM_PREFIX = "user.claim";
        public static final String UTM_PARAMETER_PREFIX = "utm_";
        public static final String IDENTITY_TEMPLATE_VALUE_PREFIX = "server.placeholder";
        public static final String TEMPLATE_PLACEHOLDERS_ELEM = "EmailTemplatePlaceholders";
        public static final String TEMPLATE_PLACEHOLDER_ELEM = "EmailTemplatePlaceholder";
        public static final String TEMPLATE_PLACEHOLDER_KEY_ATTRIB = "key";
        public static final String UTM_PARAMETERS_PLACEHOLDER = "utm-parameters";
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

        public static final String SYNC_EMAIL_NOTIFICATION = "syncEmailNotification";
        public static final String EMAIL_SYNC = "email.sync";
        public static final String HTTP_SYNC = "http.sync";

        public static final String CARBON_PRODUCT_URL_TEMPLATE_PLACEHOLDER = "carbon.product-url";
        public static final String ACCOUNT_RECOVERY_ENDPOINT_PLACEHOLDER = "account.recovery.endpoint-url";
        public static final String AUTHENTICATION_ENDPOINT_PLACEHOLDER = "authentication.endpoint-url";
        public static final String CARBON_PRODUCT_URL_WITH_USER_TENANT_TEMPLATE_PLACEHOLDER
                = "product-url-with-user-tenant";
        public static final String ORGANIZATION_NAME_PLACEHOLDER = "organization-name";
        public static final String ORGANIZATION_ID_PLACEHOLDER = "organization-id";

        public static final String ENABLE_ORGANIZATION_LEVEL_EMAIL_BRANDING = "EnableOrganizationLevelEmailBranding";
        public static final String ORGANIZATION_LEVEL_EMAIL_BRANDING_FALLBACKS_ELEM
                =  "OrganizationLevelEmailBrandingFallbacks";
        public static final String ORGANIZATION_LEVEL_EMAIL_BRANDING_FALLBACK_ELEM
                =  "OrganizationLevelEmailBrandingFallback";
        public static final String ORGANIZATION_LEVEL_EMAIL_BRANDING_FALLBACK_KEY_ATTRIBUTE = "key";

        public static final String BRANDING_PREFERENCES_IS_ENABLED_PATH = "/configs/isBrandingEnabled";
        public static final String BRANDING_PREFERENCES_COPYRIGHT_TEXT_PATH = "/organizationDetails/copyrightText";
        public static final String BRANDING_PREFERENCES_SUPPORT_EMAIL_PATH = "/organizationDetails/supportEmail";
        public static final String BRANDING_PREFERENCES_DISPLAY_NAME_PATH = "/organizationDetails/displayName";
        public static final String BRANDING_PREFERENCES_LOGO_URL_PATH = "/images/logo/imgURL";
        public static final String BRANDING_PREFERENCES_LOGO_ALTTEXT_PATH = "/images/logo/altText";
        public static final String BRANDING_PREFERENCES_LIGHT_THEME = "LIGHT";
        public static final String CUSTOM_TEXT_COMMON_SCREEN = "common";
        public static final String ORGANIZATION_COPYRIGHT_PLACEHOLDER = "organization.copyright.text";
        public static final String CUSTOM_TEXT_COPYRIGHT_PATH = "/text/copyright";
        public static final String CUSTOM_TEXT_COPYRIGHT_YEAR_KEY = "{{currentYear}}";
        public static final String NEW_LINE_CHARACTER_STRING = "\\n";
        public static final String NEW_LINE_CHARACTER_HTML = "<br>";

        /**
         * Error codes from the event output adapters used for email notification delivery.
         */
        public static class AdapterErrorCodes {

            private AdapterErrorCodes() {
                                
            }

            // Email adapter error codes.
            public static final String EMAIL_SEND_FAILED = "EMAIL-OA-65000";
            public static final String EMAIL_MISSING_ADDRESS = "EMAIL-OA-65001";
            public static final String EMAIL_ENCODING_FAILED = "EMAIL-OA-65002";
            public static final String EMAIL_AUTH_FAILED = "EMAIL-OA-65003";
            public static final String EMAIL_SEND_REJECTED = "EMAIL-OA-65004";

            // HTTP adapter error codes.
            public static final String HTTP_CLIENT_INIT_FAILED = "HTTP-OA-65000";
            public static final String HTTP_CLIENT_NOT_INITIALIZED = "HTTP-OA-65001";
            public static final String HTTP_PUBLISH_UNAUTHORIZED = "HTTP-OA-65003";
            public static final String HTTP_PUBLISH_FORBIDDEN = "HTTP-OA-65004";
            public static final String HTTP_PUBLISH_BAD_REQUEST = "HTTP-OA-65005";
            public static final String HTTP_PUBLISH_TOO_MANY_REQUESTS = "HTTP-OA-65006";
            public static final String HTTP_PUBLISH_SERVICE_UNAVAILABLE = "HTTP-OA-65007";
            public static final String HTTP_PUBLISH_SERVER_ERROR = "HTTP-OA-65008";
            public static final String HTTP_PUBLISH_FAILED_IO = "HTTP-OA-65009";
            public static final String HTTP_TOKEN_REFRESH_MISSING_CREDS = "HTTP-OA-65010";
            public static final String HTTP_TOKEN_FETCH_FAILED = "HTTP-OA-65011";
        }

        public enum ErrorMessages {

            EMAIL_SEND_FAILED("ENH-65001",
                    "Your email could not be sent due to a mail server issue. "
                            + "Please try again or contact support if the problem persists."),
            EMAIL_MISSING_ADDRESS("ENH-65002",
                    "Your email could not be sent because no recipient address was provided. "
                            + "Please try again."),
            EMAIL_ENCODING_FAILED("ENH-65003",
                    "Your email could not be sent because the recipient address contains unsupported characters. "
                            + "Please check the email address and try again."),
            EMAIL_AUTH_FAILED("ENH-65004",
                    "Your email could not be sent because the mail server could not verify the sender. "
                            + "Please contact your administrator to check the email server settings."),
            EMAIL_SEND_REJECTED("ENH-65005",
                    "Your email could not be sent because the recipient address was not accepted by the mail "
                            + "server. Please verify the email address and try again."),
            HTTP_CLIENT_INIT_FAILED("ENH-65006",
                    "The email service is not ready. Please contact your administrator to check "
                            + "the email service configuration."),
            HTTP_CLIENT_NOT_INITIALIZED("ENH-65007",
                    "The email service is not ready. Please contact your administrator to check "
                            + "the email service configuration."),
            HTTP_PUBLISH_UNAUTHORIZED("ENH-65008",
                    "The email could not be delivered because access was denied. Please contact your "
                            + "administrator to verify the email service credentials."),
            HTTP_PUBLISH_FORBIDDEN("ENH-65009",
                    "The email could not be delivered because the configured account does not have the "
                            + "required permissions. Please contact your administrator."),
            HTTP_PUBLISH_BAD_REQUEST("ENH-65010",
                    "The email could not be delivered because the email service did not accept "
                            + "the request. Please contact your administrator."),
            HTTP_PUBLISH_TOO_MANY_REQUESTS("ENH-65011",
                    "The email could not be delivered because the email service is receiving too "
                            + "many requests. Please try again in a few moments."),
            HTTP_PUBLISH_SERVER_ERROR("ENH-65012",
                    "The email could not be delivered because the email service encountered an "
                            + "unexpected error. Please try again or contact support."),
            HTTP_PUBLISH_SERVICE_UNAVAILABLE("ENH-65013",
                    "The email could not be delivered because the email service is temporarily "
                            + "unavailable. Please try again in a few moments."),
            HTTP_PUBLISH_FAILED_IO("ENH-65014",
                    "The email could not be delivered because a connection to the email service "
                            + "could not be established. Please try again or contact support."),
            HTTP_TOKEN_REFRESH_MISSING_CREDS("ENH-65015",
                    "The email could not be delivered because the service authentication failed. "
                            + "Please contact your administrator to verify the email service credentials."),
            HTTP_TOKEN_FETCH_FAILED("ENH-65016",
                    "The email could not be delivered because the service authentication failed. "
                            + "Please contact your administrator to verify the email service credentials."),
            UNKNOWN_ERROR("ENH-65017",
                    "The notification could not be delivered due to an unexpected error. "
                            + "Please try again or contact support."),
            EMAIL_NOTIFICATION_THROTTLED("ENH-65018",
                    "The email could not be delivered because the email service is temporarily suspended "
                            + "due to repeated failures. Please try again later or contact support.");

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
        }
    }

    public static class SMSNotification {
        public static final String CLAIM_URI_MOBILE = "http://wso2.org/claims/mobile";

        public static final String STREAM_ID = "id_gov_sms_notify_stream:1.0.0";
        public static final String STREAM_NAME = "id_gov_sms_notify_stream";
        public static final String STREAM_VERSION = "1.0.0";
        public static final String DEFAULT_SMS_NOTIFICATION_LOCALE = "en_US";
    }

    /**
     * Define Push Notification constants.
     */
    public static class PushNotification {

        public static final String PUSH_NOTIFICATION_EVENT = "TRIGGER_PUSH_NOTIFICATION";
        public static final String PUSH_NOTIFICATION_HANDLER_NAME = "PushNotificationHandler";
        public static final String PUSH_PUBLISHER_NAME = "PushPublisher";
        public static final String PUSH_AUTHENTICATION_SCENARIO = "AUTHENTICATION";

        public static final String NOTIFICATION_SCENARIO = "NOTIFICATION_SCENARIO";
        public static final String NOTIFICATION_PROVIDER = "notificationProvider";
        public static final String PUSH_ID = "pushId";
        public static final String DEVICE_TOKEN = "deviceToken";
        public static final String CHALLENGE = "challenge";
        public static final String NUMBER_CHALLENGE = "numberChallenge";
        public static final String IP_ADDRESS = "ipAddress";
        public static final String REQUEST_DEVICE_OS = "deviceOS";
        public static final String REQUEST_DEVICE_BROWSER = "browser";
        public static final String DEVICE_ID = "deviceId";
        public static final String DEVICE_HANDLE = "deviceHandle";
    }

    /**
     * Define Push Notification templates.
     */
    public enum PushNotificationTemplate {

        AUTHENTICATION(
                PushNotification.PUSH_AUTHENTICATION_SCENARIO,
                "Authentication Request",
                "{{user-name}} from {{organization-name}} is trying to login");

        private String scenario;
        private String title;
        private String body;

        PushNotificationTemplate(String scenario, String title, String body) {

            this.scenario = scenario;
            this.title = title;
            this.body = body;
        }

        public String getScenario() {

            return scenario;
        }

        public String getTitle() {

            return title;
        }

        public String getBody() {

            return body;
        }
    }

    /**
     * Define Push Notification placeholders.
     */
    public enum PushNotificationPlaceholder {

        USER_NAME("user-name"),
        USER_GIVEN_NAME("user.claim.givenname"),
        USER_STORE_DOMAIN("userstore-domain"),
        ORGANIZATION_NAME("organization-name"),
        TENANT_DOMAIN("tenant-domain"),;

        private String placeholder;

        PushNotificationPlaceholder(String placeholder) {

            this.placeholder = placeholder;
        }

        public String getPlaceholder() {

            return placeholder;
        }
    }

    /**
     * Define logging constants.
     */
    public static class LogConstants {

        private LogConstants() {
        }
        public static final String NOTIFICATION_HANDLER_SERVICE = "notification-handler-service";

        /**
         * Define action IDs for diagnostic logs.
         */
        public static class ActionIDs {

            private ActionIDs() {
            }

            public static final String HANDLE_EVENT = "handle-event";
            public static final String PUBLISH_SYNC_EMAIL_NOTIFICATION = "publish-sync-email-notification";
        }

        /**
         * Define common and reusable Input keys for diagnostic logs.
         */
        public static class InputKeys {

            private InputKeys() {
            }
            public static final String EVENT_NAME = "event name";
            public static final String TENANT_DOMAIN = "tenant domain";
        }
    }
}
