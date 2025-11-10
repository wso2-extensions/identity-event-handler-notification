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

import org.apache.http.Header;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.Authentication;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementClientException;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Unit tests for {@link TokenManager}.
 * Note: TokenManager is a singleton with complex initialization that depends on external configuration.
 * These tests focus on testing the authentication building and validation logic through the Authentication class.
 */
public class TokenManagerTest {

    private Map<String, String> authProperties;
    private Header mockHeader;

    @BeforeMethod
    public void setUp() {
        authProperties = new HashMap<>();
        mockHeader = mock(Header.class);
    }

    @AfterMethod
    public void tearDown() {
        authProperties.clear();
    }

    @Test
    public void testAuthenticationWithClientCredential() throws Exception {
        authProperties.put(Authentication.Property.CLIENT_ID.getName(), "test-client-id");
        authProperties.put(Authentication.Property.CLIENT_SECRET.getName(), "test-client-secret");
        authProperties.put(Authentication.Property.SCOPE.getName(), "test-scope");
        authProperties.put(Authentication.Property.TOKEN_ENDPOINT.getName(), "https://test.com/token");

        try (MockedStatic<NotificationSenderUtils> mockedUtils = mockStatic(NotificationSenderUtils.class)) {
            mockedUtils.when(() -> NotificationSenderUtils.buildAuthenticationHeader(any())).thenReturn(mockHeader);

            Authentication auth = new Authentication.AuthenticationBuilder("CLIENT_CREDENTIAL", authProperties).build();

            Assert.assertNotNull(auth);
            Assert.assertEquals(auth.getType(), Authentication.Type.CLIENT_CREDENTIAL);
            Assert.assertEquals(auth.getProperty(Authentication.Property.CLIENT_ID.getName()), "test-client-id");
            Assert.assertEquals(auth.getProperty(Authentication.Property.CLIENT_SECRET.getName()),
                    "test-client-secret");
            Assert.assertEquals(auth.getProperty(Authentication.Property.SCOPE.getName()), "test-scope");
            Assert.assertEquals(auth.getProperty(Authentication.Property.TOKEN_ENDPOINT.getName()),
                    "https://test.com/token");
        }
    }

    @Test
    public void testAuthenticationWithBasicType() throws Exception {
        authProperties.put(Authentication.Property.USERNAME.getName(), "testuser");
        authProperties.put(Authentication.Property.PASSWORD.getName(), "testpass");

        try (MockedStatic<NotificationSenderUtils> mockedUtils = mockStatic(NotificationSenderUtils.class)) {
            mockedUtils.when(() -> NotificationSenderUtils.buildAuthenticationHeader(any())).thenReturn(mockHeader);

            Authentication auth = new Authentication.AuthenticationBuilder("BASIC", authProperties).build();

            Assert.assertNotNull(auth);
            Assert.assertEquals(auth.getType(), Authentication.Type.BASIC);
            Assert.assertNotEquals(auth.getType(), Authentication.Type.CLIENT_CREDENTIAL);
        }
    }

    @Test
    public void testAuthenticationWithBearerType() throws Exception {
        authProperties.put(Authentication.Property.ACCESS_TOKEN.getName(), "test-access-token");

        try (MockedStatic<NotificationSenderUtils> mockedUtils = mockStatic(NotificationSenderUtils.class)) {
            mockedUtils.when(() -> NotificationSenderUtils.buildAuthenticationHeader(any())).thenReturn(mockHeader);

            Authentication auth = new Authentication.AuthenticationBuilder("BEARER", authProperties).build();

            Assert.assertNotNull(auth);
            Assert.assertEquals(auth.getType(), Authentication.Type.BEARER);
        }
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testAuthenticationWithMissingClientId() throws Exception {
        authProperties.put(Authentication.Property.CLIENT_SECRET.getName(), "test-client-secret");
        authProperties.put(Authentication.Property.SCOPE.getName(), "test-scope");
        authProperties.put(Authentication.Property.TOKEN_ENDPOINT.getName(), "https://test.com/token");

        new Authentication.AuthenticationBuilder("CLIENT_CREDENTIAL", authProperties).build();
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testAuthenticationWithMissingClientSecret() throws Exception {
        authProperties.put(Authentication.Property.CLIENT_ID.getName(), "test-client-id");
        authProperties.put(Authentication.Property.SCOPE.getName(), "test-scope");
        authProperties.put(Authentication.Property.TOKEN_ENDPOINT.getName(), "https://test.com/token");

        new Authentication.AuthenticationBuilder("CLIENT_CREDENTIAL", authProperties).build();
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testAuthenticationWithMissingTokenEndpoint() throws Exception {
        authProperties.put(Authentication.Property.CLIENT_ID.getName(), "test-client-id");
        authProperties.put(Authentication.Property.CLIENT_SECRET.getName(), "test-client-secret");
        authProperties.put(Authentication.Property.SCOPE.getName(), "test-scope");

        new Authentication.AuthenticationBuilder("CLIENT_CREDENTIAL", authProperties).build();
    }

    @Test
    public void testAuthenticationInternalProperties() throws Exception {
        authProperties.put(Authentication.Property.CLIENT_ID.getName(), "test-client-id");
        authProperties.put(Authentication.Property.CLIENT_SECRET.getName(), "test-client-secret");
        authProperties.put(Authentication.Property.SCOPE.getName(), "test-scope");
        authProperties.put(Authentication.Property.TOKEN_ENDPOINT.getName(), "https://test.com/token");

        try (MockedStatic<NotificationSenderUtils> mockedUtils = mockStatic(NotificationSenderUtils.class)) {
            mockedUtils.when(() -> NotificationSenderUtils.buildAuthenticationHeader(any())).thenReturn(mockHeader);

            Authentication auth = new Authentication.AuthenticationBuilder("CLIENT_CREDENTIAL", authProperties).build();

            // Test adding internal properties (simulating what TokenManager would do)
            auth.addInternalProperty("AccessToken", "test-access-token");
            auth.addInternalProperty("RefreshToken", "test-refresh-token");

            Assert.assertEquals(auth.getInternalProperties().get("AccessToken"), "test-access-token");
            Assert.assertEquals(auth.getInternalProperties().get("RefreshToken"), "test-refresh-token");
        }
    }

    @Test
    public void testAuthenticationGetInternalProperties() throws Exception {
        authProperties.put(Authentication.Property.CLIENT_ID.getName(), "test-client-id");
        authProperties.put(Authentication.Property.CLIENT_SECRET.getName(), "test-client-secret");
        authProperties.put(Authentication.Property.SCOPE.getName(), "test-scope");
        authProperties.put(Authentication.Property.TOKEN_ENDPOINT.getName(), "https://test.com/token");

        try (MockedStatic<NotificationSenderUtils> mockedUtils = mockStatic(NotificationSenderUtils.class)) {
            mockedUtils.when(() -> NotificationSenderUtils.buildAuthenticationHeader(any())).thenReturn(mockHeader);

            Authentication auth = new Authentication.AuthenticationBuilder("CLIENT_CREDENTIAL", authProperties).build();

            Map<String, String> internalProps = auth.getInternalProperties();
            Assert.assertNotNull(internalProps);
        }
    }
}
