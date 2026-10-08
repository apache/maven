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
package org.apache.maven.configuration.internal;

import java.io.StringReader;
import org.apache.maven.api.xml.XmlService;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MavenBasicComponentConfiguratorTest {

    private MavenBasicComponentConfigurator configurator;
    private ClassRealm realm;
    private ExpressionEvaluator evaluator;

    @BeforeEach
    void setUp() throws Exception {
        configurator = new MavenBasicComponentConfigurator();
        ClassWorld world = new ClassWorld();
        realm = world.newRealm("test", getClass().getClassLoader());
        evaluator = new ExpressionEvaluator() {
            public Object evaluate(String expr) {
                return expr;
            }

            public java.io.File alignToBaseDirectory(java.io.File f) {
                return f;
            }
        };
    }

    private PlexusConfiguration toConfig(String xml) throws Exception {
        Xpp3Dom dom = new Xpp3Dom(XmlService.read(new StringReader("<configuration>" + xml + "</configuration>")));
        return new XmlPlexusConfiguration(dom);
    }

    @Test
    void testDirectSettingOverridesPreInitializedDefaultWhenEmpty() throws Exception {
        TestMojo mojo = new TestMojo();
        assertEquals("defaultDirect", mojo.direct);

        PlexusConfiguration config = toConfig("<direct></direct>");
        configurator.configureComponent(mojo, config, evaluator, realm, null);

        assertEquals("", mojo.direct);
    }

    @Test
    void testDirectSettingPreservesPreInitializedDefaultWhenSelfClosing() throws Exception {
        TestMojo mojo = new TestMojo();
        assertEquals("defaultDirect", mojo.direct);

        PlexusConfiguration config = toConfig("<direct/>");
        configurator.configureComponent(mojo, config, evaluator, realm, null);

        assertEquals("defaultDirect", mojo.direct);
    }

    @Test
    void testNestedSettingOverridesPreInitializedDefaultWhenEmpty() throws Exception {
        TestMojo mojo = new TestMojo();
        assertEquals("article,report,book", mojo.settings.docClassesToTargets);

        PlexusConfiguration config = toConfig("<settings><docClassesToTargets></docClassesToTargets></settings>");
        configurator.configureComponent(mojo, config, evaluator, realm, null);

        assertEquals("", mojo.settings.docClassesToTargets);
    }

    @Test
    void testNestedSettingPreservesPreInitializedDefaultWhenSelfClosing() throws Exception {
        TestMojo mojo = new TestMojo();
        assertEquals("article,report,book", mojo.settings.docClassesToTargets);

        PlexusConfiguration config = toConfig("<settings><docClassesToTargets/></settings>");
        configurator.configureComponent(mojo, config, evaluator, realm, null);

        assertEquals("article,report,book", mojo.settings.docClassesToTargets);
    }

    @Test
    void testEmptyTagOverridesDefaultValueAttribute() throws Exception {
        TestMojo mojo = new TestMojo();
        assertEquals("defaultDirect", mojo.direct);

        PlexusConfiguration config = toConfig("<direct default-value=\"fallback\"></direct>");
        configurator.configureComponent(mojo, config, evaluator, realm, null);

        assertEquals("", mojo.direct);
    }

    public static class TestMojo {
        public String direct = "defaultDirect";
        public TestSettings settings = new TestSettings();
    }

    public static class TestSettings {
        public String docClassesToTargets = "article,report,book";
    }
}
