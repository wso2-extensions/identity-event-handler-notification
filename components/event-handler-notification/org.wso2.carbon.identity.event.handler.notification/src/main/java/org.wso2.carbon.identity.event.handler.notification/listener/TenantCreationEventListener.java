/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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

package org.wso2.carbon.identity.event.handler.notification.listener;

import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.event.output.adapter.core.OutputEventAdapterConfiguration;
import org.wso2.carbon.event.publisher.core.config.EventPublisherConfiguration;
import org.wso2.carbon.event.publisher.core.config.mapping.TextOutputMapping;
import org.wso2.carbon.identity.event.handler.notification.NotificationConstants;
import org.wso2.carbon.identity.event.handler.notification.internal.NotificationHandlerDataHolder;
import org.wso2.carbon.identity.event.handler.notification.util.NotificationUtil;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.user.api.UserStoreException;

import java.util.HashMap;
import java.util.Map;

public class TenantCreationEventListener implements TenantMgtListener {

    @Override
    public void onTenantCreate(TenantInfoBean tenantInfoBean) throws StratosException {

    }

    @Override
    public void onTenantUpdate(TenantInfoBean tenantInfoBean) throws StratosException {

    }

    @Override
    public void onTenantDelete(int i) {

    }

    @Override
    public void onTenantRename(int i, String s, String s2) throws StratosException {

    }

    @Override
    public void onTenantInitialActivation(int i) throws StratosException {
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            carbonContext.setTenantId(i);
            carbonContext.setTenantDomain(NotificationHandlerDataHolder.getInstance().getRealmService().getTenantManager().getDomain(i));
        } catch (UserStoreException e) {
            throw new StratosException("Error in starting a tenant flow.", e);
        }
        NotificationUtil.deployStream(NotificationConstants.EmailNotification.STREAM_NAME,
                NotificationConstants.EmailNotification.STREAM_VERSION,
                NotificationConstants.EmailNotification.STREAM_ID);
        EventPublisherConfiguration eventPublisherConfig = getEventPublisherConfig();
        NotificationUtil.deployPublisher(eventPublisherConfig);
        PrivilegedCarbonContext.endTenantFlow();

    }

    @Override
    public void onTenantActivation(int i) throws StratosException {

    }

    @Override
    public void onTenantDeactivation(int i) throws StratosException {

    }

    @Override
    public void onSubscriptionPlanChange(int i, String s, String s2) throws StratosException {

    }

    @Override
    public int getListenerOrder() {
        return 0;
    }

    @Override
    public void onPreDelete(int i) throws StratosException {

    }

    private static EventPublisherConfiguration getEventPublisherConfig() {
        EventPublisherConfiguration eventPublisherConfiguration = new EventPublisherConfiguration();
        eventPublisherConfiguration.setEventPublisherName(NotificationConstants.EmailNotification.EVENT_PUBLISHER_NAME);
        eventPublisherConfiguration.setFromStreamName(NotificationConstants.EmailNotification.STREAM_NAME);
        eventPublisherConfiguration.setFromStreamVersion(NotificationConstants.EmailNotification.STREAM_VERSION);

        OutputEventAdapterConfiguration outputEventAdapterConfiguration = new OutputEventAdapterConfiguration();
        outputEventAdapterConfiguration.setName(NotificationConstants.EmailNotification.OUTPUT_ADAPTOR_NAME);
        outputEventAdapterConfiguration.setMessageFormat(NotificationConstants.EmailNotification.OUTPUT_ADAPTOR_MESSAGE_FORMAT);
        outputEventAdapterConfiguration.setType(NotificationConstants.EmailNotification.OUTPUT_ADAPTOR_TYPE);

        eventPublisherConfiguration.setToAdapterConfiguration(outputEventAdapterConfiguration);
        TextOutputMapping textOutputMapping = new TextOutputMapping();
        textOutputMapping.setCustomMappingEnabled(true);
        textOutputMapping.setRegistryResource(false);
        textOutputMapping.setMappingText(NotificationConstants.EmailNotification.OUTPUT_MAPPING_TEXT);
        textOutputMapping.setCacheTimeoutDuration(0);

        eventPublisherConfiguration.setOutputMapping(textOutputMapping);
        Map<String, String> adapterDynamicProperties = new HashMap<>();
        adapterDynamicProperties.put(NotificationConstants.EmailNotification.OUTPUT_ADAPTOR_DYNAMIC_EMAIL_ADD_PROPERTY,
                NotificationConstants.EmailNotification.OUTPUT_ADAPTOR_DYNAMIC_EMAIL_ADD_VALUE);
        adapterDynamicProperties.put(NotificationConstants.EmailNotification.OUTPUT_ADAPTOR_DYNAMIC_EMAIL_TYPE_PROPERTY,
                NotificationConstants.EmailNotification.OUTPUT_ADAPTOR_DYNAMIC_EMAIL_TYPE_VALUE);
        adapterDynamicProperties.put(NotificationConstants.EmailNotification.OUTPUT_ADAPTOR_DYNAMIC_EMAIL_SUBJECT_PROPERTY,
                NotificationConstants.EmailNotification.OUTPUT_ADAPTOR_DYNAMIC_EMAIL_SUBJECT_VALUE);
        eventPublisherConfiguration.setToAdapterDynamicProperties(adapterDynamicProperties);
        return eventPublisherConfiguration;
    }
}