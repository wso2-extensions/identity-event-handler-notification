/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.email.mgt.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtServerException;
import org.wso2.carbon.email.mgt.exceptions.I18nMgtEmailConfigException;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
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
    private static final String CONTENT= "[\"Subject\",\"Body\",\"Footer\"]";
    private static final String CONTENT_INVALID= "{[\"Subject\",\"Body\",\"Footer\"]}";
    private static final String CONTENT_INCOMPLETE= "[]";

    private static final int CASE_1 = 1;
    private static final int CASE_2 = 2;
    private static final int CASE_3 = 3;
    private static final int CASE_4 = 4;
    private static final int CASE_5 = 5;

    @Mock
    private Resource templateResource;

    @Mock
    private Log log;

    private MockedStatic<LogFactory> mockedLogFactory;
    private AutoCloseable closeable;

    @BeforeMethod
    public void setUp() {

        closeable = openMocks(this);
    }

    @AfterMethod
    public void tearDown() throws Exception {

        if (closeable != null) {
            closeable.close();
        }
        if (mockedLogFactory != null) {
            mockedLogFactory.close();
        }
    }

    @DataProvider(name = "provideTestData")
    public Object[][] provideTestData() {

        Map<String, String> map1 = new HashMap<>();
        map1.put(TEMPLATE_TYPE_DISPLAY_NAME, DISPLAY_NAME);
        map1.put(TEMPLATE_TYPE, TYPE);
        map1.put(TEMPLATE_CONTENT_TYPE, CONTENT_TYPE);
        map1.put(TEMPLATE_LOCALE, LOCALE);

        return new Object[][] {
                { map1, CONTENT.getBytes(StandardCharsets.UTF_8), CASE_1},
                { map1, null, CASE_2},
                { map1, CONTENT_INVALID.getBytes(StandardCharsets.UTF_8), CASE_3},
                { map1, CONTENT_INCOMPLETE.getBytes(StandardCharsets.UTF_8), CASE_4},
                { map1, CONTENT.getBytes(StandardCharsets.UTF_8), CASE_5}
        };
    }

    @Test(dataProvider = "provideTestData")
    public void testGetEmailTemplate(Map<String, String> configMap, byte[] content, int caseNo) throws Exception {

        mockedLogFactory = mockStatic(LogFactory.class);
        mockedLogFactory.when(() -> LogFactory.getLog(any(Class.class))).thenReturn(log);
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

            Assert.assertEquals(emailTemplate.getTemplateDisplayName(), DISPLAY_NAME);
            Assert.assertEquals(emailTemplate.getTemplateType(), TYPE);
            Assert.assertEquals(emailTemplate.getEmailContentType(), TEMPLATE_CONTENT_TYPE_WITH_UTF_8);
            Assert.assertEquals(emailTemplate.getLocale(), LOCALE);
            Assert.assertEquals(emailTemplate.getSubject(), "Subject");
            Assert.assertEquals(emailTemplate.getBody(), "Body");
            Assert.assertEquals(emailTemplate.getFooter(), "Footer");

        } else if (caseNo == CASE_2) {
            EmailTemplate emailTemplate = I18nEmailUtil.getEmailTemplate(templateResource);

            Assert.assertNull(emailTemplate.getSubject(), "Subject should be null");
            Assert.assertNull(emailTemplate.getBody(), "Body should be null");
            Assert.assertNull(emailTemplate.getFooter(), "Footer should be null");

        } else if (caseNo == CASE_3) {
            try {
                I18nEmailUtil.getEmailTemplate(templateResource);
                Assert.fail("Test should throw an exception of type: " + I18nEmailMgtServerException.class.getName());
            } catch (I18nEmailMgtServerException e) {
                Assert.assertTrue(e.getMessage().contains("Error deserializing"));
            }

        } else if (caseNo == CASE_4) {
            try {
                I18nEmailUtil.getEmailTemplate(templateResource);
                Assert.fail("Test should throw an exception of type: " + I18nEmailMgtServerException.class.getName());
            } catch (I18nMgtEmailConfigException e) {
                Assert.assertTrue(e.getMessage().contains("Template"));
            }
        } else if (caseNo == CASE_5) {
            try {
                I18nEmailUtil.getEmailTemplate(templateResource);
                Assert.fail("Test should throw an exception of type: " + I18nEmailMgtServerException.class.getName());
            } catch (I18nEmailMgtServerException e) {
                Assert.assertTrue(e.getMessage().contains("Error retrieving a template"));
            }
        }

    }
}