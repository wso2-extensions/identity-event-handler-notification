/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.email.mgt.service;

import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.dao.NotificationTemplateDAO;
import org.wso2.carbon.email.mgt.dao.impl.NotificationTemplateDAOImpl;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.List;

public class NotificationTemplateService {

    private final NotificationTemplateDAO notificationTemplateDAO;

    public NotificationTemplateService() {

        this.notificationTemplateDAO = new NotificationTemplateDAOImpl();
    }

    /**
     * Get all notification template types for given path and tenant domain.
     *
     * @param channelType  Channel type.
     * @param tenantDomain Tenant domain.
     * @return List of available template types.
     * @throws NotificationTemplateManagerException If an error occurred while retrieving the template types.
     */
    public List<String> getNotificationTemplateTypes(String channelType, String tenantDomain)
            throws NotificationTemplateManagerException {

        int tenantID = getTenantID(tenantDomain);
        return notificationTemplateDAO.getNotificationTemplateTypes(getNotificationTemplatePath(channelType), tenantID);
    }

    /**
     * Get tenant ID for the given tenant domain.
     *
     * @param tenantDomain Tenant domain.
     * @return Tenant ID.
     * @throws NotificationTemplateManagerException Error while retrieving tenant UUID.
     */
    private int getTenantID(String tenantDomain) throws NotificationTemplateManagerException {

        if (tenantDomain != null) {
            int tenantID = IdentityTenantUtil.getTenantId(tenantDomain);
            if (tenantID != MultitenantConstants.INVALID_TENANT_ID) {
                return tenantID;
            }
        }
        throw new NotificationTemplateManagerException("Invalid tenant domain: " + tenantDomain);
    }

    private String getNotificationTemplatePath(String channelType) {

        if (NotificationChannels.EMAIL_CHANNEL.getChannelType().equals(channelType)) {
            return I18nMgtConstants.EMAIL_TEMPLATE_PATH;
        } else if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(channelType)) {
            return I18nMgtConstants.SMS_TEMPLATE_PATH;
        } else {
            throw new IllegalArgumentException("Invalid channel type: " + channelType);
        }
    }

}
