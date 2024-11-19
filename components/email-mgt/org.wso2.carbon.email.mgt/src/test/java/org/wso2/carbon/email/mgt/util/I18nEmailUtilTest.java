/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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
package org.wso2.carbon.email.mgt.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtServerException;
import org.wso2.carbon.email.mgt.exceptions.I18nMgtEmailConfigException;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.TEMPLATE_CONTENT_TYPE;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.TEMPLATE_LOCALE;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.TEMPLATE_TYPE;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.TEMPLATE_TYPE_DISPLAY_NAME;

public class I18nEmailUtilTest {

    private static final String DISPLAY_NAME = "Display Name";
    private static final String TYPE = "templateType";
    private static final String CONTENT_TYPE = "text/html";
    private static final String TEMPLATE_CONTENT_TYPE_WITH_UTF_8 = "text/html; charset="
            + StandardCharsets.UTF_8.displayName();
    private static final String LOCALE = "en_US";
    private static final String CONTENT = "[\"Subject\",\"Body\",\"Footer\"]";
    private static final String CONTENT_INVALID = "{[\"Subject\",\"Body\",\"Footer\"]}";
    private static final String CONTENT_INCOMPLETE = "[]";

    private static final int CASE_1 = 1;
    private static final int CASE_2 = 2;
    private static final int CASE_3 = 3;
    private static final int CASE_4 = 4;
    private static final int CASE_5 = 5;

    @Mock
    private Resource templateResource;

    @Mock
    private Log log;

    @BeforeMethod
    public void setUp() {

        MockitoAnnotations.openMocks(this);
    }

    @DataProvider(name = "provideTestData")
    public Object[][] provideTestData() {

        Map<String, String> map1 = new HashMap<>();
        map1.put(TEMPLATE_TYPE_DISPLAY_NAME, DISPLAY_NAME);
        map1.put(TEMPLATE_TYPE, TYPE);
        map1.put(TEMPLATE_CONTENT_TYPE, CONTENT_TYPE);
        map1.put(TEMPLATE_LOCALE, LOCALE);

        return new Object[][]{
                {map1, CONTENT.getBytes(StandardCharsets.UTF_8), CASE_1},
                {map1, null, CASE_2},
                {map1, CONTENT_INVALID.getBytes(StandardCharsets.UTF_8), CASE_3},
                {map1, CONTENT_INCOMPLETE.getBytes(StandardCharsets.UTF_8), CASE_4},
                {map1, CONTENT.getBytes(StandardCharsets.UTF_8), CASE_5}
        };
    }

    @Test(dataProvider = "provideTestData")
    public void testGetEmailTemplate(Map<String, String> configMap, byte[] content, int caseNo) throws Exception {

        try (MockedStatic<LogFactory> logFactory = mockStatic(LogFactory.class)) {
            logFactory.when(() -> LogFactory.getLog(any(Class.class))).thenReturn(log);
            doNothing().when(log).debug(any());
            doNothing().when(log).error(any());
            doNothing().when(log).error(any(), any(Throwable.class));

            when(log.isDebugEnabled()).thenReturn(true);
            when(templateResource.getProperty(TEMPLATE_TYPE_DISPLAY_NAME)).thenReturn(configMap.get
                    (TEMPLATE_TYPE_DISPLAY_NAME));
            when(templateResource.getProperty(TEMPLATE_TYPE)).thenReturn(configMap.get(TEMPLATE_TYPE));
            when(templateResource.getProperty(TEMPLATE_CONTENT_TYPE)).thenReturn(configMap.get(TEMPLATE_CONTENT_TYPE));
            when(templateResource.getProperty(TEMPLATE_CONTENT_TYPE)).thenReturn(configMap.get(TEMPLATE_CONTENT_TYPE));
            when(templateResource.getProperty(TEMPLATE_LOCALE)).thenReturn(configMap.get(TEMPLATE_LOCALE));
            if (caseNo == CASE_5) {
                when(templateResource.getContent()).thenThrow(new RegistryException("Test registry exception."));
            } else {
                when(templateResource.getContent()).thenReturn(content);
            }

            if (caseNo == CASE_1) {
                EmailTemplate emailTemplate = I18nEmailUtil.getEmailTemplate(templateResource);

                assertEquals(emailTemplate.getTemplateDisplayName(), DISPLAY_NAME);
                assertEquals(emailTemplate.getTemplateType(), TYPE);
                assertEquals(emailTemplate.getEmailContentType(), TEMPLATE_CONTENT_TYPE_WITH_UTF_8);
                assertEquals(emailTemplate.getLocale(), LOCALE);
                assertEquals(emailTemplate.getSubject(), "Subject");
                assertEquals(emailTemplate.getBody(), "Body");
                assertEquals(emailTemplate.getFooter(), "Footer");

            } else if (caseNo == CASE_2) {
                EmailTemplate emailTemplate = I18nEmailUtil.getEmailTemplate(templateResource);

                Assert.assertNull(emailTemplate.getSubject(), "Subject should be null");
                Assert.assertNull(emailTemplate.getBody(), "Body should be null");
                Assert.assertNull(emailTemplate.getFooter(), "Footer should be null");

            } else if (caseNo == CASE_3) {
                try {
                    I18nEmailUtil.getEmailTemplate(templateResource);
                    Assert.fail(
                            "Test should throw an exception of type: " + I18nEmailMgtServerException.class.getName());
                } catch (I18nEmailMgtServerException e) {
                    Assert.assertTrue(e.getMessage().contains("Error deserializing"));
                }

            } else if (caseNo == CASE_4) {
                try {
                    I18nEmailUtil.getEmailTemplate(templateResource);
                    Assert.fail(
                            "Test should throw an exception of type: " + I18nEmailMgtServerException.class.getName());
                } catch (I18nMgtEmailConfigException e) {
                    Assert.assertTrue(e.getMessage().contains("Template"));
                }
            } else if (caseNo == CASE_5) {
                try {
                    I18nEmailUtil.getEmailTemplate(templateResource);
                    Assert.fail(
                            "Test should throw an exception of type: " + I18nEmailMgtServerException.class.getName());
                } catch (I18nEmailMgtServerException e) {
                    Assert.assertTrue(e.getMessage().contains("Error retrieving a template"));
                }
            }
        }
    }

    @DataProvider(name = "provideLocaleData")
    public Object[][] provideLocaleData() {

        return new Object[][]{
                {"en_US", "en_US"},
                {"en*US", I18nMgtConstants.DEFAULT_NOTIFICATION_LOCALE},
                {"", I18nMgtConstants.DEFAULT_NOTIFICATION_LOCALE}
        };
    }

    @Test(dataProvider = "provideLocaleData")
    public void testNormalizeLocaleFormat(String locale, String expectedLocale) {

        String result = I18nEmailUtil.normalizeLocaleFormat(locale);
        assertEquals(result, expectedLocale);
    }

    @Test
    public void testGetNotificationLocale() {

        try (MockedStatic<IdentityUtil> identityUtil = mockStatic(IdentityUtil.class)) {
            String customDefaultLocale = "fr-FR";
            identityUtil.when(() -> IdentityUtil.getProperty(I18nMgtConstants.NOTIFICATION_DEFAULT_LOCALE))
                    .thenReturn(customDefaultLocale);
            String result = I18nEmailUtil.getNotificationLocale();
            assertEquals(result, customDefaultLocale);

            identityUtil.when(() -> IdentityUtil.getProperty(I18nMgtConstants.NOTIFICATION_DEFAULT_LOCALE))
                    .thenReturn(null);
            String result2 = I18nEmailUtil.getNotificationLocale();
            assertEquals(result2, I18nMgtConstants.DEFAULT_NOTIFICATION_LOCALE);
        }
    }
}
