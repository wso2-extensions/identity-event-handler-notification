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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.notification.sender.tenant.config.internal.NotificationSenderTenantConfigDataHolder;
import org.wso2.carbon.identity.secret.mgt.core.SecretManager;
import org.wso2.carbon.identity.secret.mgt.core.exception.SecretManagementException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SMS_PROVIDER;

/**
 * Unit tests for {@link NotificationSenderSecretProcessor#deleteAssociatedSMSSecrets(String)}.
 *
 * <p>Cross-checks the exact secret names deleted per auth type against the keys SMS's own
 * {@code addAuthenticationProperties}/{@code encryptCredential} path actually creates - a mismatch here
 * would silently leave orphaned rows in {@code IDN_SECRET} after a sender is deleted.
 */
public class NotificationSenderSecretProcessorTest {

    private static final String SECRET_TYPE = SMS_PROVIDER + "_SECRET_PROPERTIES";

    @Mock
    private SecretManager secretManager;

    @BeforeMethod
    public void setUp() {

        initMocks(this);
        NotificationSenderTenantConfigDataHolder.getInstance().setSecretManager(secretManager);
    }

    @Test
    public void testDeleteAssociatedSMSSecretsDeletesEveryExpectedSecretPerAuthType() throws SecretManagementException {

        when(secretManager.isSecretExist(anyString(), anyString())).thenReturn(true);

        NotificationSenderSecretProcessor.deleteAssociatedSMSSecrets(SMS_PROVIDER);

        // BASIC: password + the lowercase "username" (SMS's own casing, not Email's camelCase "userName").
        verify(secretManager).deleteSecret(SECRET_TYPE, SMS_PROVIDER + ":BASIC:password");
        verify(secretManager).deleteSecret(SECRET_TYPE, SMS_PROVIDER + ":BASIC:username");

        // CLIENT_CREDENTIAL: clientId, clientSecret, and the cached token under "accessToken" (SMS's own
        // key, not Email's "internalAccessToken").
        verify(secretManager).deleteSecret(SECRET_TYPE, SMS_PROVIDER + ":CLIENT_CREDENTIAL:clientId");
        verify(secretManager).deleteSecret(SECRET_TYPE, SMS_PROVIDER + ":CLIENT_CREDENTIAL:clientSecret");
        verify(secretManager).deleteSecret(SECRET_TYPE, SMS_PROVIDER + ":CLIENT_CREDENTIAL:accessToken");

        // BEARER: the static access token.
        verify(secretManager).deleteSecret(SECRET_TYPE, SMS_PROVIDER + ":BEARER:accessToken");

        // API_KEY: the api key value, keyed "value" (SMS's own key, not Email's "apiKeyValue").
        verify(secretManager).deleteSecret(SECRET_TYPE, SMS_PROVIDER + ":API_KEY:value");

        // PASSWORD_CREDENTIAL: clientId, clientSecret, username, password, and the cached token.
        verify(secretManager).deleteSecret(SECRET_TYPE, SMS_PROVIDER + ":PASSWORD_CREDENTIAL:clientId");
        verify(secretManager).deleteSecret(SECRET_TYPE, SMS_PROVIDER + ":PASSWORD_CREDENTIAL:clientSecret");
        verify(secretManager).deleteSecret(SECRET_TYPE, SMS_PROVIDER + ":PASSWORD_CREDENTIAL:username");
        verify(secretManager).deleteSecret(SECRET_TYPE, SMS_PROVIDER + ":PASSWORD_CREDENTIAL:password");
        verify(secretManager).deleteSecret(SECRET_TYPE, SMS_PROVIDER + ":PASSWORD_CREDENTIAL:accessToken");

        // Exactly 12 secrets total across all five auth types - no extras, no omissions.
        verify(secretManager, times(12)).deleteSecret(eq(SECRET_TYPE), anyString());
    }

    @Test
    public void testDeleteAssociatedSMSSecretsNeverUsesEmailsKeyNames() throws SecretManagementException {

        when(secretManager.isSecretExist(anyString(), anyString())).thenReturn(true);

        NotificationSenderSecretProcessor.deleteAssociatedSMSSecrets(SMS_PROVIDER);

        // Email's key names must never be used for SMS - reusing them as-is would silently skip SMS's
        // actual secrets, leaving them orphaned.
        verify(secretManager, never()).deleteSecret(SECRET_TYPE, SMS_PROVIDER + ":BASIC:userName");
        verify(secretManager, never())
                .deleteSecret(SECRET_TYPE, SMS_PROVIDER + ":CLIENT_CREDENTIAL:internalAccessToken");
        verify(secretManager, never())
                .deleteSecret(SECRET_TYPE, SMS_PROVIDER + ":PASSWORD_CREDENTIAL:userName");
        verify(secretManager, never()).deleteSecret(SECRET_TYPE, SMS_PROVIDER + ":API_KEY:apiKeyValue");
        verify(secretManager, never())
                .deleteSecret(SECRET_TYPE, SMS_PROVIDER + ":PASSWORD_CREDENTIAL:internalAccessToken");
    }

    @Test
    public void testDeleteAssociatedSMSSecretsSkipsSecretsThatDoNotExist() throws SecretManagementException {

        when(secretManager.isSecretExist(anyString(), anyString())).thenReturn(false);

        NotificationSenderSecretProcessor.deleteAssociatedSMSSecrets(SMS_PROVIDER);

        verify(secretManager, never()).deleteSecret(anyString(), anyString());
    }
}
