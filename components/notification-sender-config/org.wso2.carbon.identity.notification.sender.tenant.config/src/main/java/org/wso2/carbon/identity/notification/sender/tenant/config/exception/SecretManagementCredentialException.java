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

package org.wso2.carbon.identity.notification.sender.tenant.config.exception;

import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage;

/**
 * Unchecked wrapper for a secret-manager failure encountered while encrypting or decrypting a
 * notification sender credential, used at internal call sites whose public method signature must not
 * change to add a new checked exception. Callers that need to surface this as a
 * {@link NotificationSenderManagementServerException} should catch this type and convert it.
 */
public class SecretManagementCredentialException extends RuntimeException {

    private final ErrorMessage errorMessage;

    public SecretManagementCredentialException(ErrorMessage errorMessage, Throwable cause) {

        super(errorMessage.getMessage(), cause);
        this.errorMessage = errorMessage;
    }

    public ErrorMessage getErrorMessage() {

        return errorMessage;
    }
}
