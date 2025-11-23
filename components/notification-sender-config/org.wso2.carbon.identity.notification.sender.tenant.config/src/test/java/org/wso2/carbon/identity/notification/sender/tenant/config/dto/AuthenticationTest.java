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

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.Authentication.Property;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.Authentication.Type;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementClientException;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for {@link Authentication}.
 */
public class AuthenticationTest {

    private Map<String, String> authProperties;

    @BeforeMethod
    public void setUp() {
        authProperties = new HashMap<>();
    }

    @AfterMethod
    public void tearDown() {
        authProperties.clear();
    }

    @Test
    public void testAuthenticationBuilderWithBasicAuth() throws Exception {

        authProperties.put(Property.USERNAME.getName(), "testuser");
        authProperties.put(Property.PASSWORD.getName(), "testpass");

        Authentication auth = new Authentication.AuthenticationBuilder("BASIC", authProperties).build();

        Assert.assertNotNull(auth);
        Assert.assertEquals(auth.getType(), Type.BASIC);
        Assert.assertEquals(auth.getProperty(Property.USERNAME.getName()), "testuser");
        Assert.assertEquals(auth.getProperty(Property.PASSWORD.getName()), "testpass");
    }

    @Test
    public void testAuthenticationBuilderWithBearerAuth() throws Exception {

        authProperties.put(Property.ACCESS_TOKEN.getName(), "test-access-token");

        Authentication auth = new Authentication.AuthenticationBuilder("BEARER", authProperties).build();

        Assert.assertNotNull(auth);
        Assert.assertEquals(auth.getType(), Type.BEARER);
        Assert.assertEquals(auth.getProperty(Property.ACCESS_TOKEN.getName()), "test-access-token");
    }

    @Test
    public void testAuthenticationBuilderWithClientCredential() throws Exception {

        authProperties.put(Property.CLIENT_ID.getName(), "test-client-id");
        authProperties.put(Property.CLIENT_SECRET.getName(), "test-client-secret");
        authProperties.put(Property.SCOPE.getName(), "test-scope");
        authProperties.put(Property.TOKEN_ENDPOINT.getName(), "https://test.com/token");

        Authentication auth = new Authentication.AuthenticationBuilder("CLIENT_CREDENTIAL", authProperties).build();

        Assert.assertNotNull(auth);
        Assert.assertEquals(auth.getType(), Type.CLIENT_CREDENTIAL);
        Assert.assertEquals(auth.getProperty(Property.CLIENT_ID.getName()), "test-client-id");
        Assert.assertEquals(auth.getProperty(Property.CLIENT_SECRET.getName()), "test-client-secret");
        Assert.assertEquals(auth.getProperty(Property.SCOPE.getName()), "test-scope");
        Assert.assertEquals(auth.getProperty(Property.TOKEN_ENDPOINT.getName()), "https://test.com/token");
    }

    @Test
    public void testAuthenticationBuilderWithApiKey() throws Exception {

        authProperties.put(Property.HEADER.getName(), "X-API-KEY");
        authProperties.put(Property.VALUE.getName(), "test-api-key-value");

        Authentication auth = new Authentication.AuthenticationBuilder("API_KEY", authProperties).build();

        Assert.assertNotNull(auth);
        Assert.assertEquals(auth.getType(), Type.API_KEY);
        Assert.assertEquals(auth.getProperty(Property.HEADER.getName()), "X-API-KEY");
        Assert.assertEquals(auth.getProperty(Property.VALUE.getName()), "test-api-key-value");
    }

    @Test
    public void testAuthenticationBuilderWithNoneType() throws Exception {

        Authentication auth = new Authentication.AuthenticationBuilder("NONE", authProperties).build();

        Assert.assertNotNull(auth);
        Assert.assertEquals(auth.getType(), Type.NONE);
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testAuthenticationBuilderWithMissingProperty() throws Exception {

        authProperties.put(Property.USERNAME.getName(), "testuser");

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
    public void testAddInternalProperty() throws Exception {
        authProperties.put(Property.USERNAME.getName(), "testuser");
        authProperties.put(Property.PASSWORD.getName(), "testpass");

        Authentication auth = new Authentication.AuthenticationBuilder("BASIC", authProperties).build();
        
        auth.addInternalProperty("testKey", "testValue");
        Assert.assertEquals(auth.getInternalProperties().get("testKey"), "testValue");
    }
}
