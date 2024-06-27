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

import org.wso2.carbon.email.mgt.cache.NotificationTypeCache;
import org.wso2.carbon.email.mgt.cache.NotificationTypeCacheKey;
import org.wso2.carbon.email.mgt.cache.NotificationTypeListCache;
import org.wso2.carbon.email.mgt.store.dao.NotificationTypeDAO;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is to perform CRUD operations for Notification Types.
 */
public class CacheBackedNotificationTypeDAO extends NotificationTypeDAO {

    private final NotificationTypeCache notificationTypeCache = NotificationTypeCache.getInstance();
    private final NotificationTypeListCache notificationTypeListCache = NotificationTypeListCache.getInstance();

    public void addNotificationTemplateType(String type, String displayName, String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        super.addNotificationTemplateType(type, displayName, channelName, tenantId);

        NotificationTypeCacheKey cacheKey = new NotificationTypeCacheKey(type, channelName);
        notificationTypeCache.addToCache(cacheKey, displayName, tenantId);
        notificationTypeListCache.clearCacheEntry(channelName, tenantId);
    }

    public String getNotificationTemplateType(String type, String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        NotificationTypeCacheKey cacheKey = new NotificationTypeCacheKey(type, channelName);
        String templateTypeDisplayName = notificationTypeCache.getValueFromCache(cacheKey, tenantId);

        if (templateTypeDisplayName != null) {
            return templateTypeDisplayName;
        }

        templateTypeDisplayName = super.getNotificationTemplateType(type, channelName, tenantId);
        notificationTypeCache.addToCache(cacheKey, templateTypeDisplayName, tenantId);

        return templateTypeDisplayName;
    }

    public List<String> listNotificationTemplateTypes(String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        List<String> templateTypes = notificationTypeListCache.getValueFromCache(channelName, tenantId);

        if (templateTypes != null) {
            return templateTypes;
        }

        templateTypes = super.listNotificationTemplateTypes(channelName, tenantId);
        notificationTypeListCache.addToCache(channelName, (ArrayList<String>) templateTypes, tenantId);

        return templateTypes;
    }

    public void deleteNotificationTemplateType(String type, String channelName, int tenantId)
            throws NotificationTemplateManagerServerException {

        super.deleteNotificationTemplateType(type, channelName, tenantId);
        notificationTypeCache.clearCacheEntry(new NotificationTypeCacheKey(type, channelName), tenantId);
        notificationTypeListCache.clearCacheEntry(channelName, tenantId);
    }
}
