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

package org.wso2.carbon.identity.event.handler.notification.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.identity.common.util.IdentityUtils;
import org.wso2.carbon.identity.event.EventConstants;
import org.wso2.carbon.identity.event.handler.notification.NotificationConstants;
import org.wso2.carbon.identity.event.handler.notification.email.bean.Notification;
import org.wso2.carbon.identity.event.handler.notification.exception.NotificationHandlerException;
import org.wso2.carbon.identity.event.handler.notification.internal.NotificationHandlerDataHolder;
import org.wso2.carbon.identity.mgt.IdentityStore;
import org.wso2.carbon.identity.mgt.claim.Claim;
import org.wso2.carbon.identity.mgt.exception.IdentityStoreException;
import org.wso2.carbon.identity.mgt.exception.UserNotFoundException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Notification utils.
 */
public class NotificationUtil {

    private static Logger log = LoggerFactory.getLogger(NotificationUtil.class);
    private static Properties properties = new Properties();

    public static Map<String, String> getUserClaimValues(String uniqueUserId) throws NotificationHandlerException {
        IdentityStore identityStore = NotificationHandlerDataHolder.getInstance().getRealmService().getIdentityStore();
        List<Claim> userClaims;
        Map<String, String> claimsMap = new HashMap<String, String>();

        try {
            userClaims = identityStore.getClaimsOfUser(uniqueUserId);
            if (userClaims != null) {
                for (Claim userClaim : userClaims) {
                    claimsMap.put(userClaim.getClaimUri(), userClaim.getValue());
                }
            }

        } catch (UserNotFoundException | IdentityStoreException e) {
            throw NotificationHandlerException.error("Error occurred while get user's claims", e);
        }
        return claimsMap;
    }

    //todo need to refactor this method
    //    public static Map<String, String> getUserClaimValues(String userName, String domainName, String tenantDomain)
    //            throws IdentityEventException {
    //
    //        RealmService realmService = NotificationHandlerDataHolder.getInstance().getRealmService();
    //        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
    //        UserStoreManager userStoreManager = null;
    //        try {
    //            userStoreManager = realmService.getTenantUserRealm(tenantId).getUserStoreManager();
    //            if (userStoreManager == null) {
    //                String message = "Error occurred while retrieving userStoreManager for tenant " + tenantDomain;
    //                throw new IdentityEventException(message);
    //            } else if (userStoreManager instanceof AbstractUserStoreManager) {
    //                userStoreManager = ((AbstractUserStoreManager) userStoreManager).
    // getSecondaryUserStoreManager(domainName);
    //            }
    //        } catch (UserStoreException e) {
    //            String message = "Error occurred while retrieving user claim values for user " + userName +
    // " in user " + "store " + domainName + " in tenant " + tenantDomain;
    //            throw new IdentityEventException(message, e);
    //        }
    //        return getUserClaimValues(userName, userStoreManager);
    //    }

    public static Map<String, String> getPlaceholderValues(EmailTemplate emailTemplate,
            Map<String, String> placeHolderData, Map<String, String> userClaims) {

        List<String> placeHolders = new ArrayList<>();
        placeHolders.addAll(extractPlaceHolders(emailTemplate.getBody()));
        placeHolders.addAll(extractPlaceHolders(emailTemplate.getSubject()));
        placeHolders.addAll(extractPlaceHolders(emailTemplate.getFooter()));

        if (userClaims != null && !userClaims.isEmpty()) {
            for (String placeHolder : placeHolders) {
                if (placeHolder.contains(NotificationConstants.EmailNotification.USER_CLAIM_PREFIX)) {
                    String userClaim = userClaims
                            .get(NotificationConstants.EmailNotification.WSO2_CLAIM_URI + placeHolder
                                    .substring(placeHolder.lastIndexOf(".") + 1));
                    if (StringUtils.isNotEmpty(userClaim)) {
                        placeHolderData.put(placeHolder, userClaim);
                    }
                }
            }
        }
        return placeHolderData;
    }

    public static List<String> extractPlaceHolders(String value) {

        String exp = "\\{\\{(.*?)\\}\\}";
        Pattern pattern = Pattern.compile(exp);
        Matcher matcher = pattern.matcher(value);
        List<String> placeHolders = new ArrayList<>();
        while (matcher.find()) {
            String group = matcher.group().replace("{{", "").replace("}}", "");
            placeHolders.add(group);
        }
        return placeHolders;
    }

    public static Notification buildNotification(Map<String, String> placeHolderData) throws
            NotificationHandlerException {
        String emailTemplateType = placeHolderData.get(NotificationConstants.EmailNotification.EMAIL_TEMPLATE_TYPE);
        String userUniqueId = placeHolderData.get(EventConstants.EventProperty.USER_UNIQUE_ID);
        Map<String, String> userClaims = NotificationUtil.getUserClaimValues(userUniqueId);

        String locale = NotificationConstants.EmailNotification.LOCALE_DEFAULT;
        if (userClaims.containsKey(NotificationConstants.EmailNotification.CLAIM_URI_LOCALE)) {
            locale = userClaims.get(NotificationConstants.EmailNotification.CLAIM_URI_LOCALE);
        }

        EmailTemplate emailTemplate = null;
        if (StringUtils.isNotBlank(emailTemplateType) && StringUtils.isNotBlank(locale)) {
            try {
                emailTemplate = NotificationHandlerDataHolder.getInstance().getEmailTemplateManager()
                        .getEmailTemplate(locale, emailTemplateType);
            } catch (I18nEmailMgtException e) {
                throw NotificationHandlerException.error("Error while getting email template");
            }
        }

        if (emailTemplate == null) {
            throw NotificationHandlerException.error("Email template can not be null.");
        }

        NotificationUtil.getPlaceholderValues(emailTemplate, placeHolderData, userClaims);

        String sendTo = null;
        if (userClaims.containsKey(NotificationConstants.EmailNotification.CLAIM_URI_EMAIL)) {
            sendTo = userClaims.get(NotificationConstants.EmailNotification.CLAIM_URI_EMAIL);
        }
        if (StringUtils.isEmpty(sendTo)) {
            throw NotificationHandlerException
                    .error("Email notification sending failed. Sending email address is not configured for the user.");
        }

        Notification.EmailNotificationBuilder builder = new Notification.EmailNotificationBuilder(sendTo);
        builder.setTemplate(emailTemplate);
        builder.setPlaceHolderData(placeHolderData);
        Notification emailNotification = builder.build();
        return emailNotification;
    }

    public static void sendEmail(Notification notification) throws NotificationHandlerException {
        Properties properties = getProperties();
        final String username = properties.getProperty(NotificationConstants.SMTPProperty.MAIL_SMTP_USER);
        final String password = properties.getProperty(NotificationConstants.SMTPProperty.MAIL_SMTP_PASSWORD);

        Properties props = new Properties();
        props.put(NotificationConstants.SMTPProperty.MAIL_SMTP_AUTH,
                properties.getProperty(NotificationConstants.SMTPProperty.MAIL_SMTP_AUTH));
        props.put(NotificationConstants.SMTPProperty.MAIL_SMTP_STARTTLS_ENABLE,
                properties.getProperty(NotificationConstants.SMTPProperty.MAIL_SMTP_STARTTLS_ENABLE));
        props.put(NotificationConstants.SMTPProperty.MAIL_SMTP_HOST,
                properties.getProperty(NotificationConstants.SMTPProperty.MAIL_SMTP_HOST));
        props.put(NotificationConstants.SMTPProperty.MAIL_SMTP_PORT,
                properties.getProperty(NotificationConstants.SMTPProperty.MAIL_SMTP_PORT));

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(
                    new InternetAddress(properties.getProperty(NotificationConstants.SMTPProperty.MAIL_SMTP_FROM)));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(notification.getSendTo()));
            message.setSentDate(new Date());
            message.setSubject(notification.getSubject());
            message.setText(notification.getBody() + notification.getFooter());
            Transport.send(message);

        } catch (AddressException e) {
            throw NotificationHandlerException.error("Error wrongly formatted email address", e);
        } catch (MessagingException e) {
            throw NotificationHandlerException.error("Error while creating the massage content", e);
        }

    }

    public static Properties getProperties() {
        return properties;
    }

    public static void loadProperties() {
        InputStream inStream = null;
        Path path = Paths.get(IdentityUtils.getCarbonHomeDirectory(), "conf",
                I18nMgtConstants.EMAIL_CONF_DIRECTORY);

        // Open the default configuration file in carbon conf directory path .
        File messageMgtPropertyFile = new File(path.toUri().getPath(),
                NotificationConstants.EmailNotification.EMAIL_SMTP_CONFIG_PATH);

        try {
            // If the configuration exists in the carbon conf directory, read properties from there
            if (messageMgtPropertyFile.exists()) {
                inStream = new FileInputStream(messageMgtPropertyFile);
            }
            if (inStream != null) {
                properties.load(inStream);
            }
            //Even if the configurations are not found, individual modules can behave themselves without configuration
        } catch (FileNotFoundException e) {
            log.warn("Could not find configuration file for Message Sending module.", e);
        } catch (IOException e) {
            log.warn("Error while opening input stream for property file.", e);
            // Finally close input stream
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch (IOException e) {
                log.error("Error while closing input stream ", e);
            }
        }

    }
}


