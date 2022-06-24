/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
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

package org.wso2.carbon.identity.notification.sender.tenant.config.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO for SMS sender.
 */
public class SMSSenderDTO {
  
    private String name;
    private String provider;
    private String providerURL;
    private String key;
    private String secret;
    private String sender;
    private Map<String, String> properties = new HashMap<>();

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getProvider() {

        return provider;
    }

    public void setProvider(String provider) {

        this.provider = provider;
    }

    public String getProviderURL() {

        return providerURL;
    }

    public void setProviderURL(String providerURL) {

        this.providerURL = providerURL;
    }

    public String getKey() {

        return key;
    }

    public void setKey(String key) {

        this.key = key;
    }

    public String getSecret() {

        return secret;
    }

    public void setSecret(String secret) {

        this.secret = secret;
    }

    public String getSender() {

        return sender;
    }

    public void setSender(String sender) {

        this.sender = sender;
    }

    public Map<String, String> getProperties() {

        return properties;
    }

    public void setProperties(Map<String, String> properties) {

        this.properties = properties;
    }
}
