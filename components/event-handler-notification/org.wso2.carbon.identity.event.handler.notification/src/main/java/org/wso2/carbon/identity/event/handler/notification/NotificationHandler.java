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
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.event.stream.core.EventStreamService;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.core.handler.InitConfig;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.event.handler.notification.email.bean.Notification;
import org.wso2.carbon.identity.event.handler.notification.internal.NotificationHandlerDataHolder;
import org.wso2.carbon.identity.event.handler.notification.util.NotificationUtil;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the Email and SMS Notification Handler which connected to the direct CEP stream.
 * Extended from the DefaultNotificationHandler which is define the default notification send.
 *
 */
public class NotificationHandler extends DefaultNotificationHandler {

    private static final Log log = LogFactory.getLog(NotificationHandler.class);
    private static final String STREAM_ID = "id_gov_notify_stream:1.0.0";

    @Override
    public void handleEvent(Event event) throws IdentityEventException {
        //We can set the notification template from the identity-even.properties file as a property of the subscription
        //property. Then it will get the first priority.
        String notificationTemplate = getNotificationTemplate(event);
        if(StringUtils.isNotEmpty(notificationTemplate)){
            event.getEventProperties().put(NotificationConstants.EmailNotification.EMAIL_TEMPLATE_TYPE,
                    notificationTemplate);
        }
        Map<String, String> arbitraryDataMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : event.getEventProperties().entrySet()) {
            if (entry.getValue() instanceof String) {
                arbitraryDataMap.put(entry.getKey(), (String) entry.getValue());
            }
        }
        Notification notification = NotificationUtil.buildNotification(event, arbitraryDataMap);

        //Stream definition will be read from the the identity-even.properties file as a property of the subscription
        //property. Then it will get the first priority.
        String streamDefinitionID = getStreamDefinitionID(event);
        //This stream-id was set to the map to pass to the publishToStream method only to avoid API change.
        arbitraryDataMap.put("tmp-stream-id", streamDefinitionID);

        try {
            // Use the super tenant's EmailPublisher.xml and id_gov_notify_stream_1.0.0.json to publish the stream.
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            publishToStream(notification, arbitraryDataMap);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    protected void publishToStream(Notification notification, Map<String, String> placeHolderDataMap) {

        EventStreamService service = NotificationHandlerDataHolder.getInstance().getEventStreamService();

        org.wso2.carbon.databridge.commons.Event databridgeEvent = new org.wso2.carbon.databridge.commons.Event();
        databridgeEvent.setTimeStamp(System.currentTimeMillis());
        Map<String, String> arbitraryDataMap = new HashMap<>();

        databridgeEvent.setStreamId(placeHolderDataMap.remove("tmp-stream-id"));

        arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_EVENT_TYPE, I18nEmailUtil.
                getNormalizedName(notification.getTemplate().getTemplateDisplayName()));
        arbitraryDataMap.put(IdentityEventConstants.EventProperty.USER_NAME,
                placeHolderDataMap.get(IdentityEventConstants.EventProperty.USER_NAME));
        arbitraryDataMap.put(IdentityEventConstants.EventProperty.USER_STORE_DOMAIN,
                placeHolderDataMap.get(IdentityEventConstants.EventProperty.USER_STORE_DOMAIN));
        arbitraryDataMap.put(IdentityEventConstants.EventProperty.TENANT_DOMAIN,
                placeHolderDataMap.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN));
        arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_SEND_FROM, notification.getSendFrom());
        for (Map.Entry<String, String> placeHolderDataEntry : placeHolderDataMap.entrySet()) {
            arbitraryDataMap.put(placeHolderDataEntry.getKey(), placeHolderDataEntry.getValue());
        }
        arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_SUBJECT_TEMPLATE, notification.
                getTemplate().getSubject());
        arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_BODY_TEMPLATE, notification.
                getTemplate().getBody());
        arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_FOOTER_TEMPLATE, notification.
                getTemplate().getFooter());
        arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_LOCALE, notification.getTemplate().
                getLocale());
        arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_CONTENT_TYPE, notification.
                getTemplate().getEmailContentType());
        arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_SEND_TO, notification.getSendTo());
        arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_SUBJECT, notification.getSubject());
        arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_BODY, notification.getBody());
        arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_FOOTER, notification.getFooter());


        databridgeEvent.setArbitraryDataMap(arbitraryDataMap);
        service.publish(databridgeEvent);
    }


    @Override
    public String getStreamDefinitionID(Event event) throws IdentityEventException {
        String streamDefinitionID = super.getStreamDefinitionID(event);
        if(StringUtils.isEmpty(streamDefinitionID)){
            streamDefinitionID = STREAM_ID ;
        }
        return streamDefinitionID;
    }

    @Override
    public String getName() {
        return "emailSend";
    }
}
