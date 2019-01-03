package org.wso2.carbon.identity.event.handler.notification;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.event.stream.core.EventStreamService;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.core.handler.InitConfig;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.event.handler.notification.email.bean.Notification;
import org.wso2.carbon.identity.event.handler.notification.exception.NotificationRuntimeException;
import org.wso2.carbon.identity.event.handler.notification.internal.NotificationHandlerDataHolder;
import org.wso2.carbon.identity.event.handler.notification.util.NotificationUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * DefaultNotificationHandler is based on simple key/value parameters which was send by an event to any given stream.
 *
 * We can define the streamid and the notification template from the config file (identity-event.properties)
 * But notification template is not implemented as the default implementation.
 *
 */
public class DefaultNotificationHandler extends AbstractEventHandler {
    private static final Log log = LogFactory.getLog(DefaultNotificationHandler.class);

    private static final String STREAM_DEFINITION_ID = "stream" ;
    private static final String NOTIFICATION_TEMPLATE_TYPE = "notification_template" ;

    private static final String DEFAULT_STREAM_ID = "id_gov_notify_stream:1.0.0";

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        Map<String, String> arbitraryDataMap = buildNotificationData(event);
        publishToStream(arbitraryDataMap, event);
    }

    /**
     * This method will build the specific notification data which under this module.
     *
     * @param event
     * @return
     * @throws IdentityEventException
     */
    protected Map<String, String> buildNotificationData(Event event) throws IdentityEventException{

        Map<String, String> arbitraryDataMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : event.getEventProperties().entrySet()) {
            if (entry.getValue() instanceof String) {
                arbitraryDataMap.put(entry.getKey(), (String) entry.getValue());
            }
        }

        String sendTo = arbitraryDataMap.get(NotificationConstants.EmailNotification.ARBITRARY_SEND_TO);
        Map<String, String> userClaims = new HashMap<>();

        String notificationTemplate = getNotificationTemplate(event);
        if(StringUtils.isEmpty(notificationTemplate)) {
            notificationTemplate = (String) event.getEventProperties().get(NotificationConstants.EmailNotification.EMAIL_TEMPLATE_TYPE);
        }

        if(StringUtils.isNotEmpty(notificationTemplate)) {

            String username = (String) event.getEventProperties().get(IdentityEventConstants.EventProperty.USER_NAME);
            org.wso2.carbon.user.core.UserStoreManager userStoreManager = (org.wso2.carbon.user.core.UserStoreManager) event.getEventProperties().get(
                    IdentityEventConstants.EventProperty.USER_STORE_MANAGER);
            String userStoreDomainName = (String) event.getEventProperties().get(IdentityEventConstants.EventProperty.USER_STORE_DOMAIN);
            String tenantDomain = (String) event.getEventProperties().get(IdentityEventConstants.EventProperty.TENANT_DOMAIN);
            String sendFrom = (String) event.getEventProperties().get(NotificationConstants.EmailNotification.ARBITRARY_SEND_FROM);

            if (StringUtils.isNotBlank(username) && userStoreManager != null) {
                userClaims = NotificationUtil.getUserClaimValues(username, userStoreManager);
            } else if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(userStoreDomainName) &&
                    StringUtils.isNotBlank(tenantDomain)) {
                userClaims = NotificationUtil.getUserClaimValues(username, userStoreDomainName, tenantDomain);
            }

            String locale = NotificationConstants.EmailNotification.LOCALE_DEFAULT;
            if (userClaims.containsKey(NotificationConstants.EmailNotification.CLAIM_URI_LOCALE)) {
                locale = userClaims.get(NotificationConstants.EmailNotification.CLAIM_URI_LOCALE);
            }

            if(StringUtils.isEmpty(sendTo)) {
                if (userClaims.containsKey(NotificationConstants.EmailNotification.CLAIM_URI_EMAIL)) {
                    sendTo = userClaims.get(NotificationConstants.EmailNotification.CLAIM_URI_EMAIL);
                }
            }

            EmailTemplate emailTemplate;
            try {
                emailTemplate = NotificationHandlerDataHolder.getInstance().getEmailTemplateManager().getEmailTemplate(notificationTemplate, locale, tenantDomain);
            } catch (I18nEmailMgtException e) {
                String message = "Error when retrieving template from tenant registry.";
                throw NotificationRuntimeException.error(message, e);
            }

            NotificationUtil.getPlaceholderValues(emailTemplate, arbitraryDataMap, userClaims);

            Notification.EmailNotificationBuilder builder =
                    new Notification.EmailNotificationBuilder(sendTo);
            builder.setSendFrom(sendFrom);
            builder.setTemplate(emailTemplate);
            builder.setPlaceHolderData(arbitraryDataMap);
            Notification notification = builder.build();

            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_EVENT_TYPE, I18nEmailUtil.getNormalizedName(notificationTemplate));
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_SEND_FROM, notification.getSendFrom());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_SUBJECT_TEMPLATE, notification.
                    getTemplate().getSubject());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_BODY_TEMPLATE, notification.
                    getTemplate().getBody());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_FOOTER_TEMPLATE, notification.
                    getTemplate().getFooter());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_LOCALE, notification.getTemplate().
                    getLocale());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_CONTENT_TYPE, notification.
                    getTemplate().getEmailContentType());
            //arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_SEND_TO, notification.getSendTo());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_SUBJECT, notification.getSubject());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_BODY, notification.getBody());
            arbitraryDataMap.put(NotificationConstants.EmailNotification.ARBITRARY_FOOTER, notification.getFooter());
        }


        Map<String, String> arbitraryDataClaims = getArbitraryDataClaimsFromProperties(event);
        Set<String> keys = arbitraryDataClaims.keySet();
        for (String key : keys) {
            String claim = arbitraryDataClaims.get(key);
            String value = userClaims.get(claim);
            arbitraryDataMap.put(key, value);
        }

        Map<String, String> arbitraryDataFromProperties = getArbitraryDataFromProperties(event);
        arbitraryDataMap.putAll(arbitraryDataFromProperties);

        return arbitraryDataMap ;
    }


    /**
     *
     * This method is used to publish the event to the event service.
     *
     * @param dataMap
     * @param event
     * @throws IdentityEventException
     */
    protected void publishToStream(Map<String, String> dataMap, Event event) throws IdentityEventException{

        EventStreamService service = NotificationHandlerDataHolder.getInstance().getEventStreamService();
        org.wso2.carbon.databridge.commons.Event databridgeEvent = new org.wso2.carbon.databridge.commons.Event();
        databridgeEvent.setTimeStamp(System.currentTimeMillis());

        try {
            databridgeEvent.setStreamId(getStreamDefinitionID(event));
        } catch (IdentityEventException e) {
            throw new IdentityEventException("Stream definition was not specified in the " +
                    "identity-event.properties file for " + event.getEventName());
        }

        databridgeEvent.setArbitraryDataMap(dataMap);
        service.publish(databridgeEvent);
    }

    private Map<String, String> getArbitraryDataClaimsFromProperties(Event event) throws IdentityEventException {
        Map<String, String> claimMap = new HashMap<>();
        String tmpKeyStartWith = this.getName() + ".subscription." + event.getEventName() + "." + "claim" ;
        Properties subscriptionProperties = getSubscriptionProperties(event.getEventName());
        Set<Object> subscriptionPropertyKeys = subscriptionProperties.keySet();
        for (Object subscriptionPropertyKey : subscriptionPropertyKeys) {
            String key = (String)subscriptionPropertyKey;
            if(key.startsWith(tmpKeyStartWith)){
                String attribute = key.substring(tmpKeyStartWith.length() + 1);
                String value = (String)subscriptionProperties.get(key);
                claimMap.put(attribute, value);
            }
        }
        return claimMap;
    }

    private Map<String, String> getArbitraryDataFromProperties(Event event) throws IdentityEventException {
        Map<String, String> dataMap = new HashMap<>();
        String streamIdKey =  this.getName() + ".subscription." + event.getEventName() + "." + STREAM_DEFINITION_ID;
        String templateType =  this.getName() + ".subscription." + event.getEventName() + "." + NOTIFICATION_TEMPLATE_TYPE;
        String claimKeyStartWith = this.getName() + ".subscription." + event.getEventName() + "." + "claim" ;

        Properties subscriptionProperties = getSubscriptionProperties(event.getEventName());
        Set<Object> subscriptionPropertyKeys = subscriptionProperties.keySet();
        for (Object subscriptionPropertyKey : subscriptionPropertyKeys) {
            String key = (String)subscriptionPropertyKey;
            if(!key.startsWith(claimKeyStartWith) && !key.equalsIgnoreCase(streamIdKey) &&  !key.equalsIgnoreCase(templateType)){
                String keyPrefix = this.getName() + ".subscription." + event.getEventName();
                String attribute = key.substring(keyPrefix.length() + 1);
                String value = (String)subscriptionProperties.get(key);
                dataMap.put(attribute, value);
            }
        }
        return dataMap;
    }


    @Override
    public String getName() {
        return "default.notification.sender";
    }

    protected String getStreamDefinitionID(Event event) throws IdentityEventException {
        String streamDefinitionID = getSubscriptionProperty(STREAM_DEFINITION_ID, event.getEventName());
        if(StringUtils.isEmpty(streamDefinitionID)){
            streamDefinitionID = DEFAULT_STREAM_ID ;
        }
        return streamDefinitionID;
    }

    protected String getNotificationTemplate(Event event) throws IdentityEventException {
        return getSubscriptionProperty(NOTIFICATION_TEMPLATE_TYPE, event.getEventName());
    }
}
