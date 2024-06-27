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

package org.wso2.carbon.email.mgt.store;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.email.mgt.store.dao.AppNotificationTemplateDAO;
import org.wso2.carbon.email.mgt.store.dao.cache.CacheBackedNotificationTypeDAO;
import org.wso2.carbon.email.mgt.store.dao.NotificationTypeDAO;
import org.wso2.carbon.email.mgt.store.dao.OrgNotificationTemplateDAO;
import org.wso2.carbon.email.mgt.store.dao.cache.CacheBackedOrgNotificationTemplateDAO;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for managing the notification templates in the database.
 */
public class DBBasedTemplateManager implements TemplatePersistenceManager {

    private static final Log log = LogFactory.getLog(DBBasedTemplateManager.class);

    private final NotificationTypeDAO notificationTypeDAO = new CacheBackedNotificationTypeDAO();
    private final OrgNotificationTemplateDAO orgNotificationTemplateDAO = new CacheBackedOrgNotificationTemplateDAO();
    private final AppNotificationTemplateDAO appNotificationTemplateDAO = new AppNotificationTemplateDAO();

    @Override
    public void addNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        String templateTypeKey = displayName.toLowerCase();
        int tenantId = getTenantId(tenantDomain);

        notificationTypeDAO.addNotificationTemplateType(templateTypeKey, displayName, notificationChannel, tenantId);

        if (log.isDebugEnabled()) {
            log.debug(String.format("%s template type: %s for tenant: %s successfully added.", notificationChannel,
                    displayName, tenantDomain));
        }
    }

    @Override
    public boolean isNotificationTemplateTypeExists(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        String templateTypeKey = displayName.toLowerCase();
        int tenantId = getTenantId(tenantDomain);

        String templateTypeDisplayName =
                notificationTypeDAO.getNotificationTemplateType(templateTypeKey, notificationChannel, tenantId);
        boolean isNotificationTemplateTypeExists = StringUtils.isNotBlank(templateTypeDisplayName);

        if (log.isDebugEnabled()) {
            log.debug(String.format("%s template type: %s for tenant: %s is exists: %s.",
                    notificationChannel, displayName, tenantDomain, isNotificationTemplateTypeExists));
        }

        return isNotificationTemplateTypeExists;
    }

    @Override
    public List<String> listNotificationTemplateTypes(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        List<String> templateTypesDisplayNames =
                notificationTypeDAO.listNotificationTemplateTypes(notificationChannel, getTenantId(tenantDomain));

        if (log.isDebugEnabled()) {
            log.debug(String.format("%s template types for tenant: %s successfully listed.",
                    notificationChannel, tenantDomain));
        }

        return templateTypesDisplayNames;
    }

    @Override
    public void deleteNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        String templateTypeKey = displayName.toLowerCase();
        int tenantId = getTenantId(tenantDomain);

        notificationTypeDAO.deleteNotificationTemplateType(templateTypeKey, notificationChannel, tenantId);

        if (log.isDebugEnabled()) {
            log.debug(String.format("%s template type: %s for tenant: %s successfully deleted.",
                    notificationChannel, displayName, tenantDomain));
        }
    }

    @Override
    public void addOrUpdateNotificationTemplate(NotificationTemplate notificationTemplate, String applicationUuid,
                                                String tenantDomain) throws NotificationTemplateManagerServerException {

        String displayName = notificationTemplate.getDisplayName();
        String notificationChannel = notificationTemplate.getNotificationChannel();
        String locale = notificationTemplate.getLocale();
        int tenantId = getTenantId(tenantDomain);

        if (!isNotificationTemplateTypeExists(displayName, notificationChannel, tenantDomain)) {
            // Registry impl creates template type if not exists
            addNotificationTemplateType(displayName, notificationChannel, tenantDomain);
        }

        if (!isNotificationTemplateExists(displayName, locale, notificationChannel, applicationUuid, tenantDomain)) {
            if (StringUtils.isBlank(applicationUuid)) {
                orgNotificationTemplateDAO.addNotificationTemplate(notificationTemplate, tenantId);
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Org %s template with locale: %s for type: %s for tenant: %s successfully added.",
                            notificationChannel, locale, displayName, tenantDomain));
                }
            } else {
                appNotificationTemplateDAO.addNotificationTemplate(notificationTemplate, applicationUuid, tenantId);
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "App %s template with locale: %s for type: %s for application: %s for tenant: %s " +
                                    "successfully added.",
                            notificationChannel, locale, displayName, applicationUuid, tenantDomain));
                }
            }
        } else {
            // Registry impl updates the template if exists
            if (StringUtils.isBlank(applicationUuid)) {
                orgNotificationTemplateDAO.updateNotificationTemplate(notificationTemplate, tenantId);
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Org %s template with locale: %s for type: %s for tenant: %s successfully updated.",
                            notificationChannel, locale, displayName, tenantDomain));
                }
            } else {
                appNotificationTemplateDAO.updateNotificationTemplate(notificationTemplate, applicationUuid, tenantId);
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "App %s template with locale: %s for type: %s for application: %s for tenant: %s " +
                                    "successfully updated.",
                            notificationChannel, locale, displayName, applicationUuid, tenantDomain));
                }
            }
        }
    }

    @Override
    public boolean isNotificationTemplateExists(String displayName, String locale, String notificationChannel,
                                                String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        String templateTypeKey = displayName.toLowerCase();
        int tenantId = getTenantId(tenantDomain);

        boolean isNotificationTemplateExists;
        if (StringUtils.isBlank(applicationUuid)) {
            isNotificationTemplateExists =
                    orgNotificationTemplateDAO.isNotificationTemplateExists(locale, templateTypeKey,
                            notificationChannel, tenantId);

            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Org %s template with locale: %s for type: %s for tenant: %s is exists: %s.",
                        notificationChannel, locale, displayName, tenantDomain, isNotificationTemplateExists));
            }
        } else {
            isNotificationTemplateExists =
                    appNotificationTemplateDAO.isNotificationTemplateExists(locale, templateTypeKey, notificationChannel,
                            applicationUuid, tenantId);

            if (log.isDebugEnabled()) {
                log.debug(String.format("App %s template with locale: %s for type: %s for application: %s " +
                                "for tenant: %s is exists: %s.", notificationChannel, locale, displayName,
                        applicationUuid, tenantDomain, isNotificationTemplateExists));
            }
        }

        return isNotificationTemplateExists;
    }

    @Override
    public NotificationTemplate getNotificationTemplate(String displayName, String locale, String notificationChannel,
                                                        String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        String templateTypeKey = displayName.toLowerCase();
        int tenantId = getTenantId(tenantDomain);

        NotificationTemplate notificationTemplate;
        if (StringUtils.isBlank(applicationUuid)) {
            notificationTemplate =
                    orgNotificationTemplateDAO.getNotificationTemplate(locale, templateTypeKey, notificationChannel,
                            tenantId);
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Org %s template with locale: %s for type: %s for tenant: %s successfully retrieved.",
                        notificationChannel, locale, displayName, tenantDomain));
            }
        } else {
            notificationTemplate =
                    appNotificationTemplateDAO.getNotificationTemplate(locale, templateTypeKey, notificationChannel,
                            applicationUuid, tenantId);

            if (log.isDebugEnabled()) {
                log.debug(String.format("App %s template with locale: %s for type: %s for application: %s " +
                                "for tenant: %s successfully retrieved.", notificationChannel, locale, displayName,
                        applicationUuid, tenantDomain));
            }
        }

        return notificationTemplate;
    }

    @Override
    public List<NotificationTemplate> listNotificationTemplates(String templateType, String notificationChannel,
                                                                String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        int tenantId = getTenantId(tenantDomain);

        List<NotificationTemplate> notificationTemplates;
        if (StringUtils.isBlank(applicationUuid)) {
            notificationTemplates =
                    orgNotificationTemplateDAO.listNotificationTemplates(templateType, notificationChannel,
                            tenantId);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Org %s templates for type: %s for tenant: %s successfully listed.",
                        notificationChannel, templateType, tenantDomain));
            }
        } else {
            notificationTemplates =
                    appNotificationTemplateDAO.listNotificationTemplates(templateType, notificationChannel,
                            applicationUuid, tenantId);
            if (log.isDebugEnabled()) {
                log.debug(String.format("App %s templates for type: %s for application: %s for tenant: %s " +
                                "successfully listed.", notificationChannel, templateType, applicationUuid,
                        tenantDomain));
            }
        }

        return notificationTemplates;
    }

    @Override
    public List<NotificationTemplate> listAllNotificationTemplates(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        List<String> templateTypes = listNotificationTemplateTypes(notificationChannel, tenantDomain);

        List<NotificationTemplate> templateList = new ArrayList<>();
        for (String templateType : templateTypes) {
            List<NotificationTemplate> notificationTemplates =
                    listNotificationTemplates(templateType, notificationChannel, null, tenantDomain);
            templateList.addAll(notificationTemplates);
        }

        return templateList;
    }

    @Override
    public void deleteNotificationTemplate(String displayName, String locale, String notificationChannel,
                                           String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        String templateTypeKey = displayName.toLowerCase();
        int tenantId = getTenantId(tenantDomain);

        if (StringUtils.isBlank(applicationUuid)) {
            orgNotificationTemplateDAO.removeNotificationTemplate(locale, templateTypeKey, notificationChannel,
                    tenantId);
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Org %s template with locale: %s for type: %s for tenant: %s successfully deleted.",
                        notificationChannel, locale, displayName, tenantDomain));
            }
        } else {
            appNotificationTemplateDAO.removeNotificationTemplate(locale, templateTypeKey, notificationChannel,
                    applicationUuid, tenantId);
            if (log.isDebugEnabled()) {
                log.debug(String.format("App %s template with locale: %s for type: %s for application: %s " +
                                "for tenant: %s successfully deleted.", notificationChannel, locale, displayName,
                        applicationUuid, tenantDomain));
            }
        }
    }

    @Override
    public void deleteNotificationTemplates(String displayName, String notificationChannel, String applicationUuid,
                                            String tenantDomain) throws NotificationTemplateManagerServerException {

        String templateTypeKey = displayName.toLowerCase();
        int tenantId = getTenantId(tenantDomain);

        if (StringUtils.isBlank(applicationUuid)) {
            orgNotificationTemplateDAO.removeNotificationTemplates(templateTypeKey, notificationChannel, tenantId);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Org %s templates for type: %s for tenant: %s successfully deleted.",
                        notificationChannel, displayName, tenantDomain));
            }
        } else {
            appNotificationTemplateDAO.removeNotificationTemplates(templateTypeKey, notificationChannel,
                    applicationUuid, tenantId);
            if (log.isDebugEnabled()) {
                log.debug(String.format("App %s templates for type: %s for application: %s for tenant: %s " +
                                "successfully deleted.", notificationChannel, displayName, applicationUuid,
                        tenantDomain));
            }
        }
    }

    /**
     * Get the tenant id of the given tenant domain.
     * @param tenantDomain
     * @return
     * @throws NotificationTemplateManagerServerException
     */
    private int getTenantId(String tenantDomain) throws NotificationTemplateManagerServerException {

        int tenantId;
        try {
            RealmService realmService = I18nMgtDataHolder.getInstance().getRealmService();
            tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            throw new NotificationTemplateManagerServerException("ERROR_CODE_RETRIEVE_TENANT_ID",
                    "Error while retrieving tenant id", e);
        }

        if (tenantId == MultitenantConstants.INVALID_TENANT_ID) {
            throw new NotificationTemplateManagerServerException("ERROR_CODE_INVALID_TENANT_DOMAIN");
        }
        return tenantId;
    }
}
