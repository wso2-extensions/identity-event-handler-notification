/*
 * Copyright (c) 2024-2025, WSO2 LLC. (http://www.wso2.com).
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.identity.core.util.LambdaExceptionUtils;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.organization.application.resource.hierarchy.traverse.service.OrgAppResourceResolverService;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.OrgResourceResolverService;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.exception.OrgResourceHierarchyTraverseException;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.strategy.FirstFoundAggregationStrategy;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.strategy.MergeAllAggregationStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        if (systemDefaultTemplateManager.isNotificationTemplateTypeExists(displayName, notificationChannel,
                        tenantDomain)) {
            return true;
        }

        try {
            OrganizationManager organizationManager = I18nMgtDataHolder.getInstance().getOrganizationManager();
            String organizationId = organizationManager.resolveOrganizationId(tenantDomain);

            OrgResourceResolverService orgResourceResolverService =
                    I18nMgtDataHolder.getInstance().getOrgResourceResolverService();
            Boolean templateTypeExists = orgResourceResolverService.getResourcesFromOrgHierarchy(
                    organizationId,
                    LambdaExceptionUtils.rethrowFunction(
                            orgId -> notificationTemplateTypeExistenceRetriever(displayName, notificationChannel,
                                    orgId)),
                    new FirstFoundAggregationStrategy<>());
            if (templateTypeExists != null) {
                return templateTypeExists;
            }
            return false;
        } catch (OrganizationManagementException | OrgResourceHierarchyTraverseException e) {
            String errorMsg = String.format("Unexpected server error occurred while checking the existence of " +
                    "email template type: %s for tenant: %s", displayName, tenantDomain);
            throw new NotificationTemplateManagerServerException(errorMsg, e);
        }
    }

    @Override
    public List<String> listNotificationTemplateTypes(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        List<String> dbBasedTemplateTypes;
        try {
            OrganizationManager organizationManager = I18nMgtDataHolder.getInstance().getOrganizationManager();
            String organizationId = organizationManager.resolveOrganizationId(tenantDomain);

            OrgResourceResolverService orgResourceResolverService =
                    I18nMgtDataHolder.getInstance().getOrgResourceResolverService();
            dbBasedTemplateTypes = orgResourceResolverService.getResourcesFromOrgHierarchy(
                    organizationId,
                    LambdaExceptionUtils.rethrowFunction(
                            orgId -> notificationTemplateTypesRetriever(notificationChannel, orgId)),
                    new MergeAllAggregationStrategy<>(this::mergeAndRemoveDuplicates));
        } catch (OrganizationManagementException | OrgResourceHierarchyTraverseException e) {
            String errorMsg = String.format(
                    "Unexpected server error occurred while resolving all email templates for tenant: %s",
                    tenantDomain);
            throw new NotificationTemplateManagerServerException(errorMsg, e);
        }

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
    public void deleteAllNotificationTemplates(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (templatePersistenceManager.isNotificationTemplateTypeExists(displayName, notificationChannel,
                tenantDomain)) {
            templatePersistenceManager.deleteAllNotificationTemplates(displayName, notificationChannel, tenantDomain);
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

        if (systemDefaultTemplateManager.isNotificationTemplateExists(displayName, locale, notificationChannel,
                null, tenantDomain)) {
            return true;
        }

        try {
            OrganizationManager organizationManager = I18nMgtDataHolder.getInstance().getOrganizationManager();
            String organizationId = organizationManager.resolveOrganizationId(tenantDomain);

            OrgResourceResolverService orgResourceResolverService =
                    I18nMgtDataHolder.getInstance().getOrgResourceResolverService();
            Boolean templateExists = orgResourceResolverService.getResourcesFromOrgHierarchy(
                    organizationId,
                    LambdaExceptionUtils.rethrowFunction(
                            orgId -> notificationTemplateExistenceRetriever(displayName, locale, notificationChannel,
                                    applicationUuid, orgId)),
                    new FirstFoundAggregationStrategy<>());
            if (templateExists != null) {
                return templateExists;
            }
            return false;
        } catch (OrganizationManagementException | OrgResourceHierarchyTraverseException e) {
            String errorMsg = String.format("Unexpected server error occurred while checking the existence of " +
                    "email template with type: %s for tenant: %s", displayName, tenantDomain);
            if (applicationUuid != null) {
                errorMsg += String.format(" and application id: %s", applicationUuid);
            }
            throw new NotificationTemplateManagerServerException(errorMsg, e);
        }
    }

    @Override
    public NotificationTemplate getNotificationTemplate(String displayName, String locale, String notificationChannel,
                                                        String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        NotificationTemplate notificationTemplate;
        try {
            OrganizationManager organizationManager = I18nMgtDataHolder.getInstance().getOrganizationManager();
            String organizationId = organizationManager.resolveOrganizationId(tenantDomain);

            OrgAppResourceResolverService orgResourceManagementService =
                    I18nMgtDataHolder.getInstance().getOrgAppResourceResolverService();
            notificationTemplate = orgResourceManagementService.getResourcesFromOrgHierarchy(
                    organizationId,
                    applicationUuid,
                    LambdaExceptionUtils.rethrowFunction(
                            (orgId, appId) -> notificationTemplateRetriever(displayName, locale, notificationChannel,
                                    orgId, appId)),
                    new FirstFoundAggregationStrategy<>());
        } catch (OrganizationManagementException | OrgResourceHierarchyTraverseException e) {
            String errorMsg = String.format(
                    "Unexpected server error occurred while resolving email template with type: %s for tenant: %s",
                    displayName, tenantDomain);
            if (applicationUuid != null) {
                errorMsg += String.format(" and application id: %s", applicationUuid);
            }
            throw new NotificationTemplateManagerServerException(errorMsg, e);
        }

        if (notificationTemplate != null) {
            return notificationTemplate;
        } else {
            return systemDefaultTemplateManager.getNotificationTemplate(displayName, locale, notificationChannel,
                    null, tenantDomain);
        }
    }

    @Override
    public List<NotificationTemplate> listNotificationTemplates(String templateType, String notificationChannel,
                                                                String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        List<NotificationTemplate> dbBasedTemplates;
        try {
            OrganizationManager organizationManager = I18nMgtDataHolder.getInstance().getOrganizationManager();
            String organizationId = organizationManager.resolveOrganizationId(tenantDomain);

            OrgAppResourceResolverService orgResourceManagementService =
                    I18nMgtDataHolder.getInstance().getOrgAppResourceResolverService();
            dbBasedTemplates =
                    orgResourceManagementService.getResourcesFromOrgHierarchy(organizationId,
                            applicationUuid,
                            LambdaExceptionUtils.rethrowFunction(
                                    (orgId, appId) -> notificationTemplatesRetriever(templateType, notificationChannel, orgId,
                                            appId)),
                            new MergeAllAggregationStrategy<>(this::mergeAndRemoveDuplicateTemplates));
        } catch (OrganizationManagementException | OrgResourceHierarchyTraverseException e) {
            String errorMsg = String.format(
                    "Unexpected server error occurred while resolving email templates with type: %s for tenant: %s",
                    templateType, tenantDomain);
            if (applicationUuid != null) {
                errorMsg += String.format(" and application id: %s", applicationUuid);
            }
            throw new NotificationTemplateManagerServerException(errorMsg, e);
        }

        List<NotificationTemplate> inMemoryBasedTemplates = new ArrayList<>();
        if (systemDefaultTemplateManager.isNotificationTemplateTypeExists(templateType, notificationChannel,
                tenantDomain)) {
            inMemoryBasedTemplates =
                    systemDefaultTemplateManager.listNotificationTemplates(templateType, notificationChannel,
                            null, tenantDomain);
        }

        return mergeAndRemoveDuplicateTemplates(dbBasedTemplates, inMemoryBasedTemplates);
    }

    @Override
    public List<NotificationTemplate> listAllNotificationTemplates(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        List<NotificationTemplate> dbBasedTemplates;
        try {
            OrganizationManager organizationManager = I18nMgtDataHolder.getInstance().getOrganizationManager();
            String organizationId = organizationManager.resolveOrganizationId(tenantDomain);

            OrgResourceResolverService orgResourceManagementService =
                    I18nMgtDataHolder.getInstance().getOrgResourceResolverService();
            dbBasedTemplates = orgResourceManagementService.getResourcesFromOrgHierarchy(
                    organizationId,
                    LambdaExceptionUtils.rethrowFunction(
                            orgId -> allNotificationTemplatesRetriever(notificationChannel, orgId)),
                    new MergeAllAggregationStrategy<>(this::mergeAndRemoveDuplicateTemplates));
        } catch (OrganizationManagementException | OrgResourceHierarchyTraverseException e) {
            String errorMsg = String.format(
                    "Unexpected server error occurred while resolving all email templates for tenant: %s",
                    tenantDomain);
            throw new NotificationTemplateManagerServerException(errorMsg, e);
        }

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
     * @param primaryTemplates   Primary Templates
     * @param secondaryTemplates Secondary Templates
     * @return Merged list without duplicates.
     */
    private <T> List<T> mergeAndRemoveDuplicates(List<T> primaryTemplates, List<T> secondaryTemplates) {

        if (CollectionUtils.isEmpty(primaryTemplates)) {
            return secondaryTemplates;
        }
        if (CollectionUtils.isEmpty(secondaryTemplates)) {
            return primaryTemplates;
        }

        Set<T> uniqueElements = new HashSet<>();
        uniqueElements.addAll(primaryTemplates);
        uniqueElements.addAll(secondaryTemplates);
        return new ArrayList<>(uniqueElements);
    }

    /**
     * Merges two NotificationTemplate lists and removes duplicate templates.
     *
     * @param primaryTemplates   Primary Templates
     * @param secondaryTemplates Secondary Templates
     * @return Merged list without duplicates.
     */
    private List<NotificationTemplate> mergeAndRemoveDuplicateTemplates(
            List<NotificationTemplate> primaryTemplates,
            List<NotificationTemplate> secondaryTemplates) {

        if (CollectionUtils.isEmpty(primaryTemplates)) {
            return secondaryTemplates;
        }

        if (CollectionUtils.isEmpty(secondaryTemplates)) {
            return primaryTemplates;
        }

        Map<String, NotificationTemplate> templateMap = new HashMap<>();
        primaryTemplates.forEach(template -> templateMap.put(template.getDisplayName(), template));

        // Add secondary templates, only if not already present
        secondaryTemplates.forEach(template -> templateMap.putIfAbsent(template.getDisplayName(), template));
        return new ArrayList<>(templateMap.values());
    }

    private Optional<Boolean> notificationTemplateTypeExistenceRetriever(String displayName, String notificationChannel,
                                                                         String orgId)
            throws NotificationTemplateManagerServerException, OrganizationManagementException {

        OrganizationManager organizationManager = I18nMgtDataHolder.getInstance().getOrganizationManager();
        String tenantDomainOfOrg = organizationManager.resolveTenantDomain(orgId);

        boolean templateTypeExists =
                templatePersistenceManager.isNotificationTemplateTypeExists(displayName, notificationChannel,
                        tenantDomainOfOrg);
        if (!templateTypeExists) {
            return Optional.empty();
        }
        return Optional.of(true);
    }

    private Optional<Boolean> notificationTemplateExistenceRetriever(String displayName, String locale,
                                                                     String notificationChannel, String appId,
                                                                     String orgId)
            throws NotificationTemplateManagerServerException, OrganizationManagementException {

        OrganizationManager organizationManager = I18nMgtDataHolder.getInstance().getOrganizationManager();
        String tenantDomainOfOrg = organizationManager.resolveTenantDomain(orgId);

        boolean templateExists =
                templatePersistenceManager.isNotificationTemplateExists(displayName, locale, notificationChannel, appId,
                        tenantDomainOfOrg);
        if (!templateExists) {
            return Optional.empty();
        }
        return Optional.of(true);
    }

    private Optional<List<String>> notificationTemplateTypesRetriever(String notificationChannel, String orgId)
            throws NotificationTemplateManagerServerException, OrganizationManagementException {

        OrganizationManager organizationManager = I18nMgtDataHolder.getInstance().getOrganizationManager();
        String tenantDomainOfOrg = organizationManager.resolveTenantDomain(orgId);
        List<String> notificationTemplates =
                templatePersistenceManager.listNotificationTemplateTypes(notificationChannel, tenantDomainOfOrg);
        return Optional.ofNullable(notificationTemplates);
    }

    private Optional<NotificationTemplate> notificationTemplateRetriever(String displayName, String locale,
                                                                         String notificationChannel, String orgId,
                                                                         String appId)
            throws OrganizationManagementException, NotificationTemplateManagerServerException {

        OrganizationManager organizationManager = I18nMgtDataHolder.getInstance().getOrganizationManager();
        String tenantDomainOfOrg = organizationManager.resolveTenantDomain(orgId);
        NotificationTemplate template =
                templatePersistenceManager.getNotificationTemplate(displayName, locale, notificationChannel, appId,
                        tenantDomainOfOrg);
        if (template == null && StringUtils.isNotBlank(appId)) {
            template = templatePersistenceManager.getNotificationTemplate(displayName, locale, notificationChannel,
                    null, tenantDomainOfOrg);
        }
        return Optional.ofNullable(template);
    }

    private Optional<List<NotificationTemplate>> notificationTemplatesRetriever(String templateType,
                                                                                String notificationChannel,
                                                                                String orgId, String appId)
            throws OrganizationManagementException, NotificationTemplateManagerServerException {

        OrganizationManager organizationManager = I18nMgtDataHolder.getInstance().getOrganizationManager();
        String tenantDomainOfOrg = organizationManager.resolveTenantDomain(orgId);
        if (templatePersistenceManager.isNotificationTemplateTypeExists(templateType,
                notificationChannel, tenantDomainOfOrg)) {
            List<NotificationTemplate> notificationTemplates =
                    templatePersistenceManager.listNotificationTemplates(templateType, notificationChannel, appId,
                            tenantDomainOfOrg);
            if (StringUtils.isNotBlank(appId)) {
                List<NotificationTemplate> orgNotificationTemplates =
                        templatePersistenceManager.listNotificationTemplates(templateType, notificationChannel, null,
                                tenantDomainOfOrg);
                notificationTemplates =
                        mergeAndRemoveDuplicateTemplates(notificationTemplates, orgNotificationTemplates);
            }
            return Optional.ofNullable(notificationTemplates);
        }
        return Optional.empty();
    }

    private Optional<List<NotificationTemplate>> allNotificationTemplatesRetriever(String notificationChannel,
                                                                                   String orgId)
            throws OrganizationManagementException, NotificationTemplateManagerServerException {

        OrganizationManager organizationManager = I18nMgtDataHolder.getInstance().getOrganizationManager();
        String tenantDomainOfOrg = organizationManager.resolveTenantDomain(orgId);
        List<NotificationTemplate> notificationTemplates =
                templatePersistenceManager.listAllNotificationTemplates(notificationChannel, tenantDomainOfOrg);
        return Optional.ofNullable(notificationTemplates);
    }
}
