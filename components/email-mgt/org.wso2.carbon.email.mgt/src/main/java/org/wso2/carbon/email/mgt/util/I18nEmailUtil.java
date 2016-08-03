/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.email.mgt.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtServerException;
import org.wso2.carbon.email.mgt.exceptions.I18nMgtEmailConfigException;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.CollectionImpl;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.ResourceImpl;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class I18nEmailUtil {
    private static final Log log = LogFactory.getLog(I18nEmailUtil.class);


    private I18nEmailUtil() {
    }

    /**
     * @param templateTypeName
     * @return
     */
    public static String getNormalizedName(String templateTypeName) {
        if (StringUtils.isNotBlank(templateTypeName)) {
            return templateTypeName.replaceAll("\\s+", "").toLowerCase();
        }
        throw new IllegalArgumentException("Invalid template type name provided : " + templateTypeName);
    }


    /**
     * @return
     */
    public static List<EmailTemplate> getDefaultEmailTemplates() {
        String configFilePath = CarbonUtils.getCarbonConfigDirPath() + File.separator +
                I18nMgtConstants.EMAIL_CONF_DIRECTORY + File.separator + I18nMgtConstants.EMAIL_ADMIN_CONF_FILE;

        List<EmailTemplate> defaultTemplates = new ArrayList<>();
        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            log.error("Email Configuration File is not present at: " + configFilePath);
        }

        XMLStreamReader xmlStreamReader = null;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(configFile);
            xmlStreamReader = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
            StAXOMBuilder builder = new StAXOMBuilder(xmlStreamReader);

            OMElement documentElement = builder.getDocumentElement();
            Iterator iterator = documentElement.getChildElements();
            while (iterator.hasNext()) {
                OMElement omElement = (OMElement) iterator.next();
                String type = omElement.getAttributeValue(new QName(I18nMgtConstants.TEMPLATE_TYPE));
                String displayName = omElement.getAttributeValue(new QName(I18nMgtConstants.TEMPLATE_TYPE_DISPLAY_NAME));
                String locale = omElement.getAttributeValue(new QName(I18nMgtConstants.TEMPLATE_LOCALE));
                String contentType = omElement.getAttributeValue(new QName(I18nMgtConstants.TEMPLATE_CONTENT_TYPE));

                Map<String, String> emailContentMap = getEmailContent(omElement);
                String subject = emailContentMap.get(I18nMgtConstants.TEMPLATE_SUBJECT);
                String body = emailContentMap.get(I18nMgtConstants.TEMPLATE_BODY);
                String footer = emailContentMap.get(I18nMgtConstants.TEMPLATE_FOOTER);

                // create the DTO and add to list
                EmailTemplate emailTemplateDTO = new EmailTemplate();
                emailTemplateDTO.setTemplateType(type);
                emailTemplateDTO.setTemplateDisplayName(displayName);
                emailTemplateDTO.setLocale(locale);
                emailTemplateDTO.setEmailContentType(contentType);

                emailTemplateDTO.setSubject(subject);
                emailTemplateDTO.setBody(body);
                emailTemplateDTO.setFooter(footer);

                defaultTemplates.add(emailTemplateDTO);
            }
        } catch (XMLStreamException | FileNotFoundException e) {
            log.warn("Error while loading default templates to the registry.", e);
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

        return defaultTemplates;
    }

    private static Map<String, String> getEmailContent(OMElement templateElement) {
        Map<String, String> emailContentMap = new HashMap<>();
        Iterator it = templateElement.getChildElements();
        while (it.hasNext()) {
            OMElement element = (OMElement) it.next();
            String elementName = element.getLocalName();
            String elementText = element.getText();
            if (StringUtils.equalsIgnoreCase(I18nMgtConstants.TEMPLATE_SUBJECT, elementName)) {
                emailContentMap.put(I18nMgtConstants.TEMPLATE_SUBJECT, elementText);
            } else if (StringUtils.equalsIgnoreCase(I18nMgtConstants.TEMPLATE_BODY, elementName)) {
                emailContentMap.put(I18nMgtConstants.TEMPLATE_BODY, elementText);
            } else if (StringUtils.equalsIgnoreCase(I18nMgtConstants.TEMPLATE_FOOTER, elementName)) {
                emailContentMap.put(I18nMgtConstants.TEMPLATE_FOOTER, elementText);
            }
        }
        return emailContentMap;
    }


    /**
     * @param emailTemplate
     * @return
     * @throws I18nEmailMgtException
     */
    public static Resource createTemplateResource(EmailTemplate emailTemplate) throws I18nEmailMgtException {
        Resource templateResource = new ResourceImpl();

        String templateDisplayName = emailTemplate.getTemplateDisplayName();
        String templateType = I18nEmailUtil.getNormalizedName(templateDisplayName);
        String locale = emailTemplate.getLocale();
        String contentType = emailTemplate.getEmailContentType();

        String subject = emailTemplate.getSubject();
        String body = emailTemplate.getBody();
        String footer = emailTemplate.getFooter();

        // set template properties
        templateResource.setProperty(I18nMgtConstants.TEMPLATE_TYPE_DISPLAY_NAME, templateDisplayName);
        templateResource.setProperty(I18nMgtConstants.TEMPLATE_TYPE, templateType);
        templateResource.setProperty(I18nMgtConstants.TEMPLATE_LOCALE, locale);
        templateResource.setProperty(I18nMgtConstants.TEMPLATE_CONTENT_TYPE, contentType);

        templateResource.setMediaType(RegistryConstants.TAG_MEDIA_TYPE);

        String contentArray[] = {subject, body, footer};
        String content = new Gson().toJson(contentArray);

        try {
            byte[] contentByteArray = content.getBytes("UTF-8");
            templateResource.setContent(contentByteArray);
        } catch (RegistryException | UnsupportedEncodingException e) {
            String error = "Error creating a registry resource from contents of %s email template type in %s locale.";
            throw new I18nEmailMgtServerException(String.format(error, templateDisplayName, locale), e);
        }

        return templateResource;
    }

    /**
     * @param templateResource
     * @return
     * @throws I18nEmailMgtException
     */
    public static EmailTemplate getEmailTemplate(Resource templateResource) throws I18nEmailMgtException {
        EmailTemplate emailTemplate = new EmailTemplate();
        try {
            // process email template meta-data properties
            String templateDisplayName = templateResource.getProperty(I18nMgtConstants.TEMPLATE_TYPE_DISPLAY_NAME);
            String templateType = templateResource.getProperty(I18nMgtConstants.TEMPLATE_TYPE);
            String contentType = templateResource.getProperty(I18nMgtConstants.TEMPLATE_CONTENT_TYPE);
            String locale = templateResource.getProperty(I18nMgtConstants.TEMPLATE_LOCALE);

            emailTemplate.setTemplateDisplayName(templateDisplayName);
            emailTemplate.setTemplateType(templateType);
            emailTemplate.setEmailContentType(contentType);
            emailTemplate.setLocale(locale);

            // process email template content
            Object content = templateResource.getContent();
            if (content != null) {
                byte templateContentArray[] = (byte[]) content;
                String templateContent = new String(templateContentArray, Charset.forName("UTF-8"));

                String[] templateContentElements;
                try {
                    templateContentElements = new Gson().fromJson(templateContent, String[].class);
                } catch (JsonSyntaxException ex) {
                    String error = "Error deserializing '%s:%s' template from tenant registry.";
                    throw new I18nEmailMgtServerException(String.format(error, templateDisplayName, locale), ex);
                }

                if (templateContentElements.length != 3) {
                    String errorMsg = "Template %s:%s body is in invalid format. Missing subject,body or footer.";
                    throw new I18nMgtEmailConfigException(String.format(errorMsg, templateDisplayName, locale));
                }

                emailTemplate.setSubject(templateContentElements[0]);
                emailTemplate.setBody(templateContentElements[1]);
                emailTemplate.setFooter(templateContentElements[2]);
            } else {
                String error = String.format("Unable to find any content in %s:%s email template.",
                        templateDisplayName, locale);
                log.error(error);
            }
        } catch (RegistryException e) {
            String error = "Error retrieving a template object from the registry resource";
            throw new I18nEmailMgtServerException(error, e);
        }
        return emailTemplate;
    }

    /**
     * @param normalizedTemplateName
     * @param templateDisplayName
     * @return
     */
    public static Collection createTemplateType(String normalizedTemplateName, String templateDisplayName) {
        Collection collection = new CollectionImpl();
        collection.addProperty(I18nMgtConstants.EMAIL_TEMPLATE_NAME, normalizedTemplateName);
        collection.addProperty(I18nMgtConstants.EMAIL_TEMPLATE_TYPE_DISPLAY_NAME, templateDisplayName);
        return collection;
    }


}
