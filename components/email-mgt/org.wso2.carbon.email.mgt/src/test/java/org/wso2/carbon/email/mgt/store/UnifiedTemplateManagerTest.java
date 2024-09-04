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
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.utils.CarbonUtils;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NOTIFICATION_TEMPLATES_STORAGE_CONFIG;

/**
 * Class that contains the test cases for {@link UnifiedTemplateManager}.
 */
@WithCarbonHome
@PrepareForTest({I18nMgtDataHolder.class, CarbonUtils.class, IdentityUtil.class})
public class UnifiedTemplateManagerTest extends PowerMockTestCase {

    private static final String tenantDomain = "carbon.super";

    @Mock
    I18nMgtDataHolder i18nMgtDataHolder;
    @Mock
    TemplatePersistenceManagerFactory templatePersistenceManagerFactory;
    @Mock
    TemplatePersistenceManager templatePersistenceManager;

    UnifiedTemplateManager unifiedTemplateManager;
    List<NotificationTemplate> defaultSystemTemplates;
    NotificationTemplate positiveNotificationTemplate;
    NotificationTemplate negativeNotificationTemplate;


    @BeforeMethod
    public void setUp() {

        initTestNotificationTemplates();

        initMocks(this);
        mockStatic(I18nMgtDataHolder.class);
        i18nMgtDataHolder = PowerMockito.mock(I18nMgtDataHolder.class);
        when(I18nMgtDataHolder.getInstance()).thenReturn(i18nMgtDataHolder);
        when(i18nMgtDataHolder.getDefaultEmailTemplates()).thenReturn(defaultSystemTemplates);

        mockStatic(IdentityUtil.class);
        when(IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG)).thenReturn("registry");

        templatePersistenceManager = PowerMockito.mock(TemplatePersistenceManager.class);
        unifiedTemplateManager = new UnifiedTemplateManager(templatePersistenceManager);
        templatePersistenceManagerFactory = PowerMockito.mock(TemplatePersistenceManagerFactory.class);
        when(templatePersistenceManagerFactory.getTemplatePersistenceManager()).thenReturn(unifiedTemplateManager);
    }

    @Test
    public void testListAllNotificationTemplates() throws Exception {

        assertFalse(unifiedTemplateManager.listAllNotificationTemplates(
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain).isEmpty());
        assertTrue(unifiedTemplateManager.listAllNotificationTemplates(
                NotificationChannels.SMS_CHANNEL.getChannelType(), tenantDomain).isEmpty());
    }

    @Test
    public void testListNotificationTemplates() throws Exception {

        List<NotificationTemplate> templateListForDummyScenario =
                unifiedTemplateManager.listNotificationTemplates(positiveNotificationTemplate.getType(),
                        NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain);
        assertTrue(templateListForDummyScenario.isEmpty());

        List<NotificationTemplate> templateListForNegativeScenario = unifiedTemplateManager.listNotificationTemplates(
                negativeNotificationTemplate.getType(),
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain);
        assertTrue(templateListForNegativeScenario.isEmpty());

        List<NotificationTemplate> templateListForValidScenario = unifiedTemplateManager.listNotificationTemplates(
                defaultSystemTemplates.get(0).getType(),
                NotificationChannels.EMAIL_CHANNEL.getChannelType(), StringUtils.EMPTY, tenantDomain);
        assertFalse(templateListForValidScenario.isEmpty());
    }

    @Test
    public void testAddOrUpdateNotificationTemplateWhenAddingDefaultTemplate() throws Exception {

        NotificationTemplate notificationTemplate = defaultSystemTemplates.get(0);

        // Mock the behavior to simulate add template -> the template not exists in the persistent storage
        when(templatePersistenceManager.isNotificationTemplateExists(
                notificationTemplate.getDisplayName(),
                notificationTemplate.getLocale(),
                notificationTemplate.getNotificationChannel(),
                null,
                tenantDomain)).thenReturn(false);
        unifiedTemplateManager.addOrUpdateNotificationTemplate(notificationTemplate, null, tenantDomain);

        // Assert logic
        verify(templatePersistenceManager, never()).addOrUpdateNotificationTemplate(notificationTemplate, null,
                tenantDomain);
        verify(templatePersistenceManager, never()).deleteNotificationTemplate(
                notificationTemplate.getDisplayName(),
                notificationTemplate.getLocale(),
                notificationTemplate.getNotificationChannel(),
                null,
                tenantDomain);
    }

    @Test
    public void testAddOrUpdateNotificationTemplateWhenUpdatingToDefaultTemplate() throws Exception {

        NotificationTemplate notificationTemplate = defaultSystemTemplates.get(0);

        // Mock the behavior to simulate update template -> the template exists in the persistent storage
        when(templatePersistenceManager.isNotificationTemplateExists(
                notificationTemplate.getDisplayName(),
                notificationTemplate.getLocale(),
                notificationTemplate.getNotificationChannel(),
                null,
                tenantDomain)).thenReturn(true);
        unifiedTemplateManager.addOrUpdateNotificationTemplate(notificationTemplate, null, tenantDomain);

        // Assert logic
        verify(templatePersistenceManager, never()).addOrUpdateNotificationTemplate(notificationTemplate, null,
                tenantDomain);
        verify(templatePersistenceManager).deleteNotificationTemplate(
                notificationTemplate.getDisplayName(),
                notificationTemplate.getLocale(),
                notificationTemplate.getNotificationChannel(),
                null,
                tenantDomain);
    }

    @Test
    public void testAddOrUpdateNotificationTemplateWhenAddingCustomTemplate() throws Exception {

        NotificationTemplate notificationTemplate = positiveNotificationTemplate;
        unifiedTemplateManager.addOrUpdateNotificationTemplate(notificationTemplate, null, tenantDomain);

        // Assert logic
        verify(templatePersistenceManager).addOrUpdateNotificationTemplate(notificationTemplate, null, tenantDomain);
        verify(templatePersistenceManager, never()).isNotificationTemplateExists(
                notificationTemplate.getDisplayName(),
                notificationTemplate.getLocale(),
                notificationTemplate.getNotificationChannel(),
                null,
                tenantDomain);
        verify(templatePersistenceManager, never()).deleteNotificationTemplate(
                notificationTemplate.getDisplayName(),
                notificationTemplate.getLocale(),
                notificationTemplate.getNotificationChannel(),
                null,
                tenantDomain);
    }

    @Test
    public void testAddOrUpdateNotificationTemplateWhenUpdatingCustomTemplate() throws Exception {

        NotificationTemplate notificationTemplate = positiveNotificationTemplate;
        unifiedTemplateManager.addOrUpdateNotificationTemplate(notificationTemplate, null, tenantDomain);

        // Assert logic
        verify(templatePersistenceManager).addOrUpdateNotificationTemplate(notificationTemplate, null, tenantDomain);
        verify(templatePersistenceManager, never()).isNotificationTemplateExists(
                notificationTemplate.getDisplayName(),
                notificationTemplate.getLocale(),
                notificationTemplate.getNotificationChannel(),
                null,
                tenantDomain);
        verify(templatePersistenceManager, never()).deleteNotificationTemplate(
                notificationTemplate.getDisplayName(),
                notificationTemplate.getLocale(),
                notificationTemplate.getNotificationChannel(),
                null,
                tenantDomain);
    }

    @Test
    public void testAddOrUpdateNotificationTemplateWhenNullTemplate() throws Exception {

        unifiedTemplateManager.addOrUpdateNotificationTemplate(null, null, tenantDomain);

        // Assert logic
        verify(templatePersistenceManager).addOrUpdateNotificationTemplate(null, null, tenantDomain);
    }

    private void initTestNotificationTemplates() {

        defaultSystemTemplates = new ArrayList<>();
        NotificationTemplate defaultNotificationTemplate = new NotificationTemplate();
        defaultNotificationTemplate.setNotificationChannel(NotificationChannels.EMAIL_CHANNEL.getChannelType());
        defaultNotificationTemplate.setType("passwordReset");
        defaultNotificationTemplate.setDisplayName("PasswordReset");
        defaultNotificationTemplate.setLocale("en_US");
        defaultNotificationTemplate.setBody("passwordReset_Body");
        defaultNotificationTemplate.setSubject("passwordReset_Subject");
        defaultNotificationTemplate.setFooter("passwordReset_Footer");
        defaultNotificationTemplate.setContentType("text/html");
        defaultSystemTemplates.add(defaultNotificationTemplate);

        positiveNotificationTemplate = new NotificationTemplate();
        positiveNotificationTemplate.setNotificationChannel(NotificationChannels.EMAIL_CHANNEL.getChannelType());
        positiveNotificationTemplate.setType("dummyType");
        positiveNotificationTemplate.setDisplayName("dummyDisplayName");
        positiveNotificationTemplate.setLocale("en_US");
        positiveNotificationTemplate.setBody("dummyBody");
        positiveNotificationTemplate.setSubject("dummySubject");
        positiveNotificationTemplate.setFooter("dummyFooter");

        negativeNotificationTemplate = new NotificationTemplate();
        negativeNotificationTemplate.setNotificationChannel(NotificationChannels.EMAIL_CHANNEL.getChannelType());
        negativeNotificationTemplate.setType("dummyType");
        negativeNotificationTemplate.setDisplayName("dummyDisplayName");
        negativeNotificationTemplate.setLocale("en_US");
    }
}
