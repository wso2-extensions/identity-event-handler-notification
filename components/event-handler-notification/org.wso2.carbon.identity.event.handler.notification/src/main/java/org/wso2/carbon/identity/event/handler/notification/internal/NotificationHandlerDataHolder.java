/*
 * Copyright (c) 2016-2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

import java.util.HashMap;
import java.util.Map;
import org.wso2.carbon.event.publisher.core.EventPublisherService;
import org.wso2.carbon.event.stream.core.EventStreamService;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.governance.service.notification.NotificationTemplateManager;
import org.wso2.carbon.identity.notification.push.provider.PushProvider;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementService;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.email.mgt.EmailTemplateManager;

public class NotificationHandlerDataHolder {

    private static volatile NotificationHandlerDataHolder instance = new NotificationHandlerDataHolder();
    private RealmService realmService = null;
    private RegistryService registryService = null;
    private EventStreamService eventStreamService = null;
    private EventPublisherService eventPublisherService = null;
    private EmailTemplateManager emailTemplateManager = null;
    private NotificationTemplateManager notificationTemplateManager = null;
    private OrganizationManager organizationManager;
    private ApplicationManagementService applicationManagementService;
    private NotificationSenderManagementService notificationSenderManagementService;
    private final Map<String, PushProvider> pushNotificationProviders = new HashMap<>();

    public ApplicationManagementService getApplicationManagementService() {

        return applicationManagementService;
    }

    public void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        this.applicationManagementService = applicationManagementService;
    }

    private NotificationHandlerDataHolder() {

    }

    public static NotificationHandlerDataHolder getInstance() {
        return instance;
    }

    public RegistryService getRegistryService() {
        return registryService;
    }

    public void setRegistryService(RegistryService registryService) {
        this.registryService = registryService;
    }

    public RealmService getRealmService() {
        return realmService;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

    public EventStreamService getEventStreamService() {
        return eventStreamService;
    }

    public void setEventStreamService(EventStreamService eventStreamService) {
        this.eventStreamService = eventStreamService;
    }

    public void setEventPublisherService(EventPublisherService eventPublisherService) {
        this.eventPublisherService = eventPublisherService;
    }

    public EventPublisherService getEventPublisherService() {
        return eventPublisherService;
    }

    public void setEmailTemplateManager(EmailTemplateManager emailTemplateManager) {
        this.emailTemplateManager = emailTemplateManager;
    }

    public EmailTemplateManager getEmailTemplateManager() {
        return emailTemplateManager;
    }

    /**
     * Set notification template manager service.
     *
     * @param notificationTemplateManager
     * {@link org.wso2.carbon.identity.governance.service.notification.NotificationTemplateManager}
     */
    public void setNotificationTemplateManager(NotificationTemplateManager notificationTemplateManager) {

        this.notificationTemplateManager = notificationTemplateManager;
    }

    /**
     * Get notification template manager service.
     *
     * @return {@link org.wso2.carbon.identity.governance.service.notification.NotificationTemplateManager}
     */
    public NotificationTemplateManager getNotificationTemplateManager() {

        return notificationTemplateManager;
    }

    /**
     * Get organization manager service.
     *
     * @return {@link org.wso2.carbon.identity.organization.management.service.OrganizationManager} service.
     */
    public OrganizationManager getOrganizationManager() {

        return organizationManager;
    }

    /**
     * Set organization manager.
     *
     * @param organizationManager {@link org.wso2.carbon.identity.organization.management.service.OrganizationManager} service.
     */
    public void setOrganizationManager(OrganizationManager organizationManager) {

        this.organizationManager = organizationManager;
    }

    /**
     * Set notification sender management service.
     *
     * @param notificationSenderManagementService
     * {@link org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementService}
     */
    public void setNotificationSenderManagementService(NotificationSenderManagementService notificationSenderManagementService) {

        this.notificationSenderManagementService = notificationSenderManagementService;
    }

    /**
     * Get notification sender management service.
     *
     * @return {@link org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementService}
     */
    public NotificationSenderManagementService getNotificationSenderManagementService() {

        return notificationSenderManagementService;
    }

    /**
     * Add a push notification provider.
     *
     * @param providerName Name of the provider.
     * @param provider     {@link org.wso2.carbon.identity.notification.push.provider.PushProvider} instance.
     */
    public void addPushProvider(String providerName, PushProvider provider) {

        pushNotificationProviders.put(providerName, provider);
    }

    /**
     * Remove a push notification provider.
     *
     * @param providerName Name of the provider.
     */
    public void removePushProvider(String providerName) {

        pushNotificationProviders.remove(providerName);
    }

    /**
     * Get a push notification provider.
     *
     * @param providerName Name of the provider.
     * @return {@link org.wso2.carbon.identity.notification.push.provider.PushProvider} instance.
     */
    public PushProvider getPushProvider(String providerName) {

        return pushNotificationProviders.get(providerName);
    }
}
