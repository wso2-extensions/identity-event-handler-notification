/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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

package org.wso2.carbon.identity.event.handler.email.email.model;

import java.io.Serializable;

public class EmailTemplate implements Serializable {

    private static final long serialVersionUID = -8854592668563155088L;

    private String notificationEvent;
    private String subject;
    private String body;
    private String footer;
    private String locale;
    private String contentType;

    public EmailTemplate(String notificationEvent, String subject, String body, String footer, String locale,
                         String contentType) {
        this.notificationEvent = notificationEvent;
        this.subject = subject;
        this.body = body;
        this.footer = footer;
        this.locale = locale;
        this.contentType = contentType;
    }

    public String getNotificationEvent() {
        return this.notificationEvent;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getFooter() {
        return footer;
    }

    public String getLocale() {
        return locale;
    }

    public String getContentType() {
        return contentType;
    }

}
