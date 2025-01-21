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
import org.wso2.carbon.identity.notification.push.provider.PushProvider;
import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderException;
import org.wso2.carbon.identity.notification.push.provider.model.PushNotificationData;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementService;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.PushSenderDTO;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PushNotificationHandlerTest {

    @InjectMocks
    private PushNotificationHandler pushNotificationHandler;

    @Mock
    private NotificationHandlerDataHolder notificationHandlerDataHolder;

    @Mock
    private OrganizationManager organizationManager;

    @Mock
    private PushProvider pushProvider;

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

            verify(pushProvider, times(1)).sendNotification(any(PushNotificationData.class), eq(pushSenderDTO),
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

            doThrow(new PushProviderException("Error")).when(pushProvider)
                    .sendNotification(any(PushNotificationData.class), any(PushSenderDTO.class), anyString());

            pushNotificationHandler.handleEvent(event);
        }
    }

}
