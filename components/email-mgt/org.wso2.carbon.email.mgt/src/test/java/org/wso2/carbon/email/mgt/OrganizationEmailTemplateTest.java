/*
 * Copyright (c) 2019-2024, WSO2 LLC. (http://www.wso2.com).
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
package org.wso2.carbon.email.mgt;

import org.apache.commons.lang.StringUtils;
import org.h2.jdbc.JdbcResultSet;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.email.mgt.internal.I18nMgtServiceComponent;
import org.wso2.carbon.email.mgt.store.DBBasedTemplateManager;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.identity.base.IdentityValidationUtil;
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.common.testng.WithH2Database;
import org.wso2.carbon.identity.common.testng.WithRealmService;
import org.wso2.carbon.identity.common.testng.WithRegistry;
import org.wso2.carbon.identity.governance.IdentityMgtConstants;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.identity.organization.management.service.internal.OrganizationManagementDataHolder;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.CONTENT;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NotificationTableColumns.CONTENT_TYPE;

/**
 * Class that contains the test cases for the implementation of Email Template Manager.
 */
@WithCarbonHome
@WithRegistry(injectToSingletons = {I18nMgtDataHolder.class})
@WithRealmService(injectToSingletons = {I18nMgtDataHolder.class})
@WithH2Database(jndiName = "jdbc/WSO2IdentityDB", files = {"dbscripts/h2.sql"})
public class OrganizationEmailTemplateTest {

    private EmailTemplateManagerImpl emailTemplateManager;

    private String tenantDomain = "carbon.super";
    List<NotificationTemplate> defaultEmailTemplate;
    List<NotificationTemplate> defaultSMSTemplate;

    @BeforeMethod
    public void setUp() {

        defaultEmailTemplate = new I18nMgtServiceComponent().loadDefaultTemplatesFromFile(
                NotificationChannels.EMAIL_CHANNEL.getChannelType());
        defaultSMSTemplate = new I18nMgtServiceComponent().loadDefaultTemplatesFromFile(
                NotificationChannels.SMS_CHANNEL.getChannelType());
        I18nMgtDataHolder.getInstance().setDefaultEmailTemplates(defaultEmailTemplate);
        I18nMgtDataHolder.getInstance().setDefaultSMSTemplates(defaultSMSTemplate);

        OrganizationManagementDataHolder.getInstance()
                .setRealmService(I18nMgtDataHolder.getInstance().getRealmService());

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
    @Test(dataProvider = "notificationTemplateDataProvider")
    public void testGetNotificationTemplate(String notificationChannel, String displayName, String type, String locale,
                                            String contentType, byte[] content) throws Exception {

        try (MockedStatic<OrganizationManagementUtil> orgMgtUtil = mockStatic(OrganizationManagementUtil.class)) {
            orgMgtUtil.when(() -> OrganizationManagementUtil.isOrganization(tenantDomain)).thenReturn(false);

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
                assertNotNull(notificationTemplate.getSubject(),
                        "EMAIL notification template must have a subject");
            }
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
    @Test(dataProvider = "invalidNotificationTemplateDataProvider")
    public void testGetNotificationTemplateErrors(String notificationChannel, String displayName, String type,
                                                  String locale, String contentType, boolean isValidTemplate,
                                                  boolean isValidLocale, String errorMsg,
                                                  String expectedErrorCode, byte[] content) throws Exception {

        try (MockedStatic<IdentityValidationUtil> identityValidationUtil = mockStatic(IdentityValidationUtil.class);
             MockedConstruction<JdbcResultSet> ignored = Mockito.mockConstruction(JdbcResultSet.class,
                (mock, context) -> {
                    when(mock.next()).thenReturn(true);
                    InputStream contentBinaryStream = content == null ? null : new ByteArrayInputStream(content);
                    when(mock.getBinaryStream(CONTENT)).thenReturn(contentBinaryStream);
                    when(mock.getString(CONTENT_TYPE)).thenReturn(contentType);
                })) {

            identityValidationUtil.when(
                            () -> IdentityValidationUtil.isValid(anyString(), any(String[].class), any(String[].class)))
                    .thenReturn(isValidTemplate);
            identityValidationUtil.when(
                            () -> IdentityValidationUtil.isValidOverBlackListPatterns(anyString(), anyString()))
                    .thenReturn(isValidLocale);

            try {
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
    }

    /**
     * Test error scenarios of adding a notification template type.
     *
     * @param templateName Notification template name
     * @param channel      Notification channel
     * @param domain       Tenant domain
     * @param errorCode    Expected error code (NOTE: Without the scenario code)
     * @param errorMessage Error message
     * @param scenarioCode Error scenario
     */
    @Test(dataProvider = "addNotificationTemplateTypeProvider")
    public void TestAddNotificationTemplateType(String templateName, String channel, String domain, String errorCode,
                                                String errorMessage, int scenarioCode) {

        try {
            emailTemplateManager
                    .addNotificationTemplateType(templateName, channel, domain);
        } catch (NotificationTemplateManagerException e) {
            String expectedCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(errorCode,
                            I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            assertEquals(e.getErrorCode(), expectedCode, errorMessage);
        }
    }

    /**
     * Test the error scenarios of AddNotificationTemplate method.
     *
     * @param tenantDomain    Tenant domain
     * @param errorCode       Error code
     * @param errorMessage    Error message
     * @param templateContent Contents to build notification template
     * @throws Exception Error in the test scenario
     */
    @Test(dataProvider = "addNotificationTemplateProvider")
    public void testAddNotificationTemplate(String tenantDomain, String errorCode,
                                            String errorMessage, String[] templateContent) throws Exception {

        NotificationTemplate notificationTemplate;
        if (templateContent == null) {
            notificationTemplate = null;
        } else {
            notificationTemplate = buildSampleNotificationTemplate(templateContent);
        }
        try {
            EmailTemplateManagerImpl emailTemplateManager =
                    getTemplateManagerWithMockedDependencies(errorCode, notificationTemplate, tenantDomain);
            emailTemplateManager.addNotificationTemplate(notificationTemplate, tenantDomain);
            throw new Exception("Exception should be thrown for above testing scenarios");
        } catch (NotificationTemplateManagerException e) {
            if (StringUtils.isBlank(e.getErrorCode())) {
                throw new Exception("Error code cannot be NULL", e);
            }
            String expectedCode = I18nEmailUtil.prependOperationScenarioToErrorCode(errorCode,
                    I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            assertEquals(e.getErrorCode(), expectedCode, errorMessage);
        }
    }

    private EmailTemplateManagerImpl getTemplateManagerWithMockedDependencies(String errorCode,
                                                                              NotificationTemplate notificationTemplate,
                                                                              String tenantDomain)
            throws NotificationTemplateManagerServerException {

        EmailTemplateManagerImpl emailTemplateManager;
        emailTemplateManager = this.emailTemplateManager;
        if (I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ERROR_ADDING_TEMPLATE.getCode().equals(errorCode)) {
            try (MockedConstruction<DBBasedTemplateManager> dbBasedTemplateManagerMocked = Mockito.mockConstruction(
                    DBBasedTemplateManager.class)) {
                emailTemplateManager = new EmailTemplateManagerImpl();
                DBBasedTemplateManager dbBasedTemplateManager = dbBasedTemplateManagerMocked.constructed().get(0);
                doThrow(new NotificationTemplateManagerServerException("Error adding template")).when(dbBasedTemplateManager)
                        .addOrUpdateNotificationTemplate(notificationTemplate, null, tenantDomain);
            }
        }
        return emailTemplateManager;
    }

    /**
     * Test for retrieving default notification templates from the config file.
     *
     * @param baseDirectoryPath   Resource folder location
     * @param notificationChannel Notification channel (EMAIL or SMS)
     * @param message             Error message
     * @throws Exception Error in the test scenario
     */
    @Test(dataProvider = "getDefaultNotificationTemplatesList")
    public void testGetDefaultNotificationTemplates(String baseDirectoryPath, String notificationChannel,
                                                    String message) throws Exception {

        int numberOfDefaultTemplates = getNumberOfDefaultTemplates(notificationChannel);
        I18nMgtServiceComponent component = new I18nMgtServiceComponent();
        List<NotificationTemplate> defaultNotificationTemplates =
                component.loadDefaultTemplatesFromFile(notificationChannel);
        assertEquals(defaultNotificationTemplates.size(), numberOfDefaultTemplates, message);
    }

    /**
     * Contains notification templates and error scenarios for addNotificationTemplate API.
     *
     * @return Object[][]
     */
    @DataProvider(name = "getDefaultNotificationTemplatesList")
    private Object[][] getDefaultNotificationTemplatesList() {

        String baseDirectoryPath = Paths.get(System.getProperty("user.dir"),
                "src", "test", "resources").toString();

        String notificationChannel1 = NotificationChannels.SMS_CHANNEL.getChannelType();
        String message1 = "Testing default number of SMS templates : ";

        String notificationChannel2 = NotificationChannels.EMAIL_CHANNEL.getChannelType();
        String message2 = "Testing default number of EMAIL templates : ";

        return new Object[][]{
                {baseDirectoryPath, notificationChannel1, message1},
                {baseDirectoryPath, notificationChannel2, message2}
        };
    }

    /**
     * Contains notification templates and error scenarios for addNotificationTemplate API.
     *
     * @return Object[][]
     */
    @DataProvider(name = "addNotificationTemplateProvider")
    private Object[][] addNotificationTemplateProvider() {

        String tenantDomain = "test domain";
        String displayName = "Test Value";
        String testNotificationChannel = "Test Value";
        String type = "Test Value";
        String contentType = "Test Value";
        String locale = "Test Value";
        String body = "Test Value";
        String subject = "Test Value";
        String footer = "Test Value";
        String smsChannel = NotificationChannels.SMS_CHANNEL.getChannelType();
        String emailChannel = NotificationChannels.EMAIL_CHANNEL.getChannelType();

        String errorCode1 = I18nMgtConstants.ErrorMessages.ERROR_CODE_NULL_TEMPLATE_OBJECT.getCode();
        String message1 = "Empty NotificationTemplate object :";

        String errorCode2 = I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_NAME.getCode();
        String message2 = "Empty template name in the notification template object : ";
        String[] templateContent2 =
                {StringUtils.EMPTY, testNotificationChannel, type, contentType, locale, body, subject, footer};

        String errorCode3 = I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_LOCALE.getCode();
        String message3 = "Empty locale in the notification template object : ";
        String[] templateContent3 =
                {displayName, testNotificationChannel, type, contentType, StringUtils.EMPTY, body, subject, footer};

        String errorCode4 = I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SMS_TEMPLATE.getCode();
        String message4 = "Invalid SMS template : ";
        String[] templateContent4 =
                {displayName, smsChannel, type, contentType, locale, StringUtils.EMPTY, subject, footer};

        String errorCode5 = I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_CHANNEL.getCode();
        String message5 = "Empty notification channel in the notification template object : ";
        String[] templateContent5 = {displayName, StringUtils.EMPTY, type, contentType, locale, body, subject, footer};

        String errorCode6 = I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SMS_TEMPLATE_CONTENT.getCode();
        String message6 = "Invalid content in the SMS template : ";
        String[] templateContent6 = {displayName, smsChannel, type, contentType, locale, body, subject, footer};

        String errorCode7 = I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_EMAIL_TEMPLATE.getCode();
        String message7 = "Invalid EMAIL template : ";
        String[] templateContent7 =
                {displayName, emailChannel, type, contentType, locale, body, StringUtils.EMPTY, footer};

        String errorCode8 = I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ERROR_ADDING_TEMPLATE.getCode();
        String message8 = "Invalid EMAIL template : ";
        String[] templateContent8 =
                {displayName, emailChannel, type, contentType, locale, body, subject, footer};

        return new Object[][]{
                {tenantDomain, errorCode1, message1, null},
                {tenantDomain, errorCode2, message2, templateContent2},
                {tenantDomain, errorCode3, message3, templateContent3},
                {tenantDomain, errorCode4, message4, templateContent4},
                {tenantDomain, errorCode5, message5, templateContent5},
                {tenantDomain, errorCode6, message6, templateContent6},
                {tenantDomain, errorCode7, message7, templateContent7},
                {tenantDomain, errorCode8, message8, templateContent8}
        };
    }

    /**
     * Contains the details of error codes and error scenarios.
     *
     * @return Object[][]
     */
    @DataProvider(name = "addNotificationTemplateTypeProvider")
    private Object[][] addNotificationTemplateTypeProvider() {

        String testTemplateName = "Test template";
        String testTemplate2Name = "Test template 2";
        String testChannel = "Test Channel";
        String testDomain = "Test Domain";

        int testScenario1 = 1;
        String expectedErrorCode1 = I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_NAME.getCode();
        String errorMessage1 = "TEST EMPTY notification template template name : ";

        int testScenario2 = 2;
        String expectedErrorCode2 = I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getCode();
        String errorMessage2 = "TEST already existing resource : ";

        int testScenario3 = 3;
        String expectedErrorCode3 = I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_TEMPLATE.getCode();
        String errorMessage3 = "TEST runtime exception while looking for the resource : ";

        return new Object[][]{
                {StringUtils.EMPTY, testChannel, testDomain, expectedErrorCode1, errorMessage1, testScenario1},
                {testTemplateName, testChannel, testDomain, expectedErrorCode2, errorMessage2, testScenario2},
                {testTemplate2Name, testChannel, testDomain, expectedErrorCode3, errorMessage3, testScenario3},
        };
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

        return new Object[][]{
                {notificationChannel1, notificationTemplateType, notificationTemplateType, locale,
                        templateContentType1, templateContent1},
                {notificationChannel2, notificationTemplateType, notificationTemplateType, locale,
                        contentType, templateContent2}
        };
    }

    /**
     * Contains data for invalid requests.
     *
     * @return Object[][]
     */
    @DataProvider(name = "invalidNotificationTemplateDataProvider")
    private Object[][] invalidNotificationTemplateDataProvider() throws Exception {

        String locale = "en_US";
        String notificationTemplateType = "invalid-accountconfirmation";
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

        return new Object[][]{
                {StringUtils.EMPTY, notificationTemplateType, notificationTemplateType, locale,
                        StringUtils.EMPTY, false, true, errorMsg1, expectedErrorCode1, null},
                {StringUtils.EMPTY, notificationTemplateType, notificationTemplateType, locale,
                        StringUtils.EMPTY, true, false, errorMsg2, expectedErrorCode2, null},
                {notificationChannel1, notificationTemplateType, notificationTemplateType, locale,
                        StringUtils.EMPTY, true, true, StringUtils.EMPTY, expectedErrorCode3, templateContent1},
                {notificationChannel2, notificationTemplateType, notificationTemplateType, locale,
                        contentType, true, true, StringUtils.EMPTY, expectedErrorCode4, templateContent2},
                {notificationChannel3, notificationTemplateType, notificationTemplateType, locale,
                        contentType, true, true, StringUtils.EMPTY, expectedErrorCode5, null
                }
        };
    }

    /**
     * Build a NotificationTemplate model from the given input parameters.
     * NOTE: parameter order : displayName, channel, type, contentType, locale, body, subject, footer
     *
     * @return Notification Template model
     */
    private NotificationTemplate buildSampleNotificationTemplate(String[] templateContent) {

        NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setNotificationChannel(templateContent[1]);
        notificationTemplate.setDisplayName(templateContent[0]);
        notificationTemplate.setType(templateContent[2]);
        notificationTemplate.setContentType(templateContent[3]);
        notificationTemplate.setLocale(templateContent[4]);
        notificationTemplate.setFooter(templateContent[7]);
        notificationTemplate.setBody(templateContent[5]);
        notificationTemplate.setSubject(templateContent[6]);
        return notificationTemplate;
    }

    /**
     * Get the number of default notification templates in the config file.
     *
     * @param notificationChannel Notification channel (EMAIL or SMS)
     * @return Number of default notification templates
     */
    private int getNumberOfDefaultTemplates(String notificationChannel) {

        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            return defaultSMSTemplate.size();
        } else {
            return defaultEmailTemplate.size();
        }
    }
}

