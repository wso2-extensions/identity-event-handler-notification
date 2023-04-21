package org.wso2.carbon.email.mgt.model;

import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.xds.common.constant.XDSWrapper;

public class EmailTemplateXDSWrapper implements XDSWrapper {

    private String displayName;
    private String tenantDomain;
    private String templateTypeName;
    private String localeCode;
    private EmailTemplate emailTemplate;
    private String timestamp;

public EmailTemplateXDSWrapper(EmailTemplateXDSWrapperBuilder builder) {
        this.displayName = builder.displayName;
        this.tenantDomain = builder.tenantDomain;
        this.templateTypeName = builder.templateTypeName;
        this.localeCode = builder.localeCode;
        this.emailTemplate = builder.emailTemplate;
        this.timestamp = builder.timestamp;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public String getTemplateTypeName() {
        return templateTypeName;
    }

    public String getLocaleCode() {
        return localeCode;
    }

    public EmailTemplate getEmailTemplate() {
        return emailTemplate;
    }

    public static class EmailTemplateXDSWrapperBuilder {

        private String displayName;
        private String tenantDomain;
        private String templateTypeName;
        private String localeCode;
        private EmailTemplate emailTemplate;
        private String timestamp;


        public EmailTemplateXDSWrapperBuilder setDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public EmailTemplateXDSWrapperBuilder setTenantDomain(String tenantDomain) {
            this.tenantDomain = tenantDomain;
            return this;
        }

        public EmailTemplateXDSWrapperBuilder setTemplateTypeName(String templateTypeName) {
            this.templateTypeName = templateTypeName;
            return this;
        }

        public EmailTemplateXDSWrapperBuilder setLocaleCode(String localeCode) {
            this.localeCode = localeCode;
            return this;
        }

        public EmailTemplateXDSWrapperBuilder setEmailTemplate(EmailTemplate emailTemplate) {
            this.emailTemplate = emailTemplate;
            return this;
        }

        public EmailTemplateXDSWrapper build() {

            this.timestamp = String.valueOf(System.currentTimeMillis());
            return new EmailTemplateXDSWrapper(this);
        }
    }
}
