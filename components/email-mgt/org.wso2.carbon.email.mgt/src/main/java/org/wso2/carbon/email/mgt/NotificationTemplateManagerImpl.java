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

package org.wso2.carbon.email.mgt;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.email.mgt.constants.TemplateMgtConstants;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.email.mgt.store.TemplatePersistenceManager;
import org.wso2.carbon.email.mgt.store.TemplatePersistenceManagerFactory;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementServerException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerClientException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerInternalException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.identity.governance.service.notification.NotificationTemplateManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;

import java.util.ArrayList;
import java.util.List;

import static org.wso2.carbon.email.mgt.constants.TemplateMgtConstants.DEFAULT_EMAIL_NOTIFICATION_LOCALE;
import static org.wso2.carbon.email.mgt.constants.TemplateMgtConstants.DEFAULT_SMS_NOTIFICATION_LOCALE;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.normalizeLocaleFormat;

/**
 * Provides functionality to manage notification templates.
 */
public class NotificationTemplateManagerImpl implements NotificationTemplateManager {

    private static final Log log = LogFactory.getLog(NotificationTemplateManagerImpl.class);

    private final TemplatePersistenceManager userDefinedTemplatePersistenceManager;
    private final TemplatePersistenceManager systemTemplatePersistenceManager;
    private final TemplatePersistenceManager unifiedTemplatePersistenceManager;

    public NotificationTemplateManagerImpl() {

        TemplatePersistenceManagerFactory templatePersistenceManagerFactory = new TemplatePersistenceManagerFactory();
        this.userDefinedTemplatePersistenceManager =
                templatePersistenceManagerFactory.getUserDefinedTemplatePersistenceManager();
        this.systemTemplatePersistenceManager = templatePersistenceManagerFactory.getSystemTemplatePersistenceManager();
        this.unifiedTemplatePersistenceManager = templatePersistenceManagerFactory.getTemplatePersistenceManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
     public List<String> getAllNotificationTemplateTypes(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

         try {
             if (OrganizationManagementUtil.isOrganization(tenantDomain)) {
                 // Return the root organization's template types.
                 tenantDomain = getRootOrgTenantDomain(tenantDomain);
             }
             List<String> templateTypes = unifiedTemplatePersistenceManager
                     .listNotificationTemplateTypes(notificationChannel, tenantDomain);
             return templateTypes != null ? templateTypes : new ArrayList<>();
         } catch (NotificationTemplateManagerServerException ex) {
             String errorMsg = String.format("Error when retrieving %s template types of %s tenant.",
                     notificationChannel.toLowerCase(), tenantDomain);
             throw new NotificationTemplateManagerServerException(errorMsg, ex);
         } catch (OrganizationManagementException e) {
             throw new NotificationTemplateManagerServerException(e.getMessage(), e);
         }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addNotificationTemplateType(String notificationChannel, String displayName, String tenantDomain)
            throws NotificationTemplateManagerException {

        validateDisplayNameOfTemplateType(displayName);
        try {
            if (unifiedTemplatePersistenceManager
                    .isNotificationTemplateTypeExists(displayName, notificationChannel, tenantDomain)) {
                // This error is caught in the catch block below to generate the
                // NotificationTemplateManagerServerException.
                throw new NotificationTemplateManagerInternalException(
                        TemplateMgtConstants.ErrorCodes.TEMPLATE_TYPE_ALREADY_EXISTS, StringUtils.EMPTY);
            }
            unifiedTemplatePersistenceManager.addNotificationTemplateType(displayName, notificationChannel,
                    tenantDomain);
        } catch (NotificationTemplateManagerServerException e) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_TEMPLATE.getCode(),
                    TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
            String message = String.format(
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_TEMPLATE.getMessage(), displayName,
                    tenantDomain);
            throw new NotificationTemplateManagerException(code, message, e);
        } catch (NotificationTemplateManagerInternalException e) {
            if (TemplateMgtConstants.ErrorCodes.TEMPLATE_TYPE_ALREADY_EXISTS.equals(e.getErrorCode())) {
                String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                        TemplateMgtConstants.ErrorMessages.ERROR_CODE_TEMPLATE_TYPE_ALREADY_EXISTS.getCode(),
                        TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
                String message = String.format(
                        TemplateMgtConstants.ErrorMessages.ERROR_CODE_TEMPLATE_TYPE_ALREADY_EXISTS.getMessage(),
                        displayName, tenantDomain);
                throw new NotificationTemplateManagerServerException(code, message, e);
            }
            if (log.isDebugEnabled()) {
                log.debug("Error when adding template type : " + displayName + " to tenant : " + tenantDomain, e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteNotificationTemplateType(String notificationChannel, String templateDisplayName,
                                               String tenantDomain)
            throws NotificationTemplateManagerException {

        validateDisplayNameOfTemplateType(templateDisplayName);
        assertTemplateTypeExists(templateDisplayName, notificationChannel, tenantDomain);

        // System template types are cannot be deleted since these are hard coded values.
        if (systemTemplatePersistenceManager.isNotificationTemplateTypeExists(templateDisplayName, notificationChannel,
                null)) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_SYSTEM_RESOURCE_DELETION_NOT_ALLOWED.getCode(),
                    TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
            String message = String.format(
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_SYSTEM_RESOURCE_DELETION_NOT_ALLOWED.getMessage(),
                    "System template types are not eligible for deletion.");
            throw new NotificationTemplateManagerServerException(code, message);
        }

        try {
            unifiedTemplatePersistenceManager.deleteNotificationTemplateType(templateDisplayName,
                    notificationChannel, tenantDomain);
        } catch (NotificationTemplateManagerException ex) {
            String errorMsg = String.format
                    ("Error deleting template type %s from %s tenant.", templateDisplayName, tenantDomain);
            throw handleServerException(errorMsg, ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNotificationTemplateTypeExists(String notificationChannel, String templateTypeDisplayName,
                                                  String tenantDomain)
            throws NotificationTemplateManagerException {

        try {
            return unifiedTemplatePersistenceManager.isNotificationTemplateTypeExists(templateTypeDisplayName,
                    notificationChannel, tenantDomain);
        } catch (NotificationTemplateManagerServerException e) {
            String error = String.format("Error when retrieving templates of %s tenant.", tenantDomain);
            throw handleServerException(error, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NotificationTemplate> getAllNotificationTemplates(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        try {
            if (OrganizationManagementUtil.isOrganization(tenantDomain)) {
                // Return the root organization's email templates.
                tenantDomain = getRootOrgTenantDomain(tenantDomain);
            }
            return userDefinedTemplatePersistenceManager.listAllNotificationTemplates(
                    notificationChannel, tenantDomain);
        } catch (NotificationTemplateManagerServerException e) {
            String error = String.format("Error when retrieving templates of %s tenant.", tenantDomain);
            throw handleServerException(error, e);
        } catch (OrganizationManagementException e) {
            throw handleServerException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NotificationTemplate> getNotificationTemplatesOfType(String notificationChannel,
                                                                     String templateDisplayName, String tenantDomain)
            throws NotificationTemplateManagerException {

        return getNotificationTemplatesOfType(notificationChannel, templateDisplayName, tenantDomain, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NotificationTemplate> getNotificationTemplatesOfType(String notificationChannel,
                                              String templateDisplayName, String tenantDomain, String applicationUuid)
            throws NotificationTemplateManagerException {

        try {
            if (OrganizationManagementUtil.isOrganization(tenantDomain)) {
                // Return the root organization's email templates.
                tenantDomain = getRootOrgTenantDomain(tenantDomain);
            }
            assertTemplateTypeExists(templateDisplayName, notificationChannel, tenantDomain);
            List<NotificationTemplate> notificationTemplates =
                    userDefinedTemplatePersistenceManager.listNotificationTemplates(templateDisplayName,
                            notificationChannel, applicationUuid, tenantDomain);
            if (notificationTemplates == null) {
                notificationTemplates = new ArrayList<>();
            }
            return notificationTemplates;
        } catch (OrganizationManagementException e) {
            throw handleServerException(e.getMessage(), e);
        }
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public NotificationTemplate getNotificationTemplate(String notificationChannel, String templateType, String locale,
                                                         String tenantDomain)
            throws NotificationTemplateManagerException {

        return getNotificationTemplate(notificationChannel, templateType, locale, tenantDomain, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NotificationTemplate getNotificationTemplate(String notificationChannel, String templateType, String locale,
                                                         String tenantDomain, String applicationUuid)
            throws NotificationTemplateManagerException {

        try {
            if (OrganizationManagementUtil.isOrganization(tenantDomain)) {
                // To return the root organization's notification template.
                tenantDomain = getRootOrgTenantDomain(tenantDomain);
                // If it's application specific template is required, get the root organization's application.
                if (StringUtils.isNotBlank(applicationUuid)) {
                    applicationUuid = I18nMgtDataHolder.getInstance().getApplicationManagementService()
                            .getMainAppId(applicationUuid);
                }
            }
        } catch (OrganizationManagementException e) {
            throw new NotificationTemplateManagerException(e.getMessage(), e);
        } catch (IdentityApplicationManagementServerException e) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_ERROR_RESOLVING_MAIN_APPLICATION.getCode(),
                    TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
            String message = String.format(
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_ERROR_RESOLVING_MAIN_APPLICATION.getMessage(),
                    applicationUuid, tenantDomain);
            throw new NotificationTemplateManagerException(code, message, e);
        }
        validateTemplateLocale(locale);
        locale = normalizeLocaleFormat(locale);
        validateDisplayNameOfTemplateType(templateType);
        assertTemplateTypeExists(templateType, notificationChannel, tenantDomain);
        NotificationTemplate notificationTemplate = userDefinedTemplatePersistenceManager.getNotificationTemplate(
                templateType, locale, notificationChannel, applicationUuid, tenantDomain);

        String defaultLocale = getDefaultNotificationLocale(notificationChannel);
        if (notificationTemplate == null) {
            if (StringUtils.equalsIgnoreCase(locale, defaultLocale)) {
                // Template is not available in the default locale. Therefore, breaking the flow at the consuming side
                // to avoid NPE.
                String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                        TemplateMgtConstants.ErrorMessages.ERROR_CODE_TEMPLATE_NOT_FOUND.getCode(),
                        TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
                String errorMessage = String
                        .format(TemplateMgtConstants.ErrorMessages.ERROR_CODE_TEMPLATE_NOT_FOUND.getMessage(),
                                templateType, tenantDomain);
                throw new NotificationTemplateManagerServerException(code, errorMessage);
            } else {
                if (log.isDebugEnabled()) {
                    String message = String
                            .format("'%s' template in '%s' locale was not found in '%s' tenant. Trying to return the "
                                            + "template in default locale : '%s'", templateType, locale, tenantDomain,
                                    defaultLocale);
                    log.debug(message);
                }
                // Try to get the template type in default locale.
                return getNotificationTemplate(notificationChannel, templateType, defaultLocale, tenantDomain);
            }
        }
        return notificationTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NotificationTemplate> getAllSystemNotificationTemplatesOfType(String notificationChannel,
                                              String templateDisplayName) throws NotificationTemplateManagerException {

        assertSystemTemplateTypeExists(templateDisplayName, notificationChannel);
        List<NotificationTemplate> notificationTemplates =
                systemTemplatePersistenceManager.listNotificationTemplates(templateDisplayName,
                        notificationChannel, null, null);
        if (notificationTemplates == null) {
            notificationTemplates = new ArrayList<>();
        }
        return notificationTemplates;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NotificationTemplate getSystemNotificationTemplate(String notificationChannel, String templateType,
                                                               String locale)
            throws NotificationTemplateManagerException {

        validateTemplateLocale(locale);
        locale = normalizeLocaleFormat(locale);
        validateDisplayNameOfTemplateType(templateType);
        assertSystemTemplateTypeExists(templateType, notificationChannel);
        NotificationTemplate notificationTemplate =
                systemTemplatePersistenceManager.getNotificationTemplate(templateType,locale, notificationChannel,
                        null, null);

        String defaultLocale = getDefaultNotificationLocale(notificationChannel);
        if (notificationTemplate == null) {
            if (StringUtils.equalsIgnoreCase(locale, defaultLocale)) {
                // Template is not available in the default locale. Therefore, breaking the flow at the consuming side
                // to avoid NPE.
                String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                        TemplateMgtConstants.ErrorMessages.ERROR_CODE_TEMPLATE_NOT_FOUND.getCode(),
                        TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
                String errorMessage = String
                        .format(TemplateMgtConstants.ErrorMessages.ERROR_CODE_SYSTEM_TEMPLATE_NOT_FOUND.getMessage(),
                                templateType);
                throw new NotificationTemplateManagerServerException(code, errorMessage);
            } else {
                if (log.isDebugEnabled()) {
                    String message = String
                            .format("'%s' system template in '%s' locale was not found. Trying to return the "
                                            + "template in default locale : '%s'", templateType, locale,
                                    defaultLocale);
                    log.debug(message);
                }
                // Try to get the template type in default locale.
                return getSystemNotificationTemplate(notificationChannel, templateType, defaultLocale);
            }
        }
        return notificationTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addNotificationTemplate(NotificationTemplate notificationTemplate, String tenantDomain)
            throws NotificationTemplateManagerException {

        addNotificationTemplate(notificationTemplate, tenantDomain, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addNotificationTemplate(NotificationTemplate notificationTemplate, String tenantDomain,
                                         String applicationUuid) throws NotificationTemplateManagerException {

        validateNotificationTemplate(notificationTemplate);
        String displayName = notificationTemplate.getDisplayName();
        String locale = notificationTemplate.getLocale();
        String notificationChannel = notificationTemplate.getNotificationChannel();
        locale = normalizeLocaleFormat(locale);
        if (notificationTemplate.getLocale() != null && !notificationTemplate.getLocale().equals(locale)) {
            notificationTemplate.setLocale(locale);
        }
        assertTemplateTypeExists(displayName, notificationChannel, tenantDomain);
        if (userDefinedTemplatePersistenceManager.isNotificationTemplateExists(displayName, locale, notificationChannel,
                applicationUuid, tenantDomain)) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_TEMPLATE_ALREADY_EXISTS.getCode(),
                    TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
            String message = String.format(
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_TEMPLATE_ALREADY_EXISTS.getMessage(), displayName,
                    tenantDomain);
            throw new NotificationTemplateManagerServerException(code, message);
        }
        try {
            userDefinedTemplatePersistenceManager.addOrUpdateNotificationTemplate(notificationTemplate, applicationUuid,
                    tenantDomain);
        } catch (NotificationTemplateManagerServerException e) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_TEMPLATE.getCode(),
                    TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
            String message =
                    String.format(TemplateMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_TEMPLATE.getMessage(),
                            displayName, tenantDomain);
            throw new NotificationTemplateManagerServerException(code, message, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateNotificationTemplate(NotificationTemplate notificationTemplate, String tenantDomain)
            throws NotificationTemplateManagerException {

        updateNotificationTemplate(notificationTemplate, tenantDomain, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateNotificationTemplate(NotificationTemplate notificationTemplate, String tenantDomain,
                                            String applicationUuid) throws NotificationTemplateManagerException {

        validateNotificationTemplate(notificationTemplate);
        String displayName = notificationTemplate.getDisplayName();
        String locale = notificationTemplate.getLocale();
        String notificationChannel = notificationTemplate.getNotificationChannel();
        locale = normalizeLocaleFormat(locale);
        if (notificationTemplate.getLocale() != null && !notificationTemplate.getLocale().equals(locale)) {
            notificationTemplate.setLocale(locale);
        }
        assertTemplateTypeExists(displayName, notificationChannel, tenantDomain);
        if (!userDefinedTemplatePersistenceManager.isNotificationTemplateExists(
                displayName, locale, notificationChannel, applicationUuid, tenantDomain)) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_TEMPLATE_NOT_FOUND.getCode(),
                    TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
            String message = String.format(
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_TEMPLATE_NOT_FOUND.getMessage(), displayName,
                    tenantDomain);
            throw new NotificationTemplateManagerServerException(code, message);
        }
        try {
            userDefinedTemplatePersistenceManager.addOrUpdateNotificationTemplate(notificationTemplate, applicationUuid,
                    tenantDomain);
        } catch (NotificationTemplateManagerServerException e) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_ERROR_UPDATING_TEMPLATE.getCode(),
                    TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
            String message =
                    String.format(TemplateMgtConstants.ErrorMessages.ERROR_CODE_ERROR_UPDATING_TEMPLATE.getMessage(),
                            displayName, locale, tenantDomain);
            throw new NotificationTemplateManagerServerException(code, message, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteNotificationTemplate(String notificationChannel, String templateDisplayName, String locale,
                                            String tenantDomain)
            throws NotificationTemplateManagerException {

        deleteNotificationTemplate(notificationChannel, templateDisplayName, locale, tenantDomain, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteNotificationTemplate(String notificationChannel, String templateDisplayName, String locale,
                                            String tenantDomain, String applicationUuid)
            throws NotificationTemplateManagerException {

        // Validate the name and locale code.
        if (StringUtils.isBlank(templateDisplayName)) {
            String errorCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            TemplateMgtConstants.ErrorMessages.ERROR_CODE_INVALID_TEMPLATE_DISPLAY_NAME.getCode(),
                            TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
            throw new NotificationTemplateManagerClientException( errorCode,
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_INVALID_TEMPLATE_DISPLAY_NAME.getMessage());
        }
        if (StringUtils.isBlank(locale)) {
            String errorCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            TemplateMgtConstants.ErrorMessages.ERROR_CODE_INVALID_LOCALE.getCode(),
                            TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
            throw new NotificationTemplateManagerClientException(errorCode,
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_INVALID_LOCALE.getMessage());
        }
        locale = normalizeLocaleFormat(locale);
        try {
            userDefinedTemplatePersistenceManager.deleteNotificationTemplate(templateDisplayName, locale,
                    notificationChannel, applicationUuid, tenantDomain);
        } catch (NotificationTemplateManagerServerException ex) {
            String msg = String.format("Error deleting %s:%s template from %s tenant registry.", templateDisplayName,
                    locale, tenantDomain);
            throw handleServerException(msg, ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetNotificationTemplateType(String notificationChannel, String templateType,
                                                      String tenantDomain) throws NotificationTemplateManagerException {
        try {
            unifiedTemplatePersistenceManager.deleteNotificationTemplateType(templateType, notificationChannel,
                    tenantDomain);
            unifiedTemplatePersistenceManager.addNotificationTemplateType(templateType, notificationChannel,
                    tenantDomain);
        } catch (NotificationTemplateManagerException e) {
            String msg = String.format("Error deleting custom templates for %s template type %s from %s .",
                    notificationChannel, templateType, tenantDomain);
            throw handleServerException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNotificationTemplateExists(String notificationChannel, String templateDisplayName, String locale,
                                                 String tenantDomain)
            throws NotificationTemplateManagerException {

        return isNotificationTemplateExists(notificationChannel, templateDisplayName, locale, tenantDomain, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNotificationTemplateExists(String notificationChannel, String templateDisplayName, String locale,
                                                 String tenantDomain, String applicationUuid)
            throws NotificationTemplateManagerException {

        try {
            locale = normalizeLocaleFormat(locale);
            return userDefinedTemplatePersistenceManager.isNotificationTemplateExists(templateDisplayName, locale,
                    notificationChannel, applicationUuid, tenantDomain);
        } catch (NotificationTemplateManagerServerException e) {
            String error = String.format("Error when retrieving notification templates of %s tenant.", tenantDomain);
            throw new NotificationTemplateManagerServerException(error, e);
        }
    }

    private void validateDisplayNameOfTemplateType(String templateDisplayName)
            throws NotificationTemplateManagerClientException {

        if (StringUtils.isBlank(templateDisplayName)) {
            String errorCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            TemplateMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_NAME.getCode(),
                            TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
            throw new NotificationTemplateManagerClientException(errorCode,
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_NAME.getMessage());
        }
    }

    private String getRootOrgTenantDomain(String tenantDomain) throws OrganizationManagementException {

        OrganizationManager organizationManager = I18nMgtDataHolder.getInstance().getOrganizationManager();
        String orgId = organizationManager.resolveOrganizationId(tenantDomain);
        String primaryOrgId = organizationManager.getPrimaryOrganizationId(orgId);
        return organizationManager.resolveTenantDomain(primaryOrgId);
    }

    private NotificationTemplateManagerServerException handleServerException(String errorMsg, Throwable ex)
            throws NotificationTemplateManagerServerException {

        log.error(errorMsg);
        return new NotificationTemplateManagerServerException(errorMsg, ex);
    }

    private void validateTemplateLocale(String locale) throws NotificationTemplateManagerClientException {

        if (StringUtils.isBlank(locale)) {
            String errorCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            TemplateMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_LOCALE.getCode(),
                            TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
            throw new NotificationTemplateManagerClientException(errorCode,
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_LOCALE.getMessage());
        }
        final String LOCAL_REGEX = "^[a-z]{2}_[A-Z]{2}$";
        if (!locale.matches(LOCAL_REGEX)) {
            String errorCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            TemplateMgtConstants.ErrorMessages.ERROR_CODE_INVALID_LOCALE.getCode(),
                            TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
            throw new NotificationTemplateManagerClientException(errorCode,
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_INVALID_LOCALE.getMessage());
        }
    }

    private void validateNotificationTemplate(NotificationTemplate notificationTemplate)
            throws NotificationTemplateManagerClientException {

        if (notificationTemplate == null) {
            String errorCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            TemplateMgtConstants.ErrorMessages.ERROR_CODE_NULL_TEMPLATE_OBJECT.getCode(),
                            TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
            throw new NotificationTemplateManagerClientException(errorCode,
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_NULL_TEMPLATE_OBJECT.getMessage());
        }
        String displayName = notificationTemplate.getDisplayName();
        validateDisplayNameOfTemplateType(displayName);
        String normalizedDisplayName = I18nEmailUtil.getNormalizedName(displayName);
        if (!StringUtils.equalsIgnoreCase(normalizedDisplayName, notificationTemplate.getType())) {
            if (log.isDebugEnabled()) {
                String message = String.format("In the template normalizedDisplayName : %s is not equal to the " +
                                "template type : %s. Therefore template type is sent to : %s", normalizedDisplayName,
                        notificationTemplate.getType(), normalizedDisplayName);
                log.debug(message);
            }
            notificationTemplate.setType(normalizedDisplayName);
        }
        validateTemplateLocale(notificationTemplate.getLocale());
        String body = notificationTemplate.getBody();
        String subject = notificationTemplate.getSubject();
        String footer = notificationTemplate.getFooter();
        if (StringUtils.isBlank(notificationTemplate.getNotificationChannel())) {
            String errorCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            TemplateMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_CHANNEL.getCode(),
                            TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
            throw new NotificationTemplateManagerClientException(errorCode,
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_CHANNEL.getMessage());
        }
        if (NotificationChannels.SMS_CHANNEL.getChannelType()
                .equalsIgnoreCase(notificationTemplate.getNotificationChannel())) {
            if (StringUtils.isBlank(body)) {
                String errorCode =
                        I18nEmailUtil.prependOperationScenarioToErrorCode(
                                TemplateMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SMS_TEMPLATE.getCode(),
                                TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
                throw new NotificationTemplateManagerClientException(errorCode,
                        TemplateMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SMS_TEMPLATE.getMessage());
            }
            if (StringUtils.isNotBlank(subject) || StringUtils.isNotBlank(footer)) {
                String errorCode =
                        I18nEmailUtil.prependOperationScenarioToErrorCode(
                                TemplateMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SMS_TEMPLATE_CONTENT.getCode(),
                                TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
                throw new NotificationTemplateManagerClientException(errorCode,
                        TemplateMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SMS_TEMPLATE_CONTENT.getMessage());
            }
        } else {
            if (StringUtils.isBlank(subject) || StringUtils.isBlank(body)) {
                String errorCode =
                        I18nEmailUtil.prependOperationScenarioToErrorCode(
                                TemplateMgtConstants.ErrorMessages.ERROR_CODE_INVALID_EMAIL_TEMPLATE.getCode(),
                                TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
                throw new NotificationTemplateManagerClientException(errorCode,
                        TemplateMgtConstants.ErrorMessages.ERROR_CODE_INVALID_EMAIL_TEMPLATE.getMessage());
            }
        }
    }

    private String getDefaultNotificationLocale(String notificationChannel) {

        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            return DEFAULT_SMS_NOTIFICATION_LOCALE;
        } else {
            return DEFAULT_EMAIL_NOTIFICATION_LOCALE;
        }
    }

    private void assertSystemTemplateTypeExists(String templateType, String notificationChannel)
            throws NotificationTemplateManagerServerException {

        if (!systemTemplatePersistenceManager.isNotificationTemplateTypeExists(templateType, notificationChannel,
                null)) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_SYSTEM_TEMPLATE_TYPE_NOT_FOUND.getCode(),
                    TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
            String message = String.format(
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_SYSTEM_TEMPLATE_TYPE_NOT_FOUND.getMessage(),
                    templateType);
            throw new NotificationTemplateManagerServerException(code, message);
        }
    }

    private void assertTemplateTypeExists(String templateType, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (!unifiedTemplatePersistenceManager.isNotificationTemplateTypeExists(templateType, notificationChannel,
                tenantDomain)) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_TEMPLATE_TYPE_NOT_FOUND.getCode(),
                    TemplateMgtConstants.ErrorScenarios.NOTIFICATION_TEMPLATE_MANAGER);
            String message = String.format(
                    TemplateMgtConstants.ErrorMessages.ERROR_CODE_TEMPLATE_TYPE_NOT_FOUND.getMessage(),
                    templateType, tenantDomain);
            throw new NotificationTemplateManagerServerException(code, message);
        }
    }
}
