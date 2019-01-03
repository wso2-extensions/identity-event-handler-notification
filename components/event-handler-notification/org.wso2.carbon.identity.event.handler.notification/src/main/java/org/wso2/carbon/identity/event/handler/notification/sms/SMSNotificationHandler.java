package org.wso2.carbon.identity.event.handler.notification.sms;

import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.notification.DefaultNotificationHandler;

import java.util.Map;

public class SMSNotificationHandler extends DefaultNotificationHandler {
    @Override
    public void handleEvent(Event event) throws IdentityEventException {
        super.handleEvent(event);
    }

    @Override
    protected Map<String, String> buildNotificationData(Event event) throws IdentityEventException {
        return super.buildNotificationData(event);
    }

    @Override
    protected void publishToStream(Map<String, String> dataMap, Event event) throws IdentityEventException {
        super.publishToStream(dataMap, event);
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    protected String getStreamDefinitionID(Event event) throws IdentityEventException {
        return super.getStreamDefinitionID(event);
    }

    @Override
    protected String getNotificationTemplate(Event event) throws IdentityEventException {
        return super.getNotificationTemplate(event);
    }
}
