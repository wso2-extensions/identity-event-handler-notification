/*
 * Copyright (c) 2022-2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.event.handler.notification.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.lang.StringUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.email.mgt.EmailTemplateManager;
import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.core.ServiceURL;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.internal.IdentityCoreServiceComponent;
import org.wso2.carbon.identity.core.util.IdentityConfigParser;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.notification.NotificationConstants;
import org.wso2.carbon.identity.event.handler.notification.internal.NotificationHandlerDataHolder;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertEquals;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.EmailNotification.ORGANIZATION_NAME_PLACEHOLDER;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.EmailNotification.UTM_PARAMETERS_PLACEHOLDER;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.EmailNotification.UTM_PARAMETER_PREFIX;

/**
 * Class that contains the test cases for NotificationUtil class.
 */
public class NotificationUtilTest {

    // Placeholders.
    String ORGANIZATION_LOGO_URL_PLACEHOLDER = "organization.logo.img";
    String ORGANIZATION_LOGO_ALT_TEXT_PLACEHOLDER = "organization.logo.altText";
    String ORGANIZATION_COPYRIGHT_TEXT_PLACEHOLDER = "organization.copyright.text";
    String ORGANIZATION_SUPPORT_EMAIL_PLACEHOLDER = "organization.support.mail";
    String ORGANIZATION_PRIMARY_COLOR_PLACEHOLDER = "organization.color.primary";
    String ORGANIZATION_BACKGROUND_COLOR_PLACEHOLDER = "organization.color.background";
    String ORGANIZATION_FONT_PLACEHOLDER = "organization.font";
    String ORGANIZATION_FONT_COLOR_PLACEHOLDER = "organization.font.color";
    String ORGANIZATION_BUTTON_FONT_COLOR_PLACEHOLDER = "organization.button.font.color";
    String ORGANIZATION_THEME_BACKGROUND_COLOR_PLACEHOLDER = "organization.theme.background.color";
    String ORGANIZATION_THEME_BORDER_COLOR_PLACEHOLDER = "organization.theme.border.color";

    // Sample values for branding fallbacks.
    String ORGANIZATION_LIGHT_LOGO_URL_FALLBACK = "https://example.com/logo";
    String ORGANIZATION_COPYRIGHT_TEXT_FALLBACK = "Fallback Copyright Text";
    String ORGANIZATION_SUPPORT_EMAIL_FALLBACK = "fallback@support.com";
    String ORGANIZATION_PRIMARY_COLOR_FALLBACK = "#3CB371";
    String ORGANIZATION_BACKGROUND_COLOR_FALLBACK = "#94DBB4";
    String ORGANIZATION_FONT_FALLBACK = "Arial";
    String ORGANIZATION_FONT_COLOR_FALLBACK = "#353230";
    String ORGANIZATION_BUTTON_FONT_COLOR_FALLBACK = "#FFFFFF";
    String ORGANIZATION_LIGHT_BACKGROUND_COLOR_FALLBACK = "#F2F3F4";
    String ORGANIZATION_DARK_BACKGROUND_COLOR_FALLBACK = "#212F3D";
    String ORGANIZATION_LIGHT_BORDER_COLOR_FALLBACK = "#D5D8DC";
    String ORGANIZATION_DARK_BORDER_COLOR_FALLBACK = "#AEB6BF";

    // Sample values for branding preferences.
    String ORGANIZATION_LIGHT_LOGO_URL = "light.logo.url";
    String ORGANIZATION_DARK_LOGO_URL = "dark.logo.url";
    String ORGANIZATION_LIGHT_LOGO_ALT_TEXT = "light.logo.alt.text";
    String ORGANIZATION_DARK_LOGO_ALT_TEXT =  "dark.logo.alt.text";
    String ORGANIZATION_COPYRIGHT_TEXT = "Example Copyright Text";
    String ORGANIZATION_SUPPORT_EMAIL = "support@example.com";
    String ORGANIZATION_LIGHT_PRIMARY_COLOR = "#EE2B34";
    String ORGANIZATION_DARK_PRIMARY_COLOR = "#00529F";
    String ORGANIZATION_LIGHT_BACKGROUND_COLOR = "#CCE6FF";
    String ORGANIZATION_DARK_BACKGROUND_COLOR = "#CCCEEE";
    String ORGANIZATION_LIGHT_FONT = "Arial";
    String ORGANIZATION_DARK_FONT = "Arial Black";
    String ORGANIZATION_LIGHT_FONT_COLOR = "#000000";
    String ORGANIZATION_DARK_FONT_COLOR = "#FFFFFF";
    String ORGANIZATION_LIGHT_BUTTON_FONT_COLOR = "#F7F9F9";
    String ORGANIZATION_DARK_BUTTON_FONT_COLOR = "#FBFCFC";

    String LIGHT_THEME = "LIGHT";
    String DARK_THEME = "DARK";

    String BRANDING_ENABLED = "true";
    String BRANDING_DISABLED = "false";

    private static final String DUMMY_PROTOCOL = "https";
    private static final String SAMPLE_EMAIL = "test@test.com";
    private static final String SAMPLE_ORGANIZATION_NAME = "OrganizationA";
    private static final String SAMPLE_LOCALE = "fr-FR";
    private static final String SAMPLE_EMAIL_BODY = "SampleEmailBody";

    private static final String ACCOUNT_RECOVERY_ENDPOINT_URL = "https://example.com/account/recovery";
    private static final String AUTHENTICATION_ENDPOINT_URL = "https://example.com/authentication";

    int CASE_1 = 1;
    int CASE_2 = 2;
    int CASE_3 = 3;
    int CASE_4 = 4;

    @Mock
    EmailTemplateManager mockEmailTemplateManager;

    @Mock
    EmailTemplate mockEmailTemplate;

    @Mock
    ConfigurationContextService configurationContextService;

    @Mock
    ConfigurationContext configurationContext;

    @Mock
    AxisConfiguration axisConfiguration;

    @Mock
    ServerConfiguration serverConfiguration;

    @BeforeMethod
    public void setUp() {

        initMocks(this);
    }

    @DataProvider(name = "GetBrandingPreferenceDataProvider")
    public Object[][] provideTestData() {

        String brandingPreferencesStr = "{" +
                "\"configs\":{\"isBrandingEnabled\":%s}," +
                "\"urls\": {\"recoveryPortalURL\":\"%s\"}," +
                "\"organizationDetails\":{\"copyrightText\":\"%s\",\"supportEmail\":\"%s\"}," +
                "\"theme\":{\"activeTheme\":\"%s\"," +
                "\"LIGHT\":{\"buttons\":{\"primary\":{\"base\":{\"font\":{\"color\":\"%s\"}}}}," +
                "\"colors\":{\"primary\":\"%s\"},\"images\":{\"logo\": {\"altText\": \"%s\",\"imgURL\": \"%s\"}}," +
                "\"page\":{\"background\":{\"backgroundColor\":\"%s\"},\"font\":{\"color\":\"%s\"}}," +
                "\"typography\":{\"font\":{\"fontFamily\":\"%s\"}}}," +
                "\"DARK\":{\"buttons\":{\"primary\":{\"base\":{\"font\":{\"color\":\"%s\"}}}}," +
                "\"colors\":{\"primary\":\"%s\"},\"images\":{\"logo\": {\"altText\": \"%s\",\"imgURL\": \"%s\"}}," +
                "\"page\":{\"background\":{\"backgroundColor\":\"%s\"},\"font\":{\"color\":\"%s\"}}," +
                "\"typography\":{\"font\":{\"fontFamily\":\"%s\"}}}}}";

        String brandingPreferencesStr1 = String.format(brandingPreferencesStr,
                BRANDING_ENABLED, ORGANIZATION_COPYRIGHT_TEXT, ORGANIZATION_SUPPORT_EMAIL, LIGHT_THEME,
                ORGANIZATION_LIGHT_BUTTON_FONT_COLOR, ORGANIZATION_LIGHT_PRIMARY_COLOR, ORGANIZATION_LIGHT_LOGO_ALT_TEXT,
                ORGANIZATION_LIGHT_LOGO_URL, ORGANIZATION_LIGHT_BACKGROUND_COLOR, ORGANIZATION_LIGHT_FONT_COLOR,
                ORGANIZATION_LIGHT_FONT, ORGANIZATION_DARK_BUTTON_FONT_COLOR, ORGANIZATION_DARK_PRIMARY_COLOR,
                ORGANIZATION_DARK_LOGO_ALT_TEXT, ORGANIZATION_DARK_LOGO_URL, ORGANIZATION_DARK_BACKGROUND_COLOR,
                ORGANIZATION_DARK_FONT_COLOR, ORGANIZATION_DARK_FONT);
        String brandingPreferencesStr2 = String.format(brandingPreferencesStr,
                BRANDING_ENABLED, "", ORGANIZATION_COPYRIGHT_TEXT, ORGANIZATION_SUPPORT_EMAIL, DARK_THEME,
                ORGANIZATION_LIGHT_BUTTON_FONT_COLOR, ORGANIZATION_LIGHT_PRIMARY_COLOR, ORGANIZATION_LIGHT_LOGO_ALT_TEXT,
                ORGANIZATION_LIGHT_LOGO_URL, ORGANIZATION_LIGHT_BACKGROUND_COLOR, ORGANIZATION_LIGHT_FONT_COLOR,
                ORGANIZATION_LIGHT_FONT, ORGANIZATION_DARK_BUTTON_FONT_COLOR, ORGANIZATION_DARK_PRIMARY_COLOR,
                ORGANIZATION_DARK_LOGO_ALT_TEXT, ORGANIZATION_DARK_LOGO_URL, ORGANIZATION_DARK_BACKGROUND_COLOR,
                ORGANIZATION_DARK_FONT_COLOR, ORGANIZATION_DARK_FONT);
        String brandingPreferencesStr3 = String.format(brandingPreferencesStr,
                BRANDING_DISABLED, ORGANIZATION_COPYRIGHT_TEXT, ORGANIZATION_SUPPORT_EMAIL, LIGHT_THEME,
                ORGANIZATION_LIGHT_BUTTON_FONT_COLOR, ORGANIZATION_LIGHT_PRIMARY_COLOR, ORGANIZATION_LIGHT_LOGO_ALT_TEXT,
                ORGANIZATION_LIGHT_LOGO_URL, ORGANIZATION_LIGHT_BACKGROUND_COLOR, ORGANIZATION_LIGHT_FONT_COLOR,
                ORGANIZATION_LIGHT_FONT, ORGANIZATION_DARK_BUTTON_FONT_COLOR, ORGANIZATION_DARK_PRIMARY_COLOR,
                ORGANIZATION_DARK_LOGO_ALT_TEXT, ORGANIZATION_DARK_LOGO_URL, ORGANIZATION_DARK_BACKGROUND_COLOR,
                ORGANIZATION_DARK_FONT_COLOR, ORGANIZATION_DARK_FONT);

        JsonNode brandingPreferences1, brandingPreferences2, brandingPreferences3;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            brandingPreferences1 = objectMapper.readTree(brandingPreferencesStr1);
            brandingPreferences2 = objectMapper.readTree(brandingPreferencesStr2);
            brandingPreferences3 = objectMapper.readTree(brandingPreferencesStr3);
        } catch (JsonProcessingException e) {
            brandingPreferences1 = null;
            brandingPreferences2 = null;
            brandingPreferences3 = null;
        }

        Map<String, String> brandingFallbacks = new HashMap<>();
        brandingFallbacks.put("light_logo_url", ORGANIZATION_LIGHT_LOGO_URL_FALLBACK);
        brandingFallbacks.put("copyright_text", ORGANIZATION_COPYRIGHT_TEXT_FALLBACK);
        brandingFallbacks.put("support_mail", ORGANIZATION_SUPPORT_EMAIL_FALLBACK);
        brandingFallbacks.put("primary_color", ORGANIZATION_PRIMARY_COLOR_FALLBACK);
        brandingFallbacks.put("background_color", ORGANIZATION_BACKGROUND_COLOR_FALLBACK);
        brandingFallbacks.put("light_background_color", ORGANIZATION_LIGHT_BACKGROUND_COLOR_FALLBACK);
        brandingFallbacks.put("dark_background_color", ORGANIZATION_DARK_BACKGROUND_COLOR_FALLBACK);
        brandingFallbacks.put("font_style", ORGANIZATION_FONT_FALLBACK);
        brandingFallbacks.put("font_color", ORGANIZATION_FONT_COLOR_FALLBACK);
        brandingFallbacks.put("button_font_color", ORGANIZATION_BUTTON_FONT_COLOR_FALLBACK);
        brandingFallbacks.put("light_border_color", ORGANIZATION_LIGHT_BORDER_COLOR_FALLBACK);
        brandingFallbacks.put("dark_border_color", ORGANIZATION_DARK_BORDER_COLOR_FALLBACK);

        return new Object[][] {
                {brandingPreferences1, brandingFallbacks, CASE_1},
                {brandingPreferences2, brandingFallbacks, CASE_2},
                {brandingPreferences3, brandingFallbacks, CASE_3},
                {null, brandingFallbacks, CASE_4}
        };
    }

    @DataProvider(name = "GetPlaceholderValuesDataProvider")
    public Object[][] providePlaceholderValuesTestData() {

        EmailTemplate emailTemplate = new EmailTemplate();
        emailTemplate.setSubject("Sample Subject");
        emailTemplate.setBody("Test email body. UTM Parameter query string : {{" + UTM_PARAMETERS_PLACEHOLDER + "}}.");
        emailTemplate.setFooter("Sample Footer");
        emailTemplate.setLocale("en_US");
        emailTemplate.setTemplateType("liteuseremailconfirmation");
        emailTemplate.setTemplateDisplayName("liteuseremailconfirmation");


        Map<String, String> placeHolderData = new HashMap<>();
        placeHolderData.put(UTM_PARAMETER_PREFIX + "source", "UTM_SOURCE_SAMPLE");
        placeHolderData.put(UTM_PARAMETER_PREFIX + "medium", "UTM_MEDIUM_SAMPLE");
        placeHolderData.put(UTM_PARAMETER_PREFIX + "campaign", "UTM_CAMPAIGN_SAMPLE");
        placeHolderData.put("random-placeholder", "random-value");

        Map<String, String> userClaims = new HashMap<>();

        return new Object[][] {
                {emailTemplate, placeHolderData, userClaims, null}
        };
    }

    @Test(dataProvider = "GetBrandingPreferenceDataProvider")
    public void testGetBrandingPreference(JsonNode brandingPreferences, Map<String, String> brandingFallback, int caseNo) {

        String logoUrl = NotificationUtil.getBrandingPreference(
                ORGANIZATION_LOGO_URL_PLACEHOLDER, brandingPreferences, brandingFallback);
        String logoAltText = NotificationUtil.getBrandingPreference(
                ORGANIZATION_LOGO_ALT_TEXT_PLACEHOLDER, brandingPreferences, brandingFallback);
        String copyrightText = NotificationUtil.getBrandingPreference(
                ORGANIZATION_COPYRIGHT_TEXT_PLACEHOLDER, brandingPreferences, brandingFallback);
        String supportMail = NotificationUtil.getBrandingPreference(
                ORGANIZATION_SUPPORT_EMAIL_PLACEHOLDER, brandingPreferences, brandingFallback);
        String primaryColor = NotificationUtil.getBrandingPreference(
                ORGANIZATION_PRIMARY_COLOR_PLACEHOLDER, brandingPreferences, brandingFallback);
        String backgroundColor = NotificationUtil.getBrandingPreference(
                ORGANIZATION_BACKGROUND_COLOR_PLACEHOLDER, brandingPreferences, brandingFallback);
        String font = NotificationUtil.getBrandingPreference(
                ORGANIZATION_FONT_PLACEHOLDER, brandingPreferences, brandingFallback);
        String fontColor = NotificationUtil.getBrandingPreference(
                ORGANIZATION_FONT_COLOR_PLACEHOLDER, brandingPreferences, brandingFallback);
        String buttonFontColor = NotificationUtil.getBrandingPreference(
                ORGANIZATION_BUTTON_FONT_COLOR_PLACEHOLDER, brandingPreferences, brandingFallback);
        String themeBackgroundColor = NotificationUtil.getBrandingPreference(
                ORGANIZATION_THEME_BACKGROUND_COLOR_PLACEHOLDER, brandingPreferences, brandingFallback);
        String themeBorderColor = NotificationUtil.getBrandingPreference(
                ORGANIZATION_THEME_BORDER_COLOR_PLACEHOLDER, brandingPreferences, brandingFallback);

        if (caseNo == 1) {
            assertEquals(logoUrl, ORGANIZATION_LIGHT_LOGO_URL);
            assertEquals(logoAltText, ORGANIZATION_LIGHT_LOGO_ALT_TEXT);
            assertEquals(copyrightText, ORGANIZATION_COPYRIGHT_TEXT);
            assertEquals(supportMail, ORGANIZATION_SUPPORT_EMAIL);
            assertEquals(primaryColor, ORGANIZATION_LIGHT_PRIMARY_COLOR);
            assertEquals(backgroundColor, ORGANIZATION_LIGHT_BACKGROUND_COLOR);
            assertEquals(font, ORGANIZATION_LIGHT_FONT);
            assertEquals(fontColor, ORGANIZATION_LIGHT_FONT_COLOR);
            assertEquals(buttonFontColor, ORGANIZATION_LIGHT_BUTTON_FONT_COLOR);
            assertEquals(themeBackgroundColor, ORGANIZATION_LIGHT_BACKGROUND_COLOR_FALLBACK);
            assertEquals(themeBorderColor, ORGANIZATION_LIGHT_BORDER_COLOR_FALLBACK);
        }

        if (caseNo == 2) {
            assertEquals(logoUrl, ORGANIZATION_DARK_LOGO_URL);
            assertEquals(logoAltText, ORGANIZATION_DARK_LOGO_ALT_TEXT);
            assertEquals(primaryColor, ORGANIZATION_DARK_PRIMARY_COLOR);
            assertEquals(backgroundColor, ORGANIZATION_DARK_BACKGROUND_COLOR);
            assertEquals(font, ORGANIZATION_DARK_FONT);
            assertEquals(fontColor, ORGANIZATION_DARK_FONT_COLOR);
            assertEquals(buttonFontColor, ORGANIZATION_DARK_BUTTON_FONT_COLOR);
            assertEquals(themeBackgroundColor, ORGANIZATION_DARK_BACKGROUND_COLOR_FALLBACK);
            assertEquals(themeBorderColor, ORGANIZATION_DARK_BORDER_COLOR_FALLBACK);
        }

        if (caseNo == 3) {
            assertEquals(logoUrl, ORGANIZATION_LIGHT_LOGO_URL_FALLBACK);
            assertEquals(logoAltText, StringUtils.EMPTY);
            assertEquals(copyrightText, ORGANIZATION_COPYRIGHT_TEXT_FALLBACK);
            assertEquals(supportMail, ORGANIZATION_SUPPORT_EMAIL_FALLBACK);
            assertEquals(primaryColor, ORGANIZATION_PRIMARY_COLOR_FALLBACK);
            assertEquals(backgroundColor, ORGANIZATION_BACKGROUND_COLOR_FALLBACK);
            assertEquals(font, ORGANIZATION_FONT_FALLBACK);
            assertEquals(fontColor, ORGANIZATION_FONT_COLOR_FALLBACK);
            assertEquals(buttonFontColor, ORGANIZATION_BUTTON_FONT_COLOR_FALLBACK);
            assertEquals(themeBackgroundColor, ORGANIZATION_LIGHT_BACKGROUND_COLOR_FALLBACK);
            assertEquals(themeBorderColor, ORGANIZATION_LIGHT_BORDER_COLOR_FALLBACK);
        }

        if (caseNo == 4) {
            assertEquals(logoUrl, ORGANIZATION_LIGHT_LOGO_URL_FALLBACK);
            assertEquals(logoAltText, StringUtils.EMPTY);
            assertEquals(copyrightText, ORGANIZATION_COPYRIGHT_TEXT_FALLBACK);
            assertEquals(supportMail, ORGANIZATION_SUPPORT_EMAIL_FALLBACK);
            assertEquals(primaryColor, ORGANIZATION_PRIMARY_COLOR_FALLBACK);
            assertEquals(backgroundColor, ORGANIZATION_BACKGROUND_COLOR_FALLBACK);
            assertEquals(font, ORGANIZATION_FONT_FALLBACK);
            assertEquals(fontColor, ORGANIZATION_FONT_COLOR_FALLBACK);
            assertEquals(buttonFontColor, ORGANIZATION_BUTTON_FONT_COLOR_FALLBACK);
            assertEquals(themeBackgroundColor, ORGANIZATION_LIGHT_BACKGROUND_COLOR_FALLBACK);
            assertEquals(themeBorderColor, ORGANIZATION_LIGHT_BORDER_COLOR_FALLBACK);
        }
    }

    @Test
    public void testGetNotificationLocale() {

        String result = NotificationUtil.getNotificationLocale();
        assertEquals(result, I18nMgtConstants.DEFAULT_NOTIFICATION_LOCALE);
    }

    @DataProvider(name = "testEmailOTPLocaleDataProvider")
    public Object[][] testEmailOTPLocaleDataProvider() {

        return new Object[][] {
                { true },   // For provisioned users with Locale changed in myaccount.
                { false }   // For provisioned users without associated locale.
        };
    }

    @Test(dataProvider = "testEmailOTPLocaleDataProvider")
    public void testEmailOTPNotificationUsesCorrectLocaleForProvisionedUser(boolean containsAssociatedLocale)
            throws Exception {

        try (MockedStatic<NotificationHandlerDataHolder> notificationHandlerDataHolder =
                     mockStatic(NotificationHandlerDataHolder.class);
             MockedStatic<IdentityConfigParser> identityConfigParser = mockStatic(IdentityConfigParser.class);
             MockedStatic<ConfigurationFacade> configurationFacade = mockStatic(ConfigurationFacade.class);
             MockedStatic<IdentityCoreServiceComponent> identityCoreServiceComponent
                     = mockStatic(IdentityCoreServiceComponent.class);
             MockedStatic<CarbonUtils> carbonUtils = mockStatic(CarbonUtils.class)){

            mockNotificationHandlerDataHolder(notificationHandlerDataHolder);
            mockIdentityConfigParser(identityConfigParser);
            mockConfigurationFacade(configurationFacade);
            mockConfigurationContext(identityCoreServiceComponent);
            mockCarbonUtils(carbonUtils);

            Map<String, Object> eventProperties = new HashMap<>();
            eventProperties.put(NotificationConstants.IS_FEDERATED_USER, true);
            eventProperties.put(NotificationConstants.FEDERATED_USER_CLAIMS, new HashMap<>());

            if (containsAssociatedLocale) {
                eventProperties.put(NotificationConstants.EmailNotification.ARBITRARY_LOCALE, SAMPLE_LOCALE);
            }
            Map<String, String> placeHolderData = new HashMap<>();
            placeHolderData.put(NotificationConstants.EmailNotification.ARBITRARY_SEND_TO, SAMPLE_EMAIL);
            placeHolderData.put(ORGANIZATION_NAME_PLACEHOLDER, SAMPLE_ORGANIZATION_NAME);

            ArgumentCaptor<String> localeCaptor = ArgumentCaptor.forClass(String.class);
            Event event = new Event(IdentityEventConstants.Event.TRIGGER_NOTIFICATION, eventProperties);

            NotificationUtil.buildNotification(event, placeHolderData);

            Mockito.verify(mockEmailTemplateManager).isEmailTemplateExists(any(), localeCaptor.capture(), any(), any());
            String capturedLocale = localeCaptor.getValue();

            if (containsAssociatedLocale) {
                assertEquals(SAMPLE_LOCALE, capturedLocale);
            } else {
                assertEquals(I18nMgtConstants.DEFAULT_NOTIFICATION_LOCALE, capturedLocale);
            }
        }
    }

    @Test(dataProvider = "GetPlaceholderValuesDataProvider")
    public void testGetPlaceHolderValues(EmailTemplate emailTemplate, Map<String, String> placeHolderData,
                                         Map<String, String> userClaims, String applicationUuid)
            throws URLBuilderException {

        try (
                MockedStatic<IdentityConfigParser> staticMockedIdentityConfigParser =
                        Mockito.mockStatic(IdentityConfigParser.class);
                MockedStatic<IdentityUtil> staticMockedIdentityUtil = Mockito.mockStatic(IdentityUtil.class);
                MockedStatic<ConfigurationFacade> staticMockedConfigurationFacade =
                        Mockito.mockStatic(ConfigurationFacade.class);
                MockedStatic<ServiceURLBuilder> staticMockedServiceURLBuilder =
                        Mockito.mockStatic(ServiceURLBuilder.class);
        ) {
            IdentityConfigParser mockedIdentityConfigParser = mock(IdentityConfigParser.class);
            when(mockedIdentityConfigParser.getConfigElement(
                    NotificationConstants.EmailNotification.ORGANIZATION_LEVEL_EMAIL_BRANDING_FALLBACKS_ELEM))
                    .thenReturn(null);

            staticMockedIdentityConfigParser.when(IdentityConfigParser::getInstance)
                    .thenReturn(mockedIdentityConfigParser);
            staticMockedIdentityUtil.when(() -> IdentityUtil.getProperty(
                    NotificationConstants.EmailNotification.ENABLE_ORGANIZATION_LEVEL_EMAIL_BRANDING))
                    .thenReturn("false");

            staticMockedConfigurationFacade.when(ConfigurationFacade::getInstance)
                    .thenReturn(mock(ConfigurationFacade.class));

            ServiceURL serviceURL = mock(ServiceURL.class);
            when(serviceURL.getAbsolutePublicURL()).thenReturn("https://wso2test.com");
            ServiceURLBuilder mockedServiceURLBuilder = mock(ServiceURLBuilder.class);
            when(mockedServiceURLBuilder.build()).thenReturn(serviceURL);
            staticMockedServiceURLBuilder.when(ServiceURLBuilder::create).thenReturn(mockedServiceURLBuilder);

            NotificationUtil.getPlaceholderValues(emailTemplate, placeHolderData, userClaims, applicationUuid);
            Assert.assertNotNull(placeHolderData.get(UTM_PARAMETERS_PLACEHOLDER));
            Assert.assertEquals(placeHolderData.get(UTM_PARAMETERS_PLACEHOLDER),
                    "&" + UTM_PARAMETER_PREFIX + "campaign" + "=" + "UTM_CAMPAIGN_SAMPLE" +
                    "&" + UTM_PARAMETER_PREFIX + "medium" + "=" + "UTM_MEDIUM_SAMPLE" +
                    "&" + UTM_PARAMETER_PREFIX + "source" + "=" + "UTM_SOURCE_SAMPLE");
        }
    }

    private void mockNotificationHandlerDataHolder(MockedStatic<NotificationHandlerDataHolder>
                                                           notificationHandlerDataHolderMockedStatic)
            throws Exception {

        NotificationHandlerDataHolder mockNotificationHandlerDataHolder = mock(NotificationHandlerDataHolder.class);
        notificationHandlerDataHolderMockedStatic.when(NotificationHandlerDataHolder::getInstance)
                .thenReturn(mockNotificationHandlerDataHolder);
        when(mockNotificationHandlerDataHolder.getEmailTemplateManager()).thenReturn(mockEmailTemplateManager);
        mockEmailTemplate();
    }

    private void mockEmailTemplate() throws I18nEmailMgtException {

        when(mockEmailTemplateManager.isEmailTemplateExists(any(), any(), any(), any())).thenReturn(true);
        when(mockEmailTemplateManager.getEmailTemplate(any(), any(), any(), any())).thenReturn(mockEmailTemplate);
        when(mockEmailTemplate.getBody()).thenReturn(SAMPLE_EMAIL_BODY);
    }

    private void mockIdentityConfigParser(MockedStatic<IdentityConfigParser> identityConfigParser) {

        IdentityConfigParser mockIdentityConfigParser = mock(IdentityConfigParser.class);
        identityConfigParser.when(IdentityConfigParser::getInstance)
                .thenReturn(mockIdentityConfigParser);
    }

    private void mockConfigurationFacade(MockedStatic<ConfigurationFacade> configurationFacade) {

        ConfigurationFacade mockConfigurationFacade = mock(ConfigurationFacade.class);
        configurationFacade.when(ConfigurationFacade::getInstance).thenReturn(mockConfigurationFacade);
        when(mockConfigurationFacade.getAccountRecoveryEndpointAbsolutePath())
                .thenReturn(ACCOUNT_RECOVERY_ENDPOINT_URL);
        when(mockConfigurationFacade.getAuthenticationEndpointAbsoluteURL()).thenReturn(AUTHENTICATION_ENDPOINT_URL);
    }

    private void mockConfigurationContext(MockedStatic<IdentityCoreServiceComponent> identityCoreServiceComponent) {

        identityCoreServiceComponent.when(() -> IdentityCoreServiceComponent.getServiceURLBuilderFactory())
                .thenReturn(null);
        identityCoreServiceComponent.when(() -> IdentityCoreServiceComponent.getConfigurationContextService())
                .thenReturn(configurationContextService);
        when(configurationContextService.getServerConfigContext()).thenReturn(configurationContext);
        when(configurationContext.getAxisConfiguration()).thenReturn(axisConfiguration);
    }

    private void mockCarbonUtils(MockedStatic<CarbonUtils> carbonUtils) {

        carbonUtils.when(() -> CarbonUtils.getTransportProxyPort(axisConfiguration, null)).thenReturn(-1);
        carbonUtils.when(() -> CarbonUtils.getServerConfiguration()).thenReturn(serverConfiguration);
        carbonUtils.when(CarbonUtils::getManagementTransport).thenReturn(DUMMY_PROTOCOL);
    }
}
