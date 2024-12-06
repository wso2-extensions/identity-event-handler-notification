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
     * Delete all email templates from the tenant registry. Email templates are identified with the templateTypeName and
     * localeCode.
     *
     * @param templateTypeName Email template type name.
     * @param tenantDomain Tenant domain.
     * @throws I18nEmailMgtException If an error occurred while deleting the email templates.
     */
    void deleteEmailTemplates(String templateTypeName, String tenantDomain) throws I18nEmailMgtException;

    /**
     * Delete all email templates from the tenant registry. Email templates are identified with the templateTypeName,
     * localeCode and application UUID.
     *
     * @param templateTypeName Email template type name.
     * @param tenantDomain Tenant domain.
     * @param applicationUuid Application UUID.
     * @throws I18nEmailMgtException If an error occurred while deleting the email templates.
     */
    void deleteEmailTemplates(String templateTypeName,
                              String tenantDomain,
                              String applicationUuid) throws I18nEmailMgtException;


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
     * Get an all email templates of an specific template type, from tenant registry.
     *
     * @param templateDisplayName Email template type displace name.
     * @param tenantDomain Tenant domain.
     * @return A list of email templates that matches to the provided template type.
     * @throws I18nEmailMgtException if an error occurred.
     */
    default List<EmailTemplate> getEmailTemplateType(String templateDisplayName, String tenantDomain)
            throws I18nEmailMgtException {

        throw new I18nEmailMgtException("Method not yet supported");
    }

    /**
     * @deprecated Use {@link #getEmailTemplateType(String, String, String, boolean)} instead.
     * <p>
     * Get all email templates of a specific template type for an application, from tenant registry.
     *
     * @param templateDisplayName Email template type displace name.
     * @param tenantDomain Tenant domain.
     * @param applicationUuid Application UUID.
     * @return A list of email templates that matches to the provided template type.
     * @throws I18nEmailMgtException if an error occurred.
     */
    @Deprecated
    default List<EmailTemplate> getEmailTemplateType(
            String templateDisplayName, String tenantDomain, String applicationUuid) throws I18nEmailMgtException {

        throw new I18nEmailMgtException("Method not implemented");
    }

    /**
     * Retrieves all email templates of a specific type for a given tenant and application UUID.
     * <p>
     * This method validates and fetches email templates based on the specified type. The behavior of the method
     * depends on the {@code resolve} parameter:
     * <ul>
     *     <li><b>Resolved Template (resolve = true):</b> Retrieves the templates resolved through the ancestor
     *     organization hierarchy.</li>
     *     <li><b>Current org's or app's template (resolve = false):</b> Retrieves the templates specific to the
     *     current organization or application.</li>
     * </ul>
     *
     * @param templateDisplayName the display name of the email template type (e.g., "Welcome Email").
     * @param tenantDomain        the tenant domain of the organization to retrieve the template from.
     * @param applicationUuid     the UUID of the application associated with the template.
     * @param resolve             a flag indicating whether to retrieve resolved template ({@code true}) or template
     *                            specific to the current organization or application ({@code false}).
     * @return a list of {@link EmailTemplate} objects matching the specified type and criteria.
     * @throws I18nEmailMgtException if any unexpected error occurs while retrieving email templates.
     */
    default List<EmailTemplate> getEmailTemplateType(String templateDisplayName, String tenantDomain,
                                                     String applicationUuid, boolean resolve)
            throws I18nEmailMgtException {

        throw new I18nEmailMgtException("getEmailTemplateType method not implemented in " + this.getClass().getName());
    }

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
     * @deprecated Adding default templates not needed anymore due to the introduction of
     * {@link org.wso2.carbon.email.mgt.store.SystemDefaultTemplateManager}.
     *
     * @param tenantDomain
     * @throws I18nEmailMgtException
     */
    @Deprecated
    void addDefaultEmailTemplates(String tenantDomain) throws I18nEmailMgtException;

    /**
     * Check whether the given email template type exists in the system.
     *
     * @param templateTypeDisplayName Display name of the template type.
     * @param locale                  Locale of the email template
     * @param tenantDomain            Tenant Domain
     * @return True if the template type exists, false otherwise.
     */
    default boolean isEmailTemplateExists(String templateTypeDisplayName, String locale, String tenantDomain)
            throws I18nEmailMgtException {

        throw new I18nEmailMgtException("Method not yet supported");
    }

    /**
     * Check whether the given email template type exists in the system.
     *
     * @param templateTypeDisplayName Display name of the template type.
     * @param tenantDomain            Tenant Domain
     * @return True if the template type exists, false otherwise.
     */
    default boolean isEmailTemplateTypeExists(String templateTypeDisplayName, String tenantDomain)
            throws I18nEmailMgtException {

        throw new I18nEmailMgtException("Method not yet supported");
    }

    /**
     * Add an application email template to tenant registry.
     *
     * @param emailTemplate Email template to be added.
     * @param tenantDomain Tenant domain.
     * @param applicationUuid Application UUID.
     * @throws I18nEmailMgtException If an error occurred while adding the email template.
     */
    void addEmailTemplate(EmailTemplate emailTemplate,
                          String tenantDomain,
                          String applicationUuid) throws I18nEmailMgtException;

    /**
     * Delete an application email template from the tenant registry. Email template is identified with the
     * templateTypeName, localeCode and application UUID.
     *
     * @param templateTypeName Email template type name.
     * @param localeCode Locale code of the email template.
     * @param tenantDomain Tenant domain.
     * @param applicationUuid Application UUID.
     * @throws I18nEmailMgtException If an error occurred while deleting the email template.
     */
    void deleteEmailTemplate(String templateTypeName,
                             String localeCode,
                             String tenantDomain,
                             String applicationUuid) throws I18nEmailMgtException;

    /**
     * @deprecated Use {@link #getEmailTemplate(String, String, String, String, boolean)} instead.
     * <p>
     * Get an email template from tenant registry with application UUID.
     *
     * @param templateType Email template type.
     * @param locale Locale of the email template.
     * @param tenantDomain Tenant domain.
     * @param applicationUuid Application UUID.
     * @return Email template of the application with fallback to organization template.
     * @throws I18nEmailMgtException If an error occurred while getting the email template.
     */
    @Deprecated
    EmailTemplate getEmailTemplate(String templateType,
                                   String locale,
                                   String tenantDomain,
                                   String applicationUuid) throws I18nEmailMgtException;

    /**
     * Retrieves an email template for a specified type, locale, tenant domain, and application UUID.
     * <p>
     * This method resolves and retrieves an email template based on the provided parameters. The behavior is
     * dependent on the {@code resolve} parameter:
     * <ul>
     *     <li><b>Resolved template (resolve = true):</b> Retrieves the template resolved through the ancestor
     *     organization hierarchy.</li>
     *     <li><b>Current org's or app's template (resolve = false):</b> Retrieves the template specific to the
     *     current organization or application only.</li>
     * </ul>
     *
     * @param templateType    the type of the email template.
     * @param locale          the locale of the email template (e.g., "en_US").
     * @param tenantDomain    the tenant domain of the organization to retrieve the template from.
     * @param applicationUuid the UUID of the application associated with the template.
     * @param resolve         a flag indicating whether to retrieve resolved template ({@code true}) or template
     *                        specific to the current organization or application ({@code false}).
     * @return the {@link EmailTemplate} matching the specified criteria.
     * @throws I18nEmailMgtException if any unexpected error occurs while retrieving email template.
     */
    default EmailTemplate getEmailTemplate(String templateType, String locale, String tenantDomain,
                                           String applicationUuid, boolean resolve) throws I18nEmailMgtException {

        throw new I18nEmailMgtException("getEmailTemplate method not implemented in " + this.getClass().getName());
    }

    /**
     * Check whether the given email template type exists for the application.
     *
     * @param templateTypeDisplayName Display name of the template type.
     * @param locale                  Locale of the email template
     * @param tenantDomain            Tenant Domain
     * @param applicationUuid         Application UUID
     * @return True if the template type exists, false otherwise.
     * @throws I18nEmailMgtException If an error occurred while checking the existence of the email template.
     */
    default boolean isEmailTemplateExists(String templateTypeDisplayName, String locale,
                                          String tenantDomain, String applicationUuid) throws I18nEmailMgtException {

        throw new I18nEmailMgtException("Method not implemented");
    }
}
