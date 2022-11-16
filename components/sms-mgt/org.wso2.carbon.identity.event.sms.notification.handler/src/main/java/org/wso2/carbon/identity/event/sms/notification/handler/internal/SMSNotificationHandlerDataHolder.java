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

package org.wso2.carbon.identity.event.sms.notification.handler.internal;

import org.wso2.carbon.event.outbound.adapter.websubhub.WebSubHubAdapterService;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * SMS Notification Handler service component's value holder.
 */
public class SMSNotificationHandlerDataHolder {

    private static final SMSNotificationHandlerDataHolder instance =
            new SMSNotificationHandlerDataHolder();
    private static WebSubHubAdapterService webSubHubEventAdapter;

    private static NotificationSenderManagementService notificationSenderManagementService;

    private RealmService realmService;

    private SMSNotificationHandlerDataHolder() {

    }

    public static SMSNotificationHandlerDataHolder getInstance() {

        return instance;
    }

    public static WebSubHubAdapterService getWebSubHubEventAdapterService() {

        return webSubHubEventAdapter;
    }

    public static void setWebSubHubEventAdapterService(WebSubHubAdapterService webSubHubEventAdapter) {

        SMSNotificationHandlerDataHolder.webSubHubEventAdapter = webSubHubEventAdapter;
    }

    public static NotificationSenderManagementService getNotificationSenderManagementService() {

        return notificationSenderManagementService;
    }

    public static void setNotificationSenderManagementService(NotificationSenderManagementService
                                                                      notificationSenderManagementService) {

        SMSNotificationHandlerDataHolder.notificationSenderManagementService = notificationSenderManagementService;
    }

    public RealmService getRealmService() {

        return realmService;
    }

    public void setRealmService(RealmService realmService) {

        this.realmService = realmService;
    }
}
