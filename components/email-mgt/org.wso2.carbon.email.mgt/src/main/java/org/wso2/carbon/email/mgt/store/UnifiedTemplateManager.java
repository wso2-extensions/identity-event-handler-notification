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

package org.wso2.carbon.email.mgt.store;

import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class serves as a unified template management system that delegates the template persistence operations
 * to both template persistent manger crafted from the factory  and an in-memory manager.
 * This class will function as a wrapper class for the template manager produced from the factory.
 */
public class UnifiedTemplateManager implements TemplatePersistenceManager {

    private final TemplatePersistenceManager templatePersistenceManager;
    private final SystemDefaultTemplateManager systemDefaultTemplateManager = new SystemDefaultTemplateManager();

    public UnifiedTemplateManager(TemplatePersistenceManager persistenceManager) {

        this.templatePersistenceManager = persistenceManager;
    }

    @Override
    public void addNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        templatePersistenceManager.addNotificationTemplateType(displayName, notificationChannel, tenantDomain);
    }

    @Override
    public boolean isNotificationTemplateTypeExists(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        return systemDefaultTemplateManager.isNotificationTemplateTypeExists(displayName, notificationChannel,
                tenantDomain) ||
                templatePersistenceManager.isNotificationTemplateTypeExists(displayName, notificationChannel,
                        tenantDomain);
    }

    @Override
    public List<String> listNotificationTemplateTypes(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        List<String> dbBasedTemplateTypes = templatePersistenceManager.listNotificationTemplateTypes
                (notificationChannel, tenantDomain);
        List<String> inMemoryTemplateTypes =
                systemDefaultTemplateManager.listNotificationTemplateTypes(notificationChannel, tenantDomain);

        return mergeAndRemoveDuplicates(dbBasedTemplateTypes, inMemoryTemplateTypes);
    }

    @Override
    public void deleteNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (templatePersistenceManager.isNotificationTemplateTypeExists(displayName, notificationChannel,
                tenantDomain)) {
            templatePersistenceManager.deleteNotificationTemplateType(displayName, notificationChannel, tenantDomain);
        }
    }

    @Override
    public void addOrUpdateNotificationTemplate(NotificationTemplate notificationTemplate, String applicationUuid,
                                                String tenantDomain) throws NotificationTemplateManagerServerException {

        if (!systemDefaultTemplateManager.hasSameTemplate(notificationTemplate)) {
            templatePersistenceManager.addOrUpdateNotificationTemplate(notificationTemplate, applicationUuid,
                    tenantDomain);
        } else {
            // Template is already managed as a system default template. Handle add or update.
            String displayName = notificationTemplate.getDisplayName();
            String locale = notificationTemplate.getLocale();
            String notificationChannel = notificationTemplate.getNotificationChannel();
            boolean isExistsInStorage =
                    templatePersistenceManager.isNotificationTemplateExists(displayName, locale, notificationChannel,
                            applicationUuid, tenantDomain);
            if (isExistsInStorage) {
                // This request is to reset existing template to default content. Hence, delete the existing template.
                templatePersistenceManager.deleteNotificationTemplate(displayName, locale, notificationChannel,
                        applicationUuid, tenantDomain);
            } else {
                // This request is to add a new template with a same content that is already managed as a system default
                // template. Storing such templates is redundant. Hence, avoid storing those templates as duplicate
                // contents to optimize the storage.
            }
        }
    }

    @Override
    public boolean isNotificationTemplateExists(String displayName, String locale, String notificationChannel,
                                                String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        return systemDefaultTemplateManager.isNotificationTemplateExists(displayName, locale, notificationChannel,
                applicationUuid, tenantDomain) ||
                templatePersistenceManager.isNotificationTemplateExists(displayName, locale, notificationChannel,
                        applicationUuid, tenantDomain);
    }

    @Override
    public NotificationTemplate getNotificationTemplate(String displayName, String locale, String notificationChannel,
                                                        String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        NotificationTemplate notificationTemplate = templatePersistenceManager.getNotificationTemplate(displayName,
                locale, notificationChannel, applicationUuid, tenantDomain);
        if (notificationTemplate != null) {
            return notificationTemplate;
        } else {
            return systemDefaultTemplateManager.getNotificationTemplate(displayName, locale, notificationChannel,
                    applicationUuid, tenantDomain);
        }
    }

    @Override
    public List<NotificationTemplate> listNotificationTemplates(String templateType, String notificationChannel,
                                                                String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        List<NotificationTemplate> dbBasedTemplates = new ArrayList<>();
        if (templatePersistenceManager.isNotificationTemplateTypeExists(templateType, notificationChannel,
                tenantDomain)) {
            dbBasedTemplates =
                    templatePersistenceManager.listNotificationTemplates(templateType, notificationChannel,
                            applicationUuid, tenantDomain);
        }

        List<NotificationTemplate> inMemoryBasedTemplates = new ArrayList<>();
        if (systemDefaultTemplateManager.isNotificationTemplateTypeExists(templateType, notificationChannel,
                tenantDomain)) {
            inMemoryBasedTemplates =
                    systemDefaultTemplateManager.listNotificationTemplates(templateType, notificationChannel,
                            applicationUuid, tenantDomain);
        }

        return mergeAndRemoveDuplicateTemplates(dbBasedTemplates, inMemoryBasedTemplates);
    }

    @Override
    public List<NotificationTemplate> listAllNotificationTemplates(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        List<NotificationTemplate> dbBasedTemplates =
                templatePersistenceManager.listAllNotificationTemplates(notificationChannel, tenantDomain);
        List<NotificationTemplate> inMemoryBasedTemplates =
                systemDefaultTemplateManager.listAllNotificationTemplates(notificationChannel, tenantDomain);

        return mergeAndRemoveDuplicateTemplates(dbBasedTemplates, inMemoryBasedTemplates);
    }

    @Override
    public void deleteNotificationTemplate(String displayName, String locale, String notificationChannel,
                                           String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (templatePersistenceManager.isNotificationTemplateExists(displayName, locale, notificationChannel,
                applicationUuid, tenantDomain)) {
            templatePersistenceManager.deleteNotificationTemplate(displayName, locale, notificationChannel,
                    applicationUuid, tenantDomain);
        }
    }

    @Override
    public void deleteNotificationTemplates(String displayName, String notificationChannel, String applicationUuid,
                                            String tenantDomain) throws NotificationTemplateManagerServerException {

        if (templatePersistenceManager.isNotificationTemplateTypeExists(displayName, notificationChannel,
                tenantDomain)) {
            templatePersistenceManager.deleteNotificationTemplates(displayName, notificationChannel, applicationUuid,
                    tenantDomain);
        }
    }

    /**
     * Merges two lists and removes duplicates.
     *
     * @param dbBasedTemplates DbBasedTemplates
     * @param inMemoryTemplates InMemoryTemplates
     * @return Merged list without duplicates.
     */
    private <T> List<T> mergeAndRemoveDuplicates(List<T> dbBasedTemplates, List<T> inMemoryTemplates) {

        Set<T> uniqueElements = new HashSet<>();
        uniqueElements.addAll(dbBasedTemplates);
        uniqueElements.addAll(inMemoryTemplates);
        return new ArrayList<>(uniqueElements);
    }

    /**
     * Merges two NotificationTemplate lists and removes duplicate templates.
     *
     * @param dbBasedTemplates DbBasedTemplates
     * @param inMemoryTemplates InMemoryTemplates
     * @return Merged list without duplicates.
     */
    private List<NotificationTemplate> mergeAndRemoveDuplicateTemplates(
            List<NotificationTemplate> dbBasedTemplates,
            List<NotificationTemplate> inMemoryTemplates) {

        Map<String, NotificationTemplate> templateMap = new HashMap<>();
        dbBasedTemplates.forEach(template -> templateMap.put(template.getDisplayName(), template));

        // Add in-memory templates, only if not already present
        inMemoryTemplates.forEach(template -> templateMap.putIfAbsent(template.getDisplayName(), template));
        return new ArrayList<>(templateMap.values());
    }
}
