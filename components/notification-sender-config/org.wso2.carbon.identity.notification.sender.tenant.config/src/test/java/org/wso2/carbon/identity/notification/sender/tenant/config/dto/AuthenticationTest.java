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

import org.apache.http.Header;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.Authentication.Property;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.Authentication.Type;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementClientException;
import org.wso2.carbon.identity.notification.sender.tenant.config.utils.NotificationSenderUtils;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Unit tests for {@link Authentication}.
 */
public class AuthenticationTest {

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
    public void testAuthenticationBuilderWithBasicAuth() throws Exception {
        authProperties.put(Property.USERNAME.getName(), "testuser");
        authProperties.put(Property.PASSWORD.getName(), "testpass");

        try (MockedStatic<NotificationSenderUtils> mockedUtils = mockStatic(NotificationSenderUtils.class)) {
            mockedUtils.when(() -> NotificationSenderUtils.buildAuthenticationHeader(
                    eq(Type.BASIC), any())).thenReturn(mockHeader);

            Authentication auth = new Authentication.AuthenticationBuilder("BASIC", authProperties).build();

            Assert.assertNotNull(auth);
            Assert.assertEquals(auth.getType(), Type.BASIC);
            Assert.assertEquals(auth.getProperty(Property.USERNAME.getName()), "testuser");
            Assert.assertEquals(auth.getProperty(Property.PASSWORD.getName()), "testpass");
        }
    }

    @Test
    public void testAuthenticationBuilderWithBearerAuth() throws Exception {
        authProperties.put(Property.ACCESS_TOKEN.getName(), "test-access-token");

        try (MockedStatic<NotificationSenderUtils> mockedUtils = mockStatic(NotificationSenderUtils.class)) {
            mockedUtils.when(() -> NotificationSenderUtils.buildAuthenticationHeader(
                    eq(Type.BEARER), any())).thenReturn(mockHeader);

            Authentication auth = new Authentication.AuthenticationBuilder("BEARER", authProperties).build();

            Assert.assertNotNull(auth);
            Assert.assertEquals(auth.getType(), Type.BEARER);
            Assert.assertEquals(auth.getProperty(Property.ACCESS_TOKEN.getName()), "test-access-token");
        }
    }

    @Test
    public void testAuthenticationBuilderWithClientCredential() throws Exception {
        authProperties.put(Property.CLIENT_ID.getName(), "test-client-id");
        authProperties.put(Property.CLIENT_SECRET.getName(), "test-client-secret");
        authProperties.put(Property.SCOPE.getName(), "test-scope");
        authProperties.put(Property.TOKEN_ENDPOINT.getName(), "https://test.com/token");

        try (MockedStatic<NotificationSenderUtils> mockedUtils = mockStatic(NotificationSenderUtils.class)) {
            mockedUtils.when(() -> NotificationSenderUtils.buildAuthenticationHeader(
                    eq(Type.CLIENT_CREDENTIAL), any())).thenReturn(mockHeader);

            Authentication auth = new Authentication.AuthenticationBuilder("CLIENT_CREDENTIAL", authProperties).build();

            Assert.assertNotNull(auth);
            Assert.assertEquals(auth.getType(), Type.CLIENT_CREDENTIAL);
            Assert.assertEquals(auth.getProperty(Property.CLIENT_ID.getName()), "test-client-id");
            Assert.assertEquals(auth.getProperty(Property.CLIENT_SECRET.getName()), "test-client-secret");
            Assert.assertEquals(auth.getProperty(Property.SCOPE.getName()), "test-scope");
            Assert.assertEquals(auth.getProperty(Property.TOKEN_ENDPOINT.getName()), "https://test.com/token");
        }
    }

    @Test
    public void testAuthenticationBuilderWithApiKey() throws Exception {
        authProperties.put(Property.HEADER.getName(), "X-API-KEY");
        authProperties.put(Property.VALUE.getName(), "test-api-key-value");

        try (MockedStatic<NotificationSenderUtils> mockedUtils = mockStatic(NotificationSenderUtils.class)) {
            mockedUtils.when(() -> NotificationSenderUtils.buildAuthenticationHeader(
                    eq(Type.API_KEY), any())).thenReturn(mockHeader);

            Authentication auth = new Authentication.AuthenticationBuilder("API_KEY", authProperties).build();

            Assert.assertNotNull(auth);
            Assert.assertEquals(auth.getType(), Type.API_KEY);
            Assert.assertEquals(auth.getProperty(Property.HEADER.getName()), "X-API-KEY");
            Assert.assertEquals(auth.getProperty(Property.VALUE.getName()), "test-api-key-value");
        }
    }

    @Test
    public void testAuthenticationBuilderWithNoneType() throws Exception {
        try (MockedStatic<NotificationSenderUtils> mockedUtils = mockStatic(NotificationSenderUtils.class)) {
            mockedUtils.when(() -> NotificationSenderUtils.buildAuthenticationHeader(
                    eq(Type.NONE), any())).thenReturn(mockHeader);

            Authentication auth = new Authentication.AuthenticationBuilder("NONE", authProperties).build();

            Assert.assertNotNull(auth);
            Assert.assertEquals(auth.getType(), Type.NONE);
        }
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testAuthenticationBuilderWithMissingProperty() throws Exception {
        authProperties.put(Property.USERNAME.getName(), "testuser");
        // Missing password property

        new Authentication.AuthenticationBuilder("BASIC", authProperties).build();
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testAuthenticationBuilderWithBlankProperty() throws Exception {
        authProperties.put(Property.USERNAME.getName(), "testuser");
        authProperties.put(Property.PASSWORD.getName(), "");

        new Authentication.AuthenticationBuilder("BASIC", authProperties).build();
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testAuthenticationBuilderWithNullAuthType() throws Exception {
        new Authentication.AuthenticationBuilder(null, authProperties).build();
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testAuthenticationBuilderWithEmptyAuthType() throws Exception {
        new Authentication.AuthenticationBuilder("", authProperties).build();
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testAuthenticationBuilderWithUnsupportedAuthType() throws Exception {
        new Authentication.AuthenticationBuilder("UNSUPPORTED_TYPE", authProperties).build();
    }

    @Test
    public void testTypeValueOfName() throws Exception {
        Assert.assertEquals(Type.valueOfName("BASIC"), Type.BASIC);
        Assert.assertEquals(Type.valueOfName("BEARER"), Type.BEARER);
        Assert.assertEquals(Type.valueOfName("CLIENT_CREDENTIAL"), Type.CLIENT_CREDENTIAL);
        Assert.assertEquals(Type.valueOfName("API_KEY"), Type.API_KEY);
        Assert.assertEquals(Type.valueOfName("NONE"), Type.NONE);
    }

    @Test
    public void testTypeValueOfNameCaseInsensitive() throws Exception {
        Assert.assertEquals(Type.valueOfName("basic"), Type.BASIC);
        Assert.assertEquals(Type.valueOfName("Bearer"), Type.BEARER);
        Assert.assertEquals(Type.valueOfName("client_credential"), Type.CLIENT_CREDENTIAL);
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testTypeValueOfNameWithNull() throws Exception {
        Type.valueOfName(null);
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testTypeValueOfNameWithEmpty() throws Exception {
        Type.valueOfName("");
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testTypeValueOfNameWithInvalid() throws Exception {
        Type.valueOfName("INVALID_TYPE");
    }

    @Test
    public void testGetters() throws Exception {
        authProperties.put(Property.USERNAME.getName(), "testuser");
        authProperties.put(Property.PASSWORD.getName(), "testpass");

        try (MockedStatic<NotificationSenderUtils> mockedUtils = mockStatic(NotificationSenderUtils.class)) {
            mockedUtils.when(() -> NotificationSenderUtils.buildAuthenticationHeader(
                    eq(Type.BASIC), any())).thenReturn(mockHeader);

            Authentication auth = new Authentication.AuthenticationBuilder("BASIC", authProperties).build();

            Assert.assertEquals(auth.getType(), Type.BASIC);
            Assert.assertNotNull(auth.getProperties());
            Assert.assertEquals(auth.getProperties().size(), 2);
            Assert.assertEquals(auth.getProperty(Property.USERNAME.getName()), "testuser");
        }
    }

    @Test
    public void testAddInternalProperty() throws Exception {
        authProperties.put(Property.USERNAME.getName(), "testuser");
        authProperties.put(Property.PASSWORD.getName(), "testpass");

        try (MockedStatic<NotificationSenderUtils> mockedUtils = mockStatic(NotificationSenderUtils.class)) {
            mockedUtils.when(() -> NotificationSenderUtils.buildAuthenticationHeader(
                    eq(Type.BASIC), any())).thenReturn(mockHeader);

            Authentication auth = new Authentication.AuthenticationBuilder("BASIC", authProperties).build();
            
            auth.addInternalProperty("testKey", "testValue");
            Assert.assertEquals(auth.getProperty("testKey"), "testValue");
        }
    }

    @Test
    public void testPropertyEnum() {
        Assert.assertEquals(Property.USERNAME.getName(), "username");
        Assert.assertEquals(Property.PASSWORD.getName(), "password");
        Assert.assertEquals(Property.HEADER.getName(), "header");
        Assert.assertEquals(Property.VALUE.getName(), "value");
        Assert.assertEquals(Property.ACCESS_TOKEN.getName(), "accessToken");
        Assert.assertEquals(Property.CLIENT_ID.getName(), "client_id");
        Assert.assertEquals(Property.CLIENT_SECRET.getName(), "client_secret");
        Assert.assertEquals(Property.SCOPE.getName(), "scope");
        Assert.assertEquals(Property.TOKEN_ENDPOINT.getName(), "token_endpoint");
    }

    @Test
    public void testTypeEnum() {
        Assert.assertEquals(Type.NONE.getName(), "NONE");
        Assert.assertEquals(Type.BEARER.getName(), "BEARER");
        Assert.assertEquals(Type.CLIENT_CREDENTIAL.getName(), "CLIENT_CREDENTIAL");
        Assert.assertEquals(Type.BASIC.getName(), "BASIC");
        Assert.assertEquals(Type.API_KEY.getName(), "API_KEY");
    }
}
