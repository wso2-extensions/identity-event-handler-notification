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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@page import="org.apache.axis2.context.ConfigurationContext" %>
<%@page import="org.apache.commons.lang.ArrayUtils" %>
<%@page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.email.mgt.ui.I18nEmailMgtConfigServiceClient" %>
<%@ page import="org.wso2.carbon.email.mgt.ui.Util" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.util.ResourceBundle" %>

<jsp:include page="../dialog/display_messages.jsp"/>


<%
    String emailTemplateType = request.getParameter("templateType");
    String username = request.getParameter("username");
    Locale[] availableLocale = Locale.getAvailableLocales();

    String forwardTo = null;
    String BUNDLE = "org.wso2.carbon.email.mgt.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());

    I18nEmailMgtConfigServiceClient client;
    String[] emailTemplateTypes = null;
    try {
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext = (ConfigurationContext) config.getServletContext().
                getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        client = new I18nEmailMgtConfigServiceClient(cookie, backendServerURL, configContext);

        // get template types
        emailTemplateTypes = client.getEmailTemplateTypes();
        if (emailTemplateTypes == null) {
            emailTemplateTypes = new String[0];
        }

    } catch (Exception e) {
        String message = resourceBundle.getString("error.while.loading.email.template.data");
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
        forwardTo = "../admin/error.jsp";
    }

    if (forwardTo != null) {
%>

<script type="text/javascript">
    function forward() {
        location.href = "<%=Encode.forJavaScriptBlock(forwardTo)%>";
    }

    forward();
</script>

<%
        return;
    }
%>


<fmt:bundle
        basename="org.wso2.carbon.email.mgt.ui.i18n.Resources">
    <carbon:breadcrumb label="email.add"
                       resourceBundle="org.wso2.carbon.identity.user.profile.ui.i18n.Resources"
                       topPage="true" request="<%=request%>"/>

    <div id="middle">
        <h2><fmt:message key='email.template.management'/></h2>

        <div id="workArea">


            <script type="text/javascript">

                function validate() {

                    var value = document.getElementsByName("emailType")[0].value;
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="email.template.type.is.required"/>');
                        return false;
                    }

                    var emailLocaleIndex = document.getElementsByName("emailLocale")[0].selectedIndex;
                    if (emailLocaleIndex == 0) {
                        CARBON.showWarningDialog('<fmt:message key="email.template.locale.is.required"/>');
                        return false;
                    }


                    var value = document.getElementsByName("emailSubject")[0].value;
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="email.template.subject.is.required"/>');
                        return false;
                    } else if (value.length > 50) {
                        CARBON.showWarningDialog('<fmt:message key="email.template.subject.is.too.long"/>');
                        return false;
                    }


                    var value = document.getElementsByName("emailBody")[0].value;
                    console.log(value);
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="email.template.body.is.required"/>');
                        return false;
                    }

                    var value = document.getElementsByName("emailFooter")[0].value;
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="email.template.footer.is.required"/>');
                        return false;
                    }
                    document.addemailtemplate.submit();
                }

                function cancelForm() {
                    location.href = 'email-template-mgt.jsp';
                }

            </script>

            <form name="addemailtemplate" action="email-template-add-finish-ajaxprocessor.jsp" method="post">
                <table style="width: 100%" class="styledLeft">
                    <thead>
                    <tr>
                        <th colspan="2"><fmt:message key='add.new.emailtemplate.details'/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td class="formRow">
                            <table class="normal" cellspacing="0">
                                <tr>
                                    <td class="leftCol-med labelField"><fmt:message
                                            key="email.template.type"/></td>
                                    <td><select id="emailType" name="emailType" class="leftCol-med">
                                        <%
                                            if (ArrayUtils.isNotEmpty(emailTemplateTypes)) {
                                                for (String templateDisplayName : emailTemplateTypes) {
                                                    String selected = StringUtils.equalsIgnoreCase(templateDisplayName, emailTemplateType) ? "selected" : "";
                                                    if (StringUtils.isNotBlank(templateDisplayName)) {
                                        %>
                                        <option value="<%=templateDisplayName%>" <%=selected%>><%=templateDisplayName%>
                                        </option>
                                        <%
                                                    }
                                                }
                                            }
                                        %>
                                    </select></td>
                                </tr>

                                <tr>
                                    <td class="leftCol-med labelField"><fmt:message
                                            key="email.template.locale"/></td>
                                    <td><select id="emailLocale" name="emailLocale" class="leftCol-med">
                                        <%
                                            for (Locale aLocale : availableLocale) {
                                                String localeCode = Util.getLocaleCode(aLocale);
                                        %>
                                        <option value="<%=Encode.forHtmlAttribute(localeCode)%>">
                                            <%=Encode.forHtmlContent(aLocale.getDisplayName())%>
                                        </option>
                                        <%
                                            }
                                        %>
                                    </select></td>
                                </tr>
                                <tr>
                                    <td class="leftCol-med labelField"><fmt:message
                                            key="email.template.contentType"/></td>
                                    <td><select id="emailContentType" name="emailContentType" class="leftCol-med">
                                        <option>text/html</option>
                                        <option>text/plain</option>
                                    </select></td>
                                </tr>
                                <tr>
                                    <td class="leftCol-small"><fmt:message key='email.template.subject'/><font
                                            color="red">*</font></td>
                                    <td><input type="text" name="emailSubject" id="emailSubject" class="text-box-big"
                                               style="width:500px"/></td>
                                </tr>
                                <tr>
                                    <td class="leftCol-small"><fmt:message key='email.template.body'/><font color="red">*</font>
                                    </td>
                                    <td><textarea name="emailBody" id="emailBody" class="text-box-big"
                                                  style="width: 500px; height: 170px;"></textarea></td>
                                </tr>
                                <tr>
                                    <td class="leftCol-small"><fmt:message key='email.template.footer'/><font
                                            color="red">*</font></td>
                                    <td><textarea name="emailFooter" id="emailFooter" class="text-box-big"
                                                  style="width: 265px; height: 87px;"></textarea></td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2" class="buttonRow">
                            <button onclick="validate()" type="button" class="button">Add</button>
                            <button onclick="cancelForm()" type="button" class="button">Cancel</button>
                        </td>
                    </tr>

                    </tbody>
                </table>
            </form>
        </div>
    </div>
</fmt:bundle>
