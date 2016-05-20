/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.event.handler.email.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.event.handler.email.constants.EmailEventConstants;
import org.wso2.carbon.identity.event.handler.email.exception.EmailEventServiceException;
import org.wso2.carbon.identity.event.handler.email.internal.EmailEventServiceComponent;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.claim.Claim;
import org.wso2.carbon.user.core.service.RealmService;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailEventUtil {

    private static Log log = LogFactory.getLog(EmailEventUtil.class);

    /**
     * This method is used to load the Email template for a specific tenant space.
     *
     * @param tenantId     - The tenant Id of the tenant that specific email template needs to be add.
     * @param resourcePath - Path to get the specific email template.
     * @throws EmailEventServiceException
     */
    public static EmailInfoDTO loadEmailTemplate(int tenantId, String resourcePath) throws EmailEventServiceException {

        if (log.isDebugEnabled()) {
            log.debug("Reading email templates from registry path : " + resourcePath);
        }

        Resource resourceValue = null;
        RegistryService registry = EmailEventServiceComponent.getRegistryService();
        EmailInfoDTO emailInfoDTO = new EmailInfoDTO();

        try {
            UserRegistry userReg = registry.getConfigSystemRegistry(tenantId);
            resourceValue = userReg.get(resourcePath);
            if (resourceValue != null) {
                byte[] emailTemplateContentArray = (byte[]) resourceValue.getContent();
                String emailContentType = resourceValue.getMediaType();
                String emailTemplateLocaleContent = new String(emailTemplateContentArray, Charset.forName("UTF-8"));

                // Retrieves the content of each email template.
                String[] emailTemplateContent = emailTemplateLocaleContent.split("\\|");
                if (emailTemplateContent.length > 3) {
                    throw new EmailEventServiceException("Cannot have | character in the template");
                }

                emailInfoDTO.setSubject(emailTemplateContent[0]);
                emailInfoDTO.setBody(emailTemplateContent[1]);
                emailInfoDTO.setFooter(emailTemplateContent[2]);
                emailInfoDTO.setEmailContentType(emailContentType);

                if (log.isDebugEnabled()) {
                    log.debug("Successfully read the email templates in resource path : " + resourcePath);
                }
            }
        } catch (ResourceNotFoundException e) {
            // Ignore the registry resource exception.
            emailInfoDTO = new EmailInfoDTO();
            if (log.isDebugEnabled()) {
                log.debug("Ignored ResourceNotFoundException", e);
            }
        } catch (RegistryException e) {
            throw new
                    EmailEventServiceException("Error occurred while reading email templates from path : " + resourcePath);
        }
        return emailInfoDTO;
    }

    /**
     * Get the claims from the user store manager
     *
     * @param userName user name
     * @param tenantId tenantId
     * @return claim value
     * @throws EmailEventServiceException if fails
     */
    public static Map<String, String> getClaimFromUserStoreManager(String userName, int tenantId)
            throws EmailEventServiceException {

        org.wso2.carbon.user.core.UserStoreManager userStoreManager = null;
        RealmService realmService = EmailEventServiceComponent.getRealmService();

        try {
            if (realmService.getTenantUserRealm(tenantId) != null) {
                userStoreManager = (org.wso2.carbon.user.core.UserStoreManager) realmService.getTenantUserRealm(tenantId).
                        getUserStoreManager();
            }

        } catch (Exception e) {
            String msg = "Error retrieving the user store manager for tenant id : " + tenantId;
            log.error(msg, e);
            throw new EmailEventServiceException(msg, e);
        }
        try {
            Map<String, String> claimsMap = new HashMap<String, String>();
            if (userStoreManager != null) {
                Claim[] userClaimValues = userStoreManager
                        .getUserClaimValues(userName, UserCoreConstants.DEFAULT_PROFILE);
                for (int i = 0; i < userClaimValues.length; i++) {
                    claimsMap.put(userClaimValues[i].getClaimUri(), userClaimValues[i].getValue());

                }
            }
            return claimsMap;
        } catch (Exception e) {
            String msg = "Unable to retrieve the claim for user : " + userName;
            log.error(msg, e);
            throw new EmailEventServiceException(msg, e);
        }
    }

    public static List<String> extractPlaceHolders(String value) {
        String exp = "\\{(.*?)\\}";

        Pattern pattern = Pattern.compile(exp);
        Matcher matcher = pattern.matcher(value);

        List<String> placeHolders = new ArrayList<>();
        while (matcher.find()) {
            String group = matcher.group().replace("{", "").replace("}", "");
            placeHolders.add(group);
        }
        return placeHolders;
    }

    public static Map<String, String> getTagData(List<String> placeHolders, Map<String, String> userClaimMap,
                                                 Map<String, String> placeHolderMapProperties) {

        Map<String, String> tagDataMap = new HashMap<>();
        List<String> userClaims = new ArrayList<>();
        List<String> userClaimUris = new ArrayList<>();
        String tagKey = null;
        for (String placeholder : placeHolders) {
            if (placeHolderMapProperties.containsKey(placeholder)) {
                tagDataMap.put(placeholder, (String) placeHolderMapProperties.get(placeholder));
            }
            //Need to add a check for claimURIs
            else if (placeholder.contains(EmailEventConstants.DEFAULT_CLAIM_URI)) {
                userClaimUris.add(placeholder);
            } else {
                String claimUri = EmailEventConstants.DEFAULT_CLAIM_URI + placeholder;
                userClaims.add(claimUri);
            }
        }

        //retrieve user claims
        if (userClaimMap != null && !userClaimMap.isEmpty()) {
            for (String claimUri : userClaimUris) {
                if (userClaimMap.containsKey(claimUri)) {
                    tagDataMap.put(claimUri, userClaimMap.get(claimUri));
                }
            }

            for (String claim : userClaims) {
                if (userClaimMap.containsKey(claim)) {
                    tagKey = claim.substring(claim.lastIndexOf("/") + 1);
                    tagDataMap.put(tagKey, userClaimMap.get(claim));
                }
            }
        }

        return tagDataMap;
    }
}

