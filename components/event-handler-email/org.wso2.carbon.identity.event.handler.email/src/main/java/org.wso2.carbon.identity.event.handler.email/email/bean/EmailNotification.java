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

package org.wso2.carbon.identity.event.handler.email.email.bean;


import org.wso2.carbon.identity.event.handler.email.email.model.EmailTemplate;
import org.wso2.carbon.identity.event.handler.email.exception.NotificationEventRuntimeException;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class EmailNotification extends Notification implements Serializable {

    private String sendTo;
    private String sendFrom;
    private EmailTemplate template;
    private String subject;
    private String body;
    private String footer;
    private Map<String, String> placeHolderData;

    protected EmailNotification(EmailNotificationBuilder builder) {
        this.sendTo = builder.sendTo;
        this.sendFrom = builder.sendFrom;
        this.template = builder.template;
        this.placeHolderData = builder.placeHolderData;
        this.subject = replaceTags(template.getSubject(), placeHolderData);
        this.body = replaceTags(template.getBody(), placeHolderData);
        this.footer = replaceTags(template.getFooter(), placeHolderData);
    }

    private static String replaceTags(String content, Map<String, String> tagsData) {

        for (Map.Entry<String, String> entry : tagsData.entrySet()) {
            try {
                content = content.replaceAll("\\{\\{url:" + entry.getKey() + "\\}\\}", URLEncoder.encode(entry.getValue(),
                        "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw NotificationEventRuntimeException.error(e.getMessage(), e);
            }
            content = content.replaceAll("\\{\\{" + entry.getKey() + "\\}\\}", entry.getValue());
        }
        return content;
    }

    public String getSendTo() {
        return this.sendTo;
    }

    public String getSendFrom() {
        return this.sendFrom;
    }

    public EmailTemplate getTemplate() {
        return this.template;
    }

    public String getSubject() {
        return this.subject;
    }

    public String getBody() {
        return this.body;
    }

    public String getFooter() {
        return this.footer;
    }

    public static class EmailNotificationBuilder {

        private String sendTo;
        private String sendFrom;
        private EmailTemplate template;
        private Map<String, String> placeHolderData;

        public EmailNotificationBuilder(String sendTo) {
            this.sendTo = sendTo;
        }

        public void setTemplate(EmailTemplate template) {
            this.template = template;
        }

        public void setSendFrom(String sendFrom) {
            this.sendFrom = sendFrom;
        }

        public void setPlaceHolderData(Map<String, String> placeHolderData) {
            this.placeHolderData = placeHolderData;
        }

        public void addPlaceHolderData(String key, String value) {
            this.placeHolderData.put(key, value);
        }

        public EmailNotification build() {
            return new EmailNotification(this);
        }
    }

}
