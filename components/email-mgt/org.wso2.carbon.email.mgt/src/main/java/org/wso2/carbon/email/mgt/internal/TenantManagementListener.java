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

package org.wso2.carbon.email.mgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.email.mgt.DBBasedEmailTemplateManager;
import org.wso2.carbon.email.mgt.EmailTemplateManager;
import org.wso2.carbon.email.mgt.EmailTemplateManagerImpl;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.identity.governance.service.notification.NotificationTemplateManager;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;

public class TenantManagementListener implements TenantMgtListener {

    private static final int EXEC_ORDER = 21;
    private static final Log log = LogFactory.getLog(TenantManagementListener.class);

    /**
     * Add the default Email Templates to the registry when a new tenant is registered.
     *
     * @param tenantInfo Information about the newly created tenant.
     */
    public void onTenantCreate(TenantInfoBean tenantInfo) throws StratosException {
        //Load email template configuration on tenant creation.
        String tenantDomain = tenantInfo.getTenantDomain();
        EmailTemplateManager templateManager = new DBBasedEmailTemplateManager();
        try {
            templateManager.addDefaultEmailTemplates(tenantDomain);
        } catch (I18nEmailMgtException e) {
            String message = "Error occurred while loading default email templates for the tenant : " + tenantDomain;
            log.error(message);
            throw new StratosException(message, e);
        }
        try {
            NotificationTemplateManager notificationTemplateManager = new EmailTemplateManagerImpl();
            notificationTemplateManager.addDefaultNotificationTemplates(
                    NotificationChannels.SMS_CHANNEL.getChannelType(), tenantDomain);
        } catch (NotificationTemplateManagerException e) {
            String message = "Error occurred while loading default SMS notification templates for the " +
                    "tenant : " + tenantDomain;
            log.error(message);
            throw new StratosException(message, e);
        }
    }

    public void onTenantUpdate(TenantInfoBean tenantInfo) throws StratosException {
        // It is not required to implement this method for I18n mgt.
    }

    @Override
    public void onPreDelete(int tenantId) throws StratosException {
        // It is not required to implement this method for I18n mgt.
    }

    @Override
    public void onTenantDelete(int i) {
        // It is not required to implement this method for I18n mgt.
    }

    public void onTenantRename(int tenantId, String oldDomainName,
                               String newDomainName) throws StratosException {
        // It is not required to implement this method for I18n mgt.
    }

    public int getListenerOrder() {
        return EXEC_ORDER;
    }

    public void onTenantInitialActivation(int tenantId) throws StratosException {
        // It is not required to implement this method for I18n mgt.
    }

    public void onTenantActivation(int tenantId) throws StratosException {
        // It is not required to implement this method for I18n mgt.
    }

    public void onTenantDeactivation(int tenantId) throws StratosException {
        // It is not required to implement this method for I18n mgt.
    }

    public void onSubscriptionPlanChange(int tenentId, String oldPlan, String newPlan) throws StratosException {
        // It is not required to implement this method for I18n mgt.
    }

}
