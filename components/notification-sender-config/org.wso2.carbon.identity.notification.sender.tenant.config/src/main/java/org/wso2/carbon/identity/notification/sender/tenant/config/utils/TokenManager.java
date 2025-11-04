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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.external.api.client.api.model.APIClientConfig;
import org.wso2.carbon.identity.external.api.token.handler.api.exception.TokenHandlerException;
import org.wso2.carbon.identity.external.api.token.handler.api.model.GrantContext;
import org.wso2.carbon.identity.external.api.token.handler.api.model.TokenRequestContext;
import org.wso2.carbon.identity.external.api.token.handler.api.model.TokenResponse;
import org.wso2.carbon.identity.external.api.token.handler.api.service.TokenAcquirerService;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.Authentication;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementServerException;

/**
 * Manager class for handling Client Credentials Token acquisition and management.
 */
public class TokenManager {

    private static final Log LOG = LogFactory.getLog(TokenManager.class);
    private final TokenAcquirerService tokenAcquirerService;
    private static final TokenManager tokenManager = new TokenManager();

    private TokenManager() {

        this.tokenAcquirerService = new TokenAcquirerService(buildAPIClientConfig());
    }

    public static TokenManager getInstance() {

        return tokenManager;
    }

    public void retrieveToken(Authentication authentication) throws NotificationSenderManagementServerException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Retrieving token for authentication type: " + authentication.getType());
        }

        TokenRequestContext tokenRequestContext = buildTokenRequestContext(authentication);
        tokenAcquirerService.setTokenRequestContext(tokenRequestContext);

        TokenResponse tokenResponse;
        String refreshToken = authentication.getInternalProperties().get("RefreshToken");

        try {
            if (refreshToken != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Attempting to retrieve access token using refresh token grant.");
                }
                try {
                    tokenResponse = tokenAcquirerService.getNewAccessTokenFromRefreshGrant(refreshToken);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Successfully retrieved access token using refresh token grant.");
                    }
                } catch (TokenHandlerException e) {
                    LOG.warn("Failed to retrieve access token using refresh token grant. Falling back to client " +
                            "credentials grant.", e);
                    tokenResponse = tokenAcquirerService.getNewAccessToken();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Successfully retrieved access token using client credentials grant.");
                    }
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No refresh token available. Retrieving access token using client credentials grant.");
                }
                tokenResponse = tokenAcquirerService.getNewAccessToken();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Successfully retrieved access token using client credentials grant.");
                }
            }
        } catch (TokenHandlerException e) {
            LOG.error("Error while retrieving token.", e);
            throw new NotificationSenderManagementServerException(
                    ErrorMessage.ERROR_CODE_ERROR_WHILE_RETRIEVING_TOKEN, null, e);
        }

        addInternalProperties(tokenResponse, authentication);
    }

    private void addInternalProperties(TokenResponse tokenResponse, Authentication authentication) {

        authentication.addInternalProperty("RefreshToken", tokenResponse.getRefreshToken());
        authentication.addInternalProperty("AccessToken", tokenResponse.getAccessToken());
    }

    private TokenRequestContext buildTokenRequestContext(Authentication authentication)
            throws NotificationSenderManagementServerException {

        if (authentication.getType() != Authentication.Type.CLIENT_CREDENTIAL) {
            LOG.error("Unsupported authentication type for token retrieval: " + authentication.getType());
            throw new NotificationSenderManagementServerException(
                    ErrorMessage.ERROR_CODE_ERROR_UNSUPPORTED_AUTH_TYPE_FOR_TOKEN_RETRIEVAL, null);
        }

        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Building token request context for client credentials grant.");
            }
            GrantContext grantContext = new GrantContext.Builder()
                    .grantType(GrantContext.GrantType.CLIENT_CREDENTIAL)
                    .properties(authentication.getProperties())
                    .build();

            TokenRequestContext.Builder builder = new TokenRequestContext.Builder()
                    .grantContext(grantContext)
                    .endpointUrl(authentication.getProperty(Authentication.Property.TOKEN_ENDPOINT.getName()));

            return builder.build();
        } catch (TokenHandlerException e) {
            LOG.error("Error while building token request context.", e);
            throw new NotificationSenderManagementServerException(
                    ErrorMessage.ERROR_CODE_ERROR_WHILE_TOKEN_REQUEST_BUILDING, null, e);
        }
    }

    private APIClientConfig buildAPIClientConfig() {

        APIClientConfig.Builder configBuilder = new APIClientConfig.Builder();
        return configBuilder.build();
    }
}
