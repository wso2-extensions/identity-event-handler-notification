package org.wso2.carbon.identity.notification.sender.tenant.config.model;

import org.wso2.carbon.identity.notification.sender.tenant.config.dto.EmailSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.SMSSenderDTO;
import org.wso2.carbon.identity.xds.common.constant.XDSWrapper;


/**
 * Notification sender XDS wrapper.
 */
public class NotificationSenderXDSWrapper implements XDSWrapper {

    private EmailSenderDTO emailSender;
    private SMSSenderDTO smsSender;
    private String senderName;
    private String timestamp;

    public NotificationSenderXDSWrapper(NotificationSenderXDSWrapperBuilder builder) {

        this.emailSender = builder.emailSender;
        this.smsSender = builder.smsSender;
        this.senderName = builder.senderName;
        this.timestamp = builder.timestamp;
    }

    public EmailSenderDTO getEmailSender() {
        return emailSender;
    }

    public SMSSenderDTO getSMSSender() {
        return smsSender;
    }

    public String getSenderName() {
        return senderName;
    }

    /**
     * Get the timestamp of the notification sender.
     */
    public static class NotificationSenderXDSWrapperBuilder {

        private EmailSenderDTO emailSender;
        private SMSSenderDTO smsSender;
        private String senderName;
        private String timestamp;

        public NotificationSenderXDSWrapperBuilder setEmailSender(EmailSenderDTO emailSender) {
            this.emailSender = emailSender;
            return this;
        }

        public NotificationSenderXDSWrapperBuilder setSMSSender(SMSSenderDTO smsSender) {
            this.smsSender = smsSender;
            return this;
        }

        public NotificationSenderXDSWrapperBuilder setSenderName(String senderName) {
            this.senderName = senderName;
            return this;
        }

        public NotificationSenderXDSWrapper build() {

            this.timestamp = String.valueOf(System.currentTimeMillis());
            return new NotificationSenderXDSWrapper(this);
        }
    }
}
