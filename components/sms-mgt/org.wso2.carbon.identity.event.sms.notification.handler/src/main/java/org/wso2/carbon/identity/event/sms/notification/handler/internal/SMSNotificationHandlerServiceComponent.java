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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.event.outbound.adapter.websubhub.WebSubHubAdapterService;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.event.sms.notification.handler.SMSNotificationHandler;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * SMS Notification Handler service component.
 */
@Component(
        name = "identity.event.sms.notification.handler",
        immediate = true)
public class SMSNotificationHandlerServiceComponent {

    private static final Log log = LogFactory.getLog(SMSNotificationHandlerServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            context.getBundleContext().registerService(AbstractEventHandler.class.getName(),
                    new SMSNotificationHandler(), null);
        } catch (Throwable e) {
            log.error("Error occurred while activating SMS Notification Handler Service Component", e);
        }
        if (log.isDebugEnabled()) {
            log.debug("SMS Notification Handler service is activated");
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        if (log.isDebugEnabled()) {
            log.debug("SMS Notification Handler service is de-activated");
        }
    }

    @Reference(
            name = "org.wso2.carbon.event.outbound.adapter.websubhub",
            service = WebSubHubAdapterService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetWebSubHubAdapterService"
    )
    protected void setWebSubHubAdapterService(WebSubHubAdapterService webSubHubAdapterService) {

        SMSNotificationHandlerDataHolder.setWebSubHubEventAdapterService(webSubHubAdapterService);
    }

    protected void unsetWebSubHubAdapterService(WebSubHubAdapterService webSubHubAdapterService) {

        SMSNotificationHandlerDataHolder.setWebSubHubEventAdapterService(null);
    }

    @Reference(
            name = "org.wso2.carbon.identity.notification.sender.tenant.config",
            service = NotificationSenderManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetNotificationSenderManagementService"
    )
    protected void setNotificationSenderManagementService(NotificationSenderManagementService
                                                                  notificationSenderManagementService) {

        SMSNotificationHandlerDataHolder.setNotificationSenderManagementService(notificationSenderManagementService);
    }

    protected void unsetNotificationSenderManagementService(NotificationSenderManagementService
                                                                    notificationSenderManagementService) {

        SMSNotificationHandlerDataHolder.setNotificationSenderManagementService(null);
    }

    @Reference(
            name = "RealmService",
            service = org.wso2.carbon.user.core.service.RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        SMSNotificationHandlerDataHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {

        SMSNotificationHandlerDataHolder.getInstance().setRealmService(null);
    }
}
