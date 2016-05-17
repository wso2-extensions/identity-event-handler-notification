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

package org.wso2.carbon.identity.event.handler.email.util;

import org.wso2.carbon.identity.event.handler.email.exception.EmailEventServiceException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class NotificationBuilder {

    private NotificationBuilder() {
    }

    public static Notification createNotification(String notificationType, EmailInfoDTO emailInfoFTO, NotificationData data)
            throws EmailEventServiceException {

        String subject = null;
        String body = null;
        String footer = null;
        String emailFormat = null;
        Notification notification = null;

        if ("EMAIL".equals(notificationType)) {
            subject = emailInfoFTO.getSubject();
            body = emailInfoFTO.getBody();
            footer = emailInfoFTO.getFooter();
            emailFormat = emailInfoFTO.getEmailContentType();

            //Replace all the tags in the NotificationData.
            Map<String, String> tagsData = data.getTagsData();
            try {
                subject = replaceTags(tagsData, subject);
                body = replaceTags(tagsData, body);
                footer = replaceTags(tagsData, footer);
            } catch (UnsupportedEncodingException e) {
                throw new EmailEventServiceException("Unsupported encoding while creating notification", e);
            }
            notification = new EmailNotification();
            notification.setSubject(subject);
            notification.setBody(body);
            notification.setFooter(footer);
            notification.setSendFrom(data.getSendFrom());
            notification.setSendTo(data.getSendTo());
            if (emailFormat != null) {
                notification.setContentType(emailFormat);
            }

        }
        return notification;
    }

    private static String replaceTags(Map<String, String> tagsData, String content)
            throws UnsupportedEncodingException {
        for (Map.Entry<String, String> entry : tagsData.entrySet()) {

            String data = entry.getValue();
            String key = entry.getKey();
            if (data != null) {
                content = content.replaceAll("\\{url:" + key + "\\}",
                        URLEncoder.encode(tagsData.get(key), "UTF-8"));
                content = content.replaceAll("\\{" + key + "\\}", tagsData.get(key));
            } else {
                content = content.replaceAll("\\{url:" + key + "\\}", "");
                content = content.replaceAll("\\{" + key + "\\}", "");
            }
        }
        return content;
    }
}
