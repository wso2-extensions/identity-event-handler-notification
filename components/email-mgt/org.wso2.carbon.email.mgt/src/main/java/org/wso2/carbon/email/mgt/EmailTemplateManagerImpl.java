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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;

/**
 * Provides functionality to manage email templates used in notification emails.
 */
public class EmailTemplateManagerImpl implements EmailTemplateManager {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateManagerImpl.class);

    @Override
    public EmailTemplate getEmailTemplate(String locale, String templateType) throws I18nEmailMgtException {
        EmailTemplate emailTemplate = I18nEmailUtil.getTemplateCollectionMap().get(locale).get(templateType);
        if (emailTemplate == null) {
            throw new I18nEmailMgtException("Can not find the email template type: "
                    + templateType + " with locale: " + locale);
        }
        return emailTemplate;
    }
}
