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
import org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import java.nio.file.Paths;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.testng.Assert.assertTrue;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NOTIFICATION_TEMPLATES_STORAGE_CONFIG;

/**
 * Class that contains the test cases for {@link TemplatePersistenceManagerFactory}.
 */
@PrepareForTest({I18nMgtDataHolder.class, IdentityUtil.class})
public class TemplatePersistenceManagerFactoryTest extends PowerMockTestCase {

    @Mock
    RegistryResourceMgtService resourceMgtService;
    @Mock
    I18nMgtDataHolder i18nMgtDataHolder;
    private TemplatePersistenceManagerFactory templatePersistenceManagerFactory;

    @BeforeMethod
    public void setUp() {

        setUpCarbonHome();

        initMocks(this);
        mockStatic(I18nMgtDataHolder.class);
        i18nMgtDataHolder = PowerMockito.mock(I18nMgtDataHolder.class);
        when(I18nMgtDataHolder.getInstance()).thenReturn(i18nMgtDataHolder);
        when(i18nMgtDataHolder.getRegistryResourceMgtService()).thenReturn(resourceMgtService);

        mockStatic(IdentityUtil.class);
        templatePersistenceManagerFactory = new TemplatePersistenceManagerFactory();
    }

    @Test
    public void shouldReturnDBBasedTemplateManagerWhenConfigIsDatabase() {

        when(IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG)).thenReturn("database");
        TemplatePersistenceManager templatePersistenceManager =
                templatePersistenceManagerFactory.getTemplatePersistenceManager();
        assertTrue(templatePersistenceManager instanceof DBBasedTemplateManager);
    }

    @Test
    public void shouldReturnHybridTemplateManagerWhenConfigIsOnMigration() {

        when(IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG)).thenReturn("hybrid");
        TemplatePersistenceManager templatePersistenceManager =
                templatePersistenceManagerFactory.getTemplatePersistenceManager();
        assertTrue(templatePersistenceManager instanceof HybridTemplateManager);
    }

    @Test
    public void shouldReturnRegistryBasedTemplateManagerWhenConfigIsRegistry() {

        when(IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG)).thenReturn("registry");
        TemplatePersistenceManager templatePersistenceManager =
                templatePersistenceManagerFactory.getTemplatePersistenceManager();
        assertTrue(templatePersistenceManager instanceof RegistryBasedTemplateManager);
    }

    @Test
    public void shouldReturnDBBasedTemplateManagerWhenConfigIsInvalid() {

        when(IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG)).thenReturn("invalid");
        TemplatePersistenceManager templatePersistenceManager =
                templatePersistenceManagerFactory.getTemplatePersistenceManager();
        assertTrue(templatePersistenceManager instanceof DBBasedTemplateManager);
    }

    @Test
    public void shouldReturnDBBasedTemplateManagerWhenConfigIsBlank() {

        when(IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG)).thenReturn("");
        TemplatePersistenceManager templatePersistenceManager =
                templatePersistenceManagerFactory.getTemplatePersistenceManager();
        assertTrue(templatePersistenceManager instanceof DBBasedTemplateManager);
    }

    @Test
    public void shouldReturnDBBasedTemplateManagerWhenConfigIsNull() {

        when(IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG)).thenReturn(null);
        TemplatePersistenceManager templatePersistenceManager =
                templatePersistenceManagerFactory.getTemplatePersistenceManager();
        assertTrue(templatePersistenceManager instanceof DBBasedTemplateManager);
    }

    private static void setUpCarbonHome() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, Paths.get(carbonHome,
                "repository/conf").toString());
    }
}
