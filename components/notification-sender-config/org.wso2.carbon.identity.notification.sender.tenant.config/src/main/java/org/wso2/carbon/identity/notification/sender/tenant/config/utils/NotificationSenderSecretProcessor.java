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

import org.wso2.carbon.identity.notification.sender.tenant.config.internal.NotificationSenderTenantConfigDataHolder;
import org.wso2.carbon.identity.secret.mgt.core.exception.SecretManagementException;
import org.wso2.carbon.identity.secret.mgt.core.model.ResolvedSecret;
import org.wso2.carbon.identity.secret.mgt.core.model.Secret;

import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.BASIC;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CLIENT_CREDENTIAL;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CLIENT_ID;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CLIENT_SECRET;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PASSWORD;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SECRET_PROPERTIES;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.USERNAME;

/**
 * This class is used to encrypt and decrypt the secret properties of the notification sender.
 */
public class NotificationSenderSecretProcessor {

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
        deleteSecretsForAuthType(notificationSender, CLIENT_CREDENTIAL, CLIENT_ID, CLIENT_SECRET);
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
                return;
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
