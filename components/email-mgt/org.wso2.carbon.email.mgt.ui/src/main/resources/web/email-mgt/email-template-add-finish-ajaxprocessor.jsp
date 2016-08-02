<!--
~ Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
~
~ WSO2 Inc. licenses this file to you under the Apache License,
~ Version 2.0 (the "License"); you may not use this file except
~ in compliance with the License.
~ You may obtain a copy of the License at
~
~ http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing,
~ software distributed under the License is distributed on an
~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~ KIND, either express or implied. See the License for the
~ specific language governing permissions and limitations
~ under the License.
-->
<%@page import="org.apache.axis2.context.ConfigurationContext" %>
<%@page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.email.mgt.model.xsd.EmailTemplate" %>
<%@ page import="org.wso2.carbon.email.mgt.ui.I18nEmailMgtConfigServiceClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<jsp:include page="../dialog/display_messages.jsp"/>

<%
    String httpMethod = request.getMethod();
    if (!"post".equalsIgnoreCase(httpMethod)) {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        return;
    }

    String emailTypeDisplayName = request.getParameter("emailType");
    String emailContentType = request.getParameter("emailContentType");
    String emailLocale = request.getParameter("emailLocale");

    String emailSubject = request.getParameter("emailSubject");
    String emailBody = request.getParameter("emailBody");
    String emailFooter = request.getParameter("emailFooter");

    EmailTemplate templateAdded = new EmailTemplate();
    if (StringUtils.isNotBlank(emailTypeDisplayName)) {
        templateAdded.setTemplateDisplayName(emailTypeDisplayName);
    }
    if (StringUtils.isNotBlank(emailLocale)) {
        templateAdded.setLocale(emailLocale);
    }
    if (StringUtils.isNotBlank(emailContentType)) {
        templateAdded.setEmailContentType(emailContentType);
    }
    if (StringUtils.isNotBlank(emailSubject)) {
        templateAdded.setSubject(emailSubject);
    }
    if (StringUtils.isNotBlank(emailBody)) {
        templateAdded.setBody(emailBody);
    }
    if (StringUtils.isNotBlank(emailFooter)) {
        templateAdded.setFooter(emailFooter);
    }

    try {
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext = (ConfigurationContext) config
                .getServletContext()
                .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        I18nEmailMgtConfigServiceClient configClient =
                new I18nEmailMgtConfigServiceClient(cookie, backendServerURL, configContext);
        configClient.addEmailConfig(templateAdded);
        CarbonUIMessage.sendCarbonUIMessage("Email Template successfully Added", CarbonUIMessage.INFO, request);

%>

<script type="text/javascript">
    location.href = "email-template-add.jsp";
</script>
<%
} catch (Exception e) {
    CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request);
%>
<script type="text/javascript">
    location.href = "email-template-add.jsp";
</script>
<%
    }
%>

<script>
    forward();
</script>