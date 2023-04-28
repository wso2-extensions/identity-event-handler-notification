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
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementException;
import org.wso2.carbon.identity.configuration.mgt.core.model.Attribute;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.SMSSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementClientException;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementException;
import org.wso2.carbon.identity.notification.sender.tenant.config.handlers.ChannelConfigurationHandler;
import org.wso2.carbon.identity.notification.sender.tenant.config.internal.NotificationSenderTenantConfigDataHolder;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.DEFAULT_HANDLER_NAME;

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
    private static final String WEB_SUB_HUB_HANDLER_NAME = "choreo";

    @BeforeMethod
    public void setup() {

        MockitoAnnotations.openMocks(this);

        setCarbonHome();
        setCarbonContextForTenant();
        notificationSenderManagementService = new NotificationSenderManagementServiceImpl();

        when(defaultChannelConfigurationHandler.getName()).thenReturn(DEFAULT_HANDLER_NAME);
        when(websubhubChannelConfigurationHandler.getName()).thenReturn(WEB_SUB_HUB_HANDLER_NAME);

        NotificationSenderTenantConfigDataHolder.getInstance()
                .registerConfigurationHandler(defaultChannelConfigurationHandler);
        NotificationSenderTenantConfigDataHolder.getInstance()
                .registerConfigurationHandler(websubhubChannelConfigurationHandler);
        NotificationSenderTenantConfigDataHolder.getInstance()
                .setConfigurationManager(configurationManager);

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
    public void testDeleteNotificationSender(Resource resource) throws ConfigurationManagementException,
            NotificationSenderManagementException {

        when(configurationManager.getResource(anyString(), anyString())).thenReturn(resource);
        doNothing().when(websubhubChannelConfigurationHandler).deleteNotificationSender(anyString());
        doNothing().when(defaultChannelConfigurationHandler).deleteNotificationSender(anyString());

        notificationSenderManagementService.deleteNotificationSender("SMSPublisher");

    }

    @Test(expectedExceptions = NotificationSenderManagementClientException.class,
            dataProvider = "deleteNotificationSenderExceptionDataProvider")
    public void testDeleteNotificationSenderException(Resource resource) throws NotificationSenderManagementException,
            ConfigurationManagementException {

        when(configurationManager.getResource(anyString(), anyString())).
                thenReturn(resource);
        notificationSenderManagementService.deleteNotificationSender("SMSPublisher");
    }

    @DataProvider(name = "deleteNotificationSenderDataProvider")
    public Object[][] provideDataForResource() {

        return new Object[][]{
                //resource
                {constructResource(DEFAULT_HANDLER_NAME, false)},
                {constructResource(WEB_SUB_HUB_HANDLER_NAME, false)},
                {constructResource(null, false)},
                {constructResource(null, true)}
        };
    }

    @DataProvider(name = "deleteNotificationSenderExceptionDataProvider")
    public Object[][] provideDataForResourceForException() {

        return new Object[][]{
                //resource
                {constructResource("dummyChannel", false)},
                {null},
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
