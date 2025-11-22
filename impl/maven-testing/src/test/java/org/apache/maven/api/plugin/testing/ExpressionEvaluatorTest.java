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
package org.apache.maven.api.plugin.testing;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.testing.stubs.ProjectStub;
import org.apache.maven.api.plugin.testing.stubs.SessionMock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

/**
 * @author Edwin Punzalan
 */
@MojoTest
public class ExpressionEvaluatorTest {

    private static final String LOCAL_REPO = "target/local-repo/";
    private static final String GROUP_ID = "test";
    private static final String ARTIFACT_ID = "test-plugin";
    private static final String COORDINATES = GROUP_ID + ":" + ARTIFACT_ID + ":0.0.1-SNAPSHOT:goal";
    private static final String CONFIG = "<project>\n"
            + "    <build>\n"
            + "        <plugins>\n"
            + "            <plugin>\n"
            + "                <groupId>" + GROUP_ID + "</groupId>\n"
            + "                <artifactId>" + ARTIFACT_ID + "</artifactId>\n"
            + "                <configuration>\n"
            + "                    <basedir>${project.basedir}</basedir>\n"
            + "                    <workdir>${project.basedir}/workDirectory</workdir>\n"
            + "                </configuration>\n"
            + "            </plugin>\n"
            + "        </plugins>\n"
            + "    </build>\n"
            + "</project>\n";

    @Test
    @InjectMojo(goal = COORDINATES, pom = CONFIG)
    public void testInjection(ExpressionEvaluatorMojo mojo) {
        assertNotNull(mojo.basedir);
        assertNotNull(mojo.workdir);
        assertDoesNotThrow(mojo::execute);
    }

    @Test
    @InjectMojo(goal = COORDINATES, pom = CONFIG)
    @Basedir("${basedir}/target/test-classes")
    @MojoParameter(name = "param", value = "paramValue")
    public void testParam(ExpressionEvaluatorMojo mojo) {
        assertNotNull(mojo.basedir);
        assertNotNull(mojo.workdir);
        assertEquals("paramValue", mojo.param);
        assertDoesNotThrow(mojo::execute);
    }

    @Test
    @InjectMojo(goal = COORDINATES, pom = CONFIG)
    @MojoParameter(name = "param", value = "paramValue")
    @MojoParameter(name = "param2", value = "param2Value")
    public void testParams(ExpressionEvaluatorMojo mojo) {
        assertNotNull(mojo.basedir);
        assertNotNull(mojo.workdir);
        assertEquals("paramValue", mojo.param);
        assertEquals("param2Value", mojo.param2);
        assertDoesNotThrow(mojo::execute);
    }

    @Test
    @InjectMojo(goal = COORDINATES, pom = CONFIG)
    @Basedir("${basedir}/target/test-classes")
    @MojoParameter(name = "strings", value = "<string>value1</string><string>value2</string>")
    public void testComplexParam(ExpressionEvaluatorMojo mojo) {
        assertNotNull(mojo.basedir);
        assertNotNull(mojo.workdir);
        assertEquals(List.of("value1", "value2"), mojo.strings);
    }

    @Test
    @InjectMojo(goal = COORDINATES, pom = CONFIG)
    @Basedir("${basedir}/target/test-classes")
    @MojoParameter(name = "strings", value = "value1,value2", xml = false)
    public void testCommaSeparatedParam(ExpressionEvaluatorMojo mojo) {
        assertNotNull(mojo.basedir);
        assertNotNull(mojo.workdir);
        assertEquals(List.of("value1", "value2"), mojo.strings);
    }

    @Test
    @InjectMojo(goal = COORDINATES, pom = CONFIG)
    @MojoParameter(name = "stringArray", value = "<string>item1</string><string>item2</string><string>item3</string>")
    public void testStringArray(ExpressionEvaluatorMojo mojo) {
        assertNotNull(mojo.stringArray);
        assertEquals(3, mojo.stringArray.length);
        assertEquals("item1", mojo.stringArray[0]);
        assertEquals("item2", mojo.stringArray[1]);
        assertEquals("item3", mojo.stringArray[2]);
    }

    @Test
    @InjectMojo(goal = COORDINATES, pom = CONFIG)
    @MojoParameter(name = "mapParam", value = "<key1>value1</key1><key2>value2</key2>")
    public void testMapParam(ExpressionEvaluatorMojo mojo) {
        assertNotNull(mojo.mapParam);
        assertEquals(2, mojo.mapParam.size());
        assertEquals("value1", mojo.mapParam.get("key1"));
        assertEquals("value2", mojo.mapParam.get("key2"));
    }

    @Test
    @InjectMojo(goal = COORDINATES, pom = CONFIG)
    @MojoParameter(
            name = "propertiesParam",
            value = "<property><name>prop1</name><value>val1</value></property>"
                    + "<property><name>prop2</name><value>val2</value></property>")
    public void testPropertiesParam(ExpressionEvaluatorMojo mojo) {
        assertNotNull(mojo.propertiesParam);
        assertEquals(2, mojo.propertiesParam.size());
        assertEquals("val1", mojo.propertiesParam.getProperty("prop1"));
        assertEquals("val2", mojo.propertiesParam.getProperty("prop2"));
    }

    @Test
    @InjectMojo(goal = COORDINATES, pom = CONFIG)
    @MojoParameter(name = "beanParam", value = "<field1>fieldValue</field1><field2>42</field2>")
    public void testBeanParam(ExpressionEvaluatorMojo mojo) {
        assertNotNull(mojo.beanParam);
        assertEquals("fieldValue", mojo.beanParam.field1);
        assertEquals(42, mojo.beanParam.field2);
    }

    @Test
    @InjectMojo(goal = COORDINATES, pom = CONFIG)
    @MojoParameter(name = "intValue", value = "123")
    public void testIntValue(ExpressionEvaluatorMojo mojo) {
        assertEquals(123, mojo.intValue);
    }

    @Test
    @InjectMojo(goal = COORDINATES, pom = CONFIG)
    @MojoParameter(name = "boolValue", value = "true")
    public void testBoolValue(ExpressionEvaluatorMojo mojo) {
        assertEquals(true, mojo.boolValue);
    }

    @Test
    @InjectMojo(goal = COORDINATES, pom = CONFIG)
    @MojoParameter(name = "strings", value = "one,two,three,four", xml = false)
    public void testCommaSeparatedListWithXmlFalse(ExpressionEvaluatorMojo mojo) {
        assertNotNull(mojo.strings);
        assertEquals(4, mojo.strings.size());
        assertEquals("one", mojo.strings.get(0));
        assertEquals("two", mojo.strings.get(1));
        assertEquals("three", mojo.strings.get(2));
        assertEquals("four", mojo.strings.get(3));
    }

    @Test
    @InjectMojo(goal = COORDINATES, pom = CONFIG)
    @MojoParameter(
            name = "strings",
            value = "<string>alpha</string><string>beta</string><string>gamma</string>",
            xml = true)
    public void testListWithXmlTrue(ExpressionEvaluatorMojo mojo) {
        assertNotNull(mojo.strings);
        assertEquals(3, mojo.strings.size());
        assertEquals("alpha", mojo.strings.get(0));
        assertEquals("beta", mojo.strings.get(1));
        assertEquals("gamma", mojo.strings.get(2));
    }

    @Test
    @InjectMojo(goal = COORDINATES, pom = CONFIG)
    @MojoParameter(name = "strings", value = "value-with-<special>&chars", xml = false)
    public void testSpecialCharactersWithXmlFalse(ExpressionEvaluatorMojo mojo) {
        assertNotNull(mojo.strings);
        assertEquals(1, mojo.strings.size());
        assertEquals("value-with-<special>&chars", mojo.strings.get(0));
    }

    @Test
    @InjectMojo(goal = COORDINATES, pom = CONFIG)
    @MojoParameter(name = "stringArray", value = "a,b,c", xml = false)
    public void testArrayWithCommaSeparated(ExpressionEvaluatorMojo mojo) {
        assertNotNull(mojo.stringArray);
        assertEquals(3, mojo.stringArray.length);
        assertEquals("a", mojo.stringArray[0]);
        assertEquals("b", mojo.stringArray[1]);
        assertEquals("c", mojo.stringArray[2]);
    }

    @Mojo(name = "goal")
    @Named("test:test-plugin:0.0.1-SNAPSHOT:goal") // this one is usually generated by maven-plugin-plugin
    public static class ExpressionEvaluatorMojo implements org.apache.maven.api.plugin.Mojo {
        private Path basedir;

        private Path workdir;

        private String param;

        private String param2;

        private List<String> strings;

        private String[] stringArray;

        private Map<String, String> mapParam;

        private Properties propertiesParam;

        private TestBean beanParam;

        private int intValue;

        private boolean boolValue;

        /** {@inheritDoc} */
        @Override
        public void execute() throws MojoException {
            if (basedir == null) {
                throw new MojoException("basedir was not injected.");
            }

            if (workdir == null) {
                throw new MojoException("workdir was not injected.");
            } else if (!workdir.startsWith(basedir)) {
                throw new MojoException("workdir does not start with basedir.");
            }
        }
    }

    /**
     * A simple bean for testing complex parameter injection.
     */
    public static class TestBean {
        private String field1;
        private int field2;

        public String getField1() {
            return field1;
        }

        public void setField1(String field1) {
            this.field1 = field1;
        }

        public int getField2() {
            return field2;
        }

        public void setField2(int field2) {
            this.field2 = field2;
        }
    }

    @Provides
    @SuppressWarnings("unused")
    Session session() {
        Session session = SessionMock.getMockSession(LOCAL_REPO);
        doReturn(new Properties()).when(session).getSystemProperties();
        doReturn(new Properties()).when(session).getUserProperties();
        doAnswer(iom -> Paths.get(MojoExtension.getBasedir())).when(session).getRootDirectory();
        return session;
    }

    @Provides
    Project project() {
        ProjectStub project = new ProjectStub();
        project.setBasedir(Paths.get(MojoExtension.getBasedir()));
        return project;
    }
}
