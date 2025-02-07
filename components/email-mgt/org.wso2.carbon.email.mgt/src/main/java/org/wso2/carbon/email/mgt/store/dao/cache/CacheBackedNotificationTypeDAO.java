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
import org.wso2.carbon.email.mgt.cache.AppNotificationTemplateListCache;
import org.wso2.carbon.email.mgt.cache.NotificationTypeCache;
import org.wso2.carbon.email.mgt.cache.NotificationTypeCacheKey;
import org.wso2.carbon.email.mgt.cache.NotificationTypeListCache;
import org.wso2.carbon.email.mgt.cache.OrgNotificationTemplateCache;
import org.wso2.carbon.email.mgt.cache.OrgNotificationTemplateListCache;
import org.wso2.carbon.email.mgt.cache.OrgNotificationTemplateListCacheKey;
import org.wso2.carbon.email.mgt.store.dao.NotificationTypeDAO;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is to perform CRUD operations for Notification Types.
 */
public class CacheBackedNotificationTypeDAO extends NotificationTypeDAO {

    private static final Log log = LogFactory.getLog(CacheBackedNotificationTypeDAO.class);
    private final NotificationTypeCache notificationTypeCache = NotificationTypeCache.getInstance();
    private final NotificationTypeListCache notificationTypeListCache = NotificationTypeListCache.getInstance();
    private final OrgNotificationTemplateCache orgNotificationTemplateCache =
            OrgNotificationTemplateCache.getInstance();
    private final OrgNotificationTemplateListCache
            orgNotificationTemplateListCache = OrgNotificationTemplateListCache.getInstance();
    private final AppNotificationTemplateCache appNotificationTemplateCache =
            AppNotificationTemplateCache.getInstance();
    private final AppNotificationTemplateListCache
            appNotificationTemplateListCache = AppNotificationTemplateListCache.getInstance();

    @Override
    public void addNotificationTemplateType(String type, String displayName, String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        super.addNotificationTemplateType(type, displayName, channelName, tenantId);

        NotificationTypeCacheKey cacheKey = new NotificationTypeCacheKey(type, channelName);
        notificationTypeCache.addToCache(cacheKey, displayName, tenantId);
        notificationTypeListCache.clearCacheEntry(channelName, tenantId);
    }

    @Override
    public String getNotificationTemplateType(String type, String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        NotificationTypeCacheKey cacheKey = new NotificationTypeCacheKey(type, channelName);
        String templateTypeDisplayName = notificationTypeCache.getValueFromCache(cacheKey, tenantId);

        if (templateTypeDisplayName != null) {
            if (log.isDebugEnabled()) {
                log.debug("Cache hit in NotificationTypeCache for template type: " + type + " in channel: " +
                        channelName + " for tenant: " + tenantId);
            }
            return templateTypeDisplayName;
        }

        if (log.isDebugEnabled()) {
            log.debug("Cache miss in NotificationTypeCache for template type: " + type + " in channel: " +
                    channelName + " for tenant: " + tenantId);
        }

        templateTypeDisplayName = super.getNotificationTemplateType(type, channelName, tenantId);
        notificationTypeCache.addToCache(cacheKey, templateTypeDisplayName, tenantId);

        return templateTypeDisplayName;
    }

    @Override
    public List<String> listNotificationTemplateTypes(String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        List<String> templateTypes = notificationTypeListCache.getValueFromCache(channelName, tenantId);

        if (templateTypes != null) {
            if (log.isDebugEnabled()) {
                log.debug("Cache hit in NotificationTypeListCache for template types in channel: " + channelName +
                        " for tenant: " + tenantId);
            }
            return templateTypes;
        }

        if (log.isDebugEnabled()) {
            log.debug("Cache miss in NotificationTypeListCache for template types in channel: " + channelName +
                    " for tenant: " + tenantId);
        }

        templateTypes = super.listNotificationTemplateTypes(channelName, tenantId);
        notificationTypeListCache.addToCache(channelName, (ArrayList<String>) templateTypes, tenantId);

        return templateTypes;
    }

    @Override
    public void deleteNotificationTemplateType(String type, String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        super.deleteNotificationTemplateType(type, channelName, tenantId);
        notificationTypeCache.clearCacheEntry(new NotificationTypeCacheKey(type, channelName), tenantId);
        notificationTypeListCache.clearCacheEntry(channelName, tenantId);

        orgNotificationTemplateCache.clear(tenantId);
        orgNotificationTemplateListCache.clearCacheEntry(new OrgNotificationTemplateListCacheKey(type, channelName),
                tenantId);

        appNotificationTemplateCache.clear(tenantId);
        appNotificationTemplateListCache.clear(tenantId);
    }
}
