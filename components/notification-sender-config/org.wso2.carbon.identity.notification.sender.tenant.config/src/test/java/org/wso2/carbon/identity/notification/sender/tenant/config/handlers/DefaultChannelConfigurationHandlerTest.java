package org.wso2.carbon.identity.notification.sender.tenant.config.handlers;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.ClusteringMessage;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.email.mgt.SMSProviderPayloadTemplateManager;
import org.wso2.carbon.email.mgt.model.SMSProviderTemplate;
import org.wso2.carbon.event.publisher.core.EventPublisherService;
import org.wso2.carbon.event.publisher.core.config.EventPublisherConfiguration;
import org.wso2.carbon.event.publisher.core.exception.EventPublisherConfigurationException;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementClientException;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementException;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementServerException;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.configuration.mgt.core.model.ResourceFile;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.SMSSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementException;
import org.wso2.carbon.identity.notification.sender.tenant.config.internal.NotificationSenderTenantConfigDataHolder;
import org.wso2.carbon.identity.notification.sender.tenant.config.utils.NotificationSenderUtils;
import org.wso2.carbon.identity.tenant.resource.manager.core.ResourceManager;
import org.wso2.carbon.identity.tenant.resource.manager.exception.TenantResourceManagementClientException;
import org.wso2.carbon.identity.tenant.resource.manager.exception.TenantResourceManagementException;
import org.wso2.carbon.identity.tenant.resource.manager.exception.TenantResourceManagementServerException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertThrows;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.DEFAULT_HANDLER_NAME;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PUBLISHER_RESOURCE_TYPE;

/**
 * Unit tests for {@link DefaultChannelConfigurationHandler}.
 */
public class DefaultChannelConfigurationHandlerTest {

    private DefaultChannelConfigurationHandler defaultChannelConfigurationHandler;
    @Mock
    private SMSProviderPayloadTemplateManager smsProviderPayloadTemplateManager;
    @Mock
    private ConfigurationManager configurationManager;
    @Mock
    private EventPublisherService carbonEventPublisherService;
    @Mock
    private ClusteringAgent clusteringAgent;
    @Mock
    private ResourceManager resourceManager;

    private MockedStatic<NotificationSenderUtils> notificationSenderUtilsStatic;
    private MockedStatic<PrivilegedCarbonContext> privilegedCarbonContextStatic;

    @BeforeMethod
    public void setUp() {

        setCarbonHome();
        initMocks(this);
        privilegedCarbonContextStatic = mockStatic(PrivilegedCarbonContext.class);
        setCarbonContextForTenant(privilegedCarbonContextStatic);
        notificationSenderUtilsStatic = mockStatic(NotificationSenderUtils.class);
        defaultChannelConfigurationHandler = new DefaultChannelConfigurationHandler();

        NotificationSenderTenantConfigDataHolder.getInstance()
                .setSMSProviderPayloadTemplateManager(smsProviderPayloadTemplateManager);
        NotificationSenderTenantConfigDataHolder.getInstance()
                .setConfigurationManager(configurationManager);
        NotificationSenderTenantConfigDataHolder.getInstance()
                .setCarbonEventPublisherService(carbonEventPublisherService);
        NotificationSenderTenantConfigDataHolder.getInstance()
                .setClusteringAgent(clusteringAgent);
        NotificationSenderTenantConfigDataHolder.getInstance()
                .setResourceManager(resourceManager);
    }
    
    @AfterMethod
    public void tearDown() {

        notificationSenderUtilsStatic.close();
        privilegedCarbonContextStatic.close();
    }

    @Test
    public void testGetName() {

        Assert.assertEquals(defaultChannelConfigurationHandler.getName(), DEFAULT_HANDLER_NAME);
    }

    @Test(dataProvider = "addSMSSenderDataProvider")
    public void testAddSMSSender(String name, String providerName, String inlineBodyProperty, String providerUrl)
            throws Exception {

        SMSSenderDTO smsSenderDTO = constructSMSSenderDTO(name, providerName, inlineBodyProperty, providerUrl);
        SMSProviderTemplate smsProviderTemplate = constructSMSProviderTemplate();

        List<EventPublisherConfiguration> eventPublisherConfigurationList =
                new ArrayList<>();
        eventPublisherConfigurationList.add(constructEventPublisherConfiguration("SMSPublisher"));

        when(smsProviderPayloadTemplateManager
                .getSMSProviderPayloadTemplateByProvider(smsSenderDTO.getProvider()))
                .thenReturn(smsProviderTemplate);
        when(configurationManager.getResource(PUBLISHER_RESOURCE_TYPE, "SMSPublisher"))
                .thenReturn(null);
        when(carbonEventPublisherService.getAllActiveEventPublisherConfigurations())
                .thenReturn(eventPublisherConfigurationList);

        InputStream inputStream = constructInputStream();

        notificationSenderUtilsStatic.when(() -> NotificationSenderUtils.generateSMSPublisher(any(SMSSenderDTO.class)))
                .thenReturn(inputStream);

        Resource resource = constructResource(inputStream, smsSenderDTO);

        when(configurationManager.addResource(Mockito.eq(PUBLISHER_RESOURCE_TYPE), any(Resource.class)))
                .thenReturn(resource);

        doNothing().when(resourceManager).addEventPublisherConfiguration(any(ResourceFile.class));
        when(clusteringAgent.sendMessage(any(ClusteringMessage.class), anyBoolean())).thenReturn(new ArrayList<>());

        notificationSenderUtilsStatic.when(() -> NotificationSenderUtils.buildSmsSenderFromResource(any()))
                .thenReturn(smsSenderDTO);

        SMSSenderDTO response = defaultChannelConfigurationHandler.addSMSSender(smsSenderDTO);

        Assert.assertEquals(response.getName(), smsSenderDTO.getName());
        Assert.assertEquals(response.getProvider(), smsSenderDTO.getProvider());

    }

    @DataProvider(name = "addSMSSenderDataProvider")
    public Object[][] provideSMSSenderDTOData() {

        return new Object[][]{
//                 name, provider, providerURL, contentType
                {null, "Twilio", null,
                        "https://api.twilio.com/2010-04-01/Accounts/AC247e7b734c1e2dc380b9fa8fb444762d/Messages.json"},
                {"SMSPublisher", "Twilio", null,
                        "https://api.twilio.com/2010-04-01/Accounts/AC247e7b734c1e2dc380b9fa8fb444762d/Messages.json"},
                {null, "Twilio", "Your one-time password for the {{application-name}} is {{otpToken}}. This expires in "
                        + "{{otp-expiry-time}} minutes",
                        "https://api.twilio.com/2010-04-01/Accounts/AC247e7b734c1e2dc380b9fa8fb444762d/Messages.json"}

        };
    }

    @Test(dataProvider = "addSMSSenderDataProviderForException")
    public void testAddSMSSenderExceptions(Object smsSenderDTO, Object smsProviderTemplate,
                                           Object eventPublisherConfigurationList,
                                           Object inputStream, Object addedResource,
                                           Object existingSMSPublisherResource) throws Exception {

        when(smsProviderPayloadTemplateManager
                .getSMSProviderPayloadTemplateByProvider(((SMSSenderDTO) smsSenderDTO).getProvider()))
                .thenReturn((SMSProviderTemplate) smsProviderTemplate);
        when(configurationManager.getResource(PUBLISHER_RESOURCE_TYPE, "SMSPublisher"))
                .thenReturn((Resource) existingSMSPublisherResource);
        when(carbonEventPublisherService.getAllActiveEventPublisherConfigurations())
                .thenReturn((List<EventPublisherConfiguration>) eventPublisherConfigurationList);

        notificationSenderUtilsStatic.when(() -> NotificationSenderUtils.generateSMSPublisher(any(SMSSenderDTO.class)))
                .thenReturn((InputStream) inputStream);

        when(configurationManager.addResource(Mockito.eq(PUBLISHER_RESOURCE_TYPE), any(Resource.class)))
                .thenReturn((Resource) addedResource);

        doNothing().when(resourceManager).addEventPublisherConfiguration(any(ResourceFile.class));
        when(clusteringAgent.sendMessage(any(ClusteringMessage.class), anyBoolean())).thenReturn(new ArrayList<>());

        assertThrows(NotificationSenderManagementException.class,
                () -> defaultChannelConfigurationHandler.addSMSSender((SMSSenderDTO) smsSenderDTO));
    }

    @DataProvider(name = "addSMSSenderDataProviderForException")
    public Object[][] provideSMSSenderDTOExceptions() {

        SMSProviderTemplate smsProviderTemplate = constructSMSProviderTemplate();
        List<EventPublisherConfiguration> eventPublisherConfigurationList = new ArrayList<>();
        eventPublisherConfigurationList.add(constructEventPublisherConfiguration("SMSPublisher"));
        InputStream inputStream = constructInputStream();

        SMSSenderDTO smsSenderDTO1 = constructSMSSenderDTO("SMSPublisher",
                "Twilio",
                "Your one-time password for the {{application-name}} is {{otpToken}}. This expires in "
                        + "{{otp-expiry-time}} minutes",
                "https://api.twilio.com/2010-04-01/Accounts/AC247e7b734c1e2dc380b9fa8fb444762d/Messages.json");

        Resource addedResource1 = constructResource(inputStream, smsSenderDTO1);

        SMSSenderDTO smsSenderDTO2 = constructSMSSenderDTO("SMSPublisher",
                null,
                "Your one-time password for the {{application-name}} is {{otpToken}}. This expires in "
                        + "{{otp-expiry-time}} minutes",
                "https://api.twilio.com/2010-04-01/Accounts/AC247e7b734c1e2dc380b9fa8fb444762d/Messages.json");

        Resource addedResource2 = constructResource(inputStream, smsSenderDTO2);

        SMSSenderDTO smsSenderDTO3 = constructSMSSenderDTO("SMSPublisher",
                "Twilio",
                "Your one-time password for the {{application-name}} is {{otpToken}}. This expires in "
                        + "{{otp-expiry-time}} minutes",
                null);

        Resource addedResource3 = constructResource(inputStream, smsSenderDTO3);

        SMSSenderDTO smsSenderDTO4 = constructSMSSenderDTO("SMSPublisher",
                "Twilio",
                null,
                null);

        Resource addedResource4 = constructResource(inputStream, smsSenderDTO4);

//        smsSenderDTO, smsProviderTemplate, eventPublisherConfigurationList, inputStream, addedResource,
//        existingSMSPublisherResource
        return new Object[][]{
                {
                        smsSenderDTO1,
                        smsProviderTemplate,
                        eventPublisherConfigurationList,
                        inputStream,
                        addedResource1,
                        new Resource()
                },
                {
                        smsSenderDTO2,
                        smsProviderTemplate,
                        eventPublisherConfigurationList,
                        inputStream,
                        addedResource2,
                        null
                },
                {
                        smsSenderDTO1,
                        smsProviderTemplate,
                        null,
                        inputStream,
                        addedResource1,
                        null
                },
                {
                        smsSenderDTO4,
                        null,
                        eventPublisherConfigurationList,
                        inputStream,
                        addedResource4,
                        null
                },
                {
                        smsSenderDTO1,
                        smsProviderTemplate,
                        new ArrayList<>(),
                        inputStream,
                        addedResource1,
                        null
                },
        };
    }

    @Test
    public void testAddSMSSenderConfigurationManagementException() throws ConfigurationManagementException,
            EventPublisherConfigurationException, NotificationSenderManagementException {

        SMSSenderDTO smsSenderDTO = constructSMSSenderDTO("SMSPublisher",
                "Twilio",
                "Your one-time password for the {{application-name}} is {{otpToken}}. This expires in "
                        + "{{otp-expiry-time}} minutes",
                "https://api.twilio.com/2010-04-01/Accounts/AC247e7b734c1e2dc380b9fa8fb444762d/Messages.json");

        List<EventPublisherConfiguration> eventPublisherConfigurationList = new ArrayList<>();
        eventPublisherConfigurationList.add(constructEventPublisherConfiguration("SMSPublisher"));

        when(configurationManager.getResource(PUBLISHER_RESOURCE_TYPE, "SMSPublisher"))
                .thenReturn(null);
        when(carbonEventPublisherService.getAllActiveEventPublisherConfigurations())
                .thenReturn(eventPublisherConfigurationList);

        doThrow(ConfigurationManagementException.class).when(configurationManager)
                .addResource(Mockito.eq(PUBLISHER_RESOURCE_TYPE), any(Resource.class));

        assertThrows(NotificationSenderManagementException.class,
                () -> defaultChannelConfigurationHandler.addSMSSender(smsSenderDTO));
    }

    @Test
    public void testAddSMSSenderEventPublisherConfigurationException() throws ConfigurationManagementException,
            EventPublisherConfigurationException, NotificationSenderManagementException {

        SMSSenderDTO smsSenderDTO = constructSMSSenderDTO("SMSPublisher",
                "Twilio",
                "Your one-time password for the {{application-name}} is {{otpToken}}. This expires in "
                        + "{{otp-expiry-time}} minutes",
                "https://api.twilio.com/2010-04-01/Accounts/AC247e7b734c1e2dc380b9fa8fb444762d/Messages.json");

        when(configurationManager.getResource(PUBLISHER_RESOURCE_TYPE, "SMSPublisher"))
                .thenReturn(null);
        doThrow(EventPublisherConfigurationException.class).when(carbonEventPublisherService)
                .getAllActiveEventPublisherConfigurations();

        assertThrows(NotificationSenderManagementException.class,
                () -> defaultChannelConfigurationHandler.addSMSSender(smsSenderDTO));

    }

    @Test
    public void testDeleteNotificationSender() throws ClusteringFault, NotificationSenderManagementException {

        when(clusteringAgent.sendMessage(any(ClusteringMessage.class), anyBoolean())).thenReturn(new ArrayList<>());
        defaultChannelConfigurationHandler.deleteNotificationSender("SMSPublisher");
    }

    @Test
    public void testDeleteNotificationSenderTenantResourceManagementClientException() throws
            NotificationSenderManagementException, TenantResourceManagementException {

        doThrow(TenantResourceManagementClientException.class).when(resourceManager)
                .removeEventPublisherConfiguration(PUBLISHER_RESOURCE_TYPE, "SMSPublisher");
        assertThrows(NotificationSenderManagementException.class,
                () -> defaultChannelConfigurationHandler.deleteNotificationSender("SMSPublisher"));
    }

    @Test
    public void testDeleteNotificationSenderTenantResourceManagementServerException() throws
            NotificationSenderManagementException, TenantResourceManagementException {

        doThrow(TenantResourceManagementServerException.class).when(resourceManager)
                .removeEventPublisherConfiguration(PUBLISHER_RESOURCE_TYPE, "SMSPublisher");
        assertThrows(NotificationSenderManagementException.class,
                () -> defaultChannelConfigurationHandler.deleteNotificationSender("SMSPublisher"));
    }

    @Test
    public void testDeleteNotificationSenderTenantManagementServerException() throws TenantResourceManagementException {

        doThrow(TenantResourceManagementException.class).when(resourceManager)
                .removeEventPublisherConfiguration(PUBLISHER_RESOURCE_TYPE, "SMSPublisher");
        assertThrows(NotificationSenderManagementException.class,
                () -> defaultChannelConfigurationHandler.deleteNotificationSender("SMSPublisher"));
    }

    @Test
    public void testDeleteNotificationSenderConfigurationManagementClientException() throws
            NotificationSenderManagementException, ConfigurationManagementException {

        doThrow(ConfigurationManagementClientException.class).when(configurationManager)
                .deleteResource(PUBLISHER_RESOURCE_TYPE, "SMSPublisher");
        assertThrows(NotificationSenderManagementException.class,
                () -> defaultChannelConfigurationHandler.deleteNotificationSender("SMSPublisher"));
    }

    @Test
    public void testDeleteNotificationSenderConfigurationManagementServerException() throws
            NotificationSenderManagementException, ConfigurationManagementException {

        doThrow(ConfigurationManagementServerException.class).when(configurationManager)
                .deleteResource(PUBLISHER_RESOURCE_TYPE, "SMSPublisher");
        assertThrows(NotificationSenderManagementException.class,
                () -> defaultChannelConfigurationHandler.deleteNotificationSender("SMSPublisher"));
    }

    @Test
    public void testUpdateSMSSender() throws EventPublisherConfigurationException, ConfigurationManagementException,
            TenantResourceManagementException, ClusteringFault, NotificationSenderManagementException {

        SMSSenderDTO smsSenderDTORequestObject = constructSMSSenderDTO(
                "SMSPublisher",
                "Twilio",
                "Your one-time password for the {{application-name}} is {{otpToken}}. This expires in "
                        + "{{otp-expiry-time}} minutes",
                "https://api.twilio.com/2010-04-01/Accounts/AC247e7b734c1e2dc380b9fa8fb444762d/Messages.json"
                                                                      );

        List<EventPublisherConfiguration> eventPublisherConfigurationList =
                new ArrayList<>();
        eventPublisherConfigurationList.add(constructEventPublisherConfiguration("SMSPublisher"));

        when(carbonEventPublisherService.getAllActiveEventPublisherConfigurations())
                .thenReturn(eventPublisherConfigurationList);
        when(configurationManager.replaceResource(Mockito.eq(PUBLISHER_RESOURCE_TYPE), any(Resource.class)))
                .thenReturn(new Resource());
        doNothing().when(resourceManager).addEventPublisherConfiguration(any(ResourceFile.class));
        when(clusteringAgent.sendMessage(any(ClusteringMessage.class), anyBoolean())).thenReturn(new ArrayList<>());

        notificationSenderUtilsStatic.when(() -> NotificationSenderUtils.buildSmsSenderFromResource(any()))
                .thenReturn(smsSenderDTORequestObject);

        SMSSenderDTO smsSenderDTOResponseObject = defaultChannelConfigurationHandler
                .updateSMSSender(smsSenderDTORequestObject);

        Assert.assertEquals(smsSenderDTOResponseObject.getName(), smsSenderDTORequestObject.getName());
        Assert.assertEquals(smsSenderDTOResponseObject.getProvider(), smsSenderDTORequestObject.getProvider());
    }

    @Test
    public void testUpdateSMSSenderException() throws EventPublisherConfigurationException,
            ConfigurationManagementException, NotificationSenderManagementException {

        SMSSenderDTO smsSenderDTORequestObject = constructSMSSenderDTO(
                "SMSPublisher",
                "Twilio",
                "Your one-time password for the {{application-name}} is {{otpToken}}. This expires in "
                        + "{{otp-expiry-time}} minutes",
                "https://api.twilio.com/2010-04-01/Accounts/AC247e7b734c1e2dc380b9fa8fb444762d/Messages.json"

                                                                      );

        List<EventPublisherConfiguration> eventPublisherConfigurationList =
                new ArrayList<>();
        eventPublisherConfigurationList.add(constructEventPublisherConfiguration("SMSPublisher"));

        when(carbonEventPublisherService.getAllActiveEventPublisherConfigurations())
                .thenReturn(eventPublisherConfigurationList);
        doThrow(ConfigurationManagementException.class).when(configurationManager)
                .replaceResource(Mockito.eq(PUBLISHER_RESOURCE_TYPE), any(Resource.class));

        assertThrows(NotificationSenderManagementException.class,
                () -> defaultChannelConfigurationHandler.updateSMSSender(smsSenderDTORequestObject));
    }

    private SMSSenderDTO constructSMSSenderDTO(String name, String providerName, String inLineBodyProperty,
                                               String providerUrl) {

        SMSSenderDTO smsSenderDTO = new SMSSenderDTO();

        if (name != null) {
            smsSenderDTO.setName(name);
        }

        smsSenderDTO.setProvider(providerName);
        smsSenderDTO.setProviderURL(providerUrl);
        smsSenderDTO.setSender("+12536496764");
        smsSenderDTO.setContentType("FORM");

        Map<String, String> properties = new HashMap<>();
        properties.put("http.headers", "Authorization: Basic ABCD");

        if (inLineBodyProperty != null) {
            properties.put("body", inLineBodyProperty);
        }
        smsSenderDTO.setProperties(properties);

        return smsSenderDTO;

    }

    private SMSProviderTemplate constructSMSProviderTemplate() {

        SMSProviderTemplate smsProviderTemplate = new SMSProviderTemplate();
        smsProviderTemplate.setProvider("Twilio");
        smsProviderTemplate.setBody("Your one-time password for the {{application-name}} is " +
                "{{otpToken}}. This expires in {{otp-expiry-time}} minutes");
        return smsProviderTemplate;
    }

    private EventPublisherConfiguration constructEventPublisherConfiguration(String name) {

        EventPublisherConfiguration eventPublisherConfiguration = new EventPublisherConfiguration();
        eventPublisherConfiguration.setEventPublisherName(name);
        eventPublisherConfiguration.setFromStreamName("id_gov_sms_notify_stream");
        eventPublisherConfiguration.setFromStreamVersion("1.0.0");
        return eventPublisherConfiguration;
    }

    private Resource constructResource(InputStream inputStream, SMSSenderDTO smsSenderDTO) {

        Resource resource = new Resource();
        ResourceFile file = new ResourceFile();
        file.setName(smsSenderDTO.getName());
        file.setInputStream(inputStream);
        List<ResourceFile> resourceFiles = new ArrayList<>();
        resourceFiles.add(file);
        resource.setFiles(resourceFiles);

        return resource;
    }

    private InputStream constructInputStream() {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        return new ByteArrayInputStream(outputStream.toByteArray());

    }

    private void setCarbonHome() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes", "repository").
                toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, Paths.get(carbonHome, "conf").toString());
    }

    private void setCarbonContextForTenant(MockedStatic<PrivilegedCarbonContext> privilegedCarbonContextStatic) {
        
        PrivilegedCarbonContext privilegedCarbonContext = mock(PrivilegedCarbonContext.class);
        privilegedCarbonContextStatic.when(PrivilegedCarbonContext::getThreadLocalCarbonContext)
                .thenReturn(privilegedCarbonContext);
        when(privilegedCarbonContext.getTenantDomain()).thenReturn("tenant");
        when(privilegedCarbonContext.getTenantId()).thenReturn(1);
    }
}
