/*
 * Copyright (c) 2016-2024, WSO2 LLC. (http://www.wso2.com).
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
package org.wso2.carbon.email.mgt.internal;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.email.mgt.EmailTemplateManager;
import org.wso2.carbon.email.mgt.EmailTemplateManagerImpl;
import org.wso2.carbon.email.mgt.NotificationTemplateManagerImpl;
import org.wso2.carbon.email.mgt.SMSProviderPayloadTemplateManager;
import org.wso2.carbon.email.mgt.SMSProviderPayloadTemplateManagerImpl;
import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.model.SMSProviderTemplate;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.identity.governance.service.notification.NotificationTemplateManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.OrgResourceResolverService;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NOTIFICATION_TEMPLATES_DEBUG_TENANTS;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NOTIFICATION_TEMPLATES_LEGACY_TENANTS;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.SERVICE_PROPERTY_KEY_SERVICE_NAME;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.SERVICE_PROPERTY_VAL_EMAIL_TEMPLATE_MANAGER;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.SERVICE_PROPERTY_VAL_NOTIFICATION_TEMPLATE_MANAGER;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.SMS_PROVIDER;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.SMS_PROVIDER_POST_BODY_TEMPLATES_DIR_PATH;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.TEMPLATE_BODY;

@Component(
         name = "I18nMgtServiceComponent", 
         immediate = true)
public class I18nMgtServiceComponent {

    private static final Log log = LogFactory.getLog(I18nMgtServiceComponent.class);

    private I18nMgtDataHolder dataHolder = I18nMgtDataHolder.getInstance();

    @Activate
    protected void activate(ComponentContext context) {
        try {
            BundleContext bundleCtx = context.getBundleContext();

            // Load default notification templates from file
            I18nMgtDataHolder.getInstance().setDefaultEmailTemplates(
                    loadDefaultTemplatesFromFile(NotificationChannels.EMAIL_CHANNEL.getChannelType()));
            I18nMgtDataHolder.getInstance().setDefaultSMSTemplates(
                    loadDefaultTemplatesFromFile(NotificationChannels.SMS_CHANNEL.getChannelType()));

            // Register Email Mgt Service as an OSGi service.
            EmailTemplateManagerImpl emailTemplateManager = new EmailTemplateManagerImpl();
            ServiceRegistration emailTemplateSR = bundleCtx.registerService(EmailTemplateManager.class.getName(),
                    emailTemplateManager, null);
            if (emailTemplateSR != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Email Template Mgt Service registered.");
                }
            } else {
                log.error("Error registering Email Template Mgt Service.");
            }

            // Register EmailTemplateManagerImpl.
            Hashtable<String, String> emailTemplateManagerServiceProperties = new Hashtable<>();
            emailTemplateManagerServiceProperties.put(SERVICE_PROPERTY_KEY_SERVICE_NAME,
                    SERVICE_PROPERTY_VAL_EMAIL_TEMPLATE_MANAGER);
            ServiceRegistration notificationManagerSR = bundleCtx
                    .registerService(NotificationTemplateManager.class.getName(), emailTemplateManager,
                            emailTemplateManagerServiceProperties);
            if (notificationManagerSR != null) {
                log.debug("Notification Template Mgt Service registered.");
            } else {
                log.error("Error registering Notification Template Mgt Service.");
            }

            // Register Notification Template Mgt Service as an OSGi service.
            NotificationTemplateManagerImpl notificationTemplateManager = new NotificationTemplateManagerImpl();
            Hashtable<String, String> notificationTemplateManagerServiceProperties = new Hashtable<>();
            notificationTemplateManagerServiceProperties.put(SERVICE_PROPERTY_KEY_SERVICE_NAME,
                    SERVICE_PROPERTY_VAL_NOTIFICATION_TEMPLATE_MANAGER);
            ServiceRegistration notificationTemplateSR = bundleCtx
                    .registerService(NotificationTemplateManager.class.getName(), notificationTemplateManager,
                            notificationTemplateManagerServiceProperties);
            if (notificationTemplateSR != null) {
                log.debug("Notification Template Mgt Service registered.");
            } else {
                log.error("Error registering Notification Template Mgt Service.");
            }

            // Register SMSProviderPayloadTemplateManagerImpl.
            SMSProviderPayloadTemplateManagerImpl smsProviderPayloadTemplateManager =
                    new SMSProviderPayloadTemplateManagerImpl();
            ServiceRegistration smsProviderPayloadTemplateManagerSR = bundleCtx
                    .registerService(SMSProviderPayloadTemplateManager.class.getName(),
                            smsProviderPayloadTemplateManager, null);
            if (smsProviderPayloadTemplateManagerSR != null) {
                if (log.isDebugEnabled()) {
                    log.debug("SMS Provider Payload Template Mgt Service registered.");
                }
            } else {
                log.error("Error registering SMS Provider Payload Template Mgt Service.");
            }

            // Load SMS service providers' sms send API payloads.
            loadDefaultSMSProviderPostBodyTemplates();
            log.debug("I18n Management is activated");
        } catch (Throwable e) {
            log.error("Error while activating I18n Management bundle", e);
        }
    }

    /**
     * Load default SMS providers' SMS send API post body templates on server startup.
     */
    private void loadDefaultSMSProviderPostBodyTemplates() {

        Path path = SMS_PROVIDER_POST_BODY_TEMPLATES_DIR_PATH;
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            if (log.isDebugEnabled()) {
                log.debug("SMS providers' SMS send API body templates are not present at: " + path);
            }
        }
        List<SMSProviderTemplate> defaultSMSProviderPostBodyTemplates = new ArrayList<>();
        XMLStreamReader xmlStreamReader = null;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(path.toString());
            xmlStreamReader = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
            StAXOMBuilder builder = new StAXOMBuilder(xmlStreamReader);

            OMElement documentElement = builder.getDocumentElement();
            Iterator iterator = documentElement.getChildElements();
            while (iterator.hasNext()) {
                OMElement omElement = (OMElement) iterator.next();
                Iterator it = omElement.getChildElements();
                String body = null;
                while (it.hasNext()) {
                    OMElement element = (OMElement) it.next();
                    String elementName = element.getLocalName();
                    String elementText = element.getText();
                    if (StringUtils.equalsIgnoreCase(TEMPLATE_BODY, elementName)) {
                        body = elementText;
                    }
                }

                // Create SMS provider template.
                SMSProviderTemplate smsProviderTemplate = new SMSProviderTemplate();
                smsProviderTemplate.setProvider(omElement.getAttributeValue(new QName(SMS_PROVIDER)));
                smsProviderTemplate.setBody(body);
                defaultSMSProviderPostBodyTemplates.add(smsProviderTemplate);
            }
            SMSProviderPayloadTemplateDataHolder.getInstance()
                    .setSMSProvidersAPIPayloads(defaultSMSProviderPostBodyTemplates);
        } catch (XMLStreamException | FileNotFoundException e) {
            log.warn("Error while loading default SMS providers' SMS send POST API payload templates.", e);
        } finally {
            try {
                if (xmlStreamReader != null) {
                    xmlStreamReader.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (XMLStreamException e) {
                log.error("Error while closing XML stream", e);
            } catch (IOException e) {
                log.error("Error while closing input stream", e);
            }
        }
    }

    /**
     * Loads the default templates from the file for the channel(EMAIL or SMS) and create list of Notification Template.
     *
     * @param notificationChannel   Channel of the notification.
     * @return                      List of NotificationTemplate.
     */
    public List<NotificationTemplate> loadDefaultTemplatesFromFile(String notificationChannel) {

        String configFilePath = buildNotificationTemplateConfigPath(notificationChannel);
        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            log.error("Email Configuration File is not present at: " + configFilePath);
        }

        List<NotificationTemplate> defaultNotificationTemplates = new ArrayList<>();
        XMLStreamReader xmlStreamReader = null;

        try (InputStream inputStream = new FileInputStream(configFile)) {
            xmlStreamReader = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
            StAXOMBuilder builder = new StAXOMBuilder(xmlStreamReader);

            OMElement documentElement = builder.getDocumentElement();
            Iterator iterator = documentElement.getChildElements();
            while (iterator.hasNext()) {
                OMElement omElement = (OMElement) iterator.next();
                Map<String, String> templateContentMap = getNotificationTemplateContent(omElement);

                // Create notification template model with the template attributes.
                NotificationTemplate notificationTemplate = new NotificationTemplate();
                notificationTemplate.setType(omElement.getAttributeValue(new QName(I18nMgtConstants.TEMPLATE_TYPE)));
                notificationTemplate.setDisplayName(
                        omElement.getAttributeValue(new QName(I18nMgtConstants.TEMPLATE_TYPE_DISPLAY_NAME)));
                notificationTemplate
                        .setLocale(omElement.getAttributeValue(new QName(I18nMgtConstants.TEMPLATE_LOCALE)));
                notificationTemplate.setBody(templateContentMap.get(I18nMgtConstants.TEMPLATE_BODY));
                notificationTemplate.setNotificationChannel(notificationChannel);

                if (NotificationChannels.EMAIL_CHANNEL.getChannelType().equals(notificationChannel)) {
                    notificationTemplate.setContentType(
                            omElement.getAttributeValue(new QName(I18nMgtConstants.TEMPLATE_CONTENT_TYPE)));
                    notificationTemplate.setFooter(templateContentMap.get(I18nMgtConstants.TEMPLATE_FOOTER));
                    notificationTemplate.setSubject(templateContentMap.get(I18nMgtConstants.TEMPLATE_SUBJECT));
                }
                defaultNotificationTemplates.add(notificationTemplate);
            }
        } catch (XMLStreamException | IOException e) {
            log.warn("Error while loading default templates from file.", e);
        } finally {
            try {
                if (xmlStreamReader != null) {
                    xmlStreamReader.close();
                }
            } catch (XMLStreamException e) {
                log.error("Error while closing XML stream", e);
            }
        }
        return defaultNotificationTemplates;
    }

    /**
     * Get the template attributes of the notification template such as SUBJECT, BODY, EMAIL.
     *
     * @param templateElement OMElement
     * @return List of attributes in the notification template
     */
    private static Map<String, String> getNotificationTemplateContent(OMElement templateElement) {

        Map<String, String> notificationTemplateContent = new HashMap<>();
        Iterator it = templateElement.getChildElements();
        while (it.hasNext()) {
            OMElement element = (OMElement) it.next();
            String elementName = element.getLocalName();
            String elementText = element.getText();
            if (StringUtils.equalsIgnoreCase(I18nMgtConstants.TEMPLATE_SUBJECT, elementName)) {
                notificationTemplateContent.put(I18nMgtConstants.TEMPLATE_SUBJECT, elementText);
            } else if (StringUtils.equalsIgnoreCase(I18nMgtConstants.TEMPLATE_BODY, elementName)) {
                notificationTemplateContent.put(I18nMgtConstants.TEMPLATE_BODY, elementText);
            } else if (StringUtils.equalsIgnoreCase(I18nMgtConstants.TEMPLATE_FOOTER, elementName)) {
                notificationTemplateContent.put(I18nMgtConstants.TEMPLATE_FOOTER, elementText);
            }
        }
        return notificationTemplateContent;
    }

    /**
     * Build the file path of the notification template config file according to the given channel.
     *
     * @param notificationChannel Notification channel name (EMAIL,SMS)
     * @return Path of the configuration file
     */
    private String buildNotificationTemplateConfigPath(String notificationChannel) {

        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            return CarbonUtils.getCarbonConfigDirPath() + File.separator +
                    I18nMgtConstants.SMS_CONF_DIRECTORY + File.separator + I18nMgtConstants.SMS_TEMPLAE_ADMIN_CONF_FILE;
        }
        return CarbonUtils.getCarbonConfigDirPath() + File.separator +
                I18nMgtConstants.EMAIL_CONF_DIRECTORY + File.separator + I18nMgtConstants.EMAIL_ADMIN_CONF_FILE;
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (log.isDebugEnabled()) {
            log.debug("I18n Management bundle is de-activated");
        }
    }

    @Reference(
            name = "org.wso2.carbon.identity.application.mgt",
            service = ApplicationManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetApplicationManagementService")
    protected void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        log.debug("Setting Application Management Service.");
        dataHolder.setApplicationManagementService(applicationManagementService);
    }

    protected void unsetApplicationManagementService(ApplicationManagementService applicationManagementService) {

        log.debug("Unsetting Application Management Service.");
        dataHolder.setApplicationManagementService(null);
    }

    @Reference(
             name = "realm.service", 
             service = org.wso2.carbon.user.core.service.RealmService.class, 
             cardinality = ReferenceCardinality.MANDATORY, 
             policy = ReferencePolicy.DYNAMIC, 
             unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Realm Service");
        }
        dataHolder.setRealmService(realmService);
    }

    @Reference(
             name = "registry.service", 
             service = org.wso2.carbon.registry.core.service.RegistryService.class, 
             cardinality = ReferenceCardinality.MANDATORY, 
             policy = ReferencePolicy.DYNAMIC, 
             unbind = "unsetRegistryService")
    protected void setRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Registry Service");
        }
        dataHolder.setRegistryService(registryService);
    }

    @Reference(
             name = "RegistryResourceMgtService", 
             service = org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService.class, 
             cardinality = ReferenceCardinality.MANDATORY, 
             policy = ReferencePolicy.DYNAMIC, 
             unbind = "unsetRegistryResourceMgtService")
    protected void setRegistryResourceMgtService(RegistryResourceMgtService registryResourceMgtService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting Registry Resource Mgt Service.");
        }
        dataHolder.setRegistryResourceMgtService(registryResourceMgtService);
    }

    protected void unsetRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("UnSetting the Registry Service");
        }
        dataHolder.setRegistryService(null);
    }

    protected void unsetRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("UnSetting the Realm Service");
        }
        dataHolder.setRealmService(null);
    }

    protected void unsetRegistryResourceMgtService(RegistryResourceMgtService registryResourceMgtService) {
        if (log.isDebugEnabled()) {
            log.debug("UnSetting Registry Resource Mgt Service.");
        }
        dataHolder.setRegistryResourceMgtService(null);
    }

    @Reference(
            name = "organization.management.service",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManager")
    protected void setOrganizationManager(OrganizationManager organizationManager) {

        dataHolder.getInstance().setOrganizationManager(organizationManager);
    }

    protected void unsetOrganizationManager(OrganizationManager organizationManager) {

        dataHolder.getInstance().setOrganizationManager(null);
    }

    @Reference(
            name = "org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service",
            service = OrgResourceResolverService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrgResourceResolverService")
    protected void setOrgResourceResolverService(OrgResourceResolverService orgResourceResolverService) {

        dataHolder.setOrgResourceResolverService(orgResourceResolverService);
    }

    protected void unsetOrgResourceResolverService(OrgResourceResolverService orgResourceResolverService) {

        dataHolder.setOrgResourceResolverService(null);
    }
}

