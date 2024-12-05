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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.core.ThreadLocalAwareExecutors;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * This class is responsible for on-demand migrating notification templates from the registry to the database.
 *
 * Any new notification templates will be stored in database by using {@link DBBasedTemplateManager} while reading any
 * existing templates by using both {@link DBBasedTemplateManager} & {@link RegistryBasedTemplateManager}.
 */
public class HybridTemplateManager implements TemplatePersistenceManager {

    private static final Log log = LogFactory.getLog(HybridTemplateManager.class);
    private final ExecutorService executorService = ThreadLocalAwareExecutors.newFixedThreadPool(1);

    private TemplatePersistenceManager dbBasedTemplateManager = new DBBasedTemplateManager();
    private TemplatePersistenceManager registryBasedTemplateManager = new RegistryBasedTemplateManager();
    private List<String> legacyTenants = I18nMgtDataHolder.getInstance().getLegacyTenants();

    @Override
    public void addNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (legacyTenants.contains(tenantDomain)) {
            log.info(String.format("Adding %s template type for channel: %s to the registry for legacy tenant %s.",
                    displayName, notificationChannel, tenantDomain));
            registryBasedTemplateManager.addNotificationTemplateType(displayName, notificationChannel, tenantDomain);
            return;
        }

        dbBasedTemplateManager.addNotificationTemplateType(displayName, notificationChannel, tenantDomain);
    }

    @Override
    public boolean isNotificationTemplateTypeExists(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (legacyTenants.contains(tenantDomain)) {
            return registryBasedTemplateManager.isNotificationTemplateTypeExists(displayName, notificationChannel,
                            tenantDomain);
        }

        return dbBasedTemplateManager.isNotificationTemplateTypeExists(displayName, notificationChannel,
                tenantDomain) ||
                registryBasedTemplateManager.isNotificationTemplateTypeExists(displayName, notificationChannel,
                        tenantDomain);
    }

    @Override
    public List<String> listNotificationTemplateTypes(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (legacyTenants.contains(tenantDomain)) {
            return registryBasedTemplateManager.listNotificationTemplateTypes(notificationChannel, tenantDomain);
        }

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
    public void deleteAllNotificationTemplates(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (dbBasedTemplateManager.isNotificationTemplateTypeExists(displayName, notificationChannel, tenantDomain)) {
            dbBasedTemplateManager.deleteAllNotificationTemplates(displayName, notificationChannel, tenantDomain);
        }

        if (registryBasedTemplateManager.isNotificationTemplateTypeExists(displayName, notificationChannel,
                tenantDomain)) {
            registryBasedTemplateManager.deleteAllNotificationTemplates(displayName, notificationChannel, tenantDomain);
        }
    }

    @Override
    public void addOrUpdateNotificationTemplate(NotificationTemplate notificationTemplate, String applicationUuid,
                                                String tenantDomain) throws NotificationTemplateManagerServerException {

        if (legacyTenants.contains(tenantDomain)) {
            log.info(String.format("Adding %s template: %s for locale: %s in tenant: %s to the registry for legacy tenant.",
                    notificationTemplate.getNotificationChannel(), notificationTemplate.getDisplayName(),
                    notificationTemplate.getLocale(), tenantDomain));
            registryBasedTemplateManager.addOrUpdateNotificationTemplate(notificationTemplate, applicationUuid,
                    tenantDomain);
            return;
        }

        dbBasedTemplateManager.addOrUpdateNotificationTemplate(notificationTemplate, applicationUuid, tenantDomain);

        String displayName = notificationTemplate.getDisplayName();
        String locale = notificationTemplate.getLocale();
        String notificationChannel = notificationTemplate.getNotificationChannel();

        if (registryBasedTemplateManager.isNotificationTemplateExists(displayName, locale, notificationChannel,
                applicationUuid, tenantDomain)) {
            NotificationTemplate registryBasedTemplate = registryBasedTemplateManager.getNotificationTemplate(displayName,
                    locale, notificationChannel, applicationUuid, tenantDomain);

//            registryBasedTemplateManager.deleteNotificationTemplate(displayName, locale, notificationChannel,
//                    applicationUuid, tenantDomain);
            log.info(String.format("Copied %s template: %s for locale: %s in tenant: %s from registry to the database.",
                    notificationChannel, displayName, locale, tenantDomain));

            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Moved %s template: %s for locale: %s in tenant: %s from registry to the database.",
                        notificationChannel, displayName, locale, tenantDomain));
            }

            String usernameInContext = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            CompletableFuture.runAsync(
                    () -> migrateChildOrgTemplates(registryBasedTemplate, tenantDomain, applicationUuid,
                            usernameInContext),
                    executorService);
        }
    }

    @Override
    public boolean isNotificationTemplateExists(String displayName, String locale, String notificationChannel,
                                                String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (legacyTenants.contains(tenantDomain)) {
            return registryBasedTemplateManager.isNotificationTemplateExists(displayName, locale, notificationChannel,
                            applicationUuid, tenantDomain);
        }

        return dbBasedTemplateManager.isNotificationTemplateExists(displayName, locale, notificationChannel,
                applicationUuid, tenantDomain) ||
                registryBasedTemplateManager.isNotificationTemplateExists(displayName, locale, notificationChannel,
                        applicationUuid, tenantDomain);
    }

    @Override
    public NotificationTemplate getNotificationTemplate(String displayName, String locale, String notificationChannel,
                                                        String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (legacyTenants.contains(tenantDomain)) {
            return registryBasedTemplateManager.getNotificationTemplate(displayName, locale, notificationChannel,
                            applicationUuid, tenantDomain);
        }

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

        if (legacyTenants.contains(tenantDomain)) {
            return registryBasedTemplateManager.listNotificationTemplates(templateType, notificationChannel,
                            applicationUuid, tenantDomain);
        }

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

        if (legacyTenants.contains(tenantDomain)) {
            return registryBasedTemplateManager.listAllNotificationTemplates(notificationChannel, tenantDomain);
        }

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

    private void migrateChildOrgTemplates(NotificationTemplate parentTemplate, String parentTenantDomain,
                                          String parentAppId, String usernameInContext) {

        String cursor = null;
        int pageSize = 100;
        try {
            String displayName = parentTemplate.getDisplayName();
            String locale = parentTemplate.getLocale();
            String notificationChannel = parentTemplate.getNotificationChannel();

            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(parentTenantDomain, true);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(usernameInContext);

            OrganizationManager organizationManager = I18nMgtDataHolder.getInstance().getOrganizationManager();
            ApplicationManagementService applicationManagementService =
                    I18nMgtDataHolder.getInstance().getApplicationManagementService();
            do {
                try {
                    List<BasicOrganization> childOrganizations =
                            organizationManager.getOrganizations(pageSize, cursor, null, "DESC", "", false);
                    Map<String, String> childAppIds = new HashMap<>();
                    if (StringUtils.isNotBlank(parentAppId)) {
                        childAppIds = applicationManagementService.getChildAppIds(parentAppId, parentTenantDomain,
                                childOrganizations.stream().map(BasicOrganization::getId).collect(Collectors.toList()));
                        if (childAppIds.isEmpty()) {
                            continue;
                        }
                    }
                    for (BasicOrganization childOrganization : childOrganizations) {
                        String childTenantDomain = organizationManager.resolveTenantDomain(childOrganization.getId());
                        String childAppId = childAppIds.get(childTenantDomain);

                        if (StringUtils.isNotBlank(parentAppId) && StringUtils.isBlank(childAppId)) {
                            continue;
                        }

                        NotificationTemplate childNotificationTemplate =
                                registryBasedTemplateManager.getNotificationTemplate(
                                        displayName, locale, notificationChannel, childAppId, childTenantDomain);
                        if (childNotificationTemplate != null) {
                            if (!areNotificationTemplatesEqual(parentTemplate, childNotificationTemplate) &&
                                    !dbBasedTemplateManager.isNotificationTemplateExists(displayName, locale,
                                            notificationChannel, childAppId, childTenantDomain)) {
                                dbBasedTemplateManager.addOrUpdateNotificationTemplate(childNotificationTemplate,
                                        childAppId, childTenantDomain);
                            }

                            registryBasedTemplateManager.deleteNotificationTemplates(displayName, notificationChannel,
                                    childAppId, childTenantDomain);
                            migrateChildOrgTemplates(childNotificationTemplate, childTenantDomain, childAppId,
                                    usernameInContext);
                        }
                    }
                    cursor = childOrganizations.isEmpty() ? null : Base64.getEncoder().encodeToString(
                            childOrganizations.get(childOrganizations.size() - 1).getCreated()
                                    .getBytes(StandardCharsets.UTF_8));
                } catch (OrganizationManagementException e) {
                    log.error("Error occurred while retrieving child organizations of organization " +
                            "with tenant domain: " + parentTenantDomain, e);
                } catch (IdentityApplicationManagementException e){
                    log.error("Error occurred while retrieving child app ids of application with id: " +
                            parentAppId + " and tenant domain: " + parentTenantDomain, e);
                } catch (NotificationTemplateManagerServerException e) {
                    log.error("Error occurred while migrating child organization templates of parent template: " +
                            displayName + " for locale: " + locale + " and notification channel: " +
                            notificationChannel, e);
                }
            } while (cursor != null);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    private boolean areNotificationTemplatesEqual(NotificationTemplate template1, NotificationTemplate template2) {

        return StringUtils.equals(template1.getDisplayName(), template2.getDisplayName()) &&
                StringUtils.equals(template1.getLocale(), template2.getLocale()) &&
                StringUtils.equals(template1.getNotificationChannel(), template2.getNotificationChannel()) &&
                StringUtils.equals(template1.getContentType(), template2.getContentType()) &&
                StringUtils.equals(template1.getBody(), template2.getBody()) &&
                StringUtils.equals(template1.getSubject(), template2.getSubject()) &&
                StringUtils.equals(template1.getFooter(), template2.getFooter());
    }
}
