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

package org.wso2.carbon.email.mgt.internal;

import org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;

public class I18nMgtDataHolder{
    private RealmService realmService;
    private RegistryService registryService;
    private RegistryResourceMgtService registryResourceMgtService;

    private static I18nMgtDataHolder instance = new I18nMgtDataHolder();

    private I18nMgtDataHolder() {
    }

    public static I18nMgtDataHolder getInstance() {
        return instance;
    }

    public RealmService getRealmService() {
        if (realmService == null) {
            throw new RuntimeException("Realm Service has not been set. Component has not initialized properly.");
        }
        return realmService;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

    public RegistryService getRegistryService() {
        if (registryService == null) {
            throw new RuntimeException("Registry Service has not been set. Component has not initialized properly.");
        }
        return registryService;
    }

    public void setRegistryService(RegistryService registryService) {
        this.registryService = registryService;
    }

    public RegistryResourceMgtService getRegistryResourceMgtService() {
        if (registryResourceMgtService == null) {
            throw new RuntimeException("Registry Resource Mgt Service has not been set." +
                    "Component has not initialized properly.");
        }
        return registryResourceMgtService;
    }

    public void setRegistryResourceMgtService(RegistryResourceMgtService registryResourceMgtService) {
        this.registryResourceMgtService = registryResourceMgtService;
    }
}
