/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementClientException;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementException;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementServerException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ACCESS_TOKEN_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.AUTH_HEADER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.dto.Authentication.Property.ACCESS_TOKEN;

/**
 * Authentication configuration for the notification sending provider.
 */
public class Authentication {

    private static final Log LOG = LogFactory.getLog(Authentication.class);
    private final Type type;
    private final Map<String, String> authProperties;
    private final Map<String, String> internalAuthProperties;
    private Header authHeader;

    public Authentication(AuthenticationBuilder builder) throws NotificationSenderManagementServerException {

        type = builder.authType;
        authProperties = builder.resolvedAuthProperties;
        internalAuthProperties = builder.internalAuthProperties;
    }

    public Authentication.Type getType() {

        return type;
    }

    public Map<String, String> getProperties() {

        return authProperties;
    }

    public String getProperty(String propertyName) {

        return authProperties.get(propertyName);
    }

    public void addInternalProperty(String propKey, String propValue) {

        internalAuthProperties.put(propKey, propValue);
    }

    public Map<String, String> getInternalProperties() {

        return internalAuthProperties;
    }

    public Header getAuthHeader() throws NotificationSenderManagementServerException {

        if (authHeader != null) {
            return authHeader;
        }

        // move the logic this class as private method
        authHeader = buildAuthenticationHeader();
        return authHeader;
    }

    /**
     * Build authentication header.
     *
     * @return Header object.
     */
    public Header buildAuthenticationHeader() {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Building authentication header for auth type: " + type);
        }

        switch (type) {
            case BASIC:
                String credentials = authProperties.get(Property.USERNAME.getName()) + ":" +
                        authProperties.get(Property.PASSWORD.getName());
                byte[] encodedBytes = Base64.getEncoder().encode(credentials.getBytes(StandardCharsets.UTF_8));
                return new org.apache.http.message.BasicHeader(
                        AUTH_HEADER,
                        "Basic " + new String(encodedBytes, StandardCharsets.UTF_8));
            case CLIENT_CREDENTIAL:
                if (internalAuthProperties.get(ACCESS_TOKEN_PROP) == null) {
                    return null;
                }
                return new BasicHeader(
                        AUTH_HEADER,
                        "Bearer " + internalAuthProperties.get(ACCESS_TOKEN_PROP)
                );
            case BEARER:
                return new BasicHeader(
                        AUTH_HEADER,
                        "Bearer " + authProperties.get(ACCESS_TOKEN.getName())
                );
            case API_KEY:
                return new BasicHeader(
                        authProperties.get(Property.HEADER.getName()),
                        authProperties.get(Property.VALUE.getName())
                );
            default:
                return null;
        }
    }

    /**
     * This Builder build endpoint by taking the authentication type and properties as input.
     */
    public static class AuthenticationBuilder {

        private final Type authType;
        private final Map<String, String> propertiesMap;
        private final Map<String, String> resolvedAuthProperties = new HashMap<>();
        private final Map<String, String> internalAuthProperties = new HashMap<>();

        public AuthenticationBuilder (String type, Map<String, String> authPropertiesMap)
                throws NotificationSenderManagementClientException {

            this.authType = Type.valueOfName(type);
            this.propertiesMap = authPropertiesMap;
        }

        public Authentication build() throws NotificationSenderManagementException {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Building authentication configuration for type: " + authType);
            }

            switch (authType) {
                case BASIC:
                    resolvedAuthProperties.put(Property.USERNAME.getName(), getProperty(Property.USERNAME));
                    resolvedAuthProperties.put(Property.PASSWORD.getName(), getProperty(Property.PASSWORD));
                    break;
                case BEARER:
                    resolvedAuthProperties.put(Property.ACCESS_TOKEN.getName(), getProperty(Property.ACCESS_TOKEN));
                    break;
                case CLIENT_CREDENTIAL:
                    resolvedAuthProperties.put(Property.CLIENT_ID.getName(), getProperty(Property.CLIENT_ID));
                    resolvedAuthProperties.put(Property.CLIENT_SECRET.getName(), getProperty(Property.CLIENT_SECRET));
                    resolvedAuthProperties.put(Property.SCOPE.getName(), getProperty(Property.SCOPE));
                    resolvedAuthProperties.put(
                            Property.TOKEN_ENDPOINT.getName(), getProperty(Property.TOKEN_ENDPOINT));
                    break;
                case API_KEY:
                    resolvedAuthProperties.put(Property.HEADER.getName(), getProperty(Property.HEADER));
                    resolvedAuthProperties.put(Property.VALUE.getName(), getProperty(Property.VALUE));
                    break;
                case NONE:
                    break;
            }
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("Successfully built authentication configuration for type: " + authType);
            }
            return new Authentication(this);
        }

        private String getProperty(Property propName) throws NotificationSenderManagementClientException {

            if (propertiesMap != null && propertiesMap.containsKey(propName.getName())) {
                String propValue = propertiesMap.get(propName.getName());
                if (StringUtils.isNotBlank(propValue)) {
                    return propValue;
                }
                LOG.error("Authentication property '" + propName.getName() + "' is blank.");
                throw new NotificationSenderManagementClientException(
                        ErrorMessage.ERROR_CODE_BLANK_AUTH_PROPERTY, propName.getName());
            }

            LOG.error("Required authentication property '" + propName.getName() + "' is missing.");
            throw new NotificationSenderManagementClientException(
                    ErrorMessage.ERROR_CODE_MISSING_AUTH_PROPERTY, propName.getName());
        }
    }

    /**
     * Authentication Type.
     */
    public enum Type {

        NONE("NONE"),
        BEARER("BEARER"),
        CLIENT_CREDENTIAL("CLIENT_CREDENTIAL"),
        BASIC("BASIC"),
        API_KEY("API_KEY");

        private final String name;

        Type(String name) {

            this.name = name;
        }

        public String getName() {

            return name;
        }

        public static Type valueOfName(String name) throws NotificationSenderManagementClientException {

            if (name == null || name.isEmpty()) {
                LOG.error("Authentication type is missing or empty.");
                throw new NotificationSenderManagementClientException(ErrorMessage.ERROR_CODE_MISSING_AUTH_TYPE, name);
            }

            for (Type type : Type.values()) {
                if (type.name.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            LOG.error("Unsupported authentication type: " + name);
            throw new NotificationSenderManagementClientException(ErrorMessage.ERROR_CODE_UNSUPPORTED_AUTH_TYPE, name);
        }
    }

    /**
     * Authentication Property Enum.
     */
    public enum Property {

        USERNAME("username"),
        PASSWORD("password"),
        HEADER("header"),
        VALUE("value"),
        ACCESS_TOKEN("accessToken"),
        CLIENT_ID("clientId"),
        CLIENT_SECRET("clientSecret"),
        SCOPE("scopes"),
        TOKEN_ENDPOINT("tokenEndpoint");

        private final String name;

        Property(String name) {

            this.name = name;
        }

        public String getName() {

            return name;
        }

        public static Property valueOfName(String name) {

            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Authentication type cannot be null or empty.");
            }

            for (Property property : Property.values()) {
                if (property.name.equalsIgnoreCase(name)) {
                    return property;
                }
            }
            throw new IllegalArgumentException("Invalid authentication type: " + name);
        }
    }
}
