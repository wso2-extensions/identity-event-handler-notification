/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

import org.wso2.carbon.identity.notification.sender.tenant.config.dto.EmailSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.PushSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.SMSSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementException;

import java.util.List;

/**
 * Service of Notification Sender Management operations.
 */
public interface NotificationSenderManagementService {

    /**
     * Create an email sender resource with a resource file.
     *
     * @param emailSender Email sender post request.
     * @return Email sender.
     * @throws NotificationSenderManagementException    Notification sender management exception.
     */
    EmailSenderDTO addEmailSender(EmailSenderDTO emailSender) throws NotificationSenderManagementException;

    /**
     * Create a sms sender resource with a resource file.
     *
     * @param smsSender SMS sender post request.
     * @return SMS sender.
     * @throws NotificationSenderManagementException    Notification sender management exception.
     */
    SMSSenderDTO addSMSSender(SMSSenderDTO smsSender) throws NotificationSenderManagementException;

    /**
     * Create a push sender resource with a resource file.
     *
     * @param pushSender Push sender post request.
     * @return Push sender.
     * @throws NotificationSenderManagementException    Notification sender management exception.
     */
    default PushSenderDTO addPushSender(PushSenderDTO pushSender) throws NotificationSenderManagementException {

        return null;
    }

    /**
     * Delete a SMS/Email sender by name.
     *
     * @param senderName Name of the notification sender.
     * @throws NotificationSenderManagementException    Notification sender management exception.
     */
    void deleteNotificationSender(String senderName) throws NotificationSenderManagementException;

    /**
     * Retrieve the email sender details by name.
     *
     * @param senderName Email sender's name.
     * @return Email sender.
     * @throws NotificationSenderManagementException    Notification sender management exception.
     */
    EmailSenderDTO getEmailSender(String senderName) throws NotificationSenderManagementException;

    /**
     * Retrieve the sms sender details by name.
     *
     * @param senderName SMS sender's name.
     * @return SMS sender.
     * @throws NotificationSenderManagementException    Notification sender management exception.
     */
    SMSSenderDTO getSMSSender(String senderName) throws NotificationSenderManagementException;

    /**
     * Retrieve the sms sender details by name for the current organization with an option to exclude inherited
     * tenant settings.
     * When the 'inheritTenantSettings' flag is set to true, the method includes configurations from the parent tenant.
     * When set to false, it retrieves only the configurations explicitly set for the current tenant.
     *
     * @param inheritTenantSettings Whether to retrieve inherit tenant settings.
     * @param senderName SMS sender's name.
     * @return SMS sender.
     * @throws NotificationSenderManagementException    Notification sender management exception.
     */
    default SMSSenderDTO getSMSSender(String senderName, boolean inheritTenantSettings)
            throws NotificationSenderManagementException {

        return getSMSSender(senderName);
    }

    /**
     * Retrieve the push sender details by name for the current organization with an option to exclude inherited
     * tenant settings.
     * When the 'inheritTenantSettings' flag is set to true, the method includes configurations from the parent tenant.
     * When set to false, it retrieves only the configurations explicitly set for the current tenant.
     *
     * @param senderName Push sender's name.
     * @param inheritTenantSettings Whether to retrieve inherit tenant settings when called from a
     *                              sub organization context.
     * @return null.
     * @throws NotificationSenderManagementException    Notification sender management exception.
     */
    default PushSenderDTO getPushSender(String senderName, boolean inheritTenantSettings)
            throws NotificationSenderManagementException {

        return null;
    }

    /**
     * Retrieve all email senders of the tenant.
     *
     * @return Email senders of the tenant.
     * @throws NotificationSenderManagementException    Notification sender management exception.
     */
    List<EmailSenderDTO> getEmailSenders() throws NotificationSenderManagementException;

    /**
     * Retrieve all sms senders of the tenant.
     *
     * @return SMS senders of the tenant.
     * @throws NotificationSenderManagementException    Notification sender management exception.
     */
    List<SMSSenderDTO> getSMSSenders() throws NotificationSenderManagementException;

    /**
     * Retrieves all SMS senders configured for the current tenant with an option to exclude inherited tenant settings.
     * When the 'inheritTenantSettings' flag is set to true, the method includes configurations from the parent tenant.
     * When set to false, it retrieves only the configurations explicitly set for the current tenant.
     *
     * @param inheritTenantSettings Whether to retrieve inherit tenant settings.
     * @return SMS senders of the tenant.
     * @throws NotificationSenderManagementException    Notification sender management exception.
     */
    default List<SMSSenderDTO> getSMSSenders(boolean inheritTenantSettings)
            throws NotificationSenderManagementException {

        return getSMSSenders();
    }

    /**
     * Retrieves all push senders configured for the current tenant with an option to exclude inherited tenant settings.
     * When the 'inheritTenantSettings' flag is set to true, the method includes configurations from the parent tenant.
     * When set to false, it retrieves only the configurations explicitly set for the current tenant.
     *
     * @param inheritTenantSettings Whether to retrieve inherit tenant settings.
     * @return Null.
     * @throws NotificationSenderManagementException    Notification sender management exception.
     */
    default List<PushSenderDTO> getPushSenders(boolean inheritTenantSettings)
            throws NotificationSenderManagementException {

        return null;
    }

    /**
     * Update email sender details.
     *
     * @param emailSender Email sender's updated configurations.
     * @return Updated email sender.
     * @throws NotificationSenderManagementException    Notification sender management exception.
     */
    EmailSenderDTO updateEmailSender(EmailSenderDTO emailSender) throws NotificationSenderManagementException;

    /**
     * Update sms sender details.
     *
     * @param smsSender SMS sender's updated configurations.
     * @return Updated SMS sender.
     * @throws NotificationSenderManagementException    Notification sender management exception.
     */
    SMSSenderDTO updateSMSSender(SMSSenderDTO smsSender) throws NotificationSenderManagementException;

    /**
     * Update push sender details.
     *
     * @param pushSender Push sender's updated configurations.
     * @return Updated Push sender.
     * @throws NotificationSenderManagementException    Notification sender management exception.
     */
    default PushSenderDTO updatePushSender(PushSenderDTO pushSender) throws NotificationSenderManagementException {

        return null;
    }
}
