/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.session.scope;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.inject.OutOfScopeException;
import org.apache.maven.SessionScoped;
import org.apache.maven.api.Session;
import org.apache.maven.session.scope.internal.SessionScope;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@PlexusTest
@ExtendWith(MockitoExtension.class)
public class SessionScopeProxyTest {

    @Mock
    Session session;

    @Inject
    SessionScope sessionScope;

    @Inject
    PlexusContainer container;

    @Test
    void testProxiedSessionScopedBean() throws ComponentLookupException {
        MySingletonBean bean = container.lookup(MySingletonBean.class);
        assertNotNull(bean);
        assertNotNull(bean.myBean);

        assertThrows(OutOfScopeException.class, () -> bean.myBean.getSession());

        sessionScope.enter();
        sessionScope.seed(Session.class, this.session);
        assertNotNull(bean.myBean.getSession());
    }

    @Named
    @Singleton
    static class MySingletonBean {
        @Inject
        MySessionScopedBean myBean;
    }

    @Named
    @SessionScoped
    static class MySessionScopedBean {
        @Inject
        Session session;

        public Session getSession() {
            return session;
        }
    }
}
