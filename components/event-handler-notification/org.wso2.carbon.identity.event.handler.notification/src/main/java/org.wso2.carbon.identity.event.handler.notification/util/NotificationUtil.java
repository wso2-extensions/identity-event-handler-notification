/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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

package org.wso2.carbon.identity.event.handler.notification.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;
import org.wso2.carbon.event.stream.core.EventStreamService;
import org.wso2.carbon.event.stream.core.exception.EventStreamConfigurationException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.handler.notification.NotificationConstants;
import org.wso2.carbon.identity.event.handler.notification.email.model.EmailTemplate;
import org.wso2.carbon.identity.event.handler.notification.exception.NotificationEventRuntimeException;
import org.wso2.carbon.identity.event.handler.notification.internal.NotificationHandlerDataHolder;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.Claim;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.event.publisher.core.exception.EventPublisherConfigurationException;
import org.wso2.carbon.event.publisher.core.config.EventPublisherConfiguration;
import org.wso2.carbon.event.stream.core.EventStreamService;
import org.wso2.carbon.event.stream.core.exception.EventStreamConfigurationException;
import org.wso2.carbon.identity.event.handler.notification.exception.NotificationEventRuntimeException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.event.publisher.core.EventPublisherService;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;
import org.wso2.carbon.event.output.adapter.core.OutputEventAdapterConfiguration;
import org.wso2.carbon.event.publisher.core.config.mapping.TextOutputMapping;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationUtil {

    private static Log log = LogFactory.getLog(NotificationUtil.class);

    public static EmailTemplate loadEmailTemplate(String notificationType, String locale, String tenantDomain) {
        StringBuilder resourcePath = new StringBuilder(NotificationConstants.EmailNotification.EMAIL_TEMPLATE_PATH)
                .append(notificationType).append("/")
                .append(notificationType).append(".").append(locale);
        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        RegistryService registry = NotificationHandlerDataHolder.getInstance().getRegistryService();
        Resource resourceValue = null;
        try {
            UserRegistry userReg = registry.getConfigSystemRegistry(tenantId);
            resourceValue = userReg.get(resourcePath.toString());
            if (resourceValue != null) {
                byte[] emailTemplateContentArray = (byte[]) resourceValue.getContent();
                String emailContentType = resourceValue.getMediaType();
                if (StringUtils.isBlank(emailContentType)) {
                    emailContentType = NotificationConstants.EmailNotification.TEMPLATE_CONTENT_TYPE_DEFAULT;
                }
                String emailTemplateContentString = new String(emailTemplateContentArray, Charset.forName("UTF-8"));
                if (log.isDebugEnabled()) {
                    String message = "Successfully read the org.wso2.carbon.identity.event.handler.notification template:\n" + emailTemplateContentString +
                            "\nin resource path : " + resourcePath + " for tenant " + tenantDomain;
                    log.debug(message);
                }
                String[] emailTemplateContent = emailTemplateContentString.split("\\|");
                if (emailTemplateContent.length <= 3) {
                    EmailTemplate template = new EmailTemplate(notificationType, emailTemplateContent[0],
                            emailTemplateContent[1],
                            emailTemplateContent[2], locale, emailContentType);
                    return template;
                } else {
                    log.error("Cannot have \"|\" character in the template");
                }
            }
        } catch (ResourceNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("Email template not found at path " + resourcePath + " for tenant " + tenantDomain, e);
            }
        } catch (RegistryException e) {
            log.error("Error occurred while reading org.wso2.carbon.identity.event.handler.notification templates from path: " + resourcePath +
                    " for tenant " + tenantDomain, e);
        }
        return null;
    }

    public static Map<String, String> getUserClaimValues(String userName, UserStoreManager userStoreManager) {

        Claim[] userClaims = new Claim[0];
        Map<String, String> claimsMap = new HashMap<String, String>();
        try {
            userClaims = userStoreManager.getUserClaimValues(userName, UserCoreConstants.DEFAULT_PROFILE);
            for (int i = 0; i < userClaims.length; i++) {
                claimsMap.put(userClaims[i].getClaimUri(), userClaims[i].getValue());

            }
        } catch (UserStoreException e) {
            String domainNameProperty = getUserStoreDomainName(userStoreManager);
            String message = null;
            if (StringUtils.isNotBlank(domainNameProperty)) {
                message = "Error occurred while retrieving user claim values for user " + userName + " in user store "
                        + domainNameProperty + " in tenant " + getTenantDomain(userStoreManager);
            } else {
                message = "Error occurred while retrieving user claim values for user " + userName + " in tenant "
                        + getTenantDomain(userStoreManager);
            }
            log.error(message, e);
        }

        return claimsMap;
    }

    public static Map<String, String> getUserClaimValues(String userName, String domainName, String tenantDomain) {

        RealmService realmService = NotificationHandlerDataHolder.getInstance().getRealmService();
        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        UserStoreManager userStoreManager = null;
        try {
            userStoreManager = realmService.getTenantUserRealm(tenantId).getUserStoreManager();
            if (userStoreManager instanceof AbstractUserStoreManager) {
                userStoreManager = ((AbstractUserStoreManager) userStoreManager).getSecondaryUserStoreManager(domainName);
            }
        } catch (UserStoreException e) {
            String message = "Error occurred while retrieving user claim values for user " + userName + " in user " +
                    "store " + domainName + " in tenant " + tenantDomain;
            log.error(message, e);
        }
        return getUserClaimValues(userName, userStoreManager);
    }

    public static Map<String, String> getPlaceholderValues(EmailTemplate emailTemplate, Map<String, String> placeHolderData, Map<String, String> userClaims) {

        List<String> placeHolders = new ArrayList<>();
        placeHolders.addAll(extractPlaceHolders(emailTemplate.getBody()));
        placeHolders.addAll(extractPlaceHolders(emailTemplate.getSubject()));
        placeHolders.addAll(extractPlaceHolders(emailTemplate.getFooter()));

        if (userClaims != null && !userClaims.isEmpty()) {
            for (String placeHolder : placeHolders) {
                if (placeHolder.contains(NotificationConstants.EmailNotification.USER_CLAIM_PREFIX + "." + NotificationConstants.EmailNotification.IDENTITY_CLAIM_PREFIX)) {
                    String identityClaim = userClaims.get(NotificationConstants.EmailNotification.WSO2_CLAIM_URI
                            + NotificationConstants.EmailNotification.IDENTITY_CLAIM_PREFIX + "/"
                            + placeHolder.substring(placeHolder.lastIndexOf(".") + 1));
                    if (StringUtils.isNotEmpty(identityClaim)) {
                        placeHolderData.put(placeHolder, identityClaim);
                    }
                } else if (placeHolder.contains(NotificationConstants.EmailNotification.USER_CLAIM_PREFIX)) {
                    String userClaim = userClaims.get(NotificationConstants.EmailNotification.WSO2_CLAIM_URI
                            + placeHolder.substring(placeHolder.lastIndexOf(".") + 1));
                    if (StringUtils.isNotEmpty(userClaim)) {
                        placeHolderData.put(placeHolder, userClaim);
                    }
                }
            }
        }
        return placeHolderData;
    }

    public static List<String> extractPlaceHolders(String value) {

        String exp = "\\{(.*?)\\}";
        Pattern pattern = Pattern.compile(exp);
        Matcher matcher = pattern.matcher(value);
        List<String> placeHolders = new ArrayList<>();
        while (matcher.find()) {
            String group = matcher.group().replace("{", "").replace("}", "");
            placeHolders.add(group);
        }
        return placeHolders;
    }

    public static String getUserStoreDomainName(UserStoreManager userStoreManager) {
        String domainNameProperty = null;
        if (userStoreManager instanceof org.wso2.carbon.user.core.UserStoreManager) {
            domainNameProperty = ((org.wso2.carbon.user.core.UserStoreManager)
                    userStoreManager).getRealmConfiguration()
                    .getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME);
            if (StringUtils.isBlank(domainNameProperty)) {
                domainNameProperty = IdentityUtil.getPrimaryDomainName();
            }
        }
        return domainNameProperty;
    }

    public static String getTenantDomain(UserStoreManager userStoreManager) {
        try {
            return IdentityTenantUtil.getTenantDomain(userStoreManager.getTenantId());
        } catch (UserStoreException e) {
            throw NotificationEventRuntimeException.error(e.getMessage(), e);
        }
    }

    public static void deployDefaultStreamsAndPublishers() throws NotificationEventRuntimeException {
        try {
            EventStreamService service = NotificationHandlerDataHolder.getInstance().getEventStreamService();
            EventPublisherService eventPublisherService = NotificationHandlerDataHolder.getInstance().getEventPublisherService();

            StreamDefinition streamDefinition = new StreamDefinition("id_gov_notify_stream", "1.0.0", "id_gov_notify_stream:1.0.0");
            service.addEventStreamDefinition(streamDefinition);

            EventPublisherConfiguration eventPublisherConfiguration = new EventPublisherConfiguration();
            eventPublisherConfiguration.setEventPublisherName("org.wso2.carbon.identity.event.handler.notification Publisher");
            eventPublisherConfiguration.setFromStreamName("id_gov_notify_stream");
            eventPublisherConfiguration.setFromStreamVersion("1.0.0");

            OutputEventAdapterConfiguration outputEventAdapterConfiguration = new OutputEventAdapterConfiguration();
            outputEventAdapterConfiguration.setName("org.wso2.carbon.identity.event.handler.notification Publisher");
            outputEventAdapterConfiguration.setMessageFormat("text");
            outputEventAdapterConfiguration.setType("org.wso2.carbon.identity.event.handler.notification");

            eventPublisherConfiguration.setToAdapterConfiguration(outputEventAdapterConfiguration);
            TextOutputMapping textOutputMapping = new TextOutputMapping();
            textOutputMapping.setCustomMappingEnabled(true);
            textOutputMapping.setRegistryResource(false);
            textOutputMapping.setMappingText("{{body}}{{footer}}");
            textOutputMapping.setCacheTimeoutDuration(0);

            eventPublisherConfiguration.setOutputMapping(textOutputMapping);
            //setTraceEnabled(boolean enableTracing)
            //setStatisticsEnabled(boolean enableStatistics)
            Map<String, String> adapterDynamicProperties = new HashMap<>();
            adapterDynamicProperties.put("org.wso2.carbon.identity.event.handler.notification.address", "{{send-to}}");
            adapterDynamicProperties.put("org.wso2.carbon.identity.event.handler.notification.type", "text/plain");
            adapterDynamicProperties.put("org.wso2.carbon.identity.event.handler.notification.subject", "{{subject}}");
            eventPublisherConfiguration.setToAdapterDynamicProperties(adapterDynamicProperties);
            //setEditable(boolean editable)


            try {
                eventPublisherService.deployEventPublisherConfiguration(eventPublisherConfiguration);
            } catch (EventPublisherConfigurationException e) {
                throw NotificationEventRuntimeException.error("Error in deploying a publisher.");
            }

        } catch (MalformedStreamDefinitionException e) {
            throw NotificationEventRuntimeException.error("Error in deploying a stream.");
        } catch (EventStreamConfigurationException e) {
            throw NotificationEventRuntimeException.error("Error in deploying a stream.");
        }
    }
}

