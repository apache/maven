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
 * @since 4.2.0
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
    private boolean coordinateErrorEmitted = false;

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
                try {
                    generateHelpMojo(coords);
                } catch (IOException e) {
                    error("Failed to generate HelpMojo", e);
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
            if (!coordinateErrorEmitted) {
                coordinateErrorEmitted = true;
                error("Could not determine plugin coordinates. "
                        + "Supply -Amaven.plugin.groupId, -Amaven.plugin.artifactId and "
                        + "-Amaven.plugin.version via the maven-plugin-mixin.");
            }
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

    private void generateHelpMojo(String[] coords) throws IOException {
        // Determine the package: use the first mojo's package as the HelpMojo package.
        // For plugins with mojos in multiple packages, the first registered mojo's package is used.
        String pkg = mojoElements.values().stream()
                .map(te -> {
                    String fqn = te.getQualifiedName().toString();
                    int dot = fqn.lastIndexOf('.');
                    return dot > 0 ? fqn.substring(0, dot) : "";
                })
                .findFirst()
                .orElse("");

        String helpFqn = pkg.isEmpty() ? "HelpMojo" : pkg + ".HelpMojo";
        // Idempotency guard: JSR 269 fires a final round with processingOver()==true
        // even when no new files were generated; without this guard, createSourceFile()
        // would throw FilerException on the second call.
        if (generatedFactoryNames.contains(helpFqn)) {
            return;
        }
        String pluginXmlPath = "/META-INF/maven/plugin.xml";
        String goalPrefix = coords[3];
        String artifactId = coords[1];
        String version = coords[2];
        try (PrintWriter pw = new PrintWriter(
                processingEnv.getFiler().createSourceFile(helpFqn).openWriter())) {
            writeHelpMojoHeader(pw, pkg, artifactId, version, goalPrefix, pluginXmlPath);
            writeHelpMojoMethods(pw, pluginXmlPath);
        }
        generatedFactoryNames.add(helpFqn);
        note("Generated " + helpFqn);
    }

    private static void writeHelpMojoHeader(
            PrintWriter pw,
            String pkg,
            String artifactId,
            String version,
            String goalPrefix,
            String pluginXmlPath) {
        if (!pkg.isEmpty()) {
            pw.println("package " + pkg + ";");
            pw.println();
        }
        pw.println("import org.apache.maven.api.di.Inject;");
        pw.println("import org.apache.maven.api.plugin.Log;");
        pw.println("import org.apache.maven.api.plugin.MojoException;");
        pw.println("import org.apache.maven.api.plugin.annotations.Mojo;");
        pw.println("import org.apache.maven.api.plugin.annotations.Parameter;");
        pw.println("import org.w3c.dom.Document;");
        pw.println("import org.w3c.dom.Element;");
        pw.println("import org.w3c.dom.Node;");
        pw.println("import org.w3c.dom.NodeList;");
        pw.println("import org.xml.sax.SAXException;");
        pw.println("import javax.xml.parsers.DocumentBuilder;");
        pw.println("import javax.xml.parsers.DocumentBuilderFactory;");
        pw.println("import javax.xml.parsers.ParserConfigurationException;");
        pw.println("import java.io.IOException;");
        pw.println("import java.io.InputStream;");
        pw.println("import java.util.ArrayList;");
        pw.println("import java.util.List;");
        pw.println();
        pw.println("/**");
        pw.println(" * Display help information on " + artifactId + ".<br>");
        pw.println(" * Call <code>mvn " + goalPrefix + ":help -Ddetail=true -Dgoal=&lt;goal-name&gt;</code>"
                + " to display parameter details.");
        pw.println(" *");
        pw.println(" * @since " + version);
        pw.println(" */");
        pw.println("@Mojo(name = \"help\", projectRequired = false)");
        pw.println("public class HelpMojo implements org.apache.maven.api.plugin.Mojo {");
        pw.println();
        pw.println("    @Inject");
        pw.println("    private Log logger;");
        pw.println();
        pw.println("    /** If {@code true}, display all settable properties for each goal. */");
        pw.println("    @Parameter(property = \"detail\", defaultValue = \"false\")");
        pw.println("    private boolean detail;");
        pw.println();
        pw.println("    /** The name of the goal for which to show help."
                + " If unspecified, all goals will be displayed. */");
        pw.println("    @Parameter(property = \"goal\")");
        pw.println("    private String goal;");
        pw.println();
        pw.println("    /** The maximum length of a display line, should be positive. */");
        pw.println("    @Parameter(property = \"lineLength\", defaultValue = \"80\")");
        pw.println("    private int lineLength;");
        pw.println();
        pw.println("    /** The number of spaces per indentation level, should be positive. */");
        pw.println("    @Parameter(property = \"indentSize\", defaultValue = \"2\")");
        pw.println("    private int indentSize;");
        pw.println();
        pw.println("    private static final String PLUGIN_XML_PATH = \"" + pluginXmlPath + "\";");
        pw.println("    private static final int DEFAULT_LINE_LENGTH = 80;");
    }

    @SuppressWarnings("checkstyle:MethodLength")
    private static void writeHelpMojoMethods(PrintWriter pw, String pluginXmlPath) {
        pw.println();
        pw.println("    @Override");
        pw.println("    public void execute() throws MojoException {");
        pw.println("        if (lineLength <= 0) {");
        pw.println(
                "            logger.warn(\"The parameter 'lineLength' should be positive, using '80' as default.\");");
        pw.println("            lineLength = DEFAULT_LINE_LENGTH;");
        pw.println("        }");
        pw.println("        if (indentSize <= 0) {");
        pw.println(
                "            logger.warn(\"The parameter 'indentSize' should be positive, using '2' as default.\");");
        pw.println("            indentSize = 2;");
        pw.println("        }");
        pw.println("        Document doc = loadPluginXml();");
        pw.println("        StringBuilder sb = new StringBuilder();");
        pw.println("        Node plugin = getSingleChild(doc, \"plugin\");");
        pw.println("        String name = getTextContent(plugin, \"name\");");
        pw.println("        String version = getTextContent(plugin, \"version\");");
        pw.println("        String id = getTextContent(plugin, \"groupId\") + \":\"");
        pw.println("                + getTextContent(plugin, \"artifactId\") + \":\" + version;");
        pw.println("        if (name != null && !name.isEmpty() && !name.contains(id)) {");
        pw.println("            append(sb, name + \" \" + version, 0);");
        pw.println("        } else {");
        pw.println("            append(sb, name != null && !name.isEmpty() ? name : id, 0);");
        pw.println("        }");
        pw.println("        String pluginDescription = getTextContent(plugin, \"description\");");
        pw.println("        if (pluginDescription != null) {");
        pw.println("            append(sb, pluginDescription, 1);");
        pw.println("        }");
        pw.println("        append(sb, \"\", 0);");
        pw.println("        String goalPrefix = getTextContent(plugin, \"goalPrefix\");");
        pw.println("        List<Node> mojos = findChildren(getSingleChild(plugin, \"mojos\"), \"mojo\");");
        pw.println("        if (goal == null || goal.isEmpty()) {");
        pw.println("            append(sb, \"This plugin has \" + mojos.size()");
        pw.println("                    + (mojos.size() > 1 ? \" goals:\" : \" goal:\"), 0);");
        pw.println("            append(sb, \"\", 0);");
        pw.println("        }");
        pw.println("        for (Node mojo : mojos) {");
        pw.println("            writeGoal(sb, goalPrefix, (Element) mojo);");
        pw.println("        }");
        pw.println("        if (logger.isInfoEnabled()) {");
        pw.println("            logger.info(sb.toString());");
        pw.println("        }");
        pw.println("    }");
        pw.println();
        pw.println("    private Document loadPluginXml() throws MojoException {");
        pw.println("        try (InputStream is = getClass().getResourceAsStream(PLUGIN_XML_PATH)) {");
        pw.println("            if (is == null) {");
        pw.println(
                "                throw new MojoException(\"Could not find plugin descriptor at \" + PLUGIN_XML_PATH);");
        pw.println("            }");
        pw.println("            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();");
        pw.println("            dbf.setFeature(\"http://xml.org/sax/features/external-general-entities\", false);");
        pw.println(
                "            dbf.setFeature(\"http://xml.org/sax/features/external-parameter-entities\", false);");
        pw.println("            dbf.setExpandEntityReferences(false);");
        pw.println("            DocumentBuilder db = dbf.newDocumentBuilder();");
        pw.println("            return db.parse(is);");
        pw.println("        } catch (IOException | SAXException | ParserConfigurationException e) {");
        pw.println(
                "            throw new MojoException(\"Failed to parse \" + PLUGIN_XML_PATH + \": \" + e.getMessage(), e);");
        pw.println("        }");
        pw.println("    }");
        pw.println();
        pw.println("    private void writeGoal(StringBuilder sb, String goalPrefix, Element mojo) {");
        pw.println("        String mojoGoal = getTextContent(mojo, \"goal\");");
        pw.println("        if (goal != null && !goal.isEmpty() && !goal.equals(mojoGoal)) {");
        pw.println("            return;");
        pw.println("        }");
        pw.println("        append(sb, goalPrefix + \":\" + mojoGoal, 0);");
        pw.println("        String deprecated = getTextContent(mojo, \"deprecated\");");
        pw.println("        String description = getTextContent(mojo, \"description\");");
        pw.println("        if (deprecated != null && !deprecated.isEmpty()) {");
        pw.println("            append(sb, \"Deprecated. \" + deprecated, 1);");
        pw.println("            if (detail && description != null) {");
        pw.println("                append(sb, \"\", 0);");
        pw.println("                append(sb, description, 1);");
        pw.println("            }");
        pw.println("        } else if (description != null) {");
        pw.println("            append(sb, description, 1);");
        pw.println("        }");
        pw.println("        append(sb, \"\", 0);");
        pw.println("        if (detail) {");
        pw.println("            Node parametersNode = findChild(mojo, \"parameters\");");
        pw.println("            if (parametersNode != null) {");
        pw.println("                List<Node> params = findChildren(parametersNode, \"parameter\");");
        pw.println("                // filter out read-only parameters (editable=false)");
        pw.println("                params = params.stream()");
        pw.println("                        .filter(p -> !\"false\".equals(getTextContent(p, \"editable\")))");
        pw.println("                        .toList();");
        pw.println("                if (!params.isEmpty()) {");
        pw.println("                    append(sb, \"Available parameters:\", 1);");
        pw.println("                    append(sb, \"\", 0);");
        pw.println("                    for (Node param : params) {");
        pw.println("                        writeParameter(sb, param);");
        pw.println("                    }");
        pw.println("                }");
        pw.println("            }");
        pw.println("        }");
        pw.println("    }");
        pw.println();
        pw.println("    private void writeParameter(StringBuilder sb, Node parameter) {");
        pw.println("        String paramName = getTextContent(parameter, \"name\");");
        pw.println("        String paramDescription = getTextContent(parameter, \"description\");");
        pw.println("        String defaultValue = getTextContent(parameter, \"defaultValue\");");
        pw.println("        String suffix = (defaultValue != null && !defaultValue.isEmpty())");
        pw.println("                ? \" (Default: \" + defaultValue + \")\" : \"\";");
        pw.println("        append(sb, paramName + suffix, 2);");
        pw.println("        String deprecated = getTextContent(parameter, \"deprecated\");");
        pw.println("        if (deprecated != null && !deprecated.isEmpty()) {");
        pw.println("            append(sb, \"Deprecated. \" + deprecated, 3);");
        pw.println("            append(sb, \"\", 0);");
        pw.println("        }");
        pw.println("        if (paramDescription != null && !paramDescription.isEmpty()) {");
        pw.println("            append(sb, paramDescription, 3);");
        pw.println("        }");
        pw.println("        String required = getTextContent(parameter, \"required\");");
        pw.println("        if (\"true\".equals(required)) {");
        pw.println("            append(sb, \"Required: Yes\", 3);");
        pw.println("        }");
        pw.println("        String expression = getTextContent(parameter, \"expression\");");
        pw.println("        if (expression != null && !expression.isEmpty()) {");
        pw.println("            String property = expression.startsWith(\"${\") && expression.endsWith(\"}\")"
                + " && !expression.substring(2).contains(\"${\")");
        pw.println("                    ? expression.substring(2, expression.length() - 1) : null;");
        pw.println("            if (property != null) {");
        pw.println("                append(sb, \"User property: \" + property, 3);");
        pw.println("            }");
        pw.println("        }");
        pw.println("        append(sb, \"\", 0);");
        pw.println("    }");
        pw.println();
        pw.println("    private void append(StringBuilder sb, String description, int indent) {");
        pw.println("        for (String line : toLines(description, indent)) {");
        pw.println("            sb.append(line).append('\\n');");
        pw.println("        }");
        pw.println("    }");
        pw.println();
        pw.println("    private List<String> toLines(String text, int indent) {");
        pw.println("        List<String> lines = new ArrayList<>();");
        pw.println("        String ind = \"\\t\".repeat(indent);");
        pw.println("        for (String plainLine : text.split(\"(\\r\\n)|(\\r)|(\\n)\")) {");
        pw.println("            toLines(lines, ind + plainLine);");
        pw.println("        }");
        pw.println("        return lines;");
        pw.println("    }");
        pw.println();
        pw.println("    private void toLines(List<String> lines, String line) {");
        pw.println("        int lineIndent = 0;");
        pw.println("        for (int i = 0; i < line.length() && line.charAt(i) == '\\t'; i++) {");
        pw.println("            lineIndent++;");
        pw.println("        }");
        pw.println("        StringBuilder buf = new StringBuilder(256);");
        pw.println("        for (String token : line.split(\" +\")) {");
        pw.println("            if (buf.length() > 0) {");
        pw.println("                if (buf.length() + token.length() >= lineLength) {");
        pw.println("                    lines.add(buf.toString());");
        pw.println("                    buf.setLength(0);");
        pw.println("                    buf.append(\" \".repeat(lineIndent * indentSize));");
        pw.println("                } else {");
        pw.println("                    buf.append(' ');");
        pw.println("                }");
        pw.println("            }");
        pw.println("            for (char c : token.toCharArray()) {");
        pw.println("                buf.append(c == '\\t' ? \" \".repeat(indentSize - buf.length() % indentSize)");
        pw.println("                        : c == '\\u00A0' ? ' ' : c);");
        pw.println("            }");
        pw.println("        }");
        pw.println("        lines.add(buf.toString());");
        pw.println("    }");
        pw.println();
        pw.println("    private static String getTextContent(Node parent, String elementName) {");
        pw.println("        Node child = findChild(parent, elementName);");
        pw.println("        return child != null ? child.getTextContent() : null;");
        pw.println("    }");
        pw.println();
        pw.println("    private static Node findChild(Node parent, String name) {");
        pw.println("        NodeList children = parent.getChildNodes();");
        pw.println("        for (int i = 0; i < children.getLength(); i++) {");
        pw.println("            Node n = children.item(i);");
        pw.println("            if (name.equals(n.getNodeName())) return n;");
        pw.println("        }");
        pw.println("        return null;");
        pw.println("    }");
        pw.println();
        pw.println("    private static Node getSingleChild(Node parent, String name) throws MojoException {");
        pw.println("        Node n = findChild(parent, name);");
        pw.println(
                "        if (n == null) throw new MojoException(\"Missing element <\" + name + \"> in plugin.xml\");");
        pw.println("        return n;");
        pw.println("    }");
        pw.println();
        pw.println("    private static List<Node> findChildren(Node parent, String name) {");
        pw.println("        List<Node> result = new ArrayList<>();");
        pw.println("        NodeList children = parent.getChildNodes();");
        pw.println("        for (int i = 0; i < children.getLength(); i++) {");
        pw.println("            Node n = children.item(i);");
        pw.println("            if (name.equals(n.getNodeName())) result.add(n);");
        pw.println("        }");
        pw.println("        return result;");
        pw.println("    }");
        pw.println("}");
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
