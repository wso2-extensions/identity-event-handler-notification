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
package org.wso2.carbon.email.mgt.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.email.mgt.EmailTemplateManager;
import org.wso2.carbon.email.mgt.EmailTemplateManagerImpl;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;

/**
 * Email mgt service.
 */
@Component(
        name = "org.wso2.carbon.email.mgt.internal.I18nMgtServiceComponent",
        immediate = true,
        property = { "componentName=wso2-carbon-email-mgt" })
public class I18nMgtServiceComponent {

    private static Logger log = LoggerFactory.getLogger(I18nMgtServiceComponent.class);

    private I18nMgtDataHolder dataHolder = I18nMgtDataHolder.getInstance();

    @Activate
    protected void activate(ComponentContext context) {
        try {
            BundleContext bundleCtx = context.getBundleContext();

            // Register Email Mgt Service as an OSGi service
            ServiceRegistration emailTemplateSR = bundleCtx
                    .registerService(EmailTemplateManager.class.getName(), new EmailTemplateManagerImpl(), null);

            if (emailTemplateSR != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Email Template Mgt Service registered.");
                }
            } else {
                log.error("Error registering Email Template Mgt Service.");
            }

            // load default email templates
            I18nEmailUtil.buildEmailTemplates();

            log.debug("I18n Management is activated");
        } catch (Throwable e) {
            log.error("Error while activating I18n Management bundle", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (log.isDebugEnabled()) {
            log.debug("I18n Management bundle is de-activated");
        }
    }
}
