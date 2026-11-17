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
package org.apache.maven.cli.props;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.apache.maven.api.services.InterpolatorException;
import org.apache.maven.utils.StringInterpolatorException;

/**
 * @deprecated Use {@link org.apache.maven.utils.MavenProperties} instead.
 */
@Deprecated
public class MavenProperties extends org.apache.maven.utils.MavenProperties {
    public MavenProperties() {}

    public MavenProperties(Path location) throws IOException {
        super(location);
    }

    public MavenProperties(Path location, UnaryOperator<String> callback) throws IOException {
        super(location, callback);
    }

    public MavenProperties(boolean substitute) {
        super(substitute);
    }

    public MavenProperties(Path location, boolean substitute) {
        super(location, substitute);
    }

    @Override
    public boolean update(Map<String, String> props) {
        // Preserve dispatch through subclasses overriding the legacy typed overload.
        MavenProperties properties;
        if (props instanceof MavenProperties mavenProperties) {
            properties = mavenProperties;
        } else {
            properties = new MavenProperties();
            properties.putAll(props);
        }
        return update(properties);
    }

    public boolean update(MavenProperties properties) {
        return super.update(properties);
    }

    @Override
    public void substitute(UnaryOperator<String> callback) {
        try {
            super.substitute(callback);
        } catch (StringInterpolatorException e) {
            throw new InterpolatorException(e.getMessage(), e);
        }
    }

    public static class PropertiesReader extends org.apache.maven.utils.MavenProperties.PropertiesReader {
        public PropertiesReader(Reader reader, boolean maybeTyped) {
            super(reader, maybeTyped);
        }
    }

    public static class PropertiesWriter extends org.apache.maven.utils.MavenProperties.PropertiesWriter {
        public PropertiesWriter(Writer writer, boolean typed) {
            super(writer, typed);
        }
    }

    protected static class Layout extends org.apache.maven.utils.MavenProperties.Layout {
        public Layout() {}

        public Layout(List<String> commentLines, List<String> valueLines) {
            super(commentLines, valueLines);
        }
    }
}
