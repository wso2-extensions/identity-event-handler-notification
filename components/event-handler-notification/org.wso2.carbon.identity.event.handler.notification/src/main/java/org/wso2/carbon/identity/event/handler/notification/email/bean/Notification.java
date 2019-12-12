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

package org.wso2.carbon.identity.event.handler.notification.email.bean;


import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.event.handler.notification.exception.NotificationRuntimeException;
import org.wso2.carbon.email.mgt.model.EmailTemplate;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.regex.Matcher;

public class Notification implements Serializable {

    private String sendTo;
    private String sendFrom;
    private EmailTemplate template;
    private String subject;
    private String body;
    private String footer;
    private Map<String, String> placeHolderData;

    protected Notification(EmailNotificationBuilder builder) {
        this.sendTo = builder.sendTo;
        this.sendFrom = builder.sendFrom;
        this.template = builder.template;
        this.placeHolderData = builder.placeHolderData;
        this.body = replaceTags(template.getBody(), placeHolderData);
        if (StringUtils.isNotEmpty(template.getSubject())) {
            this.subject = replaceTags(template.getSubject(), placeHolderData);
        }
        if (StringUtils.isNotEmpty(template.getFooter())) {
            this.footer = replaceTags(template.getFooter(), placeHolderData);
        }
    }

    private static String replaceTags(String content, Map<String, String> tagsData) {

        for (Map.Entry<String, String> entry : tagsData.entrySet()) {
            try {

                // In email templates the placeholders in a URL, are defined in the format of {{url:key}} -
                // eg:{{url:user-name}}, So that the values should be URL Encoded
                content = content.replaceAll("\\{\\{url:" + entry.getKey() + "\\}\\}", URLEncoder.encode(entry.getValue(),
                        "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw NotificationRuntimeException.error(e.getMessage(), e);
            }

            // Backslashes (\) and dollar signs ($) in the replacement string is not treated as a literals.
            content = content.replaceAll("\\{\\{" + entry.getKey() + "\\}\\}", Matcher.quoteReplacement(entry
                    .getValue()));
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

        // mandatory attributes
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

        public Notification build() {
            return new Notification(this);
        }
    }

}
