/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.notification.sender.tenant.config;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.event.publisher.core.EventPublisherService;
import org.wso2.carbon.event.publisher.core.config.EventPublisherConfiguration;
import org.wso2.carbon.event.publisher.core.exception.EventPublisherConfigurationException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementException;
import org.wso2.carbon.identity.configuration.mgt.core.model.Attribute;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resources;
import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderException;
import org.wso2.carbon.identity.notification.push.provider.impl.FCMPushProvider;
import org.wso2.carbon.identity.notification.push.provider.model.PushSenderData;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.EmailSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.PushSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.SMSSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementClientException;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementException;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementServerException;
import org.wso2.carbon.identity.notification.sender.tenant.config.handlers.ChannelConfigurationHandler;
import org.wso2.carbon.identity.notification.sender.tenant.config.internal.NotificationSenderTenantConfigDataHolder;
import org.wso2.carbon.identity.secret.mgt.core.SecretManager;
import org.wso2.carbon.identity.secret.mgt.core.SecretResolveManager;
import org.wso2.carbon.identity.secret.mgt.core.exception.SecretManagementException;
import org.wso2.carbon.identity.secret.mgt.core.model.ResolvedSecret;
import org.wso2.carbon.identity.secret.mgt.core.model.SecretType;
import org.wso2.carbon.identity.tenant.resource.manager.core.ResourceManager;
import org.wso2.carbon.idp.mgt.model.ConnectedAppsResult;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.DEFAULT_HANDLER_NAME;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.DEFAULT_PUSH_PUBLISHER;

/**
 * Unit tests for {@link NotificationSenderManagementServiceImpl}.
 */
public class NotificationSenderManagementServiceImplTest {

    private NotificationSenderManagementServiceImpl notificationSenderManagementService;

    @Mock
    private ChannelConfigurationHandler defaultChannelConfigurationHandler;
    @Mock
    private ChannelConfigurationHandler websubhubChannelConfigurationHandler;
    @Mock
    private ConfigurationManager configurationManager;
    @Mock
    private ApplicationManagementService applicationManagementService;
    @Mock
    private FCMPushProvider fcmPushProvider;
    private static final String WEB_SUB_HUB_HANDLER_NAME = "choreo";

    @BeforeMethod
    public void setup() {

        setCarbonHome();
        setCarbonContextForTenant();
        notificationSenderManagementService = new NotificationSenderManagementServiceImpl();

        initMocks(this);
        when(defaultChannelConfigurationHandler.getName()).thenReturn(DEFAULT_HANDLER_NAME);
        when(websubhubChannelConfigurationHandler.getName()).thenReturn(WEB_SUB_HUB_HANDLER_NAME);

        NotificationSenderTenantConfigDataHolder.getInstance()
                .registerConfigurationHandler(defaultChannelConfigurationHandler);
        NotificationSenderTenantConfigDataHolder.getInstance()
                .registerConfigurationHandler(websubhubChannelConfigurationHandler);
        NotificationSenderTenantConfigDataHolder.getInstance()
                .setConfigurationManager(configurationManager);
        NotificationSenderTenantConfigDataHolder.getInstance()
                .setApplicationManagementService(applicationManagementService);
        NotificationSenderTenantConfigDataHolder.getInstance()
                .addPushProvider("FCM", fcmPushProvider);
    }

    @Test(dataProvider = "addSMSSenderDataProvider")
    public void testAddSMSSender(String channelType) throws NotificationSenderManagementException {

        when(defaultChannelConfigurationHandler.addSMSSender(any(SMSSenderDTO.class)))
                .thenReturn(constructSMSSenderDto(channelType));
        when(websubhubChannelConfigurationHandler.addSMSSender(any(SMSSenderDTO.class)))
                .thenReturn(constructSMSSenderDto(channelType));

        SMSSenderDTO smsSenderDTO =
                notificationSenderManagementService.addSMSSender(constructSMSSenderDto(channelType));
        Assert.assertEquals(smsSenderDTO.getProperties().get("channel.type"), channelType);
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testAddSMSSenderException() throws NotificationSenderManagementException {

        SMSSenderDTO smsSenderDTO =
                notificationSenderManagementService.addSMSSender(constructSMSSenderDto("dummyType"));
    }

    @DataProvider(name = "addSMSSenderDataProvider")
    public Object[][] provideDataForSMSSenderDto() {

        return new Object[][]{
                // channel type
                {DEFAULT_HANDLER_NAME},
                {WEB_SUB_HUB_HANDLER_NAME},
                {""},
                {null}
        };
    }

    @Test(dataProvider = "deleteNotificationSenderDataProvider")
    public void testDeleteNotificationSender(Resource resource, ConnectedAppsResult connectedAppsResult)
            throws ConfigurationManagementException, NotificationSenderManagementException,
            IdentityApplicationManagementException {

        when(configurationManager.getResource(anyString(), anyString())).thenReturn(resource);
        when(applicationManagementService.getConnectedAppsForLocalAuthenticator(anyString(), anyString(),
                anyInt(), anyInt()))
                .thenReturn(connectedAppsResult);
        doNothing().when(websubhubChannelConfigurationHandler).deleteNotificationSender(anyString());
        doNothing().when(defaultChannelConfigurationHandler).deleteNotificationSender(anyString());

        notificationSenderManagementService.deleteNotificationSender("SMSPublisher");

    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class,
            dataProvider = "deleteNotificationSenderExceptionDataProvider")
    public void testDeleteNotificationSenderException(Resource resource, ConnectedAppsResult connectedAppsResult)
            throws NotificationSenderManagementException, ConfigurationManagementException,
            IdentityApplicationManagementException {

        when(configurationManager.getResource(anyString(), anyString())).
                thenReturn(resource);
        when(applicationManagementService.getConnectedAppsForLocalAuthenticator(anyString(), anyString(),
                anyInt(), anyInt()))
                .thenReturn(connectedAppsResult);
        notificationSenderManagementService.deleteNotificationSender("SMSPublisher");
    }

    @DataProvider(name = "deleteNotificationSenderDataProvider")
    public Object[][] provideDataForResource() {

        ConnectedAppsResult connectedAppsResult = new ConnectedAppsResult();
        connectedAppsResult.setApps(new ArrayList<>());

        return new Object[][]{
                //resource
                {constructResource(DEFAULT_HANDLER_NAME, false), connectedAppsResult},
                {constructResource(WEB_SUB_HUB_HANDLER_NAME, false), connectedAppsResult},
                {constructResource(null, false), connectedAppsResult},
                {constructResource(null, true), connectedAppsResult}
        };
    }

    @DataProvider(name = "deleteNotificationSenderExceptionDataProvider")
    public Object[][] provideDataForResourceForException() {

        ConnectedAppsResult connectedAppsResult = new ConnectedAppsResult();
        connectedAppsResult.setApps(new ArrayList<>());

        return new Object[][]{
                //resource
                {constructResource("dummyChannel", false), connectedAppsResult},
                {null, connectedAppsResult},
        };
    }

    @Test(dataProvider = "updateNotificationSenderDataProvider")
    public void testUpdateSMSSender(SMSSenderDTO smsSenderDTO, Resource resource)
            throws ConfigurationManagementException, NotificationSenderManagementException {

        when(configurationManager.getResource(anyString(), anyString())).thenReturn(resource);
        when(defaultChannelConfigurationHandler.updateSMSSender(any(SMSSenderDTO.class)))
                .thenReturn(constructSMSSenderDto(smsSenderDTO.getProperties().get("channel.type")));
        when(websubhubChannelConfigurationHandler.updateSMSSender(any(SMSSenderDTO.class)))
                .thenReturn(constructSMSSenderDto(smsSenderDTO.getProperties().get("channel.type")));
        SMSSenderDTO smsSenderDTOReturned = notificationSenderManagementService.updateSMSSender(smsSenderDTO);
        Assert.assertEquals(smsSenderDTOReturned.getProperties().get("channel.type")
                , smsSenderDTO.getProperties().get("channel.type"));

    }

    @DataProvider(name = "updateNotificationSenderDataProvider")
    public Object[][] provideDataForUpdateSMSSender() {

        SMSSenderDTO smsSenderDTO1 = constructSMSSenderDto(DEFAULT_HANDLER_NAME);
        Resource resource1 = constructResource(DEFAULT_HANDLER_NAME, false);

        SMSSenderDTO smsSenderDTO2 = constructSMSSenderDto(WEB_SUB_HUB_HANDLER_NAME);
        Resource resource2 = constructResource(WEB_SUB_HUB_HANDLER_NAME, false);

        return new Object[][]{
                //SMSSenderDTO object, Resource object
                {smsSenderDTO1, resource1},
                {smsSenderDTO2, resource2}
        };
    }

    @Test(dataProvider = "updateNotificationSenderExceptionDataProvider",
            expectedExceptions = NotificationSenderManagementClientException.class)
    public void testUpdateSMSSenderExceptions(SMSSenderDTO smsSenderDTO, Resource resource)
            throws ConfigurationManagementException, NotificationSenderManagementException {

        when(configurationManager.getResource(anyString(), anyString())).thenReturn(resource);
        when(defaultChannelConfigurationHandler.updateSMSSender(any(SMSSenderDTO.class)))
                .thenReturn(constructSMSSenderDto(smsSenderDTO.getProperties().get("channel.type")));
        when(websubhubChannelConfigurationHandler.updateSMSSender(any(SMSSenderDTO.class)))
                .thenReturn(constructSMSSenderDto(smsSenderDTO.getProperties().get("channel.type")));

        notificationSenderManagementService.updateSMSSender(smsSenderDTO);

    }

    @DataProvider(name = "updateNotificationSenderExceptionDataProvider")
    public Object[][] provideDataForUpdateSMSSenderExceptions() {

        SMSSenderDTO smsSenderDTO1 = constructSMSSenderDto(DEFAULT_HANDLER_NAME);

        SMSSenderDTO smsSenderDTO2 = constructSMSSenderDto(DEFAULT_HANDLER_NAME);
        Resource resource2 = constructResource(WEB_SUB_HUB_HANDLER_NAME, false);

        SMSSenderDTO smsSenderDTO3 = constructSMSSenderDto("dummyType");
        Resource resource3 = constructResource("dummyType", false);

        return new Object[][]{
                //SMSSenderDTO object, Resource object
                {smsSenderDTO1, null},
                {smsSenderDTO2, resource2},
                {smsSenderDTO3, resource3}
        };
    }

    @Test
    public void testAddPushSender()
            throws NotificationSenderManagementException, ConfigurationManagementException, PushProviderException {

        PushSenderDTO pushSender = new PushSenderDTO();
        pushSender.setProvider("FCM");
        Map<String, String> properties = new HashMap<>();
        properties.put("key1", "value1");
        pushSender.setProperties(properties);

        when(fcmPushProvider.preProcessProperties(any(PushSenderData.class))).thenReturn(properties);

        Resource resource = new Resource();
        resource.setResourceName(DEFAULT_PUSH_PUBLISHER);
        resource.setResourceId("sampleResourceId");
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("key1", "value1"));
        attributes.add(new Attribute("provider", "FCM"));
        resource.setAttributes(attributes);
        when(configurationManager.addResource(any(String.class), any(Resource.class))).thenReturn(resource);

        when(fcmPushProvider.storePushProviderSecretProperties(any(PushSenderData.class))).thenReturn(properties);
        when(fcmPushProvider.retrievePushProviderSecretProperties(any(PushSenderData.class))).thenReturn(properties);
        when(fcmPushProvider.postProcessProperties(any(PushSenderData.class))).thenReturn(properties);

        PushSenderDTO result = notificationSenderManagementService.addPushSender(pushSender);

        assert DEFAULT_PUSH_PUBLISHER.equals(result.getName());
        assert "FCM".equals(result.getProvider());
        assert properties.equals(result.getProperties());

        verify(configurationManager, times(1)).getResource(any(String.class), any(String.class));
        verify(configurationManager, times(1)).addResource(any(String.class), any(Resource.class));
        verify(fcmPushProvider, times(1)).storePushProviderSecretProperties(any(PushSenderData.class));
        verify(fcmPushProvider, times(1)).preProcessProperties(any(PushSenderData.class));
        verify(fcmPushProvider, times(1)).postProcessProperties(any(PushSenderData.class));
        verify(fcmPushProvider, times(1)).retrievePushProviderSecretProperties(any(PushSenderData.class));
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testAddPushSenderFailByExistingResource()
            throws NotificationSenderManagementException, ConfigurationManagementException {

        PushSenderDTO pushSender = new PushSenderDTO();
        pushSender.setProvider("FCM");
        Map<String, String> properties = new HashMap<>();
        properties.put("key1", "value1");
        pushSender.setProperties(properties);

        when(configurationManager.getResource(anyString(), anyString())).thenReturn(new Resource());
        notificationSenderManagementService.addPushSender(pushSender);
    }

    @Test(expectedExceptions = NotificationSenderManagementServerException.class)
    public void testAddPushSenderFailByProvider() throws NotificationSenderManagementException {

        PushSenderDTO pushSender = new PushSenderDTO();
        pushSender.setProvider("TestProvider");
        Map<String, String> properties = new HashMap<>();
        properties.put("key1", "value1");
        pushSender.setProperties(properties);

        notificationSenderManagementService.addPushSender(pushSender);
    }

    @Test(expectedExceptions = NotificationSenderManagementException.class)
    public void testAddPushSenderFailByConfigManagement() throws ConfigurationManagementException,
            PushProviderException, NotificationSenderManagementException {

        PushSenderDTO pushSender = new PushSenderDTO();
        pushSender.setProvider("FCM");
        Map<String, String> properties = new HashMap<>();
        properties.put("key1", "value1");
        pushSender.setProperties(properties);

        when(fcmPushProvider.preProcessProperties(any(PushSenderData.class))).thenReturn(properties);

        Resource resource = new Resource();
        resource.setResourceName(DEFAULT_PUSH_PUBLISHER);
        resource.setResourceId("sampleResourceId");
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("key1", "value1"));
        attributes.add(new Attribute("provider", "FCM"));
        resource.setAttributes(attributes);
        when(configurationManager.addResource(any(String.class), any(Resource.class))).thenThrow(
                ConfigurationManagementException.class);

        notificationSenderManagementService.addPushSender(pushSender);
    }

    @Test(expectedExceptions = NotificationSenderManagementException.class)
    public void testAddPushSenderFailWhenStoringProviderSecrets()
            throws PushProviderException, ConfigurationManagementException, NotificationSenderManagementException {

        PushSenderDTO pushSender = new PushSenderDTO();
        pushSender.setProvider("FCM");
        Map<String, String> properties = new HashMap<>();
        properties.put("key1", "value1");
        pushSender.setProperties(properties);

        when(fcmPushProvider.preProcessProperties(any(PushSenderData.class))).thenReturn(properties);

        Resource resource = new Resource();
        resource.setResourceName(DEFAULT_PUSH_PUBLISHER);
        resource.setResourceId("sampleResourceId");
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("key1", "value1"));
        attributes.add(new Attribute("provider", "FCM"));
        resource.setAttributes(attributes);
        when(configurationManager.addResource(any(String.class), any(Resource.class))).thenReturn(resource);

        when(fcmPushProvider.storePushProviderSecretProperties(any(PushSenderData.class))).thenThrow(
                PushProviderException.class);

        notificationSenderManagementService.addPushSender(pushSender);
    }

    @Test
    public void testGetPushSender()
            throws NotificationSenderManagementException, PushProviderException, ConfigurationManagementException {

        String senderName = "TestPushSender";
        Map<String, String> properties = new HashMap<>();
        properties.put("key1", "value1");

        Resource resource = new Resource();
        resource.setResourceName(senderName);
        resource.setResourceId("sampleResourceId");
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("key1", "value1"));
        attributes.add(new Attribute("provider", "FCM"));
        resource.setAttributes(attributes);

        when(configurationManager.getResource(anyString(), anyString())).thenReturn(resource);
        when(fcmPushProvider.retrievePushProviderSecretProperties(any(PushSenderData.class))).thenReturn(properties);
        when(fcmPushProvider.postProcessProperties(any(PushSenderData.class))).thenReturn(properties);

        PushSenderDTO result = notificationSenderManagementService.getPushSender(senderName, false);

        assert senderName.equals(result.getName());
        assert "FCM".equals(result.getProvider());
        assert properties.equals(result.getProperties());

        verify(configurationManager, times(1)).getResource(anyString(), anyString());
        verify(fcmPushProvider, times(1)).postProcessProperties(any(PushSenderData.class));
        verify(fcmPushProvider, times(1)).retrievePushProviderSecretProperties(any(PushSenderData.class));
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testGetPushSenderFailureByNoResource() throws NotificationSenderManagementException {

        String senderName = "TestPushSender";
        Map<String, String> properties = new HashMap<>();
        properties.put("key1", "value1");

        notificationSenderManagementService.getPushSender(senderName, false);
    }

    @Test
    public void testGetPushSenders()
            throws NotificationSenderManagementException, ConfigurationManagementException, PushProviderException {

        String senderName = "TestPushSender";
        Map<String, String> properties = new HashMap<>();
        properties.put("key1", "value1");

        Resource resource = new Resource();
        resource.setResourceName(senderName);
        resource.setResourceId("sampleResourceId");
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("key1", "value1"));
        attributes.add(new Attribute("provider", "FCM"));
        attributes.add(new Attribute("type", "push"));
        resource.setAttributes(attributes);
        List<Resource> resourceList = new ArrayList<>();
        resourceList.add(resource);
        Resources resources = new Resources(resourceList);

        when(configurationManager.getResourcesByType(anyString())).thenReturn(resources);
        when(fcmPushProvider.retrievePushProviderSecretProperties(any(PushSenderData.class))).thenReturn(properties);
        when(fcmPushProvider.postProcessProperties(any(PushSenderData.class))).thenReturn(properties);

        List<PushSenderDTO> results = notificationSenderManagementService.getPushSenders(false);
        assert results.size() == 1;
        PushSenderDTO result = results.get(0);
        assert senderName.equals(result.getName());
        assert "FCM".equals(result.getProvider());
        assert properties.equals(result.getProperties());

        verify(configurationManager, times(1)).getResourcesByType(anyString());
        verify(fcmPushProvider, times(1)).postProcessProperties(any(PushSenderData.class));
        verify(fcmPushProvider, times(1)).retrievePushProviderSecretProperties(any(PushSenderData.class));
    }

    @Test(expectedExceptions = NotificationSenderManagementException.class)
    public void testGetPushSendersFailureByConfigManagement() throws ConfigurationManagementException,
            NotificationSenderManagementException {

        when(configurationManager.getResourcesByType(anyString())).thenThrow(ConfigurationManagementException.class);
        notificationSenderManagementService.getPushSenders(false);
    }

    @Test
    public void testUpdatePushSender()
            throws NotificationSenderManagementException, ConfigurationManagementException, PushProviderException {

        PushSenderDTO pushSender = new PushSenderDTO();
        pushSender.setName(DEFAULT_PUSH_PUBLISHER);
        pushSender.setProvider("FCM");
        Map<String, String> properties = new HashMap<>();
        properties.put("key1", "value1");
        pushSender.setProperties(properties);

        Resource resource = new Resource();
        resource.setResourceName(DEFAULT_PUSH_PUBLISHER);
        resource.setResourceId("sampleResourceId");
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("key1", "value1"));
        attributes.add(new Attribute("provider", "FCM"));
        resource.setAttributes(attributes);

        when(configurationManager.getResource(anyString(), anyString())).thenReturn(resource);
        when(configurationManager.replaceResource(any(String.class), any(Resource.class))).thenReturn(resource);
        when(fcmPushProvider.storePushProviderSecretProperties(any(PushSenderData.class))).thenReturn(properties);
        when(fcmPushProvider.retrievePushProviderSecretProperties(any(PushSenderData.class))).thenReturn(properties);
        when(fcmPushProvider.postProcessProperties(any(PushSenderData.class))).thenReturn(properties);

        PushSenderDTO result = notificationSenderManagementService.updatePushSender(pushSender);

        assert DEFAULT_PUSH_PUBLISHER.equals(result.getName());
        assert "FCM".equals(result.getProvider());
        assert properties.equals(result.getProperties());

        verify(configurationManager, times(1)).getResource(anyString(), anyString());
        verify(configurationManager, times(1)).replaceResource(any(String.class), any(Resource.class));
        verify(fcmPushProvider, times(1)).storePushProviderSecretProperties(any(PushSenderData.class));
        verify(fcmPushProvider, times(1)).postProcessProperties(any(PushSenderData.class));
        verify(fcmPushProvider, times(1)).retrievePushProviderSecretProperties(any(PushSenderData.class));
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testUpdatePushSenderFailByNoResource()
            throws NotificationSenderManagementException, ConfigurationManagementException {

        PushSenderDTO pushSender = new PushSenderDTO();
        pushSender.setProvider("FCM");
        Map<String, String> properties = new HashMap<>();
        properties.put("key1", "value1");
        pushSender.setProperties(properties);

        when(configurationManager.getResource(anyString(), anyString())).thenReturn(null);
        notificationSenderManagementService.updatePushSender(pushSender);
    }

    @Test(expectedExceptions = NotificationSenderManagementServerException.class)
    public void testUpdatePushSenderFailByProvider()
            throws NotificationSenderManagementException, ConfigurationManagementException {

        PushSenderDTO pushSender = new PushSenderDTO();
        pushSender.setName(DEFAULT_PUSH_PUBLISHER);
        pushSender.setProvider("TestProvider");
        Map<String, String> properties = new HashMap<>();
        properties.put("key1", "value1");
        pushSender.setProperties(properties);

        Resource resource = new Resource();
        resource.setResourceName(DEFAULT_PUSH_PUBLISHER);
        resource.setResourceId("sampleResourceId");
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("key1", "value1"));
        attributes.add(new Attribute("provider", "FCM"));
        resource.setAttributes(attributes);

        when(configurationManager.getResource(anyString(), anyString())).thenReturn(resource);

        notificationSenderManagementService.updatePushSender(pushSender);
    }

    @Test(expectedExceptions = NotificationSenderManagementException.class)
    public void testUpdatePushSenderFailByConfigManagement()
            throws NotificationSenderManagementException, ConfigurationManagementException {

        PushSenderDTO pushSender = new PushSenderDTO();
        pushSender.setName(DEFAULT_PUSH_PUBLISHER);
        pushSender.setProvider("FCM");
        Map<String, String> properties = new HashMap<>();
        properties.put("key1", "value1");
        pushSender.setProperties(properties);

        Resource resource = new Resource();
        resource.setResourceName(DEFAULT_PUSH_PUBLISHER);
        resource.setResourceId("sampleResourceId");
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("key1", "value1"));
        attributes.add(new Attribute("provider", "FCM"));
        resource.setAttributes(attributes);

        when(configurationManager.getResource(anyString(), anyString())).thenReturn(resource);
        when(configurationManager.replaceResource(any(String.class), any(Resource.class))).thenThrow(
                ConfigurationManagementException.class);

        notificationSenderManagementService.updatePushSender(pushSender);
    }

    @Test
    public void testDeletePushNotificationSender()
            throws ConfigurationManagementException, NotificationSenderManagementException,
            IdentityApplicationManagementException, PushProviderException {

        Resource resource = new Resource();
        resource.setResourceName(DEFAULT_PUSH_PUBLISHER);
        resource.setResourceId("sampleResourceId");
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("key1", "value1"));
        attributes.add(new Attribute("provider", "FCM"));
        attributes.add(new Attribute("type", "push"));
        resource.setAttributes(attributes);
        resource.setHasAttribute(true);

        ConnectedAppsResult connectedAppsResult = new ConnectedAppsResult();
        connectedAppsResult.setApps(new ArrayList<>());

        when(configurationManager.getResource(anyString(), anyString())).thenReturn(resource);
        when(applicationManagementService.getConnectedAppsForLocalAuthenticator(anyString(), anyString(),
                anyInt(), anyInt()))
                .thenReturn(connectedAppsResult);
        doNothing().when(websubhubChannelConfigurationHandler).deleteNotificationSender(anyString());
        doNothing().when(defaultChannelConfigurationHandler).deleteNotificationSender(anyString());

        notificationSenderManagementService.deleteNotificationSender("PushPublisher");

        verify(fcmPushProvider, times(1)).deletePushProviderSecretProperties(any(PushSenderData.class));
    }

    @Test(expectedExceptions = NotificationSenderManagementServerException.class)
    public void testDeletePushNotificationSenderFailByProvider()
            throws ConfigurationManagementException, NotificationSenderManagementException,
            IdentityApplicationManagementException, PushProviderException {

        Resource resource = new Resource();
        resource.setResourceName(DEFAULT_PUSH_PUBLISHER);
        resource.setResourceId("sampleResourceId");
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("key1", "value1"));
        attributes.add(new Attribute("provider", "FCM"));
        attributes.add(new Attribute("type", "push"));
        resource.setAttributes(attributes);
        resource.setHasAttribute(true);

        ConnectedAppsResult connectedAppsResult = new ConnectedAppsResult();
        connectedAppsResult.setApps(new ArrayList<>());

        when(configurationManager.getResource(anyString(), anyString())).thenReturn(resource);
        when(applicationManagementService.getConnectedAppsForLocalAuthenticator(anyString(), anyString(),
                anyInt(), anyInt()))
                .thenReturn(connectedAppsResult);
        doNothing().when(websubhubChannelConfigurationHandler).deleteNotificationSender(anyString());
        doNothing().when(defaultChannelConfigurationHandler).deleteNotificationSender(anyString());

        doThrow(PushProviderException.class).when(fcmPushProvider)
                .deletePushProviderSecretProperties(any(PushSenderData.class));

        notificationSenderManagementService.deleteNotificationSender("PushPublisher");
    }

    @Test
    public void testValidateInputsSuccessWithBasicAuthWithV1() throws NotificationSenderManagementClientException {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setSmtpServerHost("smtp.example.com");
        emailSender.setSmtpPort(587);
        emailSender.setFromAddress("test@example.com");
        emailSender.setAuthType(null);
        emailSender.setUsername("testUser");
        emailSender.setPassword("testPassword");

        Map<String, String> properties = new HashMap<>();
        properties.put("mail.smtp.signature", "Test Display Name");
        properties.put("mail.smtp.replyTo", "reply@example.com");
        properties.put("userName", "testUser");
        properties.put("password", "testPassword");
        emailSender.setProperties(properties);

        notificationSenderManagementService.validateInputs(emailSender);
    }

    @Test
    public void testValidateInputsSuccessWithBasicAuthV2() throws NotificationSenderManagementClientException {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setSmtpServerHost("smtp.example.com");
        emailSender.setSmtpPort(587);
        emailSender.setFromAddress("test@example.com");
        emailSender.setAuthType("BASIC");

        Map<String, String> properties = new HashMap<>();
        properties.put("mail.smtp.signature", "Test Display Name");
        properties.put("mail.smtp.replyTo", "reply@example.com");
        properties.put("userName", "testUser");
        properties.put("password", "testPassword");
        emailSender.setProperties(properties);

        notificationSenderManagementService.validateInputs(emailSender);
    }

    @Test
    public void testValidateInputsSuccessWithClientCredentialAuth() throws NotificationSenderManagementClientException {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setSmtpServerHost("smtp.example.com");
        emailSender.setSmtpPort(587);
        emailSender.setFromAddress("test@example.com");
        emailSender.setAuthType("CLIENT_CREDENTIAL");

        Map<String, String> properties = new HashMap<>();
        properties.put("mail.smtp.signature", "Test Display Name");
        properties.put("mail.smtp.replyTo", "reply@example.com");
        properties.put("clientId", "clientId");
        properties.put("clientSecret", "clientSecret");
        properties.put("tokenEndpoint", "https://example.com/token");
        properties.put("scopes", "email");
        emailSender.setProperties(properties);

        notificationSenderManagementService.validateInputs(emailSender);
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testValidateInputsMissingRequiredFields() throws NotificationSenderManagementClientException {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setSmtpServerHost("smtp.example.com");
        emailSender.setSmtpPort(587);
        emailSender.setFromAddress("test@example.com");

        Map<String, String> properties = new HashMap<>();
        properties.put("displayName", "Test Display Name");
        emailSender.setProperties(properties);

        notificationSenderManagementService.validateInputs(emailSender);
    }

    @Test(dataProvider = "invalidAuthTypeDataProvider",
            expectedExceptions = NotificationSenderManagementClientException.class)
    public void testValidateInputsInvalidAuthType(String authType, String username, String password,
                                                  Map<String, String> properties)
            throws NotificationSenderManagementClientException {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setSmtpServerHost("smtp.example.com");
        emailSender.setSmtpPort(587);
        emailSender.setFromAddress("test@example.com");
        emailSender.setAuthType(authType);
        emailSender.setUsername(username);
        emailSender.setPassword(password);
        emailSender.setProperties(properties);

        notificationSenderManagementService.validateInputs(emailSender);
    }

    @DataProvider(name = "invalidAuthTypeDataProvider")
    public Object[][] provideInvalidAuthTypeData() {

        Map<String, String> properties1 = new HashMap<>();
        properties1.put("mail.smtp.signature", "Test Display Name");
        properties1.put("mail.smtp.replyTo", "reply@example.com");

        Map<String, String> properties2 = new HashMap<>();
        properties2.put("mail.smtp.signature", "Test Display Name");
        properties2.put("mail.smtp.replyTo", "reply@example.com");
        properties2.put("userName", "testUser");

        Map<String, String> properties3 = new HashMap<>();
        properties3.put("mail.smtp.signature", "Test Display Name");
        properties3.put("mail.smtp.replyTo", "reply@example.com");
        properties3.put("userName", "testUser");
        properties3.put("password", "testPassword");

        Map<String, String> properties4 = new HashMap<>();
        properties4.put("mail.smtp.signature", "Test Display Name");
        properties4.put("mail.smtp.replyTo", "reply@example.com");
        properties4.put("clientId", "client_id");
        properties4.put("clientSecret", "client_secret");
        properties4.put("tokenEndpoint", "https://example.com/token");
        properties4.put("scopes", "email");

        return new Object[][]{
                // authType, username, password, properties

                // Invalid: username/password in properties missing.
                {"BASIC", "testUser", "testPassword", properties1},

                // Invalid: username/password in properties present.
                {"BASIC", null, null, properties2},

                // Invalid: username/password present in both properties and first class attributes.
                {"BASIC", "testUser", "testPassword", properties3},

                // Invalid: clientId/clientSecret present in properties with BASIC authType.
                {"BASIC", null, null, properties4},

                // Invalid: username/password set for CLIENT_CREDENTIAL.
                {"CLIENT_CREDENTIAL", "testUser", "testPassword", properties2},

                // Invalid: username/password set for CLIENT_CREDENTIAL.
                {"CLIENT_CREDENTIAL", "testUser", "testPassword", properties1},

                // Invalid: username/password set for CLIENT_CREDENTIAL.
                {"CLIENT_CREDENTIAL", null, null, properties3},

                // Invalid: username/password present in properties without the authType.
                {null, null, null, properties3},

                // Invalid: clientId/clientSecret present in properties without the authType.
                {null, null, null, properties4}
        };
    }

    private SMSSenderDTO constructSMSSenderDto(String channelType) {

        SMSSenderDTO smsSenderDTO = new SMSSenderDTO();
        smsSenderDTO.setName("SMSPublisher");
        smsSenderDTO.setProvider("WebSubHub");
        smsSenderDTO.setProviderURL("https://api.dummy.com");
        smsSenderDTO.setContentType("FORM");

        if (channelType != null) {
            Map<String, String> propertyMap = new HashMap<>();
            propertyMap.put("channel.type", channelType);
            smsSenderDTO.setProperties(propertyMap);
        }
        return smsSenderDTO;
    }

    private Resource constructResource(String channelType, Boolean addOtherAttribute) {

        Resource resource = new Resource();
        List<Attribute> attributes = new ArrayList<>();

        if (addOtherAttribute) {
            Attribute attribute = new Attribute();
            attribute.setKey("dummy.key");
            attribute.setValue("dummy.value");
            attributes.add(attribute);
        }

        if (channelType != null) {
            Attribute channelTypeAttribute = new Attribute();
            channelTypeAttribute.setKey("channel.type");
            channelTypeAttribute.setValue(channelType);
            attributes.add(channelTypeAttribute);
        }
        if (!attributes.isEmpty()) {
            resource.setAttributes(attributes);
        }
        return resource;
    }

    private void setCarbonHome() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes", "repository").
                toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, Paths.get(carbonHome, "conf").toString());
    }

    private void setCarbonContextForTenant() {

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain("tenant");
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(1);
    }

    /**
     * Sets up all mocks required for HTTP-based email sender operations.
     * This configures EventPublisherService, ResourceManager, SecretManager and SecretResolveManager
     * on the singleton DataHolder so that the static methods in NotificationSenderSecretProcessor work.
     */
    private void setupHttpEmailSenderMocks(String publisherName)
            throws EventPublisherConfigurationException, SecretManagementException {

        // Mock EventPublisherService for getDefaultPublisherProperties.
        EventPublisherService eventPublisherService = Mockito.mock(EventPublisherService.class);
        EventPublisherConfiguration mockPublisherConfig = Mockito.mock(EventPublisherConfiguration.class);
        when(mockPublisherConfig.getEventPublisherName()).thenReturn(publisherName);
        when(mockPublisherConfig.getFromStreamName()).thenReturn("id_gov_notify_stream");
        when(mockPublisherConfig.getFromStreamVersion()).thenReturn("1.0.0");
        List<EventPublisherConfiguration> publisherList = new ArrayList<>();
        publisherList.add(mockPublisherConfig);
        when(eventPublisherService.getAllActiveEventPublisherConfigurations()).thenReturn(publisherList);
        NotificationSenderTenantConfigDataHolder.getInstance().setCarbonEventPublisherService(eventPublisherService);

        // Mock ResourceManager for reDeployEventPublisherConfiguration.
        ResourceManager resourceManager = Mockito.mock(ResourceManager.class);
        NotificationSenderTenantConfigDataHolder.getInstance().setResourceManager(resourceManager);

        // Mock SecretManager for encrypt/decrypt/delete credential operations.
        SecretManager secretManager = Mockito.mock(SecretManager.class);
        SecretType mockSecretType = Mockito.mock(SecretType.class);
        when(mockSecretType.getId()).thenReturn("mock-secret-type-id");
        when(secretManager.getSecretType(anyString())).thenReturn(mockSecretType);
        when(secretManager.isSecretExist(anyString(), anyString())).thenReturn(true);
        NotificationSenderTenantConfigDataHolder.getInstance().setSecretManager(secretManager);

        // Mock SecretResolveManager for decryptCredential operations.
        SecretResolveManager secretResolveManager = Mockito.mock(SecretResolveManager.class);
        ResolvedSecret resolvedSecret = Mockito.mock(ResolvedSecret.class);
        when(resolvedSecret.getResolvedSecretValue()).thenReturn("decrypted-value");
        when(secretResolveManager.getResolvedSecret(anyString(), anyString())).thenReturn(resolvedSecret);
        NotificationSenderTenantConfigDataHolder.getInstance().setSecretResolveManager(secretResolveManager);
    }

    /**
     * Constructs an HTTP-based EmailSenderDTO with the given auth type and properties.
     */
    private EmailSenderDTO constructHttpEmailSender(String name, String authType, Map<String, String> extraProperties) {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setName(name);
        emailSender.setProvider("HTTP");
        emailSender.setProviderURL("https://api.email-provider.com/send");
        emailSender.setFromAddress("noreply@example.com");
        emailSender.setAuthType(authType);

        Map<String, String> properties = new HashMap<>();
        properties.put("body", "{\"to\":\"{{to}}\",\"subject\":\"{{subject}}\",\"body\":\"{{body}}\"}");
        if (extraProperties != null) {
            properties.putAll(extraProperties);
        }
        emailSender.setProperties(properties);
        return emailSender;
    }

    /**
     * Constructs a Resource with attributes representing a persisted HTTP email sender.
     */
    private Resource constructHttpEmailSenderResource(String name, String authType) {

        Resource resource = new Resource();
        resource.setResourceName(name);
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("provider", "HTTP"));
        attributes.add(new Attribute("providerURL", "https://api.email-provider.com/send"));
        attributes.add(new Attribute("fromAddress", "noreply@example.com"));
        attributes.add(new Attribute("authType", authType));
        attributes.add(new Attribute("type", "email"));
        attributes.add(new Attribute("body", "{\"to\":\"{{to}}\",\"subject\":\"{{subject}}\",\"body\":\"{{body}}\"}"));
        resource.setAttributes(attributes);
        resource.setFiles(new ArrayList<>());
        return resource;
    }

    @Test
    public void testAddHttpEmailSenderWithBasicAuth() throws Exception {

        String name = "HttpEmailPublisher";
        setupHttpEmailSenderMocks(name);

        Map<String, String> authProps = new HashMap<>();
        authProps.put("userName", "user1");
        authProps.put("password", "pass1");
        EmailSenderDTO emailSender = constructHttpEmailSender(name, "BASIC", authProps);

        Resource addedResource = constructHttpEmailSenderResource(name, "BASIC");
        addedResource.getAttributes().add(new Attribute("userName", "encrypted-ref"));
        addedResource.getAttributes().add(new Attribute("password", "encrypted-ref"));
        when(configurationManager.addResource(anyString(), any(Resource.class))).thenReturn(addedResource);

        EmailSenderDTO result = notificationSenderManagementService.addEmailSender(emailSender, true);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getName(), name);
        Assert.assertEquals(result.getProvider(), "HTTP");
        Assert.assertEquals(result.getAuthType(), "BASIC");
        verify(configurationManager).addResource(anyString(), any(Resource.class));
    }

    @Test
    public void testAddHttpEmailSenderWithClientCredentialAuth() throws Exception {

        String name = "HttpEmailPublisher";
        setupHttpEmailSenderMocks(name);

        Map<String, String> authProps = new HashMap<>();
        authProps.put("clientId", "my-client-id");
        authProps.put("clientSecret", "my-client-secret");
        authProps.put("tokenEndpoint", "https://auth.example.com/token");
        authProps.put("scopes", "email.send");
        EmailSenderDTO emailSender = constructHttpEmailSender(name, "CLIENT_CREDENTIAL", authProps);

        Resource addedResource = constructHttpEmailSenderResource(name, "CLIENT_CREDENTIAL");
        addedResource.getAttributes().add(new Attribute("clientId", "encrypted-ref"));
        addedResource.getAttributes().add(new Attribute("tokenEndpoint", "https://auth.example.com/token"));
        addedResource.getAttributes().add(new Attribute("scopes", "email.send"));
        when(configurationManager.addResource(anyString(), any(Resource.class))).thenReturn(addedResource);

        EmailSenderDTO result = notificationSenderManagementService.addEmailSender(emailSender, true);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getAuthType(), "CLIENT_CREDENTIAL");
        verify(configurationManager).addResource(anyString(), any(Resource.class));
    }

    @Test
    public void testAddHttpEmailSenderWithBearerAuth() throws Exception {

        String name = "HttpEmailPublisher";
        setupHttpEmailSenderMocks(name);

        Map<String, String> authProps = new HashMap<>();
        authProps.put("accessToken", "my-bearer-token");
        EmailSenderDTO emailSender = constructHttpEmailSender(name, "BEARER", authProps);

        Resource addedResource = constructHttpEmailSenderResource(name, "BEARER");
        when(configurationManager.addResource(anyString(), any(Resource.class))).thenReturn(addedResource);

        EmailSenderDTO result = notificationSenderManagementService.addEmailSender(emailSender, true);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getAuthType(), "BEARER");
        verify(configurationManager).addResource(anyString(), any(Resource.class));
    }

    @Test
    public void testAddHttpEmailSenderWithApiKeyAuth() throws Exception {

        String name = "HttpEmailPublisher";
        setupHttpEmailSenderMocks(name);

        Map<String, String> authProps = new HashMap<>();
        authProps.put("apiKeyHeader", "X-API-Key");
        authProps.put("apiKeyValue", "my-api-key-value");
        EmailSenderDTO emailSender = constructHttpEmailSender(name, "API_KEY", authProps);

        Resource addedResource = constructHttpEmailSenderResource(name, "API_KEY");
        addedResource.getAttributes().add(new Attribute("apiKeyHeader", "X-API-Key"));
        when(configurationManager.addResource(anyString(), any(Resource.class))).thenReturn(addedResource);

        EmailSenderDTO result = notificationSenderManagementService.addEmailSender(emailSender, true);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getAuthType(), "API_KEY");
        verify(configurationManager).addResource(anyString(), any(Resource.class));
    }

    @Test
    public void testAddHttpEmailSenderWithNoAuth() throws Exception {

        String name = "HttpEmailPublisher";
        setupHttpEmailSenderMocks(name);

        EmailSenderDTO emailSender = constructHttpEmailSender(name, "NONE", null);

        Resource addedResource = constructHttpEmailSenderResource(name, "NONE");
        when(configurationManager.addResource(anyString(), any(Resource.class))).thenReturn(addedResource);

        EmailSenderDTO result = notificationSenderManagementService.addEmailSender(emailSender, true);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getAuthType(), "NONE");
        verify(configurationManager).addResource(anyString(), any(Resource.class));
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testAddHttpEmailSenderFailWhenAlreadyExists() throws Exception {

        String name = "HttpEmailPublisher";
        setupHttpEmailSenderMocks(name);

        when(configurationManager.getResource(anyString(), anyString()))
                .thenReturn(constructHttpEmailSenderResource(name, "BASIC"));

        Map<String, String> authProps = new HashMap<>();
        authProps.put("userName", "user1");
        authProps.put("password", "pass1");
        EmailSenderDTO emailSender = constructHttpEmailSender(name, "BASIC", authProps);

        notificationSenderManagementService.addEmailSender(emailSender, true);
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testAddHttpEmailSenderFailMissingProviderURL() throws Exception {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setName("HttpEmailPublisher");
        emailSender.setProvider("HTTP");
        // providerURL intentionally not set
        emailSender.setAuthType("NONE");
        Map<String, String> properties = new HashMap<>();
        properties.put("body", "{\"to\":\"{{to}}\",\"subject\":\"{{subject}}\",\"body\":\"{{body}}\"}");
        emailSender.setProperties(properties);

        notificationSenderManagementService.addEmailSender(emailSender);
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testAddHttpEmailSenderFailMissingBody() throws Exception {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setName("HttpEmailPublisher");
        emailSender.setProvider("HTTP");
        emailSender.setProviderURL("https://api.example.com/send");
        emailSender.setAuthType("NONE");
        // body intentionally not set in properties
        emailSender.setProperties(new HashMap<>());

        notificationSenderManagementService.addEmailSender(emailSender);
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testAddHttpEmailSenderFailBasicAuthMissingPassword() throws Exception {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setName("HttpEmailPublisher");
        emailSender.setProvider("HTTP");
        emailSender.setProviderURL("https://api.example.com/send");
        emailSender.setAuthType("BASIC");
        Map<String, String> properties = new HashMap<>();
        properties.put("body", "{\"to\":\"{{to}}\",\"subject\":\"{{subject}}\",\"body\":\"{{body}}\"}");
        properties.put("userName", "user1");
        // password intentionally missing
        emailSender.setProperties(properties);

        notificationSenderManagementService.addEmailSender(emailSender);
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testAddHttpEmailSenderFailClientCredentialMissingScopes() throws Exception {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setName("HttpEmailPublisher");
        emailSender.setProvider("HTTP");
        emailSender.setProviderURL("https://api.example.com/send");
        emailSender.setAuthType("CLIENT_CREDENTIAL");
        Map<String, String> properties = new HashMap<>();
        properties.put("body", "{\"to\":\"{{to}}\",\"subject\":\"{{subject}}\",\"body\":\"{{body}}\"}");
        properties.put("clientId", "cid");
        properties.put("clientSecret", "csecret");
        properties.put("tokenEndpoint", "https://auth.example.com/token");
        // scopes intentionally missing
        emailSender.setProperties(properties);

        notificationSenderManagementService.addEmailSender(emailSender);
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testAddHttpEmailSenderFailBearerAuthMissingToken() throws Exception {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setName("HttpEmailPublisher");
        emailSender.setProvider("HTTP");
        emailSender.setProviderURL("https://api.example.com/send");
        emailSender.setAuthType("BEARER");
        Map<String, String> properties = new HashMap<>();
        properties.put("body", "{\"to\":\"{{to}}\",\"subject\":\"{{subject}}\",\"body\":\"{{body}}\"}");
        // accessToken intentionally missing
        emailSender.setProperties(properties);

        notificationSenderManagementService.addEmailSender(emailSender);
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testAddHttpEmailSenderFailApiKeyMissingHeader() throws Exception {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setName("HttpEmailPublisher");
        emailSender.setProvider("HTTP");
        emailSender.setProviderURL("https://api.example.com/send");
        emailSender.setAuthType("API_KEY");
        Map<String, String> properties = new HashMap<>();
        properties.put("body", "{\"to\":\"{{to}}\",\"subject\":\"{{subject}}\",\"body\":\"{{body}}\"}");
        properties.put("apiKeyValue", "key-value");
        // apiKeyHeader intentionally missing
        emailSender.setProperties(properties);

        notificationSenderManagementService.addEmailSender(emailSender);
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testAddHttpEmailSenderFailUnsupportedAuthType() throws Exception {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setName("HttpEmailPublisher");
        emailSender.setProvider("HTTP");
        emailSender.setProviderURL("https://api.example.com/send");
        emailSender.setAuthType("UNSUPPORTED_TYPE");
        Map<String, String> properties = new HashMap<>();
        properties.put("body", "{\"to\":\"{{to}}\",\"subject\":\"{{subject}}\",\"body\":\"{{body}}\"}");
        emailSender.setProperties(properties);

        notificationSenderManagementService.addEmailSender(emailSender);
    }

    @Test
    public void testGetHttpEmailSenderWithBasicAuth() throws Exception {

        String name = "HttpEmailPublisher";
        setupHttpEmailSenderMocks(name);

        Resource resource = constructHttpEmailSenderResource(name, "BASIC");
        resource.getAttributes().add(new Attribute("userName", "encrypted-ref"));
        resource.getAttributes().add(new Attribute("password", "encrypted-ref"));
        when(configurationManager.getResource(anyString(), anyString())).thenReturn(resource);

        EmailSenderDTO result = notificationSenderManagementService.getEmailSender(name);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getName(), name);
        Assert.assertEquals(result.getProvider(), "HTTP");
        Assert.assertEquals(result.getProviderURL(), "https://api.email-provider.com/send");
        Assert.assertEquals(result.getAuthType(), "BASIC");
        // Username/password should be decrypted via SecretResolveManager mock.
        Assert.assertEquals(result.getUsername(), "decrypted-value");
        Assert.assertEquals(result.getPassword(), "decrypted-value");
    }

    @Test
    public void testGetHttpEmailSenderWithClientCredentialAuth() throws Exception {

        String name = "HttpEmailPublisher";
        setupHttpEmailSenderMocks(name);

        Resource resource = constructHttpEmailSenderResource(name, "CLIENT_CREDENTIAL");
        resource.getAttributes().add(new Attribute("clientId", "encrypted-ref"));
        resource.getAttributes().add(new Attribute("clientSecret", "encrypted-ref"));
        resource.getAttributes().add(new Attribute("tokenEndpoint", "https://auth.example.com/token"));
        resource.getAttributes().add(new Attribute("scopes", "email.send"));
        when(configurationManager.getResource(anyString(), anyString())).thenReturn(resource);

        EmailSenderDTO result = notificationSenderManagementService.getEmailSender(name);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getAuthType(), "CLIENT_CREDENTIAL");
        // clientId is decrypted; clientSecret is skipped in the response.
        Assert.assertEquals(result.getProperties().get("clientId"), "decrypted-value");
        Assert.assertNull(result.getProperties().get("clientSecret"));
        Assert.assertEquals(result.getProperties().get("tokenEndpoint"), "https://auth.example.com/token");
        Assert.assertEquals(result.getProperties().get("scopes"), "email.send");
    }

    @Test
    public void testGetHttpEmailSenderWithApiKeyAuth() throws Exception {

        String name = "HttpEmailPublisher";
        setupHttpEmailSenderMocks(name);

        Resource resource = constructHttpEmailSenderResource(name, "API_KEY");
        resource.getAttributes().add(new Attribute("apiKeyHeader", "X-API-Key"));
        resource.getAttributes().add(new Attribute("apiKeyValue", "encrypted-ref"));
        when(configurationManager.getResource(anyString(), anyString())).thenReturn(resource);

        EmailSenderDTO result = notificationSenderManagementService.getEmailSender(name);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getAuthType(), "API_KEY");
        Assert.assertEquals(result.getProperties().get("apiKeyHeader"), "X-API-Key");
        // apiKeyValue is skipped in the response (sensitive).
        Assert.assertNull(result.getProperties().get("apiKeyValue"));
    }

    @Test
    public void testGetHttpEmailSenderWithNoneAuth() throws Exception {

        String name = "HttpEmailPublisher";
        setupHttpEmailSenderMocks(name);

        Resource resource = constructHttpEmailSenderResource(name, "NONE");
        when(configurationManager.getResource(anyString(), anyString())).thenReturn(resource);

        EmailSenderDTO result = notificationSenderManagementService.getEmailSender(name);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getAuthType(), "NONE");
        Assert.assertEquals(result.getFromAddress(), "noreply@example.com");
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testGetHttpEmailSenderFailNotFound() throws Exception {

        when(configurationManager.getResource(anyString(), anyString())).thenReturn(null);
        notificationSenderManagementService.getEmailSender("NonExistentPublisher");
    }

    @Test
    public void testUpdateHttpEmailSenderSameAuthType() throws Exception {

        String name = "HttpEmailPublisher";
        setupHttpEmailSenderMocks(name);

        // Existing resource with BASIC auth.
        Resource existingResource = constructHttpEmailSenderResource(name, "BASIC");
        when(configurationManager.getResource(anyString(), anyString())).thenReturn(existingResource);

        // Updated sender still with BASIC auth.
        Map<String, String> authProps = new HashMap<>();
        authProps.put("userName", "updatedUser");
        authProps.put("password", "updatedPass");
        EmailSenderDTO emailSender = constructHttpEmailSender(name, "BASIC", authProps);

        Resource replacedResource = constructHttpEmailSenderResource(name, "BASIC");
        replacedResource.getAttributes().add(new Attribute("userName", "encrypted-ref"));
        replacedResource.getAttributes().add(new Attribute("password", "encrypted-ref"));
        when(configurationManager.replaceResource(anyString(), any(Resource.class))).thenReturn(replacedResource);

        EmailSenderDTO result = notificationSenderManagementService.updateEmailSender(emailSender, true);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getAuthType(), "BASIC");
        verify(configurationManager).replaceResource(anyString(), any(Resource.class));
    }

    @Test
    public void testUpdateHttpEmailSenderChangeAuthTypeFromBasicToBearer() throws Exception {

        String name = "HttpEmailPublisher";
        setupHttpEmailSenderMocks(name);

        // Existing resource with BASIC auth.
        Resource existingResource = constructHttpEmailSenderResource(name, "BASIC");
        when(configurationManager.getResource(anyString(), anyString())).thenReturn(existingResource);

        // Updated sender with BEARER auth.
        Map<String, String> authProps = new HashMap<>();
        authProps.put("accessToken", "new-bearer-token");
        EmailSenderDTO emailSender = constructHttpEmailSender(name, "BEARER", authProps);

        Resource replacedResource = constructHttpEmailSenderResource(name, "BEARER");
        when(configurationManager.replaceResource(anyString(), any(Resource.class))).thenReturn(replacedResource);

        EmailSenderDTO result = notificationSenderManagementService.updateEmailSender(emailSender, true);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getAuthType(), "BEARER");
        verify(configurationManager).replaceResource(anyString(), any(Resource.class));
    }

    @Test
    public void testUpdateHttpEmailSenderChangeAuthTypeFromBearerToApiKey() throws Exception {

        String name = "HttpEmailPublisher";
        setupHttpEmailSenderMocks(name);

        Resource existingResource = constructHttpEmailSenderResource(name, "BEARER");
        when(configurationManager.getResource(anyString(), anyString())).thenReturn(existingResource);

        Map<String, String> authProps = new HashMap<>();
        authProps.put("apiKeyHeader", "X-API-Key");
        authProps.put("apiKeyValue", "new-api-key");
        EmailSenderDTO emailSender = constructHttpEmailSender(name, "API_KEY", authProps);

        Resource replacedResource = constructHttpEmailSenderResource(name, "API_KEY");
        replacedResource.getAttributes().add(new Attribute("apiKeyHeader", "X-API-Key"));
        when(configurationManager.replaceResource(anyString(), any(Resource.class))).thenReturn(replacedResource);

        EmailSenderDTO result = notificationSenderManagementService.updateEmailSender(emailSender, true);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getAuthType(), "API_KEY");
    }

    @Test
    public void testUpdateHttpEmailSenderChangeAuthTypeToNone() throws Exception {

        String name = "HttpEmailPublisher";
        setupHttpEmailSenderMocks(name);

        Resource existingResource = constructHttpEmailSenderResource(name, "API_KEY");
        when(configurationManager.getResource(anyString(), anyString())).thenReturn(existingResource);

        EmailSenderDTO emailSender = constructHttpEmailSender(name, "NONE", null);

        Resource replacedResource = constructHttpEmailSenderResource(name, "NONE");
        when(configurationManager.replaceResource(anyString(), any(Resource.class))).thenReturn(replacedResource);

        EmailSenderDTO result = notificationSenderManagementService.updateEmailSender(emailSender, true);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getAuthType(), "NONE");
    }

    @Test
    public void testUpdateHttpEmailSenderChangeAuthTypeToClientCredential() throws Exception {

        String name = "HttpEmailPublisher";
        setupHttpEmailSenderMocks(name);

        Resource existingResource = constructHttpEmailSenderResource(name, "NONE");
        when(configurationManager.getResource(anyString(), anyString())).thenReturn(existingResource);

        Map<String, String> authProps = new HashMap<>();
        authProps.put("clientId", "cid");
        authProps.put("clientSecret", "csecret");
        authProps.put("tokenEndpoint", "https://auth.example.com/token");
        authProps.put("scopes", "email.send");
        EmailSenderDTO emailSender = constructHttpEmailSender(name, "CLIENT_CREDENTIAL", authProps);

        Resource replacedResource = constructHttpEmailSenderResource(name, "CLIENT_CREDENTIAL");
        replacedResource.getAttributes().add(new Attribute("clientId", "encrypted-ref"));
        replacedResource.getAttributes().add(new Attribute("tokenEndpoint", "https://auth.example.com/token"));
        replacedResource.getAttributes().add(new Attribute("scopes", "email.send"));
        when(configurationManager.replaceResource(anyString(), any(Resource.class))).thenReturn(replacedResource);

        EmailSenderDTO result = notificationSenderManagementService.updateEmailSender(emailSender, true);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getAuthType(), "CLIENT_CREDENTIAL");
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testUpdateHttpEmailSenderFailNotFound() throws Exception {

        when(configurationManager.getResource(anyString(), anyString())).thenReturn(null);

        Map<String, String> authProps = new HashMap<>();
        authProps.put("userName", "user1");
        authProps.put("password", "pass1");
        EmailSenderDTO emailSender = constructHttpEmailSender("NonExistentPublisher", "BASIC", authProps);

        notificationSenderManagementService.updateEmailSender(emailSender, true);
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testUpdateHttpEmailSenderFailInvalidBasicAuth() throws Exception {

        String name = "HttpEmailPublisher";

        Resource existingResource = constructHttpEmailSenderResource(name, "BASIC");
        when(configurationManager.getResource(anyString(), anyString())).thenReturn(existingResource);

        // Missing password for BASIC auth (with validation enabled).
        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setName(name);
        emailSender.setProvider("HTTP");
        emailSender.setProviderURL("https://api.example.com/send");
        emailSender.setAuthType("BASIC");
        Map<String, String> properties = new HashMap<>();
        properties.put("body", "{\"to\":\"{{to}}\",\"subject\":\"{{subject}}\",\"body\":\"{{body}}\"}");
        properties.put("userName", "user1");
        // password missing
        emailSender.setProperties(properties);

        notificationSenderManagementService.updateEmailSender(emailSender);
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testUpdateHttpEmailSenderFailMissingProviderURL() throws Exception {

        String name = "HttpEmailPublisher";

        Resource existingResource = constructHttpEmailSenderResource(name, "NONE");
        when(configurationManager.getResource(anyString(), anyString())).thenReturn(existingResource);

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setName(name);
        emailSender.setProvider("HTTP");
        // providerURL missing
        emailSender.setAuthType("NONE");
        Map<String, String> properties = new HashMap<>();
        properties.put("body", "{\"to\":\"{{to}}\",\"subject\":\"{{subject}}\",\"body\":\"{{body}}\"}");
        emailSender.setProperties(properties);

        notificationSenderManagementService.updateEmailSender(emailSender);
    }

    @Test
    public void testDeleteHttpEmailSender() throws Exception {

        String name = "HttpEmailPublisher";
        setupHttpEmailSenderMocks(name);

        Resource resource = constructHttpEmailSenderResource(name, "BASIC");
        when(configurationManager.getResource(anyString(), anyString())).thenReturn(resource);
        doNothing().when(defaultChannelConfigurationHandler).deleteNotificationSender(anyString());

        notificationSenderManagementService.deleteNotificationSender(name);

        verify(defaultChannelConfigurationHandler).deleteNotificationSender(name);
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testDeleteHttpEmailSenderFailNotFound() throws Exception {

        when(configurationManager.getResource(anyString(), anyString())).thenReturn(null);
        notificationSenderManagementService.deleteNotificationSender("NonExistentPublisher");
    }

    @Test
    public void testValidateInputsHttpProviderWithNoneAuth() throws NotificationSenderManagementClientException {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setProvider("HTTP");
        emailSender.setProviderURL("https://api.example.com/send");
        emailSender.setAuthType("NONE");
        Map<String, String> properties = new HashMap<>();
        properties.put("body", "{\"to\":\"{{to}}\",\"subject\":\"{{subject}}\",\"body\":\"{{body}}\"}");
        emailSender.setProperties(properties);

        notificationSenderManagementService.validateInputs(emailSender);
    }

    @Test
    public void testValidateInputsHttpProviderWithBearerAuth() throws NotificationSenderManagementClientException {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setProvider("HTTP");
        emailSender.setProviderURL("https://api.example.com/send");
        emailSender.setAuthType("BEARER");
        Map<String, String> properties = new HashMap<>();
        properties.put("body", "{\"to\":\"{{to}}\",\"subject\":\"{{subject}}\",\"body\":\"{{body}}\"}");
        properties.put("accessToken", "my-token");
        emailSender.setProperties(properties);

        notificationSenderManagementService.validateInputs(emailSender);
    }

    @Test
    public void testValidateInputsHttpProviderWithApiKeyAuth() throws NotificationSenderManagementClientException {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setProvider("HTTP");
        emailSender.setProviderURL("https://api.example.com/send");
        emailSender.setAuthType("API_KEY");
        Map<String, String> properties = new HashMap<>();
        properties.put("body", "{\"to\":\"{{to}}\",\"subject\":\"{{subject}}\",\"body\":\"{{body}}\"}");
        properties.put("apiKeyHeader", "X-API-Key");
        properties.put("apiKeyValue", "my-api-key");
        emailSender.setProperties(properties);

        notificationSenderManagementService.validateInputs(emailSender);
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testValidateInputsHttpProviderFailApiKeyMissingValue()
            throws NotificationSenderManagementClientException {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setProvider("HTTP");
        emailSender.setProviderURL("https://api.example.com/send");
        emailSender.setAuthType("API_KEY");
        Map<String, String> properties = new HashMap<>();
        properties.put("body", "{\"to\":\"{{to}}\",\"subject\":\"{{subject}}\",\"body\":\"{{body}}\"}");
        properties.put("apiKeyHeader", "X-API-Key");
        // apiKeyValue missing
        emailSender.setProperties(properties);

        notificationSenderManagementService.validateInputs(emailSender);
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testValidateInputsHttpProviderFailBearerMissingToken()
            throws NotificationSenderManagementClientException {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setProvider("HTTP");
        emailSender.setProviderURL("https://api.example.com/send");
        emailSender.setAuthType("BEARER");
        Map<String, String> properties = new HashMap<>();
        properties.put("body", "{\"to\":\"{{to}}\",\"subject\":\"{{subject}}\",\"body\":\"{{body}}\"}");
        // accessToken missing
        emailSender.setProperties(properties);

        notificationSenderManagementService.validateInputs(emailSender);
    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class)
    public void testValidateInputsHttpProviderFailClientCredentialMissingTokenEndpoint()
            throws NotificationSenderManagementClientException {

        EmailSenderDTO emailSender = new EmailSenderDTO();
        emailSender.setProvider("HTTP");
        emailSender.setProviderURL("https://api.example.com/send");
        emailSender.setAuthType("CLIENT_CREDENTIAL");
        Map<String, String> properties = new HashMap<>();
        properties.put("body", "{\"to\":\"{{to}}\",\"subject\":\"{{subject}}\",\"body\":\"{{body}}\"}");
        properties.put("clientId", "cid");
        properties.put("clientSecret", "csecret");
        // tokenEndpoint missing
        properties.put("scopes", "email.send");
        emailSender.setProperties(properties);

        notificationSenderManagementService.validateInputs(emailSender);
    }
}
