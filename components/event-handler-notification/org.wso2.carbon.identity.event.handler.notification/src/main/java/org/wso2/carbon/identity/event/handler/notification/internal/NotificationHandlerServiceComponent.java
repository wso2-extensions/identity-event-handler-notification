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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.event.publisher.core.EventPublisherService;
import org.wso2.carbon.event.stream.core.EventStreamService;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.event.handler.notification.DefaultNotificationHandler;
import org.wso2.carbon.identity.event.handler.notification.NotificationHandler;
import org.wso2.carbon.identity.event.handler.notification.listener.NotificationEventTenantListener;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.email.mgt.EmailTemplateManager;

/**
 * @scr.component name="identity.event.handler.notification" immediate="true
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="realm.service"
 * interface="org.wso2.carbon.user.core.service.RealmService"cardinality="1..1"
 * policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 * @scr.reference name="eventStreamManager.service"
 * interface="org.wso2.carbon.event.stream.core.EventStreamService" cardinality="1..1"
 * policy="dynamic" bind="setEventStreamService" unbind="unsetEventStreamService"
 * @scr.reference name="eventPublisherService.service"
 * interface="org.wso2.carbon.event.publisher.core.EventPublisherService" cardinality="1..1"
 * policy="dynamic" bind="setEventPublisherService" unbind="unsetEventPublisherService"
 * @scr.reference name="emailTemplateManager.service"
 * interface="org.wso2.carbon.email.mgt.EmailTemplateManager" cardinality="1..1"
 * policy="dynamic" bind="setEmailTemplateManager" unbind="unsetEmailTemplateManager"
 */

public class NotificationHandlerServiceComponent {

    private static Log log = LogFactory.getLog(NotificationHandlerServiceComponent.class);

    protected void activate(ComponentContext context) {
        try {
            context.getBundleContext().registerService(AbstractEventHandler.class.getName(), new NotificationHandler(), null);
            context.getBundleContext().registerService(AbstractEventHandler.class.getName(), new DefaultNotificationHandler(), null);
            context.getBundleContext().registerService(TenantMgtListener.class.getName(),
                    new NotificationEventTenantListener(), null);
        } catch (Throwable e) {
            log.error("Error occurred while activating Notification Handler Service Component", e);
        }
        if (log.isDebugEnabled()) {
            log.debug("Notification Handler bundle is activated");
        }
    }

    protected void deactivate(ComponentContext context) {
        if (log.isDebugEnabled()) {
            log.debug("Notification Handler bundle is de-activated");
        }
    }

    protected void setRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Registry Service");
        }
        NotificationHandlerDataHolder.getInstance().setRegistryService(registryService);
    }

    protected void unsetRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("UnSetting the Registry Service");
        }
        NotificationHandlerDataHolder.getInstance().setRegistryService(null);
    }

    protected void setRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Realm Service");
        }
        NotificationHandlerDataHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("UnSetting the Realm Service");
        }
        NotificationHandlerDataHolder.getInstance().setRealmService(null);
    }

    protected void setEventStreamService(EventStreamService eventStreamService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Event Stream Service");
        }
        NotificationHandlerDataHolder.getInstance().setEventStreamService(eventStreamService);
    }

    protected void unsetEventStreamService(EventStreamService eventStreamService) {
        if (log.isDebugEnabled()) {
            log.debug("UnSetting the Event Stream Service");
        }
        NotificationHandlerDataHolder.getInstance().setEventStreamService(null);
    }

    protected void setEventPublisherService(EventPublisherService eventPublisherService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Event Publisher Service");
        }
        NotificationHandlerDataHolder.getInstance().setEventPublisherService(eventPublisherService);
    }

    protected void unsetEventPublisherService(EventPublisherService eventPublisherService) {
        if (log.isDebugEnabled()) {
            log.debug("UnSetting the Event Publisher Service");
        }
        NotificationHandlerDataHolder.getInstance().setEventPublisherService(null);
    }

    protected void setEmailTemplateManager(EmailTemplateManager emailTemplateManager) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Email Template Manager");
        }
        NotificationHandlerDataHolder.getInstance().setEmailTemplateManager(emailTemplateManager);
    }

    protected void unsetEmailTemplateManager(EmailTemplateManager emailTemplateManager) {
        if (log.isDebugEnabled()) {
            log.debug("UnSetting the Email Template Manager");
        }
        NotificationHandlerDataHolder.getInstance().setEmailTemplateManager(null);
    }
}
