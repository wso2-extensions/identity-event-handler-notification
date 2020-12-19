/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

import org.wso2.carbon.email.mgt.model.SMSProviderTemplate;

import java.util.List;

/**
 * OSGi service for SMS providers payload template management.
 */
public interface SMSProviderPayloadTemplateManager  {

    /**
     * List all SMS providers payload templates.
     *
     * @return A list of SMSProviderTemplates.
     */
    List<SMSProviderTemplate> listSMSProviderPayloadTemplates();

    /**
     * Get a SMS provider's sms send API payload template by provider name.
     *
     * @param provider SMS provider name.
     * @return SMSProviderTemplate.
     */
    SMSProviderTemplate getSMSProviderPayloadTemplateByProvider(String provider);

}
