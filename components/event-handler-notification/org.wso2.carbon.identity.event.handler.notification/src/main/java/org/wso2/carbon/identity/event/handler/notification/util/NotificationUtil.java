/*
 * Copyright (c) 2016, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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

package org.wso2.carbon.identity.event.handler.notification.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.axiom.om.OMElement;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.event.publisher.core.EventPublisherService;
import org.wso2.carbon.event.publisher.core.config.EventPublisherConfiguration;
import org.wso2.carbon.event.publisher.core.exception.EventPublisherConfigurationException;
import org.wso2.carbon.event.stream.core.EventStreamService;
import org.wso2.carbon.event.stream.core.exception.EventStreamConfigurationException;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.branding.preference.management.core.BrandingPreferenceManager;
import org.wso2.carbon.identity.branding.preference.management.core.BrandingPreferenceManagerImpl;
import org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.model.BrandingPreference;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.util.IdentityConfigParser;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.notification.NotificationConstants;
import org.wso2.carbon.identity.event.handler.notification.email.bean.Notification;
import org.wso2.carbon.identity.event.handler.notification.exception.NotificationRuntimeException;
import org.wso2.carbon.identity.event.handler.notification.internal.NotificationHandlerDataHolder;
import org.wso2.carbon.identity.governance.model.UserIdentityClaim;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.user.api.Claim;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.EmailNotification.ACCOUNT_RECOVERY_ENDPOINT_PLACEHOLDER;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.EmailNotification.AUTHENTICATION_ENDPOINT_PLACEHOLDER;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.EmailNotification.BRANDING_PREFERENCES_COPYRIGHT_TEXT_PATH;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.EmailNotification.BRANDING_PREFERENCES_LOGO_ALTTEXT_PATH;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.EmailNotification.BRANDING_PREFERENCES_LOGO_URL_PATH;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.EmailNotification.BRANDING_PREFERENCES_SUPPORT_EMAIL_PATH;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.EmailNotification.CARBON_PRODUCT_URL_TEMPLATE_PLACEHOLDER;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.EmailNotification.CARBON_PRODUCT_URL_WITH_USER_TENANT_TEMPLATE_PLACEHOLDER;
import static org.wso2.carbon.identity.event.handler.notification.NotificationConstants.EmailNotification.ORGANIZATION_NAME_PLACEHOLDER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT;

public class NotificationUtil {

    private static final Log log = LogFactory.getLog(NotificationUtil.class);

    private static final String USER_IDENTITY_CLAIMS = "UserIdentityClaims";

    public static Map<String, String> getUserClaimValues(String userName, UserStoreManager userStoreManager) {

        Claim[] userClaims;
        Map<String, String> claimsMap = new HashMap<String, String>();
        try {
            userClaims = userStoreManager.getUserClaimValues(userName, UserCoreConstants.DEFAULT_PROFILE);
            if (userClaims != null) {
                for (Claim userClaim : userClaims) {
                    claimsMap.put(userClaim.getClaimUri(), userClaim.getValue());
                }
            }
            UserIdentityClaim userIdentityClaims =
                    (UserIdentityClaim) IdentityUtil.threadLocalProperties.get().get(USER_IDENTITY_CLAIMS);
            Map<String, String> userIdentityDataMap;
            if (userIdentityClaims == null) {
                userIdentityDataMap = new HashMap<>();
            } else {
                userIdentityDataMap = userIdentityClaims.getUserIdentityDataMap();
            }
            for (String key : userIdentityDataMap.keySet()) {
                if (!claimsMap.containsKey(key)) {
                    claimsMap.put(key, userIdentityDataMap.get(key));
                }
            }
        } catch (UserStoreException e) {
            String domainNameProperty = getUserStoreDomainName(userStoreManager);
            String message = null;
            if (StringUtils.isNotBlank(domainNameProperty)) {
                message = "Error occurred while retrieving user claim values for user " + userName + " in user store "
                        + domainNameProperty + " in tenant " + getTenantDomain(userStoreManager);
            } else {
                message = "Error occurred while retrieving user claim values for user " + userName + " in tenant "
                        + getTenantDomain(userStoreManager);
            }
            log.error(message, e);
        }

        return claimsMap;
    }

    public static Map<String, String> getUserClaimValues(String userName, String domainName, String tenantDomain)
            throws IdentityEventException {

        RealmService realmService = NotificationHandlerDataHolder.getInstance().getRealmService();
        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        UserStoreManager userStoreManager = null;
        try {
            userStoreManager = realmService.getTenantUserRealm(tenantId).getUserStoreManager();
            if (userStoreManager == null) {
                String message = "Error occurred while retrieving userStoreManager for tenant " + tenantDomain;
                throw new IdentityEventException(message);
            } else if (userStoreManager instanceof AbstractUserStoreManager) {
                userStoreManager = ((AbstractUserStoreManager) userStoreManager).getSecondaryUserStoreManager(domainName);
            }
        } catch (UserStoreException e) {
            String message = "Error occurred while retrieving user claim values for user " + userName + " in user " +
                    "store " + domainName + " in tenant " + tenantDomain;
            throw new IdentityEventException(message, e);
        }
        return getUserClaimValues(userName, userStoreManager);
    }

    /**
     * Set place holder values for email templates.
     *
     * @param emailTemplate   {@link org.wso2.carbon.email.mgt.model.EmailTemplate}
     * @param placeHolderData List of place holder data
     * @param userClaims      List of user claims
     * @return Place holder data
     */
    public static Map<String, String> getPlaceholderValues(EmailTemplate emailTemplate,
                                                           Map<String, String> placeHolderData, Map<String, String> userClaims) {

        Map<String, String> configFilePlaceholders = getConfigFilePlaceholders();

        JsonNode brandingPreferences = null;
        Map<String, String> brandingFallbacks = getBrandingFallbacksFromConfigFile();

        if (Boolean.parseBoolean(
                IdentityUtil.getProperty(NotificationConstants.EmailNotification.ENABLE_ORGANIZATION_LEVEL_EMAIL_BRANDING))) {
            try {
                BrandingPreferenceManager brandingPreferenceManager = new BrandingPreferenceManagerImpl();
                BrandingPreference responseDTO = brandingPreferenceManager.resolveBrandingPreference(
                        BrandingPreferenceMgtConstants.ORGANIZATION_TYPE,
                        placeHolderData.get("tenant-domain"),
                        BrandingPreferenceMgtConstants.DEFAULT_LOCALE);

                ObjectMapper objectMapper = new ObjectMapper();
                String json = objectMapper.writeValueAsString(responseDTO.getPreference());
                brandingPreferences = objectMapper.readTree(json);

                if (!brandingPreferences.at(NotificationConstants.EmailNotification.BRANDING_PREFERENCES_IS_ENABLED_PATH)
                        .asBoolean()) {
                    brandingPreferences = null;
                }
            } catch (BrandingPreferenceMgtException e) {
                if (BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_BRANDING_PREFERENCE_NOT_EXISTS.getCode()
                        .equals(e.getErrorCode())) {
                    brandingPreferences = null;
                } else {
                    if (log.isDebugEnabled()) {
                        String message = "Error occurred while retrieving branding preferences for organization " + placeHolderData.get("tenant-domain");
                        log.debug(message, e);
                    }
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    String message = "Error occurred while retrieving branding preferences for organization " + placeHolderData.get("tenant-domain");
                    log.debug(message, e);
                }
            }
        }

        // Having a body is mandatory.
        List<String> placeHolders = new ArrayList<>(extractPlaceHolders(emailTemplate.getBody()));
        if (StringUtils.isNotEmpty(emailTemplate.getSubject())) {
            placeHolders.addAll(extractPlaceHolders(emailTemplate.getSubject()));
        }
        if (StringUtils.isNotEmpty(emailTemplate.getFooter())) {
            placeHolders.addAll(extractPlaceHolders(emailTemplate.getFooter()));
        }
        Set<String> placeHoldersSet = new HashSet<>(placeHolders);

        for (String placeHolder : placeHoldersSet) {
            // Setting config file place holders.
            if (placeHolder.startsWith(NotificationConstants.EmailNotification.IDENTITY_TEMPLATE_VALUE_PREFIX)) {
                String key = placeHolder.substring(placeHolder.lastIndexOf(".") + 1);
                String value = configFilePlaceholders.getOrDefault(key, "");
                placeHolderData.put(placeHolder, value);
            }

            // Setting branding placeholders.
            String brandingValue = getBrandingPreference(placeHolder, brandingPreferences, brandingFallbacks);
            if (brandingValue != null) {
                placeHolderData.put(placeHolder, brandingValue);
            }

            if (userClaims != null && !userClaims.isEmpty()) {
                if (placeHolder.contains(NotificationConstants.EmailNotification.USER_CLAIM_PREFIX + "."
                        + NotificationConstants.EmailNotification.IDENTITY_CLAIM_PREFIX)) {
                    String identityClaim = userClaims.get(NotificationConstants.EmailNotification.WSO2_CLAIM_URI
                            + NotificationConstants.EmailNotification.IDENTITY_CLAIM_PREFIX + "/" + placeHolder
                            .substring(placeHolder.indexOf(".", placeHolder.indexOf("identity")) + 1));
                    if (StringUtils.isNotEmpty(identityClaim)) {
                        placeHolderData.put(placeHolder, identityClaim);
                    } else {
                        placeHolderData.put(placeHolder, "");
                    }
                } else if (placeHolder.contains(NotificationConstants.EmailNotification.USER_CLAIM_PREFIX)) {
                    String userClaim = userClaims
                            .get(NotificationConstants.EmailNotification.WSO2_CLAIM_URI + placeHolder
                                    .substring(placeHolder.indexOf(".", placeHolder.indexOf("claim")) + 1));
                    if (StringUtils.isNotEmpty(userClaim)) {
                        placeHolderData.put(placeHolder, userClaim);
                    } else {
                        placeHolderData.put(placeHolder, "");
                    }
                }
            }
        }
        // Building the server url.
        String serverURL;
        String carbonUrlWithUserTenant;
        String accountRecoveryEndpointURL = ConfigurationFacade.getInstance().getAccountRecoveryEndpointPath();
        String authenticationEndpointURL = ConfigurationFacade.getInstance().getAuthenticationEndpointURL();
        try {
            serverURL = ServiceURLBuilder.create().build().getAbsolutePublicURL();
            carbonUrlWithUserTenant = ServiceURLBuilder.create().build().getAbsolutePublicUrlWithoutPath();
            if (IdentityTenantUtil.isTenantQualifiedUrlsEnabled()) {
                carbonUrlWithUserTenant = ServiceURLBuilder.create().build().getAbsolutePublicUrlWithoutPath() + "/t" +
                        "/" + placeHolderData.get("tenant-domain");
            }
        } catch (URLBuilderException e) {
            throw NotificationRuntimeException.error("Error while building the server url.", e);
        }

        placeHolderData.put(ACCOUNT_RECOVERY_ENDPOINT_PLACEHOLDER, accountRecoveryEndpointURL);
        placeHolderData.put(AUTHENTICATION_ENDPOINT_PLACEHOLDER, authenticationEndpointURL);
        placeHolderData.put(CARBON_PRODUCT_URL_TEMPLATE_PLACEHOLDER, serverURL);
        placeHolderData.put(CARBON_PRODUCT_URL_WITH_USER_TENANT_TEMPLATE_PLACEHOLDER, carbonUrlWithUserTenant);
        return placeHolderData;
    }

    public static Map<String, String> getConfigFilePlaceholders() {

        IdentityConfigParser configParser = IdentityConfigParser.getInstance();
        OMElement placeHolderElem = configParser.getConfigElement(
                NotificationConstants.EmailNotification.TEMPLATE_PLACEHOLDERS_ELEM);
        if (placeHolderElem == null) {
            return Collections.emptyMap();
        }

        Iterator iterator = placeHolderElem.getChildrenWithLocalName(
                NotificationConstants.EmailNotification.TEMPLATE_PLACEHOLDER_ELEM);
        if (iterator == null) {
            return Collections.emptyMap();
        }

        Map<String, String> placeholderMap = new HashMap<>();
        while (iterator.hasNext()) {
            OMElement omElement = (OMElement) iterator.next();
            if (omElement != null) {
                String key = omElement.getAttributeValue(
                        new QName(NotificationConstants.EmailNotification.TEMPLATE_PLACEHOLDER_KEY_ATTRIB));
                String value = omElement.getText();
                placeholderMap.put(key, value);
            }
        }
        return placeholderMap;
    }

    public static List<String> extractPlaceHolders(String value) {

        String exp = "\\{\\{(.*?)\\}\\}";
        Pattern pattern = Pattern.compile(exp);
        Matcher matcher = pattern.matcher(value);
        List<String> placeHolders = new ArrayList<>();
        while (matcher.find()) {
            String group = matcher.group().replace("{{", "").replace("}}", "");
            placeHolders.add(group);
        }
        return placeHolders;
    }

    public static String getUserStoreDomainName(UserStoreManager userStoreManager) {

        String domainNameProperty = null;
        if (userStoreManager instanceof org.wso2.carbon.user.core.UserStoreManager) {
            domainNameProperty = ((org.wso2.carbon.user.core.UserStoreManager)
                    userStoreManager).getRealmConfiguration()
                    .getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME);
            if (StringUtils.isBlank(domainNameProperty)) {
                domainNameProperty = IdentityUtil.getPrimaryDomainName();
            }
        }
        return domainNameProperty;
    }

    public static String getTenantDomain(UserStoreManager userStoreManager) {

        try {
            return IdentityTenantUtil.getTenantDomain(userStoreManager.getTenantId());
        } catch (UserStoreException e) {
            throw NotificationRuntimeException.error("Error when getting the tenant domain.", e);
        }
    }

    public static void deployStream(String streamName, String streamVersion, String streamId)
            throws NotificationRuntimeException {

        try {
            EventStreamService service = NotificationHandlerDataHolder.getInstance().getEventStreamService();
            StreamDefinition streamDefinition = new StreamDefinition(streamName, streamVersion, streamId);
            service.addEventStreamDefinition(streamDefinition);
        } catch (MalformedStreamDefinitionException e) {
            throw NotificationRuntimeException.error("Error occurred due to a malformed stream definition.", e);
        } catch (EventStreamConfigurationException e) {
            throw NotificationRuntimeException.error("Error in deploying a stream.", e);
        }
    }

    public static void deployPublisher(EventPublisherConfiguration eventPublisherConfiguration) throws NotificationRuntimeException {

        EventPublisherService eventPublisherService = NotificationHandlerDataHolder.getInstance().getEventPublisherService();
        try {
            eventPublisherService.deployEventPublisherConfiguration(eventPublisherConfiguration);
        } catch (EventPublisherConfigurationException e) {
            throw NotificationRuntimeException.error("Error in deploying a publisher.", e);
        }
    }

    /**
     * Retrieve default organization level branding configs.
     *
     * @return map of default organization level branding configs.
     */
    public static Map<String, String> getBrandingFallbacksFromConfigFile() {

        IdentityConfigParser configParser = IdentityConfigParser.getInstance();
        OMElement fallbackElem = configParser.getConfigElement(
                NotificationConstants.EmailNotification.ORGANIZATION_LEVEL_EMAIL_BRANDING_FALLBACKS_ELEM);
        if (fallbackElem == null) {
            return Collections.emptyMap();
        }

        Iterator iterator = fallbackElem.getChildrenWithLocalName(
                NotificationConstants.EmailNotification.ORGANIZATION_LEVEL_EMAIL_BRANDING_FALLBACK_ELEM);
        if (iterator == null) {
            return Collections.emptyMap();
        }

        Map<String, String> fallbackMap = new HashMap<>();
        while (iterator.hasNext()) {
            OMElement omElement = (OMElement) iterator.next();
            if (omElement != null) {
                String key = omElement.getAttributeValue(
                        new QName(NotificationConstants.EmailNotification.ORGANIZATION_LEVEL_EMAIL_BRANDING_FALLBACK_KEY_ATTRIBUTE));
                String value = omElement.getText();
                fallbackMap.put(key, value);
            }
        }
        return fallbackMap;
    }

    /**
     * Retrive branding value by the placeholder.
     *
     * @param key                   placeholder in the email template.
     * @param brandingPreferences   list of branding preferences.
     * @param brandingFallbacks     default branding values.
     * @return map of default organization branding.
     */
    public static String getBrandingPreference(String key, JsonNode brandingPreferences, Map<String, String> brandingFallbacks) {

        String value = null;
        boolean brandingIsEnabled = (brandingPreferences != null)
                && brandingPreferences.at(NotificationConstants.EmailNotification.BRANDING_PREFERENCES_IS_ENABLED_PATH).asBoolean();
        String theme = brandingIsEnabled
                ? brandingPreferences.at("/theme/activeTheme").asText()
                : NotificationConstants.EmailNotification.BRANDING_PREFERENCES_LIGHT_THEME;

        switch (key) {
            case "organization.logo.img" :
                if (brandingIsEnabled && StringUtils.isNotBlank(
                        getBrandingPreferenceByTheme(brandingPreferences, theme, BRANDING_PREFERENCES_LOGO_URL_PATH))) {
                    value = getBrandingPreferenceByTheme(brandingPreferences, theme, BRANDING_PREFERENCES_LOGO_URL_PATH);
                } else {
                    value = (theme.equals(NotificationConstants.EmailNotification.BRANDING_PREFERENCES_LIGHT_THEME))
                            ? brandingFallbacks.get("light_logo_url")
                            : brandingFallbacks.get("dark_logo_url");
                }
                break;
            case "organization.logo.altText" :
                value = (brandingIsEnabled && StringUtils.isNotBlank(
                            getBrandingPreferenceByTheme(brandingPreferences, theme, BRANDING_PREFERENCES_LOGO_ALTTEXT_PATH)))
                        ? getBrandingPreferenceByTheme(brandingPreferences, theme, BRANDING_PREFERENCES_LOGO_ALTTEXT_PATH)
                        : StringUtils.EMPTY;
                break;
            case "organization.copyright.text" :
                value = (brandingIsEnabled && StringUtils.isNotBlank(
                            brandingPreferences.at(BRANDING_PREFERENCES_COPYRIGHT_TEXT_PATH).asText()))
                        ? brandingPreferences.at(BRANDING_PREFERENCES_COPYRIGHT_TEXT_PATH).asText()
                        : brandingFallbacks.get("copyright_text")
                            .replace("YYYY", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
                break;
            case "organization.support.mail" :
                value = (brandingIsEnabled && StringUtils.isNotBlank(
                            brandingPreferences.at(BRANDING_PREFERENCES_SUPPORT_EMAIL_PATH).asText()))
                        ? brandingPreferences.at(BRANDING_PREFERENCES_SUPPORT_EMAIL_PATH).asText()
                        : brandingFallbacks.get("support_mail");
                break;
            case "organization.color.primary" :
                value = brandingIsEnabled
                        ? !StringUtils.isBlank(getBrandingPreferenceByTheme(brandingPreferences, theme, "/colors/primary/main"))
                            ? getBrandingPreferenceByTheme(brandingPreferences, theme, "/colors/primary/main")
                            : getBrandingPreferenceByTheme(brandingPreferences, theme, "/colors/primary")
                        : brandingFallbacks.get("primary_color");
                break;
            case "organization.color.background" :
                value = brandingIsEnabled
                        ? !StringUtils.isBlank(getBrandingPreferenceByTheme(brandingPreferences, theme, "/colors/background/body/main"))
                            ? getBrandingPreferenceByTheme(brandingPreferences, theme, "/colors/background/body/main")
                            : getBrandingPreferenceByTheme(brandingPreferences, theme, "/page/background/backgroundColor")
                        : brandingFallbacks.get("background_color");
                break;
            case "organization.font" :
                value = brandingIsEnabled
                        ? getBrandingPreferenceByTheme(brandingPreferences, theme, "/typography/font/fontFamily")
                        : brandingFallbacks.get("font_style");
                break;
            case "organization.font.color" :
                value = brandingIsEnabled
                        ? !StringUtils.isBlank(getBrandingPreferenceByTheme(brandingPreferences, theme, "/colors/text/primary"))
                            ? getBrandingPreferenceByTheme(brandingPreferences, theme, "/colors/text/primary")
                            : getBrandingPreferenceByTheme(brandingPreferences, theme, "/page/font/color")
                        : brandingFallbacks.get("font_color");
                break;
            case "organization.button.font.color" :
                value = brandingIsEnabled
                        ? getBrandingPreferenceByTheme(brandingPreferences, theme, "/buttons/primary/base/font/color")
                        : brandingFallbacks.get("button_font_color");
                break;
            case "organization.theme.background.color" :
                value = brandingIsEnabled && !StringUtils.isBlank(getBrandingPreferenceByTheme(brandingPreferences, theme, "/colors/background/surface/main"))
                            ? getBrandingPreferenceByTheme(brandingPreferences, theme, "/colors/background/surface/main")
                            : theme.equals(NotificationConstants.EmailNotification.BRANDING_PREFERENCES_LIGHT_THEME)
                                ? brandingFallbacks.get("light_background_color")
                                : brandingFallbacks.get("dark_background_color");
                break;
            case "organization.theme.border.color" :
                value = brandingIsEnabled && !StringUtils.isBlank(getBrandingPreferenceByTheme(brandingPreferences, theme, "/colors/outlined/default"))
                        ? getBrandingPreferenceByTheme(brandingPreferences, theme, "/colors/outlined/default")
                        : theme.equals(NotificationConstants.EmailNotification.BRANDING_PREFERENCES_LIGHT_THEME)
                            ? brandingFallbacks.get("light_border_color")
                            : brandingFallbacks.get("dark_border_color");
                break;
            default: break;
        }

        return value;
    }

    private static String getBrandingPreferenceByTheme(JsonNode brandingPreferences, String theme, String path) {

        return brandingPreferences.at("/theme/" + theme + path).asText();
    }

    public static Notification buildNotification(Event event, Map<String, String> placeHolderData)
            throws IdentityEventException, NotificationRuntimeException {
        //send-to parameter will be set by the event senders. Here it is first read from the request parameter and
        //if it is not there, then assume this sent-to parameter should read from user's email claim only.
        String sendTo = placeHolderData.get(NotificationConstants.EmailNotification.ARBITRARY_SEND_TO);
        Map<String, String> userClaims = new HashMap<>();
        String notificationEvent = (String) event.getEventProperties().get(NotificationConstants.EmailNotification.EMAIL_TEMPLATE_TYPE);
        String username = (String) event.getEventProperties().get(IdentityEventConstants.EventProperty.USER_NAME);
        org.wso2.carbon.user.core.UserStoreManager userStoreManager = (org.wso2.carbon.user.core.UserStoreManager) event.getEventProperties().get(
                IdentityEventConstants.EventProperty.USER_STORE_MANAGER);
        String userStoreDomainName = (String) event.getEventProperties().get(IdentityEventConstants.EventProperty.USER_STORE_DOMAIN);
        String tenantDomain = (String) event.getEventProperties().get(IdentityEventConstants.EventProperty.TENANT_DOMAIN);
        String sendFrom = (String) event.getEventProperties().get(NotificationConstants.EmailNotification.ARBITRARY_SEND_FROM);

        if (StringUtils.isNotBlank(username) && userStoreManager != null) {
            userClaims = NotificationUtil.getUserClaimValues(username, userStoreManager);
        } else if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(userStoreDomainName) &&
                StringUtils.isNotBlank(tenantDomain)) {
            userClaims = NotificationUtil.getUserClaimValues(username, userStoreDomainName, tenantDomain);
        }

        String locale = getNotificationLocale();
        if (userClaims.containsKey(NotificationConstants.EmailNotification.CLAIM_URI_LOCALE)) {
            locale = userClaims.get(NotificationConstants.EmailNotification.CLAIM_URI_LOCALE);
        }
        //Only sendTo value read from claims if it is not set the event sender.
        if (StringUtils.isEmpty(sendTo)) {
            if (userClaims.containsKey(NotificationConstants.EmailNotification.CLAIM_URI_EMAIL)) {
                sendTo = userClaims.get(NotificationConstants.EmailNotification.CLAIM_URI_EMAIL);
            }
            if (StringUtils.isEmpty(sendTo)) {
                throw new IdentityEventException("Email notification sending failed. " +
                        "Sending email address is not configured for the user.");
            }
        }

        EmailTemplate emailTemplate;
        try {
            emailTemplate = NotificationHandlerDataHolder.getInstance().getEmailTemplateManager().getEmailTemplate(notificationEvent, locale, tenantDomain);
        } catch (I18nEmailMgtException e) {
            String message = "Error when retrieving template from tenant registry.";
            throw NotificationRuntimeException.error(message, e);
        }

        // This is added to change the copyright year in the email templates dynamically.
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        placeHolderData.put("current-year", String.valueOf(currentYear));

        // Resolve human-readable organization name, and add it to "organization-name" placeholder.
        String organizationName = resolveHumanReadableOrganizationName(tenantDomain);
        placeHolderData.put(ORGANIZATION_NAME_PLACEHOLDER, organizationName);

        NotificationUtil.getPlaceholderValues(emailTemplate, placeHolderData, userClaims);

        Notification.EmailNotificationBuilder builder =
                new Notification.EmailNotificationBuilder(sendTo);
        builder.setSendFrom(sendFrom);
        builder.setTemplate(emailTemplate);
        builder.setPlaceHolderData(placeHolderData);
        Notification emailNotification = builder.build();
        return emailNotification;
    }

    /**
     * If the tenant domain is a UUID, resolve the organization name from the associated organization resource.
     *
     * @param tenantDomain Tenant domain.
     * @return Human-readable name related to the represented organization space.
     * @throws IdentityEventException Error while resolving organization name.
     */
    private static String resolveHumanReadableOrganizationName(String tenantDomain) throws IdentityEventException {

        String organizationName = tenantDomain;
        try {
            if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                return organizationName;
            }
            RealmService realmService = NotificationHandlerDataHolder.getInstance().getRealmService();
            int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
            Tenant tenant = realmService.getTenantManager().getTenant(tenantId);
            if (tenant == null) {
                return organizationName;
            }
            String associatedOrganizationUUID = tenant.getAssociatedOrganizationUUID();
            if (StringUtils.isBlank(associatedOrganizationUUID)) {
                return organizationName;
            }
            OrganizationManager organizationManager =
                    NotificationHandlerDataHolder.getInstance().getOrganizationManager();
            organizationName = organizationManager.getOrganizationNameById(associatedOrganizationUUID);
        } catch (OrganizationManagementClientException e) {
            if (!ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT.getCode().equals(e.getErrorCode())) {
                throw new IdentityEventException(e.getMessage(), e);
            }
        } catch (OrganizationManagementException | UserStoreException e) {
            throw new IdentityEventException(e.getMessage(), e);
        }
        return organizationName;
    }

    /**
     * Get the notification locale.
     *
     * @return Locale
     */
    public static String getNotificationLocale() {

        return StringUtils.isNotBlank(IdentityUtil.getProperty(NotificationConstants.NOTIFICATION_DEFAULT_LOCALE))
                ? IdentityUtil.getProperty(NotificationConstants.NOTIFICATION_DEFAULT_LOCALE)
                : NotificationConstants.EmailNotification.LOCALE_DEFAULT;
    }
}

