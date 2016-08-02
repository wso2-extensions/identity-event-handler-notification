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

package org.wso2.carbon.email.mgt.ui;


import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.email.mgt.model.xsd.EmailTemplate;
import org.wso2.carbon.email.mgt.stub.I18NEmailMgtConfigServiceStub;

public class I18nEmailMgtConfigServiceClient {

    private static Log log = LogFactory.getLog(I18nEmailMgtConfigServiceClient.class);

    private I18NEmailMgtConfigServiceStub stub;

    public I18nEmailMgtConfigServiceClient(String url,
                                           ConfigurationContext configContext) throws Exception {
        try {
            stub = new I18NEmailMgtConfigServiceStub(configContext, url + "I18nEmailMgtConfigService");
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    /**
     * @param cookie        HttpSession cookie
     * @param url           Backend Carbon server URL
     * @param configContext Axis2 Configuration Context
     */
    public I18nEmailMgtConfigServiceClient(String cookie, String url,
                                           ConfigurationContext configContext) throws Exception {
        try {
            stub = new I18NEmailMgtConfigServiceStub(configContext, url + "I18nEmailMgtConfigService");
            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    private String[] handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }

    /**
     * Add an email template type for a tenant. (eg; Account Recovery)
     *
     * @param emailTemplateType
     * @throws AxisFault
     */
    public void addTemplateType(String emailTemplateType) throws AxisFault {
        try {
            stub.addEmailTemplateType(emailTemplateType);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    /**
     * Get all available template types in tenant domain.
     *
     * @return
     * @throws AxisFault
     */
    public String[] getEmailTemplateTypes() throws AxisFault {
        String[] emailTemplateTypes = new String[0];
        try {
            emailTemplateTypes = stub.getEmailTemplateTypes();
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return emailTemplateTypes;
    }

    /**
     * Saves specific Email Template of a given tenant.
     *
     * @param emailTemplate <code>Email Template</code> new Email Template information
     * @throws Exception Error when saving the Email Template.
     */
    public void saveEmailConfig(EmailTemplate emailTemplate) throws AxisFault {
        try {
            stub.saveEmailConfig(emailTemplate);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    /**
     * Adds an Email Template to the given tenant.
     *
     * @param emailTemplate <code>Email Template</code> new Email Template information
     * @throws Exception Error when adding new Email Template information
     */
    public void addEmailConfig(EmailTemplate emailTemplate) throws AxisFault {
        try {
            stub.addEmailConfig(emailTemplate);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    /**
     * Loads set of Email Templates of a given tenant.
     *
     * @throws Exception Error when loading Email Template information
     */
    public EmailTemplate[] loadEmailTemplates() throws AxisFault {
        try {
            return stub.getEmailConfig();
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }

        return new EmailTemplate[0];
    }

    /**
     * Delete an email template from a tenant's registry.
     *
     * @param templateType
     * @param localeCode
     * @throws AxisFault
     */
    public void deleteEmailTemplate(String templateType, String localeCode) throws AxisFault {
        try {
            stub.deleteEmailTemplate(templateType, localeCode);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    /**
     * Delete all templates of a template type from a tenant's registry. This will remove all the email templates for
     * a particular scenario in different locales.
     *
     * @param templateType
     * @throws AxisFault
     */
    public void deleteEmailTemplateType(String templateType) throws AxisFault {
        try {
            stub.deleteEmailTemplateType(templateType);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

}
