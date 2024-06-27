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

package org.wso2.carbon.email.mgt.cache;

import org.wso2.carbon.email.mgt.store.TemplatePersistenceManagerFactory;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class represent cache key for {@link NotificationTypeCache}.
 */
public class NotificationTypeCacheKey implements Serializable {

    private String notificationType;
    private String channelName;

    public NotificationTypeCacheKey(String type, String channelName) {

        this.notificationType = type;
        this.channelName = channelName;
    }

    public String getNotificationType() {

        return notificationType;
    }

    public String getChannelName() {

        return channelName;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NotificationTypeCacheKey that = (NotificationTypeCacheKey) o;

        if (!Objects.equals(notificationType, that.notificationType)) {
            return false;
        }
        return Objects.equals(channelName, that.channelName);
    }

    @Override
    public int hashCode() {

        int result = notificationType != null ? notificationType.hashCode() : 0;
        result = 31 * result + (channelName != null ? channelName.hashCode() : 0);
        return result;
    }
}
