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
import org.wso2.carbon.email.mgt.config.EmailTemplateManager;
import org.wso2.carbon.email.mgt.config.EmailTemplateManagerImpl;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.user.core.service.RealmService;


/**
 * @scr.component name="I18nMgtServiceComponent" immediate="true"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="realm.service"
 * interface="org.wso2.carbon.user.core.service.RealmService" cardinality="1..1"
 * policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 * @scr.reference name="RegistryResourceMgtService"
 * interface="org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryResourceMgtService" unbind="unsetRegistryResourceMgtService"
 *
 */

public class I18nMgtServiceComponent {

    private static Log log = LogFactory.getLog(I18nMgtServiceComponent.class);

    private I18nMgtDataHolder dataHolder = I18nMgtDataHolder.getInstance();

    protected void activate(ComponentContext context) {
        try {
            BundleContext bundleCtx = context.getBundleContext();
            TenantManagementListener idPMgtTenantMgtListener = new TenantManagementListener();
            ServiceRegistration tenantMgtListenerSR = bundleCtx.registerService(
                    TenantMgtListener.class.getName(), idPMgtTenantMgtListener, null);
            if (tenantMgtListenerSR != null) {
                log.debug("I18n Management - TenantMgtListener registered");
            } else {
                log.error("I18n Management - TenantMgtListener could not be registered");
            }
            loadEmailConfigurations();
            log.debug("I18n Management is activated");
        } catch (Throwable e) {
            log.error("Error while activating I18n Management bundle", e);
        }
    }

    private void loadEmailConfigurations() {
        //Load email template configuration on server startup.
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        EmailTemplateManager emailTemplateManager = new EmailTemplateManagerImpl();
        try {
            emailTemplateManager.addDefaultEmailTemplates(tenantDomain);
        } catch (I18nEmailMgtException e) {
            log.error("Error occurred while loading default email templates", e);
        }
    }

    protected void deactivate(ComponentContext context) {
        if (log.isDebugEnabled()) {
            log.debug("I18n Management bundle is de-activated");
        }
    }

    protected void setRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Realm Service");
        }
        dataHolder.setRealmService(realmService);
    }

    protected void setRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Registry Service");
        }
        dataHolder.setRegistryService(registryService);
    }

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