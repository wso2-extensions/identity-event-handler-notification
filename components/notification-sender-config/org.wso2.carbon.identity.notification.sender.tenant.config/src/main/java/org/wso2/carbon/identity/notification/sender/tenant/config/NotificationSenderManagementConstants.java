/*
 * Copyright (c) 2022-2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.notification.sender.tenant.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Notification sender management related constant class.
 */
public class NotificationSenderManagementConstants {

    private NotificationSenderManagementConstants() {

    }

    public static final String NOTIFICATION_SENDER_ERROR_PREFIX = "NSM-";
    public static final String NOTIFICATION_SENDER_CONTEXT_PATH = "/notification-senders";
    public static final String PUBLISHER_RESOURCE_TYPE = "Publisher";
    public static final String PUBLISHER_TYPE_PROPERTY = "type";
    public static final String DEFAULT_EMAIL_PUBLISHER = "EmailPublisher";
    public static final String DEFAULT_SMS_PUBLISHER = "SMSPublisher";
    public static final String DEFAULT_PUSH_PUBLISHER = "PushPublisher";
    public static final String PUBLISHER_FILE_EXTENSION = ".xml";
    public static final String RESOURCE_NOT_EXISTS_ERROR_CODE = "CONFIGM_00017";
    public static final String PLACEHOLDER_IDENTIFIER = "$";
    public static final String INLINE_BODY_PARAM_PREFIX = "body.";
    public static final String INLINE_BODY_PROPERTY = "body";
    public static final String PLUS = "+";
    public static final String URL_ENCODED_SPACE = "%20";
    public static final String CONFIG_MGT_ERROR_CODE_DELIMITER = "_";
    public static final String SECRET_PROPERTIES = "_SECRET_PROPERTIES";

    // Notification Type
    public static final String EMAIL_PROVIDER = "EMAIL_PROVIDER";
    public static final String SMS_PROVIDER = "SMS_PROVIDER";
    public static final String PUSH_PROVIDER = "PUSH_PROVIDER";

    // Email Sender's main properties.
    public static final String NAME = "name";
    public static final String SMTP_SERVER_HOST = "smtpServerHost";
    public static final String SMTP_PORT = "smtpPort";
    public static final String FROM_ADDRESS = "fromAddress";
    public static final String EMAIL_PUBLISHER_TYPE = "email";
    public static final String AUTH_TYPE = "authType";
    public static final String REPLY_TO_ADDRESS = "mail.smtp.replyTo";
    public static final String DISPLAY_NAME = "mail.smtp.signature";

    // Email Sender's additional properties.
    public static final String CLIENT_ID = "clientId";
    public static final String CLIENT_SECRET = "clientSecret";
    public static final String TOKEN_ENDPOINT = "tokenEndpoint";
    public static final String SCOPES = "scopes";
    public static final String USERNAME = "userName";
    public static final String PASSWORD = "password";

    // Email Sender Authentication types.
    public static final String BASIC = "BASIC";
    public static final String CLIENT_CREDENTIAL = "CLIENT_CREDENTIAL";

    // SMS Sender's main properties.
    public static final String PROVIDER = "provider";
    public static final String PROVIDER_URL = "providerURL";
    public static final String KEY = "key";
    public static final String SECRET = "secret";
    public static final String SENDER = "sender";
    public static final String CONTENT_TYPE = "contentType";
    public static final String CLIENT_HTTP_METHOD_PROPERTY = "http.client.method";
    public static final String SMS_PUBLISHER_TYPE = "sms";
    public static final String MY_ACCOUNT_SMS_RESOURCE_TYPE = "myaccount";
    public static final String MY_ACCOUNT_SMS_RESOURCE_NAME = "myaccount-2FA-config";

    // Push Sender's main properties.
    public static final String PUSH_PUBLISHER_TYPE = "push";

    // Constant for eventPublisher file generation.
    public static final String ROOT_ELEMENT = "eventPublisher";
    public static final String PUBLISHER_NAME = "name";
    public static final String PROCESSING_KEY = "processing";
    public static final String STATISTICS_KEY = "statistics";
    public static final String TRACE_KEY = "trace";
    public static final String XMLNS_KEY = "xmlns";
    public static final String XMLNS_VALUE = "http://wso2.org/carbon/eventpublisher";
    public static final String ENABLE = "enable";
    public static final String DISABLE = "disable";
    public static final String FROM = "from";
    public static final String STREAM_NAME = "streamName";
    public static final String STREAM_VERSION = "version";
    public static final String MAPPING = "mapping";
    public static final String CUSTOM_MAPPING_KEY = "customMapping";
    public static final String MAPPING_TYPE_KEY = "type";
    public static final String TEXT = "text";
    public static final String INLINE = "inline";
    public static final String EMAIL_INLINE_BODY = "{{body}}{{footer}}";
    public static final String TO = "to";
    public static final String ADAPTER_TYPE_KEY = "eventAdapterType";
    public static final String ADAPTER_TYPE_EMAIL_VALUE = "email";
    public static final String ADAPTER_TYPE_HTTP_VALUE = "http";
    public static final String ADAPTER_PROPERTY = "property";
    public static final String ADAPTER_PROPERTY_NAME = "name";
    public static final String EMAIL_ADDRESS_PROPERTY = "email.address";
    public static final String EMAIL_ADDRESS_VALUE = "{{send-to}}";
    public static final String EMAIL_TYPE_PROPERTY = "email.type";
    public static final String EMAIL_TYPE_VALUE = "{{content-type}}";
    public static final String EMAIL_SUBJECT_PROPERTY = "email.subject";
    public static final String EMAIL_SUBJECT_VALUE = "{{subject}}";
    public static final String SMTP_PASSWORD_PROPERTY = "mail.smtp.password";
    public static final String SMTP_FROM_PROPERTY = "mail.smtp.from";
    public static final String SMTP_USER_PROPERTY = "mail.smtp.user";
    public static final String SMTP_HOST_PROPERTY = "mail.smtp.host";
    public static final String SMTP_PORT_PROPERTY = "mail.smtp.port";
    public static final String HTTP_URL_PROPERTY = "http.url";
    public static final String CONSTANT_HTTP_POST = "HttpPost";
    public static final String CHANNEL_TYPE_PROPERTY = "channel.type";
    public static final String DEFAULT_HANDLER_NAME = "default";
    public static final String SMTP_CLIENT_ID_PROPERTY = "mail.smtp.clientId";
    public static final String SMTP_CLIENT_SECRET_PROPERTY = "mail.smtp.clientSecret";
    public static final String SMTP_TOKEN_ENDPOINT_PROPERTY = "mail.smtp.tokenEndpoint";
    public static final String SMTP_SCOPES_PROPERTY = "mail.smtp.scopes";
    public static final String SMTP_AUTH_TYPE_PROPERTY = "mail.smtp.authType";

    public static final List<String> INTERNAL_PROPERTIES =
            Collections.unmodifiableList(Arrays.asList(STREAM_NAME, STREAM_VERSION, PUBLISHER_TYPE_PROPERTY));
    public static final List<String> PROPERTIES_TO_SKIP_AT_ADAPTER_CONFIG =
            Collections.unmodifiableList(
                    Arrays.asList(STREAM_NAME, STREAM_VERSION, PUBLISHER_TYPE_PROPERTY, INLINE_BODY_PROPERTY));

    /**
     * Enums for error messages.
     */
    public enum ErrorMessage {

        // Client errors 600xx.
        ERROR_CODE_PUBLISHER_NOT_EXISTS_IN_SUPER_TENANT("60001", "No defined system notification sender.",
                "There is no notification sender: %s, defined in system."),
        ERROR_CODE_CONFLICT_PUBLISHER("60002", "Notification sender already exists.",
                "There is already exists a notification sender: %s."),
        ERROR_CODE_SMS_PROVIDER_REQUIRED("60003", "Required attribute is missing.",
                "SMS provider is not defined for notification sender."),
        ERROR_CODE_SMS_PAYLOAD_NOT_FOUND("60004", "Required attribute is missing.",
                "SMS payload is not defined for notification sender."),
        ERROR_CODE_SMS_PROVIDER_URL_REQUIRED("60005", "Required attribute is missing.",
                "SMS provider url is not defined for notification sender."),
        ERROR_CODE_PUBLISHER_NOT_EXISTS("60006", "No notification sender found.",
                "There is no notification sender for publisher: %s."),
        ERROR_CODE_CHANNEL_TYPE_UPDATE_NOT_ALLOWED("60007",
                "Updating channel type is not allowed.",
                "Updating channel type is not allowed for SMS notification sender: %s."),
        ERROR_CODE_CONNECTED_APPLICATION_EXISTS("60008",
                "Unable to disable.",
                "There are applications using this connection."),

        // Server errors 650xx.
        ERROR_CODE_NO_ACTIVE_PUBLISHERS_FOUND("65001", "No active notification senders found.",
                "There exists no active notification senders in super tenant"),
        ERROR_CODE_SERVER_ERRORS_GETTING_EVENT_PUBLISHER("65002", "Error while getting event publisher configurations.",
                "Error occurred while retrieving event publisher configurations: %s."),
        ERROR_CODE_ERROR_GETTING_NOTIFICATION_SENDER("65003", "Error while getting notification sender.",
                "Error while retrieving notification sender resource: %s."),
        ERROR_CODE_ERROR_ADDING_NOTIFICATION_SENDER("65004", "Unable to add notification sender.",
                "Server encountered an error while adding the notification sender resource: %s"),
        ERROR_CODE_ERROR_DELETING_NOTIFICATION_SENDER("65005", "Unable to delete notification sender.",
                "Server encountered an error while deleting the notification sender resource: %s"),
        ERROR_CODE_ERROR_GETTING_NOTIFICATION_SENDERS_BY_TYPE("65006", "Error while getting notification senders.",
                "Error while retrieving %s notification sender resources."),
        ERROR_CODE_ERROR_UPDATING_NOTIFICATION_SENDER("65007", "Unable to update notification sender.",
                "Error while updating notification sender: %s."),
        ERROR_CODE_TRANSFORMER_EXCEPTION("65008", "Transformer Exception.", "Transformer Exception: %s ."),
        ERROR_CODE_PARSER_CONFIG_EXCEPTION("65009", "Parser Configuration Exception.",
                "Parser Configuration Exception: %s."),
        ERROR_CODE_NO_RESOURCE_EXISTS("65010", "No notification sender found.",
                "No notification sender found with name: %s."),
        ERROR_CODE_RESOURCE_RE_DEPLOY_ERROR("65011", "Error while re-deploying resource.",
                                              "Error while re-deploying resource with name: %s."),
        ERROR_CODE_RESOURCE_DELETE_ERROR("65011", "Error while deleting resource.",
                "Error while deleting resource with name: %s."),
        ERROR_CODE_CONFIGURATION_HANDLER_NOT_FOUND("65012",
                "No configuration handler found for the given channel type.",
                "No configuration handler found for the given channel type: %s."),
        ERROR_CODE_ERROR_REGISTERING_HUB_TOPIC("65013",
                "Error while registering hub topic for websub notification channel.",
                "Error while registering hub topic for websub notification channel: %s."),
        ERROR_CODE_ERROR_UNREGISTERING_HUB_TOPIC("65014",
                "Error while unregistering hub topic for websub notification channel.",
                "Error while unregistering hub topic for websub notification channel.: %s."),
        ERROR_CODE_TOPIC_DEREGISTRATION_FAILURE_ACTIVE_SUBS("65015",
                "Error occurred while de-registering hub topic for websub notification channel.",
                "Error received from WebSubHub while attempting to de-register notification channel: %s due to active" +
                " subscribers."),
        ERROR_CODE_VALIDATING_CONNECTED_APPS("65016",
                "Error while validating connected applications.",
                "Error while validating connected applications: %s."),
        ERROR_CODE_MATCHING_PUSH_PROVIDER_NOT_FOUND("65017",
                "Matching push provider not found in the configured push sender.",
                "Matching push provider not found the configured push sender: %s."),
        ERROR_CODE_ERROR_PROCESSING_PUSH_SENDER_PROPERTIES("65018",
                "Error while processing and storing push sender properties.",
                "Error while processing and storing push sender properties: %s."),
        ERROR_CODE_ERROR_UPDATING_PUSH_SENDER_PROPERTIES("65019",
                "Error while updating push sender properties.",
                "Error while updating push sender properties: %s."),
        ERROR_CODE_ERROR_ADDING_NOTIFICATION_SENDER_SECRETS("65020", "Unable to add notification sender.",
                "Server encountered an error while adding the notification sender secrets to resource: %s"),
        ERROR_CODE_ERROR_DELETING_NOTIFICATION_SENDER_SECRETS("65021", "Unable to delete notification sender.",
                "Server encountered an error while deleting the notification sender secrets from resource: %s"),
        ERROR_CODE_INVALID_INPUTS("65022", "Invalid input.",
                "Invalid input received for notification sender."),
        ERROR_CODE_ERROR_WHILE_ENCRYPTING_CREDENTIALS("65023", "Error while encrypting credentials.",
                "Error while encrypting credentials for notification sender: %s."),
        ERROR_CODE_ERROR_WHILE_DECRYPTING_CREDENTIALS("65024", "Error while decrypting credentials.",
                "Error while decrypting credentials for notification sender: %s."),
        ERROR_CODE_ERROR_WHILE_DELETING_CREDENTIALS("65025", "Error while deleting credentials.",
                "Error while deleting credentials for notification sender: %s.");

        private final String code;
        private final String message;
        private final String description;

        ErrorMessage(String code, String message, String description) {

            this.code = code;
            this.message = message;
            this.description = description;
        }

        public String getCode() {

            return NOTIFICATION_SENDER_ERROR_PREFIX + code;
        }

        public String getMessage() {

            return message;
        }

        public String getDescription() {

            return description;
        }

        @Override
        public String toString() {

            return code + " | " + message;
        }
    }
}
