/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.event.handler.notification.util;

import org.json.JSONObject;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit test class for NotificationEventHandlerAuditLogger class.
 */
public class NotificationEventHandlerAuditLoggerTest {

    private NotificationEventHandlerAuditLogger auditLogger;
    private CarbonContext carbonContext;

    private MockedStatic<CarbonContext> mockedCarbonContext;
    private MockedStatic<UserCoreUtil> mockedUserCoreUtil;
    private MockedStatic<MultitenantUtils> mockedMultitenantUtils;
    private MockedStatic<IdentityUtil> mockedIdentityUtil;
    private MockedStatic<LoggerUtils> mockedLoggerUtils;

    @BeforeMethod
    public void setUp() {

        System.setProperty("carbon.home", ".");
        MockitoAnnotations.openMocks(this);

        // Setup mocks BEFORE creating the audit logger instance
        mockedCarbonContext = mockStatic(CarbonContext.class);
        mockedUserCoreUtil = mockStatic(UserCoreUtil.class);
        mockedMultitenantUtils = mockStatic(MultitenantUtils.class);
        mockedIdentityUtil = mockStatic(IdentityUtil.class);
        mockedLoggerUtils = mockStatic(LoggerUtils.class);

        carbonContext = mock(CarbonContext.class);
        mockedCarbonContext.when(CarbonContext::getThreadLocalCarbonContext).thenReturn(carbonContext);
        when(carbonContext.getUsername()).thenReturn("testUser");
        when(carbonContext.getTenantDomain()).thenReturn("carbon.super");

        mockedUserCoreUtil.when(() -> UserCoreUtil.addTenantDomainToEntry("testUser", "carbon.super"))
                .thenReturn("testUser@carbon.super");
        mockedMultitenantUtils.when(() -> MultitenantUtils.getTenantAwareUsername("testUser@carbon.super"))
                .thenReturn("testUser");
        mockedMultitenantUtils.when(() -> MultitenantUtils.getTenantDomain("testUser@carbon.super"))
                .thenReturn("carbon.super");
        mockedIdentityUtil.when(() -> IdentityUtil.getInitiatorId("testUser", "carbon.super"))
                .thenReturn("initiator-id-test");
        mockedLoggerUtils.when(() -> LoggerUtils.getMaskedContent(anyString())).thenReturn("masked-content");
        auditLogger = new NotificationEventHandlerAuditLogger();
    }

    @AfterMethod
    public void tearDown() {

        mockedCarbonContext.close();
        mockedUserCoreUtil.close();
        mockedMultitenantUtils.close();
        mockedIdentityUtil.close();
        mockedLoggerUtils.close();
    }

    /**
     * Test the private method 'getUser' for a regular, tenant-aware user.
     */
    @Test
    public void testGetUserRegularUser() throws Exception {

        when(carbonContext.getUsername()).thenReturn("admin");
        when(carbonContext.getTenantDomain()).thenReturn("carbon.super");
        mockedUserCoreUtil.when(() -> UserCoreUtil.addTenantDomainToEntry("admin", "carbon.super"))
                .thenReturn("admin@carbon.super");

        Method getUserMethod = NotificationEventHandlerAuditLogger.class.getDeclaredMethod("getUser");
        getUserMethod.setAccessible(true);
        String result = (String) getUserMethod.invoke(auditLogger);
        Assert.assertEquals(result, "admin@carbon.super");
    }

    /**
     * Test the private method 'getUser' for the system user.
     */
    @Test
    public void testGetUserWithSystemUser() throws Exception {

        when(carbonContext.getUsername()).thenReturn(null);
        Method getUserMethod = NotificationEventHandlerAuditLogger.class.getDeclaredMethod("getUser");
        getUserMethod.setAccessible(true);
        String result = (String) getUserMethod.invoke(auditLogger);
        Assert.assertEquals(result, CarbonConstants.REGISTRY_SYSTEM_USERNAME);
    }

    /**
     * Test the private method 'createPushAuditLogEntry' with valid data.
     */
    @Test
    public void testCreatePushAuditLogEntryWithValidData() throws Exception {

        Method createAuditLogEntryMethod = NotificationEventHandlerAuditLogger.class
                .getDeclaredMethod("createPushAuditLogEntry", String.class, String.class, String.class);
        createAuditLogEntryMethod.setAccessible(true);
        JSONObject result = (JSONObject) createAuditLogEntryMethod.invoke(auditLogger,
                "testUserId", "FCM", "carbon.super");

        Assert.assertNotNull(result);
        Assert.assertTrue(result.has("UserId"));
        Assert.assertEquals(result.getString("UserId"), "testUserId");
        Assert.assertTrue(result.has("Provider"));
        Assert.assertEquals(result.getString("Provider"), "FCM");
        Assert.assertTrue(result.has("TenantDomain"));
        Assert.assertEquals(result.getString("TenantDomain"), "carbon.super");
    }

    /**
     * Test the private method 'getInitiatorId'.
     */
    @Test
    public void testGetInitiatorId() throws Exception {

        when(carbonContext.getUsername()).thenReturn("testUser");
        when(carbonContext.getTenantDomain()).thenReturn("carbon.super");
        mockedUserCoreUtil.when(() -> UserCoreUtil.addTenantDomainToEntry("testUser", "carbon.super"))
                .thenReturn("testUser@carbon.super");
        mockedMultitenantUtils.when(() -> MultitenantUtils.getTenantAwareUsername("testUser@carbon.super"))
                .thenReturn("testUser");
        mockedMultitenantUtils.when(() -> MultitenantUtils.getTenantDomain("testUser@carbon.super"))
                .thenReturn("carbon.super");
        mockedIdentityUtil.when(() -> IdentityUtil.getInitiatorId("testUser", "carbon.super"))
                .thenReturn("initiator-id-12345");

        Method getInitiatorIdMethod = NotificationEventHandlerAuditLogger.class.getDeclaredMethod("getInitiatorId");
        getInitiatorIdMethod.setAccessible(true);
        String result = (String) getInitiatorIdMethod.invoke(auditLogger);
        Assert.assertEquals(result, "initiator-id-12345");
    }

    /**
     * Test the private method 'getInitiatorId' for system user.
     */
    @Test
    public void testGetInitiatorIdSystemUser() throws Exception {

        when(carbonContext.getUsername()).thenReturn(null);
        mockedUserCoreUtil.when(() -> UserCoreUtil.addTenantDomainToEntry(null, "carbon.super"))
                .thenReturn(null);
        mockedMultitenantUtils.when(() ->
                        MultitenantUtils.getTenantAwareUsername(CarbonConstants.REGISTRY_SYSTEM_USERNAME))
                .thenReturn(CarbonConstants.REGISTRY_SYSTEM_USERNAME);
        mockedMultitenantUtils.when(() ->
                        MultitenantUtils.getTenantDomain(CarbonConstants.REGISTRY_SYSTEM_USERNAME))
                .thenReturn("carbon.super");
        mockedIdentityUtil.when(() ->
                        IdentityUtil.getInitiatorId(CarbonConstants.REGISTRY_SYSTEM_USERNAME, "carbon.super"))
                .thenReturn(null);

        Method getInitiatorIdMethod = NotificationEventHandlerAuditLogger.class.getDeclaredMethod("getInitiatorId");
        getInitiatorIdMethod.setAccessible(true);
        String result = (String) getInitiatorIdMethod.invoke(auditLogger);
        Assert.assertEquals(result, LoggerUtils.Initiator.System.name());
    }
}
