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

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.build.incremental.Incremental;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.MojoExecutionScoped;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.plugin.descriptor.MojoDescriptor;
import org.apache.maven.api.xml.XmlNode;
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
 *   <li><strong>Mojo configuration</strong> ({@code mojo.configuration}) — a SHA-1 hash of
 *       the effective configuration XML tree with all expressions resolved. This detects
 *       changes to compiler flags, output directories, filter tokens, and any other
 *       configuration — including {@code -D} property overrides.</li>
 * </ol>
 *
 * <p>The configuration is digested directly from the XML tree available via
 * {@link MojoExecution#getConfiguration()}, rather than via type-specific value digesting.
 * Each element's name, attributes, and evaluated value are fed into the hash.
 * Expressions (e.g., {@code ${project.build.directory}}) are resolved through
 * the standard {@link ExpressionEvaluator} so that property changes between builds are
 * detected.</p>
 *
 * <p>Parameters annotated with {@link Incremental @Incremental(consider = false)} are
 * excluded from the digest — changes to those parameters (e.g., logging verbosity,
 * thread count) will not trigger a full rebuild.</p>
 *
 * @since 4.1.0
 * @see ClasspathDigester
 * @see Incremental
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
     *   <li>{@code mojo.configuration} — a SHA-1 of the evaluated configuration XML tree</li>
     * </ul>
     *
     * @return an ordered map of digest keys to their serializable digest values
     * @throws IOException if an error occurs while reading plugin JARs
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
            // Load field annotations for @Incremental(consider=false) checks.
            // The mojo class is already loaded for execution — we only read annotations,
            // not field values.
            Map<String, Field> fields = loadFieldMap(mojoDescriptor);
            ExpressionEvaluator evaluator = new PluginParameterExpressionEvaluatorV4(session, project);
            MessageDigest md = SHA1Digester.newInstance();
            for (XmlNode child : node.getChildren()) {
                String fieldName = fromXML(child.getName());
                if (shouldSkip(fields, fieldName)) {
                    continue;
                }
                digestXmlNode(md, child, evaluator);
            }
            result.put("mojo.configuration", new BytesHash(md.digest()));
        }

        return result;
    }

    /**
     * Checks whether a parameter is annotated with {@code @Incremental(consider = false)}.
     */
    private boolean shouldSkip(Map<String, Field> fields, String fieldName) {
        Field field = fields.get(fieldName);
        if (field != null) {
            Incremental incremental = field.getAnnotation(Incremental.class);
            if (incremental != null && !incremental.consider()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Recursively hashes an XML node: element name, attributes, evaluated value, and children.
     */
    private void digestXmlNode(MessageDigest md, XmlNode node, ExpressionEvaluator evaluator) {
        // Element name
        md.update(node.getName().getBytes(StandardCharsets.UTF_8));

        // Attributes (skip "default-value" — it's configurator metadata, not user config;
        // the effective value appears as the element text after merging)
        for (Map.Entry<String, String> attr : node.getAttributes().entrySet()) {
            String key = attr.getKey();
            if ("default-value".equals(key)) {
                continue;
            }
            md.update(key.getBytes(StandardCharsets.UTF_8));
            md.update(evaluateToString(evaluator, attr.getValue()).getBytes(StandardCharsets.UTF_8));
        }

        // Element value — evaluate expressions so -D property changes are detected
        String value = node.getValue();
        if (value != null && !value.isEmpty()) {
            md.update(evaluateToString(evaluator, value).getBytes(StandardCharsets.UTF_8));
        }

        // Recurse into children
        for (XmlNode child : node.getChildren()) {
            digestXmlNode(md, child, evaluator);
        }
    }

    /**
     * Evaluates an expression and returns its string representation.
     * If the expression cannot be evaluated, returns the raw expression
     * so that the hash is still deterministic.
     */
    private String evaluateToString(ExpressionEvaluator evaluator, String expression) {
        if (expression == null || expression.isEmpty()) {
            return "";
        }
        try {
            Object resolved = evaluator.evaluate(expression);
            if (resolved == null) {
                return expression;
            }
            return resolved.toString();
        } catch (ExpressionEvaluationException e) {
            // Can't resolve — use the raw expression for deterministic hashing
            return expression;
        }
    }

    /**
     * Loads the mojo class and builds a field map for {@code @Incremental} annotation checking.
     * The mojo class is already loaded by the plugin classloader for execution — this only
     * reads field annotations, not field values.
     *
     * <p>Field maps are cached per class to avoid repeated class hierarchy walks across
     * reactor executions of the same mojo.</p>
     */
    private Map<String, Field> loadFieldMap(MojoDescriptor mojoDescriptor) {
        try {
            Class<?> mojoClass = execution.getPlugin().getClassLoader().loadClass(mojoDescriptor.getImplementation());
            return FIELD_CACHE.computeIfAbsent(mojoClass, MojoConfigurationDigester::buildFieldMap);
        } catch (ClassNotFoundException e) {
            // Can't load mojo class — skip @Incremental checks, digest everything
            return Map.of();
        }
    }

    /**
     * Cache of reflected fields by class. Same mojo class is reflected for every execution
     * in a reactor build — caching avoids repeated class hierarchy walks.
     */
    private static final ConcurrentHashMap<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    private static Map<String, Field> buildFieldMap(Class<?> clazz) {
        Map<String, Field> map = new HashMap<>();
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
