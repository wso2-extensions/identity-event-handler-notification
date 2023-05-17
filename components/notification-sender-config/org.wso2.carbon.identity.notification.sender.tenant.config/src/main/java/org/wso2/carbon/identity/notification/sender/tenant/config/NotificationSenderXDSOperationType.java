package org.wso2.carbon.identity.notification.sender.tenant.config;

import org.wso2.carbon.identity.xds.common.constant.XDSOperationType;

/**
 * Notification sender XDS operation type.
 */
public enum NotificationSenderXDSOperationType implements XDSOperationType {

    ADD_EMAIL_SENDER,
    ADD_SMS_SENDER,
    DELETE_NOTIFICATION_SENDER,
    UPDATE_EMAIL_SENDER,
    UPDATE_SMS_SENDER,

}
