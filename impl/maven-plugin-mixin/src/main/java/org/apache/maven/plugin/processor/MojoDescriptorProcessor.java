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
package org.apache.maven.plugin.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.maven.api.plugin.annotations.After;
import org.apache.maven.api.plugin.annotations.Execute;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Resolution;
import org.apache.maven.api.plugin.descriptor.AfterLink;
import org.apache.maven.api.plugin.descriptor.MojoDescriptor;
import org.apache.maven.api.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.io.PluginDescriptorStaxWriter;

/**
 * JSR 269 annotation processor that generates Maven plugin descriptors ({@code META-INF/maven/plugin.xml})
 * and DI factory classes at compile time from {@link Mojo @Mojo}-annotated classes.
 *
 * <p>Plugin coordinates are read from compiler {@code -A} options, typically provided by the
 * {@code maven-plugin-mixin}.
 *
 * @since 4.1.0
 */
@SupportedAnnotationTypes("org.apache.maven.api.plugin.annotations.Mojo")
@SupportedOptions({
    MojoDescriptorProcessor.OPT_GROUP_ID,
    MojoDescriptorProcessor.OPT_ARTIFACT_ID,
    MojoDescriptorProcessor.OPT_VERSION,
    MojoDescriptorProcessor.OPT_GOAL_PREFIX,
    MojoDescriptorProcessor.OPT_NAME
})
public class MojoDescriptorProcessor extends AbstractProcessor {

    static final String OPT_GROUP_ID = "maven.plugin.groupId";
    static final String OPT_ARTIFACT_ID = "maven.plugin.artifactId";
    static final String OPT_VERSION = "maven.plugin.version";
    static final String OPT_GOAL_PREFIX = "maven.plugin.goalPrefix";
    static final String OPT_NAME = "maven.plugin.name";

    private final Map<String, TypeElement> mojoElements = new LinkedHashMap<>();
    private final Set<String> generatedFactoryNames = new LinkedHashSet<>();
    private String[] resolvedCoordinates;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (!(element instanceof TypeElement typeElement)) {
                    continue;
                }
                String fqn = typeElement.getQualifiedName().toString();
                if (generatedFactoryNames.contains(fqn)) {
                    continue;
                }
                if (typeElement.getKind() == ElementKind.INTERFACE) {
                    error("@Mojo must not be placed on an interface", element);
                    continue;
                }
                if (typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
                    error("@Mojo must not be placed on an abstract class", element);
                    continue;
                }
                if (!mojoElements.containsKey(fqn)) {
                    mojoElements.put(fqn, typeElement);
                    String[] coords = resolveCoordinates();
                    if (coords != null) {
                        try {
                            generateFactory(typeElement, coords);
                        } catch (IOException e) {
                            error("Failed to generate factory for " + fqn, e);
                        }
                    }
                }
            }
        }
        if (roundEnv.processingOver() && !mojoElements.isEmpty()) {
            String[] coords = resolveCoordinates();
            if (coords != null) {
                try {
                    writePluginXml(coords);
                } catch (IOException e) {
                    error("Failed to generate plugin.xml", e);
                }
            }
        }
        return false;
    }

    String[] resolveCoordinates() {
        if (resolvedCoordinates != null) {
            return resolvedCoordinates;
        }
        String groupId = getOption(OPT_GROUP_ID);
        String artifactId = getOption(OPT_ARTIFACT_ID);
        String version = getOption(OPT_VERSION);
        String goalPrefix = getOption(OPT_GOAL_PREFIX);
        String name = getOption(OPT_NAME);
        if (isBlank(goalPrefix)) {
            goalPrefix = deriveGoalPrefix(artifactId);
        }
        if (isBlank(name)) {
            name = artifactId;
        }
        if (isBlank(groupId) || isBlank(artifactId) || isBlank(version)) {
            error("Could not determine plugin coordinates. "
                    + "Supply -Amaven.plugin.groupId, -Amaven.plugin.artifactId and "
                    + "-Amaven.plugin.version via the maven-plugin-mixin.");
            return null;
        }
        resolvedCoordinates = new String[] {groupId, artifactId, version, goalPrefix, name};
        return resolvedCoordinates;
    }

    private String getOption(String key) {
        return processingEnv.getOptions().get(key);
    }

    private void writePluginXml(String[] coords) throws IOException {
        List<MojoDescriptor> mojos = new ArrayList<>();
        for (TypeElement mojoType : mojoElements.values()) {
            MojoDescriptor mojo = buildMojoDescriptor(mojoType);
            if (mojo != null) {
                mojos.add(mojo);
            }
        }
        PluginDescriptor descriptor = PluginDescriptor.newBuilder(true)
                .name(coords[4])
                .groupId(coords[0])
                .artifactId(coords[1])
                .version(coords[2])
                .goalPrefix(coords[3])
                .mojos(mojos)
                .build();
        try (Writer w = processingEnv
                .getFiler()
                .createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/maven/plugin.xml")
                .openWriter()) {
            PluginDescriptorStaxWriter writer = new PluginDescriptorStaxWriter();
            writer.setNamespace("http://maven.apache.org/PLUGIN/2.0.0");
            writer.write(w, descriptor);
        } catch (XMLStreamException e) {
            throw new IOException("Failed to write plugin descriptor", e);
        }
        note("Generated META-INF/maven/plugin.xml with " + mojos.size() + " mojo(s)");
    }

    private MojoDescriptor buildMojoDescriptor(TypeElement mojoType) {
        Mojo mojoAnn = mojoType.getAnnotation(Mojo.class);
        if (mojoAnn == null || mojoAnn.name().isEmpty()) {
            if (mojoAnn != null) {
                error("@Mojo.name() must not be empty", mojoType);
            }
            return null;
        }
        JavadocInfo doc = parseJavadoc(processingEnv.getElementUtils().getDocComment(mojoType));
        MojoDescriptor.Builder b = MojoDescriptor.newBuilder(true)
                .goal(mojoAnn.name())
                .implementation(mojoType.getQualifiedName().toString())
                .projectRequired(mojoAnn.projectRequired())
                .onlineRequired(mojoAnn.onlineRequired())
                .aggregator(mojoAnn.aggregator());
        set(b::phase, mojoAnn.defaultPhase());
        set(b::configurator, mojoAnn.configurator());
        set(b::dependencyResolution, mojoAnn.dependencyResolutionPathScopes());
        if (mojoAnn.dependencyCollection()) {
            b.dependencyCollection("true");
        }
        set(b::description, doc.description);
        set(b::since, doc.since);
        set(b::deprecated, doc.deprecated);
        Execute exec = mojoType.getAnnotation(Execute.class);
        if (exec != null) {
            set(b::executePhase, exec.phase());
            set(b::executeGoal, exec.goal());
            set(b::executeLifecycle, exec.lifecycle());
        }
        b.parameters(collectParameters(mojoType));
        b.resolutions(collectResolutions(mojoType));
        b.afterLinks(collectAfterLinks(mojoType));
        return b.build();
    }

    private List<org.apache.maven.api.plugin.descriptor.Parameter> collectParameters(TypeElement mojoType) {
        List<org.apache.maven.api.plugin.descriptor.Parameter> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (TypeElement cur = mojoType; cur != null && !isObject(cur); cur = superType(cur)) {
            for (Element enclosed : cur.getEnclosedElements()) {
                if (enclosed.getKind() != ElementKind.FIELD && enclosed.getKind() != ElementKind.METHOD) {
                    continue;
                }
                org.apache.maven.api.plugin.annotations.Parameter ann =
                        enclosed.getAnnotation(org.apache.maven.api.plugin.annotations.Parameter.class);
                if (ann == null) {
                    continue;
                }
                String name = ann.name().isEmpty() ? enclosed.getSimpleName().toString() : ann.name();
                if (!seen.add(name)) {
                    continue;
                }
                String type = enclosed.getKind() == ElementKind.FIELD
                        ? enclosed.asType().toString()
                        : (((ExecutableElement) enclosed).getParameters().isEmpty()
                                ? ((ExecutableElement) enclosed).getReturnType().toString()
                                : ((ExecutableElement) enclosed)
                                        .getParameters()
                                        .get(0)
                                        .asType()
                                        .toString());
                String expression = ann.property();
                if (!expression.isEmpty() && !expression.startsWith("${")) {
                    expression = "${" + expression + "}";
                }
                JavadocInfo doc = parseJavadoc(processingEnv.getElementUtils().getDocComment(enclosed));
                org.apache.maven.api.plugin.descriptor.Parameter.Builder pb =
                        org.apache.maven.api.plugin.descriptor.Parameter.newBuilder(true)
                                .name(name)
                                .type(type)
                                .required(ann.required())
                                .editable(!ann.readonly());
                set(pb::alias, ann.alias());
                set(pb::expression, expression);
                set(pb::defaultValue, ann.defaultValue());
                set(pb::description, doc.description);
                set(pb::since, doc.since);
                set(pb::deprecated, doc.deprecated);
                result.add(pb.build());
            }
        }
        return result;
    }

    private List<org.apache.maven.api.plugin.descriptor.Resolution> collectResolutions(TypeElement mojoType) {
        List<org.apache.maven.api.plugin.descriptor.Resolution> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (TypeElement cur = mojoType; cur != null && !isObject(cur); cur = superType(cur)) {
            for (Element enclosed : cur.getEnclosedElements()) {
                if (enclosed.getKind() != ElementKind.FIELD) {
                    continue;
                }
                Resolution ann = enclosed.getAnnotation(Resolution.class);
                if (ann == null) {
                    continue;
                }
                String fieldName = enclosed.getSimpleName().toString();
                if (!seen.add(fieldName)) {
                    continue;
                }
                org.apache.maven.api.plugin.descriptor.Resolution.Builder rb =
                        org.apache.maven.api.plugin.descriptor.Resolution.newBuilder(true)
                                .field(fieldName);
                set(rb::pathScope, ann.pathScope());
                set(rb::requestType, ann.requestType());
                result.add(rb.build());
            }
        }
        return result;
    }

    private List<AfterLink> collectAfterLinks(TypeElement mojoType) {
        After[] afters = mojoType.getAnnotationsByType(After.class);
        if (afters.length == 0) {
            return List.of();
        }
        List<AfterLink> result = new ArrayList<>(afters.length);
        for (After a : afters) {
            AfterLink.Builder ab =
                    AfterLink.newBuilder(true).phase(a.phase()).type(a.type().name());
            set(ab::scope, a.scope());
            result.add(ab.build());
        }
        return result;
    }

    private void generateFactory(TypeElement mojoType, String[] coords) throws IOException {
        Mojo mojoAnn = mojoType.getAnnotation(Mojo.class);
        if (mojoAnn == null || mojoAnn.name().isEmpty()) {
            return;
        }
        String fqn = mojoType.getQualifiedName().toString();
        String pkg = fqn.contains(".") ? fqn.substring(0, fqn.lastIndexOf('.')) : "";
        String simpleName = mojoType.getSimpleName().toString();
        String factoryName = simpleName + "MojoFactory";
        String factoryFqn = pkg.isEmpty() ? factoryName : pkg + "." + factoryName;
        String roleHint = coords[0] + ":" + coords[1] + ":" + coords[2] + ":" + mojoAnn.name();

        List<ExecutableElement> ctors = mojoType.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                .map(ExecutableElement.class::cast)
                .filter(c -> !c.getModifiers().contains(Modifier.PRIVATE))
                .toList();

        try (PrintWriter pw = new PrintWriter(
                processingEnv.getFiler().createSourceFile(factoryFqn, mojoType).openWriter())) {
            if (!pkg.isEmpty()) {
                pw.println("package " + pkg + ";");
                pw.println();
            }
            pw.println("@org.apache.maven.api.di.Named(\"" + roleHint + "\")");
            pw.println("public class " + factoryName + " extends " + simpleName + " {");
            for (ExecutableElement ctor : ctors) {
                if (ctor.getParameters().isEmpty()) {
                    continue;
                }
                pw.print("    public " + factoryName + "(");
                StringBuilder args = new StringBuilder();
                for (int i = 0; i < ctor.getParameters().size(); i++) {
                    VariableElement p = ctor.getParameters().get(i);
                    if (i > 0) {
                        pw.print(", ");
                        args.append(", ");
                    }
                    pw.print(p.asType() + " " + p.getSimpleName());
                    args.append(p.getSimpleName());
                }
                List<? extends TypeMirror> thrown = ctor.getThrownTypes();
                if (!thrown.isEmpty()) {
                    pw.print(") throws ");
                    pw.print(thrown.stream()
                            .map(Object::toString)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse(""));
                    pw.println(" { super(" + args + "); }");
                } else {
                    pw.println(") { super(" + args + "); }");
                }
            }
            pw.println("}");
        }
        generatedFactoryNames.add(factoryFqn);
        note("Generated factory " + factoryFqn);
    }

    static JavadocInfo parseJavadoc(String raw) {
        if (raw == null || raw.isBlank()) {
            return new JavadocInfo(null, null, null);
        }
        String[] parts = raw.split(
                "(?m)^\\s*@(?=param|return|throws|exception|since|deprecated|see|author|version|serial|serialField|serialData)\\b");
        String body = parts[0].replaceAll("\\{@\\w+\\s+([^}]*)}", "$1")
                .replaceAll("<[^>]+>", "")
                .strip()
                .replaceAll("\\s+", " ");
        String since = null, deprecated = null;
        for (int i = 1; i < parts.length; i++) {
            String tag = parts[i].trim();
            if (tag.startsWith("since")) {
                since = tag.substring(5).trim().replaceAll("\\s+", " ");
            } else if (tag.startsWith("deprecated")) {
                deprecated = tag.substring(10).trim().replaceAll("\\s+", " ");
            }
        }
        return new JavadocInfo(body.isEmpty() ? null : body, since, deprecated);
    }

    private TypeElement superType(TypeElement te) {
        TypeMirror sup = te.getSuperclass();
        return sup.getKind() != TypeKind.NONE
                        && sup instanceof DeclaredType dt
                        && dt.asElement() instanceof TypeElement st
                        && !isObject(st)
                ? st
                : null;
    }

    private static boolean isObject(TypeElement te) {
        return "java.lang.Object".equals(te.getQualifiedName().toString());
    }

    static String deriveGoalPrefix(String artifactId) {
        if (artifactId == null || artifactId.isEmpty()) {
            return "";
        }
        String s = artifactId;
        if (s.startsWith("maven-")) {
            s = s.substring(6);
        }
        if (s.endsWith("-maven-plugin")) {
            s = s.substring(0, s.length() - 13);
        } else if (s.endsWith("-plugin")) {
            s = s.substring(0, s.length() - 7);
        }
        return s;
    }

    private static void set(Consumer<String> setter, String value) {
        if (value != null && !value.isEmpty()) {
            setter.accept(value);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }

    private void note(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }

    private void error(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
    }

    private void error(String msg, Element element) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, element);
    }

    private void error(String msg, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        error(msg + "\n" + e.getClass().getName() + ": " + e.getMessage() + "\n" + sw);
    }

    record JavadocInfo(String description, String since, String deprecated) {}
}
