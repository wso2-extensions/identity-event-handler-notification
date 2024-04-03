/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.email.mgt.dao;

import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;

import java.util.List;

/**
 * This interface is used to manage the persistence of notification templates.
 */
public interface TemplatePersistenceManager {

    /**
     * Add a notification template type.
     *
     * @param displayName           Display name of the template type.
     * @param notificationChannel   Notification channel.
     * @param tenantDomain          Tenant domain.
     * @throws NotificationTemplateManagerServerException If an error occurred while adding the template type.
     */
    void addNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException;

    /**
     * Check whether the specified notification template type exists.
     *
     * @param displayName           Display name of the template type.
     * @param notificationChannel   Notification channel.
     * @param tenantDomain          Tenant domain.
     * @return True if the template type exists, false otherwise.
     * @throws NotificationTemplateManagerServerException If an error occurred while checking the existence.
     */
    boolean isNotificationTemplateTypeExists(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException;

    /**
     * Get all notification template types for given channel and tenant.
     *
     * @param notificationChannel   Notification channel.
     * @param tenantDomain          Tenant domain.
     * @return                      List of available template types.
     * @throws NotificationTemplateManagerServerException If an error occurred while retrieving the template types.
     */
    List<String> listNotificationTemplateTypes(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException;

    /**
     * Delete specified notification template type.
     *
     * @param displayName           Display name of the template type.
     * @param notificationChannel   Notification channel.
     * @param tenantDomain          Tenant domain.
     * @throws NotificationTemplateManagerServerException If an error occurred while deleting the template type.
     */
    void deleteNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException;

    /**
     * Update the notification template if exists or add a new template if not exists.
     *
     * @param notificationTemplate  Notification template.
     * @param applicationUuid       Application UUID.
     * @param tenantDomain          Tenant domain.
     * @throws NotificationTemplateManagerServerException If an error occurred while adding or updating the template.
     */
    void addOrUpdateNotificationTemplate(NotificationTemplate notificationTemplate, String applicationUuid,
                                         String tenantDomain) throws NotificationTemplateManagerServerException;

    /**
     * Check whether the specified notification template exists.
     *
     * @param displayName           Display Name.
     * @param locale                Locale of the template.
     * @param notificationChannel   Notification channel.
     * @param applicationUuid       Application UUID.
     * @param tenantDomain          Tenant domain.
     * @return True if the template exists, false otherwise.
     * @throws NotificationTemplateManagerServerException If an error occurred while checking the existence.
     */
    boolean isNotificationTemplateExists(String displayName, String locale, String notificationChannel,
                                         String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException;

    /**
     * Get specified notification template.
     *
     * @param displayName           Display Name.
     * @param locale                Locale of the template.
     * @param notificationChannel   Notification channel.
     * @param applicationUuid       Application UUID.
     * @param tenantDomain          Tenant domain.
     * @return Notification template.
     * @throws NotificationTemplateManagerServerException If an error occurred while retrieving the template.
     */
    NotificationTemplate getNotificationTemplate(String displayName, String locale, String notificationChannel,
                                                 String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException;

    /**
     * Get the list of notification templates for given template type, channel, application and tenant.
     *
     * @param templateType          Template type.
     * @param notificationChannel   Notification channel.
     * @param applicationUuid       Application UUID.
     * @param tenantDomain          Tenant domain.
     * @return List of available notification templates.
     * @throws NotificationTemplateManagerServerException If an error occurred while retrieving the templates.
     */
    List<NotificationTemplate> listNotificationTemplates(String templateType, String notificationChannel,
                                                         String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException;

    /**
     * Get all notification templates for given channel and tenant.
     *
     * @param notificationChannel   Notification channel.
     * @param tenantDomain          Tenant domain.
     * @return List of available notification templates.
     * @throws NotificationTemplateManagerServerException If an error occurred while retrieving the templates.
     */
    List<NotificationTemplate> listAllNotificationTemplates(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException;

    /**
     * Delete specified notification template.
     *
     * @param displayName           Display Name.
     * @param locale                Locale of the template.
     * @param notificationChannel   Notification channel.
     * @param applicationUuid       Application UUID.
     * @param tenantDomain          Tenant domain.
     * @throws NotificationTemplateManagerServerException If an error occurred while deleting the template.
     */
    void deleteNotificationTemplate(String displayName, String locale, String notificationChannel,
                                    String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException;

    /**
     * Delete all notification templates for given template type, channel, application and tenant.
     *
     * @param displayName           Display Name.
     * @param notificationChannel   Notification channel.
     * @param applicationUuid       Application UUID.
     * @param tenantDomain          Tenant domain.
     * @throws NotificationTemplateManagerServerException If an error occurred while deleting the templates.
     */
    void deleteNotificationTemplates(String displayName, String notificationChannel, String applicationUuid,
                                     String tenantDomain) throws NotificationTemplateManagerServerException;
}
