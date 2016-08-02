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

package org.wso2.carbon.email.mgt;

import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.email.mgt.model.EmailTemplate;

import java.util.List;

public interface EmailTemplateManager {

    /**
     * Add a new template type to tenant registry.
     *
     * @param emailTemplateType
     * @param tenantDomain
     * @throws I18nEmailMgtException
     */
    void addEmailTemplateType(String emailTemplateType,
                              String tenantDomain) throws I18nEmailMgtException;

    /**
     * Delete a template type from tenant registry.
     *
     * @param templateDisplayName
     * @param tenantDomain
     * @throws I18nEmailMgtException
     */
    void deleteEmailTemplateType(String templateDisplayName,
                                 String tenantDomain) throws I18nEmailMgtException;

    /**
     * Get all available template types in the tenant registry.
     *
     * @param tenantDomain
     * @return
     * @throws I18nEmailMgtException
     */
    List<String> getAvailableTemplateTypes(String tenantDomain) throws I18nEmailMgtException;


    /**
     * Add an email template to tenant registry.
     *
     * @param emailTemplate
     * @param tenantDomain
     * @throws I18nEmailMgtException
     */
    void addEmailTemplate(EmailTemplate emailTemplate,
                          String tenantDomain) throws I18nEmailMgtException;


    /**
     * Delete an email template from the tenant registry. Email template is identified with the templateTypeName and
     * localeCode.
     *
     * @param templateTypeName
     * @param localeCode
     * @param tenantDomain
     * @throws I18nEmailMgtException
     */
    void deleteEmailTemplate(String templateTypeName,
                             String localeCode,
                             String tenantDomain) throws I18nEmailMgtException;


    /**
     * Get an email template from tenant registry.
     *
     * @param templateType
     * @param locale
     * @param tenantDomain
     * @return
     * @throws I18nEmailMgtException
     */
    EmailTemplate getEmailTemplate(String templateType,
                                   String locale,
                                   String tenantDomain) throws I18nEmailMgtException;


    /**
     * Get all available email templates in a tenant's registry.
     *
     * @param tenantDomain
     * @return
     * @throws I18nEmailMgtException
     */
    List<EmailTemplate> getAllEmailTemplates(String tenantDomain) throws I18nEmailMgtException;


    /**
     * Add default email templates to a tenant's registry.
     *
     * @param tenantDomain
     * @throws I18nEmailMgtException
     */
    void addDefaultEmailTemplates(String tenantDomain) throws I18nEmailMgtException;

}
