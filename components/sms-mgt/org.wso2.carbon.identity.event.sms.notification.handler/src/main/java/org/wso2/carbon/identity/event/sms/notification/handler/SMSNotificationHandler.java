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

package org.wso2.carbon.identity.event.sms.notification.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.notification.DefaultNotificationHandler;
import org.wso2.carbon.identity.event.sms.notification.handler.internal.SMSNotificationHandlerDataHolder;
import org.wso2.carbon.identity.event.sms.notification.handler.model.SMSOTPEventPayload;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.SMSSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wso2.carbon.identity.event.sms.notification.handler.SMSNotificationConstants.NOTIFICATION_HANDLER_NAME;
import static org.wso2.carbon.identity.event.sms.notification.handler.SMSNotificationConstants.SCIM2_ENDPOINT;
import static org.wso2.carbon.identity.event.sms.notification.handler.SMSNotificationConstants.SMS_MASSAGE_TO_NAME;
import static org.wso2.carbon.identity.event.sms.notification.handler.SMSNotificationConstants.SMS_MESSAGE_BODY_NAME;
import static org.wso2.carbon.identity.event.sms.notification.handler.SMSNotificationConstants.SMS_NOTIFICATION_EVENT_URI;
import static org.wso2.carbon.identity.event.sms.notification.handler.SMSNotificationConstants.SMS_NOTIFICATION_HUB_TOPIC_SUFFIX;
import static org.wso2.carbon.identity.event.sms.notification.handler.SMSNotificationConstants.TENANT_DOMAIN_NAME;
import static org.wso2.carbon.identity.event.sms.notification.handler.SMSNotificationConstants.TENANT_SEPARATOR;
import static org.wso2.carbon.identity.event.sms.notification.handler.SMSNotificationConstants.USERS;
import static org.wso2.carbon.identity.event.sms.notification.handler.SMSNotificationConstants.USER_ID_CLAIM_URI;
import static org.wso2.carbon.identity.event.sms.notification.handler.SMSNotificationConstants.USER_NAME;

/**
 * Handler class for SMS Notifications.
 */
public class SMSNotificationHandler extends DefaultNotificationHandler {

    private static final Log log = LogFactory.getLog(SMSNotificationHandler.class);

    /**
     * Retrieve the reference of the given user.
     *
     * @param tenantDomain Tenant Domain.
     * @param userId       User id.
     * @return User location.
     */
    public static String getUserReference(String tenantDomain, String userId) {

        StringBuilder userReference = new StringBuilder(getSCIMURL(tenantDomain)).append(USERS).append(userId);
        return userReference.toString();
    }

    private static String getSCIMURL(String tenantDomain) {

        String scimURL;
        try {
            if (IdentityTenantUtil.isTenantQualifiedUrlsEnabled()) {
                scimURL = ServiceURLBuilder.create().addPath(SCIM2_ENDPOINT).build().getAbsolutePublicURL();
            } else {
                String serverUrl = ServiceURLBuilder.create().build().getAbsolutePublicURL();
                if (isNotASuperTenantFlow(tenantDomain)) {
                    scimURL = serverUrl + TENANT_SEPARATOR + tenantDomain + SCIM2_ENDPOINT;
                } else {
                    scimURL = serverUrl + SCIM2_ENDPOINT;
                }
            }
            return scimURL;
        } catch (URLBuilderException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error occurred while building the SCIM2 endpoint with tenant " +
                        "qualified URL.", e);
            }
            // Fallback to legacy approach during error scenarios to maintain backward compatibility.
            return getSCIMURLLegacy(tenantDomain);
        }
    }

    private static String getSCIMURLLegacy(String tenantDomain) {

        String scimURL;
        if (isNotASuperTenantFlow(tenantDomain)) {
            scimURL = IdentityUtil.getServerURL(TENANT_SEPARATOR + tenantDomain + SCIM2_ENDPOINT, true, true);
        } else {
            scimURL = IdentityUtil.getServerURL(SCIM2_ENDPOINT, true, true);
        }
        return scimURL;
    }

    private static boolean isNotASuperTenantFlow(String tenantDomain) {

        return !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain);
    }

    /**
     * Return user store manager.
     *
     * @param tenantDomain Tenant Domain.
     * @return User store manager of the given tenant domain.
     * @throws IdentityEventException
     */
    public static UserStoreManager getUserStoreManager(String tenantDomain) throws IdentityEventException {

        RealmService realmService = SMSNotificationHandlerDataHolder.getInstance().getRealmService();

        int tenantId;
        try {
            tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            throw new IdentityEventException("Failed to retrieve tenant id from tenant domain : " + tenantDomain, e);
        }

        if (MultitenantConstants.INVALID_TENANT_ID == tenantId) {
            throw new IdentityEventException("Invalid tenant domain : " + tenantDomain);
        }

        UserRealm userRealm;
        try {
            userRealm = (UserRealm) realmService.getTenantUserRealm(tenantId);
        } catch (UserStoreException e) {
            throw new IdentityEventException("Failed to retrieve user realm from tenant id : " + tenantId, e);
        }

        UserStoreManager userStoreManager;
        try {
            userStoreManager = userRealm.getUserStoreManager();
        } catch (UserStoreException e) {
            throw new IdentityEventException("Failed to retrieve user store manager.", e);
        }
        return userStoreManager;
    }

    @Override
    public String getName() {

        return NOTIFICATION_HANDLER_NAME;
    }

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        Map<String, String> arbitraryDataMap = buildNotificationData(event);

        if (isTenantWiseSMSProviderAvailable()) {
            SMSOTPEventPayload smsOtpEventPayload = constructSMSOTPPayload(arbitraryDataMap);
            Map<String, String> dataMap = constructSMSOTPDataMap(event);
            SMSNotificationHandlerDataHolder.getWebSubHubEventAdapterService().publish(smsOtpEventPayload,
                    SMS_NOTIFICATION_HUB_TOPIC_SUFFIX, SMS_NOTIFICATION_EVENT_URI,
                    dataMap);
        } else {
            publishToStream(arbitraryDataMap, event);
        }
    }

    /**
     * Check the availability of tenant specific SMS publisher configurations.
     *
     * @return boolean.
     */
    private boolean isTenantWiseSMSProviderAvailable() {

        //TODO improve this logic along with the configuration API changes.
        try {
            List<SMSSenderDTO> smsSenders = SMSNotificationHandlerDataHolder.getNotificationSenderManagementService()
                    .getSMSSenders();
            if (smsSenders.size() > 0) {
                return true;
            }
        } catch (NotificationSenderManagementException e) {
            //TODO handle exception
        }
        return false;
    }

    /**
     * Construct SMS OTP Event Payload.
     *
     * @param eventProperties Properties of the event.
     * @return SMS OTP Event Payload.
     * @throws IdentityEventException
     */
    private SMSOTPEventPayload constructSMSOTPPayload(Map<String, String> eventProperties)
            throws IdentityEventException {

        SMSOTPEventPayload smsotpEventPayload = new SMSOTPEventPayload();

        smsotpEventPayload.setMessageBody(eventProperties.get(SMS_MESSAGE_BODY_NAME));
        smsotpEventPayload.setSendTo(eventProperties.get(SMS_MASSAGE_TO_NAME));

        String username = eventProperties.get(USER_NAME);
        smsotpEventPayload.setUserName(username);

        String tenantDomain = eventProperties.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN);
        String userStoreDomain = eventProperties.get(IdentityEventConstants.EventProperty.USER_STORE_DOMAIN);
        String domainQualifiedUsername = IdentityUtil.addDomainToName(username, userStoreDomain);

        UserStoreManager userStoreManager = getUserStoreManager(tenantDomain);

        String userId;
        try {
            userId = userStoreManager.getUserClaimValue(domainQualifiedUsername, USER_ID_CLAIM_URI,
                    UserCoreConstants.DEFAULT_PROFILE);

        } catch (UserStoreException e) {
            throw new IdentityEventException("Error while extracting userid for the user: " + username, e);
        }

        smsotpEventPayload.setUserId(userId);
        smsotpEventPayload.setRef(getUserReference(tenantDomain, userId));
        smsotpEventPayload.setOrganizationId(IdentityTenantUtil.getTenantId(tenantDomain));
        smsotpEventPayload.setOrganizationName(tenantDomain);
        return smsotpEventPayload;
    }

    /**
     * Construct a map of required properties by the web sub hub adapter.
     *
     * @param event SMS Notification Event .
     * @return Property Map.
     */
    private Map<String, String> constructSMSOTPDataMap(Event event) {

        List<String> requiredDataMapAttributes = new ArrayList<>();
        requiredDataMapAttributes.add(TENANT_DOMAIN_NAME);

        Map<String, Object> eventProperties = event.getEventProperties();
        Map<String, String> dataMap = new HashMap<>();

        requiredDataMapAttributes.forEach(attribute ->
                dataMap.put(attribute, (String) eventProperties.get(attribute)));

        return dataMap;
    }
}
