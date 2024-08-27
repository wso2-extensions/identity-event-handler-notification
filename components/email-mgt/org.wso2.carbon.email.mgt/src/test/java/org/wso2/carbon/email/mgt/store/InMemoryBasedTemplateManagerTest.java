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

package org.wso2.carbon.email.mgt.store;

import org.apache.commons.lang.StringUtils;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.email.mgt.internal.I18nMgtServiceComponent;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.utils.CarbonUtils;

import java.nio.file.Paths;
import java.util.List;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Class that contains the test cases for {@link InMemoryBasedTemplateManager}.
 */
@PrepareForTest({I18nMgtDataHolder.class, CarbonUtils.class})
public class InMemoryBasedTemplateManagerTest extends PowerMockTestCase {

    private final String baseDirectoryPath = Paths.get(System.getProperty("user.dir"),
            "src", "test", "resources").toString();
    private static final String tenantDomain = "carbon.super";
    private static final String dummyDisplayName = "dummyDisplayName";
    private static final String dummyAppId = "dummyAppId";
    private static final String EN_US = "en_US";
    @Mock
    I18nMgtDataHolder i18nMgtDataHolder;
    InMemoryBasedTemplateManager inMemoryBasedTemplateManager;
    NotificationTemplate positiveNotificationTemplate;
    NotificationTemplate negativeNotificationTemplate;

    @BeforeMethod
    public void setUp() {

        initMocks(this);
        mockStatic(I18nMgtDataHolder.class);
        i18nMgtDataHolder = PowerMockito.mock(I18nMgtDataHolder.class);
        when(I18nMgtDataHolder.getInstance()).thenReturn(i18nMgtDataHolder);

        List<NotificationTemplate> defaultEmailTemplate = loadDefaultTemplatesFromFile(baseDirectoryPath,
                NotificationChannels.EMAIL_CHANNEL.getChannelType());
        when(i18nMgtDataHolder.getDefaultEmailTemplates()).thenReturn(defaultEmailTemplate);

        inMemoryBasedTemplateManager = new InMemoryBasedTemplateManager();
        initTestNotificationTemplates();
    }

    private void initTestNotificationTemplates() {

        positiveNotificationTemplate = i18nMgtDataHolder.getDefaultEmailTemplates().get(0);
        negativeNotificationTemplate = new NotificationTemplate();
        negativeNotificationTemplate.setNotificationChannel(NotificationChannels.EMAIL_CHANNEL.getChannelType());
        negativeNotificationTemplate.setType("dummyType");
        negativeNotificationTemplate.setDisplayName("dummyDisplayName");
        negativeNotificationTemplate.setLocale("en_US");
    }

    @Test
    public void testIsNotificationTemplateTypeExists() throws Exception {

        assertFalse(inMemoryBasedTemplateManager.isNotificationTemplateTypeExists(StringUtils.EMPTY,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain));
        assertFalse(inMemoryBasedTemplateManager.isNotificationTemplateTypeExists(
                negativeNotificationTemplate.getDisplayName(),
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain));
        assertTrue(inMemoryBasedTemplateManager.isNotificationTemplateTypeExists(
                positiveNotificationTemplate.getDisplayName(),
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain));
    }

    @Test
    public void testListNotificationTemplateTypes() throws Exception {

        List<String> displayNames = inMemoryBasedTemplateManager.listNotificationTemplateTypes(
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain);
        assertNotNull(displayNames);
        assertTrue(displayNames.contains(positiveNotificationTemplate.getDisplayName()));
        assertFalse(displayNames.contains(negativeNotificationTemplate.getDisplayName()));
    }

    @Test
    public void testIsNotificationTemplateExists() throws Exception {

        assertFalse(inMemoryBasedTemplateManager.isNotificationTemplateExists(StringUtils.EMPTY, EN_US,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), dummyAppId, tenantDomain));
        assertFalse(inMemoryBasedTemplateManager.isNotificationTemplateExists(dummyDisplayName, StringUtils.EMPTY,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), dummyAppId, tenantDomain));
        assertFalse(inMemoryBasedTemplateManager.isNotificationTemplateExists(dummyDisplayName, EN_US,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain));
        assertFalse(inMemoryBasedTemplateManager.isNotificationTemplateExists(
                negativeNotificationTemplate.getDisplayName(), EN_US,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain));
        assertTrue(inMemoryBasedTemplateManager.isNotificationTemplateExists(
                positiveNotificationTemplate.getDisplayName(), EN_US,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain));
    }

    @Test
    public void testGetNotificationTemplate() throws Exception {

        assertNull(inMemoryBasedTemplateManager.getNotificationTemplate(StringUtils.EMPTY, EN_US,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), dummyAppId, tenantDomain));
        assertNull(inMemoryBasedTemplateManager.getNotificationTemplate(dummyDisplayName, StringUtils.EMPTY,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), dummyAppId, tenantDomain));
        assertNull(inMemoryBasedTemplateManager.getNotificationTemplate(dummyDisplayName, EN_US,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain));
        assertNull(inMemoryBasedTemplateManager.getNotificationTemplate(
                negativeNotificationTemplate.getDisplayName(), EN_US,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain));
        assertNotNull(inMemoryBasedTemplateManager.getNotificationTemplate(
                positiveNotificationTemplate.getDisplayName(), EN_US,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain));
    }

    @Test
    public void testListNotificationTemplates() throws Exception {

        assertTrue(inMemoryBasedTemplateManager.listNotificationTemplates(StringUtils.EMPTY,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), dummyAppId, tenantDomain).isEmpty());
        assertTrue(inMemoryBasedTemplateManager.listNotificationTemplates(dummyDisplayName,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain).isEmpty());
        assertTrue(inMemoryBasedTemplateManager.listNotificationTemplates(
                negativeNotificationTemplate.getDisplayName(),
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain).isEmpty());
        assertFalse(inMemoryBasedTemplateManager.listNotificationTemplates(
                positiveNotificationTemplate.getDisplayName(),
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain).isEmpty());
    }

    @Test
    public void testListAllNotificationTemplates() throws Exception {

        assertFalse(inMemoryBasedTemplateManager.listAllNotificationTemplates(
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain).isEmpty());
        assertTrue(inMemoryBasedTemplateManager.listAllNotificationTemplates(
                NotificationChannels.SMS_CHANNEL.getChannelType(), tenantDomain).isEmpty());
    }

    /**
     * Loads the default templates from the file for the channel(EMAIL or SMS) and create list of Notification Template.
     *
     * @param notificationChannel   Channel of the notification.
     * @return                      List of NotificationTemplate.
     */
    private List<NotificationTemplate> loadDefaultTemplatesFromFile(String baseDirectoryPath,
                                                                    String notificationChannel) {

        mockNotificationChannelConfigPath(baseDirectoryPath);
        I18nMgtServiceComponent component = new I18nMgtServiceComponent();
        return component.loadDefaultTemplatesFromFile(notificationChannel);
    }

    /**
     * Mock the default config xml path of notification templates.
     *
     * @param baseDirectoryPath Resource folder location
     */
    private void mockNotificationChannelConfigPath(String baseDirectoryPath) {

        mockStatic(CarbonUtils.class);
        when(CarbonUtils.getCarbonConfigDirPath()).thenReturn(baseDirectoryPath);
    }
}
