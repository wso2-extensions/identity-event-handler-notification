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
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
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
}
