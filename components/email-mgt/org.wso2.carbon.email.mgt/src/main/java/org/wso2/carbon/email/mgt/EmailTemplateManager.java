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

/**
 * Email manager operations.
 */
public interface EmailTemplateManager {

    /**
     * Get email template by given template type
     * @param locale the locale of the email type
     * @param templateType type of the template
     * @return email template
     * @throws I18nEmailMgtException
     */
    EmailTemplate getEmailTemplate(String locale, String templateType) throws I18nEmailMgtException;
}
