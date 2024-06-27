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

import org.wso2.carbon.email.mgt.cache.OrgNotificationTemplateCache;
import org.wso2.carbon.email.mgt.cache.OrgNotificationTemplateCacheKey;
import org.wso2.carbon.email.mgt.cache.OrgNotificationTemplateListCache;
import org.wso2.carbon.email.mgt.cache.OrgNotificationTemplateListCacheKey;
import org.wso2.carbon.email.mgt.store.dao.OrgNotificationTemplateDAO;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides the cache backed implementation for {@link OrgNotificationTemplateDAO}.
 */
public class CacheBackedOrgNotificationTemplateDAO extends OrgNotificationTemplateDAO {

    private final OrgNotificationTemplateCache orgNotificationTemplateCache =
            OrgNotificationTemplateCache.getInstance();
    private final OrgNotificationTemplateListCache templateListCache = OrgNotificationTemplateListCache.getInstance();

    public void addNotificationTemplate(NotificationTemplate notificationTemplate, int tenantId)
            throws NotificationTemplateManagerServerException {

        super.addNotificationTemplate(notificationTemplate, tenantId);

        String locale = notificationTemplate.getLocale();
        String type = notificationTemplate.getType();
        String channel = notificationTemplate.getNotificationChannel();

        OrgNotificationTemplateCacheKey cacheKey = new OrgNotificationTemplateCacheKey(locale, type, channel);
        orgNotificationTemplateCache.addToCache(cacheKey, notificationTemplate, tenantId);

        OrgNotificationTemplateListCacheKey listCacheKey = new OrgNotificationTemplateListCacheKey(type, channel);
        templateListCache.clearCacheEntry(listCacheKey, tenantId);
    }

    public NotificationTemplate getNotificationTemplate(String locale, String templateType, String channelName,
                                                        int tenantId)
            throws NotificationTemplateManagerServerException {

        OrgNotificationTemplateCacheKey key = new OrgNotificationTemplateCacheKey(locale, templateType, channelName);
        NotificationTemplate orgNotificationTemplate = orgNotificationTemplateCache.getValueFromCache(key, tenantId);

        if (orgNotificationTemplate != null) {
            return orgNotificationTemplate;
        }

        orgNotificationTemplate = super.getNotificationTemplate(locale, templateType, channelName, tenantId);
        orgNotificationTemplateCache.addToCache(key, orgNotificationTemplate, tenantId);

        return orgNotificationTemplate;
    }

    public boolean isNotificationTemplateExists(String locale, String templateType, String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        OrgNotificationTemplateCacheKey key = new OrgNotificationTemplateCacheKey(locale, templateType, channelName);
        NotificationTemplate orgNotificationTemplate = orgNotificationTemplateCache.getValueFromCache(key, tenantId);

        if (orgNotificationTemplate != null) {
            return true;
        }

        // TODO: Not caching the state here. Revisit
        return super.isNotificationTemplateExists(locale, templateType, channelName, tenantId);
    }

    public List<NotificationTemplate> listNotificationTemplates(String templateType, String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        OrgNotificationTemplateListCacheKey key = new OrgNotificationTemplateListCacheKey(templateType, channelName);
        List<NotificationTemplate> notificationTemplates = templateListCache.getValueFromCache(key, tenantId);

        if (notificationTemplates != null) {
            return notificationTemplates;
        }

        notificationTemplates = super.listNotificationTemplates(templateType, channelName, tenantId);
        templateListCache.addToCache(key, (ArrayList<NotificationTemplate>) notificationTemplates, tenantId);

        return notificationTemplates;
    }

    public void updateNotificationTemplate(NotificationTemplate notificationTemplate, int tenantId)
            throws NotificationTemplateManagerServerException {

        super.updateNotificationTemplate(notificationTemplate, tenantId);

        String locale = notificationTemplate.getLocale();
        String type = notificationTemplate.getType();
        String channel = notificationTemplate.getNotificationChannel();

        OrgNotificationTemplateCacheKey cacheKey = new OrgNotificationTemplateCacheKey(locale, type, channel);
        orgNotificationTemplateCache.addToCache(cacheKey, notificationTemplate, tenantId);

        OrgNotificationTemplateListCacheKey listCacheKey = new OrgNotificationTemplateListCacheKey(type, channel);
        templateListCache.clearCacheEntry(listCacheKey, tenantId);
    }

    public void removeNotificationTemplate(String locale, String templateType, String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        super.removeNotificationTemplate(locale, templateType, channelName, tenantId);

        OrgNotificationTemplateCacheKey cacheKey =
                new OrgNotificationTemplateCacheKey(locale, templateType, channelName);
        orgNotificationTemplateCache.clearCacheEntry(cacheKey, tenantId);

        OrgNotificationTemplateListCacheKey listCacheKey =
                new OrgNotificationTemplateListCacheKey(templateType, channelName);
        templateListCache.clearCacheEntry(listCacheKey, tenantId);

    }

    public void removeNotificationTemplates(String templateType, String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        super.removeNotificationTemplates(templateType, channelName, tenantId);

        //TODO: Revisit for more precise invalidation logic
        orgNotificationTemplateCache.clear(tenantId);

        OrgNotificationTemplateListCacheKey listCacheKey =
                new OrgNotificationTemplateListCacheKey(templateType, channelName);
        templateListCache.clearCacheEntry(listCacheKey, tenantId);
    }
}
