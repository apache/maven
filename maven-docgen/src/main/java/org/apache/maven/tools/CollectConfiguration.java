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
package org.apache.maven.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;

import org.apache.maven.api.annotations.Config;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.codehaus.plexus.util.io.CachingWriter;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.AST;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.ASTNode;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.Javadoc;
import org.jboss.forge.roaster.model.JavaDocCapable;
import org.jboss.forge.roaster.model.JavaDocTag;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.impl.JavaDocImpl;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaDocSource;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

public class CollectConfiguration {

    public static void main(String[] args) throws Exception {
        try {
            Path start = Paths.get(args.length > 0 ? args[0] : ".");
            Path output = Paths.get(args.length > 1 ? args[1] : "output");

            TreeMap<String, ConfigurationKey> discoveredKeys = new TreeMap<>();

            Files.walk(start)
                    .map(Path::toAbsolutePath)
                    .filter(p -> p.getFileName().toString().endsWith(".class"))
                    .filter(p -> p.toString().contains("/target/classes/"))
                    .forEach(p -> {
                        processClass(p, discoveredKeys);
                    });

            VelocityEngine velocityEngine = new VelocityEngine();
            Properties properties = new Properties();
            properties.setProperty("resource.loaders", "classpath");
            properties.setProperty("resource.loader.classpath.class", ClasspathResourceLoader.class.getName());
            velocityEngine.init(properties);

            VelocityContext context = new VelocityContext();
            context.put("keys", discoveredKeys.values());

            try (Writer fileWriter = new CachingWriter(output, StandardCharsets.UTF_8)) {
                velocityEngine.getTemplate("page.vm").merge(context, fileWriter);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    private static void processClass(Path path, Map<String, ConfigurationKey> discoveredKeys) {
        try {
            ClassReader classReader = new ClassReader(Files.newInputStream(path));
            classReader.accept(
                    new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public FieldVisitor visitField(
                                int fieldAccess,
                                String fieldName,
                                String fieldDescriptor,
                                String fieldSignature,
                                Object fieldValue) {
                            return new FieldVisitor(Opcodes.ASM9) {
                                @Override
                                public AnnotationVisitor visitAnnotation(
                                        String annotationDescriptor, boolean annotationVisible) {
                                    if (annotationDescriptor.equals("Lorg/apache/maven/api/annotations/Config;")) {
                                        return new AnnotationVisitor(Opcodes.ASM9) {
                                            Map<String, Object> values = new HashMap<>();

                                            @Override
                                            public void visit(String name, Object value) {
                                                values.put(name, value);
                                            }

                                            @Override
                                            public void visitEnum(String name, String descriptor, String value) {
                                                values.put(name, value);
                                            }

                                            @Override
                                            public void visitEnd() {
                                                JavaType<?> jtype = parse(Paths.get(path.toString()
                                                        .replace("/target/classes/", "/src/main/java/")
                                                        .replace(".class", ".java")));
                                                FieldSource<JavaClassSource> f =
                                                        ((JavaClassSource) jtype).getField(fieldName);

                                                String fqName = null;
                                                String desc = cloneJavadoc(f.getJavaDoc())
                                                        .removeAllTags()
                                                        .getFullText()
                                                        .replace("*", "\\*");
                                                String since = getSince(f);
                                                String source =
                                                        switch ((values.get("source") != null
                                                                        ? (String) values.get("source")
                                                                        : Config.Source.USER_PROPERTIES.toString())
                                                                .toLowerCase()) {
                                                            case "model" -> "Model properties";
                                                            case "user_properties" -> "User properties";
                                                            default -> throw new IllegalStateException();
                                                        };
                                                String type =
                                                        switch ((values.get("type") != null
                                                                ? (String) values.get("type")
                                                                : "java.lang.String")) {
                                                            case "java.lang.String" -> "String";
                                                            case "java.lang.Integer" -> "Integer";
                                                            default -> throw new IllegalStateException();
                                                        };
                                                discoveredKeys.put(
                                                        fieldValue.toString(),
                                                        new ConfigurationKey(
                                                                fieldValue.toString(),
                                                                values.get("defaultValue") != null
                                                                        ? values.get("defaultValue")
                                                                                .toString()
                                                                        : null,
                                                                fqName,
                                                                desc,
                                                                since,
                                                                source,
                                                                type));
                                            }
                                        };
                                    }
                                    return null;
                                }
                            };
                        }
                    },
                    0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static JavaDocSource<Object> cloneJavadoc(JavaDocSource<?> javaDoc) {
        Javadoc jd = (Javadoc) javaDoc.getInternal();
        return new JavaDocImpl(javaDoc.getOrigin(), (Javadoc)
                ASTNode.copySubtree(AST.newAST(jd.getAST().apiLevel()), jd));
    }

    private static String unquote(String s) {
        return (s.startsWith("\"") && s.endsWith("\"")) ? s.substring(1, s.length() - 1) : s;
    }

    private static JavaType<?> parse(Path path) {
        try {
            return Roaster.parse(path.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean toBoolean(String value) {
        return ("yes".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value));
    }

    /**
     * Would be record, but... Velocity have no idea what it is nor how to handle it.
     */
    public static class ConfigurationKey {
        private final String key;
        private final String defaultValue;
        private final String fqName;
        private final String description;
        private final String since;
        private final String configurationSource;
        private final String configurationType;

        @SuppressWarnings("checkstyle:parameternumber")
        public ConfigurationKey(
                String key,
                String defaultValue,
                String fqName,
                String description,
                String since,
                String configurationSource,
                String configurationType) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.fqName = fqName;
            this.description = description;
            this.since = since;
            this.configurationSource = configurationSource;
            this.configurationType = configurationType;
        }

        public String getKey() {
            return key;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public String getFqName() {
            return fqName;
        }

        public String getDescription() {
            return description;
        }

        public String getSince() {
            return since;
        }

        public String getConfigurationSource() {
            return configurationSource;
        }

        public String getConfigurationType() {
            return configurationType;
        }
    }

    private static String nvl(String string, String def) {
        return string == null ? def : string;
    }

    private static boolean hasConfigurationSource(JavaDocCapable<?> javaDocCapable) {
        return getTag(javaDocCapable, "@configurationSource") != null;
    }

    private static String getConfigurationType(JavaDocCapable<?> javaDocCapable) {
        String type = getTag(javaDocCapable, "@configurationType");
        if (type != null) {
            String linkPrefix = "{@link ";
            String linkSuffix = "}";
            if (type.startsWith(linkPrefix) && type.endsWith(linkSuffix)) {
                type = type.substring(linkPrefix.length(), type.length() - linkSuffix.length());
            }
            String javaLangPackage = "java.lang.";
            if (type.startsWith(javaLangPackage)) {
                type = type.substring(javaLangPackage.length());
            }
        }
        return nvl(type, "n/a");
    }

    private static String getConfigurationSource(JavaDocCapable<?> javaDocCapable) {
        String source = getTag(javaDocCapable, "@configurationSource");
        if ("{@link RepositorySystemSession#getConfigProperties()}".equals(source)) {
            return "Session Configuration";
        } else if ("{@link System#getProperty(String,String)}".equals(source)) {
            return "Java System Properties";
        } else if ("{@link org.apache.maven.api.model.Model#getProperties()}".equals(source)) {
            return "Model Properties";
        } else if ("{@link Session#getUserProperties()}".equals(source)) {
            return "Session Properties";
        } else {
            return source;
        }
    }

    private static String getSince(JavaDocCapable<?> javaDocCapable) {
        List<JavaDocTag> tags;
        if (javaDocCapable != null) {
            if (javaDocCapable instanceof FieldSource<?> fieldSource) {
                tags = fieldSource.getJavaDoc().getTags("@since");
                if (tags.isEmpty()) {
                    return getSince(fieldSource.getOrigin());
                } else {
                    return tags.get(0).getValue();
                }
            } else if (javaDocCapable instanceof JavaClassSource classSource) {
                tags = classSource.getJavaDoc().getTags("@since");
                if (!tags.isEmpty()) {
                    return tags.get(0).getValue();
                }
            }
        }
        return "";
    }

    private static String getTag(JavaDocCapable<?> javaDocCapable, String tagName) {
        List<JavaDocTag> tags;
        if (javaDocCapable != null) {
            if (javaDocCapable instanceof FieldSource<?> fieldSource) {
                tags = fieldSource.getJavaDoc().getTags(tagName);
                if (tags.isEmpty()) {
                    return getTag(fieldSource.getOrigin(), tagName);
                } else {
                    return tags.get(0).getValue();
                }
            }
        }
        return null;
    }

    private static final Pattern CONSTANT_PATTERN = Pattern.compile(".*static final.* ([A-Z_]+) = (.*);");

    private static final ToolProvider JAVAP = ToolProvider.findFirst("javap").orElseThrow();

    /**
     * Builds "constant table" for one single class.
     *
     * Limitations:
     * - works only for single class (no inherited constants)
     * - does not work for fields that are Enum.name()
     * - more to come
     */
    private static Map<String, String> extractConstants(Path file) {
        StringWriter out = new StringWriter();
        JAVAP.run(new PrintWriter(out), new PrintWriter(System.err), "-constants", file.toString());
        Map<String, String> result = new HashMap<>();
        out.getBuffer().toString().lines().forEach(l -> {
            Matcher matcher = CONSTANT_PATTERN.matcher(l);
            if (matcher.matches()) {
                result.put(matcher.group(1), matcher.group(2));
            }
        });
        return result;
    }
}
