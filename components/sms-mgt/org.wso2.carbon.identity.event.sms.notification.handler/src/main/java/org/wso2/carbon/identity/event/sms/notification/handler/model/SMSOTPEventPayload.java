/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.event.sms.notification.handler.model;

import org.wso2.carbon.event.outbound.adapter.websubhub.model.EventPayload;

/**
 * Model Class for SMS OTP Event Payload.
 */
public class SMSOTPEventPayload extends EventPayload {

    private String userId;
    private String userName;
    private String messageBody;
    private String sendTo;

    public String getUserId() {

        return userId;
    }

    public void setUserId(String userId) {

        this.userId = userId;
    }

    public String getUserName() {

        return userName;
    }

    public void setUserName(String userName) {

        this.userName = userName;
    }

    public String getMessageBody() {

        return messageBody;
    }

    public void setMessageBody(String messageBody) {

        this.messageBody = messageBody;
    }

    public String getSendTo() {

        return sendTo;
    }

    public void setSendTo(String sendTo) {

        this.sendTo = sendTo;
    }
}
