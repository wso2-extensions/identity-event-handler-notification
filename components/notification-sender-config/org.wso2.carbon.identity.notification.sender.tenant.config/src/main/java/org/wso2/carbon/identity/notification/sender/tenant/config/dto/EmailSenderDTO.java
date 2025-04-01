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
 * DTO for Email sender.
 */
public class EmailSenderDTO {
  
    private String name;
    private String smtpServerHost;
    private Integer smtpPort;
    private String fromAddress;
    private String username;
    private String password;
    private String authType;
    private Map<String, String> properties = new HashMap<>();

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getSmtpServerHost() {

        return smtpServerHost;
    }

    public void setSmtpServerHost(String smtpServerHost) {

        this.smtpServerHost = smtpServerHost;
    }

    public Integer getSmtpPort() {

        return smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {

        this.smtpPort = smtpPort;
    }

    public String getFromAddress() {

        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {

        this.fromAddress = fromAddress;
    }

    public String getUsername() {

        return username;
    }

    public void setUsername(String username) {

        this.username = username;
    }

    public String getPassword() {

        return password;
    }

    public void setPassword(String password) {

        this.password = password;
    }


    public Map<String, String> getProperties() {

        return properties;
    }

    public void setProperties(Map<String, String> properties) {

        this.properties = properties;
    }

    public String getAuthType() {

        return authType;
    }

    public void setAuthType(String authType) {

        this.authType = authType;
    }
}
