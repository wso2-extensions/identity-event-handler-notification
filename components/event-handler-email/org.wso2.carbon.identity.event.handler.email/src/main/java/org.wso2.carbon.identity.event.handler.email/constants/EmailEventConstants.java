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

package org.wso2.carbon.identity.event.handler.email.constants;

public class EmailEventConstants {

    private EmailEventConstants() {
    }

    public static final String EMAIL_TEMPLATE_PATH = "identity/Email/";
    public static final String LOCALE_DEFAULT = "en_US";
    public static final String TEMPLATE_CONTENT_TYPE_DEFAULT = "text/text";
    public static final String CLAIM_URI_LOCALE = "http://wso2.org/claims/locality";
    public static final String CLAIM_URI_EMAIL = "http://wso2.org/claims/emailaddress";
    public static final String CLAIM_URI_FIRST_NAME = "http://wso2.org/claims/givenname";
    public static final String CLAIM_URI_USER_NAME = "http://wso2.org/claims/username";
    public static final String DEFAULT_CLAIM_URI = "http://wso2.org/claims/";
    public static final String DEFAULT_IDENTITY_PREFIX = "identity/";

    public static final String CARBON_DOMAIN = "carbon.super";

    public static enum templateTypes {
        accountlock, accountunlock
    }

    public class EventProperty {
        public static final String TEMPLATE_TYPE = "TEMPLATE_TYPE";
    }

    public class EmailProperty {
        public static final String EMAIL_SUBJECT = "email.subject";
        public static final String EMAIL_CONTENT_TYPE = "email.type";
        public static final String EMAIL_ADDRESS ="email.address";
        public static final String EMAIL_CONTENT_LINE_SEPARATOR ="line.separator";
    }

}
