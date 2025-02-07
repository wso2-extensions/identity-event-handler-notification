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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NOTIFICATION_TEMPLATES_STORAGE_CONFIG;

/**
 * Factory class to get an TemplatePersistenceManager.
 */
public class TemplatePersistenceManagerFactory {

    private static final Log log = LogFactory.getLog(TemplatePersistenceManagerFactory.class);

    /**
     * Returns a {@link TemplatePersistenceManager} implementation that handles both system templates and user defined
     * templates.
     * Based on the configuration, the returning {@link UnifiedTemplateManager} instance will use an instance
     * of user defined template manager as follows:
     * if the storage type is configured as database, {@link DBBasedTemplateManager} will be used.
     * if the storage type is configured as hybrid, {@link HybridTemplateManager} will be used.
     * if the storage type is configured as registry, {@link RegistryBasedTemplateManager} will be used.
     * For any other case, {@link DBBasedTemplateManager} will be used.
     *
     * @return an implementation of {@link TemplatePersistenceManager}.
     */
    public TemplatePersistenceManager getTemplatePersistenceManager() {

        return new UnifiedTemplateManager(getUserDefinedTemplatePersistenceManager());
    }

    /**
     * Returns a {@link TemplatePersistenceManager} implementation based on the configuration that handles only user
     * defined templates.
     * If the storage type is configured as database, an instance of {@link DBBasedTemplateManager} will be returned.
     * If the storage type is configured as hybrid, an instance of {@link HybridTemplateManager} will be returned.
     * If the storage type is configured as registry, an instance of {@link RegistryBasedTemplateManager} will be returned.
     * For any other case, an instance of {@link DBBasedTemplateManager} will be returned.
     *
     * @return an implementation of {@link TemplatePersistenceManager}.
     */
    public TemplatePersistenceManager getUserDefinedTemplatePersistenceManager() {

        String notificationTemplatesStorageType = IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG);

        TemplatePersistenceManager persistenceManager = new DBBasedTemplateManager();

        if (StringUtils.isNotBlank(notificationTemplatesStorageType)) {
            if (notificationTemplatesStorageType.equals("hybrid")) {
                persistenceManager = new HybridTemplateManager();
                log.info("Hybrid template persistent manager initialized.");
            } else if (notificationTemplatesStorageType.equals("registry")) {
                persistenceManager = new RegistryBasedTemplateManager();
                log.warn("Registry based template persistent manager initialized.");
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Template persistent manager initialized with the type: " + persistenceManager.getClass());
        }
        return persistenceManager;
    }
}
