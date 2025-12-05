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

package org.wso2.carbon.identity.notification.sender.tenant.config.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.external.api.client.api.exception.APIClientConfigException;
import org.wso2.carbon.identity.external.api.client.api.model.APIClientConfig;
import org.wso2.carbon.identity.external.api.token.handler.api.exception.TokenHandlerException;
import org.wso2.carbon.identity.external.api.token.handler.api.model.GrantContext;
import org.wso2.carbon.identity.external.api.token.handler.api.model.TokenInvocationResult;
import org.wso2.carbon.identity.external.api.token.handler.api.model.TokenRequestContext;
import org.wso2.carbon.identity.external.api.token.handler.api.service.TokenAcquirerService;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.Authentication;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementServerException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;

import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ACCESS_TOKEN_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.REFRESH_TOKEN_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.TOKEN_RETRIEVAL_HTTP_CONNECTION_REQUEST_TIMEOUT_IN_MILLIS;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.TOKEN_RETRIEVAL_HTTP_CONNECTION_TIMEOUT_IN_MILLIS;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.TOKEN_RETRIEVAL_HTTP_READ_TIMEOUT_IN_MILLIS;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.TOKEN_RETRIEVAL_MAX_PER_ROUTE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.TOKEN_RETRIEVAL_POOL_SIZE_TO_BE_SET;


/**
 * Manager class for handling Client Credentials Token acquisition and management.
 */
public class TokenManager {

    private static final Log LOG = LogFactory.getLog(TokenManager.class);
    private static final TokenManager tokenManager = new TokenManager();

    private final TokenAcquirerService tokenAcquirerService;

    private TokenManager() {

        try {
            this.tokenAcquirerService = new TokenAcquirerService(buildAPIClientConfig());
        } catch (APIClientConfigException e) {
            throw new RuntimeException("Error initializing TokenManager.", e);
        }
    }

    public static TokenManager getInstance() {

        return tokenManager;
    }

    /**
     * Retrieve a new access token using the refresh token and update the authentication object.
     *
     * @param authentication The authentication object containing necessary properties.
     * @throws NotificationSenderManagementServerException If an error occurs during token retrieval.
     */
    public void getNewAccessToken(Authentication authentication) throws NotificationSenderManagementServerException {

        TokenRequestContext tokenRequestContext = buildTokenRequestContext(authentication);
        tokenAcquirerService.setTokenRequestContext(tokenRequestContext);

        try {
            TokenInvocationResult tokenInvocationResult;
            tokenInvocationResult = tokenAcquirerService
                    .getNewAccessToken(authentication.getInternalProperties().get(REFRESH_TOKEN_PROP));
            handleTokenInvocationResult(tokenInvocationResult, authentication);
        } catch (TokenHandlerException e) {
            throw new NotificationSenderManagementServerException(
                    ErrorMessage.ERROR_CODE_ERROR_WHILE_RETRIEVING_TOKEN, null, e);
        }
    }

    private void handleTokenInvocationResult(TokenInvocationResult tokenInvocationResult, Authentication authentication)
            throws NotificationSenderManagementServerException {

        if (tokenInvocationResult.getStatus() != TokenInvocationResult.Status.SUCCESS) {
            throw new NotificationSenderManagementServerException(
                    ErrorMessage.ERROR_CODE_ERROR_WHILE_RETRIEVING_TOKEN, null);
        }
        authentication.addInternalProperty(
                ACCESS_TOKEN_PROP, tokenInvocationResult.getTokenResponse().getAccessToken());
        authentication.addInternalProperty(
                REFRESH_TOKEN_PROP, tokenInvocationResult.getTokenResponse().getRefreshToken());
    }

    private TokenRequestContext buildTokenRequestContext(Authentication authentication)
            throws NotificationSenderManagementServerException {

        if (authentication.getType() != Authentication.Type.CLIENT_CREDENTIAL) {
            throw new NotificationSenderManagementServerException(
                    ErrorMessage.ERROR_CODE_ERROR_UNSUPPORTED_AUTH_TYPE_FOR_TOKEN_RETRIEVAL, null);
        }

        try {
            Map<String, String> grantTypeProperties = new HashMap<>();
            grantTypeProperties.put(
                    GrantContext.Property.CLIENT_ID.getName(),
                    authentication.getProperty(Authentication.Property.CLIENT_ID.getName()));
            grantTypeProperties.put(
                    GrantContext.Property.CLIENT_SECRET.getName(),
                    authentication.getProperty(Authentication.Property.CLIENT_SECRET.getName()));
            grantTypeProperties.put(
                    GrantContext.Property.SCOPE.getName(),
                    authentication.getProperty(Authentication.Property.SCOPE.getName()));

            GrantContext grantContext = new GrantContext.Builder()
                    .grantType(GrantContext.GrantType.CLIENT_CREDENTIAL)
                    .properties(grantTypeProperties)
                    .build();

            TokenRequestContext.Builder builder = new TokenRequestContext.Builder()
                    .grantContext(grantContext)
                    .endpointUrl(authentication.getProperty(Authentication.Property.TOKEN_ENDPOINT.getName()));

            return builder.build();
        } catch (TokenHandlerException e) {
            throw new NotificationSenderManagementServerException(
                    ErrorMessage.ERROR_CODE_ERROR_WHILE_TOKEN_REQUEST_BUILDING, null, e);
        }
    }

    private APIClientConfig buildAPIClientConfig() throws APIClientConfigException {

        APIClientConfig.Builder builder = new APIClientConfig.Builder();

        applyIfPositive(builder::httpConnectionTimeoutInMillis, TOKEN_RETRIEVAL_HTTP_CONNECTION_TIMEOUT_IN_MILLIS);
        applyIfPositive(builder::httpReadTimeoutInMillis, TOKEN_RETRIEVAL_HTTP_READ_TIMEOUT_IN_MILLIS);
        applyIfPositive(builder::httpConnectionRequestTimeoutInMillis,
                TOKEN_RETRIEVAL_HTTP_CONNECTION_REQUEST_TIMEOUT_IN_MILLIS);
        applyIfPositive(builder::poolSizeToBeSet, TOKEN_RETRIEVAL_POOL_SIZE_TO_BE_SET);
        applyIfPositive(builder::defaultMaxPerRoute, TOKEN_RETRIEVAL_MAX_PER_ROUTE);

        return builder.build();
    }

    private void applyIfPositive(IntConsumer setter, String propertyKey) {

        int value = getIntProperty(propertyKey);
        if (value > 0) {
            setter.accept(value);
        }
    }

    private int getIntProperty(String key) {

        String configValue = IdentityUtil.getProperty(key);
        if (StringUtils.isBlank(configValue)) {
            return -1;
        }

        try {
            return Integer.parseInt(configValue.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
