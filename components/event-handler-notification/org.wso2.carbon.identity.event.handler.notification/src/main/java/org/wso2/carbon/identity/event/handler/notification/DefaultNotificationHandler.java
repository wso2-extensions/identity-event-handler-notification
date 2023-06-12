/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.event.handler.notification;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.event.stream.core.EventStreamService;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.core.handler.InitConfig;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.event.handler.notification.email.bean.Notification;
import org.wso2.carbon.identity.event.handler.notification.exception.NotificationRuntimeException;
import org.wso2.carbon.identity.event.handler.notification.internal.NotificationHandlerDataHolder;
import org.wso2.carbon.identity.event.handler.notification.util.NotificationUtil;
import org.wso2.carbon.identity.governance.IdentityGovernanceUtil;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * DefaultNotificationHandler is based on simple key/value parameters which was send by an event to any given stream.
 *
 * We can define the streamid and the notification template from the config file (identity-event.properties)
 * But notification template is not implemented as the default implementation.
 *
 */
public class DefaultNotificationHandler extends AbstractEventHandler {

    private static final Log log = LogFactory.getLog(DefaultNotificationHandler.class);

    private static final String STREAM_DEFINITION_ID = "stream" ;
    private static final String NOTIFICATION_TEMPLATE_TYPE = "notification_template" ;
    private static final String DEFAULT_STREAM_ID = "id_gov_notify_stream:1.0.0";

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        Map<String, String> arbitraryDataMap = buildNotificationData(event);
        publishToStream(arbitraryDataMap, event);
    }

    /**
     * Resolve notification channel to server supported notification channel (SMS or EMAIL).
     *
     * @param notificationChannel Notification channel
     * @return Resolved notification channel
     */
    private String resolveNotificationChannel(String notificationChannel) {

        if (NotificationChannels.EMAIL_CHANNEL.getChannelType().equals(notificationChannel)
                || NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            return notificationChannel;
        } else {
            if (log.isDebugEnabled()) {
                String message = String.format("Notification channel : %s is not supported by the server. "
                                + "Notification channel changed to : %s", notificationChannel,
                        IdentityGovernanceUtil.getDefaultNotificationChannel());
                log.debug(message);
            }
            return IdentityGovernanceUtil.getDefaultNotificationChannel();
        }
    }

    /**
     * This method will build the specific notification data which under this module.
     *
     * @param event Event attributes
     * @return Data map with event properties
     * @throws IdentityEventException Error building the arbitrary data map
     */
    protected Map<String, String> buildNotificationData(Event event) throws IdentityEventException {

        Map<String, String> arbitraryDataMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : event.getEventProperties().entrySet()) {
            if (entry.getValue() instanceof String) {
                arbitraryDataMap.put(entry.getKey(), (String) entry.getValue());
            }
        }

        // Read the send-to parameter which was set by the notification senders.
        String sendTo = arbitraryDataMap.get(NotificationConstants.EmailNotification.ARBITRARY_SEND_TO);
        Map<String, String> userClaims = new HashMap<>();

        String notificationTemplateName = getNotificationTemplate(event);
        if (StringUtils.isEmpty(notificationTemplateName)) {
            notificationTemplateName = (String) event.getEventProperties()
                    .get(NotificationConstants.EmailNotification.EMAIL_TEMPLATE_TYPE);
        }

        if(StringUtils.isNotEmpty(notificationTemplateName)) {

            String username = (String) event.getEventProperties().get(IdentityEventConstants.EventProperty.USER_NAME);
            org.wso2.carbon.user.core.UserStoreManager userStoreManager = (org.wso2.carbon.user.core.UserStoreManager)
                    event.getEventProperties().get(
                    IdentityEventConstants.EventProperty.USER_STORE_MANAGER);
            String userStoreDomainName = (String) event.getEventProperties().get(
                    IdentityEventConstants.EventProperty.USER_STORE_DOMAIN);
            String tenantDomain = (String) event.getEventProperties().get(
                    IdentityEventConstants.EventProperty.TENANT_DOMAIN);
            String sendFrom = (String) event.getEventProperties().get(
                    NotificationConstants.EmailNotification.ARBITRARY_SEND_FROM);

            // Resolve Notification channel.
            String notificationChannel = resolveNotificationChannel(
                    (String) event.getEventProperties().get(IdentityEventConstants.EventProperty.NOTIFICATION_CHANNEL));

            if (StringUtils.isNotBlank(username) && userStoreManager != null) {
                userClaims = NotificationUtil.getUserClaimValues(username, userStoreManager);
            } else if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(userStoreDomainName) &&
                    StringUtils.isNotBlank(tenantDomain)) {
                userClaims = NotificationUtil.getUserClaimValues(username, userStoreDomainName, tenantDomain);
            }

            // Resolve notification template locale according to the notification channel.
            String locale = NotificationUtil.getNotificationLocale();
            if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel) && userClaims
                    .containsKey(NotificationConstants.SMSNotification.DEFAULT_SMS_NOTIFICATION_LOCALE)) {
                locale = userClaims.get(NotificationConstants.SMSNotification.DEFAULT_SMS_NOTIFICATION_LOCALE);
            } else {

                // By default EMAIL notification template locale is selected.
                if (userClaims.containsKey(NotificationConstants.EmailNotification.CLAIM_URI_LOCALE)) {
                    locale = userClaims.get(NotificationConstants.EmailNotification.CLAIM_URI_LOCALE);
                }
            }
            if(StringUtils.isEmpty(sendTo)) {
                if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
                    if (userClaims.containsKey(NotificationConstants.SMSNotification.CLAIM_URI_MOBILE)) {
                        sendTo = userClaims.get(NotificationConstants.SMSNotification.CLAIM_URI_MOBILE);
                    }
                }
                else {
                    if (userClaims.containsKey(NotificationConstants.EmailNotification.CLAIM_URI_EMAIL)) {
                        sendTo = userClaims.get(NotificationConstants.EmailNotification.CLAIM_URI_EMAIL);
                    }
                }
            }

            NotificationTemplate notificationTemplate;
            try {
                notificationTemplate = NotificationHandlerDataHolder.getInstance().getNotificationTemplateManager()
                        .getNotificationTemplate(notificationChannel, notificationTemplateName, locale, tenantDomain);
            } catch (NotificationTemplateManagerException exception) {
                String message = "Error when retrieving template from tenant registry.";
                throw NotificationRuntimeException.error(message, exception);
            }

            // Add template properties for arbitraryDataMap.
            addNotificationTemplateDataToArbitraryDataMap(notificationTemplate, notificationTemplateName, sendTo,
                    sendFrom, arbitraryDataMap, userClaims);
        }
        Map<String, String> arbitraryDataClaims = getArbitraryDataClaimsFromProperties(event);
        Set<String> keys = arbitraryDataClaims.keySet();
        for (String key : keys) {
            String claim = arbitraryDataClaims.get(key);
            String value = userClaims.get(claim);
            arbitraryDataMap.put(key, value);
        }
        Map<String, String> arbitraryDataFromProperties = getArbitraryDataFromProperties(event);
        arbitraryDataMap.putAll(arbitraryDataFromProperties);
        return arbitraryDataMap ;
    }

    /**
     * Add the notification template data to the arbitrary data map.
     *
     * @param notificationTemplate     {@link
     *                                 org.wso2.carbon.identity.governance.service.notification.NotificationTemplateManager}
     *                                 object
     * @param notificationTemplateName Notification template
     * @param sendTo                   Notification send to address
     * @param sendFrom                 Notification send from address
     * @param arbitraryDataMap         Arbitrary data map
     * @param userClaims               User claims
     */
    private void addNotificationTemplateDataToArbitraryDataMap(NotificationTemplate notificationTemplate,
            String notificationTemplateName, String sendTo, String sendFrom, Map<String, String> arbitraryDataMap,
            Map<String, String> userClaims) {

        // Build Notification object using notification template data.
        // todo: Refer to https://github.com/wso2/product-is/issues/7006
        EmailTemplate emailTemplate = buildEmailTemplate(notificationTemplate);
        Notification notification = buildEmailNotification(emailTemplate, arbitraryDataMap, userClaims, sendTo,
                sendFrom);

        // Add values to the arbitrary data map.
        arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_EVENT_TYPE,
                I18nEmailUtil.getNormalizedName(notificationTemplateName));
        arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_SEND_FROM, notification.getSendFrom());
        arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_BODY_TEMPLATE, notification.
                getTemplate().getBody());
        arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_LOCALE, notification.getTemplate().
                getLocale());
        arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_SEND_TO, notification.getSendTo());
        arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_BODY, notification.getBody());

        // Additional properties if the notification channel is not SMS.
        if (!NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationTemplate.getNotificationChannel())) {
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_SUBJECT_TEMPLATE, notification.
                    getTemplate().getSubject());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_FOOTER_TEMPLATE, notification.
                    getTemplate().getFooter());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_CONTENT_TYPE, notification.
                    getTemplate().getEmailContentType());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_SUBJECT, notification.getSubject());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_FOOTER, notification.getFooter());
        }
    }

    /**
     * Build an Email Template object using SMS template data.
     *
     * @param notificationTemplate {@link
     *                             org.wso2.carbon.identity.governance.service.notification.NotificationTemplateManager}
     *                             object
     * @return {@link org.wso2.carbon.email.mgt.model.EmailTemplate} object
     */
    private EmailTemplate buildEmailTemplate(NotificationTemplate notificationTemplate) {

        // Build an email template using SMS template data.
        EmailTemplate emailTemplate = new EmailTemplate();
        emailTemplate.setTemplateDisplayName(notificationTemplate.getDisplayName());
        emailTemplate.setTemplateType(notificationTemplate.getType());
        emailTemplate.setLocale(notificationTemplate.getLocale());
        emailTemplate.setBody(notificationTemplate.getBody());

        if (NotificationChannels.EMAIL_CHANNEL.getChannelType().equals(notificationTemplate.getNotificationChannel())) {
            emailTemplate.setSubject(notificationTemplate.getSubject());
            emailTemplate.setFooter(notificationTemplate.getFooter());
            emailTemplate.setEmailContentType(notificationTemplate.getContentType());
        }
        return emailTemplate;
    }

    /**
     * Build Email Notification from the emailTemplate and the arbitrary data.
     *
     * @param emailTemplate    {@link org.wso2.carbon.email.mgt.model.EmailTemplate} object
     * @param arbitraryDataMap Arbitrary data map
     * @param userClaims       User claims
     * @param sendTo           Notification send to address
     * @param sendFrom         Notification send from address
     * @return {@link org.wso2.carbon.identity.event.handler.notification.email.bean.Notification} object
     */
    private Notification buildEmailNotification(EmailTemplate emailTemplate, Map<String, String> arbitraryDataMap,
            Map<String, String> userClaims, String sendTo, String sendFrom) {

        NotificationUtil.getPlaceholderValues(emailTemplate, arbitraryDataMap, userClaims);
        Notification.EmailNotificationBuilder builder = new Notification.EmailNotificationBuilder(sendTo);
        builder.setSendFrom(sendFrom);
        builder.setTemplate(emailTemplate);
        builder.setPlaceHolderData(arbitraryDataMap);
        return builder.build();
    }

    /**
     *
     * This method is used to publish the event to the event service.
     *
     * @param dataMap
     * @param event
     * @throws IdentityEventException
     */
    protected void publishToStream(Map<String, String> dataMap, Event event) throws IdentityEventException{

        EventStreamService service = NotificationHandlerDataHolder.getInstance().getEventStreamService();
        org.wso2.carbon.databridge.commons.Event databridgeEvent = new org.wso2.carbon.databridge.commons.Event();
        databridgeEvent.setTimeStamp(System.currentTimeMillis());

        try {
            databridgeEvent.setStreamId(getStreamDefinitionID(event));
        } catch (IdentityEventException e) {
            throw new IdentityEventException("Stream definition was not specified in the " +
                    "identity-event.properties file for " + event.getEventName());
        }

        databridgeEvent.setArbitraryDataMap(dataMap);
        service.publish(databridgeEvent);
    }

    private Map<String, String> getArbitraryDataClaimsFromProperties(Event event) throws IdentityEventException {
        Map<String, String> claimMap = new HashMap<>();
        String tmpKeyStartWith = this.getName() + ".subscription." + event.getEventName() + "." + "claim" ;
        Properties subscriptionProperties = getSubscriptionProperties(event.getEventName());
        Set<Object> subscriptionPropertyKeys = subscriptionProperties.keySet();
        for (Object subscriptionPropertyKey : subscriptionPropertyKeys) {
            String key = (String)subscriptionPropertyKey;
            if(key.startsWith(tmpKeyStartWith)){
                String attribute = key.substring(tmpKeyStartWith.length() + 1);
                String value = (String)subscriptionProperties.get(key);
                claimMap.put(attribute, value);
            }
        }
        return claimMap;
    }

    private Map<String, String> getArbitraryDataFromProperties(Event event) throws IdentityEventException {
        Map<String, String> dataMap = new HashMap<>();
        String streamIdKey =  this.getName() + ".subscription." + event.getEventName() + "." + STREAM_DEFINITION_ID;
        String templateType =  this.getName() + ".subscription." + event.getEventName() + "." + NOTIFICATION_TEMPLATE_TYPE;
        String claimKeyStartWith = this.getName() + ".subscription." + event.getEventName() + "." + "claim" ;

        Properties subscriptionProperties = getSubscriptionProperties(event.getEventName());
        Set<Object> subscriptionPropertyKeys = subscriptionProperties.keySet();
        for (Object subscriptionPropertyKey : subscriptionPropertyKeys) {
            String key = (String)subscriptionPropertyKey;
            if(!key.startsWith(claimKeyStartWith) && !key.equalsIgnoreCase(streamIdKey) &&
                    !key.equalsIgnoreCase(templateType)){
                String keyPrefix = this.getName() + ".subscription." + event.getEventName();
                String attribute = key.substring(keyPrefix.length() + 1);
                String value = (String)subscriptionProperties.get(key);
                dataMap.put(attribute, value);
            }
        }
        return dataMap;
    }


    @Override
    public String getName() {
        return "default.notification.sender";
    }

    protected String getStreamDefinitionID(Event event) throws IdentityEventException {
        String streamDefinitionID = getSubscriptionProperty(STREAM_DEFINITION_ID, event.getEventName());
        if(StringUtils.isEmpty(streamDefinitionID)){
            streamDefinitionID = DEFAULT_STREAM_ID ;
        }
        return streamDefinitionID;
    }

    protected String getNotificationTemplate(Event event) throws IdentityEventException {
        return getSubscriptionProperty(NOTIFICATION_TEMPLATE_TYPE, event.getEventName());
    }
}
