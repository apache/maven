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
import org.eclipse.sisu.Typed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        ComponentLookupException e =
                assertThrows(ComponentLookupException.class, () -> container.lookup(MySingletonBean2.class));
        assertTrue(e.getMessage().matches("[\\s\\S]*: Can not set .* field .* to [\\s\\S]*"));

        MySingletonBean bean = container.lookup(MySingletonBean.class);
        assertNotNull(bean);
        assertNotNull(bean.anotherBean);
        assertSame(bean.anotherBean.getClass(), AnotherBean.class);
        assertNotNull(bean.myBean);
        assertNotSame(bean.myBean.getClass(), MySessionScopedBean.class);

        assertThrows(OutOfScopeException.class, () -> bean.myBean.getSession());

        sessionScope.enter();
        sessionScope.seed(Session.class, this.session);
        assertNotNull(bean.myBean.getSession());
        assertNotNull(bean.myBean.getAnotherBean());
        assertSame(bean.myBean.getAnotherBean().getClass(), AnotherBean.class);
    }

    @Named
    static class MySingletonBean {
        @Inject
        @Named("scoped")
        BeanItf myBean;

        @Inject
        @Named("another")
        BeanItf2 anotherBean;
    }

    @Named
    static class MySingletonBean2 {
        @Inject
        @Named("scoped")
        MySessionScopedBean myBean;

        @Inject
        @Named("another")
        BeanItf2 anotherBean;
    }

    interface BeanItf {
        Session getSession();

        BeanItf2 getAnotherBean();
    }

    interface BeanItf2 {}

    @Named("another")
    @Singleton
    static class AnotherBean implements BeanItf2 {}

    @Named("scoped")
    @SessionScoped
    @Typed
    static class MySessionScopedBean implements BeanItf {
        @Inject
        Session session;

        @Inject
        BeanItf2 anotherBean;

        public Session getSession() {
            return session;
        }

        public BeanItf2 getAnotherBean() {
            return anotherBean;
        }
    }
}
