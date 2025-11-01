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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ACCESS_TOKEN_AUTH_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CLIENT_ID_AUTH_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CLIENT_SECRET_AUTH_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.HEADER_AUTH_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PASSWORD_AUTH_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SCOPE_AUTH_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.USERNAME_AUTH_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.VALUE_AUTH_PROP;

/**
 * Authentication configuration for the notification sending provider.
 */
public class Authentication {

    private final Type type;
    private final List<AuthProperty> authProperties;

    public Authentication(AuthenticationBuilder builder) {

        type = builder.authType;
        authProperties = builder.resolvedAuthProperties;
    }

    public Authentication.Type getType() {

        return type;
    }

    public List<AuthProperty> getProperties() {

        return authProperties;
    }

    public AuthProperty getProperty(String propertyName) {

        return this.authProperties.stream()
                .filter(property -> propertyName.equals(property.getName()))
                .findFirst()
                .orElse(null);
    }

    public void setInternalAuthProperty(String key, String value) {

        authProperties.add(new AuthProperty.Builder(key, value, AuthProperty.Scope.INTERNAL).build());
    }

    /**
     * This Builder build endpoint by taking the authentication type and properties as input.
     */
    public static class AuthenticationBuilder {

        private final Type authType;
        private final Map<String, String> propertiesMap;
        private final List<AuthProperty> resolvedAuthProperties = new ArrayList<>();

        public AuthenticationBuilder (String type, Map<String, String> authPropertiesMap) {

            this.authType = Type.valueOfName(type);
            this.propertiesMap = authPropertiesMap;
        }

        public AuthenticationBuilder setInternalAuthProperty(String key, String value) {

            resolvedAuthProperties.add(new AuthProperty.Builder(key, value, AuthProperty.Scope.INTERNAL).build());
            return this;
        }

        public Authentication build() {

            switch (authType) {
                case BASIC:
                    resolvedAuthProperties.add(getProperty(Type.BASIC, propertiesMap, USERNAME_AUTH_PROP));
                    resolvedAuthProperties.add(getProperty(Type.BASIC, propertiesMap, PASSWORD_AUTH_PROP));
                    break;
                case BEARER:
                    resolvedAuthProperties.add(getProperty(Type.BEARER, propertiesMap, ACCESS_TOKEN_AUTH_PROP));
                    break;
                case CLIENT_CREDENTIAL:
                    resolvedAuthProperties.add(getProperty(Type.CLIENT_CREDENTIAL, propertiesMap, CLIENT_ID_AUTH_PROP));
                    resolvedAuthProperties.add(
                            getProperty(Type.CLIENT_CREDENTIAL, propertiesMap, CLIENT_SECRET_AUTH_PROP));
                    resolvedAuthProperties.add(getProperty(Type.CLIENT_CREDENTIAL, propertiesMap, SCOPE_AUTH_PROP));
                    break;
                case API_KEY:
                    resolvedAuthProperties.add(getProperty(Type.BEARER, propertiesMap, HEADER_AUTH_PROP));
                    resolvedAuthProperties.add(getProperty(Type.BEARER, propertiesMap, VALUE_AUTH_PROP));
                    break;
                case NONE:
                    break;
                default:
                    throw new IllegalArgumentException(String.format("An invalid authentication type '%s' is " +
                            "provided for the authentication configuration of the endpoint.", authType.name()));
            }
            return new Authentication(this);
        }

        private AuthProperty getProperty(Type authType, Map<String, String> actionEndpointProperties,
                                   String propName) {

            if (actionEndpointProperties != null && actionEndpointProperties.containsKey(propName)) {
                String propValue = actionEndpointProperties.get(propName);
                if (StringUtils.isNotBlank(propValue)) {
                    return new AuthProperty.Builder(propName, propValue, AuthProperty.Scope.EXTERNAL).build();
                }
                throw new IllegalArgumentException(String.format("The Property %s cannot be blank.", propName));
            }

            throw new NoSuchElementException(String.format("The property %s must be provided as an authentication " +
                    "property for the %s authentication type.", propName, authType.name()));
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

        public static Type valueOfName(String name) {

            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Authentication type cannot be null or empty.");
            }

            for (Type type : Type.values()) {
                if (type.name.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid authentication type: " + name);
        }
    }
}
