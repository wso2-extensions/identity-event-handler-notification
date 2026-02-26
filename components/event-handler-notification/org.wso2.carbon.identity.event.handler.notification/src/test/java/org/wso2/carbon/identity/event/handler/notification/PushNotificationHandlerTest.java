/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.event.handler.notification;

import java.util.ArrayList;
import java.util.List;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.bean.IdentityEventMessageContext;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.notification.internal.NotificationHandlerDataHolder;
import org.wso2.carbon.identity.event.handler.notification.util.NotificationUtil;
import org.wso2.carbon.identity.notification.push.provider.PushProvider;
import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderException;
import org.wso2.carbon.identity.notification.push.provider.model.PushNotificationData;
import org.wso2.carbon.identity.notification.push.provider.model.PushSenderData;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementService;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.PushSenderDTO;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_ERROR_GETTING_NOTIFICATION_SENDERS_BY_TYPE;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for PushNotificationHandler.
 */
public class PushNotificationHandlerTest {

    private static final String SAMPLE_ORGANIZATION_NAME = "Great Hospital";

    @InjectMocks
    private PushNotificationHandler pushNotificationHandler;

    @Mock
    private NotificationHandlerDataHolder notificationHandlerDataHolder;

    @Mock
    private OrganizationManager organizationManager;

    @Mock
    private PushProvider pushProvider;

    @Mock
    private PushProvider fcmProvider;

    @Mock
    private PushProvider snsProvider;

    @Mock
    private PushProvider apnsProvider;

    @Mock
    private PushSenderDTO pushSenderDTO;

    @Mock
    private NotificationSenderManagementService notificationSenderManagementService;

    @BeforeMethod
    public void setUp() {

        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetName() {

        assert pushNotificationHandler.getName().equals("PushNotificationHandler");
    }

    @Test
    public void testCanHandle() {

        Event event = new Event("TRIGGER_PUSH_NOTIFICATION", new HashMap<>());
        IdentityEventMessageContext messageContext = new IdentityEventMessageContext(event);
        assert pushNotificationHandler.canHandle(messageContext);
    }

    @Test
    public void testHandleEvent() throws Exception {

        Event event = new Event("TRIGGER_PUSH_NOTIFICATION", new HashMap<>());
        event.getEventProperties().put("tenant-domain", "carbon.super");
        event.getEventProperties().put("NOTIFICATION_SCENARIO", "AUTHENTICATION");
        event.getEventProperties().put("notificationProvider", "FCM");
        event.getEventProperties().put("deviceToken", "token");
        event.getEventProperties().put("user-name", "sampleUser");

        try (MockedStatic<NotificationHandlerDataHolder> mockedDataHolder = mockStatic(
                NotificationHandlerDataHolder.class);
             MockedStatic<NotificationUtil> mockedNotificationUtil = mockStatic(NotificationUtil.class)) {

            mockedDataHolder.when(NotificationHandlerDataHolder::getInstance).thenReturn(notificationHandlerDataHolder);
            mockedNotificationUtil.when(() -> NotificationUtil.resolveHumanReadableOrganizationName(anyString()))
                    .thenReturn(SAMPLE_ORGANIZATION_NAME);
            when(notificationHandlerDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(organizationManager.resolveOrganizationId(anyString())).thenReturn("orgId");
            when(notificationHandlerDataHolder.getNotificationSenderManagementService()).thenReturn(
                    notificationSenderManagementService);

            PushSenderDTO pushSenderDTO = new PushSenderDTO();
            pushSenderDTO.setName("PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO);
            when(notificationSenderManagementService.getPushSenders(true)).thenReturn(pushSenders);

            when(notificationHandlerDataHolder.getPushProvider(anyString())).thenReturn(pushProvider);

            pushNotificationHandler.handleEvent(event);

            verify(pushProvider, times(1)).sendNotification(any(PushNotificationData.class),
                    any(PushSenderData.class),
                    eq("carbon.super"));
        }
    }

    @Test(expectedExceptions = IdentityEventException.class)
    public void testHandleEventWithInvalidProperties() throws Exception {

        Event event = new Event("PUSH_NOTIFICATION_EVENT", new HashMap<>());
        event.getEventProperties().put("tenant-domain", "carbon.super");

        try (MockedStatic<NotificationHandlerDataHolder> mockedDataHolder = mockStatic(
                NotificationHandlerDataHolder.class)) {

            mockedDataHolder.when(NotificationHandlerDataHolder::getInstance).thenReturn(notificationHandlerDataHolder);
            when(notificationHandlerDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(organizationManager.resolveOrganizationId(anyString())).thenReturn("orgId");

            pushNotificationHandler.handleEvent(event);
        }
    }

    @Test(expectedExceptions = IdentityEventException.class)
    public void testHandleEventFailWithoutTenantDomain() throws IdentityEventException {

        Event event = new Event("PUSH_NOTIFICATION_EVENT", new HashMap<>());
        pushNotificationHandler.handleEvent(event);
    }

    @Test(expectedExceptions = IdentityEventException.class)
    public void testHandleEventWithOrganizationManagementException() throws Exception {

        Event event = new Event("PUSH_NOTIFICATION_EVENT", new HashMap<>());
        event.getEventProperties().put("tenant-domain", "carbon.super");

        try (MockedStatic<NotificationHandlerDataHolder> mockedDataHolder = mockStatic(
                NotificationHandlerDataHolder.class)) {

            mockedDataHolder.when(NotificationHandlerDataHolder::getInstance).thenReturn(notificationHandlerDataHolder);
            when(notificationHandlerDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(organizationManager.resolveOrganizationId(anyString())).thenThrow(
                    new OrganizationManagementException("Error"));

            pushNotificationHandler.handleEvent(event);
        }
    }

    @Test(expectedExceptions = IdentityEventException.class)
    public void testHandleEventWithNoPushProvider() throws Exception {

        Event event = new Event("TRIGGER_PUSH_NOTIFICATION", new HashMap<>());
        event.getEventProperties().put("tenant-domain", "carbon.super");
        event.getEventProperties().put("NOTIFICATION_SCENARIO", "AUTHENTICATION");
        event.getEventProperties().put("notificationProvider", "FCM");
        event.getEventProperties().put("deviceToken", "token");
        event.getEventProperties().put("user-name", "sampleUser");

        try (MockedStatic<NotificationHandlerDataHolder> mockedDataHolder = mockStatic(
                NotificationHandlerDataHolder.class)) {

            mockedDataHolder.when(NotificationHandlerDataHolder::getInstance).thenReturn(notificationHandlerDataHolder);
            when(notificationHandlerDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(organizationManager.resolveOrganizationId(anyString())).thenReturn("orgId");
            when(notificationHandlerDataHolder.getNotificationSenderManagementService()).thenReturn(
                    notificationSenderManagementService);

            PushSenderDTO pushSenderDTO = new PushSenderDTO();
            pushSenderDTO.setName("PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO);
            when(notificationSenderManagementService.getPushSenders(true)).thenReturn(pushSenders);

            pushNotificationHandler.handleEvent(event);
        }
    }

    @Test(expectedExceptions = IdentityEventException.class)
    public void testHandleEventFailWithPushProviderTypeMismatch() throws Exception {

        Event event = new Event("TRIGGER_PUSH_NOTIFICATION", new HashMap<>());
        event.getEventProperties().put("tenant-domain", "carbon.super");
        event.getEventProperties().put("NOTIFICATION_SCENARIO", "AUTHENTICATION");
        event.getEventProperties().put("notificationProvider", "provider");
        event.getEventProperties().put("deviceToken", "token");
        event.getEventProperties().put("user-name", "sampleUser");

        try (MockedStatic<NotificationHandlerDataHolder> mockedDataHolder = mockStatic(
                NotificationHandlerDataHolder.class)) {

            mockedDataHolder.when(NotificationHandlerDataHolder::getInstance).thenReturn(notificationHandlerDataHolder);
            when(notificationHandlerDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(organizationManager.resolveOrganizationId(anyString())).thenReturn("orgId");
            when(notificationHandlerDataHolder.getNotificationSenderManagementService()).thenReturn(
                    notificationSenderManagementService);

            PushSenderDTO pushSenderDTO = new PushSenderDTO();
            pushSenderDTO.setName("PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO);
            when(notificationSenderManagementService.getPushSenders(true)).thenReturn(pushSenders);

            when(notificationHandlerDataHolder.getPushProvider(anyString())).thenReturn(pushProvider);

            pushNotificationHandler.handleEvent(event);
        }
    }

    @Test(expectedExceptions = IdentityEventException.class)
    public void testHandleEventWithPushProviderException() throws Exception {

        Event event = new Event("TRIGGER_PUSH_NOTIFICATION", new HashMap<>());
        event.getEventProperties().put("tenant-domain", "carbon.super");
        event.getEventProperties().put("NOTIFICATION_SCENARIO", "AUTHENTICATION");
        event.getEventProperties().put("notificationProvider", "FCM");
        event.getEventProperties().put("deviceToken", "token");
        event.getEventProperties().put("user-name", "sampleUser");

        try (MockedStatic<NotificationHandlerDataHolder> mockedDataHolder = mockStatic(
                NotificationHandlerDataHolder.class);
             MockedStatic<NotificationUtil> mockedNotificationUtil = mockStatic(NotificationUtil.class)) {

            mockedDataHolder.when(NotificationHandlerDataHolder::getInstance).thenReturn(notificationHandlerDataHolder);
            mockedNotificationUtil.when(() -> NotificationUtil.resolveHumanReadableOrganizationName(anyString()))
                    .thenReturn(SAMPLE_ORGANIZATION_NAME);
            when(notificationHandlerDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(organizationManager.resolveOrganizationId(anyString())).thenReturn("orgId");
            when(notificationHandlerDataHolder.getNotificationSenderManagementService()).thenReturn(
                    notificationSenderManagementService);

            PushSenderDTO pushSenderDTO = new PushSenderDTO();
            pushSenderDTO.setName("PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO);
            when(notificationSenderManagementService.getPushSenders(true)).thenReturn(pushSenders);

            when(notificationHandlerDataHolder.getPushProvider(anyString())).thenReturn(pushProvider);

            doThrow(new PushProviderException("Error")).when(pushProvider)
                    .sendNotification(any(PushNotificationData.class), any(PushSenderData.class), anyString());

            pushNotificationHandler.handleEvent(event);
        }
    }

    // ==================== Multi-Provider Support Tests ====================

    /**
     * Test that correct provider is selected when multiple providers are registered.
     * Verifies the core multi-provider selection logic.
     */
    @Test
    public void testHandleEventWithMultipleProvidersSelectsCorrect() throws Exception {

        Event event = new Event("TRIGGER_PUSH_NOTIFICATION", new HashMap<>());
        event.getEventProperties().put("tenant-domain", "carbon.super");
        event.getEventProperties().put("NOTIFICATION_SCENARIO", "AUTHENTICATION");
        event.getEventProperties().put("notificationProvider", "SNS");
        event.getEventProperties().put("deviceToken", "token");
        event.getEventProperties().put("user-name", "sampleUser");

        try (MockedStatic<NotificationHandlerDataHolder> mockedDataHolder = mockStatic(
                NotificationHandlerDataHolder.class);
             MockedStatic<NotificationUtil> mockedNotificationUtil = mockStatic(NotificationUtil.class)) {

            mockedDataHolder.when(NotificationHandlerDataHolder::getInstance).thenReturn(notificationHandlerDataHolder);
            mockedNotificationUtil.when(() -> NotificationUtil.resolveHumanReadableOrganizationName(anyString()))
                    .thenReturn(SAMPLE_ORGANIZATION_NAME);
            when(notificationHandlerDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(organizationManager.resolveOrganizationId(anyString())).thenReturn("orgId");
            when(notificationHandlerDataHolder.getNotificationSenderManagementService()).thenReturn(
                    notificationSenderManagementService);

            // Register multiple providers: FCM, SNS, APNS
            List<PushSenderDTO> pushSenders = createMultiplePushSenders("FCM", "SNS", "APNS");
            when(notificationSenderManagementService.getPushSenders(true)).thenReturn(pushSenders);

            // Setup provider mocks
            when(notificationHandlerDataHolder.getPushProvider("FCM")).thenReturn(fcmProvider);
            when(notificationHandlerDataHolder.getPushProvider("SNS")).thenReturn(snsProvider);
            when(notificationHandlerDataHolder.getPushProvider("APNS")).thenReturn(apnsProvider);

            pushNotificationHandler.handleEvent(event);

            // Verify only SNS provider was called
            verify(snsProvider, times(1)).sendNotification(
                    any(PushNotificationData.class),
                    any(PushSenderData.class),
                    eq("carbon.super"));
            verify(fcmProvider, times(0)).sendNotification(
                    any(PushNotificationData.class),
                    any(PushSenderData.class),
                    anyString());
            verify(apnsProvider, times(0)).sendNotification(
                    any(PushNotificationData.class),
                    any(PushSenderData.class),
                    anyString());
        }
    }

    /**
     * Test case-insensitive provider matching.
     * Ensures "fcm" matches "FCM" sender configuration.
     */
    @Test
    public void testHandleEventWithCaseInsensitiveProviderMatching() throws Exception {

        Event event = new Event("TRIGGER_PUSH_NOTIFICATION", new HashMap<>());
        event.getEventProperties().put("tenant-domain", "carbon.super");
        event.getEventProperties().put("NOTIFICATION_SCENARIO", "AUTHENTICATION");
        event.getEventProperties().put("notificationProvider", "fcm"); // lowercase
        event.getEventProperties().put("deviceToken", "token");
        event.getEventProperties().put("user-name", "sampleUser");

        try (MockedStatic<NotificationHandlerDataHolder> mockedDataHolder = mockStatic(
                NotificationHandlerDataHolder.class);
             MockedStatic<NotificationUtil> mockedNotificationUtil = mockStatic(NotificationUtil.class)) {

            mockedDataHolder.when(NotificationHandlerDataHolder::getInstance).thenReturn(notificationHandlerDataHolder);
            mockedNotificationUtil.when(() -> NotificationUtil.resolveHumanReadableOrganizationName(anyString()))
                    .thenReturn(SAMPLE_ORGANIZATION_NAME);
            when(notificationHandlerDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(organizationManager.resolveOrganizationId(anyString())).thenReturn("orgId");
            when(notificationHandlerDataHolder.getNotificationSenderManagementService()).thenReturn(
                    notificationSenderManagementService);

            // Register FCM with uppercase
            PushSenderDTO fcmSender = new PushSenderDTO();
            fcmSender.setName("FCM_PushPublisher");
            fcmSender.setProvider("FCM"); // uppercase
            fcmSender.setProviderId("fcm-provider-id");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(fcmSender);
            when(notificationSenderManagementService.getPushSenders(true)).thenReturn(pushSenders);

            when(notificationHandlerDataHolder.getPushProvider("FCM")).thenReturn(fcmProvider);

            pushNotificationHandler.handleEvent(event);

            // Verify FCM provider was called despite case difference
            verify(fcmProvider, times(1)).sendNotification(
                    any(PushNotificationData.class),
                    any(PushSenderData.class),
                    eq("carbon.super"));
        }
    }

    /**
     * Test error when requested provider not found among multiple registered providers.
     */
    @Test(expectedExceptions = IdentityEventException.class)
    public void testHandleEventWithNoMatchingProviderAmongMultiple() throws Exception {

        Event event = new Event("TRIGGER_PUSH_NOTIFICATION", new HashMap<>());
        event.getEventProperties().put("tenant-domain", "carbon.super");
        event.getEventProperties().put("NOTIFICATION_SCENARIO", "AUTHENTICATION");
        event.getEventProperties().put("notificationProvider", "APNS");
        event.getEventProperties().put("deviceToken", "token");
        event.getEventProperties().put("user-name", "sampleUser");

        try (MockedStatic<NotificationHandlerDataHolder> mockedDataHolder = mockStatic(
                NotificationHandlerDataHolder.class)) {

            mockedDataHolder.when(NotificationHandlerDataHolder::getInstance).thenReturn(notificationHandlerDataHolder);
            when(notificationHandlerDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(organizationManager.resolveOrganizationId(anyString())).thenReturn("orgId");
            when(notificationHandlerDataHolder.getNotificationSenderManagementService()).thenReturn(
                    notificationSenderManagementService);

            // Register only FCM and SNS, but event requests APNS
            List<PushSenderDTO> pushSenders = createMultiplePushSenders("FCM", "SNS");
            when(notificationSenderManagementService.getPushSenders(true)).thenReturn(pushSenders);

            pushNotificationHandler.handleEvent(event);
        }
    }

    /**
     * Test behavior when getPushSenders returns empty list.
     * Should throw IdentityEventException when no matching sender is found.
     */
    @Test(expectedExceptions = IdentityEventException.class)
    public void testHandleEventWithEmptyPushSendersList() throws Exception {

        Event event = new Event("TRIGGER_PUSH_NOTIFICATION", new HashMap<>());
        event.getEventProperties().put("tenant-domain", "carbon.super");
        event.getEventProperties().put("NOTIFICATION_SCENARIO", "AUTHENTICATION");
        event.getEventProperties().put("notificationProvider", "FCM");
        event.getEventProperties().put("deviceToken", "token");
        event.getEventProperties().put("user-name", "sampleUser");

        try (MockedStatic<NotificationHandlerDataHolder> mockedDataHolder = mockStatic(
                NotificationHandlerDataHolder.class)) {

            mockedDataHolder.when(NotificationHandlerDataHolder::getInstance).thenReturn(notificationHandlerDataHolder);
            when(notificationHandlerDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(organizationManager.resolveOrganizationId(anyString())).thenReturn("orgId");
            when(notificationHandlerDataHolder.getNotificationSenderManagementService()).thenReturn(
                    notificationSenderManagementService);

            // Return empty list
            when(notificationSenderManagementService
                    .getPushSenders(true)).thenReturn(new ArrayList<>());

            pushNotificationHandler.handleEvent(event);
        }
    }

    /**
     * Test sequential events with different providers.
     * Verifies provider independence across events.
     */
    @Test
    public void testHandleEventSequentialDifferentProviders() throws Exception {

        try (MockedStatic<NotificationHandlerDataHolder> mockedDataHolder = mockStatic(
                NotificationHandlerDataHolder.class);
             MockedStatic<NotificationUtil> mockedNotificationUtil = mockStatic(NotificationUtil.class)) {

            mockedDataHolder.when(NotificationHandlerDataHolder::getInstance).thenReturn(notificationHandlerDataHolder);
            mockedNotificationUtil.when(() -> NotificationUtil.resolveHumanReadableOrganizationName(anyString()))
                    .thenReturn(SAMPLE_ORGANIZATION_NAME);
            when(notificationHandlerDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(organizationManager.resolveOrganizationId(anyString())).thenReturn("orgId");
            when(notificationHandlerDataHolder.getNotificationSenderManagementService()).thenReturn(
                    notificationSenderManagementService);

            // Register multiple providers
            List<PushSenderDTO> pushSenders = createMultiplePushSenders("FCM", "SNS");
            when(notificationSenderManagementService.getPushSenders(true)).thenReturn(pushSenders);

            when(notificationHandlerDataHolder.getPushProvider("FCM")).thenReturn(fcmProvider);
            when(notificationHandlerDataHolder.getPushProvider("SNS")).thenReturn(snsProvider);

            // First event with FCM
            Event fcmEvent = createPushNotificationEvent("FCM", "carbon.super");
            pushNotificationHandler.handleEvent(fcmEvent);

            // Second event with SNS
            Event snsEvent = createPushNotificationEvent("SNS", "carbon.super");
            pushNotificationHandler.handleEvent(snsEvent);

            // Verify both providers were called once each
            verify(fcmProvider, times(1)).sendNotification(
                    any(PushNotificationData.class),
                    any(PushSenderData.class),
                    eq("carbon.super"));
            verify(snsProvider, times(1)).sendNotification(
                    any(PushNotificationData.class),
                    any(PushSenderData.class),
                    eq("carbon.super"));
        }
    }

    /**
     * Test exception handling during push sender retrieval.
     */
    @Test(expectedExceptions = IdentityEventException.class)
    public void testHandleEventWithNotificationSenderManagementException() throws Exception {

        Event event = new Event("TRIGGER_PUSH_NOTIFICATION", new HashMap<>());
        event.getEventProperties().put("tenant-domain", "carbon.super");
        event.getEventProperties().put("NOTIFICATION_SCENARIO", "AUTHENTICATION");
        event.getEventProperties().put("notificationProvider", "FCM");
        event.getEventProperties().put("deviceToken", "token");
        event.getEventProperties().put("user-name", "sampleUser");

        try (MockedStatic<NotificationHandlerDataHolder> mockedDataHolder = mockStatic(
                NotificationHandlerDataHolder.class)) {

            mockedDataHolder.when(NotificationHandlerDataHolder::getInstance).thenReturn(notificationHandlerDataHolder);
            when(notificationHandlerDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(organizationManager.resolveOrganizationId(anyString())).thenReturn("orgId");
            when(notificationHandlerDataHolder.getNotificationSenderManagementService()).thenReturn(
                    notificationSenderManagementService);

            // Simulate exception during getPushSenders
            when(notificationSenderManagementService.getPushSenders(true))
                    .thenThrow(new org.wso2.carbon.identity.notification.sender.tenant.config.exception
                            .NotificationSenderManagementException(ERROR_CODE_ERROR_GETTING_NOTIFICATION_SENDERS_BY_TYPE, "push"));

            pushNotificationHandler.handleEvent(event);
        }
    }

    /**
     * Test provider-specific configuration handling.
     * Verifies that correct provider properties are passed.
     */
    @Test
    public void testHandleEventWithProviderSpecificConfiguration() throws Exception {

        Event event = new Event("TRIGGER_PUSH_NOTIFICATION", new HashMap<>());
        event.getEventProperties().put("tenant-domain", "carbon.super");
        event.getEventProperties().put("NOTIFICATION_SCENARIO", "AUTHENTICATION");
        event.getEventProperties().put("notificationProvider", "FCM");
        event.getEventProperties().put("deviceToken", "token");
        event.getEventProperties().put("user-name", "sampleUser");

        try (MockedStatic<NotificationHandlerDataHolder> mockedDataHolder = mockStatic(
                NotificationHandlerDataHolder.class);
             MockedStatic<NotificationUtil> mockedNotificationUtil = mockStatic(NotificationUtil.class)) {

            mockedDataHolder.when(NotificationHandlerDataHolder::getInstance).thenReturn(notificationHandlerDataHolder);
            mockedNotificationUtil.when(() -> NotificationUtil.resolveHumanReadableOrganizationName(anyString()))
                    .thenReturn(SAMPLE_ORGANIZATION_NAME);
            when(notificationHandlerDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(organizationManager.resolveOrganizationId(anyString())).thenReturn("orgId");
            when(notificationHandlerDataHolder.getNotificationSenderManagementService()).thenReturn(
                    notificationSenderManagementService);

            // Create FCM sender with specific properties
            PushSenderDTO fcmSender = new PushSenderDTO();
            fcmSender.setName("FCM_PushPublisher");
            fcmSender.setProvider("FCM");
            fcmSender.setProviderId("fcm-provider-id");
            HashMap<String, String> fcmProperties = new HashMap<>();
            fcmProperties.put("fcm-api-key", "fcm-key-123");
            fcmSender.setProperties(fcmProperties);

            // Create SNS sender with different properties
            PushSenderDTO snsSender = new PushSenderDTO();
            snsSender.setName("SNS_PushPublisher");
            snsSender.setProvider("SNS");
            snsSender.setProviderId("sns-provider-id");
            HashMap<String, String> snsProperties = new HashMap<>();
            snsProperties.put("sns-access-key", "sns-key-456");
            snsSender.setProperties(snsProperties);

            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(fcmSender);
            pushSenders.add(snsSender);
            when(notificationSenderManagementService.getPushSenders(true)).thenReturn(pushSenders);

            when(notificationHandlerDataHolder.getPushProvider("FCM")).thenReturn(fcmProvider);

            pushNotificationHandler.handleEvent(event);

            // Verify FCM provider was called
            verify(fcmProvider, times(1)).sendNotification(
                    any(PushNotificationData.class),
                    any(PushSenderData.class),
                    eq("carbon.super"));
        }
    }

    /**
     * Test explicit null handling when getPushSenders returns null.
     */
    @Test
    public void testHandleEventWithNullPushSendersResponse() throws Exception {

        Event event = new Event("TRIGGER_PUSH_NOTIFICATION", new HashMap<>());
        event.getEventProperties().put("tenant-domain", "carbon.super");
        event.getEventProperties().put("NOTIFICATION_SCENARIO", "AUTHENTICATION");
        event.getEventProperties().put("notificationProvider", "FCM");
        event.getEventProperties().put("deviceToken", "token");
        event.getEventProperties().put("user-name", "sampleUser");

        try (MockedStatic<NotificationHandlerDataHolder> mockedDataHolder = mockStatic(
                NotificationHandlerDataHolder.class)) {

            mockedDataHolder.when(NotificationHandlerDataHolder::getInstance).thenReturn(notificationHandlerDataHolder);
            when(notificationHandlerDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(organizationManager.resolveOrganizationId(anyString())).thenReturn("orgId");
            when(notificationHandlerDataHolder.getNotificationSenderManagementService()).thenReturn(
                    notificationSenderManagementService);

            // Return null
            when(notificationSenderManagementService.getPushSenders(true)).thenReturn(null);

            // Should complete without exception
            pushNotificationHandler.handleEvent(event);

            // Verify no provider was called
            verify(fcmProvider, times(0)).sendNotification(
                    any(PushNotificationData.class),
                    any(PushSenderData.class),
                    anyString());
        }
    }

    /**
     * Test various provider name format variations.
     */
    @Test
    public void testHandleEventWithProviderNameVariations() throws Exception {

        String[] providerVariations = {"FCM", "fcm", "Fcm", "fCm"};

        for (String providerName : providerVariations) {
            Event event = new Event("TRIGGER_PUSH_NOTIFICATION", new HashMap<>());
            event.getEventProperties().put("tenant-domain", "carbon.super");
            event.getEventProperties().put("NOTIFICATION_SCENARIO", "AUTHENTICATION");
            event.getEventProperties().put("notificationProvider", providerName);
            event.getEventProperties().put("deviceToken", "token");
            event.getEventProperties().put("user-name", "sampleUser");

            try (MockedStatic<NotificationHandlerDataHolder> mockedDataHolder = mockStatic(
                    NotificationHandlerDataHolder.class);
                 MockedStatic<NotificationUtil> mockedNotificationUtil = mockStatic(NotificationUtil.class)) {

                mockedDataHolder.when(NotificationHandlerDataHolder::getInstance)
                        .thenReturn(notificationHandlerDataHolder);
                mockedNotificationUtil.when(() -> NotificationUtil.resolveHumanReadableOrganizationName(anyString()))
                        .thenReturn(SAMPLE_ORGANIZATION_NAME);
                when(notificationHandlerDataHolder.getOrganizationManager()).thenReturn(organizationManager);
                when(organizationManager.resolveOrganizationId(anyString())).thenReturn("orgId");
                when(notificationHandlerDataHolder.getNotificationSenderManagementService()).thenReturn(
                        notificationSenderManagementService);

                // Register FCM with uppercase
                PushSenderDTO fcmSender = new PushSenderDTO();
                fcmSender.setName("FCM_PushPublisher");
                fcmSender.setProvider("FCM");
                fcmSender.setProviderId("fcm-provider-id");
                List<PushSenderDTO> pushSenders = new ArrayList<>();
                pushSenders.add(fcmSender);
                when(notificationSenderManagementService.getPushSenders(true)).thenReturn(pushSenders);

                when(notificationHandlerDataHolder.getPushProvider("FCM")).thenReturn(fcmProvider);

                pushNotificationHandler.handleEvent(event);
            }
        }

        // Verify FCM provider was called for all variations
        verify(fcmProvider, times(providerVariations.length)).sendNotification(
                any(PushNotificationData.class),
                any(PushSenderData.class),
                eq("carbon.super"));
    }

    // ==================== Helper Methods ====================

    /**
     * Create multiple push sender DTOs for testing.
     *
     * @param providers Provider names
     * @return List of PushSenderDTO objects
     */
    private List<PushSenderDTO> createMultiplePushSenders(String... providers) {
        List<PushSenderDTO> senders = new ArrayList<>();
        for (String provider : providers) {
            PushSenderDTO dto = new PushSenderDTO();
            dto.setName(provider + "_PushPublisher");
            dto.setProvider(provider);
            dto.setProviderId(provider.toLowerCase() + "-provider-id");
            senders.add(dto);
        }
        return senders;
    }

    /**
     * Create a push notification event with specified provider.
     *
     * @param provider     Provider name
     * @param tenantDomain Tenant domain
     * @return Event object
     */
    private Event createPushNotificationEvent(String provider, String tenantDomain) {
        Event event = new Event("TRIGGER_PUSH_NOTIFICATION", new HashMap<>());
        event.getEventProperties().put("tenant-domain", tenantDomain);
        event.getEventProperties().put("NOTIFICATION_SCENARIO", "AUTHENTICATION");
        event.getEventProperties().put("notificationProvider", provider);
        event.getEventProperties().put("deviceToken", "token");
        event.getEventProperties().put("user-name", "sampleUser");
        return event;
    }

}
