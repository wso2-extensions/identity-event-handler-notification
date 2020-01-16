/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.email.mgt;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import org.apache.commons.lang.StringUtils;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.IObjectFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import static org.powermock.api.mockito.PowerMockito.when;
import static org.testng.Assert.*;

import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.identity.base.IdentityValidationUtil;
import org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService;
import org.wso2.carbon.identity.governance.IdentityMgtConstants;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.registry.core.Resource;

/**
 * Class that contains the test cases for the implementation of Email Template Manager.
 */
@PrepareForTest({ IdentityValidationUtil.class, I18nMgtDataHolder.class })
public class EmailTemplateManagerImplTest extends PowerMockTestCase {

    private EmailTemplateManagerImpl emailTemplateManager;

    @Mock
    RegistryResourceMgtService resourceMgtService;

    @Mock
    I18nMgtDataHolder i18nMgtDataHolder;

    @Mock
    Resource resource;

    @ObjectFactory
    public IObjectFactory getObjectFactory() {

        return new org.powermock.modules.testng.PowerMockObjectFactory();
    }

    private String tenantDomain = "carbon.super";

    @BeforeMethod
    public void setUp() {

        initMocks(this);
        mockStatic(I18nMgtDataHolder.class);
        i18nMgtDataHolder = PowerMockito.mock(I18nMgtDataHolder.class);
        when(I18nMgtDataHolder.getInstance()).thenReturn(i18nMgtDataHolder);

        // Mock RegistryResourceMgtService.
        when(i18nMgtDataHolder.getRegistryResourceMgtService()).thenReturn(resourceMgtService);
        emailTemplateManager = new EmailTemplateManagerImpl();
    }

    /**
     * Contains the test scenarios for getting notification template using the notification channel.
     *
     * @param notificationChannel Notification channel
     * @param displayName         Display name
     * @param type                Type
     * @param locale              Locale
     * @param contentType         Content type
     * @param content             Template content
     * @throws Exception Error testing getNotificationTemplate implementation.
     */
    @Test(dataProvider = "notificationTemplateDataProvider", enabled = true)
    public void testGetNotificationTemplate(String notificationChannel, String displayName, String type, String locale,
            String contentType, byte[] content) throws Exception {

        mockRegistryResource(notificationChannel, displayName, type, locale, contentType, content);
        mockIsValidTemplate(true, true);
        NotificationTemplate notificationTemplate = emailTemplateManager
                .getNotificationTemplate(notificationChannel, type, locale, tenantDomain);
        assertNotNull(notificationTemplate);
        assertNotNull(notificationTemplate.getBody(), "Template should have a notification body");
        assertEquals(notificationTemplate.getNotificationChannel(), notificationChannel);

        // Validate not having subject or footer in SMS notification template.
        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            assertNull(notificationTemplate.getFooter(), "SMS notification template cannot have a footer");
            assertNull(notificationTemplate.getSubject(), "SMS notification template cannot have a subject");
        } else {
            assertNotNull(notificationTemplate.getFooter(), "EMAIL notification template must have a footer");
            assertNotNull(notificationTemplate.getSubject(), "EMAIL notification template must have a subject");
        }
    }

    /**
     * Contains the error scenarios for resolving notification template.
     *
     * @param notificationChannel Notification channel
     * @param displayName         Display name
     * @param type                Type
     * @param locale              Locale
     * @param content             Template content
     * @param isValidTemplate     Is valid template
     * @param isValidLocale       Is valid locale
     * @param errorMsg            Error message
     * @param expectedErrorCode   Expected error code
     * @param contentType         Content type
     * @throws Exception Error testing getNotificationTemplate implementation.
     */
    @Test(dataProvider = "invalidNotificationTemplateDataProvider", enabled = true)
    public void testGetNotificationTemplateErrors(String notificationChannel, String displayName, String type,
            String locale, String contentType, boolean isValidTemplate, boolean isValidLocale, String errorMsg,
            String expectedErrorCode, byte[] content) throws Exception {

        mockIsValidTemplate(isValidTemplate, isValidLocale);
        try {
            mockRegistryResource(notificationChannel, displayName, type, locale, contentType, content);
            NotificationTemplate notificationTemplate = emailTemplateManager
                    .getNotificationTemplate(notificationChannel, type, locale, tenantDomain);
            assertNull(notificationTemplate, "Cannot return a notificationTemplate");
        } catch (NotificationTemplateManagerException e) {
            String errorCode = e.getErrorCode();
            assertNotNull(errorCode, "Error code cannot be empty");
            if (StringUtils.isEmpty(errorMsg)) {
                errorMsg = e.getMessage();
            }
            assertEquals(errorCode, expectedErrorCode, errorMsg);
        }
    }

    /**
     * Mock registry resource for notification template.
     *
     * @param notificationChannel Notification channel
     * @param displayName         Notification template displayName
     * @param templateType        Notification template type
     * @param locale              Notification template locale
     * @param contentType         Notification template content type
     * @param templateContent     Notification template content (Subject,body,footer etc)
     * @throws Exception Error mocking notification template
     */
    private void mockRegistryResource(String notificationChannel, String displayName, String templateType,
            String locale, String contentType, byte[] templateContent) throws Exception {

        when(resourceMgtService.getIdentityResource(Matchers.anyString(), Matchers.anyString(), Matchers.anyString()))
                .thenReturn(resource);

        // Mock Resource properties.
        when(resource.getProperty(I18nMgtConstants.TEMPLATE_TYPE_DISPLAY_NAME)).thenReturn(displayName);
        when(resource.getProperty(I18nMgtConstants.TEMPLATE_TYPE)).thenReturn(templateType);
        when(resource.getProperty(I18nMgtConstants.TEMPLATE_LOCALE)).thenReturn(locale);
        if (NotificationChannels.EMAIL_CHANNEL.getChannelType().equals(notificationChannel)) {
            when(resource.getProperty(I18nMgtConstants.TEMPLATE_CONTENT_TYPE)).thenReturn(contentType);
        }
        when(resource.getContent()).thenReturn(templateContent);
    }

    /**
     * Mocks IdentityValidationUtil template validation methos.
     *
     * @param isValidTemplate Whether the template is valid or not
     * @param isValidLocale   Whether the locate is valid or not
     */
    private void mockIsValidTemplate(boolean isValidTemplate, boolean isValidLocale) {

        mockStatic(IdentityValidationUtil.class);

        // Mock methods in validateTemplateType method.
        when(IdentityValidationUtil
                .isValid(Matchers.anyString(), Matchers.any(String[].class), Matchers.any(String[].class)))
                .thenReturn(isValidTemplate);

        // Mock methods in validateLocale method.
        when(IdentityValidationUtil.isValidOverBlackListPatterns(Matchers.anyString(), Matchers.anyString())).
                thenReturn(isValidLocale);
    }

    /**
     * Contains the template details and the expected outcome for the scenarios.
     *
     * @return Object[][]
     * @throws Exception Error converting to a byte []
     */
    @DataProvider(name = "notificationTemplateDataProvider")
    private Object[][] notificationTemplateDataProvider() throws Exception {

        String locale = "en_US";
        String notificationTemplateType = "accountconfirmation";
        String charsetName = "UTF-8";
        String contentType = "html/plain";

        // Template 1: SMS.
        String notificationChannel1 = NotificationChannels.SMS_CHANNEL.getChannelType();
        String templateContentType1 = StringUtils.EMPTY;
        byte[] templateContent1 = "[\"body\"]".getBytes(charsetName);

        // Template 2: EMAIL.
        String notificationChannel2 = NotificationChannels.EMAIL_CHANNEL.getChannelType();
        byte[] templateContent2 = "[\"subject\",\"body\",\"footer\"]".getBytes(charsetName);

        return new Object[][] {
                { notificationChannel1, notificationTemplateType, notificationTemplateType, locale,
                        templateContentType1, templateContent1 },
                { notificationChannel2, notificationTemplateType, notificationTemplateType, locale,
                  contentType, templateContent2}
        };
    }

    /**
     * Contains data for invalid requests.
     *
     * @return Object[][]
     */
    @DataProvider(name = "invalidNotificationTemplateDataProvider")
    private Object[][] invalidNotificationTemplateDataProvider() throws Exception{

        String locale = "en_US";
        String notificationTemplateType = "accountconfirmation";
        String charsetName = "UTF-8";
        String contentType = "html/plain";

        // Invalid template type.
        String errorMsg1 = "Invalid template type : ";
        String expectedErrorCode1 = I18nEmailUtil.prependOperationScenarioToErrorCode(
                I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_TEMPLATE_NAME.getCode(),
                I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);

        // Invalid template locale.
        String errorMsg2 = "Invalid template locale : ";
        String expectedErrorCode2 =
                I18nEmailUtil.prependOperationScenarioToErrorCode(
                        I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_LOCALE.getCode(),
                        I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);

        // Template 1: SMS.
        String notificationChannel1 = NotificationChannels.SMS_CHANNEL.getChannelType();
        String expectedErrorCode3 = IdentityMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SMS_TEMPLATE_CONTENT
                .getCode();
        byte[] templateContent1 = "[\"subject\",\"body\",\"footer\"]".getBytes(charsetName);

        // Template 2: EMAIL.
        String notificationChannel2 = NotificationChannels.EMAIL_CHANNEL.getChannelType();
        byte[] templateContent2 = "[\"body\"]".getBytes(charsetName);
        String expectedErrorCode4 = IdentityMgtConstants.ErrorMessages.ERROR_CODE_INVALID_EMAIL_TEMPLATE_CONTENT
                .getCode();

        // No content in the EMAIL template.
        String notificationChannel3 = NotificationChannels.EMAIL_CHANNEL.getChannelType();
        String expectedErrorCode5 = IdentityMgtConstants.ErrorMessages.ERROR_CODE_NO_CONTENT_IN_TEMPLATE
                .getCode();

        return new Object[][] {
                { StringUtils.EMPTY, notificationTemplateType, notificationTemplateType, locale,
                  StringUtils.EMPTY, false, true, errorMsg1, expectedErrorCode1, null },
                { StringUtils.EMPTY, notificationTemplateType, notificationTemplateType, locale,
                  StringUtils.EMPTY, true, false, errorMsg2, expectedErrorCode2, null },
                { notificationChannel1, notificationTemplateType, notificationTemplateType, locale,
                  StringUtils.EMPTY, true, true, StringUtils.EMPTY, expectedErrorCode3, templateContent1 },
                { notificationChannel2, notificationTemplateType, notificationTemplateType, locale,
                  contentType, true, true, StringUtils.EMPTY, expectedErrorCode4, templateContent2 },
                { notificationChannel3, notificationTemplateType, notificationTemplateType, locale,
                  contentType, true, true, StringUtils.EMPTY, expectedErrorCode5, null
                }
        };
    }
}