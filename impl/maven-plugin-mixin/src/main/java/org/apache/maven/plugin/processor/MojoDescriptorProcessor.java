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
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

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

import org.apache.maven.api.plugin.annotations.After;
import org.apache.maven.api.plugin.annotations.Execute;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Resolution;
import org.apache.maven.api.plugin.descriptor.AfterLink;
import org.apache.maven.api.plugin.descriptor.MojoDescriptor;
import org.apache.maven.api.plugin.descriptor.PluginDescriptor;

/**
 * JSR 269 annotation processor that generates Maven plugin descriptors ({@code META-INF/maven/plugin.xml})
 * and DI factory classes at compile time from {@link Mojo @Mojo}-annotated classes.
 *
 * <p>The processor reads plugin coordinates from compiler {@code -A} options, typically
 * provided by the {@code maven-plugin-mixin}:
 * <ul>
 *   <li>{@code maven.plugin.groupId} — plugin group ID (required)</li>
 *   <li>{@code maven.plugin.artifactId} — plugin artifact ID (required)</li>
 *   <li>{@code maven.plugin.version} — plugin version (required)</li>
 *   <li>{@code maven.plugin.goalPrefix} — goal prefix (optional, derived from artifactId)</li>
 * </ul>
 *
 * @since 4.1.0
 */
@SupportedAnnotationTypes("org.apache.maven.api.plugin.annotations.Mojo")
@SupportedOptions({
    MojoDescriptorProcessor.OPT_GROUP_ID,
    MojoDescriptorProcessor.OPT_ARTIFACT_ID,
    MojoDescriptorProcessor.OPT_VERSION,
    MojoDescriptorProcessor.OPT_GOAL_PREFIX
})
public class MojoDescriptorProcessor extends AbstractProcessor {

    static final String OPT_GROUP_ID = "maven.plugin.groupId";
    static final String OPT_ARTIFACT_ID = "maven.plugin.artifactId";
    static final String OPT_VERSION = "maven.plugin.version";
    static final String OPT_GOAL_PREFIX = "maven.plugin.goalPrefix";

    private static final String PLUGIN_NS = "http://maven.apache.org/PLUGIN/2.0.0";

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
                    processingEnv
                            .getMessager()
                            .printMessage(Diagnostic.Kind.ERROR, "@Mojo must not be placed on an interface", element);
                    continue;
                }
                if (typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
                    processingEnv
                            .getMessager()
                            .printMessage(
                                    Diagnostic.Kind.ERROR, "@Mojo must not be placed on an abstract class", element);
                    continue;
                }
                if (!mojoElements.containsKey(fqn)) {
                    mojoElements.put(fqn, typeElement);
                    String[] coords = resolveCoordinates();
                    if (coords != null) {
                        try {
                            generateFactory(typeElement, coords);
                        } catch (IOException e) {
                            logError("Failed to generate factory for " + fqn, e);
                        }
                    }
                }
            }
        }

        if (roundEnv.processingOver()) {
            if (mojoElements.isEmpty()) {
                return false;
            }
            String[] coords = resolveCoordinates();
            if (coords == null) {
                return false;
            }
            try {
                writePluginXml(coords);
            } catch (Exception e) {
                logError("Failed to generate plugin.xml", e);
            }
        }

        return false;
    }

    // -------------------------------------------------------------------------
    // Coordinate resolution
    // -------------------------------------------------------------------------

    String[] resolveCoordinates() {
        if (resolvedCoordinates != null) {
            return resolvedCoordinates;
        }

        String groupId = getOption(OPT_GROUP_ID);
        String artifactId = getOption(OPT_ARTIFACT_ID);
        String version = getOption(OPT_VERSION);
        String goalPrefix = getOption(OPT_GOAL_PREFIX);
        if (goalPrefix == null || goalPrefix.isEmpty()) {
            goalPrefix = deriveGoalPrefix(artifactId);
        }

        if (groupId == null
                || groupId.isEmpty()
                || artifactId == null
                || artifactId.isEmpty()
                || version == null
                || version.isEmpty()) {
            processingEnv
                    .getMessager()
                    .printMessage(
                            Diagnostic.Kind.ERROR,
                            "maven-plugin-mixin: could not determine plugin coordinates. "
                                    + "Supply -Amaven.plugin.groupId, -Amaven.plugin.artifactId and "
                                    + "-Amaven.plugin.version via the maven-plugin-mixin.");
            return null;
        }

        resolvedCoordinates = new String[] {groupId, artifactId, version, goalPrefix};
        return resolvedCoordinates;
    }

    private String getOption(String key) {
        return processingEnv.getOptions().get(key);
    }

    // -------------------------------------------------------------------------
    // plugin.xml generation
    // -------------------------------------------------------------------------

    private void writePluginXml(String[] coords) throws IOException, XMLStreamException {
        String groupId = coords[0];
        String artifactId = coords[1];
        String version = coords[2];
        String goalPrefix = coords[3];

        List<MojoDescriptor> mojos = new ArrayList<>();
        for (TypeElement mojoType : mojoElements.values()) {
            MojoDescriptor mojo = buildMojoDescriptor(mojoType);
            if (mojo != null) {
                mojos.add(mojo);
            }
        }

        PluginDescriptor descriptor = PluginDescriptor.newBuilder(true)
                .name(artifactId)
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .goalPrefix(goalPrefix)
                .mojos(mojos)
                .build();

        FileObject out =
                processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/maven/plugin.xml");
        try (Writer w = out.openWriter()) {
            writeDescriptorXml(w, descriptor);
        }
        note("Generated META-INF/maven/plugin.xml with " + mojos.size() + " mojo(s)");
    }

    private MojoDescriptor buildMojoDescriptor(TypeElement mojoType) {
        Mojo mojoAnn = mojoType.getAnnotation(Mojo.class);
        if (mojoAnn == null) {
            return null;
        }

        String goal = mojoAnn.name();
        if (goal.isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@Mojo.name() must not be empty", mojoType);
            return null;
        }

        String fqn = mojoType.getQualifiedName().toString();
        JavadocInfo mojoDoc = parseJavadoc(processingEnv.getElementUtils().getDocComment(mojoType));

        MojoDescriptor.Builder builder = MojoDescriptor.newBuilder(true)
                .goal(goal)
                .implementation(fqn)
                .projectRequired(mojoAnn.projectRequired())
                .onlineRequired(mojoAnn.onlineRequired())
                .aggregator(mojoAnn.aggregator());

        if (!mojoAnn.defaultPhase().isEmpty()) {
            builder.phase(mojoAnn.defaultPhase());
        }
        if (!mojoAnn.configurator().isEmpty()) {
            builder.configurator(mojoAnn.configurator());
        }
        if (!mojoAnn.dependencyResolutionPathScopes().isEmpty()) {
            builder.dependencyResolution(mojoAnn.dependencyResolutionPathScopes());
        }
        if (mojoAnn.dependencyCollection()) {
            builder.dependencyCollection("true");
        }
        if (mojoDoc.description != null) {
            builder.description(mojoDoc.description);
        }
        if (mojoDoc.since != null) {
            builder.since(mojoDoc.since);
        }
        if (mojoDoc.deprecated != null) {
            builder.deprecated(mojoDoc.deprecated);
        }

        // @Execute
        Execute executeAnn = mojoType.getAnnotation(Execute.class);
        if (executeAnn != null) {
            if (!executeAnn.phase().isEmpty()) {
                builder.executePhase(executeAnn.phase());
            }
            if (!executeAnn.goal().isEmpty()) {
                builder.executeGoal(executeAnn.goal());
            }
            if (!executeAnn.lifecycle().isEmpty()) {
                builder.executeLifecycle(executeAnn.lifecycle());
            }
        }

        builder.parameters(collectParameters(mojoType));
        builder.resolutions(collectResolutions(mojoType));
        builder.afterLinks(collectAfterLinks(mojoType));

        return builder.build();
    }

    // -------------------------------------------------------------------------
    // Parameter collection
    // -------------------------------------------------------------------------

    private List<org.apache.maven.api.plugin.descriptor.Parameter> collectParameters(TypeElement mojoType) {
        List<org.apache.maven.api.plugin.descriptor.Parameter> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        TypeElement current = mojoType;
        while (current != null && !isObject(current)) {
            for (Element enclosed : current.getEnclosedElements()) {
                if (enclosed.getKind() != ElementKind.FIELD && enclosed.getKind() != ElementKind.METHOD) {
                    continue;
                }
                org.apache.maven.api.plugin.annotations.Parameter paramAnn =
                        enclosed.getAnnotation(org.apache.maven.api.plugin.annotations.Parameter.class);
                if (paramAnn == null) {
                    continue;
                }
                String paramName =
                        paramAnn.name().isEmpty() ? enclosed.getSimpleName().toString() : paramAnn.name();
                if (seen.contains(paramName)) {
                    continue;
                }
                seen.add(paramName);

                String type;
                if (enclosed.getKind() == ElementKind.FIELD) {
                    type = ((VariableElement) enclosed).asType().toString();
                } else {
                    ExecutableElement method = (ExecutableElement) enclosed;
                    type = !method.getParameters().isEmpty()
                            ? method.getParameters().get(0).asType().toString()
                            : method.getReturnType().toString();
                }

                JavadocInfo doc = parseJavadoc(processingEnv.getElementUtils().getDocComment(enclosed));

                String expression = paramAnn.property();
                if (!expression.isEmpty() && !expression.startsWith("${")) {
                    expression = "${" + expression + "}";
                }

                org.apache.maven.api.plugin.descriptor.Parameter.Builder pb =
                        org.apache.maven.api.plugin.descriptor.Parameter.newBuilder(true)
                                .name(paramName)
                                .type(type)
                                .required(paramAnn.required())
                                .editable(!paramAnn.readonly());
                if (!paramAnn.alias().isEmpty()) {
                    pb.alias(paramAnn.alias());
                }
                if (!expression.isEmpty()) {
                    pb.expression(expression);
                }
                if (!paramAnn.defaultValue().isEmpty()) {
                    pb.defaultValue(paramAnn.defaultValue());
                }
                if (doc.description != null) {
                    pb.description(doc.description);
                }
                if (doc.since != null) {
                    pb.since(doc.since);
                }
                if (doc.deprecated != null) {
                    pb.deprecated(doc.deprecated);
                }
                result.add(pb.build());
            }
            current = superTypeElement(current);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Resolution collection
    // -------------------------------------------------------------------------

    private List<org.apache.maven.api.plugin.descriptor.Resolution> collectResolutions(TypeElement mojoType) {
        List<org.apache.maven.api.plugin.descriptor.Resolution> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        TypeElement current = mojoType;
        while (current != null && !isObject(current)) {
            for (Element enclosed : current.getEnclosedElements()) {
                if (enclosed.getKind() != ElementKind.FIELD) {
                    continue;
                }
                Resolution resAnn = enclosed.getAnnotation(Resolution.class);
                if (resAnn == null) {
                    continue;
                }
                String fieldName = enclosed.getSimpleName().toString();
                if (seen.contains(fieldName)) {
                    continue;
                }
                seen.add(fieldName);

                org.apache.maven.api.plugin.descriptor.Resolution.Builder rb =
                        org.apache.maven.api.plugin.descriptor.Resolution.newBuilder(true)
                                .field(fieldName);
                if (!resAnn.pathScope().isEmpty()) {
                    rb.pathScope(resAnn.pathScope());
                }
                if (!resAnn.requestType().isEmpty()) {
                    rb.requestType(resAnn.requestType());
                }
                result.add(rb.build());
            }
            current = superTypeElement(current);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // @After collection
    // -------------------------------------------------------------------------

    private List<AfterLink> collectAfterLinks(TypeElement mojoType) {
        After[] afters = mojoType.getAnnotationsByType(After.class);
        if (afters.length == 0) {
            return List.of();
        }
        List<AfterLink> result = new ArrayList<>();
        for (After after : afters) {
            AfterLink.Builder ab = AfterLink.newBuilder(true)
                    .phase(after.phase())
                    .type(after.type().name());
            if (!after.scope().isEmpty()) {
                ab.scope(after.scope());
            }
            result.add(ab.build());
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Factory class generation
    // -------------------------------------------------------------------------

    private void generateFactory(TypeElement mojoType, String[] coords) throws IOException {
        Mojo mojoAnn = mojoType.getAnnotation(Mojo.class);
        if (mojoAnn == null || mojoAnn.name().isEmpty()) {
            return;
        }

        String fqn = mojoType.getQualifiedName().toString();
        String pkg = fqn.contains(".") ? fqn.substring(0, fqn.lastIndexOf('.')) : "";
        String simpleName = mojoType.getSimpleName().toString();
        String factorySimpleName = simpleName + "$MojoFactory";
        String factoryFqn = pkg.isEmpty() ? factorySimpleName : pkg + "." + factorySimpleName;
        String roleHint = coords[0] + ":" + coords[1] + ":" + coords[2] + ":" + mojoAnn.name();

        List<ExecutableElement> ctors = new ArrayList<>();
        for (Element enc : mojoType.getEnclosedElements()) {
            if (enc.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement ctor = (ExecutableElement) enc;
                if (!ctor.getModifiers().contains(Modifier.PRIVATE)) {
                    ctors.add(ctor);
                }
            }
        }

        JavaFileObject src = processingEnv.getFiler().createSourceFile(factoryFqn, mojoType);
        try (PrintWriter pw = new PrintWriter(src.openWriter())) {
            writeLicenseHeader(pw);
            if (!pkg.isEmpty()) {
                pw.println("package " + pkg + ";");
                pw.println();
            }
            pw.println("@org.apache.maven.api.di.Named(\"" + roleHint + "\")");
            pw.println("public class " + factorySimpleName + " extends " + simpleName + " {");
            pw.println();

            if (!ctors.isEmpty()
                    && !(ctors.size() == 1 && ctors.get(0).getParameters().isEmpty())) {
                for (ExecutableElement ctor : ctors) {
                    if (ctor.getParameters().isEmpty()) {
                        continue;
                    }
                    pw.print("    public " + factorySimpleName + "(");
                    List<? extends VariableElement> params = ctor.getParameters();
                    StringBuilder args = new StringBuilder();
                    for (int i = 0; i < params.size(); i++) {
                        VariableElement param = params.get(i);
                        if (i > 0) {
                            pw.print(", ");
                            args.append(", ");
                        }
                        pw.print(param.asType() + " " + param.getSimpleName());
                        args.append(param.getSimpleName());
                    }
                    pw.println(") {");
                    pw.println("        super(" + args + ");");
                    pw.println("    }");
                    pw.println();
                }
            }

            pw.println("}");
        }

        generatedFactoryNames.add(factoryFqn);
        note("Generated factory " + factoryFqn + " for goal '" + mojoAnn.name() + "'");
    }

    // -------------------------------------------------------------------------
    // XML writing (using JDK XMLStreamWriter to avoid woodstox dependency)
    // -------------------------------------------------------------------------

    private static void writeDescriptorXml(Writer w, PluginDescriptor descriptor)
            throws IOException, XMLStreamException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter xml = factory.createXMLStreamWriter(w);
        try {
            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeCharacters("\n");
            xml.writeStartElement("plugin");
            xml.writeDefaultNamespace(PLUGIN_NS);
            xml.writeCharacters("\n");

            writeElement(xml, "name", descriptor.getName(), 2);
            writeElement(xml, "groupId", descriptor.getGroupId(), 2);
            writeElement(xml, "artifactId", descriptor.getArtifactId(), 2);
            writeElement(xml, "version", descriptor.getVersion(), 2);
            writeElement(xml, "goalPrefix", descriptor.getGoalPrefix(), 2);

            if (descriptor.getMojos() != null && !descriptor.getMojos().isEmpty()) {
                xml.writeCharacters("  ");
                xml.writeStartElement("mojos");
                xml.writeCharacters("\n");
                for (MojoDescriptor mojo : descriptor.getMojos()) {
                    writeMojoXml(xml, mojo);
                }
                xml.writeCharacters("  ");
                xml.writeEndElement();
                xml.writeCharacters("\n");
            }

            xml.writeEndElement();
            xml.writeCharacters("\n");
            xml.writeEndDocument();
        } finally {
            xml.close();
        }
    }

    private static void writeMojoXml(XMLStreamWriter xml, MojoDescriptor mojo) throws XMLStreamException {
        xml.writeCharacters("    ");
        xml.writeStartElement("mojo");
        xml.writeCharacters("\n");

        writeElement(xml, "goal", mojo.getGoal(), 6);
        writeOptional(xml, "phase", mojo.getPhase(), 6);
        writeElement(xml, "implementation", mojo.getImplementation(), 6);
        writeElement(xml, "language", mojo.getLanguage(), 6);
        writeOptional(xml, "description", mojo.getDescription(), 6);
        writeOptional(xml, "since", mojo.getSince(), 6);
        writeOptional(xml, "deprecated", mojo.getDeprecated(), 6);
        writeElement(xml, "projectRequired", String.valueOf(mojo.isProjectRequired()), 6);
        writeElement(xml, "onlineRequired", String.valueOf(mojo.isOnlineRequired()), 6);
        writeElement(xml, "aggregator", String.valueOf(mojo.isAggregator()), 6);
        writeOptional(xml, "configurator", mojo.getConfigurator(), 6);
        writeOptional(xml, "executePhase", mojo.getExecutePhase(), 6);
        writeOptional(xml, "executeGoal", mojo.getExecuteGoal(), 6);
        writeOptional(xml, "executeLifecycle", mojo.getExecuteLifecycle(), 6);
        writeOptional(xml, "dependencyResolution", mojo.getDependencyResolution(), 6);
        writeOptional(xml, "dependencyCollection", mojo.getDependencyCollection(), 6);

        if (mojo.getParameters() != null && !mojo.getParameters().isEmpty()) {
            xml.writeCharacters("      ");
            xml.writeStartElement("parameters");
            xml.writeCharacters("\n");
            for (org.apache.maven.api.plugin.descriptor.Parameter p : mojo.getParameters()) {
                xml.writeCharacters("        ");
                xml.writeStartElement("parameter");
                xml.writeCharacters("\n");
                writeElement(xml, "name", p.getName(), 10);
                writeOptional(xml, "alias", p.getAlias(), 10);
                writeElement(xml, "type", p.getType(), 10);
                writeElement(xml, "required", String.valueOf(p.isRequired()), 10);
                writeElement(xml, "editable", String.valueOf(p.isEditable()), 10);
                writeOptional(xml, "description", p.getDescription(), 10);
                writeOptional(xml, "since", p.getSince(), 10);
                writeOptional(xml, "deprecated", p.getDeprecated(), 10);
                writeOptional(xml, "expression", p.getExpression(), 10);
                writeOptional(xml, "defaultValue", p.getDefaultValue(), 10);
                xml.writeCharacters("        ");
                xml.writeEndElement();
                xml.writeCharacters("\n");
            }
            xml.writeCharacters("      ");
            xml.writeEndElement();
            xml.writeCharacters("\n");
        }

        if (mojo.getResolutions() != null && !mojo.getResolutions().isEmpty()) {
            xml.writeCharacters("      ");
            xml.writeStartElement("resolutions");
            xml.writeCharacters("\n");
            for (org.apache.maven.api.plugin.descriptor.Resolution r : mojo.getResolutions()) {
                xml.writeCharacters("        ");
                xml.writeStartElement("resolution");
                xml.writeCharacters("\n");
                writeElement(xml, "field", r.getField(), 10);
                writeOptional(xml, "pathScope", r.getPathScope(), 10);
                writeOptional(xml, "requestType", r.getRequestType(), 10);
                xml.writeCharacters("        ");
                xml.writeEndElement();
                xml.writeCharacters("\n");
            }
            xml.writeCharacters("      ");
            xml.writeEndElement();
            xml.writeCharacters("\n");
        }

        if (mojo.getAfterLinks() != null && !mojo.getAfterLinks().isEmpty()) {
            xml.writeCharacters("      ");
            xml.writeStartElement("afterLinks");
            xml.writeCharacters("\n");
            for (AfterLink a : mojo.getAfterLinks()) {
                xml.writeCharacters("        ");
                xml.writeStartElement("afterLink");
                xml.writeCharacters("\n");
                writeOptional(xml, "phase", a.getPhase(), 10);
                writeElement(xml, "type", a.getType(), 10);
                writeOptional(xml, "scope", a.getScope(), 10);
                xml.writeCharacters("        ");
                xml.writeEndElement();
                xml.writeCharacters("\n");
            }
            xml.writeCharacters("      ");
            xml.writeEndElement();
            xml.writeCharacters("\n");
        }

        xml.writeCharacters("    ");
        xml.writeEndElement();
        xml.writeCharacters("\n");
    }

    private static void writeElement(XMLStreamWriter xml, String tag, String value, int indent)
            throws XMLStreamException {
        xml.writeCharacters(" ".repeat(indent));
        xml.writeStartElement(tag);
        xml.writeCharacters(value != null ? value : "");
        xml.writeEndElement();
        xml.writeCharacters("\n");
    }

    private static void writeOptional(XMLStreamWriter xml, String tag, String value, int indent)
            throws XMLStreamException {
        if (value != null && !value.isEmpty()) {
            writeElement(xml, tag, value, indent);
        }
    }

    // -------------------------------------------------------------------------
    // Javadoc parsing
    // -------------------------------------------------------------------------

    private static JavadocInfo parseJavadoc(String raw) {
        JavadocInfo info = new JavadocInfo();
        if (raw == null || raw.isBlank()) {
            return info;
        }
        String[] parts = raw.split("(?m)^\\s*@");
        String body = parts[0];
        body = body.replaceAll("\\{@\\w+\\s+([^}]*)}", "$1");
        body = body.replaceAll("<[^>]+>", "");
        String trimmed = body.strip();
        info.description = trimmed.isEmpty() ? null : trimmed.replaceAll("\\s+", " ");

        for (int i = 1; i < parts.length; i++) {
            String tag = parts[i].trim();
            if (tag.startsWith("since")) {
                info.since = tag.substring("since".length()).trim().replaceAll("\\s+", " ");
            } else if (tag.startsWith("deprecated")) {
                info.deprecated = tag.substring("deprecated".length()).trim().replaceAll("\\s+", " ");
            }
        }
        return info;
    }

    // -------------------------------------------------------------------------
    // Type hierarchy walking
    // -------------------------------------------------------------------------

    private TypeElement superTypeElement(TypeElement te) {
        TypeMirror superMirror = te.getSuperclass();
        if (superMirror == null || superMirror.getKind() == TypeKind.NONE) {
            return null;
        }
        if (superMirror instanceof DeclaredType dt && dt.asElement() instanceof TypeElement superType) {
            return isObject(superType) ? null : superType;
        }
        return null;
    }

    private static boolean isObject(TypeElement te) {
        return "java.lang.Object".equals(te.getQualifiedName().toString());
    }

    // -------------------------------------------------------------------------
    // Goal prefix derivation
    // -------------------------------------------------------------------------

    static String deriveGoalPrefix(String artifactId) {
        if (artifactId == null || artifactId.isEmpty()) {
            return "";
        }
        String s = artifactId;
        if (s.startsWith("maven-")) {
            s = s.substring("maven-".length());
        }
        if (s.endsWith("-maven-plugin")) {
            s = s.substring(0, s.length() - "-maven-plugin".length());
        } else if (s.endsWith("-plugin")) {
            s = s.substring(0, s.length() - "-plugin".length());
        }
        return s;
    }

    // -------------------------------------------------------------------------
    // Logging helpers
    // -------------------------------------------------------------------------

    private void note(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
    }

    private void logError(String message, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        processingEnv
                .getMessager()
                .printMessage(
                        Diagnostic.Kind.ERROR,
                        message + "\n" + e.getClass().getName() + ": " + e.getMessage() + "\n" + sw);
    }

    private static void writeLicenseHeader(PrintWriter pw) {
        pw.println("/*");
        pw.println(" * Licensed to the Apache Software Foundation (ASF) under one");
        pw.println(" * or more contributor license agreements.  See the NOTICE file");
        pw.println(" * distributed with this work for additional information");
        pw.println(" * regarding copyright ownership.  The ASF licenses this file");
        pw.println(" * to you under the Apache License, Version 2.0 (the");
        pw.println(" * \"License\"); you may not use this file except in compliance");
        pw.println(" * with the License.  You may obtain a copy of the License at");
        pw.println(" *");
        pw.println(" *   http://www.apache.org/licenses/LICENSE-2.0");
        pw.println(" *");
        pw.println(" * Unless required by applicable law or agreed to in writing,");
        pw.println(" * software distributed under the License is distributed on an");
        pw.println(" * \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY");
        pw.println(" * KIND, either express or implied.  See the License for the");
        pw.println(" * specific language governing permissions and limitations");
        pw.println(" * under the License.");
        pw.println(" */");
    }

    // -------------------------------------------------------------------------
    // Data holders
    // -------------------------------------------------------------------------

    private static class JavadocInfo {
        String description;
        String since;
        String deprecated;
    }
}
