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
package org.apache.maven.configuration;

import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.StringReader;

import org.apache.maven.configuration.internal.DefaultBeanConfigurator;
import org.apache.maven.internal.xml.XmlNodeStaxBuilder;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 */
class DefaultBeanConfiguratorTest {

    private BeanConfigurator configurator;

    @BeforeEach
    void setUp() throws Exception {
        configurator = new DefaultBeanConfigurator();
    }

    @AfterEach
    void tearDown() throws Exception {
        configurator = null;
    }

    private Xpp3Dom toConfig(String xml) {
        try {
            return new Xpp3Dom(XmlNodeStaxBuilder.build(
                    new StringReader("<configuration>" + xml + "</configuration>"),
                    (XmlNodeStaxBuilder.InputLocationBuilderStax) null));
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Test
    void testMinimal() throws BeanConfigurationException {
        SomeBean bean = new SomeBean();

        Xpp3Dom config = toConfig("<file>test</file>");

        DefaultBeanConfigurationRequest request = new DefaultBeanConfigurationRequest();
        request.setBean(bean).setConfiguration(config);

        configurator.configureBean(request);

        assertEquals(new File("test"), bean.file);
    }

    @Test
    void testPreAndPostProcessing() throws BeanConfigurationException {
        SomeBean bean = new SomeBean();

        Xpp3Dom config = toConfig("<file>${test}</file>");

        BeanConfigurationValuePreprocessor preprocessor = (value, type) -> {
            if (value != null && value.startsWith("${") && value.endsWith("}")) {
                return value.substring(2, value.length() - 1);
            }
            return value;
        };

        BeanConfigurationPathTranslator translator = path -> new File("base", path.getPath()).getAbsoluteFile();

        DefaultBeanConfigurationRequest request = new DefaultBeanConfigurationRequest();
        request.setBean(bean).setConfiguration(config);
        request.setValuePreprocessor(preprocessor).setPathTranslator(translator);

        configurator.configureBean(request);

        assertEquals(new File("base/test").getAbsoluteFile(), bean.file);
    }

    @Test
    void testChildConfigurationElement() throws BeanConfigurationException {
        SomeBean bean = new SomeBean();

        Xpp3Dom config = toConfig("<wrapper><file>test</file></wrapper>");

        DefaultBeanConfigurationRequest request = new DefaultBeanConfigurationRequest();
        request.setBean(bean).setConfiguration(config, "wrapper");

        configurator.configureBean(request);

        assertEquals(new File("test"), bean.file);
    }

    static class SomeBean {

        File file;
    }
}
