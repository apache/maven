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
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
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

/**
 * JSR 269 annotation processor that generates Maven plugin descriptors ({@code META-INF/maven/plugin.xml})
 * and DI factory classes at compile time from {@code @Mojo}-annotated classes.
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
 * <p>The generated {@code plugin.xml} follows the {@code http://maven.apache.org/PLUGIN/2.0.0} namespace.
 * One factory class is generated per mojo; the factory is annotated with
 * {@code @org.apache.maven.api.di.Named("groupId:artifactId:version:goal")} so that the DI container
 * can locate it by the role-hint used in {@code DefaultMavenPluginManager.loadV4Mojo()}.
 * The {@code DiIndexProcessor} (from {@code maven-api-di}) picks up the {@code @Named} factory in the
 * next processing round and registers it in {@code META-INF/maven/org.apache.maven.api.di.Inject}.
 *
 * <p>Design notes:
 * <ul>
 *   <li>Returns {@code false} from {@code process()} — does not claim {@code @Mojo}; other processors
 *       and IDEs can still see it.</li>
 *   <li>Annotation references are string-based — no compile dependency on {@code maven-api-core} or
 *       {@code maven-api-di}; mirror names are compared as strings.</li>
 *   <li>Full type hierarchy is walked to collect {@code @Parameter} and {@code @Resolution} fields,
 *       including those declared in parent classes from dependency JARs.</li>
 *   <li>Javadoc is extracted via {@code Elements.getDocComment()} — only available for source elements;
 *       inherited parameters from binary JARs will have {@code null} descriptions.</li>
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

    private static final String MOJO_ANN = "org.apache.maven.api.plugin.annotations.Mojo";
    private static final String PARAMETER_ANN = "org.apache.maven.api.plugin.annotations.Parameter";
    private static final String RESOLUTION_ANN = "org.apache.maven.api.plugin.annotations.Resolution";
    private static final String EXECUTE_ANN = "org.apache.maven.api.plugin.annotations.Execute";
    private static final String AFTER_ANN = "org.apache.maven.api.plugin.annotations.After";
    private static final String AFTERS_ANN = "org.apache.maven.api.plugin.annotations.Afters";

    private static final String PLUGIN_NS = "http://maven.apache.org/PLUGIN/2.0.0";

    /** Mojos collected across all rounds (FQN → TypeElement). */
    private final Map<String, TypeElement> mojoElements = new LinkedHashMap<>();

    /** Factory class names generated in previous rounds — skip re-processing them. */
    private final Set<String> generatedFactoryNames = new LinkedHashSet<>();

    /** Coordinates resolved lazily on first use (from POM + -A overrides). */
    private PomCoordinates resolvedCoordinates;

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
                // Skip factory classes we generated in a previous round
                if (generatedFactoryNames.contains(fqn)) {
                    continue;
                }
                // Validate: must be a concrete class
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
                    // Generate factory immediately in this round so DiIndexProcessor can pick it
                    // up in the next round. Factories generated in processingOver() are invisible
                    // to subsequent processors because there are no more rounds after the last one.
                    PomCoordinates coords = resolveCoordinates();
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
            PomCoordinates coords = resolveCoordinates();
            if (coords == null) {
                return false;
            }
            try {
                writePluginXml(coords);
            } catch (Exception e) {
                logError("Failed to generate plugin.xml", e);
            }
        }

        return false; // do not claim the annotation
    }

    // -------------------------------------------------------------------------
    // Coordinate resolution (POM + -A overrides)
    // -------------------------------------------------------------------------

    /**
     * Resolves plugin coordinates from {@code -A} compiler options.
     * The result is cached after the first successful resolution.
     *
     * @return resolved coordinates, or {@code null} if required options are missing
     */
    PomCoordinates resolveCoordinates() {
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

        resolvedCoordinates = new PomCoordinates(groupId, artifactId, version, goalPrefix);
        return resolvedCoordinates;
    }

    private String getOption(String key) {
        return processingEnv.getOptions().get(key);
    }

    // -------------------------------------------------------------------------
    // plugin.xml generation
    // -------------------------------------------------------------------------

    private void writePluginXml(PomCoordinates coords) throws IOException, XMLStreamException {
        String groupId = coords.groupId;
        String artifactId = coords.artifactId;
        String version = coords.version;
        String goalPrefix = coords.goalPrefix;

        FileObject out =
                processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/maven/plugin.xml");

        try (Writer w = out.openWriter()) {
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            XMLStreamWriter xml = factory.createXMLStreamWriter(w);
            try {
                xml.writeStartDocument("UTF-8", "1.0");
                xml.writeCharacters("\n");
                xml.writeStartElement("plugin");
                xml.writeDefaultNamespace(PLUGIN_NS);
                xml.writeCharacters("\n");

                writeElement(xml, "name", artifactId, 2);
                writeElement(xml, "groupId", groupId, 2);
                writeElement(xml, "artifactId", artifactId, 2);
                writeElement(xml, "version", version, 2);
                writeElement(xml, "goalPrefix", goalPrefix, 2);

                xml.writeCharacters("  ");
                xml.writeStartElement("mojos");
                xml.writeCharacters("\n");

                for (TypeElement mojoType : mojoElements.values()) {
                    writeMojo(xml, mojoType, groupId, artifactId, version);
                }

                xml.writeCharacters("  ");
                xml.writeEndElement(); // mojos
                xml.writeCharacters("\n");
                xml.writeEndElement(); // plugin
                xml.writeCharacters("\n");
                xml.writeEndDocument();
            } finally {
                xml.close();
            }
        }
        note("Generated META-INF/maven/plugin.xml with " + mojoElements.size() + " mojo(s)");
    }

    private void writeMojo(XMLStreamWriter xml, TypeElement mojoType, String groupId, String artifactId, String version)
            throws XMLStreamException {
        AnnotationMirror mojoAnn = findAnnotation(mojoType, MOJO_ANN);
        if (mojoAnn == null) {
            return;
        }

        String fqn = mojoType.getQualifiedName().toString();
        String goal = getStringValue(mojoAnn, "name", "");
        if (goal.isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@Mojo.name() must not be empty", mojoType);
            return;
        }

        // Javadoc for the mojo class
        JavadocInfo mojoDoc = parseJavadoc(processingEnv.getElementUtils().getDocComment(mojoType));

        xml.writeCharacters("    ");
        xml.writeStartElement("mojo");
        xml.writeCharacters("\n");

        writeElement(xml, "goal", goal, 6);
        writeElementIfNotEmpty(xml, "phase", getStringValue(mojoAnn, "defaultPhase", ""), 6);
        writeElement(xml, "implementation", fqn, 6);
        writeElement(xml, "language", "java", 6);
        writeElementIfNotEmpty(xml, "description", mojoDoc.description, 6);
        writeElementIfNotEmpty(xml, "since", mojoDoc.since, 6);
        writeElementIfNotEmpty(xml, "deprecated", mojoDoc.deprecated, 6);
        writeElement(xml, "projectRequired", String.valueOf(getBooleanValue(mojoAnn, "projectRequired", true)), 6);
        writeElement(xml, "onlineRequired", String.valueOf(getBooleanValue(mojoAnn, "onlineRequired", false)), 6);
        writeElement(xml, "aggregator", String.valueOf(getBooleanValue(mojoAnn, "aggregator", false)), 6);
        writeElementIfNotEmpty(xml, "configurator", getStringValue(mojoAnn, "configurator", ""), 6);

        // @Execute
        AnnotationMirror executeAnn = findAnnotation(mojoType, EXECUTE_ANN);
        if (executeAnn != null) {
            writeElementIfNotEmpty(xml, "executePhase", getStringValue(executeAnn, "phase", ""), 6);
            writeElementIfNotEmpty(xml, "executeGoal", getStringValue(executeAnn, "goal", ""), 6);
            writeElementIfNotEmpty(xml, "executeLifecycle", getStringValue(executeAnn, "lifecycle", ""), 6);
        }

        // dependencyResolution / dependencyCollection from @Mojo
        writeElementIfNotEmpty(
                xml, "dependencyResolution", getStringValue(mojoAnn, "dependencyResolutionPathScopes", ""), 6);
        boolean depCollection = getBooleanValue(mojoAnn, "dependencyCollection", false);
        if (depCollection) {
            writeElement(xml, "dependencyCollection", "true", 6);
        }

        // @Parameter fields — walk the full type hierarchy
        List<ParameterInfo> params = collectParameters(mojoType);
        if (!params.isEmpty()) {
            xml.writeCharacters("      ");
            xml.writeStartElement("parameters");
            xml.writeCharacters("\n");
            for (ParameterInfo p : params) {
                writeParameter(xml, p);
            }
            xml.writeCharacters("      ");
            xml.writeEndElement(); // parameters
            xml.writeCharacters("\n");
        }

        // @Resolution fields
        List<ResolutionInfo> resolutions = collectResolutions(mojoType);
        if (!resolutions.isEmpty()) {
            xml.writeCharacters("      ");
            xml.writeStartElement("resolutions");
            xml.writeCharacters("\n");
            for (ResolutionInfo r : resolutions) {
                writeResolution(xml, r);
            }
            xml.writeCharacters("      ");
            xml.writeEndElement(); // resolutions
            xml.writeCharacters("\n");
        }

        // @After / @Afters
        List<AfterInfo> afters = collectAfters(mojoType);
        if (!afters.isEmpty()) {
            xml.writeCharacters("      ");
            xml.writeStartElement("afterLinks");
            xml.writeCharacters("\n");
            for (AfterInfo a : afters) {
                writeAfterLink(xml, a);
            }
            xml.writeCharacters("      ");
            xml.writeEndElement(); // afterLinks
            xml.writeCharacters("\n");
        }

        xml.writeCharacters("    ");
        xml.writeEndElement(); // mojo
        xml.writeCharacters("\n");
    }

    // -------------------------------------------------------------------------
    // Parameter collection
    // -------------------------------------------------------------------------

    private List<ParameterInfo> collectParameters(TypeElement mojoType) {
        List<ParameterInfo> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        TypeElement current = mojoType;
        while (current != null && !isObject(current)) {
            for (Element enclosed : current.getEnclosedElements()) {
                if (enclosed.getKind() != ElementKind.FIELD && enclosed.getKind() != ElementKind.METHOD) {
                    continue;
                }
                AnnotationMirror paramAnn = findAnnotation(enclosed, PARAMETER_ANN);
                if (paramAnn == null) {
                    continue;
                }
                String fieldName = enclosed.getSimpleName().toString();
                String paramName = getStringValue(paramAnn, "name", "");
                if (paramName.isEmpty()) {
                    paramName = fieldName;
                }
                if (seen.contains(paramName)) {
                    continue; // subclass overrides parent
                }
                seen.add(paramName);

                String type;
                if (enclosed.getKind() == ElementKind.FIELD) {
                    type = ((VariableElement) enclosed).asType().toString();
                } else {
                    // method: use parameter type of setter, or return type of getter
                    ExecutableElement method = (ExecutableElement) enclosed;
                    if (!method.getParameters().isEmpty()) {
                        type = method.getParameters().get(0).asType().toString();
                    } else {
                        type = method.getReturnType().toString();
                    }
                }

                JavadocInfo doc = parseJavadoc(processingEnv.getElementUtils().getDocComment(enclosed));

                ParameterInfo p = new ParameterInfo();
                p.name = paramName;
                p.alias = getStringValue(paramAnn, "alias", "");
                p.type = type;
                p.required = getBooleanValue(paramAnn, "required", false);
                p.editable = !getBooleanValue(paramAnn, "readonly", false); // inverted!
                p.expression = getStringValue(paramAnn, "property", "");
                if (!p.expression.isEmpty() && !p.expression.startsWith("${")) {
                    p.expression = "${" + p.expression + "}";
                }
                p.defaultValue = getStringValue(paramAnn, "defaultValue", "");
                p.description = doc.description;
                p.since = doc.since;
                p.deprecated = doc.deprecated;
                result.add(p);
            }
            current = superTypeElement(current);
        }
        return result;
    }

    private void writeParameter(XMLStreamWriter xml, ParameterInfo p) throws XMLStreamException {
        xml.writeCharacters("        ");
        xml.writeStartElement("parameter");
        xml.writeCharacters("\n");
        writeElement(xml, "name", p.name, 10);
        writeElementIfNotEmpty(xml, "alias", p.alias, 10);
        writeElement(xml, "type", p.type, 10);
        writeElement(xml, "required", String.valueOf(p.required), 10);
        writeElement(xml, "editable", String.valueOf(p.editable), 10);
        writeElementIfNotEmpty(xml, "description", p.description, 10);
        writeElementIfNotEmpty(xml, "expression", p.expression, 10);
        writeElementIfNotEmpty(xml, "defaultValue", p.defaultValue, 10);
        xml.writeCharacters("        ");
        xml.writeEndElement(); // parameter
        xml.writeCharacters("\n");
    }

    // -------------------------------------------------------------------------
    // Resolution collection
    // -------------------------------------------------------------------------

    private List<ResolutionInfo> collectResolutions(TypeElement mojoType) {
        List<ResolutionInfo> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        TypeElement current = mojoType;
        while (current != null && !isObject(current)) {
            for (Element enclosed : current.getEnclosedElements()) {
                if (enclosed.getKind() != ElementKind.FIELD) {
                    continue;
                }
                AnnotationMirror resAnn = findAnnotation(enclosed, RESOLUTION_ANN);
                if (resAnn == null) {
                    continue;
                }
                String fieldName = enclosed.getSimpleName().toString();
                if (seen.contains(fieldName)) {
                    continue;
                }
                seen.add(fieldName);

                ResolutionInfo r = new ResolutionInfo();
                r.field = fieldName;
                r.pathScope = getStringValue(resAnn, "pathScope", "");
                r.requestType = getStringValue(resAnn, "requestType", "");
                result.add(r);
            }
            current = superTypeElement(current);
        }
        return result;
    }

    private void writeResolution(XMLStreamWriter xml, ResolutionInfo r) throws XMLStreamException {
        xml.writeCharacters("        ");
        xml.writeStartElement("resolution");
        xml.writeCharacters("\n");
        writeElement(xml, "field", r.field, 10);
        writeElementIfNotEmpty(xml, "pathScope", r.pathScope, 10);
        writeElementIfNotEmpty(xml, "requestType", r.requestType, 10);
        xml.writeCharacters("        ");
        xml.writeEndElement(); // resolution
        xml.writeCharacters("\n");
    }

    // -------------------------------------------------------------------------
    // @After / @Afters collection
    // -------------------------------------------------------------------------

    private List<AfterInfo> collectAfters(TypeElement mojoType) {
        List<AfterInfo> result = new ArrayList<>();
        // Check for @Afters (container) first
        AnnotationMirror aftersAnn = findAnnotation(mojoType, AFTERS_ANN);
        if (aftersAnn != null) {
            AnnotationValue valueAttr = getAnnotationValue(aftersAnn, "value");
            if (valueAttr != null && valueAttr.getValue() instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof AnnotationValue av && av.getValue() instanceof AnnotationMirror afterMirror) {
                        result.add(extractAfter(afterMirror));
                    }
                }
            }
        }
        // Also check for a single @After directly on the class
        AnnotationMirror afterAnn = findAnnotation(mojoType, AFTER_ANN);
        if (afterAnn != null && aftersAnn == null) {
            result.add(extractAfter(afterAnn));
        }
        return result;
    }

    private AfterInfo extractAfter(AnnotationMirror ann) {
        AfterInfo a = new AfterInfo();
        a.phase = getStringValue(ann, "phase", "");
        a.type = getEnumValue(ann, "type", "PROJECT");
        a.scope = getStringValue(ann, "scope", "");
        return a;
    }

    private void writeAfterLink(XMLStreamWriter xml, AfterInfo a) throws XMLStreamException {
        xml.writeCharacters("        ");
        xml.writeStartElement("afterLink");
        xml.writeCharacters("\n");
        writeElementIfNotEmpty(xml, "phase", a.phase, 10);
        writeElement(xml, "type", a.type, 10);
        writeElementIfNotEmpty(xml, "scope", a.scope, 10);
        xml.writeCharacters("        ");
        xml.writeEndElement(); // afterLink
        xml.writeCharacters("\n");
    }

    // -------------------------------------------------------------------------
    // Factory class generation
    // -------------------------------------------------------------------------

    private void generateFactory(TypeElement mojoType, PomCoordinates coords) throws IOException {
        String groupId = coords.groupId;
        String artifactId = coords.artifactId;
        String version = coords.version;

        AnnotationMirror mojoAnn = findAnnotation(mojoType, MOJO_ANN);
        if (mojoAnn == null) {
            return;
        }
        String goal = getStringValue(mojoAnn, "name", "");
        if (goal.isEmpty()) {
            return;
        }

        String fqn = mojoType.getQualifiedName().toString();
        String pkg = fqn.contains(".") ? fqn.substring(0, fqn.lastIndexOf('.')) : "";
        String simpleName = mojoType.getSimpleName().toString();
        String factorySimpleName = simpleName + "$MojoFactory";
        String factoryFqn = pkg.isEmpty() ? factorySimpleName : pkg + "." + factorySimpleName;
        String roleHint = groupId + ":" + artifactId + ":" + version + ":" + goal;

        // Collect non-private constructors to replicate in the factory
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
            pw.println("/**");
            pw.println(" * DI factory for the {@code " + goal + "} mojo.");
            pw.println(" * Generated by {@link org.apache.maven.plugin.processor.MojoDescriptorProcessor}.");
            pw.println(" *");
            pw.println(" * @since 4.1.0");
            pw.println(" */");
            pw.println("@org.apache.maven.api.di.Named(\"" + roleHint + "\")");
            pw.println("public class " + factorySimpleName + " extends " + simpleName + " {");
            pw.println();

            if (!ctors.isEmpty() && !hasOnlyDefaultCtor(ctors)) {
                for (ExecutableElement ctor : ctors) {
                    if (ctor.getParameters().isEmpty()) {
                        continue; // default ctor — implicit delegation to super
                    }
                    pw.print("    public " + factorySimpleName + "(");
                    List<? extends VariableElement> params = ctor.getParameters();
                    StringBuilder args = new StringBuilder();
                    for (int i = 0; i < params.size(); i++) {
                        VariableElement param = params.get(i);
                        String typeName = param.asType().toString();
                        String paramName = param.getSimpleName().toString();
                        if (i > 0) {
                            pw.print(", ");
                            args.append(", ");
                        }
                        pw.print(typeName + " " + paramName);
                        args.append(paramName);
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
        note("Generated factory " + factoryFqn + " for goal '" + goal + "'");
    }

    private boolean hasOnlyDefaultCtor(List<ExecutableElement> ctors) {
        return ctors.size() == 1 && ctors.get(0).getParameters().isEmpty();
    }

    // -------------------------------------------------------------------------
    // Javadoc parsing
    // -------------------------------------------------------------------------

    private static JavadocInfo parseJavadoc(String raw) {
        JavadocInfo info = new JavadocInfo();
        if (raw == null || raw.isBlank()) {
            return info;
        }
        // Split on block Javadoc tags (tags at start of a line, possibly with leading whitespace)
        String[] parts = raw.split("(?m)^\\s*@");
        String body = parts[0];

        // Strip inline tags like {@code ...}, {@link ...} etc. — replace with their text content
        body = body.replaceAll("\\{@\\w+\\s+([^}]*)}", "$1");
        // Remove HTML tags
        body = body.replaceAll("<[^>]+>", "");
        info.description = normalizeWhitespace(body);

        // Parse @since, @deprecated from remaining parts
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

    private static String normalizeWhitespace(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.strip();
        if (trimmed.isEmpty()) {
            return null;
        }
        // Collapse internal whitespace runs to a single space
        return trimmed.replaceAll("\\s+", " ");
    }

    // -------------------------------------------------------------------------
    // Annotation mirror utilities
    // -------------------------------------------------------------------------

    private static AnnotationMirror findAnnotation(Element element, String annotationFqn) {
        for (AnnotationMirror am : element.getAnnotationMirrors()) {
            if (am.getAnnotationType().toString().equals(annotationFqn)) {
                return am;
            }
        }
        return null;
    }

    private static AnnotationValue getAnnotationValue(AnnotationMirror am, String methodName) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                am.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(methodName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String getStringValue(AnnotationMirror am, String methodName, String defaultValue) {
        AnnotationValue av = getAnnotationValue(am, methodName);
        if (av == null) {
            return defaultValue;
        }
        Object v = av.getValue();
        return v != null ? v.toString() : defaultValue;
    }

    private static boolean getBooleanValue(AnnotationMirror am, String methodName, boolean defaultValue) {
        AnnotationValue av = getAnnotationValue(am, methodName);
        if (av == null) {
            return defaultValue;
        }
        Object v = av.getValue();
        return v instanceof Boolean ? (Boolean) v : defaultValue;
    }

    /**
     * Returns the simple name of an enum value from an annotation attribute.
     * Enum values in annotation mirrors are represented as {@link VariableElement}.
     */
    private static String getEnumValue(AnnotationMirror am, String methodName, String defaultValue) {
        AnnotationValue av = getAnnotationValue(am, methodName);
        if (av == null) {
            return defaultValue;
        }
        Object v = av.getValue();
        if (v instanceof VariableElement ve) {
            return ve.getSimpleName().toString();
        }
        return defaultValue;
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
    // XML writing helpers
    // -------------------------------------------------------------------------

    private static void writeElement(XMLStreamWriter xml, String tag, String value) throws XMLStreamException {
        writeElement(xml, tag, value, 4);
    }

    private static void writeElement(XMLStreamWriter xml, String tag, String value, int indent)
            throws XMLStreamException {
        xml.writeCharacters(" ".repeat(indent));
        xml.writeStartElement(tag);
        xml.writeCharacters(value != null ? value : "");
        xml.writeEndElement();
        xml.writeCharacters("\n");
    }

    private static void writeElementIfNotEmpty(XMLStreamWriter xml, String tag, String value, int indent)
            throws XMLStreamException {
        if (value != null && !value.isEmpty()) {
            writeElement(xml, tag, value, indent);
        }
    }

    // -------------------------------------------------------------------------
    // Goal prefix derivation
    // -------------------------------------------------------------------------

    /**
     * Derives the Maven goal prefix from the artifact ID by stripping the
     * {@code maven-} prefix and {@code -plugin} suffix (or vice-versa).
     * For example: {@code maven-compiler-plugin} → {@code compiler}.
     */
    static String deriveGoalPrefix(String artifactId) {
        if (artifactId == null || artifactId.isEmpty()) {
            return "";
        }
        String s = artifactId;
        if (s.startsWith("maven-")) {
            s = s.substring("maven-".length());
        }
        // Check longer suffix first to avoid -plugin swallowing part of -maven-plugin
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

    // -------------------------------------------------------------------------
    // License header helper
    // -------------------------------------------------------------------------

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

    private static class ParameterInfo {
        String name;
        String alias;
        String type;
        boolean required;
        boolean editable;
        String expression;
        String defaultValue;
        String description;
        String since;
        String deprecated;
    }

    private static class ResolutionInfo {
        String field;
        String pathScope;
        String requestType;
    }

    private static class AfterInfo {
        String phase;
        String type;
        String scope;
    }

    static class PomCoordinates {
        final String groupId;
        final String artifactId;
        final String version;
        final String goalPrefix;

        PomCoordinates(String groupId, String artifactId, String version, String goalPrefix) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.goalPrefix = goalPrefix;
        }
    }
}
