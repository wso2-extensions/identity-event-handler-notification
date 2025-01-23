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

package org.wso2.carbon.identity.notification.sender.tenant.config.handlers;

import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementClientException;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementException;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementServerException;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.EmailSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.SMSSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementClientException;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementException;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementServerException;
import org.wso2.carbon.identity.notification.sender.tenant.config.internal.NotificationSenderTenantConfigDataHolder;
import org.wso2.carbon.identity.notification.sender.tenant.config.utils.NotificationSenderUtils;

import java.util.Optional;

import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_ERROR_GETTING_NOTIFICATION_SENDER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PUBLISHER_RESOURCE_TYPE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.RESOURCE_NOT_EXISTS_ERROR_CODE;

/**
 * Abstract Handler class for notifications sent through different channels.
 */
public abstract class ChannelConfigurationHandler {

    /**
     * Method should return the friendly name of the channel configuration handler
     *
     * @return friendly name of the channel handler.
     */
    public abstract String getName();

    /**
     * Method holds the implementation of adding the Email sender configurations.
     * TODO: Adding this since channel support will be added to email notification senders too.
     *
     * @param emailSender contains the configurations of Email sender.
     * @return persisted Email notification sender configuration.
     * @throws NotificationSenderManagementException on errors when adding the Email notification sender configurations.
     */
    public abstract EmailSenderDTO addEmailSender(EmailSenderDTO emailSender)
            throws NotificationSenderManagementException;

    /**
     * Method holds the implementation of adding the SMS sender configurations.
     *
     * @param smsSender contains the configurations of SMS sender.
     * @return persisted SMS notification sender configuration.
     * @throws NotificationSenderManagementException on errors when adding the SMS notification sender configurations.
     */
    public abstract SMSSenderDTO addSMSSender(SMSSenderDTO smsSender) throws
            NotificationSenderManagementException;


    /**
     * Method holds the implementation of updating the Email sender configurations.
     * TODO: Adding this since channel support will be added to email notification senders too.
     *
     * @param emailSender contains the configurations of Email sender.
     * @return persisted Email notification sender configuration.
     * @throws NotificationSenderManagementException on errors when updating the Email notification sender
     *                                               configurations.
     */
    public abstract EmailSenderDTO updateEmailSender(EmailSenderDTO emailSender)
            throws NotificationSenderManagementException;

    /**
     * Method holds the implementation of updating the SMS sender configurations.
     *
     * @param smsSender contains the configurations of SMS sender.
     * @return persisted SMS notification sender configuration.
     * @throws NotificationSenderManagementException on errors when adding the SMS notification sender configurations.
     */
    public abstract SMSSenderDTO updateSMSSender(SMSSenderDTO smsSender) throws NotificationSenderManagementException;

    /**
     * Method holds the common implementation of deleting the notification sender configurations.
     *
     * @param senderName contains the configurations of SMS sender.
     * @throws NotificationSenderManagementException on errors when deleting the notification sender configurations.
     */
    public abstract void deleteNotificationSender(String senderName) throws NotificationSenderManagementException;

    /**
     * Method returns the optional instance of {@link Resource} containing the sender configurations.
     *
     * @param resourceName name of the notification sender.
     * @return optional instance of {@link Resource}.
     * @throws NotificationSenderManagementException on errors when retrieving the notification sender resources.
     */
    protected Optional<Resource> getPublisherResource(String resourceName)
            throws NotificationSenderManagementException {

        try {
            return Optional.ofNullable(NotificationSenderTenantConfigDataHolder.getInstance().getConfigurationManager()
                    .getResource(PUBLISHER_RESOURCE_TYPE, resourceName));

        } catch (ConfigurationManagementException e) {
            // If the resource not exists handling it as null and throw different error code.
            if (!RESOURCE_NOT_EXISTS_ERROR_CODE.equals(e.getErrorCode())) {
                throw handleConfigurationMgtException(e, ERROR_CODE_ERROR_GETTING_NOTIFICATION_SENDER, resourceName);
            }
        }
        return Optional.empty();
    }

    /**
     * Handle {@link ConfigurationManagementException} and wrapping the exception as
     * {@link NotificationSenderManagementException}.
     *
     * @param e     instance of {@link ConfigurationManagementException}.
     * @param error Enum errors related to notification senders operations.
     * @param data  Data related to the errors when processing notification senders.
     * @return NotificationSenderManagementConstants exception wrapping the config management exception.
     */
    protected NotificationSenderManagementException handleConfigurationMgtException(ConfigurationManagementException e,
                                                                                    ErrorMessage error, String data) {

        if (e instanceof ConfigurationManagementClientException) {
            return new NotificationSenderManagementClientException(error, data, e);
        } else if (e instanceof ConfigurationManagementServerException) {
            return new NotificationSenderManagementServerException(error, data, e);
        } else {
            return new NotificationSenderManagementException(error, data, e);
        }
    }

    /**
     * Build an SMS sender response from SMS sender's resource object.
     *
     * @param resource SMS sender resource object.
     * @return SMS sender response.
     */
    protected SMSSenderDTO buildSmsSenderFromResource(Resource resource) {

        return NotificationSenderUtils.buildSmsSenderFromResource(resource);
    }
}
