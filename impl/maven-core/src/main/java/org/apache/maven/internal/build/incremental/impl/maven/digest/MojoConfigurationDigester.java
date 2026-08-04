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
package org.apache.maven.internal.build.incremental.impl.maven.digest;

import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.MojoExecutionScoped;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.plugin.descriptor.MojoDescriptor;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.internal.xml.XmlNodeWriter;
import org.apache.maven.plugin.PluginParameterExpressionEvaluatorV4;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;

/**
 * Produces a deterministic digest of a mojo execution's configuration for incremental build
 * change detection. The digest is stored in the
 * {@link org.apache.maven.api.build.incremental.spi.IncrementalContextEnvironment#getParameters() build
 * context parameters}; if any value changes between builds, the build context escalates to a
 * full rebuild.
 *
 * <p>The digester captures two categories of non-file state:</p>
 * <ol>
 *   <li><strong>Plugin classpath</strong> ({@code mojo.classpath}) — a SHA-1 hash of every
 *       plugin dependency JAR's contents (delegated to {@link ClasspathDigester}). This
 *       detects plugin upgrades or SNAPSHOT rebuilds.</li>
 *   <li><strong>Mojo parameters</strong> ({@code mojo.parameter.<name>}) — each
 *       {@code @Parameter}-annotated field of the mojo class is reflected, its configured
 *       expression is evaluated, and the resolved value is digested through
 *       {@link Digesters#digest(java.lang.reflect.Field, Object)}. This detects changes
 *       to compiler flags, output directories, filter tokens, and any other configuration.</li>
 * </ol>
 *
 * <p>Parameters whose types are not supported by {@link Digesters} are reported as errors
 * rather than silently ignored, ensuring that the build context does not miss a configuration
 * change.</p>
 *
 * @since 4.1.0
 * @see ClasspathDigester
 * @see Digesters
 * @see org.apache.maven.api.build.incremental.spi.IncrementalContextEnvironment#getParameters()
 */
@Named
@MojoExecutionScoped
public class MojoConfigurationDigester {

    private final ClasspathDigester classpathDigester;
    private final Session session;
    private final Project project;
    private final MojoExecution execution;

    @Inject
    public MojoConfigurationDigester(Session session, Project project, MojoExecution execution) {
        this.session = session;
        this.project = project;
        this.execution = execution;
        this.classpathDigester = new ClasspathDigester(session);
    }

    /**
     * Computes the configuration digest for the current mojo execution.
     *
     * <p>The returned map contains:</p>
     * <ul>
     *   <li>{@code mojo.classpath} — the combined SHA-1 of all plugin dependency JARs</li>
     *   <li>{@code mojo.parameter.<fieldName>} — a digest for each resolved mojo parameter</li>
     * </ul>
     *
     * @return an ordered map of digest keys to their serializable digest values
     * @throws IOException if an error occurs while reading plugin JARs or evaluating parameters
     */
    public Map<String, Serializable> digest() throws IOException {
        Map<String, Serializable> result = new LinkedHashMap<>();

        MojoDescriptor mojoDescriptor = execution.getDescriptor();
        try {
            List<Artifact> classpath = new ArrayList<>(execution.getPlugin().getDependencies());
            result.put("mojo.classpath", classpathDigester.digest(classpath));
        } catch (NullPointerException e) {
            // Plugin dependency node may not be resolved yet (e.g. during early lifecycle phases).
            // Fall back to using the plugin artifact's GAV as a less precise digest.
            Artifact pluginArtifact = execution.getPlugin().getArtifact();
            if (pluginArtifact != null) {
                result.put("mojo.classpath", pluginArtifact.key().toString());
            }
        }

        XmlNode node = execution.getConfiguration().orElse(null);
        if (node != null) {
            List<String> errors = new ArrayList<>();
            ExpressionEvaluator evaluator = new PluginParameterExpressionEvaluatorV4(session, project);
            Class<?> mojoClass;
            try {
                mojoClass = execution.getPlugin().getClassLoader().loadClass(mojoDescriptor.getImplementation());
            } catch (ClassNotFoundException e) {
                errors.add("mojo class not found " + mojoDescriptor.getImplementation());
                mojoClass = null;
            }
            if (mojoClass != null) {
                for (XmlNode child : node.getChildren()) {
                    String name = fromXML(child.getName());
                    try {
                        Field field = getField(mojoClass, name);
                        if (field != null) {
                            String expression = child.getValue();
                            if (expression == null) {
                                expression = getChildrenXml(child);
                            }
                            if (expression == null) {
                                expression = child.getAttribute("default-value");
                            }
                            if (expression != null) {
                                Object value = evaluator.evaluate(expression);
                                if (value != null) {
                                    Serializable digest = Digesters.digest(field, value);
                                    if (digest != null) {
                                        result.put("mojo.parameter." + name, digest);
                                    }
                                }
                            }
                        }
                    } catch (Digesters.UnsupportedParameterTypeException e) {
                        errors.add("parameter " + name + " has unsupported type " + e.type.getName());
                    } catch (ExpressionEvaluationException | XMLStreamException e) {
                        errors.add("parameter " + name + " " + e.getMessage());
                    }
                }
            }
            if (!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append(project.toString());
                sb.append(" could not digest configuration of ").append(execution);
                for (String error : errors) {
                    sb.append("\n   ").append(error);
                }
                throw new IllegalArgumentException(sb.toString());
            }
        }
        return result;
    }

    private String getChildrenXml(XmlNode node) throws XMLStreamException {
        List<XmlNode> children = node.getChildren();
        if (children.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (XmlNode child : children) {
            append(sb, child);
        }
        return sb.toString();
    }

    private void append(StringBuilder sb, XmlNode node) throws XMLStreamException {
        StringWriter sw = new StringWriter();
        XmlNodeWriter.write(sw, node);
        sb.append(sw.toString());
    }

    /**
     * Cache of reflected fields by class. Same mojo class is reflected for every execution
     * in a reactor build — caching avoids repeated class hierarchy walks.
     */
    private static final java.util.concurrent.ConcurrentHashMap<Class<?>, Map<String, Field>> FIELD_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    private Field getField(Class<?> clazz, String name) {
        Map<String, Field> fields = FIELD_CACHE.computeIfAbsent(clazz, MojoConfigurationDigester::buildFieldMap);
        return fields.get(name);
    }

    private static Map<String, Field> buildFieldMap(Class<?> clazz) {
        Map<String, Field> map = new java.util.HashMap<>();
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                map.putIfAbsent(field.getName(), field);
            }
        }
        return map;
    }

    // first-name --> firstName
    protected String fromXML(final String elementName) {
        boolean firstToken = true;
        boolean firstLetter = true;
        int rindex = 0;
        int windex = 0;
        int[] codepoints = elementName.codePoints().toArray();
        while (rindex < codepoints.length) {
            int cp = codepoints[rindex++];
            if (cp == '-') {
                firstToken = false;
                firstLetter = true;
            } else {
                if (firstLetter) {
                    cp = firstToken ? Character.toLowerCase(cp) : Character.toTitleCase(cp);
                    firstLetter = false;
                }
                codepoints[windex++] = cp;
            }
        }
        return new String(codepoints, 0, windex);
    }
}
