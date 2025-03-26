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

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.ClusteringMessage;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.event.publisher.core.config.EventPublisherConfiguration;
import org.wso2.carbon.event.publisher.core.exception.EventPublisherConfigurationException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementClientException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementServerException;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants.Error;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementClientException;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementException;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementServerException;
import org.wso2.carbon.identity.configuration.mgt.core.model.Attribute;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.configuration.mgt.core.model.ResourceFile;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resources;
import org.wso2.carbon.identity.notification.push.provider.PushProvider;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage;
import org.wso2.carbon.identity.notification.sender.tenant.config.clustering.EventPublisherClusterInvalidationMessage;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.EmailSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.PushSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.SMSSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementClientException;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementException;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementServerException;
import org.wso2.carbon.identity.notification.sender.tenant.config.handlers.ChannelConfigurationHandler;
import org.wso2.carbon.identity.notification.sender.tenant.config.internal.NotificationSenderTenantConfigDataHolder;
import org.wso2.carbon.identity.notification.sender.tenant.config.utils.NotificationSenderUtils;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.tenant.resource.manager.exception.TenantResourceManagementException;
import org.wso2.carbon.identity.tenant.resource.manager.util.ResourceUtils;
import org.wso2.carbon.idp.mgt.model.ConnectedAppsResult;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import static org.wso2.carbon.identity.configuration.mgt.core.constant.ConfigurationConstants.ErrorMessages.ERROR_CODE_RESOURCE_DOES_NOT_EXISTS;
import static org.wso2.carbon.identity.configuration.mgt.core.constant.ConfigurationConstants.ErrorMessages.ERROR_CODE_RESOURCE_TYPE_DOES_NOT_EXISTS;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.AUTH_TYPE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.BASIC;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CHANNEL_TYPE_PROPERTY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CLIENT_CREDENTIAL;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CLIENT_ID;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CLIENT_SECRET;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.DEFAULT_EMAIL_PUBLISHER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.DEFAULT_HANDLER_NAME;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.DEFAULT_PUSH_PUBLISHER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.DEFAULT_SMS_PUBLISHER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.DISPLAY_NAME;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.EMAIL_PUBLISHER_TYPE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_CHANNEL_TYPE_UPDATE_NOT_ALLOWED;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_CONFIGURATION_HANDLER_NOT_FOUND;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_CONFLICT_PUBLISHER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_CONNECTED_APPLICATION_EXISTS;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_ERROR_ADDING_NOTIFICATION_SENDER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_ERROR_GETTING_NOTIFICATION_SENDER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_ERROR_GETTING_NOTIFICATION_SENDERS_BY_TYPE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_ERROR_UPDATING_NOTIFICATION_SENDER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_NO_ACTIVE_PUBLISHERS_FOUND;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_NO_RESOURCE_EXISTS;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_PARSER_CONFIG_EXCEPTION;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_PUBLISHER_NOT_EXISTS;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_PUBLISHER_NOT_EXISTS_IN_SUPER_TENANT;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_RESOURCE_RE_DEPLOY_ERROR;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_SERVER_ERRORS_GETTING_EVENT_PUBLISHER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_TRANSFORMER_EXCEPTION;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_VALIDATING_CONNECTED_APPS;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.FROM_ADDRESS;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.INTERNAL_PROPERTIES;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.MY_ACCOUNT_SMS_RESOURCE_NAME;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.MY_ACCOUNT_SMS_RESOURCE_TYPE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PASSWORD;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PUBLISHER_RESOURCE_TYPE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PUBLISHER_TYPE_PROPERTY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PUSH_PUBLISHER_TYPE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.REPLY_TO_ADDRESS;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.RESOURCE_NOT_EXISTS_ERROR_CODE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SCOPES;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SMS_PUBLISHER_TYPE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SMTP_PORT;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SMTP_SERVER_HOST;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.STREAM_NAME;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.STREAM_VERSION;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.TOKEN_ENDPOINT;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.USERNAME;
import static org.wso2.carbon.identity.notification.sender.tenant.config.utils.NotificationSenderUtils.buildPushSenderFromResource;
import static org.wso2.carbon.identity.notification.sender.tenant.config.utils.NotificationSenderUtils.buildResourceFromPushSender;
import static org.wso2.carbon.identity.notification.sender.tenant.config.utils.NotificationSenderUtils.buildSmsSenderFromResource;
import static org.wso2.carbon.identity.notification.sender.tenant.config.utils.NotificationSenderUtils.deletePushSenderSecretProperties;
import static org.wso2.carbon.identity.notification.sender.tenant.config.utils.NotificationSenderUtils.generateEmailPublisher;
import static org.wso2.carbon.identity.notification.sender.tenant.config.utils.NotificationSenderUtils.getPushProvider;
import static org.wso2.carbon.identity.notification.sender.tenant.config.utils.NotificationSenderUtils.updatePushSenderCredentials;

/**
 * OSGi service of Notification Sender Management operations.
 */
public class NotificationSenderManagementServiceImpl implements NotificationSenderManagementService {

    private static final Log log = LogFactory.getLog(NotificationSenderManagementServiceImpl.class);
    public static final int MAX_RETRY_COUNT = 60;
    public static final String SMS_OTP_AUTHENTICATOR = "sms-otp-authenticator";

    static final Map<String, String> SENDERS = new HashMap<String, String>() { {
        put("SMSPublisher", SMS_OTP_AUTHENTICATOR);
    } };

    @Override
    public EmailSenderDTO addEmailSender(EmailSenderDTO emailSender) throws NotificationSenderManagementException {

        validateInputs(emailSender);

        // Set the default publisher name if name is not defined.
        if (StringUtils.isBlank(emailSender.getName())) {
            emailSender.setName(DEFAULT_EMAIL_PUBLISHER);
        }

        // Check whether a publisher already exists with the same name in the particular tenant to be added.
        Optional<Resource> resourceOptional = getPublisherResource(emailSender.getName());

        if (resourceOptional.isPresent()) {
            throw new NotificationSenderManagementClientException(ERROR_CODE_CONFLICT_PUBLISHER, emailSender.getName());
        }

        Map<String, String> defaultPublisherProperties = getDefaultPublisherProperties(emailSender.getName());
        // Add the publisher type to the new publisher.
        defaultPublisherProperties.put(PUBLISHER_TYPE_PROPERTY, EMAIL_PUBLISHER_TYPE);
        emailSender.getProperties().putAll(defaultPublisherProperties);

        Resource emailSenderResource = buildResourceFromEmailSender(emailSender);

        try {
            /*
            The input properties will be saved as the attributes of a resource to return to the notification-senders
            API responses.
            Also, an event publisher file is generated with the input values and save it as a file of the resource.
            It is used when loading tenant wise event publisher loading flow.
             */
            NotificationSenderTenantConfigDataHolder.getInstance().getConfigurationManager()
                    .addResource(PUBLISHER_RESOURCE_TYPE, emailSenderResource);

            reDeployEventPublisherConfiguration(emailSenderResource);

            return buildEmailSenderFromResource(emailSenderResource);
        } catch (ConfigurationManagementException e) {
            throw handleConfigurationMgtException(e, ERROR_CODE_ERROR_ADDING_NOTIFICATION_SENDER,
                    emailSender.getName());
        }
    }

    private void validateInputs(EmailSenderDTO emailSender) throws NotificationSenderManagementClientException {

        if (StringUtils.isBlank(emailSender.getSmtpServerHost()) ||
                emailSender.getSmtpPort() == null ||
                StringUtils.isBlank(emailSender.getFromAddress()) ||
                StringUtils.isBlank(emailSender.getProperties().get(DISPLAY_NAME)) ||
                StringUtils.isBlank(emailSender.getProperties().get(REPLY_TO_ADDRESS))) {
            throw new NotificationSenderManagementClientException(ErrorMessage.ERROR_CODE_INVALID_INPUTS);
        }

        // if authType is not specified, username and password should be set in the first class attributes.
        if (StringUtils.isBlank(emailSender.getAuthType())) {
            if (StringUtils.isBlank(emailSender.getUsername()) || StringUtils.isBlank(emailSender.getPassword())) {
                throw new NotificationSenderManagementClientException(ErrorMessage.ERROR_CODE_INVALID_INPUTS);
            }
            return;
        }

        // If authType is specified, username & password should NOT be set as first class attributes.
        if (StringUtils.isNotBlank(emailSender.getUsername()) || StringUtils.isNotBlank(emailSender.getPassword())) {
            throw new NotificationSenderManagementClientException(ErrorMessage.ERROR_CODE_INVALID_INPUTS);
        }

        // If authType is set to BASIC, username & password should be set in the properties.
        if (BASIC.equalsIgnoreCase(emailSender.getAuthType())) {
            if (StringUtils.isBlank(emailSender.getProperties().get(USERNAME)) ||
                    StringUtils.isBlank(emailSender.getProperties().get(PASSWORD))) {
                throw new NotificationSenderManagementClientException(ErrorMessage.ERROR_CODE_INVALID_INPUTS);
            }
            return;
        }

        // If authType is set to CLIENT_CREDENTIAL, client_id, client_secret, token_endpoint & scopes
        // should be set in the properties.
        if (CLIENT_CREDENTIAL.equalsIgnoreCase(emailSender.getAuthType())) {
            if (StringUtils.isBlank(emailSender.getProperties().get(CLIENT_ID)) ||
                    StringUtils.isBlank(emailSender.getProperties().get(CLIENT_SECRET)) ||
                    StringUtils.isBlank(emailSender.getProperties().get(TOKEN_ENDPOINT)) ||
                    StringUtils.isBlank(emailSender.getProperties().get(SCOPES))) {
                throw new NotificationSenderManagementClientException(ErrorMessage.ERROR_CODE_INVALID_INPUTS);
            }
            return;
        }

        throw new NotificationSenderManagementClientException(ErrorMessage.ERROR_CODE_INVALID_INPUTS);
    }

    @Override
    public SMSSenderDTO addSMSSender(SMSSenderDTO smsSender) throws NotificationSenderManagementException {

        ChannelConfigurationHandler configurationHandler = NotificationSenderTenantConfigDataHolder.getInstance()
                .getConfigurationHandlerMap().get(getChannelTypeFromSMSSenderDTO(smsSender));

        if (configurationHandler != null) {
            return configurationHandler.addSMSSender(smsSender);
        } else {
            throw new NotificationSenderManagementClientException(ERROR_CODE_CONFIGURATION_HANDLER_NOT_FOUND);
        }
    }

    @Override
    public PushSenderDTO addPushSender(PushSenderDTO pushSender) throws NotificationSenderManagementException {

        // Set the default publisher name if name is not defined.
        if (StringUtils.isEmpty(pushSender.getName())) {
            pushSender.setName(DEFAULT_PUSH_PUBLISHER);
        }
        Optional<Resource> resourceOptional = getPublisherResource(pushSender.getName());
        if (resourceOptional.isPresent()) {
            throw new NotificationSenderManagementClientException(ERROR_CODE_CONFLICT_PUBLISHER, pushSender.getName());
        }
        pushSender.getProperties().put(PUBLISHER_TYPE_PROPERTY, PUSH_PUBLISHER_TYPE);
        PushProvider pushProvider = getPushProvider(pushSender);
        try {
            Resource pushSenderResource = buildResourceFromPushSender(pushSender, pushProvider);
            Resource addedResource = NotificationSenderTenantConfigDataHolder.getInstance().getConfigurationManager()
                    .addResource(PUBLISHER_RESOURCE_TYPE, pushSenderResource);
            String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            updatePushSenderCredentials(pushSender, pushProvider, tenantDomain);
            return buildPushSenderFromResource(addedResource, true);
        } catch (ConfigurationManagementException e) {
            throw handleConfigurationMgtException(e, ERROR_CODE_ERROR_ADDING_NOTIFICATION_SENDER,
                    pushSender.getName());
        }
    }

    @Override
    public void deleteNotificationSender(String senderName) throws NotificationSenderManagementException {

        Resource resource = getPublisherResource(senderName).orElseThrow(() ->
                new NotificationSenderManagementClientException(ERROR_CODE_PUBLISHER_NOT_EXISTS, senderName));

        if (!StringUtils.equals(DEFAULT_SMS_PUBLISHER, senderName) && !canSenderDelete(senderName)) {
            throw new NotificationSenderManagementClientException(ERROR_CODE_CONNECTED_APPLICATION_EXISTS, senderName);
        }

        String channel = getChannelTypeFromResource(resource);
        if (NotificationSenderTenantConfigDataHolder.getInstance().getConfigurationHandlerMap().containsKey(channel)) {

            // If pushSender, delete push provider secret properties from the secret store.
            if (resource.isHasAttribute()) {
                for (Attribute attribute : resource.getAttributes()) {
                    if (PUBLISHER_TYPE_PROPERTY.equals(attribute.getKey())
                            && PUSH_PUBLISHER_TYPE.equals(attribute.getValue())) {
                        deletePushSenderSecretProperties(resource);
                    }
                }
            }

            NotificationSenderTenantConfigDataHolder.getInstance().getConfigurationHandlerMap()
                    .get(channel).deleteNotificationSender(senderName);
        } else {
            throw new NotificationSenderManagementClientException(ERROR_CODE_CONFIGURATION_HANDLER_NOT_FOUND);
        }
    }

    @Override
    public EmailSenderDTO getEmailSender(String senderName) throws NotificationSenderManagementException {

        Optional<Resource> resourceOptional  = getPublisherResource(senderName);
        if (!resourceOptional.isPresent()) {
            throw new NotificationSenderManagementClientException(ERROR_CODE_PUBLISHER_NOT_EXISTS, senderName);
        }
        Resource resource = resourceOptional.get();
        return buildEmailSenderFromResource(resource);
    }

    @Override
    public SMSSenderDTO getSMSSender(String senderName, boolean inheritTenantSettings)
            throws NotificationSenderManagementException {

        if (inheritTenantSettings) {
            return getSMSSender(senderName);
        }

        Optional<Resource> resourceOptional = getPublisherResource(senderName);
        if (!resourceOptional.isPresent()) {
            throw new NotificationSenderManagementClientException(ERROR_CODE_PUBLISHER_NOT_EXISTS, senderName);
        }
        Resource resource = resourceOptional.get();
        return buildSmsSenderFromResource(resource);
    }

    @Override
    public SMSSenderDTO getSMSSender(String senderName) throws NotificationSenderManagementException {

        try {
            Optional<Resource> resourceOptional = getPublisherResource(senderName);
            String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            if (resourceOptional.isPresent()) {
                Resource resource = resourceOptional.get();
                return buildSmsSenderFromResource(resource);
            }
            if (OrganizationManagementUtil.isOrganization(tenantDomain)) {
                resourceOptional =
                        getPublisherResource(NotificationSenderUtils.getPrimaryTenantId(tenantDomain), senderName);
                if (resourceOptional.isPresent()) {
                    Resource resource = resourceOptional.get();
                    return buildSmsSenderFromResource(resource);
                }
            }
            throw new NotificationSenderManagementClientException(ERROR_CODE_PUBLISHER_NOT_EXISTS, senderName);
        } catch (OrganizationManagementException e) {
            throw new NotificationSenderManagementServerException(ERROR_CODE_SERVER_ERRORS_GETTING_EVENT_PUBLISHER,
                    e.getMessage(), e);
        }
    }

    @Override
    public PushSenderDTO getPushSender(String senderName, boolean inheritTenantSettings)
            throws NotificationSenderManagementException {

        Optional<Resource> resourceOptional = getPublisherResource(senderName);
        if (resourceOptional.isPresent()) {
            Resource resource = resourceOptional.get();
            return buildPushSenderFromResource(resource, true);
        }
        if (!inheritTenantSettings) {
            throw new NotificationSenderManagementClientException(ERROR_CODE_PUBLISHER_NOT_EXISTS, senderName);
        }
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        try {
            if (OrganizationManagementUtil.isOrganization(tenantDomain)) {
                resourceOptional = getPublisherResource(
                        NotificationSenderUtils.getPrimaryTenantId(tenantDomain), senderName);
                if (resourceOptional.isPresent()) {
                    Resource resource = resourceOptional.get();
                    return buildPushSenderFromResource(resource, true);
                }
            }
            throw new NotificationSenderManagementClientException(ERROR_CODE_PUBLISHER_NOT_EXISTS, senderName);
        } catch (OrganizationManagementException e) {
            throw new NotificationSenderManagementServerException(ERROR_CODE_SERVER_ERRORS_GETTING_EVENT_PUBLISHER,
                    e.getMessage(), e);
        }
    }

    @Override
    public List<EmailSenderDTO> getEmailSenders() throws NotificationSenderManagementException {

        try {
            Resources publisherResources = NotificationSenderTenantConfigDataHolder.getInstance()
                    .getConfigurationManager()
                    .getResourcesByType(PUBLISHER_RESOURCE_TYPE);
            List<Resource> emailPublisherResources = publisherResources.getResources().stream().filter(resource ->
                    resource.getAttributes().stream().anyMatch(attribute ->
                            PUBLISHER_TYPE_PROPERTY.equals(attribute.getKey()) &&
                                    EMAIL_PUBLISHER_TYPE.equals(attribute.getValue()))).collect(Collectors.toList());
            return emailPublisherResources.stream().map(this::buildEmailSenderFromResource).collect(
                    Collectors.toList());
        } catch (ConfigurationManagementException e) {
            throw handleConfigurationMgtException(e, ERROR_CODE_ERROR_GETTING_NOTIFICATION_SENDERS_BY_TYPE,
                    EMAIL_PUBLISHER_TYPE);
        }
    }

    @Override
    public List<SMSSenderDTO> getSMSSenders(boolean inheritTenantSettings)
            throws NotificationSenderManagementException {

        return retrieveSMSSenders(inheritTenantSettings);
    }

    @Override
    public List<SMSSenderDTO> getSMSSenders() throws NotificationSenderManagementException {

        return retrieveSMSSenders(true);
    }

    private List<SMSSenderDTO> retrieveSMSSenders(boolean inheritTenantSettings)
            throws NotificationSenderManagementException {

        try {
            Resources publisherResources = getPublisherResources(inheritTenantSettings);
            return extractSMSSenders(publisherResources);
        } catch (ConfigurationManagementException e) {
            throw handleConfigurationMgtException(e, ERROR_CODE_ERROR_GETTING_NOTIFICATION_SENDERS_BY_TYPE,
                    SMS_PUBLISHER_TYPE);
        } catch (OrganizationManagementException e) {
            throw new NotificationSenderManagementServerException(ERROR_CODE_SERVER_ERRORS_GETTING_EVENT_PUBLISHER,
                    e.getMessage(), e);
        }
    }

    private Resources getPublisherResources(boolean inheritTenantSettings)
            throws ConfigurationManagementException, OrganizationManagementException {

        Resources publisherResources = NotificationSenderTenantConfigDataHolder.getInstance()
                .getConfigurationManager()
                .getResourcesByType(PUBLISHER_RESOURCE_TYPE);

        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        if (inheritTenantSettings && OrganizationManagementUtil.isOrganization(tenantDomain) &&
                publisherResources.getResources().isEmpty()) {
            publisherResources = NotificationSenderTenantConfigDataHolder.getInstance()
                    .getConfigurationManager()
                    .getResourcesByType(NotificationSenderUtils.getPrimaryTenantId(tenantDomain),
                            PUBLISHER_RESOURCE_TYPE);
        }
        return publisherResources;
    }

    private List<SMSSenderDTO> extractSMSSenders(Resources publisherResources) {

        return publisherResources.getResources().stream()
                .filter(resource -> resource.getAttributes().stream()
                        .anyMatch(attribute -> PUBLISHER_TYPE_PROPERTY.equals(attribute.getKey()) &&
                                SMS_PUBLISHER_TYPE.equals(attribute.getValue())))
                .map(NotificationSenderUtils::buildSmsSenderFromResource)
                .collect(Collectors.toList());
    }

    @Override
    public List<PushSenderDTO> getPushSenders(boolean inheritTenantSettings)
            throws NotificationSenderManagementException {

        try {
            Resources publisherResources = getPublisherResources(inheritTenantSettings);
            List<PushSenderDTO> pushSenders = new ArrayList<>();

            for (Resource resource : publisherResources.getResources()) {
                if (resource.getAttributes().stream().anyMatch(
                        attribute -> PUBLISHER_TYPE_PROPERTY.equals(attribute.getKey())
                        && PUSH_PUBLISHER_TYPE.equals(attribute.getValue()))) {
                    pushSenders.add(buildPushSenderFromResource(resource, true));
                }
            }
            return pushSenders;
        } catch (ConfigurationManagementException e) {
            throw handleConfigurationMgtException(e, ERROR_CODE_ERROR_GETTING_NOTIFICATION_SENDERS_BY_TYPE,
                    SMS_PUBLISHER_TYPE);
        } catch (OrganizationManagementException e) {
            throw new NotificationSenderManagementServerException(ERROR_CODE_SERVER_ERRORS_GETTING_EVENT_PUBLISHER,
                    e.getMessage(), e);
        }
    }

    @Override
    public EmailSenderDTO updateEmailSender(EmailSenderDTO emailSender) throws NotificationSenderManagementException {

        // Check whether a publisher exists to replace.
        Optional<Resource> resourceOptional = getPublisherResource(emailSender.getName());
        if (!resourceOptional.isPresent()) {
            throw new NotificationSenderManagementClientException(ERROR_CODE_NO_RESOURCE_EXISTS, emailSender.getName());
        }

        Map<String, String> defaultPublisherProperties = getDefaultPublisherProperties(emailSender.getName());
        // Add the publisher type to the new publisher.
        defaultPublisherProperties.put(PUBLISHER_TYPE_PROPERTY, EMAIL_PUBLISHER_TYPE);
        emailSender.getProperties().putAll(defaultPublisherProperties);

        Resource emailSenderResource = buildResourceFromEmailSender(emailSender);

        try {
            NotificationSenderTenantConfigDataHolder.getInstance().getConfigurationManager()
                    .replaceResource(PUBLISHER_RESOURCE_TYPE, emailSenderResource);

            reDeployEventPublisherConfiguration(emailSenderResource);

        } catch (ConfigurationManagementException e) {
            throw handleConfigurationMgtException(e, ERROR_CODE_ERROR_UPDATING_NOTIFICATION_SENDER,
                    emailSender.getName());
        }
        return buildEmailSenderFromResource(emailSenderResource);
    }

    @Override
    public SMSSenderDTO updateSMSSender(SMSSenderDTO smsSender) throws NotificationSenderManagementException {

        Resource resource = getPublisherResource(smsSender.getName()).orElseThrow(() ->
                new NotificationSenderManagementClientException(ERROR_CODE_NO_RESOURCE_EXISTS,
                        smsSender.getSender()));

        String channelType = getChannelTypeFromSMSSenderDTO(smsSender);
        String channelTypeOfExistingResource = getChannelTypeFromResource(resource);

        if (!StringUtils.equals(channelType, channelTypeOfExistingResource)) {
            throw new NotificationSenderManagementClientException(ERROR_CODE_CHANNEL_TYPE_UPDATE_NOT_ALLOWED,
                    smsSender.getName());
        }

        ChannelConfigurationHandler configurationHandler = NotificationSenderTenantConfigDataHolder.getInstance()
                .getConfigurationHandlerMap().get(channelType);
        if (configurationHandler != null) {
            return configurationHandler.updateSMSSender(smsSender);
        } else {
            throw new NotificationSenderManagementClientException(ERROR_CODE_CONFIGURATION_HANDLER_NOT_FOUND);
        }
    }

    @Override
    public PushSenderDTO updatePushSender(PushSenderDTO pushSender) throws NotificationSenderManagementException {

        // Check whether a publisher exists to replace.
        Optional<Resource> resourceOptional = getPublisherResource(pushSender.getName());
        if (!resourceOptional.isPresent()) {
            throw new NotificationSenderManagementClientException(ERROR_CODE_NO_RESOURCE_EXISTS, pushSender.getName());
        }
        pushSender.getProperties().put(PUBLISHER_TYPE_PROPERTY, PUSH_PUBLISHER_TYPE);
        PushProvider pushProvider = getPushProvider(pushSender);
        try {
            Resource pushSenderResource1 = buildResourceFromPushSender(pushSender, pushProvider);
            Resource updatedResource = NotificationSenderTenantConfigDataHolder.getInstance().getConfigurationManager()
                    .replaceResource(PUBLISHER_RESOURCE_TYPE, pushSenderResource1);
            String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            updatePushSenderCredentials(pushSender, pushProvider, tenantDomain);
            return buildPushSenderFromResource(updatedResource, true);
        } catch (ConfigurationManagementException e) {
            throw handleConfigurationMgtException(e, ERROR_CODE_ERROR_UPDATING_NOTIFICATION_SENDER,
                    pushSender.getName());
        }
    }

    private Optional<Resource> getPublisherResource(int tenantId, String resourceName)
            throws NotificationSenderManagementException {

        try {
            return Optional.ofNullable(NotificationSenderTenantConfigDataHolder.getInstance().getConfigurationManager()
                    .getResourceByTenantId(tenantId, PUBLISHER_RESOURCE_TYPE, resourceName));

        } catch (ConfigurationManagementException e) {
            // If the resource not exists handling it as null and throw different error code.
            if (!RESOURCE_NOT_EXISTS_ERROR_CODE.equals(e.getErrorCode())) {
                throw handleConfigurationMgtException(e, ERROR_CODE_ERROR_GETTING_NOTIFICATION_SENDER, resourceName);
            }
        }
        return Optional.empty();
    }

    private Optional<Resource> getPublisherResource(String resourceName) throws NotificationSenderManagementException {

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

    private ClusteringAgent getClusteringAgent() {

        return NotificationSenderTenantConfigDataHolder.getInstance().getClusteringAgent();
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
            throw  new NotificationSenderManagementServerException(ERROR_CODE_SERVER_ERRORS_GETTING_EVENT_PUBLISHER,
                    eventPublisherName, e);
        }
        return publisherInSuperTenant;
    }

    /**
     * Get default properties of super tenant Publisher.
     *
     * @param publisherName    Name of the publisher.
     *
     * @return  Map of Properties.
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
     * Build Resource by email sender body request.
     *
     * @param emailSender EmailSender post body.
     * @return Resource object.
     */
    private Resource buildResourceFromEmailSender(EmailSenderDTO emailSender)
            throws NotificationSenderManagementServerException {

        InputStream inputStream;
        try {
            inputStream = generateEmailPublisher(emailSender);
        } catch (ParserConfigurationException e) {
            throw new NotificationSenderManagementServerException(ERROR_CODE_PARSER_CONFIG_EXCEPTION,
                    e.getMessage(), e);
        } catch (TransformerException e) {
            throw new NotificationSenderManagementServerException(ERROR_CODE_TRANSFORMER_EXCEPTION, e.getMessage(), e);
        }

        Resource resource = new Resource();
        resource.setResourceName(emailSender.getName());
        Map<String, String> emailSenderAttributes = emailSender.getProperties();
        emailSenderAttributes.put(FROM_ADDRESS, emailSender.getFromAddress());
        if (StringUtils.isNotEmpty(emailSender.getUsername())) {
            emailSenderAttributes.put(USERNAME, emailSender.getUsername());
        }
        if (StringUtils.isNotEmpty(emailSender.getPassword())) {
            emailSenderAttributes.put(PASSWORD, emailSender.getPassword());
        }
        if (StringUtils.isNotEmpty(emailSender.getAuthType())) {
            emailSenderAttributes.put(AUTH_TYPE, emailSender.getAuthType());
        } else {
            emailSenderAttributes.put(AUTH_TYPE, BASIC);
        }
        emailSenderAttributes.put(SMTP_SERVER_HOST, emailSender.getSmtpServerHost());
        emailSenderAttributes.put(SMTP_PORT, String.valueOf(emailSender.getSmtpPort()));

        List<Attribute> resourceAttributes =
                emailSenderAttributes.entrySet().stream()
                        .filter(attribute -> attribute.getValue() != null && !"null".equals(attribute.getValue()))
                        .map(attribute -> new Attribute(attribute.getKey(), attribute.getValue()))
                        .collect(Collectors.toList());
        resource.setAttributes(resourceAttributes);
        // Set file.
        ResourceFile file = new ResourceFile();
        file.setName(emailSender.getName());
        file.setInputStream(inputStream);
        List<ResourceFile> resourceFiles = new ArrayList<>();
        resourceFiles.add(file);
        resource.setFiles(resourceFiles);
        return resource;
    }

    /**
     * Build an email sender response from email sender's resource object.
     *
     * @param resource Email sender resource object.
     * @return Email Sender response.
     */
    private EmailSenderDTO buildEmailSenderFromResource(Resource resource) {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setName(resource.getResourceName());
        // Skip STREAM_NAME, STREAM_VERSION and PUBLISHER_TYPE_PROPERTY properties which are stored for internal use.
        Map<String, String> attributesMap =
                resource.getAttributes().stream()
                        .filter(attribute -> !(INTERNAL_PROPERTIES.contains(attribute.getKey())))
                        .collect(Collectors.toMap(Attribute::getKey, Attribute::getValue));
        attributesMap.forEach((key, value) -> {
            switch (key) {
                case SMTP_SERVER_HOST:
                    emailSender.setSmtpServerHost(value);
                    break;
                case SMTP_PORT:
                    emailSender.setSmtpPort(Integer.valueOf(value));
                    break;
                case FROM_ADDRESS:
                    emailSender.setFromAddress(value);
                    break;
                case USERNAME:
                    emailSender.setUsername(value);
                    break;
                case PASSWORD:
                    emailSender.setPassword(value);
                    break;
                case AUTH_TYPE:
                    emailSender.setAuthType(value);
                    break;
                default:
                    emailSender.getProperties().put(key, value);
            }
        });
        return emailSender;
    }

    private NotificationSenderManagementException handleApplicationMgtException(
            IdentityApplicationManagementException e, ErrorMessage error, String data) {

        if (e instanceof IdentityApplicationManagementClientException) {
            return new NotificationSenderManagementClientException(error, data, e);
        } else if (e instanceof IdentityApplicationManagementServerException) {
            return new NotificationSenderManagementServerException(error, data, e);
        } else {
            return new NotificationSenderManagementException(error, data, e);
        }
    }

    private NotificationSenderManagementException handleConfigurationMgtException(ConfigurationManagementException e,
                                                                                  ErrorMessage error,
                                                                                  String data) {

        if (e instanceof ConfigurationManagementClientException) {
            return new NotificationSenderManagementClientException(error, data, e);
        } else if (e instanceof ConfigurationManagementServerException) {
            return new NotificationSenderManagementServerException(error, data, e);
        } else {
            return new NotificationSenderManagementException(error, data, e);
        }
    }

    /**
     * Get channel type from SMSSenderDTO object. In absence of channel type
     * property, return a default value.
     *
     * @param smsSender SMSSenderDTO object.
     * @return Channel type property value of the sms sender.
     */
    private String getChannelTypeFromSMSSenderDTO(SMSSenderDTO smsSender) {

        Map<String, String> properties = smsSender.getProperties();
        if (StringUtils.isNotEmpty(properties.get(CHANNEL_TYPE_PROPERTY))) {
            return properties.get(CHANNEL_TYPE_PROPERTY);
        } else {
            return DEFAULT_HANDLER_NAME;
        }
    }

    /**
     * Get channel type from Resource object. In absence of channel type
     * property, return a default value.
     *
     * @param resource Resource object.
     * @return Channel type property value of the resource.
     */
    private String getChannelTypeFromResource(Resource resource) {

        if (resource.getAttributes() == null || resource.getAttributes().isEmpty()) {
            return DEFAULT_HANDLER_NAME;
        } else {
            return resource.getAttributes().stream()
                    .filter(attribute -> attribute.getKey().equals(CHANNEL_TYPE_PROPERTY)).findAny()
                    .map(Attribute::getValue).orElse(DEFAULT_HANDLER_NAME);
        }
    }

    /**
     * Method to check whether the sender is allowed to delete.
     *
     * @param senderName    Name of the sender.
     * @return  whether the sender is allowed to delete.
     * @throws NotificationSenderManagementException If an error occurred while checking the sender.
     */
    private boolean canSenderDelete(String senderName) throws NotificationSenderManagementException {

        if (SENDERS.get(senderName) != null) {
            String authenticatorId = new String(Base64.getUrlEncoder()
                    .encode(SENDERS.get(senderName).getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8);
            String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            try {
                // Since we are checking whether the sender is connected to any application, we can use the limit as 1
                // and offset as 0.
                ConnectedAppsResult appsResult =
                        NotificationSenderTenantConfigDataHolder.getInstance()
                        .getApplicationManagementService()
                        .getConnectedAppsForLocalAuthenticator(authenticatorId, tenantDomain, 1 , 0);
                // If there are any connected apps, the sender cannot be deleted.
                if (appsResult.getApps() != null && appsResult.getApps().size() > 0) {
                    return false;
                }
                Resource resource = NotificationSenderTenantConfigDataHolder.getInstance().getConfigurationManager()
                        .getResource(MY_ACCOUNT_SMS_RESOURCE_TYPE, MY_ACCOUNT_SMS_RESOURCE_NAME);

                if (resource == null || resource.getAttributes() == null || resource.getAttributes().isEmpty()) {
                    return true;
                }
                List<Attribute> smsAttributes = resource.getAttributes().stream().filter(attribute -> attribute
                        .getKey().equals("sms_otp_enabled")).collect(
                        Collectors.toList());
                String smsOtpEnabled = null;        
                if (!smsAttributes.isEmpty()) {
                    smsOtpEnabled = smsAttributes.get(0).getValue();
                }
                // If SMS OTP is enabled for my account, the sender cannot be deleted.
                if (Boolean.parseBoolean(smsOtpEnabled)) {
                    return false;
                }
            } catch (IdentityApplicationManagementException e) {
                if (e instanceof IdentityApplicationManagementClientException &&
                        Error.AUTHENTICATOR_NOT_FOUND.getCode().equals(e.getErrorCode())) {
                    return true;
                }
                throw handleApplicationMgtException(e, ERROR_CODE_VALIDATING_CONNECTED_APPS,
                        senderName);
            } catch (ConfigurationManagementException e) {
                if (e instanceof ConfigurationManagementClientException && (
                        ERROR_CODE_RESOURCE_DOES_NOT_EXISTS.getCode().equals(e.getErrorCode())
                                || ERROR_CODE_RESOURCE_TYPE_DOES_NOT_EXISTS.getCode().equals(e.getErrorCode()))) {
                    return true;
                }
                throw handleConfigurationMgtException(e, ERROR_CODE_VALIDATING_CONNECTED_APPS,
                        senderName);
            }
        }
        return true;
    }
}
