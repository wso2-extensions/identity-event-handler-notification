/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.email.mgt.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtServerException;
import org.wso2.carbon.email.mgt.exceptions.I18nMgtEmailConfigException;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.identity.base.IdentityValidationUtil;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerClientException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.CollectionImpl;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.ResourceImpl;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.*;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.DEFAULT_EMAIL_LOCALE;

public class I18nEmailUtil {

    private static final Log log = LogFactory.getLog(I18nEmailUtil.class);
    public static final String CHARSET_CONSTANT = "charset";
    public static final String CHARSET_UTF_8 = CHARSET_CONSTANT + "=" + StandardCharsets.UTF_8;

    private I18nEmailUtil() {
    }

    /**
     * @param templateTypeName
     * @return
     */
    public static String getNormalizedName(String templateTypeName) {
        if (StringUtils.isNotBlank(templateTypeName)) {
            return templateTypeName.replaceAll("\\s+", "").toLowerCase();
        }
        throw new IllegalArgumentException("Invalid template type name provided : " + templateTypeName);
    }


    /**
     * Get the default email notification template List.
     *
     * @return List of default email notification templates.
     */
    @Deprecated
    public static List<EmailTemplate> getDefaultEmailTemplates() {

        List<NotificationTemplate> defaultEmailTemplates = I18nMgtDataHolder.getInstance().getDefaultEmailTemplates();
        List<EmailTemplate> mailTemplates = new ArrayList<>();
        defaultEmailTemplates.forEach(notificationTemplate ->
                mailTemplates.add(buildEmailTemplate(notificationTemplate)));
        return mailTemplates;
    }

    /**
     * Build an Email Template object using Notification template data.
     *
     * @param notificationTemplate {@link
     *                             org.wso2.carbon.identity.governance.service.notification.NotificationTemplateManager}
     *                             object
     * @return {@link org.wso2.carbon.email.mgt.model.EmailTemplate} object
     */
    public static EmailTemplate buildEmailTemplate(NotificationTemplate notificationTemplate) {

        // Build an email template using SMS template data.
        EmailTemplate emailTemplate = new EmailTemplate();
        emailTemplate.setTemplateDisplayName(notificationTemplate.getDisplayName());
        emailTemplate.setTemplateType(notificationTemplate.getType());
        emailTemplate.setLocale(notificationTemplate.getLocale());
        emailTemplate.setBody(notificationTemplate.getBody());
        emailTemplate.setSubject(notificationTemplate.getSubject());
        emailTemplate.setFooter(notificationTemplate.getFooter());
        emailTemplate.setEmailContentType(notificationTemplate.getContentType());
        return emailTemplate;
    }

    /**
     * @param emailTemplate
     * @return
     * @throws I18nEmailMgtException
     */
    public static Resource createTemplateResource(EmailTemplate emailTemplate) throws I18nEmailMgtException {
        Resource templateResource = new ResourceImpl();

        String templateDisplayName = emailTemplate.getTemplateDisplayName();
        String templateType = I18nEmailUtil.getNormalizedName(templateDisplayName);
        String locale = emailTemplate.getLocale();
        String contentType = emailTemplate.getEmailContentType();

        String subject = emailTemplate.getSubject();
        String body = emailTemplate.getBody();
        String footer = emailTemplate.getFooter();

        // set template properties
        templateResource.setProperty(I18nMgtConstants.TEMPLATE_TYPE_DISPLAY_NAME, templateDisplayName);
        templateResource.setProperty(I18nMgtConstants.TEMPLATE_TYPE, templateType);
        templateResource.setProperty(I18nMgtConstants.TEMPLATE_LOCALE, locale);
        templateResource.setProperty(I18nMgtConstants.TEMPLATE_CONTENT_TYPE, contentType);

        templateResource.setMediaType(RegistryConstants.TAG_MEDIA_TYPE);

        String contentArray[] = {subject, body, footer};
        String content = new Gson().toJson(contentArray);

        try {
            byte[] contentByteArray = content.getBytes("UTF-8");
            templateResource.setContent(contentByteArray);
        } catch (RegistryException | UnsupportedEncodingException e) {
            String error = "Error creating a registry resource from contents of %s email template type in %s locale.";
            throw new I18nEmailMgtServerException(String.format(error, templateDisplayName, locale), e);
        }

        return templateResource;
    }

    /**
     * @param templateResource
     * @return
     * @throws I18nEmailMgtException
     */
    public static EmailTemplate getEmailTemplate(Resource templateResource) throws I18nEmailMgtException {
        EmailTemplate emailTemplate = new EmailTemplate();
        try {
            // process email template meta-data properties
            String templateDisplayName = templateResource.getProperty(I18nMgtConstants.TEMPLATE_TYPE_DISPLAY_NAME);
            String templateType = templateResource.getProperty(I18nMgtConstants.TEMPLATE_TYPE);
            String contentType = templateResource.getProperty(I18nMgtConstants.TEMPLATE_CONTENT_TYPE);

            // Setting UTF-8 for all the email templates as it supports many languages and is widely adopted.
            // There is little to no value addition making the charset configurable.
            if (contentType != null && !contentType.toLowerCase().contains(CHARSET_CONSTANT)) {
                contentType = contentType + "; " + CHARSET_UTF_8;
            }
            String locale = templateResource.getProperty(I18nMgtConstants.TEMPLATE_LOCALE);

            emailTemplate.setTemplateDisplayName(templateDisplayName);
            emailTemplate.setTemplateType(templateType);
            emailTemplate.setEmailContentType(contentType);
            emailTemplate.setLocale(locale);

            // process email template content
            Object content = templateResource.getContent();
            if (content != null) {
                byte templateContentArray[] = (byte[]) content;
                String templateContent = new String(templateContentArray, Charset.forName("UTF-8"));

                String[] templateContentElements;
                try {
                    templateContentElements = new Gson().fromJson(templateContent, String[].class);
                } catch (JsonSyntaxException ex) {
                    String error = "Error deserializing '%s:%s' template from tenant registry.";
                    throw new I18nEmailMgtServerException(String.format(error, templateDisplayName, locale), ex);
                }

                if (templateContentElements == null || templateContentElements.length != 3) {
                    String errorMsg = "Template %s:%s body is in invalid format. Missing subject,body or footer.";
                    throw new I18nMgtEmailConfigException(String.format(errorMsg, templateDisplayName, locale));
                }

                emailTemplate.setSubject(templateContentElements[0]);
                emailTemplate.setBody(templateContentElements[1]);
                emailTemplate.setFooter(templateContentElements[2]);
            } else {
                String error = String.format("Unable to find any content in %s:%s email template.",
                        templateDisplayName, locale);
                log.error(error);
            }
        } catch (RegistryException e) {
            String error = "Error retrieving a template object from the registry resource";
            throw new I18nEmailMgtServerException(error, e);
        }
        return emailTemplate;
    }

    /**
     * @param normalizedTemplateName
     * @param templateDisplayName
     * @return
     */
    public static Collection createTemplateType(String normalizedTemplateName, String templateDisplayName) {
        Collection collection = new CollectionImpl();
        collection.addProperty(I18nMgtConstants.EMAIL_TEMPLATE_NAME, normalizedTemplateName);
        collection.addProperty(I18nMgtConstants.EMAIL_TEMPLATE_TYPE_DISPLAY_NAME, templateDisplayName);
        return collection;
    }

    /**
     * Prepend the operation scenario to the existing exception error code.
     * (Eg: USR-20045)
     *
     * @param exceptionErrorCode Existing error code.
     * @param scenario           Operation scenario
     * @return New error code with the scenario prepended (NOTE: Return an empty String if the provided error code is
     * empty)
     */
    public static String prependOperationScenarioToErrorCode(String exceptionErrorCode, String scenario) {

        if (StringUtils.isNotEmpty(exceptionErrorCode)) {
            // Check whether the scenario is already in the errorCode.
            if (exceptionErrorCode.contains(I18nMgtConstants.ERROR_CODE_DELIMITER)) {
                return exceptionErrorCode;
            }
            if (StringUtils.isNotEmpty(scenario)) {
                exceptionErrorCode =
                        scenario + I18nMgtConstants.ERROR_CODE_DELIMITER + exceptionErrorCode;
            }
        }
        return exceptionErrorCode;
    }

    public static int getTenantId(String tenantDomain) throws I18nEmailMgtException {

        int tenantId;
        try {
            RealmService realmService = I18nMgtDataHolder.getInstance().getRealmService();
            tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            throw new I18nEmailMgtException("ERROR_CODE_RETRIEVE_TENANT_ID", "Error while retrieving tenant id");
        }

        if (tenantId == MultitenantConstants.INVALID_TENANT_ID) {
            throw new I18nEmailMgtException("ERROR_CODE_INVALID_TENANT_DOMAIN");
        }
        return tenantId;
    }

    public static List<EmailTemplate> convertToEmailTemplates(List<NotificationTemplate> notificationTemplates) {
        List<EmailTemplate> emailTemplates = new ArrayList<>();
        for (NotificationTemplate notificationTemplate : notificationTemplates) {
            emailTemplates.add(buildEmailTemplate(notificationTemplate));
        }
        return emailTemplates;
    }

    /**
     * Validate the attributes of a notification template.
     *
     * @param notificationTemplate Notification template
     * @throws NotificationTemplateManagerClientException Invalid notification template.
     */
    public static void validateNotificationTemplate(NotificationTemplate notificationTemplate)
            throws NotificationTemplateManagerClientException {

        if (notificationTemplate == null) {
            String errorCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            I18nMgtConstants.ErrorMessages.ERROR_CODE_NULL_TEMPLATE_OBJECT.getCode(),
                            I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            throw new NotificationTemplateManagerClientException(errorCode,
                    I18nMgtConstants.ErrorMessages.ERROR_CODE_NULL_TEMPLATE_OBJECT.getMessage());
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
                            I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_CHANNEL.getCode(),
                            I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            throw new NotificationTemplateManagerClientException(errorCode,
                    I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_CHANNEL.getMessage());
        }
        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationTemplate.getNotificationChannel())) {
            if (StringUtils.isBlank(body)) {
                String errorCode =
                        I18nEmailUtil.prependOperationScenarioToErrorCode(
                                I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SMS_TEMPLATE.getCode(),
                                I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
                throw new NotificationTemplateManagerClientException(errorCode,
                        I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SMS_TEMPLATE.getMessage());
            }
            if (StringUtils.isNotBlank(subject) || StringUtils.isNotBlank(footer)) {
                String errorCode =
                        I18nEmailUtil.prependOperationScenarioToErrorCode(
                                I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SMS_TEMPLATE_CONTENT.getCode(),
                                I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
                throw new NotificationTemplateManagerClientException(errorCode,
                        I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SMS_TEMPLATE_CONTENT.getMessage());
            }
        } else {
            if (StringUtils.isBlank(subject) || StringUtils.isBlank(body)) {
                String errorCode =
                        I18nEmailUtil.prependOperationScenarioToErrorCode(
                                I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_EMAIL_TEMPLATE.getCode(),
                                I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
                throw new NotificationTemplateManagerClientException(errorCode,
                        I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_EMAIL_TEMPLATE.getMessage());
            }
        }
    }

    /**
     * Validate the display name of the notification template.
     *
     * @param displayName Display name
     * @throws NotificationTemplateManagerClientException Invalid notification template name
     */
    public static void validateDisplayNameOfTemplateType(String displayName)
            throws NotificationTemplateManagerClientException {

        if (StringUtils.isBlank(displayName)) {
            String errorCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_NAME.getCode(),
                            I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            throw new NotificationTemplateManagerClientException(errorCode,
                    I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_NAME.getMessage());
        }
        /*Template name can contain only alphanumeric characters and spaces, it can't contain registry invalid
        characters*/
        String[] whiteListPatterns = {TEMPLATE_REGEX_KEY};
        String[] blackListPatterns = {REGISTRY_INVALID_CHARS};
        if (!IdentityValidationUtil.isValid(displayName, whiteListPatterns, blackListPatterns)) {
            String errorCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_TEMPLATE_NAME.getCode(),
                            I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            String message =
                    String.format(
                            I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_TEMPLATE_NAME.getMessage(),
                            displayName);
            throw new NotificationTemplateManagerClientException(errorCode, message);
        }
    }

    /**
     * Validates the locale code of a notification template.
     *
     * @param locale Locale code
     * @throws NotificationTemplateManagerClientException Invalid notification template
     */
    public static void validateTemplateLocale(String locale) throws NotificationTemplateManagerClientException {

        if (StringUtils.isBlank(locale)) {
            String errorCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_LOCALE.getCode(),
                            I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            throw new NotificationTemplateManagerClientException(errorCode,
                    I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_LOCALE.getMessage());
        }
        // Regex check for registry invalid chars.
        if (!IdentityValidationUtil.isValidOverBlackListPatterns(locale, REGISTRY_INVALID_CHARS)) {
            String errorCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_LOCALE.getCode(),
                            I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            String message =
                    String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_LOCALE.getMessage(),
                            locale);
            throw new NotificationTemplateManagerClientException(errorCode, message);
        }
    }

    /**
     * Build notification template model from the email template attributes.
     *
     * @param emailTemplate EmailTemplate
     * @return NotificationTemplate
     */
    public static NotificationTemplate buildNotificationTemplateFromEmailTemplate(EmailTemplate emailTemplate) {

        NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setNotificationChannel(NotificationChannels.EMAIL_CHANNEL.getChannelType());
        notificationTemplate.setSubject(emailTemplate.getSubject());
        notificationTemplate.setBody(emailTemplate.getBody());
        notificationTemplate.setFooter(emailTemplate.getFooter());
        notificationTemplate.setType(emailTemplate.getTemplateType());
        notificationTemplate.setDisplayName(emailTemplate.getTemplateDisplayName());
        notificationTemplate.setLocale(emailTemplate.getLocale());
        notificationTemplate.setContentType(emailTemplate.getEmailContentType());
        return notificationTemplate;
    }

    /**
     * Get default notification template locale for a given notification channel.
     *
     * @param notificationChannel Notification channel
     * @return Default locale
     */
    public static String getDefaultNotificationLocale(String notificationChannel) {

        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            return DEFAULT_SMS_NOTIFICATION_LOCALE;
        } else {
            return DEFAULT_EMAIL_LOCALE;
        }
    }
}
