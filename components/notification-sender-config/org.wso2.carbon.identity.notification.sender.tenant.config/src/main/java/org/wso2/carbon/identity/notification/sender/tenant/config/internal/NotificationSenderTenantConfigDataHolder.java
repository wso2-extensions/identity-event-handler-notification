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

import org.apache.axis2.clustering.ClusteringAgent;
import org.wso2.carbon.email.mgt.SMSProviderPayloadTemplateManager;
import org.wso2.carbon.event.publisher.core.EventPublisherService;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.notification.sender.tenant.config.handlers.ChannelConfigurationHandler;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.tenant.resource.manager.core.ResourceManager;

import java.util.HashMap;
import java.util.Map;

/**
 * DataHolder for Tenant wise notification sender.
 */
public class NotificationSenderTenantConfigDataHolder {

    private static final NotificationSenderTenantConfigDataHolder instance =
            new NotificationSenderTenantConfigDataHolder();
    private EventPublisherService carbonEventPublisherService = null;
    private ConfigurationManager configurationManager;
    private ResourceManager resourceManager = null;
    private ClusteringAgent clusteringAgent = null;
    private SMSProviderPayloadTemplateManager smsProviderPayloadTemplateManager = null;
    Map<String, ChannelConfigurationHandler> configurationHandlerMap = new HashMap<>();
    private ApplicationManagementService applicationManagementService = null;
    private OrganizationManager organizationManager = null;

    private NotificationSenderTenantConfigDataHolder() {
    }

    public static NotificationSenderTenantConfigDataHolder getInstance() {

        return instance;
    }

    public EventPublisherService getCarbonEventPublisherService() {

        return carbonEventPublisherService;
    }

    public void setCarbonEventPublisherService(EventPublisherService carbonEventPublisherService) {

        this.carbonEventPublisherService = carbonEventPublisherService;
    }

    public void setConfigurationManager(ConfigurationManager configurationManager) {

        this.configurationManager = configurationManager;
    }

    public ConfigurationManager getConfigurationManager() {

        return configurationManager;
    }

    public ResourceManager getResourceManager() {

        return resourceManager;
    }

    public void setResourceManager(ResourceManager resourceManager) {

        this.resourceManager = resourceManager;
    }

    public ClusteringAgent getClusteringAgent() {

        return clusteringAgent;
    }

    public void setClusteringAgent(ClusteringAgent clusteringAgent) {

        this.clusteringAgent = clusteringAgent;
    }

    public void setSMSProviderPayloadTemplateManager(SMSProviderPayloadTemplateManager manager) {

        this.smsProviderPayloadTemplateManager = manager;
    }

    public SMSProviderPayloadTemplateManager getSmsProviderPayloadTemplateManager() {

        return smsProviderPayloadTemplateManager;
    }

    public Map<String, ChannelConfigurationHandler> getConfigurationHandlerMap() {

        return configurationHandlerMap;
    }

    public void registerConfigurationHandler(ChannelConfigurationHandler handler) {

        configurationHandlerMap.put(handler.getName(), handler);
    }

    public void unregisterConfigurationHandler(ChannelConfigurationHandler handler) {

        configurationHandlerMap.remove(handler.getName(), handler);
    }

    public void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        this.applicationManagementService = applicationManagementService;
    }

    public ApplicationManagementService getApplicationManagementService() {

        return applicationManagementService;
    }

    public void setOrganizationManager(OrganizationManager organizationManager) {

        this.organizationManager = organizationManager;
    }

    public OrganizationManager getOrganizationManager() {

        return organizationManager;
    }
}
