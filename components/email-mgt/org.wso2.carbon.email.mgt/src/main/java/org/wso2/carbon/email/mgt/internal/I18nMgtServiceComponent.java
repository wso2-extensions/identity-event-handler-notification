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
package org.wso2.carbon.email.mgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.email.mgt.EmailTemplateManager;
import org.wso2.carbon.email.mgt.EmailTemplateManagerImpl;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.identity.governance.service.notification.NotificationTemplateManager;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.user.core.service.RealmService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(
         name = "I18nMgtServiceComponent", 
         immediate = true)
public class I18nMgtServiceComponent {

    private static final Log log = LogFactory.getLog(I18nMgtServiceComponent.class);

    private I18nMgtDataHolder dataHolder = I18nMgtDataHolder.getInstance();

    @Activate
    protected void activate(ComponentContext context) {
        try {
            BundleContext bundleCtx = context.getBundleContext();

            // Register Email Mgt Service as an OSGi service.
            EmailTemplateManagerImpl emailTemplateManager = new EmailTemplateManagerImpl();
            ServiceRegistration emailTemplateSR = bundleCtx.registerService(EmailTemplateManager.class.getName(),
                    emailTemplateManager, null);
            if (emailTemplateSR != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Email Template Mgt Service registered.");
                }
            } else {
                log.error("Error registering Email Template Mgt Service.");
            }

            // Register EmailTemplateManagerImpl.
            ServiceRegistration notificationManagerSR = bundleCtx
                    .registerService(NotificationTemplateManager.class.getName(), emailTemplateManager, null);
            if (notificationManagerSR != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Notification Template Mgt Service registered.");
                }
            } else {
                log.error("Error registering Notification Template Mgt Service.");
            }

            TenantManagementListener emailMgtTenantListener = new TenantManagementListener();
            ServiceRegistration tenantMgtListenerSR = bundleCtx.registerService(TenantMgtListener.class.getName(),
                    emailMgtTenantListener, null);
            if (tenantMgtListenerSR != null) {
                if (log.isDebugEnabled()) {
                    log.debug("I18n Management - TenantMgtListener registered");
                }
            } else {
                log.error("I18n Management - TenantMgtListener could not be registered");
            }

            // Load default notification templates.
            loadDefaultEmailTemplates();
            loadDefaultSMSNotificationTemplates();
            log.debug("I18n Management is activated");
        } catch (Throwable e) {
            log.error("Error while activating I18n Management bundle", e);
        }
    }

    private void loadDefaultEmailTemplates() {
        // Load email template configuration on server startup if they don't already exist.
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        EmailTemplateManager emailTemplateManager = new EmailTemplateManagerImpl();
        try {
            emailTemplateManager.addDefaultEmailTemplates(tenantDomain);
        } catch (I18nEmailMgtException e) {
            log.error("Error occurred while loading default email templates", e);
        }
    }

    /**
     * Load default SMS notification template configurations on server startup if they don't already exist.
     */
    private void loadDefaultSMSNotificationTemplates() {

        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        NotificationTemplateManager notificationTemplateManager = new EmailTemplateManagerImpl();
        try {
            notificationTemplateManager
                    .addDefaultNotificationTemplates(NotificationChannels.SMS_CHANNEL.getChannelType(), tenantDomain);
        } catch (NotificationTemplateManagerException e) {
            log.error("Error occurred while loading default SMS templates", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (log.isDebugEnabled()) {
            log.debug("I18n Management bundle is de-activated");
        }
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
        dataHolder.setRealmService(realmService);
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
        dataHolder.setRegistryService(registryService);
    }

    @Reference(
             name = "RegistryResourceMgtService", 
             service = org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService.class, 
             cardinality = ReferenceCardinality.MANDATORY, 
             policy = ReferencePolicy.DYNAMIC, 
             unbind = "unsetRegistryResourceMgtService")
    protected void setRegistryResourceMgtService(RegistryResourceMgtService registryResourceMgtService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting Registry Resource Mgt Service.");
        }
        dataHolder.setRegistryResourceMgtService(registryResourceMgtService);
    }

    protected void unsetRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("UnSetting the Registry Service");
        }
        dataHolder.setRegistryService(null);
    }

    protected void unsetRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("UnSetting the Realm Service");
        }
        dataHolder.setRealmService(null);
    }

    protected void unsetRegistryResourceMgtService(RegistryResourceMgtService registryResourceMgtService) {
        if (log.isDebugEnabled()) {
            log.debug("UnSetting Registry Resource Mgt Service.");
        }
        dataHolder.setRegistryResourceMgtService(null);
    }
}

