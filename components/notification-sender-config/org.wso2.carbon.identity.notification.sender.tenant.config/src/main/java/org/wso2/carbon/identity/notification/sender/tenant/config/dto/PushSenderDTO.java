package org.wso2.carbon.identity.notification.sender.tenant.config.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO for Push notification sender.
 */
public class PushSenderDTO {

    private String name;
    private String provider;
    private String providerId;
    private Map<String, String> properties = new HashMap<>();

    public String getProviderId() {

        return providerId;
    }

    public void setProviderId(String providerId) {

        this.providerId = providerId;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getProvider() {

        return provider;
    }

    public void setProvider(String provider) {

        this.provider = provider;
    }

    public Map<String, String> getProperties() {

        return properties;
    }

    public void setProperties(Map<String, String> properties) {

        this.properties = properties;
    }
}
