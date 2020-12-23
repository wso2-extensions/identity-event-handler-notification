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

package org.wso2.carbon.email.mgt.internal;

import org.wso2.carbon.email.mgt.model.SMSProviderTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Data holder for SMS providers' send sms API payload templates.
 */
public class SMSProviderPayloadTemplateDataHolder {

    private static SMSProviderPayloadTemplateDataHolder instance = new SMSProviderPayloadTemplateDataHolder();
    private List<SMSProviderTemplate> smsProvidersAPIPayloads = new ArrayList<>();

    private SMSProviderPayloadTemplateDataHolder() {

    }

    public static SMSProviderPayloadTemplateDataHolder getInstance() {

        return instance;
    }

    /**
     * Set SMS send API payloads for default SMS providers.
     *
     * @param smsProvidersAPIPayloads List of SMS send API payloads for default SMS providers.
     */
    public void setSMSProvidersAPIPayloads(List<SMSProviderTemplate> smsProvidersAPIPayloads) {

        this.smsProvidersAPIPayloads = smsProvidersAPIPayloads;
    }

    /**
     * Get SMS send API payloads for default SMS providers.
     *
     * @return A list of SMS send API payloads for default SMS providers.
     */
    public List<SMSProviderTemplate> getSmsProvidersAPIPayloads() {

        return smsProvidersAPIPayloads;
    }
}
