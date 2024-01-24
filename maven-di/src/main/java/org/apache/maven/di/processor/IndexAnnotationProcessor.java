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
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

import org.apache.maven.api.di.Qualifier;

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
