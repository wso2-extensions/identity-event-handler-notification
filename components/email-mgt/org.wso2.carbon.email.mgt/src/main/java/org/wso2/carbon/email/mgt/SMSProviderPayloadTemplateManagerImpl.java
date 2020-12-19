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

import org.wso2.carbon.email.mgt.internal.SMSProviderPayloadTemplateDataHolder;
import org.wso2.carbon.email.mgt.model.SMSProviderTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationTemplateManager;

import java.util.List;

/**
 * SMS providers SMS send API payload template management implementation class.
 */
public class SMSProviderPayloadTemplateManagerImpl implements SMSProviderPayloadTemplateManager,
        NotificationTemplateManager {

    @Override
    public List<SMSProviderTemplate> listSMSProviderPayloadTemplates() {

        return SMSProviderPayloadTemplateDataHolder.getInstance().getSmsProvidersAPIPayloads();
    }

    @Override
    public SMSProviderTemplate getSMSProviderPayloadTemplateByProvider(String provider) {

        return SMSProviderPayloadTemplateDataHolder.getInstance().getSmsProvidersAPIPayloads().stream()
                .filter(smsProviderTemplate -> smsProviderTemplate.getProvider().equals(provider)).findAny()
                .orElse(null);
    }
}
