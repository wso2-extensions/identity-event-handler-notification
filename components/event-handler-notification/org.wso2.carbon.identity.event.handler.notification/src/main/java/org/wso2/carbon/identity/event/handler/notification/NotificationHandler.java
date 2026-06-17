/*
 * Copyright (c) 2016-2026, WSO2 LLC. (https://www.wso2.com).
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.event.output.adapter.core.exception.OutputEventAdapterException;
import org.wso2.carbon.event.stream.core.EventStreamService;
import org.wso2.carbon.event.stream.core.exception.AggregatedConsumerFailureException;
import org.wso2.carbon.event.stream.core.exception.ConsumerFailureException;
import org.wso2.carbon.event.stream.core.exception.EventStreamException;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.core.circuitbreaker.CircuitBreakerManager;
import org.wso2.carbon.identity.core.circuitbreaker.Decision;
import org.wso2.carbon.identity.core.circuitbreaker.TenantService;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.notification.NotificationConstants.EmailNotification;
import org.wso2.carbon.identity.event.handler.notification.email.bean.Notification;
import org.wso2.carbon.identity.event.handler.notification.internal.NotificationHandlerDataHolder;
import org.wso2.carbon.identity.event.handler.notification.util.NotificationUtil;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.utils.DiagnosticLog;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the Email and SMS Notification Handler which connected to the direct CEP stream.
 * Extended from the DefaultNotificationHandler which is define the default notification send.
 *
 */
public class NotificationHandler extends DefaultNotificationHandler {

    private static final Log log = LogFactory.getLog(NotificationHandler.class);
    private static final String STREAM_ID = "id_gov_notify_stream:1.0.0";

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        //We can set the notification template from the identity-even.properties file as a property of the subscription
        //property. Then it will get the first priority.
        String notificationTemplate = getNotificationTemplate(event);
        if(StringUtils.isNotEmpty(notificationTemplate)){
            event.getEventProperties().put(EmailNotification.EMAIL_TEMPLATE_TYPE,
                    notificationTemplate);
        }
        Map<String, String> arbitraryDataMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : event.getEventProperties().entrySet()) {
            if (entry.getValue() instanceof String) {
                arbitraryDataMap.put(entry.getKey(), (String) entry.getValue());
            }
        }
        if (LoggerUtils.isDiagnosticLogsEnabled()) {
            DiagnosticLog.DiagnosticLogBuilder diagnosticLogBuilder = new DiagnosticLog.DiagnosticLogBuilder(
                    NotificationConstants.LogConstants.NOTIFICATION_HANDLER_SERVICE,
                    NotificationConstants.LogConstants.ActionIDs.HANDLE_EVENT);
            diagnosticLogBuilder
                    .inputParam(NotificationConstants.LogConstants.InputKeys.EVENT_NAME,
                            arbitraryDataMap.get(NotificationConstants.TEMPLATE_TYPE))
                    .inputParam(NotificationConstants.LogConstants.InputKeys.TENANT_DOMAIN,
                            arbitraryDataMap.get(NotificationConstants.TENANT_DOMAIN))
                    .resultMessage("Notification will be handled.")
                    .resultStatus(DiagnosticLog.ResultStatus.SUCCESS)
                    .logDetailLevel(DiagnosticLog.LogDetailLevel.INTERNAL_SYSTEM);
            LoggerUtils.triggerDiagnosticLogEvent(diagnosticLogBuilder);
        }

        String tenantDomain = arbitraryDataMap.get(NotificationConstants.TENANT_DOMAIN);
        try {
            if (StringUtils.isNotBlank(tenantDomain)) {
                // Resolve the organization id and add to attribute data map.
                OrganizationManager organizationManager =
                        NotificationHandlerDataHolder.getInstance().getOrganizationManager();
                String organizationId = organizationManager.resolveOrganizationId(tenantDomain);
                arbitraryDataMap.put(EmailNotification.ORGANIZATION_ID_PLACEHOLDER,
                        organizationId);
            }
        } catch (OrganizationManagementException e) {
            throw new IdentityEventException(e.getMessage(), e);
        }

        Notification notification = NotificationUtil.buildNotification(event, arbitraryDataMap);

        if (notification == null) {
            if (log.isDebugEnabled()) {
                log.debug("Notification is null. Hence returning without sending the notification." +
                        " Event : " + event.getEventName());
            }
            return;
        }

        //Stream definition will be read from the identity-even.properties file as a property of the subscription
        //property. Then it will get the first priority.
        String streamDefinitionID = getStreamDefinitionID(event);
        //This stream-id was set to the map to pass to the publishToStream method only to avoid API change.
        arbitraryDataMap.put("tmp-stream-id", streamDefinitionID);
        if (isSyncEmailDelivery(arbitraryDataMap)) {
            publishToStreamAndNotifyErrors(notification, arbitraryDataMap);
        } else {
            publishToStream(notification, arbitraryDataMap);
        }
    }

    private boolean isSyncEmailDelivery(Map<String, String> arbitraryDataMap) {

        String syncValue = arbitraryDataMap.remove(EmailNotification.SYNC_EMAIL_NOTIFICATION);
        if (Boolean.parseBoolean(syncValue)) {
            arbitraryDataMap.put(EmailNotification.EMAIL_SYNC, Boolean.TRUE.toString());
            arbitraryDataMap.put(EmailNotification.HTTP_SYNC, Boolean.TRUE.toString());
            return true;
        }
        return false;
    }

    private org.wso2.carbon.databridge.commons.Event buildDatabridgeEvent(Notification notification,
            Map<String, String> placeHolderDataMap) {

        org.wso2.carbon.databridge.commons.Event databridgeEvent = new org.wso2.carbon.databridge.commons.Event();
        databridgeEvent.setTimeStamp(System.currentTimeMillis());
        databridgeEvent.setStreamId(placeHolderDataMap.remove("tmp-stream-id"));

        Map<String, String> arbitraryDataMap = new HashMap<>();
        arbitraryDataMap.put(EmailNotification.ARBITRARY_EVENT_TYPE, I18nEmailUtil.
                getNormalizedName(notification.getTemplate().getTemplateDisplayName()));
        arbitraryDataMap.put(IdentityEventConstants.EventProperty.USER_NAME,
                placeHolderDataMap.get(IdentityEventConstants.EventProperty.USER_NAME));
        arbitraryDataMap.put(IdentityEventConstants.EventProperty.USER_STORE_DOMAIN,
                placeHolderDataMap.get(IdentityEventConstants.EventProperty.USER_STORE_DOMAIN));
        arbitraryDataMap.put(IdentityEventConstants.EventProperty.TENANT_DOMAIN,
                placeHolderDataMap.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN));
        arbitraryDataMap.put(EmailNotification.ARBITRARY_SEND_FROM, notification.getSendFrom());
        for (Map.Entry<String, String> placeHolderDataEntry : placeHolderDataMap.entrySet()) {
            arbitraryDataMap.put(placeHolderDataEntry.getKey(), placeHolderDataEntry.getValue());
        }
        arbitraryDataMap.put(EmailNotification.ARBITRARY_SUBJECT_TEMPLATE, notification.
                getTemplate().getSubject());
        arbitraryDataMap.put(EmailNotification.ARBITRARY_BODY_TEMPLATE, notification.
                getTemplate().getBody());
        arbitraryDataMap.put(EmailNotification.ARBITRARY_FOOTER_TEMPLATE, notification.
                getTemplate().getFooter());
        arbitraryDataMap.put(EmailNotification.ARBITRARY_LOCALE, notification.getTemplate().
                getLocale());
        arbitraryDataMap.put(EmailNotification.ARBITRARY_CONTENT_TYPE, notification.
                getTemplate().getEmailContentType());
        arbitraryDataMap.put(EmailNotification.ARBITRARY_SEND_TO, notification.getSendTo());
        arbitraryDataMap.put(EmailNotification.ARBITRARY_SUBJECT, notification.getSubject());
        arbitraryDataMap.put(EmailNotification.ARBITRARY_BODY, notification.getBody());
        arbitraryDataMap.put(EmailNotification.ARBITRARY_FOOTER, notification.getFooter());

        databridgeEvent.setArbitraryDataMap(arbitraryDataMap);
        return databridgeEvent;
    }

    protected void publishToStream(Notification notification, Map<String, String> placeHolderDataMap) {

        EventStreamService service = NotificationHandlerDataHolder.getInstance().getEventStreamService();
        service.publish(buildDatabridgeEvent(notification, placeHolderDataMap));
    }

    /**
     * Publishes a notification event to the event stream, propagating consumer errors back to the caller.
     * Unlike {@link #publishToStream}, this method uses the circuit breaker to throttle requests per tenant
     * and throws an {@link IdentityEventException} if the publish fails or is throttled.
     *
     * @param notification       the notification to be published.
     * @param placeHolderDataMap placeholder data map containing template variables, including the tenant domain.
     * @throws IdentityEventException if the circuit breaker throttles the request, or if the event stream
     *                                consumer reports a known or unknown failure.
     */
    protected void publishToStreamAndNotifyErrors(Notification notification,
            Map<String, String> placeHolderDataMap) throws IdentityEventException {

        EventStreamService service = NotificationHandlerDataHolder.getInstance().getEventStreamService();
        String tenantDomain = placeHolderDataMap.get(NotificationConstants.TENANT_DOMAIN);
        Decision acquireDecision = CircuitBreakerManager.getInstance().tryAcquire(tenantDomain, TenantService.EMAIL_NOTIFICATION);
        if (acquireDecision.isAllowed()) {
            if (log.isDebugEnabled()) {
                log.debug("Circuit breaker allowed sync email notification for tenant: " + tenantDomain
                        + ". Attempting to publish.");
            }
            boolean publishSucceeded = false;
            try {
                service.publishAndNotifyErrors(buildDatabridgeEvent(notification, placeHolderDataMap));
                publishSucceeded = true;
                if (log.isDebugEnabled()) {
                    log.debug("Sync email notification published successfully for tenant: " + tenantDomain);
                }
                if (LoggerUtils.isDiagnosticLogsEnabled()) {
                    DiagnosticLog.DiagnosticLogBuilder diagnosticLogBuilder =
                            new DiagnosticLog.DiagnosticLogBuilder(
                                    NotificationConstants.LogConstants.NOTIFICATION_HANDLER_SERVICE,
                                    NotificationConstants.LogConstants.ActionIDs.PUBLISH_SYNC_EMAIL_NOTIFICATION);
                    diagnosticLogBuilder
                            .inputParam(NotificationConstants.LogConstants.InputKeys.TENANT_DOMAIN, tenantDomain)
                            .resultMessage("Sync email notification published successfully.")
                            .resultStatus(DiagnosticLog.ResultStatus.SUCCESS)
                            .logDetailLevel(DiagnosticLog.LogDetailLevel.INTERNAL_SYSTEM);
                    LoggerUtils.triggerDiagnosticLogEvent(diagnosticLogBuilder);
                }
            } catch (EventStreamException e) {
                IdentityEventException resolved = null;
                if (e instanceof AggregatedConsumerFailureException) {
                    int failureCount = ((AggregatedConsumerFailureException) e).getFailures().size();
                    if (log.isDebugEnabled()) {
                        log.debug("Sync email notification encountered " + failureCount
                                + " aggregated consumer failure(s) for tenant: " + tenantDomain);
                    }
                    for (ConsumerFailureException failure : ((AggregatedConsumerFailureException) e).getFailures()) {
                        resolved = resolveConsumerFailure(failure);
                        if (resolved != null) break;
                    }
                } else if (e instanceof ConsumerFailureException) {
                    if (log.isDebugEnabled()) {
                        log.debug("Sync email notification encountered a consumer failure for tenant: "
                                + tenantDomain);
                    }
                    resolved = resolveConsumerFailure((ConsumerFailureException) e);
                }
                if (resolved != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Resolved sync email consumer failure for tenant: " + tenantDomain
                                + ". Error: " + resolved.getMessage());
                    }
                    if (LoggerUtils.isDiagnosticLogsEnabled()) {
                        DiagnosticLog.DiagnosticLogBuilder diagnosticLogBuilder =
                                new DiagnosticLog.DiagnosticLogBuilder(
                                        NotificationConstants.LogConstants.NOTIFICATION_HANDLER_SERVICE,
                                        NotificationConstants.LogConstants.ActionIDs.PUBLISH_SYNC_EMAIL_NOTIFICATION);
                        diagnosticLogBuilder
                                .inputParam(NotificationConstants.LogConstants.InputKeys.TENANT_DOMAIN, tenantDomain)
                                .resultMessage("Sync email notification failed: " + resolved.getMessage())
                                .resultStatus(DiagnosticLog.ResultStatus.FAILED)
                                .logDetailLevel(DiagnosticLog.LogDetailLevel.INTERNAL_SYSTEM);
                        LoggerUtils.triggerDiagnosticLogEvent(diagnosticLogBuilder);
                    }
                    throw resolved;
                }
                if (log.isDebugEnabled()) {
                    log.debug("Unresolvable consumer failure during sync email notification for tenant: "
                            + tenantDomain, e);
                }
                if (LoggerUtils.isDiagnosticLogsEnabled()) {
                    DiagnosticLog.DiagnosticLogBuilder diagnosticLogBuilder =
                            new DiagnosticLog.DiagnosticLogBuilder(
                                    NotificationConstants.LogConstants.NOTIFICATION_HANDLER_SERVICE,
                                    NotificationConstants.LogConstants.ActionIDs.PUBLISH_SYNC_EMAIL_NOTIFICATION);
                    diagnosticLogBuilder
                            .inputParam(NotificationConstants.LogConstants.InputKeys.TENANT_DOMAIN, tenantDomain)
                            .resultMessage("Sync email notification failed due to an unexpected consumer error.")
                            .resultStatus(DiagnosticLog.ResultStatus.FAILED)
                            .logDetailLevel(DiagnosticLog.LogDetailLevel.INTERNAL_SYSTEM);
                    LoggerUtils.triggerDiagnosticLogEvent(diagnosticLogBuilder);
                }
                throw new IdentityEventException(
                    EmailNotification.ErrorMessages.UNKNOWN_ERROR.getCode(),
                    EmailNotification.ErrorMessages.UNKNOWN_ERROR.getMessage(), e);
            } finally {
                CircuitBreakerManager.getInstance().onComplete(
                    tenantDomain, TenantService.EMAIL_NOTIFICATION, acquireDecision, publishSucceeded);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Sync email notification throttled by circuit breaker for tenant: " + tenantDomain);
            }
            if (LoggerUtils.isDiagnosticLogsEnabled()) {
                DiagnosticLog.DiagnosticLogBuilder diagnosticLogBuilder =
                        new DiagnosticLog.DiagnosticLogBuilder(
                                NotificationConstants.LogConstants.NOTIFICATION_HANDLER_SERVICE,
                                NotificationConstants.LogConstants.ActionIDs.PUBLISH_SYNC_EMAIL_NOTIFICATION);
                diagnosticLogBuilder
                        .inputParam(NotificationConstants.LogConstants.InputKeys.TENANT_DOMAIN, tenantDomain)
                        .resultMessage("Sync email notification throttled by circuit breaker.")
                        .resultStatus(DiagnosticLog.ResultStatus.FAILED)
                        .logDetailLevel(DiagnosticLog.LogDetailLevel.INTERNAL_SYSTEM);
                LoggerUtils.triggerDiagnosticLogEvent(diagnosticLogBuilder);
            }
            throw new IdentityEventException(
                    EmailNotification.ErrorMessages.EMAIL_NOTIFICATION_THROTTLED.getCode(),
                    EmailNotification.ErrorMessages.EMAIL_NOTIFICATION_THROTTLED.getMessage());
        }
    }

    private IdentityEventException resolveConsumerFailure(ConsumerFailureException failure) {

        Throwable cause = failure.getCause();
        if (!(cause instanceof EventStreamException)) {
            return null;
        }
        Throwable adapterCause = cause.getCause();
        if (!(adapterCause instanceof OutputEventAdapterException)) {
            return null;
        }
        OutputEventAdapterException adapterEx = (OutputEventAdapterException) adapterCause;
        return buildAdapterErrorException(adapterEx.getErrorCode(), failure);
    }

    private IdentityEventException buildAdapterErrorException(String errorCode,
            ConsumerFailureException cause) {

        if (errorCode == null) {
            return new IdentityEventException(
                EmailNotification.ErrorMessages.UNKNOWN_ERROR.getCode(),
                EmailNotification.ErrorMessages.UNKNOWN_ERROR.getMessage(), cause);
        }
        switch (errorCode) {
            case EmailNotification.AdapterErrorCodes.EMAIL_SEND_FAILED:
                return new IdentityEventException(
                        EmailNotification.ErrorMessages.EMAIL_SEND_FAILED.getCode(),
                        EmailNotification.ErrorMessages.EMAIL_SEND_FAILED.getMessage(), cause);
            case EmailNotification.AdapterErrorCodes.EMAIL_MISSING_ADDRESS:
                return new IdentityEventException(
                        EmailNotification.ErrorMessages.EMAIL_MISSING_ADDRESS.getCode(),
                        EmailNotification.ErrorMessages.EMAIL_MISSING_ADDRESS.getMessage(), cause);
            case EmailNotification.AdapterErrorCodes.EMAIL_ENCODING_FAILED:
                return new IdentityEventException(
                        EmailNotification.ErrorMessages.EMAIL_ENCODING_FAILED.getCode(),
                        EmailNotification.ErrorMessages.EMAIL_ENCODING_FAILED.getMessage(), cause);
            case EmailNotification.AdapterErrorCodes.EMAIL_AUTH_FAILED:
                return new IdentityEventException(
                        EmailNotification.ErrorMessages.EMAIL_AUTH_FAILED.getCode(),
                        EmailNotification.ErrorMessages.EMAIL_AUTH_FAILED.getMessage(), cause);
            case EmailNotification.AdapterErrorCodes.EMAIL_SEND_REJECTED:
                return new IdentityEventException(
                        EmailNotification.ErrorMessages.EMAIL_SEND_REJECTED.getCode(),
                        EmailNotification.ErrorMessages.EMAIL_SEND_REJECTED.getMessage(), cause);
            case EmailNotification.AdapterErrorCodes.HTTP_CLIENT_INIT_FAILED:
                return new IdentityEventException(
                        EmailNotification.ErrorMessages.HTTP_CLIENT_INIT_FAILED.getCode(),
                        EmailNotification.ErrorMessages.HTTP_CLIENT_INIT_FAILED.getMessage(), cause);
            case EmailNotification.AdapterErrorCodes.HTTP_CLIENT_NOT_INITIALIZED:
                return new IdentityEventException(
                        EmailNotification.ErrorMessages.HTTP_CLIENT_NOT_INITIALIZED.getCode(),
                        EmailNotification.ErrorMessages.HTTP_CLIENT_NOT_INITIALIZED.getMessage(), cause);
            case EmailNotification.AdapterErrorCodes.HTTP_PUBLISH_UNAUTHORIZED:
                return new IdentityEventException(
                        EmailNotification.ErrorMessages.HTTP_PUBLISH_UNAUTHORIZED.getCode(),
                        EmailNotification.ErrorMessages.HTTP_PUBLISH_UNAUTHORIZED.getMessage(), cause);
            case EmailNotification.AdapterErrorCodes.HTTP_PUBLISH_FORBIDDEN:
                return new IdentityEventException(
                        EmailNotification.ErrorMessages.HTTP_PUBLISH_FORBIDDEN.getCode(),
                        EmailNotification.ErrorMessages.HTTP_PUBLISH_FORBIDDEN.getMessage(), cause);
            case EmailNotification.AdapterErrorCodes.HTTP_PUBLISH_BAD_REQUEST:
                return new IdentityEventException(
                        EmailNotification.ErrorMessages.HTTP_PUBLISH_BAD_REQUEST.getCode(),
                        EmailNotification.ErrorMessages.HTTP_PUBLISH_BAD_REQUEST.getMessage(), cause);
            case EmailNotification.AdapterErrorCodes.HTTP_PUBLISH_TOO_MANY_REQUESTS:
                return new IdentityEventException(
                        EmailNotification.ErrorMessages.HTTP_PUBLISH_TOO_MANY_REQUESTS.getCode(),
                        EmailNotification.ErrorMessages.HTTP_PUBLISH_TOO_MANY_REQUESTS.getMessage(), cause);
            case EmailNotification.AdapterErrorCodes.HTTP_PUBLISH_SERVER_ERROR:
                return new IdentityEventException(
                        EmailNotification.ErrorMessages.HTTP_PUBLISH_SERVER_ERROR.getCode(),
                        EmailNotification.ErrorMessages.HTTP_PUBLISH_SERVER_ERROR.getMessage(), cause);
            case EmailNotification.AdapterErrorCodes.HTTP_PUBLISH_SERVICE_UNAVAILABLE:
                return new IdentityEventException(
                        EmailNotification.ErrorMessages.HTTP_PUBLISH_SERVICE_UNAVAILABLE.getCode(),
                        EmailNotification.ErrorMessages.HTTP_PUBLISH_SERVICE_UNAVAILABLE.getMessage(),
                        cause);
            case EmailNotification.AdapterErrorCodes.HTTP_PUBLISH_FAILED_IO:
                return new IdentityEventException(
                        EmailNotification.ErrorMessages.HTTP_PUBLISH_FAILED_IO.getCode(),
                        EmailNotification.ErrorMessages.HTTP_PUBLISH_FAILED_IO.getMessage(), cause);
            case EmailNotification.AdapterErrorCodes.HTTP_TOKEN_REFRESH_MISSING_CREDS:
                return new IdentityEventException(
                        EmailNotification.ErrorMessages.HTTP_TOKEN_REFRESH_MISSING_CREDS.getCode(),
                        EmailNotification.ErrorMessages.HTTP_TOKEN_REFRESH_MISSING_CREDS.getMessage(),
                        cause);
            case EmailNotification.AdapterErrorCodes.HTTP_TOKEN_FETCH_FAILED:
                return new IdentityEventException(
                        EmailNotification.ErrorMessages.HTTP_TOKEN_FETCH_FAILED.getCode(),
                        EmailNotification.ErrorMessages.HTTP_TOKEN_FETCH_FAILED.getMessage(), cause);
            default:
                return new IdentityEventException(
                        EmailNotification.ErrorMessages.UNKNOWN_ERROR.getCode(),
                        EmailNotification.ErrorMessages.UNKNOWN_ERROR.getMessage(), cause);
        }
    }


    @Override
    public String getStreamDefinitionID(Event event) throws IdentityEventException {
        String streamDefinitionID = super.getStreamDefinitionID(event);
        if(StringUtils.isEmpty(streamDefinitionID)){
            streamDefinitionID = STREAM_ID ;
        }
        return streamDefinitionID;
    }

    @Override
    public String getName() {
        return "emailSend";
    }
}
