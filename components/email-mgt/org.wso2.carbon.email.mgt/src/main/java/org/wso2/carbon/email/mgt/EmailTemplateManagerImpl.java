/*
 * Copyright (c) 2016-2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.email.mgt;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtClientException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtInternalException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtServerException;
import org.wso2.carbon.email.mgt.exceptions.I18nMgtEmailConfigException;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.base.IdentityValidationUtil;
import org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService;
import org.wso2.carbon.identity.governance.IdentityGovernanceUtil;
import org.wso2.carbon.identity.governance.IdentityMgtConstants;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerClientException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerInternalException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.identity.governance.service.notification.NotificationTemplateManager;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.ResourceImpl;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.*;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_NOT_FOUND;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.*;
import static org.wso2.carbon.identity.base.IdentityValidationUtil.ValidatorPattern.REGISTRY_INVALID_CHARS_EXISTS;
import static org.wso2.carbon.registry.core.RegistryConstants.PATH_SEPARATOR;

/**
 * Provides functionality to manage email templates used in notification emails.
 */
public class EmailTemplateManagerImpl implements EmailTemplateManager, NotificationTemplateManager {

    private I18nMgtDataHolder dataHolder = I18nMgtDataHolder.getInstance();
    private RegistryResourceMgtService resourceMgtService = dataHolder.getRegistryResourceMgtService();

    private static final Log log = LogFactory.getLog(EmailTemplateManagerImpl.class);

    static {
        IdentityValidationUtil.addPattern(TEMPLATE_REGEX_KEY, EMAIL_TEMPLATE_TYPE_REGEX);
        IdentityValidationUtil.addPattern(REGISTRY_INVALID_CHARS, REGISTRY_INVALID_CHARS_EXISTS.getRegex());
    }

    @Override
    public void addEmailTemplateType(String emailTemplateDisplayName, String tenantDomain) throws
            I18nEmailMgtException {

        try {
            addNotificationTemplateType(emailTemplateDisplayName, NotificationChannels.EMAIL_CHANNEL.getChannelType(),
                    tenantDomain);
        } catch (NotificationTemplateManagerClientException e) {
            throw new I18nEmailMgtClientException(e.getMessage(), e);
        } catch (NotificationTemplateManagerInternalException e) {
            if (StringUtils.isNotBlank(e.getErrorCode())) {
                String errorCode = e.getErrorCode();
                if (errorCode.contains(I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getCode())) {
                    throw new I18nEmailMgtInternalException(
                            I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_ALREADY_EXISTS, e.getMessage(), e);
                }
            }
            throw new I18nEmailMgtInternalException(e.getMessage(), e);
        } catch (NotificationTemplateManagerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public void addNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        addNotificationTemplateType(displayName, notificationChannel, tenantDomain, null);
    }

    @Override
    public void addNotificationTemplateType(String displayName, String notificationChannel,
                                            String tenantDomain, String applicationUuid)
            throws NotificationTemplateManagerException {

        validateDisplayNameOfTemplateType(displayName);
        String normalizedDisplayName = I18nEmailUtil.getNormalizedName(displayName);

        // Persist the template type to registry ie. create a directory.
        String path = buildTemplateRootDirectoryPath(normalizedDisplayName, notificationChannel, applicationUuid);
        try {
            // Check whether a template exists with the same name.
            if (resourceMgtService.isResourceExists(path, tenantDomain)) {
                String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                        I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getCode(),
                        I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
                String message =
                        String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getMessage(),
                                displayName, tenantDomain);
                throw new NotificationTemplateManagerInternalException(code, message);
            }
            Collection collection = I18nEmailUtil.createTemplateType(normalizedDisplayName, displayName);
            resourceMgtService.putIdentityResource(collection, path, tenantDomain);
        } catch (IdentityRuntimeException ex) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                    I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_TEMPLATE.getCode(),
                    I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            String message =
                    String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_TEMPLATE.getMessage(),
                            displayName, tenantDomain);
            throw new NotificationTemplateManagerServerException(code, message);
        }
    }

    @Override
    public void deleteEmailTemplateType(String emailTemplateDisplayName, String tenantDomain) throws
            I18nEmailMgtException {

        validateTemplateType(emailTemplateDisplayName, tenantDomain);

        String templateType = I18nEmailUtil.getNormalizedName(emailTemplateDisplayName);
        String path = EMAIL_TEMPLATE_PATH + PATH_SEPARATOR + templateType;

        try {
            resourceMgtService.deleteIdentityResource(path, tenantDomain);
        } catch (IdentityRuntimeException ex) {
            String errorMsg = String.format
                    ("Error deleting email template type %s from %s tenant.", emailTemplateDisplayName, tenantDomain);
            handleServerException(errorMsg, ex);
        }
    }

    /**
     * @param tenantDomain
     * @return
     * @throws I18nEmailMgtServerException
     */
    @Override
    public List<String> getAvailableTemplateTypes(String tenantDomain) throws I18nEmailMgtServerException {

        try {
            List<String> templateTypeList = new ArrayList<>();
            Collection collection = (Collection) resourceMgtService.getIdentityResource(EMAIL_TEMPLATE_PATH,
                    tenantDomain);

            for (String templatePath : collection.getChildren()) {
                Resource templateTypeResource = resourceMgtService.getIdentityResource(templatePath, tenantDomain);
                if (templateTypeResource != null) {
                    String emailTemplateType = templateTypeResource.getProperty(EMAIL_TEMPLATE_TYPE_DISPLAY_NAME);
                    templateTypeList.add(emailTemplateType);
                }
            }
            return templateTypeList;
        } catch (IdentityRuntimeException | RegistryException ex) {
            String errorMsg = String.format("Error when retrieving email template types of %s tenant.", tenantDomain);
            throw new I18nEmailMgtServerException(errorMsg, ex);
        }
    }

    @Override
    public List<EmailTemplate> getAllEmailTemplates(String tenantDomain) throws I18nEmailMgtException {

        List<EmailTemplate> templateList = new ArrayList<>();

        try {
            Collection baseDirectory = (Collection) resourceMgtService.getIdentityResource(EMAIL_TEMPLATE_PATH,
                    tenantDomain);

            if (baseDirectory != null) {
                for (String templateTypeDirectory : baseDirectory.getChildren()) {
                    templateList.addAll(
                            getAllTemplatesOfTemplateTypeFromRegistry(templateTypeDirectory, tenantDomain));
                }
            }
        } catch (RegistryException | IdentityRuntimeException e) {
            String error = String.format("Error when retrieving email templates of %s tenant.", tenantDomain);
            throw new I18nEmailMgtServerException(error, e);
        }

        return templateList;
    }

    @Override
    public EmailTemplate getEmailTemplate(String templateDisplayName, String locale, String tenantDomain)
            throws I18nEmailMgtException {

        return getEmailTemplate(templateDisplayName, locale, tenantDomain, null);
    }

    @Override
    public List<EmailTemplate> getEmailTemplateType(String templateDisplayName, String tenantDomain)
            throws I18nEmailMgtException {

        return getEmailTemplateType(templateDisplayName, tenantDomain, null);
    }

    @Override
    public List<EmailTemplate> getEmailTemplateType(
            String templateDisplayName, String tenantDomain, String applicationUuid) throws I18nEmailMgtException {

        validateTemplateType(templateDisplayName, tenantDomain);
        String templateDirectory = I18nEmailUtil.getNormalizedName(templateDisplayName);
        String templateTypeRegistryPath =
                EMAIL_TEMPLATE_PATH + PATH_SEPARATOR + templateDirectory + getApplicationPath(applicationUuid);
        try {
            return getAllTemplatesOfTemplateTypeFromRegistry(templateTypeRegistryPath, tenantDomain);
        } catch (RegistryException ex) {
            String error = "Error when retrieving '%s' template type from %s tenant registry.";
            throw new I18nEmailMgtServerException(String.format(error, templateDisplayName, tenantDomain), ex);
        }
    }

    @Override
    public NotificationTemplate getNotificationTemplate(String notificationChannel, String templateType, String locale,
            String tenantDomain) throws NotificationTemplateManagerException {

        return getNotificationTemplate(notificationChannel, templateType, locale, tenantDomain, null);
    }

    @Override
    public NotificationTemplate getNotificationTemplate(String notificationChannel, String templateType, String locale,
            String tenantDomain, String applicationUuid) throws NotificationTemplateManagerException {

        // Resolve channel to either SMS or EMAIL.
        notificationChannel = resolveNotificationChannel(notificationChannel);
        validateTemplateLocale(locale);
        validateDisplayNameOfTemplateType(templateType);
        NotificationTemplate notificationTemplate = null;

        // Get notification template registry path.
        String path;
        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            path = SMS_TEMPLATE_PATH + PATH_SEPARATOR + I18nEmailUtil.getNormalizedName(templateType);
        } else {
            path = EMAIL_TEMPLATE_PATH + PATH_SEPARATOR + I18nEmailUtil.getNormalizedName(templateType) +
                    getApplicationPath(applicationUuid);
        }

        // Get registry resource.
        try {
            Resource registryResource = resourceMgtService.getIdentityResource(path, tenantDomain, locale);
            if (registryResource != null) {
                notificationTemplate = getNotificationTemplate(registryResource, notificationChannel);
            }
        } catch (IdentityRuntimeException exception) {
            String error = String
                    .format(IdentityMgtConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_TEMPLATE_FROM_REGISTRY
                            .getMessage(), templateType, locale, tenantDomain);
            throw new NotificationTemplateManagerServerException(
                    IdentityMgtConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_TEMPLATE_FROM_REGISTRY.getCode(),
                    error, exception);
        }

        // Handle not having the requested SMS template type in required locale for this tenantDomain.
        if (notificationTemplate == null) {
            String defaultLocale = getDefaultNotificationLocale(notificationChannel);
            if (StringUtils.equalsIgnoreCase(defaultLocale, locale)) {

                // Template is not available in the default locale. Therefore, breaking the flow at the consuming side
                // to avoid NPE.
                String error = String
                        .format(IdentityMgtConstants.ErrorMessages.ERROR_CODE_NO_TEMPLATE_FOUND.getMessage(),
                                templateType, locale, tenantDomain);
                throw new NotificationTemplateManagerServerException(
                        IdentityMgtConstants.ErrorMessages.ERROR_CODE_NO_TEMPLATE_FOUND.getCode(), error);
            } else {
                if (log.isDebugEnabled()) {
                    String message = String
                            .format("'%s' template in '%s' locale was not found in '%s' tenant. Trying to return the "
                                            + "template in default locale : '%s'", templateType, locale, tenantDomain,
                                    DEFAULT_SMS_NOTIFICATION_LOCALE);
                    log.debug(message);
                }
                // Try to get the template type in default locale.
                return getNotificationTemplate(notificationChannel, templateType, defaultLocale, tenantDomain);
            }
        }
        return notificationTemplate;
    }

    /**
     * Get the notification template from resource.
     *
     * @param templateResource    {@link org.wso2.carbon.registry.core.Resource} object
     * @param notificationChannel Notification channel
     * @return {@link org.wso2.carbon.identity.governance.model.NotificationTemplate} object
     * @throws NotificationTemplateManagerException Error getting the notification template
     */
    private NotificationTemplate getNotificationTemplate(Resource templateResource, String notificationChannel)
            throws NotificationTemplateManagerException {

        NotificationTemplate notificationTemplate = new NotificationTemplate();

        // Get template meta properties.
        String displayName = templateResource.getProperty(I18nMgtConstants.TEMPLATE_TYPE_DISPLAY_NAME);
        String type = templateResource.getProperty(I18nMgtConstants.TEMPLATE_TYPE);
        String locale = templateResource.getProperty(I18nMgtConstants.TEMPLATE_LOCALE);
        if (NotificationChannels.EMAIL_CHANNEL.getChannelType().equals(notificationChannel)) {
            String contentType = templateResource.getProperty(I18nMgtConstants.TEMPLATE_CONTENT_TYPE);

            // Setting UTF-8 for all the email templates as it supports many languages and is widely adopted.
            // There is little to no value addition making the charset configurable.
            if (contentType != null && !contentType.toLowerCase().contains(I18nEmailUtil.CHARSET_CONSTANT)) {
                contentType = contentType + "; " + I18nEmailUtil.CHARSET_UTF_8;
            }
            notificationTemplate.setContentType(contentType);
        }
        notificationTemplate.setDisplayName(displayName);
        notificationTemplate.setType(type);
        notificationTemplate.setLocale(locale);

        // Process template content.
        String[] templateContentElements = getTemplateElements(templateResource, notificationChannel, displayName,
                locale);
        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            notificationTemplate.setBody(templateContentElements[0]);
        } else {
            notificationTemplate.setSubject(templateContentElements[0]);
            notificationTemplate.setBody(templateContentElements[1]);
            notificationTemplate.setFooter(templateContentElements[2]);
        }
        notificationTemplate.setNotificationChannel(notificationChannel);
        return notificationTemplate;
    }

    /**
     * Process template resource content and retrieve template elements.
     *
     * @param templateResource    Resource of the template
     * @param notificationChannel Notification channel
     * @param displayName         Display name of the template
     * @param locale              Locale of the template
     * @return Template content
     * @throws NotificationTemplateManagerException If an error occurred while getting the template content
     */
    private String[] getTemplateElements(Resource templateResource, String notificationChannel, String displayName,
            String locale) throws NotificationTemplateManagerException {

        try {
            Object content = templateResource.getContent();
            if (content != null) {
                byte[] templateContentArray = (byte[]) content;
                String templateContent = new String(templateContentArray, Charset.forName("UTF-8"));

                String[] templateContentElements;
                try {
                    templateContentElements = new Gson().fromJson(templateContent, String[].class);
                } catch (JsonSyntaxException exception) {
                    String error = String.format(IdentityMgtConstants.ErrorMessages.
                            ERROR_CODE_DESERIALIZING_TEMPLATE_FROM_TENANT_REGISTRY.getMessage(), displayName, locale);
                    throw new NotificationTemplateManagerServerException(IdentityMgtConstants.ErrorMessages.
                            ERROR_CODE_DESERIALIZING_TEMPLATE_FROM_TENANT_REGISTRY.getCode(), error, exception);
                }

                // Validate template content.
                if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
                    if (templateContentElements == null || templateContentElements.length != 1) {
                        String errorMsg = String.format(IdentityMgtConstants.ErrorMessages.
                                ERROR_CODE_INVALID_SMS_TEMPLATE_CONTENT.getMessage(), displayName, locale);
                        throw new NotificationTemplateManagerServerException(IdentityMgtConstants.ErrorMessages.
                                ERROR_CODE_INVALID_SMS_TEMPLATE_CONTENT.getCode(), errorMsg);
                    }
                } else {
                    if (templateContentElements == null || templateContentElements.length != 3) {
                        String errorMsg = String.format(IdentityMgtConstants.ErrorMessages.
                                ERROR_CODE_INVALID_EMAIL_TEMPLATE_CONTENT.getMessage(), displayName, locale);
                        throw new NotificationTemplateManagerServerException(IdentityMgtConstants.ErrorMessages.
                                ERROR_CODE_INVALID_EMAIL_TEMPLATE_CONTENT.getCode(), errorMsg);
                    }
                }
                return templateContentElements;
            } else {
                String error = String.format(IdentityMgtConstants.ErrorMessages.
                        ERROR_CODE_NO_CONTENT_IN_TEMPLATE.getMessage(), displayName, locale);
                throw new NotificationTemplateManagerClientException(IdentityMgtConstants.ErrorMessages.
                        ERROR_CODE_NO_CONTENT_IN_TEMPLATE.getCode(), error);
            }
        } catch (RegistryException exception) {
            String error = IdentityMgtConstants.ErrorMessages.
                    ERROR_CODE_ERROR_RETRIEVING_TEMPLATE_OBJECT_FROM_REGISTRY.getMessage();
            throw new NotificationTemplateManagerServerException(IdentityMgtConstants.ErrorMessages.
                    ERROR_CODE_ERROR_RETRIEVING_TEMPLATE_OBJECT_FROM_REGISTRY.getCode(), error, exception);
        }
    }

    /**
     * Resolve notification channel to a server supported notification channel.
     *
     * @param notificationChannel Notification channel
     * @return Notification channel (EMAIL or SMS)
     */
    private String resolveNotificationChannel(String notificationChannel) {

        if (NotificationChannels.EMAIL_CHANNEL.getChannelType().equals(notificationChannel)) {
            return notificationChannel;
        } else if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            return notificationChannel;
        } else {
            if (log.isDebugEnabled()) {
                String message = String.format("Notification channel : %s is not supported by the server. "
                                + "Notification channel changed to : %s", notificationChannel,
                        IdentityGovernanceUtil.getDefaultNotificationChannel());
                log.debug(message);
            }
            return IdentityGovernanceUtil.getDefaultNotificationChannel();
        }
    }

    /**
     * Create a registry resource instance of the notification template.
     *
     * @param notificationTemplate Notification template
     * @return Resource
     * @throws NotificationTemplateManagerServerException If an error occurred while creating the resource
     */
    private Resource createTemplateRegistryResource(NotificationTemplate notificationTemplate)
            throws NotificationTemplateManagerServerException {

        String displayName = notificationTemplate.getDisplayName();
        String type = I18nEmailUtil.getNormalizedName(displayName);
        String locale = notificationTemplate.getLocale();
        String body = notificationTemplate.getBody();

        // Set template properties.
        Resource templateResource = new ResourceImpl();
        templateResource.setProperty(I18nMgtConstants.TEMPLATE_TYPE_DISPLAY_NAME, displayName);
        templateResource.setProperty(I18nMgtConstants.TEMPLATE_TYPE, type);
        templateResource.setProperty(I18nMgtConstants.TEMPLATE_LOCALE, locale);
        String[] templateContent;
        // Handle contents according to different channel types.
        if (NotificationChannels.EMAIL_CHANNEL.getChannelType().equals(notificationTemplate.getNotificationChannel())) {
            templateContent = new String[]{notificationTemplate.getSubject(), body, notificationTemplate.getFooter()};
            templateResource.setProperty(I18nMgtConstants.TEMPLATE_CONTENT_TYPE, notificationTemplate.getContentType());
        } else {
            templateContent = new String[]{body};
        }
        templateResource.setMediaType(RegistryConstants.TAG_MEDIA_TYPE);
        String content = new Gson().toJson(templateContent);
        try {
            byte[] contentByteArray = content.getBytes(StandardCharsets.UTF_8);
            templateResource.setContent(contentByteArray);
        } catch (RegistryException e) {
            String code =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_CREATING_REGISTRY_RESOURCE.getCode(),
                            I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            String message =
                    String.format(
                            I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_CREATING_REGISTRY_RESOURCE.getMessage(),
                            displayName, locale);
            throw new NotificationTemplateManagerServerException(code, message, e);
        }
        return templateResource;
    }

    @Override
    public void addNotificationTemplate(NotificationTemplate notificationTemplate, String tenantDomain)
            throws NotificationTemplateManagerException {

        addNotificationTemplate(notificationTemplate, tenantDomain, null);
    }

    @Override
    public void addNotificationTemplate(NotificationTemplate notificationTemplate,
                                        String tenantDomain, String applicationUuid)
            throws NotificationTemplateManagerException {

        validateNotificationTemplate(notificationTemplate);
        String notificationChannel = notificationTemplate.getNotificationChannel();

        Resource templateResource = createTemplateRegistryResource(notificationTemplate);
        String displayName = notificationTemplate.getDisplayName();
        String type = I18nEmailUtil.getNormalizedName(displayName);
        String locale = notificationTemplate.getLocale();

        String path = buildTemplateRootDirectoryPath(type, notificationChannel, applicationUuid);
        try {
            // Check whether a template type root directory exists.
            if (!resourceMgtService.isResourceExists(path, tenantDomain)) {
                // Add new template type with relevant properties.
                addNotificationTemplateType(displayName, notificationChannel, tenantDomain, applicationUuid);
                if (log.isDebugEnabled()) {
                    String msg = "Creating template type : %s in tenant registry : %s";
                    log.debug(String.format(msg, displayName, tenantDomain));
                }
            }
            resourceMgtService.putIdentityResource(templateResource, path, tenantDomain, locale);
        } catch (IdentityRuntimeException e) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                    I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ERROR_ADDING_TEMPLATE.getCode(),
                    I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            String message =
                    String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ERROR_ADDING_TEMPLATE.getMessage(),
                            displayName, locale, tenantDomain);
            throw new NotificationTemplateManagerServerException(code, message);
        }
    }

    @Override
    public void addEmailTemplate(EmailTemplate emailTemplate, String tenantDomain) throws I18nEmailMgtException {

        addEmailTemplate(emailTemplate, tenantDomain, null);
    }


    @Override
    public void deleteEmailTemplate(String templateTypeName, String localeCode, String tenantDomain) throws
            I18nEmailMgtException {

        deleteEmailTemplate(templateTypeName, localeCode, tenantDomain, null);
    }

    @Override
    public void deleteEmailTemplates(String templateTypeName, String tenantDomain) throws I18nEmailMgtException {

        validateTemplateType(templateTypeName, tenantDomain);

        String templateType = I18nEmailUtil.getNormalizedName(templateTypeName);
        String path = EMAIL_TEMPLATE_PATH + PATH_SEPARATOR + templateType;

        try {
            Collection templates = (Collection) resourceMgtService.getIdentityResource(path, tenantDomain);
            for (String subPath : templates.getChildren()) {
                // Exclude the app templates.
                if (!subPath.contains(APP_TEMPLATE_PATH)) {
                    resourceMgtService.deleteIdentityResource(subPath, tenantDomain);
                }
            }
        } catch (IdentityRuntimeException | RegistryException ex) {
            String errorMsg = String.format
                    ("Error deleting email template type %s from %s tenant.", templateType, tenantDomain);
            handleServerException(errorMsg, ex);
        }
    }

    @Override
    public void deleteEmailTemplates(String templateTypeName, String tenantDomain, String applicationUuid)
            throws I18nEmailMgtException {

        validateTemplateType(templateTypeName, tenantDomain);

        String templateType = I18nEmailUtil.getNormalizedName(templateTypeName);
        String path = EMAIL_TEMPLATE_PATH + PATH_SEPARATOR + templateType + APP_TEMPLATE_PATH +
                PATH_SEPARATOR + applicationUuid;

        try {
            if (!resourceMgtService.isResourceExists(path, tenantDomain)) {
                // No templates found for the given application UUID.
                return;
            }
            resourceMgtService.deleteIdentityResource(path, tenantDomain);
        } catch (IdentityRuntimeException ex) {
            String errorMsg = String.format("Error deleting email template type %s from %s tenant for application %s.",
                    templateType, tenantDomain, applicationUuid);
            handleServerException(errorMsg, ex);
        }
    }

    @Override
    public void addDefaultEmailTemplates(String tenantDomain) throws I18nEmailMgtException {

        try {
            addDefaultNotificationTemplates(NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain);
        } catch (NotificationTemplateManagerClientException e) {
            throw new I18nEmailMgtClientException(e.getMessage(), e);
        } catch (NotificationTemplateManagerInternalException e) {
            if (StringUtils.isNotBlank(e.getErrorCode())) {
                String errorCode = e.getErrorCode();
                if (errorCode.contains(I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getCode())) {
                    throw new I18nEmailMgtInternalException(
                            I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_ALREADY_EXISTS, e.getMessage(), e);
                }
            }
            throw new I18nEmailMgtInternalException(e.getMessage(), e);
        } catch (NotificationTemplateManagerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    /**
     * Add the default notification templates which matches the given notification channel to the respective tenants
     * registry.
     *
     * @param notificationChannel Notification channel (Eg: SMS, EMAIL)
     * @param tenantDomain Tenant domain
     * @throws NotificationTemplateManagerException Error adding the default notification templates
     */
    @Override
    public void addDefaultNotificationTemplates(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        // Get the list of Default notification templates.
        List<NotificationTemplate> notificationTemplates =
                getDefaultNotificationTemplates(notificationChannel);
        int numberOfAddedTemplates = 0;
        try {
            for (NotificationTemplate template : notificationTemplates) {
                String displayName = template.getDisplayName();
                String type = I18nEmailUtil.getNormalizedName(displayName);
                String locale = template.getLocale();
                String path = buildTemplateRootDirectoryPath(type, notificationChannel);

            /*Check for existence of each category, since some template may have migrated from earlier version
            This will also add new template types provided from file, but won't update any existing template*/
                if (!resourceMgtService.isResourceExists(addLocaleToTemplateTypeResourcePath(path, locale),
                        tenantDomain)) {
                    try {
                        addNotificationTemplate(template, tenantDomain);
                        if (log.isDebugEnabled()) {
                            String msg = "Default template added to %s tenant registry : %n%s";
                            log.debug(String.format(msg, tenantDomain, template.toString()));
                        }
                        numberOfAddedTemplates++;
                    } catch (NotificationTemplateManagerInternalException e) {
                        log.warn("Template : " + displayName + "already exists in the registry. Hence " +
                                "ignoring addition");
                    }
                }
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("Added %d default %s templates to the tenant registry : %s",
                        numberOfAddedTemplates, notificationChannel, tenantDomain));
            }
        } catch (IdentityRuntimeException ex) {
            String error = "Error when tried to check for default email templates in tenant registry : %s";
            log.error(String.format(error, tenantDomain), ex);
        }
    }

    /**
     * Get the notification templates which matches the given notification template type.
     *
     * @param notificationChannel Notification channel type. (Eg: EMAIL, SMS)
     * @return List of default notification templates
     */
    @Override
    public List<NotificationTemplate> getDefaultNotificationTemplates(String notificationChannel) {

        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            return I18nMgtDataHolder.getInstance().getDefaultSMSTemplates();
        }
        return I18nMgtDataHolder.getInstance().getDefaultEmailTemplates();
    }

    @Override
    public boolean isEmailTemplateExists(String templateTypeDisplayName, String locale, String tenantDomain)
            throws I18nEmailMgtException {

        return isEmailTemplateExists(templateTypeDisplayName, locale, tenantDomain, null);
    }

    @Override
    public boolean isEmailTemplateExists(String templateTypeDisplayName, String locale,
                                         String tenantDomain, String applicationUuid) throws I18nEmailMgtException {

        // Get template directory name from display name.
        String normalizedTemplateName = I18nEmailUtil.getNormalizedName(templateTypeDisplayName);
        String path = EMAIL_TEMPLATE_PATH + PATH_SEPARATOR + normalizedTemplateName +
                getApplicationPath(applicationUuid) + PATH_SEPARATOR + locale.toLowerCase();

        try {
            Resource template = resourceMgtService.getIdentityResource(path, tenantDomain);
            return template != null;
        } catch (IdentityRuntimeException e) {
            String error = String.format("Error when retrieving email templates of %s tenant.", tenantDomain);
            throw new I18nEmailMgtServerException(error, e);
        }
    }

    @Override
    public boolean isEmailTemplateTypeExists(String templateTypeDisplayName, String tenantDomain)
            throws I18nEmailMgtException {

        // get template directory name from display name.
        String normalizedTemplateName = I18nEmailUtil.getNormalizedName(templateTypeDisplayName);
        String path = EMAIL_TEMPLATE_PATH + PATH_SEPARATOR + normalizedTemplateName;

        try {
            Resource templateType = resourceMgtService.getIdentityResource(path, tenantDomain);
            return templateType != null;
        } catch (IdentityRuntimeException e) {
            String error = String.format("Error when retrieving email templates of %s tenant.", tenantDomain);
            throw new I18nEmailMgtServerException(error, e);
        }
    }

    @Override
    public void addEmailTemplate(EmailTemplate emailTemplate, String tenantDomain, String applicationUuid)
            throws I18nEmailMgtException {

        NotificationTemplate notificationTemplate = buildNotificationTemplateFromEmailTemplate(emailTemplate);
        try {
            addNotificationTemplate(notificationTemplate, tenantDomain, applicationUuid);
        } catch (NotificationTemplateManagerClientException e) {
            throw new I18nEmailMgtClientException(e.getMessage(), e);
        } catch (NotificationTemplateManagerInternalException e) {
            if (StringUtils.isNotBlank(e.getErrorCode())) {
                String errorCode = e.getErrorCode();
                if (errorCode.contains(I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getCode())) {
                    throw new I18nEmailMgtInternalException(
                            I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_ALREADY_EXISTS, e.getMessage(), e);
                }
            }
            throw new I18nEmailMgtInternalException(e.getMessage(), e);
        } catch (NotificationTemplateManagerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteEmailTemplate(String templateTypeName, String localeCode, String tenantDomain,
                                    String applicationUuid) throws I18nEmailMgtException {

        // Validate the name and locale code.
        if (StringUtils.isBlank(templateTypeName)) {
            throw new I18nEmailMgtClientException("Cannot Delete template. Email displayName cannot be null.");
        }

        if (StringUtils.isBlank(localeCode)) {
            throw new I18nEmailMgtClientException("Cannot Delete template. Email locale cannot be null.");
        }

        String templateType = I18nEmailUtil.getNormalizedName(templateTypeName);
        String path = EMAIL_TEMPLATE_PATH + PATH_SEPARATOR + templateType + getApplicationPath(applicationUuid);
        try {
            resourceMgtService.deleteIdentityResource(path, tenantDomain, localeCode);
        } catch (IdentityRuntimeException ex) {
            String msg = String.format("Error deleting %s:%s template from %s tenant registry.", templateTypeName,
                    localeCode, tenantDomain);
            handleServerException(msg, ex);
        }
    }

    @Override
    public EmailTemplate getEmailTemplate(String templateType, String locale, String tenantDomain,
                                          String applicationUuid) throws I18nEmailMgtException {

        try {
            NotificationTemplate notificationTemplate = getNotificationTemplate(
                    NotificationChannels.EMAIL_CHANNEL.getChannelType(), templateType, locale,
                    tenantDomain, applicationUuid);
            return buildEmailTemplate(notificationTemplate);
        } catch (NotificationTemplateManagerException exception) {
            String errorCode = exception.getErrorCode();
            String errorMsg = exception.getMessage();
            Throwable throwable = exception.getCause();

            // Match NotificationTemplateManagerExceptions with the existing I18nEmailMgtException error types.
            if (StringUtils.isNotEmpty(exception.getErrorCode())) {
                if (IdentityMgtConstants.ErrorMessages.ERROR_CODE_INVALID_NOTIFICATION_TEMPLATE.getCode()
                        .equals(errorCode) || IdentityMgtConstants.ErrorMessages.ERROR_CODE_NO_CONTENT_IN_TEMPLATE
                        .getCode().equals(errorCode) ||
                        I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_TEMPLATE_NAME.getCode()
                                .equals(errorCode) ||
                        I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_LOCALE
                                .getCode().equals(errorCode)) {
                    throw new I18nEmailMgtClientException(errorMsg, throwable);
                } else if (IdentityMgtConstants.ErrorMessages.ERROR_CODE_INVALID_EMAIL_TEMPLATE_CONTENT.getCode()
                        .equals(errorCode)) {
                    throw new I18nMgtEmailConfigException(errorMsg, throwable);
                } else if (IdentityMgtConstants.ErrorMessages.ERROR_CODE_NO_TEMPLATE_FOUND.getCode()
                        .equals(errorCode)) {
                    throw new I18nEmailMgtInternalException(I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_NODE_FOUND,
                            errorMsg, throwable);
                }
            }
            throw new I18nEmailMgtServerException(exception.getMessage(), exception.getCause());
        }
    }

    /**
     * Build application template path from application UUID or return empty string if application UUID is null.
     *
     * @param applicationUuid Application UUID.
     * @return Application template path.
     */
    private String getApplicationPath(String applicationUuid) {

        if (StringUtils.isNotBlank(applicationUuid)) {
            return APP_TEMPLATE_PATH + PATH_SEPARATOR + applicationUuid;
        }
        return StringUtils.EMPTY;
    }

    /**
     * Validate the displayName of a template type.
     *
     * @param templateDisplayName Display name of the notification template
     * @throws I18nEmailMgtClientException Invalid notification template
     */
    private void validateTemplateType(String templateDisplayName, String tenantDomain)
            throws I18nEmailMgtClientException {

        try {
            validateDisplayNameOfTemplateType(templateDisplayName);
        } catch (NotificationTemplateManagerClientException e) {
            if (StringUtils.isNotBlank(e.getErrorCode())) {
                String errorCode = e.getErrorCode();
                if (errorCode.contains(I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_NAME.getCode())) {
                    throw new I18nEmailMgtClientException("Template Type display name cannot be null", e);
                }
                if (errorCode.contains(
                        I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_TEMPLATE_NAME.getCode())) {
                    throw new I18nEmailMgtClientException(e.getMessage(), e);
                }
            }
            throw new I18nEmailMgtClientException("Invalid notification template", e);
        }
    }

    /**
     * Loop through all template resources of a given template type registry path and return a list of EmailTemplate
     * objects.
     *
     * @param templateTypeRegistryPath Registry path of the template type.
     * @param tenantDomain             Tenant domain.
     * @return List of extracted EmailTemplate objects.
     * @throws RegistryException if any error occurred.
     */
    private List<EmailTemplate> getAllTemplatesOfTemplateTypeFromRegistry(String templateTypeRegistryPath,
                                                                          String tenantDomain)
            throws RegistryException, I18nEmailMgtClientException {

        List<EmailTemplate> templateList = new ArrayList<>();
        Collection templateType = (Collection) resourceMgtService.getIdentityResource(templateTypeRegistryPath,
                tenantDomain);

        if (templateType == null) {
            String type = templateTypeRegistryPath.split(PATH_SEPARATOR)[
                    templateTypeRegistryPath.split(PATH_SEPARATOR).length - 1];
            String message =
                    String.format("Email Template Type: %s not found in %s tenant registry.", type, tenantDomain);
            throw new I18nEmailMgtClientException(EMAIL_TEMPLATE_TYPE_NOT_FOUND, message);
        }
        for (String template : templateType.getChildren()) {
            Resource templateResource = resourceMgtService.getIdentityResource(template, tenantDomain);
            // Exclude the app templates for organization template requests.
            if (templateResource != null && (templateTypeRegistryPath.contains(APP_TEMPLATE_PATH)
                    || !templateResource.getPath().contains(APP_TEMPLATE_PATH))) {
                try {
                    EmailTemplate templateDTO = I18nEmailUtil.getEmailTemplate(templateResource);
                    templateList.add(templateDTO);
                } catch (I18nEmailMgtException ex) {
                    log.error("Failed retrieving a template object from the registry resource", ex);
                }
            }
        }
        return templateList;
    }

    private void handleServerException(String errorMsg, Throwable ex) throws I18nEmailMgtServerException {

        log.error(errorMsg);
        throw new I18nEmailMgtServerException(errorMsg, ex);
    }

    /**
     * Add the locale to the template type resource path.
     *
     * @param path  Email template path
     * @param locale Locale code of the email template
     * @return Email template resource path
     */
    private String addLocaleToTemplateTypeResourcePath(String path, String locale) {

        if (StringUtils.isNotBlank(locale)) {
            return path + PATH_SEPARATOR + locale.toLowerCase();
        } else {
            return path;
        }
    }

    /**
     * Build the template type root directory path.
     *
     * @param templateType          Template type
     * @param notificationChannel   Notification channel (SMS or EMAIL)
     * @return Root directory path
     */
    private String buildTemplateRootDirectoryPath(String templateType, String notificationChannel) {

        return buildTemplateRootDirectoryPath(templateType, notificationChannel, null);
    }

    private String buildTemplateRootDirectoryPath(String templateType, String notificationChannel, String applicationUuid) {

        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            return SMS_TEMPLATE_PATH + PATH_SEPARATOR + templateType;
        }
        return EMAIL_TEMPLATE_PATH + PATH_SEPARATOR + templateType + getApplicationPath(applicationUuid);
    }
}
