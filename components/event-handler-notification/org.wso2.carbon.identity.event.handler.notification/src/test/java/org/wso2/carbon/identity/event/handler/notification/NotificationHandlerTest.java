/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.event.handler.notification;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.event.output.adapter.core.exception.OutputEventAdapterException;
import org.wso2.carbon.event.stream.core.EventStreamService;
import org.wso2.carbon.event.stream.core.exception.AggregatedConsumerFailureException;
import org.wso2.carbon.event.stream.core.exception.ConsumerFailureException;
import org.wso2.carbon.event.stream.core.exception.EventStreamException;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.core.circuitbreaker.CircuitBreakerManager;
import org.wso2.carbon.identity.core.circuitbreaker.TenantService;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.notification.NotificationConstants.EmailNotification;
import org.wso2.carbon.identity.event.handler.notification.email.bean.Notification;
import org.wso2.carbon.identity.event.handler.notification.internal.NotificationHandlerDataHolder;
import org.wso2.carbon.identity.event.handler.notification.util.NotificationUtil;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for NotificationHandler.
 */
public class NotificationHandlerTest {

    @Mock
    private NotificationHandlerDataHolder dataHolder;

    @Mock
    private EventStreamService eventStreamService;

    @Mock
    private OrganizationManager organizationManager;

    private MockedStatic<NotificationHandlerDataHolder> mockedDataHolder;
    private MockedStatic<LoggerUtils> mockedLoggerUtils;
    private MockedStatic<NotificationUtil> mockedNotificationUtil;
    private MockedStatic<I18nEmailUtil> mockedI18nEmailUtil;

    private TestNotificationHandler handler;

    private final String TENANT_DOMAIN = "test";

    @BeforeMethod
    public void setUp() throws Exception {

        MockitoAnnotations.openMocks(this);
        handler = new TestNotificationHandler();

        mockedDataHolder = mockStatic(NotificationHandlerDataHolder.class);
        mockedLoggerUtils = mockStatic(LoggerUtils.class);
        mockedNotificationUtil = mockStatic(NotificationUtil.class);
        mockedI18nEmailUtil = mockStatic(I18nEmailUtil.class);

        mockedDataHolder.when(NotificationHandlerDataHolder::getInstance).thenReturn(dataHolder);
        when(dataHolder.getEventStreamService()).thenReturn(eventStreamService);
        when(dataHolder.getOrganizationManager()).thenReturn(organizationManager);
        mockedLoggerUtils.when(LoggerUtils::isDiagnosticLogsEnabled).thenReturn(false);
        mockedI18nEmailUtil.when(() -> I18nEmailUtil.getNormalizedName(anyString())).thenReturn("TestTemplate");
    }

    @AfterMethod
    public void tearDown() {

        mockedDataHolder.close();
        mockedLoggerUtils.close();
        mockedNotificationUtil.close();
        mockedI18nEmailUtil.close();
        CircuitBreakerManager.getInstance().invalidateTenantService(TENANT_DOMAIN, TenantService.EMAIL_NOTIFICATION);
    }

    @Test
    public void testHandleEvent_asyncPath_whenSyncFlagAbsent() throws Exception {

        Notification notification = buildMockNotification();
        Event event = new Event("TEST_EVENT", new HashMap<>());
        mockedNotificationUtil.when(() -> NotificationUtil.buildNotification(any(Event.class), any()))
                .thenReturn(notification);

        handler.handleEvent(event);

        verify(eventStreamService).publish(any());
        verify(eventStreamService, never()).publishAndNotifyErrors(any());
    }

    @Test
    public void testHandleEvent_asyncPath_whenSyncFlagFalse() throws Exception {

        Notification notification = buildMockNotification();
        Event event = new Event("TEST_EVENT", new HashMap<>());
        event.getEventProperties().put(EmailNotification.SYNC_EMAIL_NOTIFICATION, "false");
        mockedNotificationUtil.when(() -> NotificationUtil.buildNotification(any(Event.class), any()))
                .thenReturn(notification);

        handler.handleEvent(event);

        verify(eventStreamService).publish(any());
        verify(eventStreamService, never()).publishAndNotifyErrors(any());
    }

    @Test
    public void testHandleEvent_syncPath_whenSyncFlagTrue() throws Exception {

        Notification notification = buildMockNotification();
        Event event = new Event("TEST_EVENT", new HashMap<>());
        event.getEventProperties().put(EmailNotification.SYNC_EMAIL_NOTIFICATION, "true");
        event.getEventProperties().put(NotificationConstants.TENANT_DOMAIN, TENANT_DOMAIN);
        mockedNotificationUtil.when(() -> NotificationUtil.buildNotification(any(Event.class), any()))
                .thenReturn(notification);

        handler.handleEvent(event);

        verify(eventStreamService).publishAndNotifyErrors(any());
        verify(eventStreamService, never()).publish(any());
    }

    @Test
    public void testHandleEvent_syncPath_addsSyncFlagsAndRemovesSyncNotificationKey() throws Exception {

        Notification notification = buildMockNotification();
        Event event = new Event("TEST_EVENT", new HashMap<>());
        event.getEventProperties().put(EmailNotification.SYNC_EMAIL_NOTIFICATION, "true");
        event.getEventProperties().put(NotificationConstants.TENANT_DOMAIN, TENANT_DOMAIN);
        mockedNotificationUtil.when(() -> NotificationUtil.buildNotification(any(Event.class), any()))
                .thenReturn(notification);

        handler.handleEvent(event);

        ArgumentCaptor<org.wso2.carbon.databridge.commons.Event> captor =
                ArgumentCaptor.forClass(org.wso2.carbon.databridge.commons.Event.class);
        verify(eventStreamService).publishAndNotifyErrors(captor.capture());
        Map<String, String> arbitraryData = captor.getValue().getArbitraryDataMap();
        Assert.assertEquals(arbitraryData.get(EmailNotification.EMAIL_SYNC), Boolean.TRUE.toString());
        Assert.assertEquals(arbitraryData.get(EmailNotification.HTTP_SYNC), Boolean.TRUE.toString());
        Assert.assertFalse(arbitraryData.containsKey(EmailNotification.SYNC_EMAIL_NOTIFICATION),
                "SYNC_EMAIL_NOTIFICATION key should be removed from the published event data");
    }

    @Test
    public void testHandleEvent_skipsPublishing_whenNotificationIsNull() throws Exception {

        Event event = new Event("TEST_EVENT", new HashMap<>());
        mockedNotificationUtil.when(() -> NotificationUtil.buildNotification(any(Event.class), any()))
                .thenReturn(null);

        handler.handleEvent(event);

        verify(eventStreamService, never()).publish(any());
        verify(eventStreamService, never()).publishAndNotifyErrors(any());
    }

    @Test
    public void testPublishToStreamAndNotifyErrors_noException_success() throws Exception {

        handler.publishToStreamAndNotifyErrors(buildMockNotification(), buildPlaceholderMap());

        verify(eventStreamService).publishAndNotifyErrors(any());
    }

    @Test
    public void testPublishToStreamAndNotifyErrors_aggregatedFailure_validCauseChain_throwsMappedException()
            throws Exception {

        ConsumerFailureException failure = buildConsumerFailure(EmailNotification.AdapterErrorCodes.EMAIL_SEND_FAILED);
        doThrow(new AggregatedConsumerFailureException(Collections.singletonList(failure)))
                .when(eventStreamService).publishAndNotifyErrors(any());

        try {
            handler.publishToStreamAndNotifyErrors(buildMockNotification(), buildPlaceholderMap());
            Assert.fail("Expected IdentityEventException");
        } catch (IdentityEventException e) {
            Assert.assertEquals(e.getErrorCode(), EmailNotification.ErrorMessages.EMAIL_SEND_FAILED.getCode());
        }
    }

    @Test
    public void testPublishToStreamAndNotifyErrors_aggregatedFailure_multipleFailures_throwsFirstMapped()
            throws Exception {

        ConsumerFailureException first = buildConsumerFailure(EmailNotification.AdapterErrorCodes.EMAIL_AUTH_FAILED);
        ConsumerFailureException second = buildConsumerFailure(EmailNotification.AdapterErrorCodes.EMAIL_SEND_REJECTED);
        doThrow(new AggregatedConsumerFailureException(Arrays.asList(first, second)))
                .when(eventStreamService).publishAndNotifyErrors(any());

        try {
            handler.publishToStreamAndNotifyErrors(buildMockNotification(), buildPlaceholderMap());
            Assert.fail("Expected IdentityEventException");
        } catch (IdentityEventException e) {
            Assert.assertEquals(e.getErrorCode(), EmailNotification.ErrorMessages.EMAIL_AUTH_FAILED.getCode());
        }
    }

    @Test
    public void testPublishToStreamAndNotifyErrors_aggregatedFailure_nonEventStreamCause_swallowed() throws Exception {

        ConsumerFailureException failure = new ConsumerFailureException("type", "stream",
                new RuntimeException("not EventStreamException"));
        doThrow(new AggregatedConsumerFailureException(Collections.singletonList(failure)))
                .when(eventStreamService).publishAndNotifyErrors(any());

        handler.publishToStreamAndNotifyErrors(buildMockNotification(), buildPlaceholderMap());
    }

    @Test
    public void testPublishToStreamAndNotifyErrors_aggregatedFailure_noAdapterExceptionCause_swallowed()
            throws Exception {

        EventStreamException streamEx = new EventStreamException("stream error with no adapter cause");
        ConsumerFailureException failure = new ConsumerFailureException("type", "stream", streamEx);
        doThrow(new AggregatedConsumerFailureException(Collections.singletonList(failure)))
                .when(eventStreamService).publishAndNotifyErrors(any());

        handler.publishToStreamAndNotifyErrors(buildMockNotification(), buildPlaceholderMap());
    }

    @Test
    public void testPublishToStreamAndNotifyErrors_singleConsumerFailure_validCauseChain_throwsMappedException()
            throws Exception {

        ConsumerFailureException failure = buildConsumerFailure(
                EmailNotification.AdapterErrorCodes.HTTP_PUBLISH_UNAUTHORIZED);
        doThrow(failure).when(eventStreamService).publishAndNotifyErrors(any());

        try {
            handler.publishToStreamAndNotifyErrors(buildMockNotification(), buildPlaceholderMap());
            Assert.fail("Expected IdentityEventException");
        } catch (IdentityEventException e) {
            Assert.assertEquals(e.getErrorCode(),
                    EmailNotification.ErrorMessages.HTTP_PUBLISH_UNAUTHORIZED.getCode());
        }
    }

    @Test
    public void testPublishToStreamAndNotifyErrors_singleConsumerFailure_nonEventStreamCause_swallowed()
            throws Exception {

        ConsumerFailureException failure = new ConsumerFailureException("type", "stream",
                new RuntimeException("not EventStreamException"));
        doThrow(failure).when(eventStreamService).publishAndNotifyErrors(any());

        handler.publishToStreamAndNotifyErrors(buildMockNotification(), buildPlaceholderMap());
    }

    @Test
    public void testPublishToStreamAndNotifyErrors_singleConsumerFailure_noAdapterExceptionCause_swallowed()
            throws Exception {

        EventStreamException streamEx = new EventStreamException("stream error");
        ConsumerFailureException failure = new ConsumerFailureException("type", "stream", streamEx);
        doThrow(failure).when(eventStreamService).publishAndNotifyErrors(any());

        handler.publishToStreamAndNotifyErrors(buildMockNotification(), buildPlaceholderMap());
    }

    @DataProvider(name = "adapterErrorCodeMappings")
    public Object[][] provideAdapterErrorCodeMappings() {

        return new Object[][] {
            { EmailNotification.AdapterErrorCodes.EMAIL_SEND_FAILED,
                    EmailNotification.ErrorMessages.EMAIL_SEND_FAILED },
            { EmailNotification.AdapterErrorCodes.EMAIL_MISSING_ADDRESS,
                    EmailNotification.ErrorMessages.EMAIL_MISSING_ADDRESS },
            { EmailNotification.AdapterErrorCodes.EMAIL_ENCODING_FAILED,
                    EmailNotification.ErrorMessages.EMAIL_ENCODING_FAILED },
            { EmailNotification.AdapterErrorCodes.EMAIL_AUTH_FAILED,
                    EmailNotification.ErrorMessages.EMAIL_AUTH_FAILED },
            { EmailNotification.AdapterErrorCodes.EMAIL_SEND_REJECTED,
                    EmailNotification.ErrorMessages.EMAIL_SEND_REJECTED },
            { EmailNotification.AdapterErrorCodes.HTTP_CLIENT_INIT_FAILED,
                    EmailNotification.ErrorMessages.HTTP_CLIENT_INIT_FAILED },
            { EmailNotification.AdapterErrorCodes.HTTP_CLIENT_NOT_INITIALIZED,
                    EmailNotification.ErrorMessages.HTTP_CLIENT_NOT_INITIALIZED },
            { EmailNotification.AdapterErrorCodes.HTTP_PUBLISH_UNAUTHORIZED,
                    EmailNotification.ErrorMessages.HTTP_PUBLISH_UNAUTHORIZED },
            { EmailNotification.AdapterErrorCodes.HTTP_PUBLISH_FORBIDDEN,
                    EmailNotification.ErrorMessages.HTTP_PUBLISH_FORBIDDEN },
            { EmailNotification.AdapterErrorCodes.HTTP_PUBLISH_BAD_REQUEST,
                    EmailNotification.ErrorMessages.HTTP_PUBLISH_BAD_REQUEST },
            { EmailNotification.AdapterErrorCodes.HTTP_PUBLISH_TOO_MANY_REQUESTS,
                    EmailNotification.ErrorMessages.HTTP_PUBLISH_TOO_MANY_REQUESTS },
            { EmailNotification.AdapterErrorCodes.HTTP_PUBLISH_SERVER_ERROR,
                    EmailNotification.ErrorMessages.HTTP_PUBLISH_SERVER_ERROR },
            { EmailNotification.AdapterErrorCodes.HTTP_PUBLISH_SERVICE_UNAVAILABLE,
                    EmailNotification.ErrorMessages.HTTP_PUBLISH_SERVICE_UNAVAILABLE },
            { EmailNotification.AdapterErrorCodes.HTTP_PUBLISH_FAILED_IO,
                    EmailNotification.ErrorMessages.HTTP_PUBLISH_FAILED_IO },
            { EmailNotification.AdapterErrorCodes.HTTP_TOKEN_REFRESH_MISSING_CREDS,
                    EmailNotification.ErrorMessages.HTTP_TOKEN_REFRESH_MISSING_CREDS },
            { EmailNotification.AdapterErrorCodes.HTTP_TOKEN_FETCH_FAILED,
                    EmailNotification.ErrorMessages.HTTP_TOKEN_FETCH_FAILED },
        };
    }

    @Test(dataProvider = "adapterErrorCodeMappings")
    public void testAdapterErrorCodeMapsToCorrectIdentityEventException(
            String adapterErrorCode, EmailNotification.ErrorMessages expectedError) throws Exception {

        doThrow(buildConsumerFailure(adapterErrorCode)).when(eventStreamService).publishAndNotifyErrors(any());

        try {
            handler.publishToStreamAndNotifyErrors(buildMockNotification(), buildPlaceholderMap());
            Assert.fail("Expected IdentityEventException for adapter error code: " + adapterErrorCode);
        } catch (IdentityEventException e) {
            Assert.assertEquals(e.getErrorCode(), expectedError.getCode(),
                    "Unexpected error code for adapter error: " + adapterErrorCode);
            Assert.assertEquals(e.getMessage(), expectedError.getMessage(),
                    "Unexpected message for adapter error: " + adapterErrorCode);
        }
    }

    @Test
    public void testPublishToStreamAndNotifyErrors_unknownAdapterErrorCode_mapsToUnknownError() throws Exception {

        doThrow(buildConsumerFailure("UNKNOWN-ADAPTER-9999")).when(eventStreamService).publishAndNotifyErrors(any());

        try {
            handler.publishToStreamAndNotifyErrors(buildMockNotification(), buildPlaceholderMap());
            Assert.fail("Expected IdentityEventException");
        } catch (IdentityEventException e) {
            Assert.assertEquals(e.getErrorCode(), EmailNotification.ErrorMessages.UNKNOWN_ERROR.getCode());
            Assert.assertEquals(e.getMessage(), EmailNotification.ErrorMessages.UNKNOWN_ERROR.getMessage());
        }
    }

    @Test
    public void testPublishToStreamAndNotifyErrors_circuitBreakerOpensAfterHighFailureRate() throws Exception {

        ConsumerFailureException failure = buildConsumerFailure(EmailNotification.AdapterErrorCodes.EMAIL_SEND_FAILED);
        doThrow(failure).when(eventStreamService).publishAndNotifyErrors(any());

        int totalCalls = 16;
        int emailSendFailedCount = 0;
        int throttledCount = 0;
        boolean circuitOpened = false;

        for (int i = 0; i < totalCalls; i++) {
            try {
                handler.publishToStreamAndNotifyErrors(buildMockNotification(), buildPlaceholderMap());
                Assert.fail("Expected IdentityEventException on call " + (i + 1));
            } catch (IdentityEventException e) {
                String errorCode = e.getErrorCode();
                if (EmailNotification.ErrorMessages.EMAIL_NOTIFICATION_THROTTLED.getCode().equals(errorCode)) {
                    circuitOpened = true;
                    throttledCount++;
                } else {
                    Assert.assertFalse(circuitOpened,
                            "Expected throttled error after circuit opened but got " + errorCode + " on call " + (i + 1));
                    Assert.assertEquals(errorCode, EmailNotification.ErrorMessages.EMAIL_SEND_FAILED.getCode(),
                            "Unexpected error code on call " + (i + 1));
                    emailSendFailedCount++;
                }
            }
        }

        Assert.assertEquals(emailSendFailedCount, 15);
        Assert.assertEquals(throttledCount, 1);
        Assert.assertTrue(circuitOpened);
    }

    @Test
    public void testPublishToStreamAndNotifyErrors_nullAdapterErrorCode_mapsToUnknownErrorMessage() throws Exception {

        OutputEventAdapterException adapterEx = new OutputEventAdapterException((String) null, "adapter error");
        EventStreamException streamEx = new EventStreamException("stream error", adapterEx);
        ConsumerFailureException failure = new ConsumerFailureException("type", "stream", streamEx);
        doThrow(failure).when(eventStreamService).publishAndNotifyErrors(any());

        try {
            handler.publishToStreamAndNotifyErrors(buildMockNotification(), buildPlaceholderMap());
            Assert.fail("Expected IdentityEventException");
        } catch (IdentityEventException e) {
            Assert.assertEquals(e.getMessage(), EmailNotification.ErrorMessages.UNKNOWN_ERROR.getMessage());
        }
    }

    private ConsumerFailureException buildConsumerFailure(String adapterErrorCode) {

        OutputEventAdapterException adapterEx = new OutputEventAdapterException(adapterErrorCode, "adapter error");
        EventStreamException streamEx = new EventStreamException("stream error", adapterEx);
        return new ConsumerFailureException("email-type", "id_gov_notify_stream:1.0.0", streamEx);
    }

    private Map<String, String> buildPlaceholderMap() {

        Map<String, String> map = new HashMap<>();
        map.put("tmp-stream-id", "id_gov_notify_stream:1.0.0");
        map.put(NotificationConstants.TENANT_DOMAIN, TENANT_DOMAIN);
        return map;
    }

    private Notification buildMockNotification() {

        Notification notification = mock(Notification.class);
        EmailTemplate template = mock(EmailTemplate.class);
        when(notification.getTemplate()).thenReturn(template);
        when(template.getTemplateDisplayName()).thenReturn("TestTemplate");
        when(template.getSubject()).thenReturn("Test Subject");
        when(template.getBody()).thenReturn("Test Body");
        when(template.getFooter()).thenReturn("Test Footer");
        when(template.getLocale()).thenReturn("en_US");
        when(template.getEmailContentType()).thenReturn("text/html");
        when(notification.getSendFrom()).thenReturn("from@test.com");
        when(notification.getSendTo()).thenReturn("to@test.com");
        when(notification.getSubject()).thenReturn("Test Subject");
        when(notification.getBody()).thenReturn("Test Body");
        when(notification.getFooter()).thenReturn("Test Footer");
        return notification;
    }

    /**
     * Overrides subscription-property lookups that require a configured handler runtime.
     */
    private static class TestNotificationHandler extends NotificationHandler {

        @Override
        protected String getNotificationTemplate(Event event) throws IdentityEventException {

            return null;
        }

        @Override
        public String getStreamDefinitionID(Event event) throws IdentityEventException {

            return "id_gov_notify_stream:1.0.0";
        }
    }
}
