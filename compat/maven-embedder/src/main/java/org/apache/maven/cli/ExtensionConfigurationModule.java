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
package org.apache.maven.cli;

import java.util.function.UnaryOperator;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.extension.internal.CoreExtensionEntry;
import org.apache.maven.impl.model.DefaultInterpolator;
import org.apache.maven.internal.xml.XmlNodeImpl;
import org.apache.maven.internal.xml.XmlPlexusConfiguration;
import org.apache.maven.model.v4.MavenTransformer;
import org.codehaus.plexus.configuration.PlexusConfiguration;

@Deprecated
public class ExtensionConfigurationModule implements Module {

    private final CoreExtensionEntry extension;
    private final UnaryOperator<String> callback;
    private final DefaultInterpolator interpolator = new DefaultInterpolator();

    public ExtensionConfigurationModule(CoreExtensionEntry extension, UnaryOperator<String> callback) {
        this.extension = extension;
        this.callback = callback;
    }

    @Override
    public void configure(Binder binder) {
        if (extension.getKey() != null) {
            XmlNode configuration = extension.getConfiguration();
            if (configuration == null) {
                configuration = new XmlNodeImpl("configuration");
            }
            UnaryOperator<String> cb = Interpolator.memoize(callback);
            UnaryOperator<String> it = s -> interpolator.interpolate(s, cb);
            configuration = new ExtensionInterpolator(it).transform(configuration);

            binder.bind(XmlNode.class)
                    .annotatedWith(Names.named(extension.getKey()))
                    .toInstance(configuration);
            binder.bind(PlexusConfiguration.class)
                    .annotatedWith(Names.named(extension.getKey()))
                    .toInstance(XmlPlexusConfiguration.toPlexusConfiguration(configuration));
        }
    }

    static class ExtensionInterpolator extends MavenTransformer {
        ExtensionInterpolator(UnaryOperator<String> transformer) {
            super(transformer);
        }

        public XmlNode transform(XmlNode node) {
            return super.transform(node);
        }
    }
}
