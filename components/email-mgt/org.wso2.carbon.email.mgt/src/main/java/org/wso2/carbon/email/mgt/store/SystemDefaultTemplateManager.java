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
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for handling default templates provided by the system.
 * System default templates are used as default org notification templates in a given tenant and based on the content
 * of the email-admin-config.xml.
 * Any modifications to system default templates needs to be done via email-admin-config.xml and any such changes will
 * be reflected only with the server startup.
 * This class have support for get, list, exists operations and any invocations to modification operations
 * throws {@link UnsupportedOperationException}.
 * This class expected to be use only with conjunction with another {@link TemplatePersistenceManager} implementation
 * which supports full CRUD operations, hence {@link UnifiedTemplateManager} provides that aggregation using this as a
 * fallback provider.
 */
public class SystemDefaultTemplateManager implements TemplatePersistenceManager {

    private final Map<String, Map<String, NotificationTemplate>> defaultEmailTemplates;
    private final Map<String, Map<String, NotificationTemplate>> defaultSMSTemplates;

    /**
     * Initializes the in-memory template manager by populating default email and SMS templates.
     */
    public SystemDefaultTemplateManager() {

        defaultEmailTemplates = populateTemplates(I18nMgtDataHolder.getInstance().getDefaultEmailTemplates());
        defaultSMSTemplates = populateTemplates(I18nMgtDataHolder.getInstance().getDefaultSMSTemplates());
    }

    @Override
    public void addNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        throw new UnsupportedOperationException();
    }

    /**
     * Checks if a notification template type exists in the in-memory store.
     *
     * @param displayName         The display name of the template type.
     * @param notificationChannel The channel of the notification (e.g., Email, SMS).
     * @param tenantDomain        The tenant domain.
     * @return true if the template type exists, false otherwise.
     * @throws NotificationTemplateManagerServerException if an error occurs during the operation.
     */
    @Override
    public boolean isNotificationTemplateTypeExists(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (StringUtils.isNotBlank(displayName)) {
            return getTemplateMap(notificationChannel).containsKey(displayName.toLowerCase());
        }
        return false;
    }

    /**
     * Lists all notification template types for a given channel in the in-memory store.
     *
     * @param notificationChannel The channel of the notification (e.g., Email, SMS).
     * @param tenantDomain        The tenant domain.
     * @return A list of template type display names.
     * @throws NotificationTemplateManagerServerException if an error occurs during the operation.
     */

    @Override
    public List<String> listNotificationTemplateTypes(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        List<String> displayNames = new ArrayList<>();
        Map<String, Map<String, NotificationTemplate>> defaultTemplates =  getTemplateMap(notificationChannel);
        defaultTemplates.forEach((displayName, innerMap) ->
                innerMap.forEach((locale, template) ->
                        displayNames.add(template.getDisplayName())
                )
        );
        return displayNames;
    }

    @Override
    public void deleteNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAllNotificationTemplates(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        throw new UnsupportedOperationException();
    }

    @Override
    public void addOrUpdateNotificationTemplate(NotificationTemplate notificationTemplate, String applicationUuid,
                                                String tenantDomain) throws NotificationTemplateManagerServerException {

        throw new UnsupportedOperationException();
    }

    /**
     * Checks if a specific notification template exists in the in-memory store.
     *
     * @param displayName         The display name of the template.
     * @param locale              The locale of the template.
     * @param notificationChannel The channel of the notification (e.g., Email, SMS).
     * @param applicationUuid     The application UUID.
     * @param tenantDomain        The tenant domain.
     * @return true if the template exists, false otherwise.
     * @throws NotificationTemplateManagerServerException if an error occurs during the operation.
     */
    @Override
    public boolean isNotificationTemplateExists(String displayName, String locale, String notificationChannel,
                                                String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (StringUtils.isBlank(applicationUuid) && !StringUtils.isBlank(displayName) && !StringUtils.isBlank(locale)) {
            Map<String, Map<String, NotificationTemplate>> defaultTemplates =  getTemplateMap(notificationChannel);
            return defaultTemplates.containsKey(displayName.toLowerCase()) &&
                    defaultTemplates.get(displayName.toLowerCase()).containsKey(locale.toLowerCase());
        }
        return false;
    }

    /**
     * Retrieves a specific notification template from the in-memory store.
     *
     * @param displayName         The display name of the template.
     * @param locale              The locale of the template.
     * @param notificationChannel The channel of the notification (e.g., Email, SMS).
     * @param applicationUuid     The application UUID.
     * @param tenantDomain        The tenant domain.
     * @return The notification template, or null if it doesn't exist.
     * @throws NotificationTemplateManagerServerException if an error occurs during the operation.
     */
    @Override
    public NotificationTemplate getNotificationTemplate(String displayName, String locale, String notificationChannel,
                                                        String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (StringUtils.isBlank(applicationUuid) && !StringUtils.isBlank(displayName) && !StringUtils.isBlank(locale)) {

            Map<String, Map<String, NotificationTemplate>> defaultTemplates =  getTemplateMap(notificationChannel);
            return defaultTemplates.containsKey(displayName.toLowerCase()) ?
                    defaultTemplates.get(displayName.toLowerCase()).get(locale.toLowerCase()) : null;
        }
        return null;

    }

    /**
     * Lists all notification templates of a specific type for a given channel in the in-memory store.
     *
     * @param templateType        The type of the templates to list.
     * @param notificationChannel The channel of the notification (e.g., Email, SMS).
     * @param applicationUuid     The application UUID.
     * @param tenantDomain        The tenant domain.
     * @return A list of notification templates.
     * @throws NotificationTemplateManagerServerException if an error occurs during the operation.
     */
    @Override
    public List<NotificationTemplate> listNotificationTemplates(String templateType, String notificationChannel,
                                                                String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (StringUtils.isBlank(applicationUuid) && !StringUtils.isBlank(templateType)) {
            List<NotificationTemplate> notificationTemplates = new ArrayList<>();
            Map<String, Map<String, NotificationTemplate>> defaultTemplates =  getTemplateMap(notificationChannel);
            if (defaultTemplates.containsKey(templateType.toLowerCase())) {
                defaultTemplates.get(templateType.toLowerCase()).forEach((locale, template) ->
                        notificationTemplates.add(template));
                return notificationTemplates;

            }
        }
        return Collections.emptyList();
    }

    /**
     * Lists all notification templates for a given channel in the in-memory store.
     *
     * @param notificationChannel The channel of the notification (e.g., Email, SMS).
     * @param tenantDomain        The tenant domain.
     * @return A list of notification templates.
     * @throws NotificationTemplateManagerServerException if an error occurs during the operation.
     */
    @Override
    public List<NotificationTemplate> listAllNotificationTemplates(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            return I18nMgtDataHolder.getInstance().getDefaultSMSTemplates();
        }
        return I18nMgtDataHolder.getInstance().getDefaultEmailTemplates();
    }

    @Override
    public void deleteNotificationTemplate(String displayName, String locale, String notificationChannel,
                                           String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteNotificationTemplates(String displayName, String notificationChannel, String applicationUuid,
                                            String tenantDomain) throws NotificationTemplateManagerServerException {

        throw new UnsupportedOperationException();
    }

    /**
     * Populates the in-memory store with default templates, organizing them by display name and locale.
     *
     * @param templates The list of templates to populate the store with.
     * @return A map of template display names to maps of locale to templates.
     */
    private Map<String, Map<String, NotificationTemplate>> populateTemplates (List<NotificationTemplate> templates) {

        Map<String, Map<String, NotificationTemplate>> templateMap = new HashMap<>();

        for (NotificationTemplate template : templates) {
            templateMap.computeIfAbsent(template.getDisplayName().toLowerCase(), k -> new HashMap<>())
                    .put(template.getLocale().toLowerCase(), template);
        }
        return templateMap;
    }

    /**
     * Retrieves the map of templates for a given notification channel (e.g., Email, SMS).
     *
     * @param notificationChannel The channel of the notification.
     * @return The map of templates, organized by display name and locale.
     */
    private Map<String, Map<String, NotificationTemplate>> getTemplateMap(String notificationChannel) {

        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            return defaultSMSTemplates;
        }
        return defaultEmailTemplates;
    }

    /**
     * Checks if there is a template available as a system default template with the exact same details.
     * This method is used to avoid managing duplicate templates.
     *
     * @param template  Notification template to check.
     * @return          True if a template with the same details is available, false otherwise.
     */
    boolean hasSameTemplate(NotificationTemplate template) {

        if (template == null) {
            return false;
        }

        Map<String, NotificationTemplate> defaultTemplatesForScenario =
                getTemplateMap(template.getNotificationChannel()).get(template.getDisplayName().toLowerCase());
        return defaultTemplatesForScenario != null && defaultTemplatesForScenario.containsValue(template);
    }
}
