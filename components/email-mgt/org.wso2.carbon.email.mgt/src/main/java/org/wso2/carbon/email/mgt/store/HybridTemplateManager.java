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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is responsible for on-demand migrating notification templates from the registry to the database.
 *
 * Any new notification templates will be stored in database by using {@link DBBasedTemplateManager} while reading any
 * existing templates by using both {@link DBBasedTemplateManager} & {@link RegistryBasedTemplateManager}.
 */
public class HybridTemplateManager implements TemplatePersistenceManager {

    private static final Log log = LogFactory.getLog(HybridTemplateManager.class);

    private TemplatePersistenceManager dbBasedTemplateManager = new DBBasedTemplateManager();
    private TemplatePersistenceManager registryBasedTemplateManager = new RegistryBasedTemplateManager();

    @Override
    public void addNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        dbBasedTemplateManager.addNotificationTemplateType(displayName, notificationChannel, tenantDomain);
    }

    @Override
    public boolean isNotificationTemplateTypeExists(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        return dbBasedTemplateManager.isNotificationTemplateTypeExists(displayName, notificationChannel,
                tenantDomain) ||
                registryBasedTemplateManager.isNotificationTemplateTypeExists(displayName, notificationChannel,
                        tenantDomain);
    }

    @Override
    public List<String> listNotificationTemplateTypes(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        List<String> dbBasedTemplateTypes = dbBasedTemplateManager.listNotificationTemplateTypes(notificationChannel,
                tenantDomain);
        List<String> registryBasedTemplateTypes = registryBasedTemplateManager.listNotificationTemplateTypes(notificationChannel,
                tenantDomain);

        return mergeAndRemoveDuplicates(dbBasedTemplateTypes, registryBasedTemplateTypes);
    }

    @Override
    public void deleteNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (dbBasedTemplateManager.isNotificationTemplateTypeExists(displayName, notificationChannel, tenantDomain)) {
            dbBasedTemplateManager.deleteNotificationTemplateType(displayName, notificationChannel, tenantDomain);
        }

        if (registryBasedTemplateManager.isNotificationTemplateTypeExists(displayName, notificationChannel,
                tenantDomain)) {
            registryBasedTemplateManager.deleteNotificationTemplateType(displayName, notificationChannel, tenantDomain);
        }
    }

    @Override
    public void addOrUpdateNotificationTemplate(NotificationTemplate notificationTemplate, String applicationUuid,
                                                String tenantDomain) throws NotificationTemplateManagerServerException {

        dbBasedTemplateManager.addOrUpdateNotificationTemplate(notificationTemplate, applicationUuid, tenantDomain);

        String displayName = notificationTemplate.getDisplayName();
        String locale = notificationTemplate.getLocale();
        String notificationChannel = notificationTemplate.getNotificationChannel();

        if (registryBasedTemplateManager.isNotificationTemplateExists(displayName, locale, notificationChannel,
                applicationUuid, tenantDomain)) {

//            registryBasedTemplateManager.deleteNotificationTemplate(displayName, locale, notificationChannel,
//                    applicationUuid, tenantDomain);
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Moved %s template: %s for locale: %s in tenant: %s from registry to the database.",
                        notificationChannel, displayName, locale, tenantDomain));
            }
        }
    }

    @Override
    public boolean isNotificationTemplateExists(String displayName, String locale, String notificationChannel,
                                                String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        return dbBasedTemplateManager.isNotificationTemplateExists(displayName, locale, notificationChannel,
                applicationUuid, tenantDomain) ||
                registryBasedTemplateManager.isNotificationTemplateExists(displayName, locale, notificationChannel,
                        applicationUuid, tenantDomain);
    }

    @Override
    public NotificationTemplate getNotificationTemplate(String displayName, String locale, String notificationChannel,
                                                        String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (dbBasedTemplateManager.isNotificationTemplateExists(displayName, locale, notificationChannel,
                applicationUuid, tenantDomain)) {
            return dbBasedTemplateManager.getNotificationTemplate(displayName, locale, notificationChannel,
                    applicationUuid, tenantDomain);
        } else {
            return registryBasedTemplateManager.getNotificationTemplate(displayName, locale, notificationChannel,
                    applicationUuid, tenantDomain);
        }
    }

    @Override
    public List<NotificationTemplate> listNotificationTemplates(String templateType, String notificationChannel,
                                                                String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        List<NotificationTemplate> dbBasedTemplates = new ArrayList<>();
        if (dbBasedTemplateManager.isNotificationTemplateTypeExists(templateType, notificationChannel,
                tenantDomain)) {
            dbBasedTemplates =
                    dbBasedTemplateManager.listNotificationTemplates(templateType, notificationChannel, applicationUuid,
                            tenantDomain);
        }

        List<NotificationTemplate> registryBasedTemplates = new ArrayList<>();
        if (registryBasedTemplateManager.isNotificationTemplateTypeExists(templateType, notificationChannel,
                tenantDomain)) {
            registryBasedTemplates =
                    registryBasedTemplateManager.listNotificationTemplates(templateType, notificationChannel,
                            applicationUuid, tenantDomain);
        }

        return mergeAndRemoveDuplicates(dbBasedTemplates, registryBasedTemplates);
    }

    @Override
    public List<NotificationTemplate> listAllNotificationTemplates(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        List<NotificationTemplate> dbBasedTemplates =
                dbBasedTemplateManager.listAllNotificationTemplates(notificationChannel, tenantDomain);
        List<NotificationTemplate> registryBasedTemplates =
                registryBasedTemplateManager.listAllNotificationTemplates(notificationChannel, tenantDomain);

        return mergeAndRemoveDuplicates(dbBasedTemplates, registryBasedTemplates);
    }

    @Override
    public void deleteNotificationTemplate(String displayName, String locale, String notificationChannel,
                                           String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (dbBasedTemplateManager.isNotificationTemplateExists(displayName, locale, notificationChannel,
                applicationUuid, tenantDomain)) {
            dbBasedTemplateManager.deleteNotificationTemplate(displayName, locale, notificationChannel, applicationUuid,
                    tenantDomain);
        } else {
            registryBasedTemplateManager.deleteNotificationTemplate(displayName, locale, notificationChannel,
                    applicationUuid, tenantDomain);
        }
    }

    @Override
    public void deleteNotificationTemplates(String displayName, String notificationChannel, String applicationUuid,
                                            String tenantDomain) throws NotificationTemplateManagerServerException {

        if (dbBasedTemplateManager.isNotificationTemplateTypeExists(displayName, notificationChannel, tenantDomain)) {
            dbBasedTemplateManager.deleteNotificationTemplates(displayName, notificationChannel, applicationUuid,
                    tenantDomain);
        }

        if (registryBasedTemplateManager.isNotificationTemplateTypeExists(displayName, notificationChannel,
                tenantDomain)) {
            registryBasedTemplateManager.deleteNotificationTemplates(displayName, notificationChannel, applicationUuid,
                    tenantDomain);
        }
    }

    /**
     * Merges two lists and removes duplicates.
     *
     * @param list1
     * @param list2
     * @return Merged list without duplicates.
     */
    private <T> List<T> mergeAndRemoveDuplicates(List<T> list1, List<T> list2) {

        Set<T> uniqueElements = new HashSet<>();
        uniqueElements.addAll(list1);
        uniqueElements.addAll(list2);
        return new ArrayList<>(uniqueElements);
    }
}
