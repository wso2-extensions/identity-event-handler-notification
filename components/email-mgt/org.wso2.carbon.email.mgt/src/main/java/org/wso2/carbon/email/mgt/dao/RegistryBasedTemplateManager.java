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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService;
import org.wso2.carbon.identity.governance.IdentityMgtConstants;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.ResourceImpl;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.APP_TEMPLATE_PATH;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.EMAIL_TEMPLATE_PATH;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.EMAIL_TEMPLATE_TYPE_DISPLAY_NAME;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_NOT_FOUND;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.SMS_TEMPLATE_PATH;
import static org.wso2.carbon.registry.core.RegistryConstants.PATH_SEPARATOR;

/**
 * This class is responsible for managing the email templates in the registry.
 */
public class RegistryBasedTemplateManager implements TemplatePersistenceManager {

    private static final Log log = LogFactory.getLog(RegistryBasedTemplateManager.class);

    private I18nMgtDataHolder dataHolder = I18nMgtDataHolder.getInstance();
    private RegistryResourceMgtService resourceMgtService = dataHolder.getRegistryResourceMgtService();

    @Override
    public void addNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        String normalizedDisplayName = I18nEmailUtil.getNormalizedName(displayName);
        String path = buildTemplateRootDirectoryPath(normalizedDisplayName, notificationChannel);

        try {
            // Persist the template type to registry ie. create a directory.
            Collection collection = I18nEmailUtil.createTemplateType(normalizedDisplayName, displayName);
            resourceMgtService.putIdentityResource(collection, path, tenantDomain);
        } catch (IdentityRuntimeException e) {
            throw new NotificationTemplateManagerServerException("Error while adding notification template type.", e);
        }
    }

    @Override
    public boolean isNotificationTemplateTypeExists(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        // get template directory name from display name.
        String normalizedTemplateName = I18nEmailUtil.getNormalizedName(displayName);
        String path = buildTemplateRootDirectoryPath(normalizedTemplateName, notificationChannel);

        try {
            return resourceMgtService.isResourceExists(path, tenantDomain);
        } catch (IdentityRuntimeException e) {
            String error = String.format("Error when retrieving email templates of %s tenant.", tenantDomain);
            throw new NotificationTemplateManagerServerException(error, e);
        }
    }

    @Override
    public List<String> listNotificationTemplateTypes(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        try {
            List<String> templateTypeList = new ArrayList<>();
            String path = buildTemplateRootDirectoryPath(notificationChannel);
            Collection collection = (Collection) resourceMgtService.getIdentityResource(path, tenantDomain);

            for (String templatePath : collection.getChildren()) {
                Resource templateTypeResource = resourceMgtService.getIdentityResource(templatePath, tenantDomain);
                if (templateTypeResource != null) {
                    String emailTemplateType = templateTypeResource.getProperty(EMAIL_TEMPLATE_TYPE_DISPLAY_NAME);
                    templateTypeList.add(emailTemplateType);
                }
            }
            return templateTypeList;
        } catch (IdentityRuntimeException | RegistryException e) {
            throw new NotificationTemplateManagerServerException("Error while listing notification template types.", e);
        }
    }

    @Override
    public void deleteNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        String templateType = I18nEmailUtil.getNormalizedName(displayName);
        String path = buildTemplateRootDirectoryPath(templateType, notificationChannel);

        try {
            resourceMgtService.deleteIdentityResource(path, tenantDomain);
        } catch (IdentityRuntimeException e) {
            throw new NotificationTemplateManagerServerException("Error while deleting notification template type.", e);
        }
    }

    @Override
    public void addOrUpdateNotificationTemplate(NotificationTemplate notificationTemplate, String applicationUuid,
                                                String tenantDomain) throws NotificationTemplateManagerServerException {

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
                addNotificationTemplateType(displayName, notificationChannel, tenantDomain);
                if (log.isDebugEnabled()) {
                    String msg = "Creating template type : %s in tenant registry : %s";
                    log.debug(String.format(msg, displayName, tenantDomain));
                }
            }
            resourceMgtService.putIdentityResource(templateResource, path, tenantDomain, locale);
        } catch (IdentityRuntimeException e) {
            throw new NotificationTemplateManagerServerException("Error while adding notification template.", e);
        }
    }

    @Override
    public boolean isNotificationTemplateExists(String displayName, String locale, String notificationChannel,
                                                String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        // Get template directory name from display name.
        String normalizedTemplateName = I18nEmailUtil.getNormalizedName(displayName);
        String path = buildTemplateRootDirectoryPath(normalizedTemplateName, notificationChannel, applicationUuid);
        path = addLocaleToTemplateTypeResourcePath(path, locale);

        try {
            return resourceMgtService.isResourceExists(path, tenantDomain);
        } catch (IdentityRuntimeException e) {
            String error =
                    String.format("Error while checking the existence of the notification template %s in %s tenant.",
                            displayName, tenantDomain);
            throw new NotificationTemplateManagerServerException(error, e);
        }
    }

    @Override
    public NotificationTemplate getNotificationTemplate(String displayName, String locale, String notificationChannel,
                                                        String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        NotificationTemplate notificationTemplate = null;

        String type = I18nEmailUtil.getNormalizedName(displayName);
        String path = buildTemplateRootDirectoryPath(type, notificationChannel, applicationUuid);

        // Get registry resource.
        try {
            Resource registryResource = resourceMgtService.getIdentityResource(path, tenantDomain, locale);
            if (registryResource != null) {
                notificationTemplate = getNotificationTemplate(registryResource, notificationChannel);
            }
        } catch (IdentityRuntimeException e) {
            String error = String
                    .format(IdentityMgtConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_TEMPLATE_FROM_REGISTRY
                            .getMessage(), displayName, locale, tenantDomain);
            throw new NotificationTemplateManagerServerException(
                    IdentityMgtConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_TEMPLATE_FROM_REGISTRY.getCode(),
                    error, e);
        }

        return notificationTemplate;
    }

    @Override
    public List<NotificationTemplate> listNotificationTemplates(String templateDisplayName, String notificationChannel,
                                                                String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        String templateDirectory = I18nEmailUtil.getNormalizedName(templateDisplayName);
        String path = buildTemplateRootDirectoryPath(templateDirectory, notificationChannel, applicationUuid);

        try {
            return getAllTemplatesOfTemplateTypeFromRegistry(path, tenantDomain);
        } catch (RegistryException e) {
            String error = "Error when retrieving '%s' template type from %s tenant registry.";
            throw new NotificationTemplateManagerServerException(
                    String.format(error, templateDisplayName, tenantDomain), e);
        }
    }

    @Override
    public List<NotificationTemplate> listAllNotificationTemplates(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        List<NotificationTemplate> templateList = new ArrayList<>();

        try {
            String path = buildTemplateRootDirectoryPath(notificationChannel);
            Collection baseDirectory = (Collection) resourceMgtService.getIdentityResource(path, tenantDomain);

            if (baseDirectory != null) {
                for (String templateTypeDirectory : baseDirectory.getChildren()) {
                    templateList.addAll(
                            getAllTemplatesOfTemplateTypeFromRegistry(templateTypeDirectory, tenantDomain));
                }
            }
        } catch (RegistryException | IdentityRuntimeException e) {
            throw new NotificationTemplateManagerServerException("Error while listing all notification templates.", e);
        }

        return templateList;
    }

    @Override
    public void deleteNotificationTemplate(String displayName, String locale, String notificationChannel,
                                           String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        String templateType = I18nEmailUtil.getNormalizedName(displayName);
        String path = buildTemplateRootDirectoryPath(templateType, notificationChannel, applicationUuid);
        try {
            resourceMgtService.deleteIdentityResource(path, tenantDomain, locale);
        } catch (IdentityRuntimeException e) {
            String msg = String.format("Error deleting %s:%s template from %s tenant registry.", displayName,
                    locale, tenantDomain);
            throw new NotificationTemplateManagerServerException(msg, e);
        }
    }

    @Override
    public void deleteNotificationTemplates(String displayName, String notificationChannel, String applicationUuid,
                                            String tenantDomain) throws NotificationTemplateManagerServerException {

        String templateType = I18nEmailUtil.getNormalizedName(displayName);
        String path = buildTemplateRootDirectoryPath(templateType, notificationChannel, applicationUuid);

        try {
            if (StringUtils.isBlank(applicationUuid)) {
                // Delete org templates
                Collection templates = (Collection) resourceMgtService.getIdentityResource(path, tenantDomain);
                for (String subPath : templates.getChildren()) {
                    // Exclude the app templates.
                    if (!subPath.contains(APP_TEMPLATE_PATH)) {
                        resourceMgtService.deleteIdentityResource(subPath, tenantDomain);
                    }
                }
            } else {
                // Delete app templates
                if (!resourceMgtService.isResourceExists(path, tenantDomain)) {
                    // No templates found for the given application UUID.
                    return;
                }
                resourceMgtService.deleteIdentityResource(path, tenantDomain);
            }
        } catch (IdentityRuntimeException | RegistryException e) {
            throw new NotificationTemplateManagerServerException("Error while deleting notification templates.", e);
        }
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
            throws NotificationTemplateManagerServerException {

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
     * @throws NotificationTemplateManagerServerException If an error occurred while getting the template content
     */
    private String[] getTemplateElements(Resource templateResource, String notificationChannel, String displayName,
                                         String locale) throws NotificationTemplateManagerServerException {

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
                throw new NotificationTemplateManagerServerException(IdentityMgtConstants.ErrorMessages.
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
     * Loop through all template resources of a given template type registry path and return a list of EmailTemplate
     * objects.
     *
     * @param templateTypeRegistryPath Registry path of the template type.
     * @param tenantDomain             Tenant domain.
     * @return List of extracted EmailTemplate objects.
     * @throws RegistryException if any error occurred.
     */
    private List<NotificationTemplate> getAllTemplatesOfTemplateTypeFromRegistry(String templateTypeRegistryPath,
                                                                                 String tenantDomain)
            throws NotificationTemplateManagerServerException, RegistryException {

        List<NotificationTemplate> templateList = new ArrayList<>();
        Collection templateType = (Collection) resourceMgtService.getIdentityResource(templateTypeRegistryPath,
                tenantDomain);

        if (templateType == null) {
            String type = templateTypeRegistryPath.split(PATH_SEPARATOR)[
                    templateTypeRegistryPath.split(PATH_SEPARATOR).length - 1];
            String message =
                    String.format("Notification Template Type: %s not found in %s tenant registry.", type,
                            tenantDomain);
            throw new NotificationTemplateManagerServerException(EMAIL_TEMPLATE_TYPE_NOT_FOUND, message);
        }
        for (String template : templateType.getChildren()) {
            Resource templateResource = resourceMgtService.getIdentityResource(template, tenantDomain);
            // Exclude the app templates for organization template requests.
            if (templateResource != null && (templateTypeRegistryPath.contains(APP_TEMPLATE_PATH)
                    || !templateResource.getPath().contains(APP_TEMPLATE_PATH))) {
                try {
                    EmailTemplate templateDTO = I18nEmailUtil.getEmailTemplate(templateResource);
                    templateList.add(I18nEmailUtil.buildNotificationTemplateFromEmailTemplate(templateDTO));
                } catch (I18nEmailMgtException e) {
                    log.error("Failed retrieving a template object from the registry resource", e);
                }
            }
        }
        return templateList;
    }

    /**
     * Add the locale to the template type resource path.
     *
     * @param path   Email template path
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
     * @param templateType        Template type
     * @param notificationChannel Notification channel (SMS or EMAIL)
     * @return Root directory path
     */
    private String buildTemplateRootDirectoryPath(String templateType, String notificationChannel) {

        return buildTemplateRootDirectoryPath(templateType, notificationChannel, null);
    }

    /**
     * Build the template root directory path.
     *
     * @param templateType          Template type
     * @param notificationChannel   Notification channel
     * @param applicationUuid       Application UUID
     * @return Root directory path
     */
    private String buildTemplateRootDirectoryPath(String templateType, String notificationChannel,
                                                  String applicationUuid) {

        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            return SMS_TEMPLATE_PATH + PATH_SEPARATOR + templateType;
        }
        return EMAIL_TEMPLATE_PATH + PATH_SEPARATOR + templateType + getApplicationPath(applicationUuid);
    }

    /**
     * Build the template root directory path.
     *
     * @param notificationChannel   Notification channel
     * @return Root directory path
     */
    private String buildTemplateRootDirectoryPath(String notificationChannel) {

        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            return SMS_TEMPLATE_PATH;
        }
        return EMAIL_TEMPLATE_PATH;
    }
}
