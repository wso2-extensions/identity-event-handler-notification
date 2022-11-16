/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.event.sms.notification.handler;

/**
 * Keep constants required by the SMS OTP Notification Event Handler.
 */
public class SMSNotificationConstants {

    private SMSNotificationConstants() {

    }

    public static final String NOTIFICATION_HANDLER_NAME = "sms.notification.sender";
    public static final String SMS_NOTIFICATION_HUB_TOPIC_SUFFIX = "NOTIFICATIONS";
    public static final String SMS_NOTIFICATION_EVENT_URI = "urn:ietf:params:notifications:smsOtp";
    public static final String TENANT_DOMAIN_NAME = "tenant-domain";
    public static final String SMS_MESSAGE_BODY_NAME = "body";
    public static final String SMS_MASSAGE_TO_NAME = "send-to";
    public static final String USER_NAME = "user-name";
    public static final String USER_ID_CLAIM_URI = "http://wso2.org/claims/userid";
    public static final String USERS = "/Users/";
    public static final String SCIM2_ENDPOINT = "/scim2";
    public static final String TENANT_SEPARATOR = "/t/";
}
