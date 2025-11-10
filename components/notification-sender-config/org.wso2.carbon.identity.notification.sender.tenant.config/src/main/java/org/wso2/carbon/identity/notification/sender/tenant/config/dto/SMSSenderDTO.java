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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.Authentication.Property;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementClientException;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementException;

import java.util.HashMap;
import java.util.Map;

import static org.wso2.carbon.identity.notification.sender.tenant.config.dto.Authentication.Type.BASIC;

/**
 * DTO for SMS sender.
 */
public class SMSSenderDTO {

    private static final Log LOG = LogFactory.getLog(SMSSenderDTO.class);
    private String name;
    private String provider;
    private String providerURL;
    private String key;
    private String secret;
    private String sender;
    private String contentType;
    private Map<String, String> properties = new HashMap<>();
    private Authentication authentication = null;

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

        public Builder addAuthProperty(String key, String value) {

            authProperties.put(key, value);
            return this;
        }

        public SMSSenderDTO build() throws NotificationSenderManagementException {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Building SMSSenderDTO for provider: " + this.provider);
            }

            SMSSenderDTO smsSenderDTO = new SMSSenderDTO();
            smsSenderDTO.setAuthentication(buildAuthConfig());
            smsSenderDTO.setName(this.name);
            smsSenderDTO.setProvider(this.provider);
            smsSenderDTO.setProviderURL(this.providerURL);
            smsSenderDTO.setKey(this.key);
            smsSenderDTO.setSecret(this.secret);
            smsSenderDTO.setSender(this.sender);
            smsSenderDTO.setContentType(this.contentType);
            smsSenderDTO.setProperties(this.properties);
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("Successfully built SMSSenderDTO for provider: " + this.provider);
            }
            return smsSenderDTO;
        }

        private Authentication buildAuthConfig() throws NotificationSenderManagementException {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Building authentication configuration for SMS sender. Auth type: " + authType);
            }

            Authentication authentication = null;
            if (StringUtils.isNotEmpty(authType)) {
                 authentication = new Authentication.AuthenticationBuilder(authType, authProperties).build();
            }

            if (authentication != null) {
                boolean isBasic = BASIC.equals(authentication.getType());

                // Non-basic: key or secret updates not allowed
                if (!isBasic) {
                    if (StringUtils.isNotEmpty(key) || StringUtils.isNotEmpty(secret)) {
                        LOG.error("Key or secret updates not allowed for non-BASIC authentication type: " +
                                authentication.getType());
                        throw new NotificationSenderManagementClientException(
                                ErrorMessage.ERROR_CODE_CHANNEL_TYPE_UPDATE_NOT_ALLOWED);
                    }
                    return authentication;
                }

                // BASIC: validate or assign key and secret
                String existingKey = authentication.getProperty(Property.USERNAME.getName());
                String existingSecret = authentication.getProperty(Property.PASSWORD.getName());

                if (StringUtils.isNotBlank(key) && !StringUtils.equals(key, existingKey)) {
                    LOG.error("Key update not allowed for BASIC authentication. Attempted to change existing key.");
                    throw new NotificationSenderManagementClientException(
                            ErrorMessage.ERROR_CODE_CHANNEL_TYPE_UPDATE_NOT_ALLOWED);
                }
                if (StringUtils.isBlank(key)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Key not provided. Using existing key from authentication configuration.");
                    }
                    key(existingKey);
                }

                if (StringUtils.isNotBlank(secret) && !StringUtils.equals(secret, existingSecret)) {
                    LOG.error("Secret update not allowed for BASIC authentication. " +
                            "Attempted to change existing secret.");
                    throw new NotificationSenderManagementClientException(
                            ErrorMessage.ERROR_CODE_CHANNEL_TYPE_UPDATE_NOT_ALLOWED);
                }
                if (StringUtils.isBlank(secret)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Secret not provided. Using existing secret from authentication configuration.");
                    }
                    secret(existingSecret);
                }

                return authentication;
            }

            // If authType not provided, then creating BASIC if key and secret provided
            if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(secret)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Auth type not provided. Creating BASIC authentication with provided key and secret.");
                }
                Map<String, String> authProps = new HashMap<>();
                authProps.put(Property.USERNAME.getName(), key);
                authProps.put(Property.PASSWORD.getName(), secret);
                return new Authentication.AuthenticationBuilder(BASIC.toString(), authProps).build();
            }
            return null;
        }
    }
}
