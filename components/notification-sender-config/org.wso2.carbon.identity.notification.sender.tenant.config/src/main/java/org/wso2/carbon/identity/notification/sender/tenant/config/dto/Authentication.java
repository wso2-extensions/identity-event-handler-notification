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
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ACCESS_TOKEN_AUTH_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CLIENT_ID_AUTH_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CLIENT_SECRET_AUTH_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.HEADER_AUTH_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PASSWORD_AUTH_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.USERNAME_AUTH_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.VALUE_AUTH_PROP;

/**
 * Authentication class which hold supported authentication types and their properties.
 */
public class Authentication {

    private final Type type;
    private final Map<String, String> authProperties;

    public Authentication(AuthenticationBuilder builder) {

        type = builder.authType;
        authProperties = builder.propertiesMap;
    }

    public Authentication.Type getType() {

        return type;
    }

    public Map<String, String> getProperties() {

        return authProperties;
    }

    /**
     * This Builder build endpoint by taking the authentication type and properties as input.
     */
    public static class AuthenticationBuilder {

        private final Type authType;
        private Map<String, String> propertiesMap;

        public AuthenticationBuilder (String type, Map<String, String> authPropertiesMap) {

            this.authType = Type.valueOfName(type);
            this.propertiesMap = authPropertiesMap;
        }

        public Authentication build() {

            Map<String, String> authProperties = new HashMap<>();
            switch (authType) {
                case BASIC:
                    authProperties.put(USERNAME_AUTH_PROP, getProperty(Type.BASIC, propertiesMap, USERNAME_AUTH_PROP));
                    authProperties.put(PASSWORD_AUTH_PROP, getProperty(Type.BASIC, propertiesMap, PASSWORD_AUTH_PROP));
                    break;
                case BEARER:
                    authProperties.put(ACCESS_TOKEN_AUTH_PROP,
                            getProperty(Type.BEARER, propertiesMap, ACCESS_TOKEN_AUTH_PROP));
                    break;
                case CLIENT_CRED:
                    authProperties.put(CLIENT_ID_AUTH_PROP,
                            getProperty(Type.CLIENT_CRED, propertiesMap, CLIENT_ID_AUTH_PROP));
                    authProperties.put(CLIENT_SECRET_AUTH_PROP,
                            getProperty(Type.CLIENT_CRED, propertiesMap, CLIENT_SECRET_AUTH_PROP));
                    break;
                case API_KEY:
                    authProperties.put(HEADER_AUTH_PROP, getProperty(Type.BEARER, propertiesMap, HEADER_AUTH_PROP));
                    authProperties.put(VALUE_AUTH_PROP, getProperty(Type.BEARER, propertiesMap, VALUE_AUTH_PROP));
                    break;
                case NONE:
                    break;
                default:
                    throw new IllegalArgumentException(String.format("An invalid authentication type '%s' is " +
                            "provided for the authentication configuration of the endpoint.", authType.name()));
            }
            propertiesMap = authProperties;
            return new Authentication(this);
        }

        private String getProperty(Type authType, Map<String, String> actionEndpointProperties,
                                   String propertyName) {

            if (actionEndpointProperties != null && actionEndpointProperties.containsKey(propertyName)) {
                String propValue = actionEndpointProperties.get(propertyName);
                if (StringUtils.isNotBlank(propValue)) {
                    return propValue;
                }
                throw new IllegalArgumentException(String.format("The Property %s cannot be blank.", propertyName));
            }

            throw new NoSuchElementException(String.format("The property %s must be provided as an authentication " +
                    "property for the %s authentication type.", propertyName, authType.name()));
        }
    }

    /**
     * Authentication Type.
     */
    public enum Type {

        NONE("NONE"),
        BEARER("BEARER"),
        CLIENT_CRED("CLIENT_CREDENTIAL"),
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
