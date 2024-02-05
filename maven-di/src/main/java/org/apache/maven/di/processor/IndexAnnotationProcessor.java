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
package org.apache.maven.di.processor;

import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import javax.xml.stream.XMLStreamReader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.maven.api.di.Qualifier;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.internal.xml.XmlNodeBuilder;

public class IndexAnnotationProcessor implements Processor {

    private ProcessingEnvironment environment;
    private Set<String> index = new TreeSet<>();

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        this.environment = processingEnv;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            if (annotation.getAnnotation(Qualifier.class) != null) {
                for (Element elem : roundEnv.getElementsAnnotatedWith(annotation)) {
                    if (elem.getKind().isClass()) {
                        addClassToIndex(environment
                                .getElementUtils()
                                .getBinaryName((TypeElement) elem)
                                .toString());
                    }
                }
            }
        }
        for (Element elem : roundEnv.getElementsAnnotatedWith(org.apache.maven.api.plugin.annotations.Mojo.class)) {
            PackageElement packageElement = environment.getElementUtils().getPackageOf(elem);
            String packageName = packageElement.getQualifiedName().toString();
            String generatorClassName = elem.getSimpleName().toString() + "Factory";

            String mojoName = elem.getAnnotation(org.apache.maven.api.plugin.annotations.Mojo.class)
                    .name();

            try {
                Reader reader = environment
                        .getFiler()
                        .getResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/maven/plugin.xml")
                        .openReader(true);
                XMLStreamReader parser = WstxInputFactory.newFactory().createXMLStreamReader(reader);
                XmlNode plugin = XmlNodeBuilder.build(parser, null);
                String groupId = plugin.getChild("groupId").getValue();
                String artifactId = plugin.getChild("artifactId").getValue();
                String version = plugin.getChild("version").getValue();

                Writer file = environment
                        .getFiler()
                        .createSourceFile(packageName + "." + generatorClassName)
                        .openWriter();
                file.write("package " + packageName + ";\n");
                file.write("public class " + generatorClassName + " {\n");
                file.write("    @org.apache.maven.api.di.Named(\"" + groupId + ":" + artifactId + ":" + version + ":"
                        + mojoName + "\")\n");
                file.write("    @org.apache.maven.api.di.Provides\n");
                file.write("    public static " + ((TypeElement) elem).getQualifiedName() + " create() {\n");
                file.write("        return new " + ((TypeElement) elem).getQualifiedName() + "();\n");
                file.write("    }\n");
                file.write("}\n");
                file.flush();
                file.close();
            } catch (Exception ex) {
                Logger.getLogger(IndexAnnotationProcessor.class.getName()).log(Level.SEVERE, null, ex);
            }

            addClassToIndex(packageName + "." + generatorClassName);
        }
        if (roundEnv.processingOver()) {
            flushIndex();
        }
        return false;
    }

    protected void addClassToIndex(String className) {
        index.add(className);
    }

    protected void flushIndex() {
        try (BufferedWriter writer = new BufferedWriter(environment
                .getFiler()
                .createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/maven/org.apache.maven.api.di.Inject")
                .openWriter())) {
            for (String line : index) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            environment.getMessager().printMessage(Diagnostic.Kind.WARNING, e.toString());
        }
    }

    @Override
    public Iterable<? extends Completion> getCompletions(
            Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
