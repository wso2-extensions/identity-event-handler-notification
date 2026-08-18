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
import org.wso2.carbon.identity.notification.sender.tenant.config.internal.NotificationSenderTenantConfigDataHolder;
import org.wso2.carbon.identity.secret.mgt.core.exception.SecretManagementException;
import org.wso2.carbon.identity.secret.mgt.core.model.ResolvedSecret;
import org.wso2.carbon.identity.secret.mgt.core.model.Secret;

import java.util.Locale;

import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ACCESS_TOKEN_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.API_KEY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.API_KEY_VALUE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.BASIC;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.BEARER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CLIENT_CREDENTIAL;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CLIENT_ID;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CLIENT_SECRET;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.INTERNAL_ACCESS_TOKEN;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PASSWORD;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PASSWORD_CREDENTIAL;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.REFRESH_TOKEN_PROP;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SECRET_PROPERTIES;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.USERNAME;

/**
 * This class is used to encrypt and decrypt the secret properties of the notification sender.
 */
public class NotificationSenderSecretProcessor {

    // SMS's own Authentication.Property.USERNAME is the literal "username" (lowercase) - distinct from the
    // shared NotificationSenderManagementConstants.USERNAME ("userName", camelCase) that Email's schema uses.
    private static final String SMS_USERNAME_PROPERTY = "username";

    // SMS's own Authentication.Property.VALUE is the literal "value" - distinct from the shared
    // NotificationSenderManagementConstants.API_KEY_VALUE ("apiKeyValue") that Email's schema uses.
    private static final String SMS_API_KEY_VALUE_PROPERTY = "value";

    /**
     * Encrypt secret property.
     *
     * @param notificationSender Notification Sender: EMAIL_PROVIDER.
     * @param authType           Authentication Type
     * @param property           Authentication Property.
     * @param value              Authentication Property Value.
     * @return Reference of the secret.
     * @throws SecretManagementException If an error occurs while encrypting the secret.
     */
    public static String encryptCredential(String notificationSender, String authType, String property, String value)
            throws SecretManagementException {

        String secretName = buildSecretName(notificationSender, authType, property);
        String secretType = notificationSender + SECRET_PROPERTIES;
        if (isSecretPropertyExists(secretType, secretName)) {
            updateExistingSecretProperty(secretType, secretName, value);
        } else {
            addNewNotificationSenderSecretProperty(secretType, secretName, value);
        }
        return buildSecretReference(secretType, secretName);
    }

    /**
     * Decrypt secret property.
     *
     * @param notificationSender Notification Sender: EMAIL_PROVIDER.
     * @param authType           Authentication Type
     * @param property           Authentication Property.
     * @throws SecretManagementException If an error occurs while decrypting the secret.
     */
    public static String decryptCredential(String notificationSender, String authType, String property)
            throws SecretManagementException {

        String secretName = buildSecretName(notificationSender, authType, property);
        String secretType = notificationSender + SECRET_PROPERTIES;
        if (!isSecretPropertyExists(secretType, secretName)) {
            throw new SecretManagementException(String.format("Unable to find the Secret Property: %s of " +
                    "Auth Type: %s and Action ID: %s from the system.", property, authType, notificationSender));
        }
        ResolvedSecret resolvedSecret = NotificationSenderTenantConfigDataHolder.getInstance().getSecretResolveManager()
                .getResolvedSecret(secretType, secretName);

        return resolvedSecret.getResolvedSecretValue();
    }

    /**
     * Delete secret property.
     *
     * @param notificationSender Notification Sender: EMAIL_PROVIDER.
     * @throws SecretManagementException If an error occurs while deleting the secret.
     */
    public static void deleteAssociatedSecrets(String notificationSender)
            throws SecretManagementException {

        deleteSecretsForAuthType(notificationSender, BASIC, PASSWORD, USERNAME);
        deleteSecretsForAuthType(notificationSender, CLIENT_CREDENTIAL, CLIENT_ID, CLIENT_SECRET,
                INTERNAL_ACCESS_TOKEN);
        deleteSecretsForAuthType(notificationSender, BEARER, ACCESS_TOKEN_PROP);
        deleteSecretsForAuthType(notificationSender, API_KEY, API_KEY_VALUE);
        deleteSecretsForAuthType(notificationSender, PASSWORD_CREDENTIAL, CLIENT_ID, CLIENT_SECRET, USERNAME, PASSWORD,
                INTERNAL_ACCESS_TOKEN);
    }

    /**
     * Delete secret property for an SMS sender. Unlike email senders, the SMS sender's cached internal
     * access token is keyed by {@code ACCESS_TOKEN_PROP} ("accessToken"), not {@code INTERNAL_ACCESS_TOKEN}
     * ("internalAccessToken") - so it needs its own cleanup rather than reusing {@link #deleteAssociatedSecrets}.
     *
     * @param notificationSender Notification Sender: SMS_PROVIDER.
     * @throws SecretManagementException If an error occurs while deleting the secret.
     */
    public static void deleteAssociatedSMSSecrets(String notificationSender)
            throws SecretManagementException {

        deleteSecretsBySMSAuthType(notificationSender, BASIC);
        deleteSecretsBySMSAuthType(notificationSender, CLIENT_CREDENTIAL);
        deleteSecretsBySMSAuthType(notificationSender, BEARER);
        deleteSecretsBySMSAuthType(notificationSender, API_KEY);
        deleteSecretsBySMSAuthType(notificationSender, PASSWORD_CREDENTIAL);
    }

    /**
     * Delete SMS secrets for a single authentication type. Used both by {@link #deleteAssociatedSMSSecrets}
     * (sender deletion, all auth types) and by the SMS update flow (a single auth type, when the sender's
     * auth type changes - to avoid leaving the previous auth type's secrets orphaned). Uses SMS's own key
     * names, unlike {@link #deleteSecretsByAuthType} which is Email-keyed and must not be reused for SMS.
     *
     * @param notificationSender Notification Sender: SMS_PROVIDER.
     * @param authType           Authentication Type.
     * @throws SecretManagementException If an error occurs while deleting the secrets.
     */
    public static void deleteSecretsBySMSAuthType(String notificationSender, String authType)
            throws SecretManagementException {

        if (StringUtils.isBlank(authType)) {
            return;
        }
        switch (authType.toUpperCase(Locale.ENGLISH)) {
            case BASIC:
                deleteSecretsForAuthType(notificationSender, BASIC, PASSWORD, SMS_USERNAME_PROPERTY);
                break;
            case CLIENT_CREDENTIAL:
                deleteSecretsForAuthType(notificationSender, CLIENT_CREDENTIAL, CLIENT_ID, CLIENT_SECRET,
                        ACCESS_TOKEN_PROP, REFRESH_TOKEN_PROP);
                break;
            case BEARER:
                deleteSecretsForAuthType(notificationSender, BEARER, ACCESS_TOKEN_PROP);
                break;
            case API_KEY:
                deleteSecretsForAuthType(notificationSender, API_KEY, SMS_API_KEY_VALUE_PROPERTY);
                break;
            case PASSWORD_CREDENTIAL:
                deleteSecretsForAuthType(notificationSender, PASSWORD_CREDENTIAL, CLIENT_ID, CLIENT_SECRET,
                        SMS_USERNAME_PROPERTY, PASSWORD, ACCESS_TOKEN_PROP, REFRESH_TOKEN_PROP);
                break;
            default:
                break;
        }
    }

    /**
     * Delete secrets by authentication type.
     *
     * @param notificationSender Notification Sender: EMAIL_PROVIDER.
     * @param authType           Authentication Type.
     * @throws SecretManagementException If an error occurs while deleting the secrets.
     */
   public static void deleteSecretsByAuthType(String notificationSender, String authType)
                throws SecretManagementException {

       if (StringUtils.isBlank(authType)) {
            return;
       }
       switch (authType.toUpperCase(Locale.ENGLISH)) {
           case BASIC:
               deleteSecretsForAuthType(notificationSender, BASIC, PASSWORD, USERNAME);
               break;
           case CLIENT_CREDENTIAL:
               deleteSecretsForAuthType(notificationSender, CLIENT_CREDENTIAL, CLIENT_ID, CLIENT_SECRET,
                       INTERNAL_ACCESS_TOKEN);
               break;
           case BEARER:
               deleteSecretsForAuthType(notificationSender, BEARER, ACCESS_TOKEN_PROP);
               break;
           case API_KEY:
               deleteSecretsForAuthType(notificationSender, API_KEY, API_KEY_VALUE);
               break;
           case PASSWORD_CREDENTIAL:
               deleteSecretsForAuthType(notificationSender, PASSWORD_CREDENTIAL, CLIENT_ID, CLIENT_SECRET, USERNAME,
                       PASSWORD, INTERNAL_ACCESS_TOKEN);
               break;
           default:
               break;
       }
   }

   /**
     * Delete internal access token secret.
     *
     * @param notificationSender Notification Sender.
     * @throws SecretManagementException If an error occurs while deleting the secret.
     */
   public static void deleteInternalAccessTokenSecret(String notificationSender) throws SecretManagementException {

       deleteInternalAccessTokenSecret(notificationSender, CLIENT_CREDENTIAL);
   }

   /**
     * Delete internal access token secret owned by a specific authentication type.
     *
     * @param notificationSender Notification Sender.
     * @param authType           Authentication Type owning the cached token (CLIENT_CREDENTIAL or
     *                            PASSWORD_CREDENTIAL).
     * @throws SecretManagementException If an error occurs while deleting the secret.
     */
   public static void deleteInternalAccessTokenSecret(String notificationSender, String authType)
           throws SecretManagementException {

       deleteSecretsForAuthType(notificationSender, authType, INTERNAL_ACCESS_TOKEN);
   }

    /**
     * Helper method to delete secrets for a specific authentication type.
     *
     * @param notificationSender Notification Sender.
     * @param authType           Authentication Type.
     * @param properties         Authentication Properties.
     * @throws SecretManagementException If an error occurs while deleting the secrets.
     */
    private static void deleteSecretsForAuthType(String notificationSender, String authType, String... properties)
            throws SecretManagementException {

        for (String property : properties) {
            String secretName = buildSecretName(notificationSender, authType, property);
            String secretType = notificationSender + SECRET_PROPERTIES;
            if (!isSecretPropertyExists(secretType, secretName)) {
                continue;
            }
            NotificationSenderTenantConfigDataHolder.getInstance().getSecretManager().deleteSecret(secretType,
                    secretName);
        }
    }

    /**
     * Create secret name.
     *
     * @param notificationSender Notification Sender.
     * @param authType           Authentication Type.
     * @param authProperty       Authentication Property.
     * @return Secret Name.
     */
    private static String buildSecretName(String notificationSender, String authType, String authProperty) {

        return notificationSender + ":" + authType + ":" + authProperty;
    }

    /**
     * Create secret reference name.
     *
     * @param secretName Name of the secret.
     * @return Secret reference name.
     * @throws SecretManagementException If an error occurs while retrieving the secret type.
     */
    private static String buildSecretReference(String secretType, String secretName) throws SecretManagementException {

        String secretTypeId = NotificationSenderTenantConfigDataHolder.getInstance().getSecretManager()
                .getSecretType(secretType).getId();
        return secretTypeId + ":" + secretName;
    }

    /**
     * Check whether the secret property exists.
     *
     * @param secretName Secret Name.
     * @return True if the secret property exists.
     * @throws SecretManagementException If an error occurs while checking the existence of the secret.
     */
    private static boolean isSecretPropertyExists(String secretType, String secretName)
            throws SecretManagementException {

        return NotificationSenderTenantConfigDataHolder.getInstance().getSecretManager()
                .isSecretExist(secretType, secretName);
    }

    /**
     * Add new Secret for Notification Sender secret type.
     *
     * @param secretType Secret type.
     * @param secretName Name of the secret.
     * @param value      secret value.
     * @throws SecretManagementException If an error occurs while adding the secret.
     */
    private static void addNewNotificationSenderSecretProperty(String secretType, String secretName, String value)
            throws SecretManagementException {

        Secret secret = new Secret();
        secret.setSecretName(secretName);
        secret.setSecretValue(value);
        NotificationSenderTenantConfigDataHolder.getInstance().getSecretManager().addSecret(secretType, secret);
    }

    /**
     * Update an existing secret of Notification Sender secret type.
     *
     * @param secretType Secret type.
     * @param secretName Name of the secret.
     * @param value      secret value.
     * @throws SecretManagementException If an error occurs while adding the secret.
     */
    private static void updateExistingSecretProperty(String secretType, String secretName, String value)
            throws SecretManagementException {

        ResolvedSecret resolvedSecret = NotificationSenderTenantConfigDataHolder.getInstance().getSecretResolveManager()
                .getResolvedSecret(secretType, secretName);
        if (!resolvedSecret.getResolvedSecretValue().equals(value)) {
            NotificationSenderTenantConfigDataHolder.getInstance().getSecretManager()
                    .updateSecretValue(secretType, secretName, value);
        }
    }
}
