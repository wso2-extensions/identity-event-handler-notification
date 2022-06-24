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
import org.wso2.carbon.identity.configuration.mgt.core.model.ResourceFile;
import org.wso2.carbon.identity.notification.sender.tenant.config.internal.NotificationSenderTenantConfigDataHolder;
import org.wso2.carbon.identity.tenant.resource.manager.exception.TenantResourceManagementException;
import org.wso2.carbon.identity.tenant.resource.manager.util.ResourceUtils;

/**
 * Cluster Messaging for Event Publisher invalidation.
 */
@IdempotentMessage
public class EventPublisherClusterInvalidationMessage extends ClusteringMessage {

    private static final Log log = LogFactory.getLog(EventPublisherClusterInvalidationMessage.class);
    private static final long serialVersionUID = 708146871146295699L;
    private final String id;
    private final String name;
    private final int tenantId;

    public EventPublisherClusterInvalidationMessage(ResourceFile resourceFile, int tenantId) {

        this.name = resourceFile.getName();
        this.id = resourceFile.getId();
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
                    .addEventPublisherConfiguration(new ResourceFile(id, name));
        } catch (TenantResourceManagementException e) {
            log.error("Error while redeploying event publisher. " + name, e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }
}
