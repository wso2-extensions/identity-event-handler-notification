package org.wso2.carbon.email.mgt;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.email.mgt.dao.AppNotificationTemplateDAO;
import org.wso2.carbon.email.mgt.dao.NotificationScenarioDAO;
import org.wso2.carbon.email.mgt.dao.OrgNotificationTemplateDAO;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.identity.governance.service.notification.NotificationTemplateManager;

import java.util.List;

import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.getTenantId;

/**
 * This class is to manage the notification templates.
 */
public class DBBasedNotificationTemplateManager implements NotificationTemplateManager {
    private static final Log log = LogFactory.getLog(DBBasedNotificationTemplateManager.class);

    private final NotificationScenarioDAO notificationScenarioDAO = new NotificationScenarioDAO();
    private final OrgNotificationTemplateDAO orgNotificationTemplateDAO = new OrgNotificationTemplateDAO();
    private final AppNotificationTemplateDAO appNotificationTemplateDAO = new AppNotificationTemplateDAO();

    @Override
    public void addNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain) throws NotificationTemplateManagerException {
        log.info("Test addNotificationTemplateType(): " + displayName);

        try {
            notificationScenarioDAO.addNotificationScenario(displayName, displayName, notificationChannel, getTenantId(tenantDomain));
        } catch (NotificationTemplateManagerServerException e) {
            throw new NotificationTemplateManagerException(e.getMessage(), e);
        } catch (I18nEmailMgtException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain, String applicationUuid) throws NotificationTemplateManagerException {

        log.info("Test addNotificationTemplateType(): " + displayName);
        addNotificationTemplateType(displayName, notificationChannel, tenantDomain);
    }

    private boolean isNotificationTemplateTypeExists(String displayName, String notificationChannel, String tenantDomain) throws NotificationTemplateManagerException {

        log.info("Test type isNotificationTemplateTypeExists(): " + displayName);

        try {
            String scenarioName = notificationScenarioDAO.getNotificationScenario(displayName, notificationChannel, getTenantId(tenantDomain));
            return StringUtils.isNotBlank(scenarioName);
        } catch (NotificationTemplateManagerServerException e) {
            throw new NotificationTemplateManagerException(e.getMessage(), e);
        } catch (I18nEmailMgtException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addNotificationTemplate(NotificationTemplate notificationTemplate, String tenantDomain) throws NotificationTemplateManagerException {

        log.info("Test org addNotificationTemplate(): " + notificationTemplate.getDisplayName() + " locale: " + notificationTemplate.getLocale());

        // Registry impl creates template type if not exists
//        if (!isEmailTemplateTypeExists(emailTemplate.getTemplateDisplayName(), tenantDomain)) {
//            addEmailTemplateType(emailTemplate.getTemplateDisplayName(), tenantDomain);
//        }

        try {
//            if (isEmailTemplateExists(emailTemplate.getTemplateDisplayName(), emailTemplate.getLocale(), tenantDomain)) {
//                // Registry impl updates the template if exists
//                orgNotificationTemplateDAO.updateNotificationTemplate(notificationTemplate, notificationTemplate.getNotificationChannel(), getTenantId(tenantDomain));
//            } else {
                orgNotificationTemplateDAO.addNotificationTemplate(notificationTemplate, notificationTemplate.getNotificationChannel(), getTenantId(tenantDomain));
//            }
        } catch (NotificationTemplateManagerServerException e) {
            throw new NotificationTemplateManagerException(e.getMessage(), e);
        } catch (I18nEmailMgtException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public NotificationTemplate getNotificationTemplate(String notificationChannel, String templateType, String locale, String tenantDomain) throws NotificationTemplateManagerException {

        log.info("Test org getNotificationTemplate(): " + templateType + " locale: " + locale);

        NotificationTemplate notificationTemplate;
        try {
            notificationTemplate = orgNotificationTemplateDAO.getNotificationTemplate(locale, templateType, notificationChannel, getTenantId(tenantDomain));
        } catch (NotificationTemplateManagerServerException e) {
            throw new NotificationTemplateManagerException(e.getMessage(), e);
        } catch (I18nEmailMgtException e) {
            throw new RuntimeException(e);
        }

        // TODO: Check why its only SMS here in registry impl
        // Handle not having the requested SMS template type in required locale for this tenantDomain.
//        if (notificationTemplate == null) {
//            return getDefaultTemplate(templateTypeDisplayName, locale, tenantDomain);
//        }

        // TODO: Remove following as its not needed when retrieving, but registry impl has it
//        validateContent(templateTypeDisplayName, locale, notificationTemplate);
        return notificationTemplate;
    }

    @Override
    public void addNotificationTemplate(NotificationTemplate notificationTemplate, String tenantDomain, String applicationUuid) throws NotificationTemplateManagerException {

        log.info("Test app addNotificationTemplate(): " + notificationTemplate.getDisplayName() + " locale: " + notificationTemplate.getLocale());

        // Registry impl creates template type if not exists
//        if (!isEmailTemplateTypeExists(emailTemplate.getTemplateDisplayName(), tenantDomain)) {
//            addEmailTemplateType(emailTemplate.getTemplateDisplayName(), tenantDomain);
//        }

        try {
//            if (isEmailTemplateExists(emailTemplate.getTemplateDisplayName(), emailTemplate.getLocale(), tenantDomain, applicationUuid)) {
//                // Registry impl updates the template if exists
//                appNotificationTemplateDAO.updateNotificationTemplate(notificationTemplate, EMAIL_CHANNEL, applicationUuid, getTenantId(tenantDomain));
//            } else {
                appNotificationTemplateDAO.addNotificationTemplate(notificationTemplate, notificationTemplate.getNotificationChannel(), applicationUuid, getTenantId(tenantDomain));
//            }
        } catch (NotificationTemplateManagerServerException e) {
            throw new NotificationTemplateManagerException(e.getMessage(), e);
        } catch (I18nEmailMgtException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public NotificationTemplate getNotificationTemplate(String notificationChannel, String templateType, String locale, String tenantDomain, String applicationUuid) throws NotificationTemplateManagerException {

        log.info("Test app getNotificationTemplate(): " + templateType);

        NotificationTemplate notificationTemplate;
        try {
            notificationTemplate = appNotificationTemplateDAO.getNotificationTemplate(locale, templateType, notificationChannel, applicationUuid, getTenantId(tenantDomain));
        } catch (NotificationTemplateManagerServerException e) {
            throw new NotificationTemplateManagerException(e.getMessage(), e);
        } catch (I18nEmailMgtException e) {
            throw new RuntimeException(e);
        }

        // TODO: Check why its only SMS here in registry impl
        // Handle not having the requested SMS template type in required locale for this tenantDomain.
//        if (notificationTemplate == null) {
//            return getDefaultTemplate(templateTypeDisplayName, locale, tenantDomain);
//        }

        // TODO: Remove following as its not needed when retrieving, but registry impl has it
//        validateContent(templateTypeDisplayName, locale, notificationTemplate);
        return notificationTemplate;
    }

    @Override
    public void addDefaultNotificationTemplates(String notificationChannel, String tenantDomain) throws NotificationTemplateManagerException {

        log.info(">>>>>>>>>>>>>>>> addDefaultNotificationTemplates()");
        getDefaultNotificationTemplates(notificationChannel).forEach(notificationTemplate -> {
            try {
                if (!isNotificationTemplateTypeExists(notificationTemplate.getDisplayName(), notificationChannel, tenantDomain)) {
                    addNotificationTemplateType(notificationTemplate.getDisplayName(), notificationChannel, tenantDomain);
                }
                addNotificationTemplate(notificationTemplate, tenantDomain);
            } catch (NotificationTemplateManagerException e) {
                // TODO: Handle errors
                log.error("Error while adding default notification templates for channel: " + notificationChannel + " the tenant: " + tenantDomain, e);
            }
        });
    }

    @Override
    public List<NotificationTemplate> getDefaultNotificationTemplates(String notificationChannel) {

        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            return I18nMgtDataHolder.getInstance().getDefaultSMSTemplates();
        }
        return I18nMgtDataHolder.getInstance().getDefaultEmailTemplates();
    }
}
