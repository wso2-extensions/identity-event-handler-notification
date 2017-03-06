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

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.email.mgt.EmailTemplateManager;
import org.wso2.carbon.identity.event.AbstractEventHandler;
import org.wso2.carbon.identity.event.handler.notification.NotificationHandler;
import org.wso2.carbon.identity.event.handler.notification.util.NotificationUtil;
import org.wso2.carbon.identity.mgt.RealmService;

/**
 * Notification handler service.
 */
@Component(
        name = "org.wso2.carbon.identity.event.handler.notification.internal.NotificationHandlerServiceComponent",
        immediate = true,
        property = { "componentName=wso2-carbon-identity-event" })
public class NotificationHandlerServiceComponent {

    private static Logger log = LoggerFactory.getLogger(NotificationHandlerServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {
        try {
            context.getBundleContext()
                    .registerService(AbstractEventHandler.class.getName(), new NotificationHandler(), null);
            NotificationUtil.loadProperties();
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
            name = "RealmService",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService") protected void setRealmService(RealmService realmService) {
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
            name = "EmailTemplateManager",
            service = EmailTemplateManager.class,
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
