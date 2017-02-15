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

package org.wso2.carbon.identity.event.handler.notification.internal;

import org.wso2.carbon.email.mgt.EmailTemplateManager;
import org.wso2.carbon.identity.mgt.RealmService;

/**
 * Notification handler data holder.
 */
public class NotificationHandlerDataHolder {

    private static volatile NotificationHandlerDataHolder instance = new NotificationHandlerDataHolder();
    private RealmService realmService = null;
    private EmailTemplateManager emailTemplateManager = null;

    private NotificationHandlerDataHolder() {

    }

    public static NotificationHandlerDataHolder getInstance() {
        return instance;
    }

    public RealmService getRealmService() {
        return realmService;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

    public void setEmailTemplateManager(EmailTemplateManager emailTemplateManager) {
        this.emailTemplateManager = emailTemplateManager;
    }

    public EmailTemplateManager getEmailTemplateManager() {
        return emailTemplateManager;
    }
}

