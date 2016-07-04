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

package org.wso2.carbon.email.mgt.ui;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Util class for UI component
 */
public class Util {

    /**
     * Map to store locale_code --> locale display name
     * eg : en_US --> English (United States)
     */
    private static Map<String, String> localeCodeMap;

    static {
        localeCodeMap = new HashMap<>();

        // add the locale displayName against each key.
        for (Locale aLocale : Locale.getAvailableLocales()) {
            String localeCode = getLocaleCode(aLocale);
            if (StringUtils.isNotBlank(localeCode)) {
                localeCodeMap.put(localeCode.toLowerCase(), aLocale.getDisplayName());
            }
        }
    }

    /**
     * Get the locale code string for a locale object, locale code is in the form languageCode_countryCode
     * for example : en_US
     *
     * @param locale Locale object.
     * @return locale code string.
     */
    public static String getLocaleCode(Locale locale) {
        String languageCode = locale.getLanguage();
        String countryCode = locale.getCountry();
        countryCode = StringUtils.isBlank(countryCode) ? languageCode : countryCode;

        if (StringUtils.isNotBlank(languageCode) && StringUtils.isNotBlank(countryCode)) {
            return languageCode + "_" + countryCode;
        }

        return null;
    }

    /**
     * Get locale display name given a locale code string.
     * ex: when en_US given we
     *
     * @param localeCode String in the format of [languageCode]_[countryCode] like en_US.
     * @return locale display name string
     */
    public static String getLocaleDisplayName(String localeCode) {
        if (StringUtils.isBlank(localeCode)) {
            return null;
        }

        String localeDisplayName = localeCodeMap.get(localeCode.toLowerCase());
        return StringUtils.isBlank(localeDisplayName) ? localeCode : localeDisplayName;
    }

}
