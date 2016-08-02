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

package org.wso2.carbon.identity.event.handler.email;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.event.stream.core.EventStreamService;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.core.handler.InitConfig;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.event.handler.email.email.bean.EmailNotification;
import org.wso2.carbon.identity.event.handler.email.email.bean.Notification;
import org.wso2.carbon.identity.event.handler.email.internal.NotificationHandlerDataHolder;
import org.wso2.carbon.identity.event.handler.email.util.NotificationUtil;

import java.util.HashMap;
import java.util.Map;

public class NotificationHandler extends AbstractEventHandler {

    private static final Log log = LogFactory.getLog(NotificationHandler.class);

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        Map<String, String> placeHolderData = new HashMap<>();

        for (Map.Entry<String, Object> entry : event.getEventProperties().entrySet()) {
            if (entry.getValue() instanceof String) {
                placeHolderData.put(entry.getKey(), (String) entry.getValue());
            }
        }

        Notification notification = NotificationUtil.getNotification(event, placeHolderData);
        publishToStream(notification, placeHolderData);
    }

    protected void publishToStream(Notification notification, Map<String, String> placeHolderDataMap) {

        EventStreamService service = NotificationHandlerDataHolder.getInstance().getEventStreamService();

        org.wso2.carbon.databridge.commons.Event databridgeEvent = new org.wso2.carbon.databridge.commons.Event();
        databridgeEvent.setTimeStamp(System.currentTimeMillis());
        Map<String, String> arbitraryDataMap = new HashMap<>();

        if (notification instanceof EmailNotification) {
            databridgeEvent.setStreamId(NotificationConstants.EmailNotification.STREAM_ID);

            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_EVENT_TYPE, ((EmailNotification) notification).getTemplate().getNotificationEvent());
            arbitraryDataMap.put(IdentityEventConstants.EventProperty.USER_NAME,
                    placeHolderDataMap.get(IdentityEventConstants.EventProperty.USER_NAME));
            arbitraryDataMap.put(IdentityEventConstants.EventProperty.USER_STORE_DOMAIN,
                    placeHolderDataMap.get(IdentityEventConstants.EventProperty.USER_STORE_DOMAIN));
            arbitraryDataMap.put(IdentityEventConstants.EventProperty.TENANT_DOMAIN,
                    placeHolderDataMap.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN));
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_SEND_FROM, ((EmailNotification) notification).getSendFrom());
            for (Map.Entry<String, String> placeHolderDataEntry : placeHolderDataMap.entrySet()) {
                arbitraryDataMap.put(placeHolderDataEntry.getKey(), placeHolderDataEntry.getValue());
            }
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_SUBJECT_TEMPLATE, ((EmailNotification) notification).getTemplate().getSubject());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_BODY_TEMPLATE, ((EmailNotification) notification).getTemplate().getBody());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_FOOTER_TEMPLATE, ((EmailNotification) notification).getTemplate().getFooter());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_LOCALE, ((EmailNotification) notification).getTemplate().getLocale());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_CONTENT_TYPE, ((EmailNotification) notification).getTemplate().getContentType());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_SEND_TO, ((EmailNotification) notification).getSendTo());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_SUBJECT, ((EmailNotification) notification).getSubject());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_BODY, ((EmailNotification) notification).getBody());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_FOOTER, ((EmailNotification) notification).getFooter());
        }

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
