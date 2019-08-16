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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(
         name = "identity.event.handler.notification", 
         immediate = true)
public class NotificationHandlerServiceComponent {

    private static final Log log = LogFactory.getLog(NotificationHandlerServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {
        try {
            context.getBundleContext().registerService(AbstractEventHandler.class.getName(), new NotificationHandler(), null);
            context.getBundleContext().registerService(AbstractEventHandler.class.getName(), new DefaultNotificationHandler(), null);
            context.getBundleContext().registerService(TenantMgtListener.class.getName(), new NotificationEventTenantListener(), null);
        } catch (Throwable e) {
            log.error("Error occurred while activating Notification Handler Service Component", e);
        }
        if (log.isDebugEnabled()) {
            log.debug("Notification Handler bundle is activated");
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (log.isDebugEnabled()) {
            log.debug("Notification Handler bundle is de-activated");
        }
    }

    @Reference(
             name = "registry.service", 
             service = org.wso2.carbon.registry.core.service.RegistryService.class, 
             cardinality = ReferenceCardinality.MANDATORY, 
             policy = ReferencePolicy.DYNAMIC, 
             unbind = "unsetRegistryService")
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

    @Reference(
             name = "realm.service", 
             service = org.wso2.carbon.user.core.service.RealmService.class, 
             cardinality = ReferenceCardinality.MANDATORY, 
             policy = ReferencePolicy.DYNAMIC, 
             unbind = "unsetRealmService")
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

    @Reference(
             name = "eventStreamManager.service", 
             service = org.wso2.carbon.event.stream.core.EventStreamService.class, 
             cardinality = ReferenceCardinality.MANDATORY, 
             policy = ReferencePolicy.DYNAMIC, 
             unbind = "unsetEventStreamService")
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

    @Reference(
             name = "eventPublisherService.service", 
             service = org.wso2.carbon.event.publisher.core.EventPublisherService.class, 
             cardinality = ReferenceCardinality.MANDATORY, 
             policy = ReferencePolicy.DYNAMIC, 
             unbind = "unsetEventPublisherService")
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

    @Reference(
             name = "emailTemplateManager.service", 
             service = org.wso2.carbon.email.mgt.EmailTemplateManager.class, 
             cardinality = ReferenceCardinality.MANDATORY, 
             policy = ReferencePolicy.DYNAMIC, 
             unbind = "unsetEmailTemplateManager")
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

