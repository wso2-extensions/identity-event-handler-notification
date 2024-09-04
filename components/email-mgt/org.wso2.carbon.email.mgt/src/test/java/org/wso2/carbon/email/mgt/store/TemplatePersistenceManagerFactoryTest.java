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

import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import java.lang.reflect.Field;
import java.nio.file.Paths;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NOTIFICATION_TEMPLATES_STORAGE_CONFIG;

/**
 * Class that contains the test cases for {@link TemplatePersistenceManagerFactory}.
 */
@WithCarbonHome
@PrepareForTest({I18nMgtDataHolder.class, IdentityUtil.class})
public class TemplatePersistenceManagerFactoryTest extends PowerMockTestCase {

    @Mock
    RegistryResourceMgtService resourceMgtService;
    @Mock
    I18nMgtDataHolder i18nMgtDataHolder;
    private TemplatePersistenceManagerFactory templatePersistenceManagerFactory;

    @BeforeMethod
    public void setUp() {

        initMocks(this);
        mockStatic(I18nMgtDataHolder.class);
        i18nMgtDataHolder = PowerMockito.mock(I18nMgtDataHolder.class);
        when(I18nMgtDataHolder.getInstance()).thenReturn(i18nMgtDataHolder);
        when(i18nMgtDataHolder.getRegistryResourceMgtService()).thenReturn(resourceMgtService);

        mockStatic(IdentityUtil.class);
        templatePersistenceManagerFactory = new TemplatePersistenceManagerFactory();
    }

    @Test
    public void shouldUseDBBasedTemplateManagerWhenConfigIsDatabase() {

        when(IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG)).thenReturn("database");
        TemplatePersistenceManager templatePersistenceManager =
                templatePersistenceManagerFactory.getTemplatePersistenceManager();
        assertTrue(templatePersistenceManager instanceof UnifiedTemplateManager);
        assertUnderlyingManagerType(templatePersistenceManager, DBBasedTemplateManager.class);
    }

    @Test
    public void shouldUseHybridTemplateManagerWhenConfigIsOnMigration() {

        when(IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG)).thenReturn("hybrid");
        TemplatePersistenceManager templatePersistenceManager =
                templatePersistenceManagerFactory.getTemplatePersistenceManager();
        assertTrue(templatePersistenceManager instanceof UnifiedTemplateManager);
        assertUnderlyingManagerType(templatePersistenceManager, HybridTemplateManager.class);
    }

    @Test
    public void shouldUseRegistryBasedTemplateManagerWhenConfigIsRegistry() {

        when(IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG)).thenReturn("registry");
        TemplatePersistenceManager templatePersistenceManager =
                templatePersistenceManagerFactory.getTemplatePersistenceManager();
        assertTrue(templatePersistenceManager instanceof UnifiedTemplateManager);
        assertUnderlyingManagerType(templatePersistenceManager, RegistryBasedTemplateManager.class);
    }

    @Test
    public void shouldUseDBBasedTemplateManagerWhenConfigIsInvalid() {

        when(IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG)).thenReturn("invalid");
        TemplatePersistenceManager templatePersistenceManager =
                templatePersistenceManagerFactory.getTemplatePersistenceManager();
        assertTrue(templatePersistenceManager instanceof UnifiedTemplateManager);
        assertUnderlyingManagerType(templatePersistenceManager, DBBasedTemplateManager.class);
    }

    @Test
    public void shouldUseDBBasedTemplateManagerWhenConfigIsBlank() {

        when(IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG)).thenReturn("");
        TemplatePersistenceManager templatePersistenceManager =
                templatePersistenceManagerFactory.getTemplatePersistenceManager();
        assertTrue(templatePersistenceManager instanceof UnifiedTemplateManager);
        assertUnderlyingManagerType(templatePersistenceManager, DBBasedTemplateManager.class);
    }

    @Test
    public void shouldUseDBBasedTemplateManagerWhenConfigIsNull() {

        when(IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG)).thenReturn(null);
        TemplatePersistenceManager templatePersistenceManager =
                templatePersistenceManagerFactory.getTemplatePersistenceManager();
        assertTrue(templatePersistenceManager instanceof UnifiedTemplateManager);
        assertUnderlyingManagerType(templatePersistenceManager, DBBasedTemplateManager.class);
    }

    private void assertUnderlyingManagerType(TemplatePersistenceManager templatePersistenceManager, Class<?> expectedClass) {
        try {
            Field field = UnifiedTemplateManager.class.getDeclaredField("templatePersistenceManager");
            field.setAccessible(true);
            Object underlyingManager = field.get(templatePersistenceManager);
            assertTrue(expectedClass.isInstance(underlyingManager));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to access the underlying TemplatePersistenceManager", e);
        }
    }
}
