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
import org.wso2.carbon.identity.configuration.mgt.core.model.Attribute;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.SMSSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementClientException;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementException;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementServerException;
import org.wso2.carbon.identity.notification.sender.tenant.config.internal.NotificationSenderTenantConfigDataHolder;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CONTENT_TYPE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_ERROR_GETTING_NOTIFICATION_SENDER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.INTERNAL_PROPERTIES;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.KEY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PROVIDER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PROVIDER_URL;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PUBLISHER_RESOURCE_TYPE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.RESOURCE_NOT_EXISTS_ERROR_CODE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SECRET;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SENDER;

/**
 * Abstract Handler class for notifications sent through different channels.
 */
public abstract class ChannelConfigurationHandler {

    public abstract String getName();
    public abstract SMSSenderDTO addSMSSender(SMSSenderDTO smsSender) throws
            NotificationSenderManagementException;

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

    protected NotificationSenderManagementException handleConfigurationMgtException(ConfigurationManagementException e,
         NotificationSenderManagementConstants.ErrorMessage error, String data) {

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

        SMSSenderDTO smsSender = new SMSSenderDTO();
        smsSender.setName(resource.getResourceName());
        // Skip STREAM_NAME, STREAM_VERSION and PUBLISHER_TYPE_PROPERTY properties which are stored for internal use.
        Map<String, String> attributesMap =
                resource.getAttributes().stream()
                        .filter(attribute -> !(INTERNAL_PROPERTIES.contains(attribute.getKey())))
                        .collect(Collectors.toMap(Attribute::getKey, Attribute::getValue));
        attributesMap.forEach((key, value) -> {
            switch (key) {
                case PROVIDER:
                    smsSender.setProvider(value);
                    break;
                case PROVIDER_URL:
                    smsSender.setProviderURL(value);
                    break;
                case KEY:
                    smsSender.setKey(value);
                    break;
                case SECRET:
                    smsSender.setSecret(value);
                    break;
                case SENDER:
                    smsSender.setSender(value);
                    break;
                case CONTENT_TYPE:
                    smsSender.setContentType(value);
                    break;
                default:
                    smsSender.getProperties().put(key, value);
            }
        });
        return smsSender;
    }
}
