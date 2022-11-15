/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.notification.sender.tenant.config.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.email.mgt.SMSProviderPayloadTemplateManager;
import org.wso2.carbon.event.publisher.core.EventPublisherService;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementService;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementServiceImpl;
import org.wso2.carbon.identity.notification.sender.tenant.config.handlers.ConfigurationHandler;
import org.wso2.carbon.identity.notification.sender.tenant.config.handlers.DefaultChannelConfigurationHandler;
import org.wso2.carbon.identity.tenant.resource.manager.core.ResourceManager;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.util.ArrayList;
import java.util.List;

/**
 * Component class for Notification Sender service.
 */
@Component(name = "identity.notification.sender.tenant.config",
           immediate = true)
public class NotificationSenderTenantConfigServiceDS {

    private static final Log log = LogFactory.getLog(NotificationSenderTenantConfigServiceDS.class);
    private static final List<ConfigurationHandler> configurationHandlerList = new
            ArrayList<>();

    /**
     * Register Tenant Aware Axis2 Configuration Context Observer as an OSGI service.
     *
     * @param context OSGI service component context.
     */
    @Activate
    protected void activate(ComponentContext context) {

        try {
            NotificationSenderManagementServiceImpl notificationSenderManagementService =
                    new NotificationSenderManagementServiceImpl();
            NotificationSenderTenantConfigDataHolder.getInstance().
                    setNotificationSenderManagementService(notificationSenderManagementService);
            configurationHandlerList.add(new DefaultChannelConfigurationHandler());
            registerConfigurationHandler();
            context.getBundleContext().registerService(NotificationSenderManagementService.class.getName(),
                    notificationSenderManagementService, null);
        } catch (Exception e) {
            log.error("Can not create the tenant wise email sender config service.", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        if (log.isDebugEnabled()) {
            log.debug("Tenant wise email sender config service bundle is de-activated");
        }
    }

    @Reference(name = "CarbonEventPublisherService",
            service = org.wso2.carbon.event.publisher.core.EventPublisherService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetCarbonEventPublisherService")
    protected void setCarbonEventPublisherService(EventPublisherService carbonEventPublisherService) {

        if (log.isDebugEnabled()) {
            log.debug("Setting the CarbonEventPublisherService");
        }
        NotificationSenderTenantConfigDataHolder.getInstance()
                .setCarbonEventPublisherService(carbonEventPublisherService);
    }

    protected void unsetCarbonEventPublisherService(EventPublisherService carbonEventPublisherService) {

        if (log.isDebugEnabled()) {
            log.debug("Un Setting the CarbonEventPublisherService Service");
        }
        NotificationSenderTenantConfigDataHolder.getInstance().setCarbonEventPublisherService(null);
    }

    @Reference(name = "ConfigurationManager",
            service = org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetConfigurationManager")
    protected void setConfigurationManager(ConfigurationManager configurationManager) {

        if (log.isDebugEnabled()) {
            log.debug("Setting the CarbonEventPublisherService");
        }
        NotificationSenderTenantConfigDataHolder.getInstance().setConfigurationManager(configurationManager);
    }

    protected void unsetConfigurationManager(ConfigurationManager configurationManager) {

        if (log.isDebugEnabled()) {
            log.debug("Un Setting theCarbonEventPublisherService Service");
        }
        NotificationSenderTenantConfigDataHolder.getInstance().setConfigurationManager(null);
    }

    @Reference(name = "config.context.service",
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetClusteringAgent")
    protected void setClusteringAgent(ConfigurationContextService configurationContextService) {

        NotificationSenderTenantConfigDataHolder.getInstance().setClusteringAgent(
                configurationContextService.getServerConfigContext().getAxisConfiguration().getClusteringAgent());
    }

    protected void unsetClusteringAgent(ConfigurationContextService configurationContextService) {

        NotificationSenderTenantConfigDataHolder.getInstance().setClusteringAgent(null);
    }

    @Reference(name = "resource.manager",
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetResourceManager")
    protected void setResourceManager(ResourceManager manager) {

        NotificationSenderTenantConfigDataHolder.getInstance().setResourceManager(manager);
    }

    protected void unsetResourceManager(ResourceManager manager) {

        NotificationSenderTenantConfigDataHolder.getInstance().setResourceManager(null);
    }

    @Reference(name = "sms.provider.payload",
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetSMSProviderPayloadTemplateManager")
    protected void setSMSProviderPayloadTemplateManager(SMSProviderPayloadTemplateManager manager) {

        NotificationSenderTenantConfigDataHolder.getInstance().setSMSProviderPayloadTemplateManager(manager);
    }

    protected void unsetSMSProviderPayloadTemplateManager(SMSProviderPayloadTemplateManager manager) {
        NotificationSenderTenantConfigDataHolder.getInstance().setSMSProviderPayloadTemplateManager(null);
    }

    @Reference(name = "configuration.handler.tracker.service",
            service = org.wso2.carbon.identity.notification.sender.tenant.config.handlers.ConfigurationHandler.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetConfigurationHandler")
    protected void setConfigurationHandler(ConfigurationHandler configurationHandler) {

        if (log.isDebugEnabled()) {
            log.debug("Setting the Configuration Handler");
        }

        if (NotificationSenderTenantConfigDataHolder.getInstance().getNotificationSenderManagementService() != null) {
            NotificationSenderTenantConfigDataHolder.getInstance().getNotificationSenderManagementService()
                    .registerConfigurationHandler(configurationHandler);
        } else {
            configurationHandlerList.add(configurationHandler);
        }
    }

    protected void unsetConfigurationHandler(ConfigurationHandler configurationHandler) {

        //TODO unregistering logic
    }

    private void registerConfigurationHandler() {

        configurationHandlerList.forEach(handler ->
                NotificationSenderTenantConfigDataHolder.getInstance()
                        .getNotificationSenderManagementService().registerConfigurationHandler(handler));
    }

}
