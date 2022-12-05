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

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.ClusteringMessage;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.email.mgt.SMSProviderPayloadTemplateManager;
import org.wso2.carbon.email.mgt.model.SMSProviderTemplate;
import org.wso2.carbon.event.publisher.core.config.EventPublisherConfiguration;
import org.wso2.carbon.event.publisher.core.exception.EventPublisherConfigurationException;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementException;
import org.wso2.carbon.identity.configuration.mgt.core.model.Attribute;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.configuration.mgt.core.model.ResourceFile;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants;
import org.wso2.carbon.identity.notification.sender.tenant.config.clustering.EventPublisherClusterDeleteMessage;
import org.wso2.carbon.identity.notification.sender.tenant.config.clustering.EventPublisherClusterInvalidationMessage;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.EmailSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.SMSSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementClientException;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementException;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementServerException;
import org.wso2.carbon.identity.notification.sender.tenant.config.internal.NotificationSenderTenantConfigDataHolder;
import org.wso2.carbon.identity.tenant.resource.manager.exception.TenantResourceManagementClientException;
import org.wso2.carbon.identity.tenant.resource.manager.exception.TenantResourceManagementException;
import org.wso2.carbon.identity.tenant.resource.manager.exception.TenantResourceManagementServerException;
import org.wso2.carbon.identity.tenant.resource.manager.util.ResourceUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CONTENT_TYPE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.DEFAULT_HANDLER_NAME;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.DEFAULT_SMS_PUBLISHER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_CONFLICT_PUBLISHER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_ERROR_ADDING_NOTIFICATION_SENDER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_ERROR_DELETING_NOTIFICATION_SENDER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_ERROR_UPDATING_NOTIFICATION_SENDER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_NO_ACTIVE_PUBLISHERS_FOUND;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_PARSER_CONFIG_EXCEPTION;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_PUBLISHER_NOT_EXISTS_IN_SUPER_TENANT;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_RESOURCE_RE_DEPLOY_ERROR;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_SERVER_ERRORS_GETTING_EVENT_PUBLISHER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_SMS_PAYLOAD_NOT_FOUND;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_SMS_PROVIDER_REQUIRED;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_SMS_PROVIDER_URL_REQUIRED;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_TRANSFORMER_EXCEPTION;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.INLINE_BODY_PROPERTY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.KEY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PROVIDER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PROVIDER_URL;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PUBLISHER_RESOURCE_TYPE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PUBLISHER_TYPE_PROPERTY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SECRET;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SENDER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SMS_PUBLISHER_TYPE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.STREAM_NAME;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.STREAM_VERSION;
import static org.wso2.carbon.identity.notification.sender.tenant.config.utils.NotificationSenderUtils.generateSMSPublisher;

/**
 * Configuration handler class for notifications sent through default channel.
 */
public class DefaultChannelConfigurationHandler extends ChannelConfigurationHandler {

    public static final int MAX_RETRY_COUNT = 60;
    private static final Log log = LogFactory.getLog(DefaultChannelConfigurationHandler.class);

    @Override
    public String getName() {

        return DEFAULT_HANDLER_NAME;
    }

    @Override
    public EmailSenderDTO addEmailSender(EmailSenderDTO emailSender) throws NotificationSenderManagementException {

        throw new IllegalArgumentException("Channel support for the email notifications is not implemented.");
    }

    @Override
    public SMSSenderDTO addSMSSender(SMSSenderDTO smsSender) throws NotificationSenderManagementException {
        // Set the default publisher name if name is not defined.
        if (StringUtils.isEmpty(smsSender.getName())) {
            smsSender.setName(DEFAULT_SMS_PUBLISHER);
        }

        validateSMSSender(smsSender);

        Optional<Resource> resourceOptional = getPublisherResource(smsSender.getName());

        if (resourceOptional.isPresent()) {
            throw new NotificationSenderManagementClientException(ERROR_CODE_CONFLICT_PUBLISHER, smsSender.getSender());
        }

        Map<String, String> defaultPublisherProperties = getDefaultPublisherProperties(smsSender.getName());
        // Add the publisher type to the new publisher.
        defaultPublisherProperties.put(PUBLISHER_TYPE_PROPERTY, SMS_PUBLISHER_TYPE);
        smsSender.getProperties().putAll(defaultPublisherProperties);
        Resource smsSenderResource = buildResourceFromSmsSender(smsSender);

        try {
            /*
            The input properties will be saved as the attributes of a resource to return to the notification-senders
            API responses.
            Also, an event publisher file is generated with the input values and save it as a file of the resource.
            It is used when loading tenant wise event publisher loading flow.
             */
            NotificationSenderTenantConfigDataHolder.getInstance().getConfigurationManager()
                    .addResource(PUBLISHER_RESOURCE_TYPE, smsSenderResource);

            reDeployEventPublisherConfiguration(smsSenderResource);
        } catch (ConfigurationManagementException e) {
            throw handleConfigurationMgtException(e, ERROR_CODE_ERROR_ADDING_NOTIFICATION_SENDER, smsSender.getName());
        }
        return buildSmsSenderFromResource(smsSenderResource);
    }

    @Override
    public void deleteNotificationSender(String senderName) throws NotificationSenderManagementException {

        try {
            NotificationSenderTenantConfigDataHolder.getInstance().getResourceManager()
                    .removeEventPublisherConfiguration(PUBLISHER_RESOURCE_TYPE, senderName);
            NotificationSenderTenantConfigDataHolder.getInstance().getConfigurationManager()
                    .deleteResource(PUBLISHER_RESOURCE_TYPE, senderName);
            sendEventPublisherClusterDeleteMessage(senderName);

        } catch (ConfigurationManagementException e) {
            throw handleConfigurationMgtException(e, ERROR_CODE_ERROR_DELETING_NOTIFICATION_SENDER, senderName);
        } catch (TenantResourceManagementException e) {
            throw handleTenantResourceManagementException(e, ERROR_CODE_ERROR_DELETING_NOTIFICATION_SENDER, senderName);
        }
    }

    @Override
    public EmailSenderDTO updateEmailSender(EmailSenderDTO emailSender) throws NotificationSenderManagementException {

        throw new IllegalArgumentException("Channel support for the email notifications is not implemented.");
    }

    @Override
    public SMSSenderDTO updateSMSSender(SMSSenderDTO smsSender) throws NotificationSenderManagementException {

        validateSMSSender(smsSender);

        Map<String, String> defaultPublisherProperties = getDefaultPublisherProperties(smsSender.getName());
        // Add the publisher type to the new publisher.
        defaultPublisherProperties.put(PUBLISHER_TYPE_PROPERTY, SMS_PUBLISHER_TYPE);
        smsSender.getProperties().putAll(defaultPublisherProperties);

        Resource smsSenderResource = buildResourceFromSmsSender(smsSender);

        try {
            NotificationSenderTenantConfigDataHolder.getInstance().getConfigurationManager()
                    .replaceResource(PUBLISHER_RESOURCE_TYPE, smsSenderResource);
            reDeployEventPublisherConfiguration(smsSenderResource);

        } catch (ConfigurationManagementException e) {
            throw handleConfigurationMgtException(e, ERROR_CODE_ERROR_UPDATING_NOTIFICATION_SENDER,
                    smsSender.getName());
        }
        return buildSmsSenderFromResource(smsSenderResource);
    }

    private void validateSMSSender(SMSSenderDTO smsSender) throws NotificationSenderManagementClientException {

        SMSProviderPayloadTemplateManager smsProviderPayloadTemplateManager =
                NotificationSenderTenantConfigDataHolder.getInstance().getSmsProviderPayloadTemplateManager();
        Map<String, String> properties = smsSender.getProperties();
        if (StringUtils.isEmpty(smsSender.getProvider())) {
            throw new NotificationSenderManagementClientException(ERROR_CODE_SMS_PROVIDER_REQUIRED);
        }
        if (StringUtils.isEmpty(properties.get(INLINE_BODY_PROPERTY))) {
            SMSProviderTemplate sendSmsAPIPayload = smsProviderPayloadTemplateManager
                    .getSMSProviderPayloadTemplateByProvider(smsSender.getProvider());
            if (sendSmsAPIPayload == null) {
                throw new NotificationSenderManagementClientException(ERROR_CODE_SMS_PAYLOAD_NOT_FOUND);
            }
        }
        if (StringUtils.isEmpty(smsSender.getProviderURL())) {
            throw new NotificationSenderManagementClientException(ERROR_CODE_SMS_PROVIDER_URL_REQUIRED);
        }
    }

    /**
     * Get default properties of super tenant Publisher.
     *
     * @param publisherName Name of the publisher.
     * @return Map of Properties.
     */
    private Map<String, String> getDefaultPublisherProperties(String publisherName)
            throws NotificationSenderManagementException {

        Map<String, String> properties = new HashMap<>();
        EventPublisherConfiguration publisherInSuperTenant = getPublisherInSuperTenant(publisherName);

        properties.put(STREAM_NAME, publisherInSuperTenant.getFromStreamName());
        properties.put(STREAM_VERSION, publisherInSuperTenant.getFromStreamVersion());
        return properties;
    }

    /**
     * Build a resource object from SMS Sender post body.
     *
     * @param smsSender SMS sender post body.
     * @return Resource object.
     */
    private Resource buildResourceFromSmsSender(SMSSenderDTO smsSender)
            throws NotificationSenderManagementServerException {

        InputStream inputStream;
        try {
            inputStream = generateSMSPublisher(smsSender);
        } catch (ParserConfigurationException e) {
            throw new NotificationSenderManagementServerException(ERROR_CODE_PARSER_CONFIG_EXCEPTION,
                    e.getMessage(), e);
        } catch (TransformerException e) {
            throw new NotificationSenderManagementServerException(ERROR_CODE_TRANSFORMER_EXCEPTION, e.getMessage(), e);
        }

        Resource resource = new Resource();
        resource.setResourceName(smsSender.getName());
        Map<String, String> smsSenderAttributes = new HashMap<>();
        smsSenderAttributes.put(PROVIDER, smsSender.getProvider());
        smsSenderAttributes.put(PROVIDER_URL, smsSender.getProviderURL());
        smsSenderAttributes.put(KEY, smsSender.getKey());
        smsSenderAttributes.put(SECRET, smsSender.getSecret());
        smsSenderAttributes.put(SENDER, smsSender.getSender());
        smsSenderAttributes.put(CONTENT_TYPE, smsSender.getContentType());
        smsSenderAttributes.putAll(smsSender.getProperties());
        List<Attribute> resourceAttributes =
                smsSenderAttributes.entrySet().stream().filter(attribute -> attribute.getValue() != null)
                        .map(attribute -> new Attribute(attribute.getKey(), attribute.getValue()))
                        .collect(Collectors.toList());
        resource.setAttributes(resourceAttributes);
        // Set file.
        ResourceFile file = new ResourceFile();
        file.setName(smsSender.getName());
        file.setInputStream(inputStream);
        List<ResourceFile> resourceFiles = new ArrayList<>();
        resourceFiles.add(file);
        resource.setFiles(resourceFiles);
        return resource;
    }

    private void reDeployEventPublisherConfiguration(Resource resource) {

        ResourceFile file = resource.getFiles().get(0);
        try {
            NotificationSenderTenantConfigDataHolder.getInstance().getResourceManager()
                    .addEventPublisherConfiguration(file);
        } catch (TenantResourceManagementException e) {
            log.warn(ERROR_CODE_RESOURCE_RE_DEPLOY_ERROR.getMessage() + e.getMessage());
        }
        sendEventPublisherClusterInvalidationMessage(file);
    }

    /**
     * Validate the email Sender and get the corresponding super tenant's event publisher configuration.
     *
     * @param eventPublisherName Event publisher name.
     * @return Corresponding super tenant's event publisher's configuration.
     */
    private EventPublisherConfiguration getPublisherInSuperTenant(String eventPublisherName)
            throws NotificationSenderManagementException {

        EventPublisherConfiguration publisherInSuperTenant;
        List<EventPublisherConfiguration> activeEventPublisherConfigurations;
        try {
            try {
                ResourceUtils.startSuperTenantFlow();
                activeEventPublisherConfigurations = NotificationSenderTenantConfigDataHolder.getInstance()
                        .getCarbonEventPublisherService().getAllActiveEventPublisherConfigurations();
                if (activeEventPublisherConfigurations == null) {
                    throw new NotificationSenderManagementClientException(ERROR_CODE_NO_ACTIVE_PUBLISHERS_FOUND);
                }
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
            // Check whether the super tenant has a publisher with the defined name.
            publisherInSuperTenant = activeEventPublisherConfigurations.stream()
                    .filter(publisher -> publisher.getEventPublisherName().equals(eventPublisherName)).findAny()
                    .orElse(null);
            if (publisherInSuperTenant == null) {
                throw new NotificationSenderManagementClientException(ERROR_CODE_PUBLISHER_NOT_EXISTS_IN_SUPER_TENANT,
                        eventPublisherName);
            }
        } catch (EventPublisherConfigurationException e) {
            throw new NotificationSenderManagementServerException(ERROR_CODE_SERVER_ERRORS_GETTING_EVENT_PUBLISHER,
                    eventPublisherName, e);
        }
        return publisherInSuperTenant;
    }

    private void sendEventPublisherClusterInvalidationMessage(ResourceFile resourceFile) {

        if (getClusteringAgent() == null) {
            return;
        }

        EventPublisherClusterInvalidationMessage message = new EventPublisherClusterInvalidationMessage(resourceFile,
                PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId());

        if (log.isDebugEnabled()) {
            log.debug("Sending cluster invalidation message to other cluster nodes for event publisher update for "
                    + resourceFile.getName());
        }

        sendClusterMessage(message, resourceFile.getName());
    }

    private ClusteringAgent getClusteringAgent() {

        return NotificationSenderTenantConfigDataHolder.getInstance().getClusteringAgent();
    }

    private void sendClusterMessage(ClusteringMessage message, String senderName) {

        int numberOfRetries = 0;
        while (numberOfRetries < MAX_RETRY_COUNT) {
            try {
                getClusteringAgent().sendMessage(message, true);
                if (log.isDebugEnabled()) {
                    log.debug("Sent [" + message + "]");
                }
                break;
            } catch (ClusteringFault e) {
                numberOfRetries++;
                if (numberOfRetries < MAX_RETRY_COUNT) {
                    log.warn("Could not send cluster invalidation message for event publisher '"
                            + senderName + "' change. Retry will be attempted in 2s. Request: " +
                            message, e);
                } else {
                    log.error("Could not send cluster invalidation message for event publisher '"
                            + senderName + " change'. Several retries failed. Request:" + message, e);
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                    // Do nothing.
                }
            }
        }
    }

    private void sendEventPublisherClusterDeleteMessage(String senderName) {

        if (getClusteringAgent() == null) {
            return;
        }

        EventPublisherClusterDeleteMessage message = new EventPublisherClusterDeleteMessage(
                NotificationSenderManagementConstants.PUBLISHER_RESOURCE_TYPE, senderName,
                PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId());

        sendClusterMessage(message, senderName);
    }

    private NotificationSenderManagementException handleTenantResourceManagementException(
            TenantResourceManagementException e,
            NotificationSenderManagementConstants.ErrorMessage error, String data) {

        if (e instanceof TenantResourceManagementClientException) {
            return new NotificationSenderManagementClientException(error, data, e);
        } else if (e instanceof TenantResourceManagementServerException) {
            return new NotificationSenderManagementServerException(error, data, e);
        } else {
            return new NotificationSenderManagementException(error, data, e);
        }
    }
}
