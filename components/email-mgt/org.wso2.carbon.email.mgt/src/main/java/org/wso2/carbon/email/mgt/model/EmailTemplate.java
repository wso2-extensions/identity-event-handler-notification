/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.email.mgt.model;

/**
 * Encapsulates email template data such as email subject,body,footer etc., which use
 * to construct the email template.
 */
public class EmailTemplate {
    private String subject;
    private String body;
    private String footer;
    private String type;
    private String display;
    private String locale;
    private String emailContentType;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public String getTemplateType() {
        return type;
    }

    public void setTemplateType(String templateType) {
        this.type = templateType;
    }

    public String getTemplateDisplayName() {
        return display;
    }

    public void setTemplateDisplayName(String templateDisplayName) {
        this.display = templateDisplayName;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getEmailContentType() {
        return emailContentType;
    }

    public void setEmailContentType(String emailContentType) {
        this.emailContentType = emailContentType;
    }

}
