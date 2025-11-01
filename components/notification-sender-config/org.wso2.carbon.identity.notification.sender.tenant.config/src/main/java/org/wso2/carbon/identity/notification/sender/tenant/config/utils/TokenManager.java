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
import org.wso2.carbon.identity.external.api.client.api.model.APIClientConfig;
import org.wso2.carbon.identity.external.api.token.handler.api.exception.TokenHandlerException;
import org.wso2.carbon.identity.external.api.token.handler.api.model.GrantContext;
import org.wso2.carbon.identity.external.api.token.handler.api.model.TokenRequestContext;
import org.wso2.carbon.identity.external.api.token.handler.api.model.TokenResponse;
import org.wso2.carbon.identity.external.api.token.handler.api.service.TokenAcquirerService;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.AuthProperty;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.Authentication;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager class for handling Client Credentials Token acquisition and management.
 */
public class TokenManager {

    private final TokenAcquirerService tokenAcquirerService;
    private TokenResponse tokenResponse;
    private Authentication authentication;

    public TokenManager() {

        this.tokenAcquirerService = new TokenAcquirerService(buildAPIClientConfig());
    }

    public void setAuthentication(Authentication authentication) {

        this.authentication = authentication;
    }

    public String getToken() throws TokenHandlerException {

        if (tokenResponse != null && StringUtils.isBlank(tokenResponse.getAccessToken())) {
            return tokenResponse.getAccessToken();
        }
        return getNewToken(false);
    }

    public String getRefreshToken() {

        return tokenResponse.getRefreshToken();
    }

    public String getNewToken(Boolean withRefreshTokenGrant) throws TokenHandlerException {

        TokenRequestContext tokenRequestContext = buildTokenRequestContext(authentication);
        tokenAcquirerService.setTokenRequestContext(tokenRequestContext);
        tokenResponse = tokenAcquirerService.getNewAccessToken();
        return tokenResponse.getAccessToken();
    }

    private TokenRequestContext buildTokenRequestContext(Authentication authentication) throws TokenHandlerException {

        // only for credentials grant type <= think where this check should go
        // properly set the grant context details with switch
        Map<String, String> authPropertiesMap = new HashMap<>();
        for (AuthProperty authProperty : authentication.getProperties()) {
            if (authProperty.getScope() == AuthProperty.Scope.EXTERNAL) {
                authPropertiesMap.put(authProperty.getName(), authProperty.getValue());
            }
        }

        GrantContext grantContext = new GrantContext.Builder()
                    .grantType(GrantContext.GrantType.CLIENT_CREDENTIAL)
                    .properties(authPropertiesMap)
                    .build();

        // get endpoint url from prop
        TokenRequestContext.Builder builder = new TokenRequestContext.Builder()
                .grantContext(grantContext)
                .endpointUrl("https://customauth.free.beeceptor.com/todos");

        return builder.build();
    }

    private APIClientConfig buildAPIClientConfig() {

        APIClientConfig.Builder configBuilder = new APIClientConfig.Builder();
        return configBuilder.build();
    }
}
