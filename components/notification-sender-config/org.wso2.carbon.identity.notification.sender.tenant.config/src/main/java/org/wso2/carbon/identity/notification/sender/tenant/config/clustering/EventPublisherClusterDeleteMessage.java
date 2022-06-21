/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.notification.sender.tenant.config.clustering;

import org.apache.axis2.clustering.ClusteringCommand;
import org.apache.axis2.clustering.ClusteringMessage;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.api.IdempotentMessage;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.notification.sender.tenant.config.internal.NotificationSenderTenantConfigDataHolder;
import org.wso2.carbon.identity.tenant.resource.manager.exception.TenantResourceManagementException;
import org.wso2.carbon.identity.tenant.resource.manager.util.ResourceUtils;

/**
 * Cluster Messaging for Event Publisher Deletion.
 */
@IdempotentMessage
public class EventPublisherClusterDeleteMessage extends ClusteringMessage {

    private static final Log log = LogFactory.getLog(EventPublisherClusterDeleteMessage.class);
    private static final long serialVersionUID = 176393211389794727L;
    private final String publisherResourceType;
    private final String senderName;
    private final int tenantId;

    public EventPublisherClusterDeleteMessage(String publisherResourceType, String senderName, int tenantId) {

        this.publisherResourceType = publisherResourceType;
        this.senderName = senderName;
        this.tenantId = tenantId;
    }

    @Override
    public ClusteringCommand getResponse() {

        return null;
    }

    @Override
    public void execute(ConfigurationContext configurationContext) {

        try {
            ResourceUtils.startTenantFlow(tenantId);
            NotificationSenderTenantConfigDataHolder.getInstance().getResourceManager()
                    .removeEventPublisherConfiguration(publisherResourceType, senderName);
        } catch (TenantResourceManagementException e) {
            log.error("Error while redeploying event publisher. " + senderName, e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }
}
