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
 * Unit tests for {@link SMSSenderDTO}.
 */
public class SMSSenderDTOTest {

    private SMSSenderDTO.Builder builder;

    @BeforeMethod
    public void setUp() {
        builder = new SMSSenderDTO.Builder();
    }

    @AfterMethod
    public void tearDown() {
        builder = null;
    }

    @Test
    public void testBuildSimpleSMSSender() throws Exception {
        SMSSenderDTO dto = builder
                .name("TestSender")
                .provider("TestProvider")
                .providerURL("https://test.provider.com")
                .sender("+1234567890")
                .contentType("application/json")
                .build();

        Assert.assertNotNull(dto);
        Assert.assertEquals(dto.getName(), "TestSender");
        Assert.assertEquals(dto.getProvider(), "TestProvider");
        Assert.assertEquals(dto.getProviderURL(), "https://test.provider.com");
        Assert.assertEquals(dto.getSender(), "+1234567890");
        Assert.assertEquals(dto.getContentType(), "application/json");
        Assert.assertNull(dto.getAuthentication());
    }

    @Test
    public void testBuildWithBasicAuthUsingKeyAndSecret() throws Exception {
        SMSSenderDTO dto = builder
                .name("TestSender")
                .provider("TestProvider")
                .providerURL("https://test.provider.com")
                .key("testuser")
                .secret("testpass")
                .build();

        Assert.assertNotNull(dto);
        Assert.assertNotNull(dto.getAuthentication());
        Assert.assertEquals(dto.getAuthentication().getType(), Type.BASIC);
        Assert.assertEquals(dto.getKey(), "testuser");
        Assert.assertEquals(dto.getSecret(), "testpass");
    }

    @Test
    public void testBuildWithBearerAuth() throws Exception {
        SMSSenderDTO dto = builder
                .name("TestSender")
                .provider("TestProvider")
                .providerURL("https://test.provider.com")
                .authType("BEARER")
                .addAuthProperty(Property.ACCESS_TOKEN.getName(), "test-token")
                .build();

        Assert.assertNotNull(dto);
        Assert.assertNotNull(dto.getAuthentication());
        Assert.assertEquals(dto.getAuthentication().getType(), Type.BEARER);
    }

    @Test
    public void testBuildWithClientCredentialAuth() throws Exception {
        SMSSenderDTO dto = builder
                .name("TestSender")
                .provider("TestProvider")
                .providerURL("https://test.provider.com")
                .authType("CLIENT_CREDENTIAL")
                .addAuthProperty(Property.CLIENT_ID.getName(), "test-client-id")
                .addAuthProperty(Property.CLIENT_SECRET.getName(), "test-client-secret")
                .addAuthProperty(Property.SCOPE.getName(), "test-scope")
                .addAuthProperty(Property.TOKEN_ENDPOINT.getName(), "https://test.com/token")
                .build();

        Assert.assertNotNull(dto);
        Assert.assertNotNull(dto.getAuthentication());
        Assert.assertEquals(dto.getAuthentication().getType(), Type.CLIENT_CREDENTIAL);
    }

    @Test
    public void testBuildWithApiKeyAuth() throws Exception {
        SMSSenderDTO dto = builder
                .name("TestSender")
                .provider("TestProvider")
                .providerURL("https://test.provider.com")
                .authType("API_KEY")
                .addAuthProperty(Property.HEADER.getName(), "X-API-KEY")
                .addAuthProperty(Property.VALUE.getName(), "test-api-key")
                .build();

        Assert.assertNotNull(dto);
        Assert.assertNotNull(dto.getAuthentication());
        Assert.assertEquals(dto.getAuthentication().getType(), Type.API_KEY);
    }

    @Test
    public void testBuildWithProperties() throws Exception {
        SMSSenderDTO dto = builder
                .name("TestSender")
                .provider("TestProvider")
                .providerURL("https://test.provider.com")
                .addProperty("customKey1", "customValue1")
                .addProperty("customKey2", "customValue2")
                .build();

        Assert.assertNotNull(dto);
        Assert.assertNotNull(dto.getProperties());
        Assert.assertEquals(dto.getProperties().size(), 2);
        Assert.assertEquals(dto.getProperties().get("customKey1"), "customValue1");
        Assert.assertEquals(dto.getProperties().get("customKey2"), "customValue2");
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testBuildWithNonBasicAuthAndKeyProvided() throws Exception {
        builder
                .name("TestSender")
                .provider("TestProvider")
                .providerURL("https://test.provider.com")
                .authType("BEARER")
                .addAuthProperty(Property.ACCESS_TOKEN.getName(), "test-token")
                .key("should-not-be-allowed")
                .build();
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testBuildWithNonBasicAuthAndSecretProvided() throws Exception {
        builder
                .name("TestSender")
                .provider("TestProvider")
                .providerURL("https://test.provider.com")
                .authType("BEARER")
                .addAuthProperty(Property.ACCESS_TOKEN.getName(), "test-token")
                .secret("should-not-be-allowed")
                .build();
    }

    @Test
    public void testBuildWithBasicAuthUsingExistingCredentials() throws Exception {
        // Build with BASIC auth and existing credentials
        SMSSenderDTO dto = builder
                .name("TestSender")
                .provider("TestProvider")
                .providerURL("https://test.provider.com")
                .authType("BASIC")
                .addAuthProperty(Property.USERNAME.getName(), "existinguser")
                .addAuthProperty(Property.PASSWORD.getName(), "existingpass")
                .build();

        Assert.assertNotNull(dto);
        Assert.assertNotNull(dto.getAuthentication());
        Assert.assertEquals(dto.getAuthentication().getType(), Type.BASIC);
        Assert.assertEquals(dto.getKey(), "existinguser");
        Assert.assertEquals(dto.getSecret(), "existingpass");
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testBuildWithBasicAuthKeyMismatch() throws Exception {
        builder
                .name("TestSender")
                .provider("TestProvider")
                .providerURL("https://test.provider.com")
                .authType("BASIC")
                .addAuthProperty(Property.USERNAME.getName(), "existinguser")
                .addAuthProperty(Property.PASSWORD.getName(), "existingpass")
                .key("differentuser")
                .build();
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testBuildWithBasicAuthSecretMismatch() throws Exception {
        builder
                .name("TestSender")
                .provider("TestProvider")
                .providerURL("https://test.provider.com")
                .authType("BASIC")
                .addAuthProperty(Property.USERNAME.getName(), "existinguser")
                .addAuthProperty(Property.PASSWORD.getName(), "existingpass")
                .secret("differentpass")
                .build();
    }

    @Test
    public void testSettersAndGetters() throws Exception {
        SMSSenderDTO dto = new SMSSenderDTO();
        
        dto.setName("TestName");
        dto.setProvider("TestProvider");
        dto.setProviderURL("https://test.com");
        dto.setKey("testkey");
        dto.setSecret("testsecret");
        dto.setSender("+1234567890");
        dto.setContentType("application/json");
        
        Map<String, String> props = new HashMap<>();
        props.put("key1", "value1");
        dto.setProperties(props);

        Assert.assertEquals(dto.getName(), "TestName");
        Assert.assertEquals(dto.getProvider(), "TestProvider");
        Assert.assertEquals(dto.getProviderURL(), "https://test.com");
        Assert.assertEquals(dto.getKey(), "testkey");
        Assert.assertEquals(dto.getSecret(), "testsecret");
        Assert.assertEquals(dto.getSender(), "+1234567890");
        Assert.assertEquals(dto.getContentType(), "application/json");
        Assert.assertEquals(dto.getProperties().get("key1"), "value1");
    }

    @Test
    public void testBuildWithOnlyKeyNoSecret() throws Exception {
        SMSSenderDTO dto = builder
                .name("TestSender")
                .provider("TestProvider")
                .providerURL("https://test.provider.com")
                .key("testuser")
                .build();

        Assert.assertNotNull(dto);
        Assert.assertNull(dto.getAuthentication(), "Authentication should be null when only key is provided");
    }

    @Test
    public void testBuildWithOnlySecretNoKey() throws Exception {
        SMSSenderDTO dto = builder
                .name("TestSender")
                .provider("TestProvider")
                .providerURL("https://test.provider.com")
                .secret("testpass")
                .build();

        Assert.assertNotNull(dto);
        Assert.assertNull(dto.getAuthentication(), "Authentication should be null when only secret is provided");
    }

    @Test
    public void testSetAuthentication() throws Exception {
        Map<String, String> authProps = new HashMap<>();
        authProps.put(Property.USERNAME.getName(), "testuser");
        authProps.put(Property.PASSWORD.getName(), "testpass");
        Authentication auth = new Authentication.AuthenticationBuilder("BASIC", authProps).build();

        SMSSenderDTO dto = new SMSSenderDTO();
        dto.setAuthentication(auth);

        Assert.assertNotNull(dto.getAuthentication());
        Assert.assertEquals(dto.getAuthentication().getType(), Type.BASIC);
    }
}
