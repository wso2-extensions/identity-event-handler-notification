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

package org.wso2.carbon.identity.event.handler.notification.internal;

import org.wso2.carbon.event.stream.core.EventStreamService;
import org.wso2.carbon.event.stream.core.exception.EventStreamConfigurationException;
import org.wso2.carbon.identity.event.handler.notification.exception.NotificationEventRuntimeException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.event.publisher.core.EventPublisherService;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;

public class NotificationHandlerDataHolder {

    private static volatile NotificationHandlerDataHolder instance = new NotificationHandlerDataHolder();
    RealmService realmService = null;
    RegistryService registryService = null;
    EventStreamService eventStreamService = null;
    EventPublisherService eventPublisherService = null;

    private NotificationHandlerDataHolder() {

    }

    public static NotificationHandlerDataHolder getInstance() {
        return instance;
    }

    public RegistryService getRegistryService() {
        return registryService;
    }

    public void setRegistryService(RegistryService registryService) {
        this.registryService = registryService;
    }

    public RealmService getRealmService() {
        return realmService;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

    public EventStreamService getEventStreamService() {
        return eventStreamService;
    }

    public void setEventStreamService(EventStreamService eventStreamService) {
        this.eventStreamService = eventStreamService;
    }

    public void setEventPublisherService(EventPublisherService eventPublisherService) {
        this.eventPublisherService = eventPublisherService;
    }

    public EventPublisherService getEventPublisherService() {
        return eventPublisherService;
    }

    public static void deployDefaultStreamsAndPublishers() throws NotificationEventRuntimeException {
        try {
            EventStreamService service = NotificationHandlerDataHolder.getInstance().getEventStreamService();
            //EventPublisherService eventPublisherService = NotificationHandlerDataHolder.getInstance().getEventPublisherService();

            StreamDefinition streamDefinition = new StreamDefinition("id_gov_notify_stream", "1.0.0", "id_gov_notify_stream:1.0.0");
            service.addEventStreamDefinition(streamDefinition);

            /*EventPublisherConfiguration eventPublisherConfiguration = new EventPublisherConfiguration();
            eventPublisherConfiguration.setEventPublisherName("org.wso2.carbon.identity.event.handler.notification Publisher");
            eventPublisherConfiguration.setFromStreamName("id_gov_notify_stream");
            eventPublisherConfiguration.setFromStreamVersion("1.0.0");

            OutputEventAdapterConfiguration outputEventAdapterConfiguration = new OutputEventAdapterConfiguration();
            outputEventAdapterConfiguration.setName("org.wso2.carbon.identity.event.handler.notification Publisher");
            outputEventAdapterConfiguration.setMessageFormat("text");
            outputEventAdapterConfiguration.setType("org.wso2.carbon.identity.event.handler.notification");

            eventPublisherConfiguration.setToAdapterConfiguration(outputEventAdapterConfiguration);
            TextOutputMapping textOutputMapping = new TextOutputMapping();
            textOutputMapping.setCustomMappingEnabled(true);
            textOutputMapping.setRegistryResource(false);
            textOutputMapping.setMappingText("{{body}}{{footer}}");
            textOutputMapping.setCacheTimeoutDuration(0);

            eventPublisherConfiguration.setOutputMapping(textOutputMapping);
            //setTraceEnabled(boolean enableTracing)
            //setStatisticsEnabled(boolean enableStatistics)
            Map<String, String> adapterDynamicProperties = new HashMap<>();
            adapterDynamicProperties.put("org.wso2.carbon.identity.event.handler.notification.address", "{{send-to}}");
            adapterDynamicProperties.put("org.wso2.carbon.identity.event.handler.notification.type", "text/plain");
            adapterDynamicProperties.put("org.wso2.carbon.identity.event.handler.notification.subject", "{{subject}}");
            eventPublisherConfiguration.setToAdapterDynamicProperties(adapterDynamicProperties);
            //setEditable(boolean editable)*/


           /* try{
                eventPublisherService.deployEventPublisherConfiguration(eventPublisherConfiguration);
            }catch(org.wso2.carbon.event.publisher.core.exception.EventPublisherConfigurationException e){

            }*/

        } catch (MalformedStreamDefinitionException e) {
            throw NotificationEventRuntimeException.error(e.getErrorMessage(), e);
        } catch (EventStreamConfigurationException e) {
            throw NotificationEventRuntimeException.error("Error in creating a stream");
        }
    }
}
