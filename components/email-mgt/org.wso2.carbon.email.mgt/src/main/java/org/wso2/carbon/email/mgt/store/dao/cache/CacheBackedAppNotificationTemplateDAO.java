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

package org.wso2.carbon.email.mgt.store.dao.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.email.mgt.cache.AppNotificationTemplateCache;
import org.wso2.carbon.email.mgt.cache.AppNotificationTemplateCacheKey;
import org.wso2.carbon.email.mgt.cache.AppNotificationTemplateListCache;
import org.wso2.carbon.email.mgt.cache.AppNotificationTemplateListCacheKey;
import org.wso2.carbon.email.mgt.store.dao.AppNotificationTemplateDAO;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides the cache backed implementation for {@link AppNotificationTemplateDAO}.
 */
public class CacheBackedAppNotificationTemplateDAO extends AppNotificationTemplateDAO {

    private static final Log log = LogFactory.getLog(CacheBackedAppNotificationTemplateDAO.class);
    private final AppNotificationTemplateCache appNotificationTemplateCache =
            AppNotificationTemplateCache.getInstance();
    private final AppNotificationTemplateListCache templateListCache = AppNotificationTemplateListCache.getInstance();

    @Override
    public void addNotificationTemplate(NotificationTemplate notificationTemplate, String applicationUuid, int tenantId)
            throws NotificationTemplateManagerServerException {

        super.addNotificationTemplate(notificationTemplate, applicationUuid, tenantId);

        String locale = notificationTemplate.getLocale();
        String type = notificationTemplate.getType();
        String channel = notificationTemplate.getNotificationChannel();

        AppNotificationTemplateCacheKey cacheKey =
                new AppNotificationTemplateCacheKey(locale, type, channel, applicationUuid);
        appNotificationTemplateCache.addToCache(cacheKey, notificationTemplate, tenantId);

        AppNotificationTemplateListCacheKey listCacheKey =
                new AppNotificationTemplateListCacheKey(type, channel, applicationUuid);
        templateListCache.clearCacheEntry(listCacheKey, tenantId);
    }

    @Override
    public NotificationTemplate getNotificationTemplate(String locale, String templateType, String channelName,
                                                        String applicationUuid, int tenantId)
            throws NotificationTemplateManagerServerException {

        AppNotificationTemplateCacheKey key =
                new AppNotificationTemplateCacheKey(locale, templateType, channelName, applicationUuid);
        NotificationTemplate appNotificationTemplate = appNotificationTemplateCache.getValueFromCache(key, tenantId);

        if (appNotificationTemplate != null) {
            if (log.isDebugEnabled()) {
                log.debug("Cache hit in AppNotificationTemplateCache for application: " + applicationUuid +
                        ", locale: " + locale + ", template type: " + templateType + " in channel: " + channelName +
                        " for tenant: " + tenantId);
            }
            return appNotificationTemplate;
        }

        if (log.isDebugEnabled()) {
            log.debug("Cache miss in AppNotificationTemplateCache for application: " + applicationUuid + ", locale: " +
                    locale + ", template type: " + templateType + " in channel: " + channelName + " for tenant: " +
                    tenantId);
        }

        appNotificationTemplate =
                super.getNotificationTemplate(locale, templateType, channelName, applicationUuid, tenantId);
        appNotificationTemplateCache.addToCache(key, appNotificationTemplate, tenantId);

        return appNotificationTemplate;
    }

    @Override
    public boolean isNotificationTemplateExists(String locale, String templateType, String channelName,
                                                        String applicationUuid, int tenantId)
            throws NotificationTemplateManagerServerException {

        AppNotificationTemplateCacheKey key =
                new AppNotificationTemplateCacheKey(locale, templateType, channelName, applicationUuid);
        NotificationTemplate appNotificationTemplate = appNotificationTemplateCache.getValueFromCache(key, tenantId);

        if (appNotificationTemplate != null) {
            if (log.isDebugEnabled()) {
                log.debug("Cache hit in AppNotificationTemplateCache for application: " + applicationUuid +
                        ", locale: " + locale + ", template type: " + templateType + " in channel: " + channelName +
                        " for tenant: " + tenantId);
            }
            return true;
        }

        if (log.isDebugEnabled()) {
            log.debug("Cache miss in AppNotificationTemplateCache for application: " + applicationUuid + ", locale: " +
                    locale + ", template type: " + templateType + " in channel: " + channelName + " for tenant: " +
                    tenantId);
        }

        return super.isNotificationTemplateExists(locale, templateType, channelName, applicationUuid, tenantId);
    }

    @Override
    public List<NotificationTemplate> listNotificationTemplates(String templateType, String channelName,
                                                                String applicationUuid, int tenantId)
            throws NotificationTemplateManagerServerException {

        AppNotificationTemplateListCacheKey key =
                new AppNotificationTemplateListCacheKey(templateType, channelName, applicationUuid);
        List<NotificationTemplate> notificationTemplates = templateListCache.getValueFromCache(key, tenantId);

        if (notificationTemplates != null) {
            if (log.isDebugEnabled()) {
                log.debug("Cache hit in AppNotificationTemplateListCache for application: " + applicationUuid +
                        ", template type: " + templateType + " in channel: " + channelName + " for tenant: " + tenantId);
            }
            return notificationTemplates;
        }

        if (log.isDebugEnabled()) {
            log.debug("Cache miss in AppNotificationTemplateListCache for application: " + applicationUuid +
                    ", template type: " + templateType + " in channel: " + channelName + " for tenant: " + tenantId);
        }

        notificationTemplates = super.listNotificationTemplates(templateType, channelName, applicationUuid, tenantId);
        templateListCache.addToCache(key, (ArrayList<NotificationTemplate>) notificationTemplates, tenantId);

        return notificationTemplates;
    }

    @Override
    public void updateNotificationTemplate(NotificationTemplate notificationTemplate, String applicationUuid,
                                           int tenantId) throws NotificationTemplateManagerServerException {

        super.updateNotificationTemplate(notificationTemplate, applicationUuid, tenantId);

        String locale = notificationTemplate.getLocale();
        String type = notificationTemplate.getType();
        String channel = notificationTemplate.getNotificationChannel();

        AppNotificationTemplateCacheKey cacheKey =
                new AppNotificationTemplateCacheKey(locale, type, channel, applicationUuid);
        appNotificationTemplateCache.addToCache(cacheKey, notificationTemplate, tenantId);

        AppNotificationTemplateListCacheKey listCacheKey =
                new AppNotificationTemplateListCacheKey(type, channel, applicationUuid);
        templateListCache.clearCacheEntry(listCacheKey, tenantId);
    }

    @Override
    public void removeNotificationTemplate(String locale, String templateType, String channelName,
                                           String applicationUuid, int tenantId)
            throws NotificationTemplateManagerServerException {

        super.removeNotificationTemplate(locale, templateType, channelName, applicationUuid, tenantId);

        AppNotificationTemplateCacheKey cacheKey =
                new AppNotificationTemplateCacheKey(locale, templateType, channelName, applicationUuid);
        appNotificationTemplateCache.clearCacheEntry(cacheKey, tenantId);

        AppNotificationTemplateListCacheKey listCacheKey =
                new AppNotificationTemplateListCacheKey(templateType, channelName, applicationUuid);
        templateListCache.clearCacheEntry(listCacheKey, tenantId);
    }

    @Override
    public void removeNotificationTemplates(String templateType, String channelName, String applicationUuid,
                                            int tenantId) throws NotificationTemplateManagerServerException {

        super.removeNotificationTemplates(templateType, channelName, applicationUuid, tenantId);

        appNotificationTemplateCache.clear(tenantId);

        AppNotificationTemplateListCacheKey listCacheKey =
                new AppNotificationTemplateListCacheKey(templateType, channelName, applicationUuid);
        templateListCache.clearCacheEntry(listCacheKey, tenantId);
    }

    @Override
    public void removeAllNotificationTemplates(String templateType, String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        super.removeAllNotificationTemplates(templateType, channelName, tenantId);

        appNotificationTemplateCache.clear(tenantId);
        // Clearing full template list cache for tenant since it's not possible to remove all entries for a template
        // type at once.
        templateListCache.clear(tenantId);
    }
}
