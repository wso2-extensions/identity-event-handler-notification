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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.email.mgt.internal.I18nMgtServiceComponent;
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Class that contains the test cases for {@link SystemDefaultTemplateManager}.
 */
@WithCarbonHome
public class SystemDefaultTemplateManagerTest {

    private static final String tenantDomain = "carbon.super";
    private static final String dummyDisplayName = "dummyDisplayName";
    private static final String dummyAppId = "dummyAppId";
    private static final String EN_US = "en_US";

    SystemDefaultTemplateManager systemDefaultTemplateManager;
    NotificationTemplate positiveNotificationTemplate;
    NotificationTemplate negativeNotificationTemplate;

    @BeforeMethod
    public void setUp() {

        List<NotificationTemplate> defaultEmailTemplate = new I18nMgtServiceComponent().loadDefaultTemplatesFromFile(
                NotificationChannels.EMAIL_CHANNEL.getChannelType());
        I18nMgtDataHolder.getInstance().setDefaultEmailTemplates(defaultEmailTemplate);

        systemDefaultTemplateManager = new SystemDefaultTemplateManager();
        initTestNotificationTemplates();
    }

    private void initTestNotificationTemplates() {

        positiveNotificationTemplate = I18nMgtDataHolder.getInstance().getDefaultEmailTemplates().get(0);
        negativeNotificationTemplate = new NotificationTemplate();
        negativeNotificationTemplate.setNotificationChannel(NotificationChannels.EMAIL_CHANNEL.getChannelType());
        negativeNotificationTemplate.setType("dummyType");
        negativeNotificationTemplate.setDisplayName("dummyDisplayName");
        negativeNotificationTemplate.setLocale("en_US");
    }

    @Test
    public void testIsNotificationTemplateTypeExists() throws Exception {

        assertFalse(systemDefaultTemplateManager.isNotificationTemplateTypeExists(StringUtils.EMPTY,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain));
        assertFalse(systemDefaultTemplateManager.isNotificationTemplateTypeExists(
                negativeNotificationTemplate.getDisplayName(),
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain));
        assertTrue(systemDefaultTemplateManager.isNotificationTemplateTypeExists(
                positiveNotificationTemplate.getDisplayName(),
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain));
    }

    @Test
    public void testListNotificationTemplateTypes() throws Exception {

        List<String> displayNames = systemDefaultTemplateManager.listNotificationTemplateTypes(
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain);
        Set<String> displayNamesSet = new HashSet<>(displayNames);
        assertNotNull(displayNames);
        assertTrue(displayNames.contains(positiveNotificationTemplate.getDisplayName()));
        assertFalse(displayNames.contains(negativeNotificationTemplate.getDisplayName()));
        assertEquals(displayNamesSet.size(), displayNames.size());
    }

    @Test
    public void testIsNotificationTemplateExists() throws Exception {

        assertFalse(systemDefaultTemplateManager.isNotificationTemplateExists(StringUtils.EMPTY, EN_US,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), dummyAppId, tenantDomain));
        assertFalse(systemDefaultTemplateManager.isNotificationTemplateExists(dummyDisplayName, StringUtils.EMPTY,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), dummyAppId, tenantDomain));
        assertFalse(systemDefaultTemplateManager.isNotificationTemplateExists(dummyDisplayName, EN_US,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain));
        assertFalse(systemDefaultTemplateManager.isNotificationTemplateExists(
                negativeNotificationTemplate.getDisplayName(), EN_US,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain));
        assertTrue(systemDefaultTemplateManager.isNotificationTemplateExists(
                positiveNotificationTemplate.getDisplayName(), EN_US,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain));
    }

    @Test
    public void testGetNotificationTemplate() throws Exception {

        assertNull(systemDefaultTemplateManager.getNotificationTemplate(StringUtils.EMPTY, EN_US,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), dummyAppId, tenantDomain));
        assertNull(systemDefaultTemplateManager.getNotificationTemplate(dummyDisplayName, StringUtils.EMPTY,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), dummyAppId, tenantDomain));
        assertNull(systemDefaultTemplateManager.getNotificationTemplate(dummyDisplayName, EN_US,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain));
        assertNull(systemDefaultTemplateManager.getNotificationTemplate(
                negativeNotificationTemplate.getDisplayName(), EN_US,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain));
        assertNotNull(systemDefaultTemplateManager.getNotificationTemplate(
                positiveNotificationTemplate.getDisplayName(), EN_US,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain));
    }

    @Test
    public void testListNotificationTemplates() throws Exception {

        assertTrue(systemDefaultTemplateManager.listNotificationTemplates(StringUtils.EMPTY,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), dummyAppId, tenantDomain).isEmpty());
        assertTrue(systemDefaultTemplateManager.listNotificationTemplates(dummyDisplayName,
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain).isEmpty());
        assertTrue(systemDefaultTemplateManager.listNotificationTemplates(
                negativeNotificationTemplate.getDisplayName(),
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain).isEmpty());
        assertFalse(systemDefaultTemplateManager.listNotificationTemplates(
                positiveNotificationTemplate.getDisplayName(),
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain).isEmpty());
    }

    @Test
    public void testListAllNotificationTemplates() throws Exception {

        assertFalse(systemDefaultTemplateManager.listAllNotificationTemplates(
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain).isEmpty());
        assertTrue(systemDefaultTemplateManager.listAllNotificationTemplates(
                NotificationChannels.SMS_CHANNEL.getChannelType(), tenantDomain).isEmpty());
    }
}
