/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage;

/**
 * Exception for NotificationSenderManagement.
 */
public class NotificationSenderManagementException extends Exception {

    private final ErrorMessage errorMessage;
    private final String descriptionData;

    public NotificationSenderManagementException(ErrorMessage errorMessage, String descriptionData,  Throwable e) {

        super(e);
        this.errorMessage = errorMessage;
        this.descriptionData = descriptionData;
    }

    public NotificationSenderManagementException(ErrorMessage errorMessage, String descriptionData) {

        this.errorMessage = errorMessage;
        this.descriptionData = descriptionData;
    }

    public NotificationSenderManagementException(ErrorMessage errorMessage) {

        this.errorMessage = errorMessage;
        this.descriptionData = null;
    }

    public String getErrorCode() {

        return errorMessage.getCode();
    }

    public String getMessage() {

        return errorMessage.getMessage();
    }

    public String getDescription() {

        return getErrorDescriptionWithData(errorMessage, descriptionData);
    }

    /**
     * Include context data to error message.
     *
     * @param error Error message.
     * @param data  Context data.
     * @return Formatted error message.
     */
    private static String getErrorDescriptionWithData(ErrorMessage error, String data) {

        String message;
        if (StringUtils.isNotBlank(data)) {
            message = String.format(error.getDescription(), data);
        } else {
            message = error.getDescription();
        }
        return message;
    }
}
