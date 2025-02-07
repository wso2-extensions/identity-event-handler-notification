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
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.CollectionImpl;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.ResourceImpl;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static java.time.ZoneOffset.UTC;

public class I18nEmailUtil {

    private static final Log log = LogFactory.getLog(I18nEmailUtil.class);
    public static final String CHARSET_CONSTANT = "charset";
    public static final String CHARSET_UTF_8 = CHARSET_CONSTANT + "=" + StandardCharsets.UTF_8;
    public static final Calendar CALENDER = Calendar.getInstance(TimeZone.getTimeZone(UTC));
    private static final String HYPHEN = "-";
    private static final String UNDERSCORE = "_";

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

    public static String normalizeLocaleFormat(String localeString) {

        Locale locale = Locale.forLanguageTag(localeString.replace(UNDERSCORE, HYPHEN).toLowerCase());
        String normalizedLocale = locale.toString();
        if (StringUtils.isNotBlank(normalizedLocale)) {
            return normalizedLocale;
        }
        return getNotificationLocale();
    }

    /**
     * Get the notification locale.
     *
     * @return Locale
     */
    public static String getNotificationLocale() {

        return StringUtils.isNotBlank(IdentityUtil.getProperty(I18nMgtConstants.NOTIFICATION_DEFAULT_LOCALE))
                ? IdentityUtil.getProperty(I18nMgtConstants.NOTIFICATION_DEFAULT_LOCALE)
                : I18nMgtConstants.DEFAULT_NOTIFICATION_LOCALE;
    }

    /**
     * Get the notification template subject, body & footer contents as a byte array.
     *
     * @param notificationTemplate  the notification template to get the content
     * @return                      the byte array of the content
     */
    public static byte[] getContentByteArray(NotificationTemplate notificationTemplate) {

        String[] templateContent = new String[]{notificationTemplate.getSubject(), notificationTemplate.getBody(),
                notificationTemplate.getFooter()};
        String content = new Gson().toJson(templateContent);
        return content.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Read the content stream and set the subject, body & footer of the notification template.
     *
     * @param contentStream                 the inputStream of the content
     * @param notificationTemplateResult    the notification template to set the content
     * @throws SQLException
     */
    public static void setContent(InputStream contentStream, NotificationTemplate notificationTemplateResult) throws
            SQLException {

        if (contentStream == null) {
            return;
        }

        try {
            byte[] contentByteArray = contentStream.readAllBytes();
            String content = new String(contentByteArray, StandardCharsets.UTF_8);
            String[] templateContent = new Gson().fromJson(content, String[].class);
            if (templateContent != null && templateContent.length == 3) {
                notificationTemplateResult.setSubject(templateContent[0]);
                notificationTemplateResult.setBody(templateContent[1]);
                notificationTemplateResult.setFooter(templateContent[2]);
            } else {
                throw new SQLException("Invalid content data.");
            }
        } catch (IOException e) {
            throw new SQLException("Error while reading content data.", e);
        }
    }

    /**
     * Get the current time as a timestamp in UTC.
     *
     * @return  the current time as a timestamp
     */
    public static Timestamp getCurrentTime() {

        return new Timestamp(new Date().getTime());
    }
}
