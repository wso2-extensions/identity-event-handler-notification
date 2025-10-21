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
import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import java.lang.reflect.Field;
import java.nio.file.Paths;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NOTIFICATION_TEMPLATES_STORAGE_CONFIG;

/**
 * Class that contains the test cases for {@link TemplatePersistenceManagerFactory}.
 */
@WithCarbonHome
public class TemplatePersistenceManagerFactoryTest {

    @Mock
    RegistryResourceMgtService resourceMgtService;
    @Mock
    I18nMgtDataHolder i18nMgtDataHolder;
    private TemplatePersistenceManagerFactory templatePersistenceManagerFactory;

    MockedStatic<I18nMgtDataHolder> i18nMgtDataHolderStatic;
    MockedStatic<IdentityUtil> identityUtilStatic;

    @BeforeMethod
    public void setUp() {

        initMocks(this);
        
        i18nMgtDataHolderStatic = mockStatic(I18nMgtDataHolder.class);
        i18nMgtDataHolder = mock(I18nMgtDataHolder.class);
        i18nMgtDataHolderStatic.when(I18nMgtDataHolder::getInstance).thenReturn(i18nMgtDataHolder);
        when(i18nMgtDataHolder.getRegistryResourceMgtService()).thenReturn(resourceMgtService);

        identityUtilStatic = mockStatic(IdentityUtil.class);
        templatePersistenceManagerFactory = new TemplatePersistenceManagerFactory();
    }
    
    @AfterMethod
    public void tearDown() {

        i18nMgtDataHolderStatic.close();
        identityUtilStatic.close();
    }

    @Test
    public void shouldUseDBBasedTemplateManagerWhenConfigIsDatabase() {

        identityUtilStatic.when(() -> IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG))
                .thenReturn("database");
        TemplatePersistenceManager templatePersistenceManager =
                templatePersistenceManagerFactory.getTemplatePersistenceManager();
        assertTrue(templatePersistenceManager instanceof UnifiedTemplateManager);
        assertUnderlyingManagerType(templatePersistenceManager, DBBasedTemplateManager.class);
    }

    @Test
    public void shouldUseHybridTemplateManagerWhenConfigIsOnMigration() {

        identityUtilStatic.when(() -> IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG))
                .thenReturn("hybrid");
        TemplatePersistenceManager templatePersistenceManager =
                templatePersistenceManagerFactory.getTemplatePersistenceManager();
        assertTrue(templatePersistenceManager instanceof UnifiedTemplateManager);
        assertUnderlyingManagerType(templatePersistenceManager, HybridTemplateManager.class);
    }

    @Test
    public void shouldUseRegistryBasedTemplateManagerWhenConfigIsRegistry() {

        identityUtilStatic.when(() -> IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG))
                .thenReturn("registry");
        TemplatePersistenceManager templatePersistenceManager =
                templatePersistenceManagerFactory.getTemplatePersistenceManager();
        assertTrue(templatePersistenceManager instanceof UnifiedTemplateManager);
        assertUnderlyingManagerType(templatePersistenceManager, RegistryBasedTemplateManager.class);
    }

    @Test
    public void shouldUseDBBasedTemplateManagerWhenConfigIsInvalid() {

        identityUtilStatic.when(() -> IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG))
                .thenReturn("invalid");
        TemplatePersistenceManager templatePersistenceManager =
                templatePersistenceManagerFactory.getTemplatePersistenceManager();
        assertTrue(templatePersistenceManager instanceof UnifiedTemplateManager);
        assertUnderlyingManagerType(templatePersistenceManager, DBBasedTemplateManager.class);
    }

    @Test
    public void shouldUseDBBasedTemplateManagerWhenConfigIsBlank() {

        identityUtilStatic.when(() -> IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG))
                .thenReturn("");
        TemplatePersistenceManager templatePersistenceManager =
                templatePersistenceManagerFactory.getTemplatePersistenceManager();
        assertTrue(templatePersistenceManager instanceof UnifiedTemplateManager);
        assertUnderlyingManagerType(templatePersistenceManager, DBBasedTemplateManager.class);
    }

    @Test
    public void shouldUseDBBasedTemplateManagerWhenConfigIsNull() {

        identityUtilStatic.when(() -> IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG))
                .thenReturn(null);
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
