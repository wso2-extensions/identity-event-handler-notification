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

package org.wso2.carbon.email.mgt.model;

import java.io.Serializable;

/**
 * Encapsulates email template data such as email subject,body,footer etc., which use
 * to construct the email template.
 */
public class EmailTemplate implements Serializable {

	private static final long serialVersionUID = -8854592668563155088L;
	private String subject;
	private String body;
	private String footer;
	private String templateType;
	private String templateDisplayName;
	private String locale;
	private String emailContentType;


	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getFooter() {
		return footer;
	}

	public void setFooter(String footer) {
		this.footer = footer;
	}

	public String getTemplateType() {
		return templateType;
	}

	public void setTemplateType(String templateType) {
		this.templateType = templateType;
	}

	public String getTemplateDisplayName() {
		return templateDisplayName;
	}

	public void setTemplateDisplayName(String templateDisplayName) {
		this.templateDisplayName = templateDisplayName;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getEmailContentType() {
		return emailContentType;
	}

	public void setEmailContentType(String emailContentType) {
		this.emailContentType = emailContentType;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("templateDisplayName: ").append(templateDisplayName).append("\n")
				.append("locale: ").append(locale).append("\n")
				.append("contentType: ").append(emailContentType).append("\n")
				.append("subject: ").append(subject).append("\n")
				.append("body: ").append(body).append("\n")
				.append("footer: ").append(footer);
		return stringBuilder.toString();
	}
}
