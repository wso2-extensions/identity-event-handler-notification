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
import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.dao.AppNotificationTemplateDAO;
import org.wso2.carbon.email.mgt.dao.NotificationScenarioDAO;
import org.wso2.carbon.email.mgt.dao.OrgNotificationTemplateDAO;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtClientException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtInternalException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtServerException;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.identity.governance.IdentityMgtConstants;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerClientException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;

import java.util.List;

import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.DEFAULT_EMAIL_LOCALE;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.*;

/**
 * Provides functionality to manage email template-scenarios & email templates.
 */
public class DBBasedEmailTemplateManager implements EmailTemplateManager {
    private static final Log log = LogFactory.getLog(DBBasedEmailTemplateManager.class);

    private final NotificationScenarioDAO notificationScenarioDAO = new NotificationScenarioDAO();
    private final OrgNotificationTemplateDAO orgNotificationTemplateDAO = new OrgNotificationTemplateDAO();
    private final AppNotificationTemplateDAO appNotificationTemplateDAO = new AppNotificationTemplateDAO();
    private static final String EMAIL_CHANNEL = NotificationChannels.EMAIL_CHANNEL.getChannelType();
    @Override
    public void addEmailTemplateType(String emailTemplateTypeDisplayName, String tenantDomain) throws I18nEmailMgtException {

        log.info("Test addEmailTemplateType(): " + emailTemplateTypeDisplayName);

        try {
            //TODO: Check this can be moved to the API layer
            validateDisplayNameOfTemplateType(emailTemplateTypeDisplayName);
            I18nEmailUtil.getNormalizedName(emailTemplateTypeDisplayName);
        } catch (NotificationTemplateManagerClientException e) {
            throw new I18nEmailMgtClientException(e.getMessage(), e);
        }

        if (isEmailTemplateTypeExists(emailTemplateTypeDisplayName, tenantDomain)) {
            String message = String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getMessage(), emailTemplateTypeDisplayName, tenantDomain);
            throw new I18nEmailMgtInternalException(I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_ALREADY_EXISTS, message);
        }

        try {
            notificationScenarioDAO.addNotificationScenario(emailTemplateTypeDisplayName, emailTemplateTypeDisplayName, EMAIL_CHANNEL, getTenantId(tenantDomain));
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public boolean isEmailTemplateTypeExists(String templateTypeDisplayName, String tenantDomain) throws I18nEmailMgtException {

        log.info("Test type isEmailTemplateTypeExists(): " + templateTypeDisplayName);

//        try {
            //TODO: Check this can be moved to the API layer
//            EmailTemplateManagerImpl.validateDisplayNameOfTemplateType(templateTypeDisplayName);
//        } catch (NotificationTemplateManagerClientException e) {
//            throw new I18nEmailMgtClientException(e.getMessage(), e);
//        }
        I18nEmailUtil.getNormalizedName(templateTypeDisplayName);

        try {
            String scenarioName = notificationScenarioDAO.getNotificationScenario(templateTypeDisplayName, EMAIL_CHANNEL, getTenantId(tenantDomain));
            return StringUtils.isNotBlank(scenarioName);
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public List<String> getAvailableTemplateTypes(String tenantDomain) throws I18nEmailMgtException {

        log.info("Test type getAvailableTemplateTypes()");
        try {
            List<String> templateTypesDisplayNames = notificationScenarioDAO.listNotificationScenarios(EMAIL_CHANNEL, getTenantId(tenantDomain));
            return templateTypesDisplayNames;
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteEmailTemplateType(String templateDisplayName, String tenantDomain) throws I18nEmailMgtException {

        log.info("Test type deleteEmailTemplateType(): " + templateDisplayName);

        try {
            //TODO: Check this can be moved to the API layer
            validateDisplayNameOfTemplateType(templateDisplayName);
            I18nEmailUtil.getNormalizedName(templateDisplayName);
        } catch (NotificationTemplateManagerClientException e) {
            //TODO: error handling is different from the registry based impl
            throw new I18nEmailMgtClientException(e.getMessage(), e);
        }

        try {
            notificationScenarioDAO.removeNotificationScenario(templateDisplayName, EMAIL_CHANNEL, getTenantId(tenantDomain));
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public void addEmailTemplate(EmailTemplate emailTemplate, String tenantDomain) throws I18nEmailMgtException {

        log.info("Test org addEmailTemplate(): " + emailTemplate.getTemplateDisplayName() + " locale: " + emailTemplate.getLocale());

        NotificationTemplate notificationTemplate = buildNotificationTemplateFromEmailTemplate(emailTemplate);
        try {
            //TODO: Check this can be moved to the API layer
            validateNotificationTemplate(notificationTemplate);
            I18nEmailUtil.getNormalizedName(notificationTemplate.getDisplayName());
        } catch (NotificationTemplateManagerClientException e) {
            throw new I18nEmailMgtClientException(e.getMessage(), e);
        }

        // TODO: Registry impl creates template type if not exists

        if (isEmailTemplateExists(emailTemplate.getTemplateDisplayName(), emailTemplate.getLocale(), tenantDomain)) {
            String locale = notificationTemplate.getLocale();
            String scenarioName = notificationTemplate.getDisplayName();
            String channel = notificationTemplate.getNotificationChannel();
            // TODO: Check with the registry impl, it doesn't have a error here.
            String message = String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_ORG_TEMPLATE.getMessage(), locale, scenarioName, channel, tenantDomain);
            throw new I18nEmailMgtInternalException(I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_ALREADY_EXISTS, message);
        }

        try {
            orgNotificationTemplateDAO.addNotificationTemplate(notificationTemplate, EMAIL_CHANNEL, getTenantId(tenantDomain));
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public EmailTemplate getEmailTemplate(String templateTypeDisplayName, String locale, String tenantDomain) throws I18nEmailMgtException {

        log.info("Test org getEmailTemplate(): " + templateTypeDisplayName + " locale: " + locale);

        try {
            validateTemplateLocale(locale);
            validateDisplayNameOfTemplateType(templateTypeDisplayName);
        } catch (NotificationTemplateManagerClientException e) {
            //TODO: ERROR_CODE_EMPTY_LOCALE & ERROR_CODE_EMPTY_TEMPLATE_NAME error codes are not handled in registry impl
            throw new I18nEmailMgtClientException(e.getMessage(), e);
        }

        String notificationChannel = EMAIL_CHANNEL;
        NotificationTemplate notificationTemplate;
        try {
            notificationTemplate = orgNotificationTemplateDAO.getNotificationTemplate(locale, templateTypeDisplayName, notificationChannel, getTenantId(tenantDomain));
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }

        // TODO: Check why its only SMS here in registry impl
        // Handle not having the requested SMS template type in required locale for this tenantDomain.
        if (notificationTemplate == null) {
            String defaultLocale = getDefaultNotificationLocale(notificationChannel);
            if (StringUtils.equalsIgnoreCase(defaultLocale, locale)) {
                // Template is not available in the default locale. Therefore, breaking the flow at the consuming side to avoid NPE.
                String error = String.format(IdentityMgtConstants.ErrorMessages.ERROR_CODE_NO_TEMPLATE_FOUND.getMessage(), templateTypeDisplayName, locale, tenantDomain);
                throw new I18nEmailMgtInternalException(I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_NODE_FOUND, error);
            } else {
                if (log.isDebugEnabled()) {
                    String message = String.format("'%s' template in '%s' locale was not found in '%s' tenant. Trying to return the template in default locale : '%s'", templateTypeDisplayName, locale, tenantDomain, DEFAULT_EMAIL_LOCALE);
                    log.debug(message);
                }
                // Try to get the template type in default locale.
                return getEmailTemplate(templateTypeDisplayName, defaultLocale, tenantDomain);
            }
        }

        // TODO: Remove following as its not needed when retrieving, but registry impl has it
        if (StringUtils.isBlank(notificationTemplate.getBody())) {
            String error = String.format(IdentityMgtConstants.ErrorMessages.
                    ERROR_CODE_NO_CONTENT_IN_TEMPLATE.getMessage(), templateTypeDisplayName, locale);
            throw new I18nEmailMgtClientException(IdentityMgtConstants.ErrorMessages.
                    ERROR_CODE_NO_CONTENT_IN_TEMPLATE.getCode(), error);
        }
        return buildEmailTemplate(notificationTemplate);
    }

    @Override
    public boolean isEmailTemplateExists(String templateTypeDisplayName, String locale, String tenantDomain)
            throws I18nEmailMgtException {

        log.info("Test org isEmailTemplateExists(): " + templateTypeDisplayName + " locale: " + locale);

//        try {
        //TODO: Check this can be moved to the API layer
//            EmailTemplateManagerImpl.validateDisplayNameOfTemplateType(templateTypeDisplayName);
//        } catch (NotificationTemplateManagerClientException e) {
//            throw new I18nEmailMgtClientException(e.getMessage(), e);
//        }
        I18nEmailUtil.getNormalizedName(templateTypeDisplayName);

        try {
            NotificationTemplate notificationTemplate = orgNotificationTemplateDAO.getNotificationTemplate(locale, templateTypeDisplayName, EMAIL_CHANNEL, getTenantId(tenantDomain));
            return notificationTemplate != null;
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public List<EmailTemplate> getEmailTemplateType(String templateDisplayName, String tenantDomain)
            throws I18nEmailMgtException {

        log.info("Test org getEmailTemplate(): " + templateDisplayName);

        try {
            //TODO: Check this can be moved to the API layer
            validateDisplayNameOfTemplateType(templateDisplayName);
            I18nEmailUtil.getNormalizedName(templateDisplayName);
        } catch (NotificationTemplateManagerClientException e) {
            throw new I18nEmailMgtClientException(e.getMessage(), e);
        }

        try {
            List<NotificationTemplate> notificationTemplates = orgNotificationTemplateDAO.listNotificationTemplates(templateDisplayName, EMAIL_CHANNEL, getTenantId(tenantDomain));
            return convertToEmailTemplates(notificationTemplates);
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException("Error while listing the templates", e);
        }
    }

    @Override
    public void deleteEmailTemplate(String templateTypeDisplayName, String locale, String tenantDomain) throws I18nEmailMgtException {

        log.info("Test org deleteEmailTemplate(): " + templateTypeDisplayName + " locale: " + locale);

        //TODO: Check this can be moved to the API layer
        // Validate the name and locale code.
        if (StringUtils.isBlank(templateTypeDisplayName)) {
            throw new I18nEmailMgtClientException("Cannot Delete template. Email displayName cannot be null.");
        }
        if (StringUtils.isBlank(locale)) {
            throw new I18nEmailMgtClientException("Cannot Delete template. Email locale cannot be null.");
        }
        I18nEmailUtil.getNormalizedName(templateTypeDisplayName);

        try {
            orgNotificationTemplateDAO.removeNotificationTemplate(locale, templateTypeDisplayName, EMAIL_CHANNEL, getTenantId(tenantDomain));
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteEmailTemplates(String templateDisplayName, String tenantDomain) throws I18nEmailMgtException {

        log.info("Test org deleteEmailTemplates(): " + templateDisplayName);

        try {
            //TODO: Check this can be moved to the API layer
            validateDisplayNameOfTemplateType(templateDisplayName);
            I18nEmailUtil.getNormalizedName(templateDisplayName);
        } catch (NotificationTemplateManagerClientException e) {
            //TODO: error handling is different from the registry based impl
            throw new I18nEmailMgtClientException(e.getMessage(), e);
        }

        try {
            orgNotificationTemplateDAO.removeNotificationTemplates(templateDisplayName, EMAIL_CHANNEL, getTenantId(tenantDomain));
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public void addEmailTemplate(EmailTemplate emailTemplate, String tenantDomain, String applicationUuid) throws I18nEmailMgtException {

        log.info("Test app addEmailTemplate(): " + emailTemplate.getTemplateDisplayName() + " locale: " + emailTemplate.getLocale());
        try {
            NotificationTemplate notificationTemplate = buildNotificationTemplateFromEmailTemplate(emailTemplate);
            appNotificationTemplateDAO.addNotificationTemplate(notificationTemplate, EMAIL_CHANNEL, applicationUuid, getTenantId(tenantDomain));
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while adding the app template", e);
        }
    }

    @Override
    public EmailTemplate getEmailTemplate(String templateDisplayName, String locale, String tenantDomain, String applicationUuid) throws I18nEmailMgtException {

        log.info("Test app getEmailTemplate(): " + templateDisplayName);
        try {
            NotificationTemplate notificationTemplate = appNotificationTemplateDAO.getNotificationTemplate(locale, templateDisplayName, EMAIL_CHANNEL, applicationUuid, getTenantId(tenantDomain));
            return buildEmailTemplate(notificationTemplate);
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while retrieving the app template", e);
        }
    }

    @Override
    public boolean isEmailTemplateExists(String templateTypeDisplayName, String locale, String tenantDomain, String applicationUuid) throws I18nEmailMgtException {

        log.info("Test app isEmailTemplateExists(): " + templateTypeDisplayName + " locale: " + locale);
        try {
            NotificationTemplate notificationTemplate = appNotificationTemplateDAO.getNotificationTemplate(locale, templateTypeDisplayName, EMAIL_CHANNEL, applicationUuid, getTenantId(tenantDomain));
            return notificationTemplate != null;
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while checking the existence of the app template", e);
        }
    }

    @Override
    public List<EmailTemplate> getEmailTemplateType(String templateDisplayName, String tenantDomain, String applicationUuid) throws I18nEmailMgtException {

        if (StringUtils.isBlank(applicationUuid)) {
            log.info("Test app getEmailTemplateType() -> switching to org");
            return getEmailTemplateType(templateDisplayName, tenantDomain);
        }

        log.info("Test app getEmailTemplateType() -> ie listEmailTemplates(): " + templateDisplayName);
        try {
            List<NotificationTemplate> notificationTemplates = appNotificationTemplateDAO.listNotificationTemplates(templateDisplayName, EMAIL_CHANNEL, applicationUuid, getTenantId(tenantDomain));
            return convertToEmailTemplates(notificationTemplates);
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while listing the templates", e);
        }
    }

    @Override
    public void deleteEmailTemplate(String templateTypeDisplayName, String locale, String tenantDomain, String applicationUuid) throws I18nEmailMgtException {

        log.info("Test app deleteEmailTemplates(): " + templateTypeDisplayName + " locale: " + locale);
        try {
            appNotificationTemplateDAO.removeNotificationTemplate(locale, templateTypeDisplayName, EMAIL_CHANNEL, applicationUuid, getTenantId(tenantDomain));
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while deleting the app templates", e);
        }
    }

    @Override
    public void deleteEmailTemplates(String templateDisplayName, String tenantDomain, String applicationUuid) throws I18nEmailMgtException {

        log.info("Test app deleteEmailTemplates(): " + templateDisplayName);
        try {
            appNotificationTemplateDAO.removeNotificationTemplates(templateDisplayName, EMAIL_CHANNEL, applicationUuid, getTenantId(tenantDomain));
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while deleting the app templates", e);
        }
    }

    @Override
    public List<EmailTemplate> getAllEmailTemplates(String tenantDomain) throws I18nEmailMgtException {

        log.info(">>>>>>>>>>>>>>>> getAllEmailTemplates() Not implemented yet");
        return getDefaultEmailTemplates();
    }

    @Override
    public void addDefaultEmailTemplates(String tenantDomain) throws I18nEmailMgtException {

        log.info(">>>>>>>>>>>>>>>> addDefaultEmailTemplates()");
        getDefaultEmailTemplates().forEach(emailTemplate -> {
            try {
                if (!isEmailTemplateTypeExists(emailTemplate.getTemplateDisplayName(),tenantDomain)) {
                    addEmailTemplateType(emailTemplate.getTemplateDisplayName(), tenantDomain);
                }
                addEmailTemplate(emailTemplate, tenantDomain);
            } catch (I18nEmailMgtException e) {
                log.error("Error while adding default email templates for the tenant : " + tenantDomain, e);
            }
        });
    }
}