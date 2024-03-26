/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtServerException;
import org.wso2.carbon.email.mgt.model.EmailTemplate;

import java.util.List;

/**
 * This service provides functionality for managing internationalized email templates used for notifications across
 * Identity components.
 */
public class I18nEmailMgtConfigService {

    private static final Log log = LogFactory.getLog(I18nEmailMgtConfigService.class);
    private EmailTemplateManager templateManager = new EmailTemplateManagerImpl();

    /**
     * Add a new email template type for a tenant.
     *
     * @param emailTemplateDisplayName Display Name of the email template (eg: Account Recovery, Password Reset)
     * @throws I18nEmailMgtServerException
     */
    public void addEmailTemplateType(String emailTemplateDisplayName) throws I18nEmailMgtServerException {

        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        try {
            templateManager.addEmailTemplateType(emailTemplateDisplayName, tenantDomain);
        } catch (I18nEmailMgtException e) {
            String errorMsg = String.format("Error while adding email template type to %s tenant.", tenantDomain);
            handleException(errorMsg, e);
        }
    }

    /**
     * Delete an email template type from a tenant registry.
     *
     * @param emailTemplateDisplayName Display name of the email template type to be deleted.
     * @throws I18nEmailMgtServerException
     */
    public void deleteEmailTemplateType(String emailTemplateDisplayName) throws I18nEmailMgtServerException {

        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        try {
            templateManager.deleteEmailTemplateType(emailTemplateDisplayName, tenantDomain);
        } catch (I18nEmailMgtException e) {
            String errorMsg = "Error occurred while deleting email template type '%s' of %s tenant.";
            handleException(String.format(errorMsg, emailTemplateDisplayName, tenantDomain), e);
        }
    }


    /**
     * Get email template types available in the tenant.
     *
     * @return List of template type display names available.
     * @throws I18nEmailMgtServerException
     */
    public String[] getEmailTemplateTypes() throws I18nEmailMgtServerException {

        String[] emailTemplateTypes = null;

        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        try {
            List<String> emailTemplateTypesList = templateManager.getAvailableTemplateTypes(tenantDomain);
            emailTemplateTypes = emailTemplateTypesList.toArray(new String[emailTemplateTypesList.size()]);
        } catch (I18nEmailMgtException e) {
            String errorMsg = String.format("Error while retrieving email template types of %s tenant.", tenantDomain);
            handleException(errorMsg, e);
        }

        return emailTemplateTypes;
    }


    /**
     * This method is used to save the email template specific to a tenant.
     *
     * @param emailTemplate - Email templates to be saved.
     * @throws I18nEmailMgtServerException
     */
    public void saveEmailTemplate(EmailTemplate emailTemplate) throws I18nEmailMgtServerException {

        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        try {
            templateManager.addEmailTemplate(emailTemplate, tenantDomain);
        } catch (I18nEmailMgtException e) {
            String error = "Error occurred while updating email template in " + tenantDomain + " tenant registry.";
            handleException(error, e);
        }
    }

    /**
     * This method is used to add an email template specific to a tenant.
     *
     * @param emailTemplate - Email templates to be saved.
     * @throws I18nEmailMgtServerException
     */
    public void addEmailTemplate(EmailTemplate emailTemplate) throws I18nEmailMgtServerException {

        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        try {
            templateManager.addEmailTemplate(emailTemplate, tenantDomain);
        } catch (I18nEmailMgtException e) {
            String errorMsg = "Error occurred while adding email template to " + tenantDomain + " tenant registry";
            handleException(errorMsg, e);
        }
    }

    /**
     * @param emailTemplateDisplayName
     * @param locale
     * @throws I18nEmailMgtServerException
     */
    public void deleteEmailTemplate(String emailTemplateDisplayName, String locale) throws I18nEmailMgtServerException {
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        try {
            templateManager.deleteEmailTemplate(emailTemplateDisplayName, locale, tenantDomain);
        } catch (I18nEmailMgtException e) {
            String errorMsg = "Error occurred while deleting email template type '" + emailTemplateDisplayName +
                    "' of locale '" + locale + "' in " + tenantDomain + " tenant registry.";
            handleException(errorMsg, e);
        }

    }

    /**
     * This method is used to load the email template specific to a tenant.
     *
     * @return an Array of templates.
     * @throws I18nEmailMgtServerException
     */
    public EmailTemplate[] getAllTemplatesForTenant() throws I18nEmailMgtServerException {

        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        EmailTemplate[] templates = null;
        try {
            List<EmailTemplate> templateDTOs = templateManager.getAllEmailTemplates(tenantDomain);
            templates = templateDTOs.toArray(new EmailTemplate[templateDTOs.size()]);
        } catch (I18nEmailMgtException e) {
            String errorMsg = "Error occurred while retrieving email templates of " + tenantDomain + " tenant.";
            handleException(errorMsg, e);
        }

        return templates;
    }


    private void handleException(String errorMessage, I18nEmailMgtException e) throws I18nEmailMgtServerException {
        log.error(errorMessage, e);
        throw new I18nEmailMgtServerException(errorMessage, e);
    }
}
