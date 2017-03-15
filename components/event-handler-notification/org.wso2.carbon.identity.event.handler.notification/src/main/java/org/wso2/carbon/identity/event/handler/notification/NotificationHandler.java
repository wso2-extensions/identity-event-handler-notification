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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.common.base.event.EventContext;
import org.wso2.carbon.identity.common.base.event.model.Event;
import org.wso2.carbon.identity.common.base.exception.IdentityException;
import org.wso2.carbon.identity.common.base.exception.IdentityRuntimeException;
import org.wso2.carbon.identity.common.base.handler.InitConfig;
import org.wso2.carbon.identity.common.base.message.MessageContext;
import org.wso2.carbon.identity.event.AbstractEventHandler;
import org.wso2.carbon.identity.event.EventException;
import org.wso2.carbon.identity.event.handler.notification.email.bean.Notification;
import org.wso2.carbon.identity.event.handler.notification.exception.NotificationHandlerException;
import org.wso2.carbon.identity.event.handler.notification.util.NotificationUtil;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class NotificationHandler extends AbstractEventHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationHandler.class);

    @Override
    public void handle(EventContext eventContext, Event event) throws EventException {
        Map<String, String> placeHolderData = new HashMap<>();

        for (Map.Entry<String, Object> entry : event.getEventProperties().entrySet()) {
            if (entry.getValue() instanceof String) {
                placeHolderData.put(entry.getKey(), (String) entry.getValue());
            }
        }

        try {
            Notification notification = NotificationUtil.buildNotification(placeHolderData);
            NotificationUtil.sendEmail(notification);
        } catch (NotificationHandlerException e) {
            throw new EventException("Error while sending email notification", e);
        }
    }

    @Override
    public void configure(InitConfig initConfig) throws IdentityException {

    }

    @Override
    public void init(InitConfig configuration) throws IdentityRuntimeException {
    }

    public String getName() {
        return "emailSend";
    }

    @Override
    public int getPriority(MessageContext messageContext) {
        return 50;
    }

    @Override
    public boolean isAsync() {
        return true;
    }
}

