/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

import org.mockito.Mock;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.configuration.mgt.core.model.Attribute;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.Authentication;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.SMSSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.SecretManagementCredentialException;
import org.wso2.carbon.identity.notification.sender.tenant.config.internal.NotificationSenderTenantConfigDataHolder;
import org.wso2.carbon.identity.secret.mgt.core.SecretManager;
import org.wso2.carbon.identity.secret.mgt.core.SecretResolveManager;
import org.wso2.carbon.identity.secret.mgt.core.exception.SecretManagementException;
import org.wso2.carbon.identity.secret.mgt.core.model.ResolvedSecret;
import org.wso2.carbon.identity.secret.mgt.core.model.SecretType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ACCESS_TOKEN_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.AUTH_EXTERNAL_PROP_PREFIX;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.AUTH_INTERNAL_PROP_PREFIX;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.AUTH_TYPE_PREFIX;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_ERROR_WHILE_ENCRYPTING_CREDENTIALS;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SMS_PROVIDER;

/**
 * Unit tests for the SMS authentication credential encryption/decryption logic in {@link NotificationSenderUtils}.
 */
public class NotificationSenderUtilsTest {

    private static final String MOCK_SECRET_TYPE_ID = "mock-secret-type-id";

    @Mock
    private SecretManager secretManager;
    @Mock
    private SecretResolveManager secretResolveManager;

    @BeforeMethod
    public void setUp() throws SecretManagementException {

        initMocks(this);
        SecretType mockSecretType = Mockito.mock(SecretType.class);
        when(mockSecretType.getId()).thenReturn(MOCK_SECRET_TYPE_ID);
        when(secretManager.getSecretType(anyString())).thenReturn(mockSecretType);
        NotificationSenderTenantConfigDataHolder.getInstance().setSecretManager(secretManager);
        NotificationSenderTenantConfigDataHolder.getInstance().setSecretResolveManager(secretResolveManager);
    }

    @Test(dataProvider = "authTypePropertiesDataProvider")
    public void testAddAuthenticationPropertiesEncryptsSensitiveProperties(
            String authType, Map<String, String> authProps, List<String> sensitiveKeys,
            List<String> nonSensitiveKeys) throws Exception {

        // No secret exists yet - simulates a brand new sender being configured for the first time.
        when(secretManager.isSecretExist(anyString(), anyString())).thenReturn(false);

        Authentication authentication = new Authentication.AuthenticationBuilder(authType, authProps).build();
        if ("CLIENT_CREDENTIAL".equals(authType) || "PASSWORD_CREDENTIAL".equals(authType)) {
            // Simulates the cached token-refresh write that originally crashed on a long JWT access token.
            authentication.addInternalProperty(ACCESS_TOKEN_PROP, "a-very-long-jwt-access-token-value");
        }

        Map<String, String> mapToBeUpdated = new HashMap<>();
        NotificationSenderUtils.addAuthenticationProperties(mapToBeUpdated, authentication);

        Assert.assertEquals(mapToBeUpdated.get(AUTH_TYPE_PREFIX), authType);

        for (String sensitiveKey : sensitiveKeys) {
            String storedValue = mapToBeUpdated.get(AUTH_EXTERNAL_PROP_PREFIX + sensitiveKey);
            String plainValue = authProps.get(sensitiveKey);
            Assert.assertNotEquals(storedValue, plainValue,
                    "Sensitive property '" + sensitiveKey + "' was stored in plaintext for auth type " + authType);
            Assert.assertEquals(storedValue, MOCK_SECRET_TYPE_ID + ":" + SMS_PROVIDER + ":" + authType + ":"
                    + sensitiveKey);
        }

        for (String nonSensitiveKey : nonSensitiveKeys) {
            Assert.assertEquals(mapToBeUpdated.get(AUTH_EXTERNAL_PROP_PREFIX + nonSensitiveKey),
                    authProps.get(nonSensitiveKey),
                    "Non-sensitive property '" + nonSensitiveKey + "' should be stored as-is for auth type "
                            + authType);
        }

        // The cached internal access token must always be encrypted, regardless of auth type, since it's the
        // property whose length (a JWT) triggered the original crash.
        String storedAccessToken = mapToBeUpdated.get(AUTH_INTERNAL_PROP_PREFIX + ACCESS_TOKEN_PROP);
        if (authentication.getInternalProperties().containsKey(ACCESS_TOKEN_PROP)) {
            Assert.assertEquals(storedAccessToken,
                    MOCK_SECRET_TYPE_ID + ":" + SMS_PROVIDER + ":" + authType + ":" + ACCESS_TOKEN_PROP);
        }
    }

    @DataProvider(name = "authTypePropertiesDataProvider")
    public Object[][] authTypePropertiesDataProvider() {

        Map<String, String> basicProps = new HashMap<>();
        basicProps.put("username", "admin");
        basicProps.put("password", "admin-password");

        Map<String, String> clientCredentialProps = new HashMap<>();
        clientCredentialProps.put("clientId", "my-client-id");
        clientCredentialProps.put("clientSecret", "my-client-secret");
        clientCredentialProps.put("tokenEndpoint", "https://idp.example.com/oauth2/token");
        clientCredentialProps.put("scopes", "sms.send");

        Map<String, String> bearerProps = new HashMap<>();
        bearerProps.put("accessToken", "static-bearer-token");

        Map<String, String> apiKeyProps = new HashMap<>();
        apiKeyProps.put("header", "X-Api-Key");
        apiKeyProps.put("value", "my-api-key-value");

        Map<String, String> passwordCredentialProps = new HashMap<>();
        passwordCredentialProps.put("clientId", "my-client-id");
        passwordCredentialProps.put("clientSecret", "my-client-secret");
        passwordCredentialProps.put("username", "resource-owner");
        passwordCredentialProps.put("password", "resource-owner-password");
        passwordCredentialProps.put("tokenEndpoint", "https://idp.example.com/oauth2/token");
        passwordCredentialProps.put("scopes", "sms.send");

        return new Object[][]{
                {"BASIC", basicProps, java.util.Arrays.asList("username", "password"), new ArrayList<String>()},
                {"CLIENT_CREDENTIAL", clientCredentialProps, java.util.Arrays.asList("clientId", "clientSecret"),
                        java.util.Arrays.asList("tokenEndpoint", "scopes")},
                {"BEARER", bearerProps, java.util.Arrays.asList("accessToken"), new ArrayList<String>()},
                {"API_KEY", apiKeyProps, java.util.Arrays.asList("value"), java.util.Arrays.asList("header")},
                {"PASSWORD_CREDENTIAL", passwordCredentialProps,
                        java.util.Arrays.asList("clientId", "clientSecret", "username", "password"),
                        java.util.Arrays.asList("tokenEndpoint", "scopes")},
        };
    }

    @Test
    public void testAddAuthenticationPropertiesNullAuthenticationIsNoOp() {

        Map<String, String> mapToBeUpdated = new HashMap<>();
        NotificationSenderUtils.addAuthenticationProperties(mapToBeUpdated, null);
        Assert.assertTrue(mapToBeUpdated.isEmpty());
    }

    @Test(expectedExceptions = SecretManagementCredentialException.class)
    public void testAddAuthenticationPropertiesWrapsEncryptionFailureAsUnchecked() throws Exception {

        when(secretManager.isSecretExist(anyString(), anyString())).thenReturn(false);
        doThrow(new SecretManagementException("secret store unavailable"))
                .when(secretManager).addSecret(anyString(), org.mockito.ArgumentMatchers.any());

        Map<String, String> authProps = new HashMap<>();
        authProps.put("username", "admin");
        authProps.put("password", "admin-password");
        Authentication authentication = new Authentication.AuthenticationBuilder("BASIC", authProps).build();

        NotificationSenderUtils.addAuthenticationProperties(new HashMap<>(), authentication);
    }

    @Test
    public void testAddAuthenticationPropertiesWrapsEncryptionFailurePreservesErrorCode() throws Exception {

        when(secretManager.isSecretExist(anyString(), anyString())).thenReturn(false);
        doThrow(new SecretManagementException("secret store unavailable"))
                .when(secretManager).addSecret(anyString(), org.mockito.ArgumentMatchers.any());

        Map<String, String> authProps = new HashMap<>();
        authProps.put("username", "admin");
        authProps.put("password", "admin-password");
        Authentication authentication = new Authentication.AuthenticationBuilder("BASIC", authProps).build();

        try {
            NotificationSenderUtils.addAuthenticationProperties(new HashMap<>(), authentication);
            Assert.fail("Expected SecretManagementCredentialException to be thrown");
        } catch (SecretManagementCredentialException e) {
            Assert.assertEquals(e.getErrorMessage(), ERROR_CODE_ERROR_WHILE_ENCRYPTING_CREDENTIALS);
        }
    }

    @Test
    public void testBuildSmsSenderFromResourceDecryptsSensitiveProperties() throws Exception {

        String usernameSecretReference = MOCK_SECRET_TYPE_ID + ":" + SMS_PROVIDER + ":BASIC:username";
        String passwordSecretReference = MOCK_SECRET_TYPE_ID + ":" + SMS_PROVIDER + ":BASIC:password";
        when(secretManager.isSecretExist(anyString(), anyString())).thenReturn(true);
        ResolvedSecret resolvedUsername = Mockito.mock(ResolvedSecret.class);
        when(resolvedUsername.getResolvedSecretValue()).thenReturn("admin");
        when(secretResolveManager.getResolvedSecret(SMS_PROVIDER + "_SECRET_PROPERTIES",
                SMS_PROVIDER + ":BASIC:username")).thenReturn(resolvedUsername);
        ResolvedSecret resolvedPassword = Mockito.mock(ResolvedSecret.class);
        when(resolvedPassword.getResolvedSecretValue()).thenReturn("admin-password");
        when(secretResolveManager.getResolvedSecret(SMS_PROVIDER + "_SECRET_PROPERTIES",
                SMS_PROVIDER + ":BASIC:password")).thenReturn(resolvedPassword);

        Resource resource = new Resource();
        resource.setResourceName("SMSPublisher");
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute(AUTH_TYPE_PREFIX, "BASIC"));
        attributes.add(new Attribute(AUTH_EXTERNAL_PROP_PREFIX + "username", usernameSecretReference));
        attributes.add(new Attribute(AUTH_EXTERNAL_PROP_PREFIX + "password", passwordSecretReference));
        resource.setAttributes(attributes);

        SMSSenderDTO smsSenderDTO = NotificationSenderUtils.buildSmsSenderFromResource(resource);

        Assert.assertEquals(smsSenderDTO.getAuthentication().getProperty("username"), "admin");
        Assert.assertEquals(smsSenderDTO.getAuthentication().getProperty("password"), "admin-password");
    }

    @Test
    public void testBuildSmsSenderFromResourceFallsBackToPlaintextForLegacyUnencryptedValues() throws Exception {

        // Pre-fix data: the value stored is the raw plaintext credential, and no matching secret was ever
        // created for it - decryptCredential() must fail fast so the fallback path returns the stored value
        // unchanged, keeping senders configured before this fix working without any migration step.
        when(secretManager.isSecretExist(anyString(), anyString())).thenReturn(false);

        Resource resource = new Resource();
        resource.setResourceName("SMSPublisher");
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute(AUTH_TYPE_PREFIX, "BASIC"));
        attributes.add(new Attribute(AUTH_EXTERNAL_PROP_PREFIX + "username", "admin"));
        attributes.add(new Attribute(AUTH_EXTERNAL_PROP_PREFIX + "password", "admin-password"));
        resource.setAttributes(attributes);

        SMSSenderDTO smsSenderDTO = NotificationSenderUtils.buildSmsSenderFromResource(resource);

        Assert.assertEquals(smsSenderDTO.getAuthentication().getProperty("username"), "admin");
        Assert.assertEquals(smsSenderDTO.getAuthentication().getProperty("password"), "admin-password");
    }

    @Test
    public void testBuildSmsSenderFromResourceLeavesNonSensitivePropertiesUntouched() throws Exception {

        // clientId/clientSecret are required for the CLIENT_CREDENTIAL auth config to build at all, but no
        // secret is registered for them here - they'll fall back to their raw (legacy-plaintext) values,
        // same as the dedicated fallback test above. The point of this test is tokenEndpoint/scopes, which
        // must never even be looked up in the secret store since they were never sensitive to begin with.
        when(secretManager.isSecretExist(anyString(), anyString())).thenReturn(false);

        Resource resource = new Resource();
        resource.setResourceName("SMSPublisher");
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute(AUTH_TYPE_PREFIX, "CLIENT_CREDENTIAL"));
        attributes.add(new Attribute(AUTH_EXTERNAL_PROP_PREFIX + "clientId", "my-client-id"));
        attributes.add(new Attribute(AUTH_EXTERNAL_PROP_PREFIX + "clientSecret", "my-client-secret"));
        attributes.add(new Attribute(AUTH_EXTERNAL_PROP_PREFIX + "tokenEndpoint",
                "https://idp.example.com/oauth2/token"));
        attributes.add(new Attribute(AUTH_EXTERNAL_PROP_PREFIX + "scopes", "sms.send"));
        resource.setAttributes(attributes);

        SMSSenderDTO smsSenderDTO = NotificationSenderUtils.buildSmsSenderFromResource(resource);

        Assert.assertEquals(smsSenderDTO.getAuthentication().getProperty("tokenEndpoint"),
                "https://idp.example.com/oauth2/token");
        Assert.assertEquals(smsSenderDTO.getAuthentication().getProperty("scopes"), "sms.send");
        verify(secretManager, never()).isSecretExist(anyString(),
                org.mockito.ArgumentMatchers.contains("tokenEndpoint"));
        verify(secretManager, never()).isSecretExist(anyString(), org.mockito.ArgumentMatchers.contains("scopes"));
    }

    @Test
    public void testBuildSmsSenderFromResourceWithoutAuthenticationDoesNotTouchSecretManager() throws Exception {

        // Non-HTTP SMS providers (e.g. Twilio via key/secret) don't configure an Authentication at all -
        // this must remain completely unaffected by the secret-manager routing added for HTTP auth types.
        Resource resource = new Resource();
        resource.setResourceName("SMSPublisher");
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("provider", "Twilio"));
        attributes.add(new Attribute("providerURL",
                "https://api.twilio.com/2010-04-01/Accounts/AC1/Messages.json"));
        resource.setAttributes(attributes);

        SMSSenderDTO smsSenderDTO = NotificationSenderUtils.buildSmsSenderFromResource(resource);

        Assert.assertNull(smsSenderDTO.getAuthentication());
        verify(secretManager, never()).isSecretExist(anyString(), anyString());
        verify(secretResolveManager, never()).getResolvedSecret(anyString(), anyString());
    }
}
