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

package org.wso2.carbon.identity.event.handler.notification;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;
import org.wso2.carbon.event.output.adapter.core.OutputEventAdapterConfiguration;
import org.wso2.carbon.event.stream.core.EventStreamService;
/*import org.wso2.carbon.event.publisher.core.EventPublisherService;*/
import org.wso2.carbon.event.publisher.core.config.EventPublisherConfiguration;
import org.wso2.carbon.event.stream.core.exception.EventStreamConfigurationException;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.core.handler.InitConfig;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.event.handler.notification.email.bean.EmailNotification;
import org.wso2.carbon.identity.event.handler.notification.email.model.EmailTemplate;
import org.wso2.carbon.identity.event.handler.notification.internal.NotificationHandlerDataHolder;
import org.wso2.carbon.identity.event.handler.notification.util.NotificationUtil;
import org.wso2.carbon.user.core.UserStoreManager;

import java.util.HashMap;
import java.util.Map;

public class NotificationHandler extends AbstractEventHandler {

    private static final String EMAIL_NOTIFICATION_TYPE = "EMAIL";

    private static final Log log = LogFactory.getLog(NotificationHandler.class);

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        Map<String, String> placeHolderData = new HashMap<>();
        String sendTo = null;
        Map<String, String> userClaims = new HashMap<>();

        for (Map.Entry<String, Object> entry : event.getEventProperties().entrySet()) {
            if (entry.getValue() instanceof String) {
                placeHolderData.put(entry.getKey(), (String) entry.getValue());
            }
        }

        String notificationEvent = (String) event.getEventProperties().get(NotificationConstants.EmailNotification.EMAIL_TEMPLATE_TYPE);
        String username = (String) event.getEventProperties().get(IdentityEventConstants.EventProperty.USER_NAME);
        UserStoreManager userStoreManager = (UserStoreManager) event.getEventProperties().get(
                IdentityEventConstants.EventProperty.USER_STORE_MANAGER);
        String userStoreDomainName = (String) event.getEventProperties().get(IdentityEventConstants.EventProperty.USER_STORE_DOMAIN);
        String tenantDomain = (String) event.getEventProperties().get(IdentityEventConstants.EventProperty.TENANT_DOMAIN);
        String sendFrom = (String) event.getEventProperties().get("send-from");

        if (StringUtils.isNotBlank(username) && userStoreManager != null) {
            userClaims = NotificationUtil.getUserClaimValues(username, userStoreManager);
        } else if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(userStoreDomainName) &&
                StringUtils.isNotBlank(tenantDomain)) {
            userClaims = NotificationUtil.getUserClaimValues(username, userStoreDomainName, tenantDomain);
        }

        String locale = NotificationConstants.EmailNotification.LOCALE_DEFAULT;
        if (userClaims.containsKey(NotificationConstants.EmailNotification.CLAIM_URI_LOCALE)) {
            locale = userClaims.get(NotificationConstants.EmailNotification.CLAIM_URI_LOCALE);
        }

        if (userClaims.containsKey(NotificationConstants.EmailNotification.CLAIM_URI_EMAIL)) {
            sendTo = userClaims.get(NotificationConstants.EmailNotification.CLAIM_URI_EMAIL);
        }
        if (StringUtils.isEmpty(sendTo)) {
            throw new IdentityEventException(
                    "Email notification sending failed. Sending org.wso2.carbon.identity.event.handler.notification address is not configured for the user.");
        }

        EmailTemplate emailTemplate = NotificationUtil.loadEmailTemplate(notificationEvent, locale, tenantDomain);

        NotificationUtil.getPlaceholderValues(emailTemplate, placeHolderData, userClaims);

        EmailNotification.EmailNotificationBuilder builder =
                new EmailNotification.EmailNotificationBuilder(sendTo);
        builder.setSendFrom(sendFrom);
        builder.setTemplate(emailTemplate);
        builder.setPlaceHolderData(placeHolderData);
        EmailNotification emailNotification = builder.build();

        publishToStream(emailNotification, placeHolderData);
    }

    protected void publishToStream(EmailNotification emailNotification, Map<String, String> placeHolderDataMap) {

        EventStreamService service = NotificationHandlerDataHolder.getInstance().getEventStreamService();
        //EventPublisherService eventPublisherService = NotificationHandlerDataHolder.getInstance().getEventPublisherService();
        org.wso2.carbon.databridge.commons.Event databridgeEvent = new org.wso2.carbon.databridge.commons.Event();

        databridgeEvent.setTimeStamp(System.currentTimeMillis());
        databridgeEvent.setStreamId("id_gov_notify_stream:1.0.0");
        Map<String, String> arbitraryDataMap = new HashMap<>();
        Object[] metadataArray = new Object[2];
        arbitraryDataMap.put("org.wso2.carbon.identity.event.handler.notification-event", emailNotification.getTemplate().getNotificationEvent());
        arbitraryDataMap.put(IdentityEventConstants.EventProperty.USER_NAME,
                placeHolderDataMap.get(IdentityEventConstants.EventProperty.USER_NAME));

        arbitraryDataMap.put(IdentityEventConstants.EventProperty.USER_STORE_DOMAIN,
                placeHolderDataMap.get(IdentityEventConstants.EventProperty.USER_STORE_DOMAIN));
        arbitraryDataMap.put(IdentityEventConstants.EventProperty.TENANT_DOMAIN,
                placeHolderDataMap.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN));
        arbitraryDataMap.put("send-from", emailNotification.getSendFrom());
        for (Map.Entry<String, String> placeHolderDataEntry : placeHolderDataMap.entrySet()) {
            arbitraryDataMap.put(placeHolderDataEntry.getKey(), placeHolderDataEntry.getValue());
        }
        arbitraryDataMap.put("subject-template", emailNotification.getTemplate().getSubject());
        arbitraryDataMap.put("body-template", emailNotification.getTemplate().getBody());
        arbitraryDataMap.put("footer-template", emailNotification.getTemplate().getFooter());
        arbitraryDataMap.put("locale", emailNotification.getTemplate().getLocale());
        arbitraryDataMap.put("content-type", emailNotification.getTemplate().getContentType());
        arbitraryDataMap.put("send-to", emailNotification.getSendTo());
        arbitraryDataMap.put("subject", emailNotification.getSubject());
        arbitraryDataMap.put("body", emailNotification.getBody());
        arbitraryDataMap.put("footer", emailNotification.getFooter());

        databridgeEvent.setArbitraryDataMap(arbitraryDataMap);
        service.publish(databridgeEvent);
    }

    @Override
    public void init(InitConfig configuration) throws IdentityRuntimeException {
    }

    @Override
    public String getName() {
        return "emailSend";
    }
}
