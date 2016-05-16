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

package org.wso2.carbon.identity.event.handler.email.handler;

import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.CarbonConfigurationContextFactory;
import org.wso2.carbon.event.output.adapter.core.OutputEventAdapter;
import org.wso2.carbon.event.output.adapter.core.exception.OutputEventAdapterException;
import org.wso2.carbon.event.output.adapter.email.EmailEventAdapter;
import org.wso2.carbon.event.output.adapter.email.EmailEventAdapterFactory;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.core.handler.InitConfig;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.event.EventMgtConstants;
import org.wso2.carbon.identity.event.EventMgtException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.event.handler.email.constants.EmailEventConstants;
import org.wso2.carbon.identity.event.handler.email.exception.EmailEventServiceException;
import org.wso2.carbon.identity.event.handler.email.util.EmailEventUtil;
import org.wso2.carbon.identity.event.handler.email.util.EmailInfoDTO;
import org.wso2.carbon.identity.event.handler.email.util.Notification;
import org.wso2.carbon.identity.event.handler.email.util.NotificationBuilder;
import org.wso2.carbon.identity.event.handler.email.util.NotificationData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmailEventHandler extends AbstractEventHandler {

    private static final String EMAIL_NOTIFICATION_TYPE = "EMAIL";

    private static final Log log = LogFactory.getLog(EmailEventHandler.class);

    @Override
    public boolean handleEvent(Event event) throws EventMgtException {

        Map<String, String> userClaimMap = new HashMap<>();
        Map<String, String> placeHolderMap = new HashMap<>();
        String emailContentType = null;
        EmailInfoDTO emailInfoDTO = null;
        NotificationData emailNotificationData = null;
        String templateType = null;
        String locale = null;
        String sendTo = null;

        Map<String, Object> eventProperties = event.getEventProperties();

        for (Map.Entry<String, Object> entry : eventProperties.entrySet()) {
            if (!entry.getKey().equals(EmailEventConstants.EventProperty.OPERATION_TYPE)) {
                placeHolderMap.put(entry.getKey(), (String) entry.getValue());
            }
        }

        String username = placeHolderMap.get(EventMgtConstants.EventProperty.USER_NAME);
        String tenantDomain = placeHolderMap.get(EventMgtConstants.EventProperty.TENANT_DOMAIN);

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);

        String operationType = (String) eventProperties.get(EmailEventConstants.EventProperty.OPERATION_TYPE);

        try {
            userClaimMap = EmailEventUtil.getClaimFromUserStoreManager(username, tenantId);
        } catch (EmailEventServiceException e) {
            throw new EventMgtException("Could not load user claims", e);
        }

        if (operationType.equals(EmailEventConstants.EventProperty.ACCOUNT_LOCKED)) {
            templateType = EmailEventConstants.EventProperty.OPERATION_ACCOUNT_LOCKED;
        }

        if (userClaimMap != null && !userClaimMap.isEmpty()) {
            if (userClaimMap.containsKey(EmailEventConstants.CLAIM_URI_LOCALE)) {
                locale = userClaimMap.get(EmailEventConstants.CLAIM_URI_LOCALE);
            }
            if (userClaimMap.containsKey(EmailEventConstants.CLAIM_URI_EMAIL)) {
                sendTo = userClaimMap.get(EmailEventConstants.CLAIM_URI_EMAIL);
            }
        }

        if (locale == null) {
            locale = EmailEventConstants.LOCALE_DEFAULT;
        }

        StringBuilder resourcePath = new StringBuilder();
        resourcePath.append(EmailEventConstants.EMAIL_TEMPLATE_PATH).append(templateType).append("/").
                append(templateType).append(".").append(locale);

        try {
            emailInfoDTO = EmailEventUtil.loadEmailTemplate(tenantId, resourcePath.toString());
        } catch (EmailEventServiceException e) {
            throw new EventMgtException(
                    "Could not load the email template configuration for user ", e);
        }

        List<String> placeHolders = EmailEventUtil.extractPlaceHolders(emailInfoDTO.getBody());
        placeHolders.addAll(EmailEventUtil.extractPlaceHolders(emailInfoDTO.getSubject()));
        placeHolders.addAll(EmailEventUtil.extractPlaceHolders(emailInfoDTO.getFooter()));

        Map<String, String> tagData = EmailEventUtil.getTagData(placeHolders, userClaimMap, placeHolderMap);
        emailNotificationData = new NotificationData(tagData);
        emailNotificationData.setSendTo(sendTo);

        try {
            Notification emailNotification = null;
            try {
                emailNotification =
                        NotificationBuilder.createNotification(EMAIL_NOTIFICATION_TYPE, emailInfoDTO,
                                emailNotificationData);
            } catch (Exception e) {
                throw new EventMgtException(
                        "Could not create the email notification for template" + e);
            }
            if (emailNotification == null) {
                throw new IllegalStateException("Notification not set. " +
                        "Please set the notification before sending messages");
            }

            // read parameter from axis2.xml
            AxisConfiguration axisConfiguration =
                    CarbonConfigurationContextFactory.getConfigurationContext()
                            .getAxisConfiguration();
            ArrayList<Parameter> axis_mailParams = axisConfiguration.getTransportOut("mailto").getParameters();
            Map<String, String> globalProperties = new HashMap<String, String>();
            for (Parameter parameter : axis_mailParams) {
                globalProperties.put(parameter.getName(), (String) parameter.getValue());
            }

            EmailEventAdapterFactory emailEventAdapterFactory = new EmailEventAdapterFactory();
            OutputEventAdapter emailEventAdapter = (EmailEventAdapter) emailEventAdapterFactory.createEventAdapter(null, globalProperties);

            //get dynamic properties
            Map<String, String> dynamicProperties = new HashMap<String, String>();
            String emailSubject = emailNotification.getSubject();
            dynamicProperties.put(EmailEventConstants.EmailProperty.EMAIL_SUBJECT, emailSubject);
            emailContentType = emailNotification.getContentType();
            if (emailContentType == null) {
                emailContentType = EmailEventConstants.TEMPLATE_CONTENT_TYPE_DEFAULT;
            }
            dynamicProperties.put(EmailEventConstants.EmailProperty.EMAIL_CONTENT_TYPE, emailContentType);
            String emailAdd = emailNotification.getSendTo();
            dynamicProperties.put(EmailEventConstants.EmailProperty.EMAIL_ADDRESS, emailAdd);

            StringBuilder contents = new StringBuilder();
            contents.append(emailNotification.getBody())
                    .append(System.getProperty(EmailEventConstants.EmailProperty.EMAIL_CONTENT_LINE_SEPARATOR))
                    .append(System.getProperty(EmailEventConstants.EmailProperty.EMAIL_CONTENT_LINE_SEPARATOR))
                    .append(emailNotification.getFooter());
            String emailBody = contents.toString();

            emailEventAdapter.init();
            emailEventAdapter.connect();
            emailEventAdapter.publish(emailBody, dynamicProperties);

            if (log.isDebugEnabled()) {
                log.debug("Email content : " + emailBody);
            }
            log.info("Mail has been sent to " + emailNotification.getSendTo());
        } catch (OutputEventAdapterException e) {
            log.error("Failed Sending Email");
            throw new EventMgtException("Failed Sending Email", e);
        }
        return true;
    }

    @Override
    public void init(InitConfig configuration) throws IdentityRuntimeException {
    }

    @Override
    public String getName() {
        return "emailSend";
    }
}
