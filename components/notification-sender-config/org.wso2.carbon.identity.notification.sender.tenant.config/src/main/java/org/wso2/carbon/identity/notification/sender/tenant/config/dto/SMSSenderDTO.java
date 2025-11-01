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

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementException;
import org.wso2.carbon.identity.notification.sender.tenant.config.utils.TokenManager;

import java.util.HashMap;
import java.util.Map;

import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PASSWORD_AUTH_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.USERNAME_AUTH_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.dto.Authentication.Type.BASIC;


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
    private String contentType;
    private Map<String, String> properties = new HashMap<>();
    private Authentication authentication;

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

    public String getContentType() {

        return contentType;
    }

    public void setContentType(String contentType) {

        this.contentType = contentType;
    }

    public Map<String, String> getProperties() {

        return properties;
    }

    public void setProperties(Map<String, String> properties) {

        this.properties = properties;
    }

    public void setAuthentication(Authentication authentication) {

        this.authentication = authentication;
    }

    public Authentication getAuthentication() {

        return authentication;
    }

    public void getToken() {

        // check allowed grant to call
        TokenManager tokenManager = new TokenManager();
        Map<String, String> authProps = new HashMap<>();
        authProps.put("client_id", "TESTID");
        authProps.put("client_secret", "TESTSECRET");
        authProps.put("scope", "testing");
        Authentication tempAuth = new Authentication.AuthenticationBuilder(
                Authentication.Type.CLIENT_CREDENTIAL.name(), authProps).build();
        tokenManager.setAuthentication(tempAuth);
        try {
            tokenManager.getToken();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while acquiring token for the SMS sender", e);
        }
    }

    /**
     * Builder for SMSSenderDTO.
     */
    public static class Builder {

        private String name;
        private String provider;
        private String providerURL;
        private String key;
        private String secret;
        private String sender;
        private String contentType;
        private final Map<String, String> properties = new HashMap<>();
        private String authType;
        private final Map<String, String> authProperties = new HashMap<>();

        public Builder name(String name) {

            this.name = name;
            return this;
        }

        public Builder provider(String provider) {

            this.provider = provider;
            return this;
        }

        public Builder providerURL(String providerURL) {

            this.providerURL = providerURL;
            return this;
        }

        public Builder key(String key) {

            this.key = key;
            return this;
        }

        public Builder secret(String secret) {

            this.secret = secret;
            return this;
        }

        public Builder sender(String sender) {

            this.sender = sender;
            return this;
        }

        public Builder contentType(String contentType) {

            this.contentType = contentType;
            return this;
        }

        public Builder addProperty(String key, String value) {

            properties.put(key, value);
            return this;
        }

        public Builder authType(String authType) {

            this.authType = authType;
            return this;
        }

        public Builder addAuthProperties(String key, String value) {

            authProperties.put(key, value);
            return this;
        }

        public SMSSenderDTO build() throws NotificationSenderManagementException {

            SMSSenderDTO smsSenderDTO = new SMSSenderDTO();
            if (StringUtils.isNotEmpty(authType)) {
                Authentication authentication = new Authentication.AuthenticationBuilder(
                        authType, authProperties).build();
                validateAuthConfig(authentication);
                smsSenderDTO.setAuthentication(authentication);
            }

            smsSenderDTO.setName(this.name);
            smsSenderDTO.setProvider(this.provider);
            smsSenderDTO.setProviderURL(this.providerURL);
            smsSenderDTO.setKey(this.key);
            smsSenderDTO.setSecret(this.secret);
            smsSenderDTO.setSender(this.sender);
            smsSenderDTO.setContentType(this.contentType);
            smsSenderDTO.setProperties(this.properties);
            return smsSenderDTO;
        }

        private void validateAuthConfig(Authentication authentication) throws NotificationSenderManagementException {

            /* If key and secret are not provided in the DTO, fetch them from the authentication
             configuration for only if the BASIC auth type is configured. */
            if (StringUtils.isEmpty(key) && StringUtils.isEmpty(secret)) {
                if (!BASIC.equals(authentication.getType())) {
                    return;
                }
                key(authentication.getProperty(USERNAME_AUTH_PROP).getValue());
                secret(authentication.getProperty(PASSWORD_AUTH_PROP).getValue());
            }

            if (!StringUtils.equals(key, authentication.getProperty(USERNAME_AUTH_PROP).getValue()) ||
                    !StringUtils.equals(secret, authentication.getProperty(PASSWORD_AUTH_PROP).getValue())) {
                throw new NotificationSenderManagementException(
                        NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_CHANNEL_TYPE_UPDATE_NOT_ALLOWED);
            }
        }
    }
}
