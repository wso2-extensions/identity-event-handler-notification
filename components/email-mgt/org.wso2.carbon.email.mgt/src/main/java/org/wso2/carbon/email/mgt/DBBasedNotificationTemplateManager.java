/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.email.mgt;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.dao.AppNotificationTemplateDAO;
import org.wso2.carbon.email.mgt.dao.NotificationScenarioDAO;
import org.wso2.carbon.email.mgt.dao.OrgNotificationTemplateDAO;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.identity.governance.IdentityMgtConstants;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerInternalException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.identity.governance.service.notification.NotificationTemplateManager;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.List;

import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.getDefaultNotificationLocale;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.resolveNotificationChannel;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.validateDisplayNameOfTemplateType;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.validateNotificationTemplate;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.validateTemplateLocale;

/**
 * This class is to manage the notification templates.
 */
public class DBBasedNotificationTemplateManager implements NotificationTemplateManager {

    private static final Log log = LogFactory.getLog(DBBasedNotificationTemplateManager.class);

    private final NotificationScenarioDAO notificationScenarioDAO = new NotificationScenarioDAO();
    private final OrgNotificationTemplateDAO orgNotificationTemplateDAO = new OrgNotificationTemplateDAO();
    private final AppNotificationTemplateDAO appNotificationTemplateDAO = new AppNotificationTemplateDAO();

    @Override
    public void addNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        validateDisplayNameOfTemplateType(displayName);
        I18nEmailUtil.getNormalizedName(displayName);

        if (isNotificationTemplateTypeExists(displayName, notificationChannel, tenantDomain)) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                    I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getCode(),
                    I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            String message =
                    String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getMessage(),
                            displayName, tenantDomain);
            throw new NotificationTemplateManagerInternalException(code, message);
        }

        notificationScenarioDAO.addNotificationScenario(displayName, displayName, notificationChannel,
                getTenantId(tenantDomain));
        if (log.isDebugEnabled()) {
            log.debug(String.format("%s template scenario type: %s for tenant: %s successfully added.",
                    notificationChannel, displayName, tenantDomain));
        }
    }

    @Override
    public void addNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain,
                                            String applicationUuid) throws NotificationTemplateManagerException {

        addNotificationTemplateType(displayName, notificationChannel, tenantDomain);
    }

    private boolean isNotificationTemplateTypeExists(String displayName, String notificationChannel,
                                                     String tenantDomain) throws NotificationTemplateManagerException {

        String scenarioName = notificationScenarioDAO.getNotificationScenario(displayName, notificationChannel,
                getTenantId(tenantDomain));
        boolean isNotificationTemplateTypeExists = StringUtils.isNotBlank(scenarioName);

        if (log.isDebugEnabled()) {
            log.debug(String.format("%s template scenario type: %s for tenant: %s is exists: %s.",
                    notificationChannel, displayName, tenantDomain, isNotificationTemplateTypeExists));
        }
        return isNotificationTemplateTypeExists;
    }

    @Override
    public void addNotificationTemplate(NotificationTemplate notificationTemplate, String tenantDomain)
            throws NotificationTemplateManagerException {

        validateNotificationTemplate(notificationTemplate);

        String templateDisplayName = notificationTemplate.getDisplayName();
        String notificationChannel = notificationTemplate.getNotificationChannel();
        String locale = notificationTemplate.getLocale();

        // Registry impl creates template type if not exists
        if (!isNotificationTemplateTypeExists(templateDisplayName, notificationChannel, tenantDomain)) {
            addNotificationTemplateType(templateDisplayName, notificationChannel, tenantDomain);
        }

        if (isNotificationTemplateExists(locale, templateDisplayName, notificationChannel, tenantDomain)) {
            // Registry impl updates the template if exists
            orgNotificationTemplateDAO.updateNotificationTemplate(notificationTemplate, getTenantId(tenantDomain));
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Org %s template with locale: %s for scenario: %s for tenant: %s successfully updated.",
                        notificationChannel, locale, templateDisplayName, tenantDomain));
            }
        } else {
            orgNotificationTemplateDAO.addNotificationTemplate(notificationTemplate, getTenantId(tenantDomain));
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Org %s template with locale: %s for scenario: %s for tenant: %s successfully added.",
                        notificationChannel, locale, templateDisplayName, tenantDomain));
            }
        }
    }

    @Override
    public NotificationTemplate getNotificationTemplate(String notificationChannel, String templateType, String locale,
                                                        String tenantDomain)
            throws NotificationTemplateManagerException {

        validateTemplateLocale(locale);
        validateDisplayNameOfTemplateType(templateType);

        notificationChannel = resolveNotificationChannel(notificationChannel);

        NotificationTemplate notificationTemplate =
                orgNotificationTemplateDAO.getNotificationTemplate(locale, templateType, notificationChannel,
                        getTenantId(tenantDomain));
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Org %s template with locale: %s for scenario: %s for tenant: %s successfully retrieved.",
                    notificationChannel, locale, templateType, tenantDomain));
        }

        if (notificationTemplate == null) {
            return getDefaultTemplate(notificationChannel, templateType, locale, tenantDomain);
        }

        return notificationTemplate;
    }

    private boolean isNotificationTemplateExists(String locale, String templateType, String notificationChannel,
                                                 String tenantDomain) throws NotificationTemplateManagerException {

        NotificationTemplate notificationTemplate =
                orgNotificationTemplateDAO.getNotificationTemplate(locale, templateType, notificationChannel,
                        getTenantId(tenantDomain));
        boolean isNotificationTemplateExists = notificationTemplate != null;

        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Org %s template with locale: %s for scenario: %s for tenant: %s is exists: %s.",
                    notificationChannel, locale, templateType, tenantDomain, isNotificationTemplateExists));
        }
        return isNotificationTemplateExists;
    }

    @Override
    public void addNotificationTemplate(NotificationTemplate notificationTemplate, String tenantDomain,
                                        String applicationUuid) throws NotificationTemplateManagerException {

        if (StringUtils.isBlank(applicationUuid)) {
            addNotificationTemplate(notificationTemplate, tenantDomain);
            return;
        }

        validateNotificationTemplate(notificationTemplate);

        String templateDisplayName = notificationTemplate.getDisplayName();
        String notificationChannel = notificationTemplate.getNotificationChannel();
        String locale = notificationTemplate.getLocale();

        // Registry impl creates template type if not exists
        if (!isNotificationTemplateTypeExists(templateDisplayName, notificationChannel, tenantDomain)) {
            addNotificationTemplateType(templateDisplayName, notificationChannel, tenantDomain);
        }

        if (isNotificationTemplateExists(locale, templateDisplayName, notificationChannel,
                tenantDomain, applicationUuid)) {
            // Registry impl updates the template if exists
            appNotificationTemplateDAO.updateNotificationTemplate(notificationTemplate, applicationUuid,
                    getTenantId(tenantDomain));
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "App %s template with locale: %s for scenario: %s for application: %s for tenant: %s " +
                                "successfully updated.",
                        notificationChannel, locale, templateDisplayName, applicationUuid, tenantDomain));
            }
        } else {
            appNotificationTemplateDAO.addNotificationTemplate(notificationTemplate, applicationUuid,
                    getTenantId(tenantDomain));
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "App %s template with locale: %s for scenario: %s for application: %s for tenant: %s " +
                                "successfully added.",
                        notificationChannel, locale, templateDisplayName, applicationUuid, tenantDomain));
            }
        }
    }

    @Override
    public NotificationTemplate getNotificationTemplate(String notificationChannel, String templateType, String locale,
                                                        String tenantDomain, String applicationUuid)
            throws NotificationTemplateManagerException {

        if (StringUtils.isBlank(applicationUuid)) {
            return getNotificationTemplate(notificationChannel, templateType, locale, tenantDomain);
        }

        validateTemplateLocale(locale);
        validateDisplayNameOfTemplateType(templateType);

        notificationChannel = resolveNotificationChannel(notificationChannel);

        NotificationTemplate notificationTemplate =
                appNotificationTemplateDAO.getNotificationTemplate(locale, templateType, notificationChannel,
                        applicationUuid, getTenantId(tenantDomain));
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "App %s template with locale: %s for scenario: %s for application: %s for tenant: %s " +
                            "successfully retrieved.",
                    notificationChannel, locale, templateType, applicationUuid, tenantDomain));
        }

        if (notificationTemplate == null) {
            return getDefaultTemplate(notificationChannel, templateType, locale, tenantDomain);
        }

        return notificationTemplate;
    }

    private boolean isNotificationTemplateExists(String locale, String templateDisplayName, String notificationChannel,
                                                 String tenantDomain, String applicationUuid)
            throws NotificationTemplateManagerException {

        NotificationTemplate notificationTemplate =
                appNotificationTemplateDAO.getNotificationTemplate(locale, templateDisplayName, notificationChannel,
                        applicationUuid, getTenantId(tenantDomain));
        boolean isNotificationTemplateExists = notificationTemplate != null;

        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "App %s template with locale: %s for scenario: %s for application: %s for tenant: %s " +
                            "is exists: %s.",
                    notificationChannel, locale, templateDisplayName, applicationUuid, tenantDomain,
                    isNotificationTemplateExists));
        }
        return isNotificationTemplateExists;
    }

    @Override
    public void addDefaultNotificationTemplates(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        int numberOfAddedTemplates = 0;

        for (NotificationTemplate notificationTemplate : getDefaultNotificationTemplates(notificationChannel)) {
            if (!isNotificationTemplateTypeExists(notificationTemplate.getDisplayName(), notificationChannel, tenantDomain)) {
                addNotificationTemplateType(notificationTemplate.getDisplayName(), notificationChannel, tenantDomain);
            }

            // From registry impl:
            // Check for existence of each category, since some template may have migrated from earlier version.
            // This will also add new template types provided from file, but won't update any existing template.
            if (!isNotificationTemplateExists(notificationTemplate.getLocale(), notificationTemplate.getDisplayName(), notificationChannel,
                    tenantDomain)) {
                addNotificationTemplate(notificationTemplate, tenantDomain);
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Default %s template with locale: %s for scenario: %s for tenant: %s successfully added.",
                            notificationChannel, notificationTemplate.getLocale(),
                            notificationTemplate.getDisplayName(), tenantDomain));
                }
                numberOfAddedTemplates++;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Added %d default %s templates to the tenant: %s", numberOfAddedTemplates,
                    notificationChannel, tenantDomain));
        }
    }

    @Override
    public List<NotificationTemplate> getDefaultNotificationTemplates(String notificationChannel) {

        List<NotificationTemplate> notificationTemplates;

        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            notificationTemplates = I18nMgtDataHolder.getInstance().getDefaultSMSTemplates();
        } else {
            notificationTemplates = I18nMgtDataHolder.getInstance().getDefaultEmailTemplates();
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Default %s templates successfully listed.", notificationChannel));
        }
        return notificationTemplates;
    }

    private NotificationTemplate getDefaultTemplate(String notificationChannel, String templateType, String locale,
                                                    String tenantDomain) throws NotificationTemplateManagerException {

        String defaultLocale = getDefaultNotificationLocale(notificationChannel);
        if (StringUtils.equalsIgnoreCase(defaultLocale, locale)) {
            // Registry impl:
            // Template is not available in the default locale.
            // Therefore, breaking the flow at the consuming side to avoid NPE.
            String error = String.format(IdentityMgtConstants.ErrorMessages.ERROR_CODE_NO_TEMPLATE_FOUND.getMessage(),
                    templateType, locale, tenantDomain);
            throw new NotificationTemplateManagerException(I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_NODE_FOUND, error);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "'%s' template in '%s' locale was not found in '%s' tenant. " +
                                "Trying to return the template in default locale: '%s'",
                        templateType, locale, tenantDomain, defaultLocale));
            }
            return getNotificationTemplate(notificationChannel, templateType, defaultLocale, tenantDomain);
        }
    }

    private int getTenantId(String tenantDomain) throws NotificationTemplateManagerServerException {

        int tenantId;
        try {
            RealmService realmService = I18nMgtDataHolder.getInstance().getRealmService();
            tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            throw new NotificationTemplateManagerServerException("ERROR_CODE_RETRIEVE_TENANT_ID",
                    "Error while retrieving tenant id");
        }

        if (tenantId == MultitenantConstants.INVALID_TENANT_ID) {
            throw new NotificationTemplateManagerServerException("ERROR_CODE_INVALID_TENANT_DOMAIN");
        }
        return tenantId;
    }
}
