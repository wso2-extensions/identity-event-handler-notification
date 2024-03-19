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
import org.wso2.carbon.email.mgt.model.NotificationTemplate;
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

        List<org.wso2.carbon.identity.governance.model.NotificationTemplate> defaultEmailTemplates = I18nMgtDataHolder.getInstance().getDefaultEmailTemplates();
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
    public static EmailTemplate buildEmailTemplate(org.wso2.carbon.identity.governance.model.NotificationTemplate notificationTemplate) {

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

    public static NotificationTemplate convertToNotificationTemplate(EmailTemplate emailTemplate) {
        return new NotificationTemplate(emailTemplate.getSubject(), emailTemplate.getBody(), emailTemplate.getFooter(),
                emailTemplate.getEmailContentType(), emailTemplate.getLocale(), emailTemplate.getTemplateType());
    }

    public static EmailTemplate convertToEmailTemplate(NotificationTemplate notificationTemplate) {
        EmailTemplate emailTemplate = new EmailTemplate();
        emailTemplate.setSubject(notificationTemplate.getSubject());
        emailTemplate.setBody(notificationTemplate.getBody());
        emailTemplate.setFooter(notificationTemplate.getFooter());
        emailTemplate.setEmailContentType(notificationTemplate.getContentType());
        emailTemplate.setLocale(notificationTemplate.getLocale());
        //TODO: check whether this is correct
        emailTemplate.setTemplateDisplayName(notificationTemplate.getScenarioType());
        emailTemplate.setTemplateType(notificationTemplate.getScenarioType());
        return emailTemplate;
    }

    public static List<EmailTemplate> convertToEmailTemplates(List<NotificationTemplate> notificationTemplates) {
        List<EmailTemplate> emailTemplates = new ArrayList<>();
        for (NotificationTemplate notificationTemplate : notificationTemplates) {
            emailTemplates.add(convertToEmailTemplate(notificationTemplate));
        }
        return emailTemplates;
    }
}
