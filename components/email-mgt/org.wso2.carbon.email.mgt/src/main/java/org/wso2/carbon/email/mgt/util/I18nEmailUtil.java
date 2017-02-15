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

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.security.caas.api.util.CarbonSecurityConstants;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**i
 * Util class for email management
 */
public class I18nEmailUtil {
    private static final Log log = LogFactory.getLog(I18nEmailUtil.class);

    private static Map<String, Map<String, EmailTemplate>> templateCollectionMap;

    private I18nEmailUtil() {
    }

    public static void buildEmailTemplates() {
        //read all directories
        Path templateDirectoryPath = Paths.get(CarbonSecurityConstants.getCarbonHomeDirectory().toString(), "conf",
                I18nMgtConstants.EMAIL_CONF_DIRECTORY);
        File emailTemplateFile = new File(templateDirectoryPath.toUri().getPath());
        File[] emailTemplateLocaleFiles = emailTemplateFile.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
        Map<String, EmailTemplate> emailTemplateTypes = null;
        templateCollectionMap = new HashMap<>();

        if (emailTemplateLocaleFiles == null) {
            log.warn("There are no locale email template directories available");
            return;
        }

        for (File directory : emailTemplateLocaleFiles) {
            File[] templateFiles = directory.listFiles();
            emailTemplateTypes = new HashMap<>();
            if (templateFiles == null) {
                log.warn("There are no emails templates available");
                return;
            }
            for (File templateFile : templateFiles) {
                Path templateFilePath = Paths.get(templateFile.getAbsolutePath());
                if (Files.exists(templateFilePath)) {
                    try (Reader in = new InputStreamReader(Files.newInputStream(templateFilePath),
                            StandardCharsets.UTF_8)) {
                        Yaml yaml = new Yaml();
                        yaml.setBeanAccess(BeanAccess.FIELD);
                        EmailTemplate emailTemplate = yaml.loadAs(in, EmailTemplate.class);
                        if (emailTemplate.getTemplateType() != null) {
                            emailTemplateTypes.put(emailTemplate.getTemplateType(), emailTemplate);
                        }

                    } catch (IOException e) {
                        log.error("Error while reading emails templates", e);
                    }
                }
            }
            templateCollectionMap.put(directory.getName(), emailTemplateTypes);
        }

    }

    public static Map<String, Map<String, EmailTemplate>> getTemplateCollectionMap() {
        return templateCollectionMap;
    }

}
