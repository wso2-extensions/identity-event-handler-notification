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

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.core.bean.context.MessageContext;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.bean.IdentityEventMessageContext;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.notification.internal.NotificationHandlerDataHolder;
import org.wso2.carbon.identity.event.handler.notification.util.NotificationUtil;
import org.wso2.carbon.identity.notification.push.provider.PushProvider;
import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderException;
import org.wso2.carbon.identity.notification.push.provider.model.PushNotificationData;
import org.wso2.carbon.identity.notification.push.provider.model.PushSenderData;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.PushSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.EmailNotification.ORGANIZATION_NAME_PLACEHOLDER;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.PushNotification.CHALLENGE;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.PushNotification.DEVICE_ID;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.PushNotification.DEVICE_TOKEN;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.PushNotification.IP_ADDRESS;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.PushNotification.NOTIFICATION_PROVIDER;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.PushNotification.NOTIFICATION_SCENARIO;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.PushNotification.NUMBER_CHALLENGE;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.PushNotification.PUSH_ID;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.PushNotification.PUSH_NOTIFICATION_EVENT;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.PushNotification.PUSH_NOTIFICATION_HANDLER_NAME;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.PushNotification.REQUEST_DEVICE_BROWSER;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.PushNotification.REQUEST_DEVICE_OS;
import static org.wso2.carbon.identity.event.handler.notification.util.NotificationUtil.extractPlaceHolders;

/**
 * This class represents the push notification event handler.
 */
public class PushNotificationHandler extends DefaultNotificationHandler {

    private static final Log LOG = LogFactory.getLog(PushNotificationHandler.class);

    @Override
    public boolean canHandle(MessageContext messageContext) throws IdentityRuntimeException {

        Event event = ((IdentityEventMessageContext) messageContext).getEvent();
        return event.getEventName().equals(PUSH_NOTIFICATION_EVENT);
    }

    @Override
    public String getName() {

        return PUSH_NOTIFICATION_HANDLER_NAME;
    }

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String tenantDomain = (String) event.getEventProperties().get(NotificationConstants.TENANT_DOMAIN);
        if (StringUtils.isBlank(tenantDomain)) {
            throw new IdentityEventException("Tenant domain is not found in the event properties.");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Handling push notification event for " + tenantDomain);
        }

        try {
            OrganizationManager organizationManager =
                    NotificationHandlerDataHolder.getInstance().getOrganizationManager();
            String organizationId = organizationManager.resolveOrganizationId(tenantDomain);
            event.getEventProperties().put(
                    NotificationConstants.EmailNotification.ORGANIZATION_ID_PLACEHOLDER, organizationId);
        } catch (OrganizationManagementException e) {
            throw new IdentityEventException(e.getMessage(), e);
        }

        if (!validatePushEventProperties(event)) {
            throw new IdentityEventException("Event properties validation failed to proceed with the " +
                    "push notification sending through provider.");
        }

        try {
            /*
             * Get the registered Push notification senders from the database. This is done to support multiple push
             * senders in the future. However, in the current implementation, only one push notification sender is
             * supported through the UI.
             */
            List<PushSenderDTO>  pushSenders = NotificationHandlerDataHolder.getInstance()
                    .getNotificationSenderManagementService().getPushSenders(true);
            if (pushSenders != null) {
                for (PushSenderDTO pushSenderDTO : pushSenders) {
                    // This is to get the supported push providers. We can include push providers through OSGi.
                    PushProvider provider = NotificationHandlerDataHolder.getInstance()
                            .getPushProvider(pushSenderDTO.getProvider());
                    if (provider == null) {
                        throw new IdentityEventException("No Push notification provider found for the name: "
                                + pushSenderDTO.getName());
                    }
                    String registeredProvider = (String) event.getEventProperties().get(NOTIFICATION_PROVIDER);
                    if (!registeredProvider.equalsIgnoreCase(pushSenderDTO.getProvider())) {
                        throw new IdentityEventException("User is not registered to the Push notification provider: "
                                + pushSenderDTO.getName());
                    }

                    PushNotificationData pushNotificationData = buildPushNotificationData(event);
                    provider.sendNotification(pushNotificationData, buildPushSenderData(pushSenderDTO), tenantDomain);
                }
            }
        } catch (NotificationSenderManagementException e) {
            throw new IdentityEventException("Error while retrieving SMS Sender: "
                    + NotificationSenderManagementConstants.DEFAULT_PUSH_PUBLISHER, e);
        } catch (PushProviderException e) {
            throw new IdentityEventException(e.getErrorCode(), e.getMessage(), e);
        }
    }

    /**
     * Validate the event properties before sending the push notification.
     *
     * @param event Event object.
     * @return True if the event properties are valid.
     */
    private boolean validatePushEventProperties(Event event) {

        Map<String, Object> eventProperties = event.getEventProperties();
        if (eventProperties == null) {
            return false;
        }
        String[] requiredProperties = {NOTIFICATION_SCENARIO, NOTIFICATION_PROVIDER, DEVICE_TOKEN};
        for (String property : requiredProperties) {
            if (eventProperties.get(property) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Build the push notification data object.
     *
     * @param event Event object.
     * @return PushNotificationData object.
     * @throws IdentityEventException If an error occurs while building the push notification data.
     */
    private PushNotificationData buildPushNotificationData(Event event) throws IdentityEventException {

        Map<String, Object> eventProperties = event.getEventProperties();

        String scenario = (String) eventProperties.get(NOTIFICATION_SCENARIO);

        NotificationConstants.PushNotificationTemplate template =
                Arrays.stream(NotificationConstants.PushNotificationTemplate.values())
                        .filter(pushNotificationTemplate -> pushNotificationTemplate.getScenario().equals(scenario))
                        .findFirst()
                        .orElseThrow(() -> new IdentityEventException(
                                "Push notification template not found for the scenario: " + scenario));

        Set<String> placeHolderSet = new HashSet<>();
        placeHolderSet.addAll(extractPlaceHolders(template.getTitle()));
        placeHolderSet.addAll(extractPlaceHolders(template.getBody()));

        // Validate if there are invalid placeholders.
        validatePlaceHolders(placeHolderSet);

        // Retrieve the placeholder values from the event properties.
        Map<String, String> placeholderValues = getPlaceHolderValues(eventProperties, placeHolderSet);

        // Resolve the organization name placeholder.
        String tenantDomain = placeholderValues.get(
                NotificationConstants.PushNotificationPlaceholder.TENANT_DOMAIN.name());
        if (StringUtils.isEmpty(tenantDomain)) {
            tenantDomain = (String) eventProperties.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN);
        }
        if (placeholderValues.containsKey(ORGANIZATION_NAME_PLACEHOLDER)) {
            String organizationName = NotificationUtil.resolveHumanReadableOrganizationName(tenantDomain);
            placeholderValues.put(ORGANIZATION_NAME_PLACEHOLDER, organizationName);
        }

        // Replace the placeholders in the push notification template with the actual values.
        String title = replacePlaceHolders(template.getTitle(), placeholderValues);
        String body = replacePlaceHolders(template.getBody(), placeholderValues);

        return new PushNotificationData.Builder()
                .setNotificationTitle(title)
                .setNotificationBody(body)
                .setUsername((String) eventProperties.get(IdentityEventConstants.EventProperty.USER_NAME))
                .setTenantDomain((String) eventProperties.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN))
                .setUserStoreDomain((String) eventProperties.get(
                        IdentityEventConstants.EventProperty.USER_STORE_DOMAIN))
                .setApplicationName((String) eventProperties.get(IdentityEventConstants.EventProperty.APPLICATION_NAME))
                .setNotificationScenario((String) eventProperties.get(NOTIFICATION_SCENARIO))
                .setPushId((String) eventProperties.get(PUSH_ID))
                .setDeviceToken((String) eventProperties.get(DEVICE_TOKEN))
                .setDeviceId((String) eventProperties.get(DEVICE_ID))
                .setChallenge((String) eventProperties.get(CHALLENGE))
                .setNumberChallenge((String) eventProperties.get(NUMBER_CHALLENGE))
                .setIpAddress((String) eventProperties.get(IP_ADDRESS))
                .setDeviceOS((String) eventProperties.get(REQUEST_DEVICE_OS))
                .setBrowser((String) eventProperties.get(REQUEST_DEVICE_BROWSER))
                .build();
    }

    /**
     * Validate the placeholders in the push notification template.
     *
     * @param placeHolderSet Set of placeholders.
     * @throws IdentityEventException If an invalid placeholder is found.
     */
    private void validatePlaceHolders(Set<String> placeHolderSet) throws IdentityEventException {

        Set<String> validPlaceHolders = Arrays.stream(NotificationConstants.PushNotificationPlaceholder.values())
                .map(NotificationConstants.PushNotificationPlaceholder::getPlaceholder).collect(Collectors.toSet());

        for (String placeHolder : placeHolderSet) {
            if (!validPlaceHolders.contains(placeHolder)) {
                throw new IdentityEventException("Invalid placeholder found: " + placeHolder);
            }
        }
    }

    /**
     * Get the placeholder values from the event properties.
     *
     * @param eventProperties Event properties.
     * @param placeholderSet  Set of placeholders.
     * @return Map of placeholder values.
     */
    private Map<String, String> getPlaceHolderValues(Map<String, Object> eventProperties, Set<String> placeholderSet) {

        NotificationConstants.PushNotificationPlaceholder[] placeHolderKeys =
                NotificationConstants.PushNotificationPlaceholder.values();
        Map<String, String> placeholderValues = new HashMap<>();

        for (String placeHolder : placeholderSet) {
            NotificationConstants.PushNotificationPlaceholder matchedKey = Arrays.stream(placeHolderKeys)
                    .filter(key -> key.getPlaceholder().equalsIgnoreCase(placeHolder))
                    .findFirst()
                    .orElse(null);

            // If a matching key is found, retrieve the corresponding value from event properties
            if (matchedKey != null) {
                String value = (String) eventProperties.get(matchedKey.getPlaceholder());
                placeholderValues.put(placeHolder, value);
            }
        }

        return placeholderValues;
    }

    /**
     * Replace the placeholders in the push notification template with the actual values.
     *
     * @param content           Push notification template content.
     * @param placeholderValues Map of placeholder values.
     * @return Push notification content with placeholders replaced.
     */
    private String replacePlaceHolders(String content, Map<String, String> placeholderValues) {

        for (Map.Entry<String, String> entry : placeholderValues.entrySet()) {
            String placeholder = entry.getKey();
            String value = entry.getValue();
            content = content.replace("{{" + placeholder + "}}", value);
        }
        return content;
    }

    /**
     * Build PushSenderData from PushSenderDTO.
     *
     * @param pushSenderDTO PushSender DTO.
     * @return PushSenderData.
     */
    private static PushSenderData buildPushSenderData(PushSenderDTO pushSenderDTO) {

        PushSenderData pushSenderData = new PushSenderData();
        pushSenderData.setName(pushSenderDTO.getName());
        pushSenderData.setProvider(pushSenderDTO.getProvider());
        pushSenderData.setProperties(pushSenderDTO.getProperties());
        pushSenderData.setProviderId(pushSenderDTO.getProviderId());
        return pushSenderData;
    }
}
