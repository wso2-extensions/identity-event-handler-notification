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
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.email.mgt.internal.I18nMgtServiceComponent;
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Class that contains the test cases for {@link UnifiedTemplateManager}.
 */
@WithCarbonHome
public class UnifiedTemplateManagerTest {

    private static final String tenantDomain = "carbon.super";

    @Mock
    TemplatePersistenceManager templatePersistenceManager;

    UnifiedTemplateManager unifiedTemplateManager;
    List<NotificationTemplate> defaultSystemTemplates;
    NotificationTemplate positiveNotificationTemplate;
    NotificationTemplate negativeNotificationTemplate;


    @BeforeMethod
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        defaultSystemTemplates = new I18nMgtServiceComponent().loadDefaultTemplatesFromFile(
                NotificationChannels.EMAIL_CHANNEL.getChannelType());
        I18nMgtDataHolder.getInstance().setDefaultEmailTemplates(defaultSystemTemplates);

        unifiedTemplateManager = new UnifiedTemplateManager(templatePersistenceManager);
        initTestNotificationTemplates();
    }

    @Test
    public void testIsNotificationTemplateExists() throws Exception {

        NotificationTemplate notificationTemplate = defaultSystemTemplates.get(0);
        assertTrue(unifiedTemplateManager.isNotificationTemplateExists(
                notificationTemplate.getDisplayName(),
                notificationTemplate.getLocale(),
                notificationTemplate.getNotificationChannel(),
                null,
                tenantDomain));
        verify(templatePersistenceManager, never()).isNotificationTemplateExists(
                notificationTemplate.getDisplayName(),
                notificationTemplate.getLocale(),
                notificationTemplate.getNotificationChannel(),
                null,
                tenantDomain);

        notificationTemplate = positiveNotificationTemplate;
        when(templatePersistenceManager.isNotificationTemplateExists(
                notificationTemplate.getDisplayName(),
                notificationTemplate.getLocale(),
                notificationTemplate.getNotificationChannel(),
                null,
                tenantDomain)).thenReturn(true);
        assertTrue(unifiedTemplateManager.isNotificationTemplateExists(
                notificationTemplate.getDisplayName(),
                notificationTemplate.getLocale(),
                notificationTemplate.getNotificationChannel(),
                null,
                tenantDomain));

        when(templatePersistenceManager.isNotificationTemplateExists(
                notificationTemplate.getDisplayName(),
                notificationTemplate.getLocale(),
                notificationTemplate.getNotificationChannel(),
                null,
                tenantDomain)).thenReturn(false);
        assertFalse(unifiedTemplateManager.isNotificationTemplateExists(
                notificationTemplate.getDisplayName(),
                notificationTemplate.getLocale(),
                notificationTemplate.getNotificationChannel(),
                null,
                tenantDomain));
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
