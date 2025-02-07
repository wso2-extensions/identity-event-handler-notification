/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.notification.sender.tenant.config.utils;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.configuration.mgt.core.model.Attribute;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.notification.push.provider.PushProvider;
import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderException;
import org.wso2.carbon.identity.notification.push.provider.model.PushSenderData;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.EmailSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.PushSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.SMSSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementServerException;
import org.wso2.carbon.identity.notification.sender.tenant.config.internal.NotificationSenderTenantConfigDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ADAPTER_PROPERTY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ADAPTER_PROPERTY_NAME;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ADAPTER_TYPE_EMAIL_VALUE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ADAPTER_TYPE_HTTP_VALUE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ADAPTER_TYPE_KEY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CLIENT_HTTP_METHOD_PROPERTY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CONSTANT_HTTP_POST;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CONTENT_TYPE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.CUSTOM_MAPPING_KEY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.DISABLE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.EMAIL_ADDRESS_PROPERTY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.EMAIL_ADDRESS_VALUE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.EMAIL_INLINE_BODY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.EMAIL_SUBJECT_PROPERTY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.EMAIL_SUBJECT_VALUE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.EMAIL_TYPE_PROPERTY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.EMAIL_TYPE_VALUE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ENABLE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_ERROR_DELETING_NOTIFICATION_SENDER_SECRETS;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_ERROR_PROCESSING_PUSH_SENDER_PROPERTIES;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_ERROR_UPDATING_PUSH_SENDER_PROPERTIES;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ErrorMessage.ERROR_CODE_MATCHING_PUSH_PROVIDER_NOT_FOUND;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.FROM;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.HTTP_URL_PROPERTY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.INLINE;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.INLINE_BODY_PARAM_PREFIX;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.INLINE_BODY_PROPERTY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.INTERNAL_PROPERTIES;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.KEY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.MAPPING;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.MAPPING_TYPE_KEY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PLACEHOLDER_IDENTIFIER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PROCESSING_KEY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PROPERTIES_TO_SKIP_AT_ADAPTER_CONFIG;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PROVIDER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PROVIDER_URL;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PUBLISHER_NAME;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.ROOT_ELEMENT;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SECRET;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SENDER;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SMTP_FROM_PROPERTY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SMTP_HOST_PROPERTY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SMTP_PASSWORD_PROPERTY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SMTP_PORT_PROPERTY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.SMTP_USER_PROPERTY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.STATISTICS_KEY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.STREAM_NAME;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.STREAM_VERSION;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.TEXT;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.TO;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.TRACE_KEY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.XMLNS_KEY;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.XMLNS_VALUE;

/**
 * Util class for notification sender.
 */
public class NotificationSenderUtils {

    /**
     * Generate EmailPublisher.xml input stream.
     *
     * @param emailSender Email sender post body.
     * @return Input stream of the EmailPublisher.
     * @throws ParserConfigurationException Parser configuration exception.
     * @throws TransformerException         Transformer exception.
     */
    public static InputStream generateEmailPublisher(EmailSenderDTO emailSender)
            throws ParserConfigurationException, TransformerException {

        Map<String, String> properties = emailSender.getProperties();
        DocumentBuilderFactory documentFactory = IdentityUtil.getSecuredDocumentBuilderFactory();
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        Document document = documentBuilder.newDocument();
        // Root element (eventPublisher).
        Element root = document.createElement(ROOT_ELEMENT);
        document.appendChild(root);
        // Collect event publisher attributes to a map and set attributes to root element.
        addEventPublisherAttributes(emailSender, document, root);
        // Add 'From' element (event stream details) to event publisher.
        addFromElement(properties, document, root);
        // Add 'Mapping' element (output mapping details) to event publisher.
        addMappingElementToEmailEventPublisher(document, root);
        // Add 'To' element (event adapter details) to event publisher.
        addToElementToEmailEventPublisher(emailSender, properties, document, root);
        DOMSource xmlSource = new DOMSource(document);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Result outputTarget = new StreamResult(outputStream);
        Transformer transformer = IdentityUtil.getSecuredTransformerFactory().newTransformer();
        transformer.transform(xmlSource, outputTarget);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * Generate SMSPublisher.xml input stream.
     *
     * @param smsSender SMS sender post body.
     * @return Input stream of the SMSPublisher.
     * @throws ParserConfigurationException Parser configuration exception.
     * @throws TransformerException         Transformer exception.
     */
    public static InputStream generateSMSPublisher(SMSSenderDTO smsSender)
            throws ParserConfigurationException, TransformerException {

        Map<String, String> properties = smsSender.getProperties();
        DocumentBuilderFactory documentFactory = IdentityUtil.getSecuredDocumentBuilderFactory();
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        Document document = documentBuilder.newDocument();
        // Root element (eventPublisher).
        Element root = document.createElement(ROOT_ELEMENT);
        document.appendChild(root);
        // Collect event publisher attributes to a map and set attributes to root element.
        addEventPublisherAttributes(smsSender, document, root);
        // Add 'From' element (event stream details) to event publisher.
        addFromElement(properties, document, root);
        // Add 'Mapping' element (output mapping details) to event publisher.
        addMappingElementToSMSEventPublisher(smsSender, properties, document, root);
        // Add 'To' element (event adapter details) to event publisher.
        addToElementToSMSEventPublisher(smsSender, properties, document, root);
        DOMSource xmlSource = new DOMSource(document);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Result outputTarget = new StreamResult(outputStream);
        Transformer transformer = IdentityUtil.getSecuredTransformerFactory().newTransformer();
        transformer.transform(xmlSource, outputTarget);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    private static void addEventPublisherAttributes(EmailSenderDTO emailSender, Document document, Element root) {

        Map<String, String> eventPublisherAttributes = new HashMap<>();
        eventPublisherAttributes.put(PUBLISHER_NAME, emailSender.getName());
        eventPublisherAttributes.put(STATISTICS_KEY, DISABLE);
        eventPublisherAttributes.put(TRACE_KEY, DISABLE);
        eventPublisherAttributes.put(XMLNS_KEY, XMLNS_VALUE);
        // Set attributes to root element.
        for (Map.Entry<String, String> eventPublisherAttribute : eventPublisherAttributes.entrySet()) {
            Attr publisherAttributes = document.createAttribute(eventPublisherAttribute.getKey());
            publisherAttributes.setValue(eventPublisherAttribute.getValue());
            root.setAttributeNode(publisherAttributes);
        }
    }

    private static void addEventPublisherAttributes(SMSSenderDTO smsSender, Document document, Element root) {

        Map<String, String> eventPublisherAttributes = new HashMap<>();
        eventPublisherAttributes.put(PUBLISHER_NAME, smsSender.getName());
        eventPublisherAttributes.put(PROCESSING_KEY, ENABLE);
        eventPublisherAttributes.put(STATISTICS_KEY, DISABLE);
        eventPublisherAttributes.put(TRACE_KEY, DISABLE);
        eventPublisherAttributes.put(XMLNS_KEY, XMLNS_VALUE);
        // Set attributes to root element.
        for (Map.Entry<String, String> eventPublisherAttribute : eventPublisherAttributes.entrySet()) {
            Attr publisherAttributes = document.createAttribute(eventPublisherAttribute.getKey());
            publisherAttributes.setValue(eventPublisherAttribute.getValue());
            root.setAttributeNode(publisherAttributes);
        }
    }

    private static void addFromElement(Map<String, String> properties, Document document, Element root) {

        Element from = document.createElement(FROM);
        root.appendChild(from);
        // Set attributes to From element.
        Attr streamNameAttr = document.createAttribute(STREAM_NAME);
        streamNameAttr.setValue(properties.get(STREAM_NAME));
        from.setAttributeNode(streamNameAttr);
        Attr streamVersionAttr = document.createAttribute(STREAM_VERSION);
        streamVersionAttr.setValue(properties.get(STREAM_VERSION));
        from.setAttributeNode(streamVersionAttr);
    }

    private static void addMappingElementToEmailEventPublisher(Document document, Element root) {

        Element mapping = document.createElement(MAPPING);
        root.appendChild(mapping);
        // Set attributes to mapping element.
        Attr customMappingAttr = document.createAttribute(CUSTOM_MAPPING_KEY);
        customMappingAttr.setValue(ENABLE);
        mapping.setAttributeNode(customMappingAttr);
        Attr mappingTypeAttr = document.createAttribute(MAPPING_TYPE_KEY);
        mappingTypeAttr.setValue(TEXT);
        mapping.setAttributeNode(mappingTypeAttr);
        // Inline element.
        Element inline = document.createElement(INLINE);
        inline.appendChild(document.createTextNode(EMAIL_INLINE_BODY));
        mapping.appendChild(inline);
    }

    private static void addMappingElementToSMSEventPublisher(SMSSenderDTO smsSender, Map<String, String> properties,
                                                             Document document, Element root) {

        Element mapping = document.createElement(MAPPING);
        root.appendChild(mapping);
        // Set attributes to mapping element.
        Attr customMappingAttr = document.createAttribute(CUSTOM_MAPPING_KEY);
        customMappingAttr.setValue(ENABLE);
        mapping.setAttributeNode(customMappingAttr);
        Attr mappingTypeAttr = document.createAttribute(MAPPING_TYPE_KEY);
        mappingTypeAttr.setValue(smsSender.getContentType().toLowerCase(Locale.getDefault()));
        mapping.setAttributeNode(mappingTypeAttr);
        // Inline element.
        Element inline = document.createElement(INLINE);
        String smsSendAPIBody;
        // If body is given as an input we expect that contains all required attributes with values.
        if (StringUtils.isNotEmpty(properties.get(INLINE_BODY_PROPERTY))) {
            smsSendAPIBody = properties.get(INLINE_BODY_PROPERTY);
        } else {
            String smsSendAPIBodyTemplate = NotificationSenderTenantConfigDataHolder.getInstance()
                    .getSmsProviderPayloadTemplateManager()
                    .getSMSProviderPayloadTemplateByProvider(smsSender.getProvider()).getBody();
            smsSendAPIBody = generateSmsSendAPIBody(smsSendAPIBodyTemplate, smsSender);
        }
        inline.appendChild(document.createTextNode(smsSendAPIBody));
        mapping.appendChild(inline);
    }

    private static void addToElementToEmailEventPublisher(EmailSenderDTO emailSender, Map<String, String> properties,
                                                          Document document, Element root) {

        Element to = document.createElement(TO);
        root.appendChild(to);
        // Set attributes to to element.
        Attr eventAdapterTypeAttr = document.createAttribute(ADAPTER_TYPE_KEY);
        eventAdapterTypeAttr.setValue(ADAPTER_TYPE_EMAIL_VALUE);
        to.setAttributeNode(eventAdapterTypeAttr);
        // Take adapter properties to a map.
        Map<String, String> adapterProperties = new HashMap<>();
        adapterProperties.put(EMAIL_ADDRESS_PROPERTY, EMAIL_ADDRESS_VALUE);
        adapterProperties.put(EMAIL_TYPE_PROPERTY, EMAIL_TYPE_VALUE);
        adapterProperties.put(EMAIL_SUBJECT_PROPERTY, EMAIL_SUBJECT_VALUE);
        if (StringUtils.isNotEmpty(emailSender.getPassword())) {
            adapterProperties.put(SMTP_PASSWORD_PROPERTY, emailSender.getPassword());
        }
        if (StringUtils.isNotEmpty(emailSender.getFromAddress())) {
            adapterProperties.put(SMTP_FROM_PROPERTY, emailSender.getFromAddress());
        }
        if (StringUtils.isNotEmpty(emailSender.getUsername())) {
            adapterProperties.put(SMTP_USER_PROPERTY, emailSender.getUsername());
        }
        if (StringUtils.isNotEmpty(emailSender.getSmtpServerHost())) {
            adapterProperties.put(SMTP_HOST_PROPERTY, emailSender.getSmtpServerHost());
        }
        if (!"null".equals(String.valueOf(emailSender.getSmtpPort()))) {
            adapterProperties.put(SMTP_PORT_PROPERTY, String.valueOf(emailSender.getSmtpPort()));
        }
        for (Map.Entry<String, String> property : properties.entrySet()) {
            if (!(PROPERTIES_TO_SKIP_AT_ADAPTER_CONFIG.contains(property.getKey()) ||
                    property.getKey().startsWith(INLINE_BODY_PARAM_PREFIX))) {
                adapterProperties.put(property.getKey(), property.getValue());
            }
        }
        // Add properties.
        for (Map.Entry<String, String> property : adapterProperties.entrySet()) {
            Element adapterProperty = document.createElement(ADAPTER_PROPERTY);
            Attr attribute = document.createAttribute(ADAPTER_PROPERTY_NAME);
            attribute.setValue(property.getKey());
            adapterProperty.setAttributeNode(attribute);
            adapterProperty.appendChild(document.createTextNode(property.getValue()));
            to.appendChild(adapterProperty);
        }
    }

    private static void addToElementToSMSEventPublisher(SMSSenderDTO smsSender, Map<String, String> properties,
                                                        Document document, Element root) {

        Element to = document.createElement(TO);
        root.appendChild(to);
        // Set attributes to to element.
        Attr eventAdapterTypeAttr = document.createAttribute(ADAPTER_TYPE_KEY);
        eventAdapterTypeAttr.setValue(ADAPTER_TYPE_HTTP_VALUE);
        to.setAttributeNode(eventAdapterTypeAttr);
        // Take adapter properties to a map.
        Map<String, String> adapterProperties = new HashMap<>();
        if (StringUtils.isNotBlank(smsSender.getProviderURL())) {
            adapterProperties.put(HTTP_URL_PROPERTY, smsSender.getProviderURL());
        } else {
            adapterProperties.put(HTTP_URL_PROPERTY, StringUtils.EMPTY);
        }
        // Default client method is httpPost. Can be changed by configuring properties.
        adapterProperties.put(CLIENT_HTTP_METHOD_PROPERTY, CONSTANT_HTTP_POST);
        for (Map.Entry<String, String> property : properties.entrySet()) {
            if (!(PROPERTIES_TO_SKIP_AT_ADAPTER_CONFIG.contains(property.getKey()) ||
                    property.getKey().startsWith(INLINE_BODY_PARAM_PREFIX))) {
                adapterProperties.put(property.getKey(), property.getValue());
            }
        }
        // Add properties.
        for (Map.Entry<String, String> property : adapterProperties.entrySet()) {
            Element adapterProperty = document.createElement(ADAPTER_PROPERTY);
            Attr attribute = document.createAttribute(ADAPTER_PROPERTY_NAME);
            attribute.setValue(property.getKey());
            adapterProperty.setAttributeNode(attribute);
            adapterProperty.appendChild(document.createTextNode(property.getValue()));
            to.appendChild(adapterProperty);
        }
    }

    /**
     * Generate SMS send API body using template and input params.
     *
     * @param smsSendAPIBodyTemplate    SMS sender's send SMS API payload template.
     * @param smsSender                 SMS sender post body.
     * @return Inline body for SMSPublisher.
     */
    private static String generateSmsSendAPIBody(String smsSendAPIBodyTemplate, SMSSenderDTO smsSender) {

        String inlineBody = smsSendAPIBodyTemplate;
        Map<String, String> inlineBodyProperties = new HashMap<>();
        /*
        key, secret, sender inputs or any property defined with key value starting from "body." are considered
        when generating the inline body of SMS publisher.
         */
        if (StringUtils.isNotEmpty(smsSender.getKey())) {
            inlineBodyProperties.put(KEY, smsSender.getKey());
        }
        if (StringUtils.isNotEmpty(smsSender.getSecret())) {
            inlineBodyProperties.put(SECRET, smsSender.getSecret());
        }
        if (StringUtils.isNotEmpty(smsSender.getSender())) {
            inlineBodyProperties.put(SENDER, smsSender.getSender());
        }
        inlineBodyProperties.putAll(smsSender.getProperties());

        for (Map.Entry property : inlineBodyProperties.entrySet()) {
            inlineBody =
                    inlineBody.replace(PLACEHOLDER_IDENTIFIER + property.getKey(), (CharSequence) property.getValue());
        }
        return inlineBody;
    }

    /**
     * Build an SMS sender response from SMS sender's resource object.
     *
     * @param resource SMS sender resource object.
     * @return SMS sender response.
     */
    public static SMSSenderDTO buildSmsSenderFromResource(Resource resource) {

        SMSSenderDTO smsSender = new SMSSenderDTO();
        smsSender.setName(resource.getResourceName());
        // Skip STREAM_NAME, STREAM_VERSION and PUBLISHER_TYPE_PROPERTY properties which are stored for internal use.
        Map<String, String> attributesMap =
                resource.getAttributes().stream()
                        .filter(attribute -> !(INTERNAL_PROPERTIES.contains(attribute.getKey())))
                        .filter(attribute -> attribute.getValue() != null)
                        .collect(Collectors.toMap(Attribute::getKey, Attribute::getValue));
        attributesMap.forEach((key, value) -> {
            switch (key) {
                case PROVIDER:
                    smsSender.setProvider(value);
                    break;
                case PROVIDER_URL:
                    smsSender.setProviderURL(value);
                    break;
                case KEY:
                    smsSender.setKey(value);
                    break;
                case SECRET:
                    smsSender.setSecret(value);
                    break;
                case SENDER:
                    smsSender.setSender(value);
                    break;
                case CONTENT_TYPE:
                    smsSender.setContentType(value);
                    break;
                default:
                    smsSender.getProperties().put(key, value);
            }
        });
        return smsSender;
    }

    /**
     * Build Resource by Push sender DTO.
     *
     * @param pushSender PushSender DTO.
     * @param pushProvider  PushProvider pushProvider.
     * @return Resource object.
     */
    public static Resource buildResourceFromPushSender(PushSenderDTO pushSender, PushProvider pushProvider)
            throws NotificationSenderManagementServerException {

        try {
            pushSender.setProperties(pushProvider.preProcessProperties(buildPushSenderData(pushSender)));
            pushSender.setProperties(pushProvider.storePushProviderSecretProperties(buildPushSenderData(pushSender)));
        } catch (PushProviderException e) {
            throw new NotificationSenderManagementServerException(ERROR_CODE_ERROR_PROCESSING_PUSH_SENDER_PROPERTIES,
                    pushSender.getName(), e);
        }

        Resource resource = new Resource();
        resource.setResourceName(pushSender.getName());
        Map<String, String> pushSenderAttributes = pushSender.getProperties();
        pushSenderAttributes.put(PROVIDER, pushSender.getProvider());
        List<Attribute> resourceAttributes =
                pushSenderAttributes.entrySet().stream()
                        .filter(attribute -> attribute.getValue() != null && !"null".equals(attribute.getValue()))
                        .map(attribute -> new Attribute(attribute.getKey(), attribute.getValue()))
                        .collect(Collectors.toList());
        resource.setAttributes(resourceAttributes);
        return resource;
    }

    /**
     * Build a push sender response from push sender's resource object.
     *
     * @param resource Push sender resource object.
     * @param isProcessProperties Is conditionally process the values of the push sender properties.
     * @return Push sender response.
     */
    public static PushSenderDTO buildPushSenderFromResource(Resource resource, boolean isProcessProperties)
            throws NotificationSenderManagementServerException {

        PushSenderDTO pushSender = new PushSenderDTO();
        pushSender.setName(resource.getResourceName());
        pushSender.setProviderId(resource.getResourceId());
        Map<String, String> attributesMap =
                resource.getAttributes().stream()
                        .filter(attribute -> !(INTERNAL_PROPERTIES.contains(attribute.getKey())))
                        .collect(Collectors.toMap(Attribute::getKey, Attribute::getValue));
        attributesMap.forEach((key, value) -> {
            if (key.equals(PROVIDER)) {
                pushSender.setProvider(value);
            } else {
                pushSender.getProperties().put(key, value);
            }
        });

        if (!isProcessProperties) {
            return pushSender;
        }

        try {
            /*
            * When the subOrganization tries to inherit push sender configurations from the parent organization,
            * to retrieve the secret properties of the push sender from parent organization, we need to start the
            * tenant flow with the tenant domain of the parent organization since, the secrets of the push sender
            * are stored against the parent organization.
            * */
            FrameworkUtils.startTenantFlow(resource.getTenantDomain());
            PushProvider provider = getPushProvider(pushSender);
            pushSender.setProperties(provider.retrievePushProviderSecretProperties(buildPushSenderData(pushSender)));
            pushSender.setProperties(provider.postProcessProperties(buildPushSenderData(pushSender)));
        } catch (PushProviderException e) {
            throw new NotificationSenderManagementServerException(ERROR_CODE_ERROR_PROCESSING_PUSH_SENDER_PROPERTIES,
                    pushSender.getName(), e);
        } finally {
            FrameworkUtils.endTenantFlow();
        }
        return pushSender;
    }

    /**
     * This method is used to trigger any push provider specific operations when updating push sender properties.
     *
     * @param pushSender Push sender DTO.
     */
    public static void updatePushSenderCredentials(PushSenderDTO pushSender, PushProvider pushProvider,
                                                   String tenantDomain)
            throws NotificationSenderManagementServerException {

        try {
            pushProvider.updateCredentials(buildPushSenderData(pushSender), tenantDomain);
        } catch (PushProviderException e) {
            throw new NotificationSenderManagementServerException(ERROR_CODE_ERROR_UPDATING_PUSH_SENDER_PROPERTIES,
                    pushSender.getName(), e);
        }
    }

    /**
     * Get the push provider for the given push sender.
     *
     * @param pushSender Push sender DTO.
     * @return Push provider.
     */
    public static PushProvider getPushProvider(PushSenderDTO pushSender)
            throws NotificationSenderManagementServerException {

        PushProvider provider = NotificationSenderTenantConfigDataHolder.getInstance()
                .getPushProvider(pushSender.getProvider());
        if (provider == null) {
            throw new NotificationSenderManagementServerException(ERROR_CODE_MATCHING_PUSH_PROVIDER_NOT_FOUND,
                    pushSender.getName());
        }
        return provider;
    }

    /**
     * Delete push sender secret properties.
     *
     * @param resource Push sender resource object.
     */
    public static void deletePushSenderSecretProperties(Resource resource)
            throws NotificationSenderManagementServerException {

        try {
            PushSenderDTO pushSender = buildPushSenderFromResource(resource, false);
            PushProvider pushProvider = getPushProvider(pushSender);
            pushProvider.deletePushProviderSecretProperties(buildPushSenderData(pushSender));
        } catch (PushProviderException e) {
            throw new NotificationSenderManagementServerException(ERROR_CODE_ERROR_DELETING_NOTIFICATION_SENDER_SECRETS,
                    e.getMessage(), e);
        }
    }

    /**
     * Get the primary tenant id of the given tenant domain.
     *
     * @return Primary tenant id.
     * @throws OrganizationManagementException If an error occurred while getting the primary tenant id.
     */
    public static int getPrimaryTenantId(String tenantDomain) throws OrganizationManagementException {

        OrganizationManager organizationManager = NotificationSenderTenantConfigDataHolder.getInstance()
                .getOrganizationManager();
        String orgId = organizationManager.resolveOrganizationId(tenantDomain);
        String primaryOrgId = organizationManager.getPrimaryOrganizationId(orgId);
        String primaryTenantDomain = organizationManager.resolveTenantDomain(primaryOrgId);
        return IdentityTenantUtil.getTenantId(primaryTenantDomain);
    }

    /**
     * Build PushSenderData from PushSenderDTO.
     *
     * @param pushSenderDTO PushSender DTO.
     * @return PushSenderData.
     */
    public static PushSenderData buildPushSenderData(PushSenderDTO pushSenderDTO) {

        PushSenderData pushSenderData = new PushSenderData();
        pushSenderData.setName(pushSenderDTO.getName());
        pushSenderData.setProvider(pushSenderDTO.getProvider());
        pushSenderData.setProperties(pushSenderDTO.getProperties());
        pushSenderData.setProviderId(pushSenderDTO.getProviderId());
        return pushSenderData;
    }
}
