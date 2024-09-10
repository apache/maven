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
package org.apache.maven.di.tool;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import org.apache.maven.api.di.Named;

// Auto-register the annotation processor
@SupportedAnnotationTypes("org.apache.maven.api.di.Named")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class DiIndexProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Collect the fully qualified names of classes annotated with @Named
        StringBuilder builder = new StringBuilder();

        processingEnv
                .getMessager()
                .printMessage(
                        Diagnostic.Kind.NOTE,
                        "Processing " + roundEnv.getRootElements().size() + " classes");

        for (Element element : roundEnv.getElementsAnnotatedWith(Named.class)) {
            if (element instanceof TypeElement typeElement) {
                // Get the fully qualified class name
                String className = typeElement.getQualifiedName().toString();

                // Handle inner classes by checking if the enclosing element is a class or interface
                Element enclosingElement = typeElement.getEnclosingElement();
                if (enclosingElement instanceof TypeElement) {
                    // It's an inner class, replace the last dot with a '$'
                    String enclosingClassName =
                            ((TypeElement) enclosingElement).getQualifiedName().toString();
                    className = enclosingClassName + "$" + typeElement.getSimpleName();
                }

                builder.append(className).append("\n");
            }
        }

        if (!builder.isEmpty()) { // Check if the StringBuilder is non-empty
            try {
                writeFile(builder.toString());
            } catch (IOException e) {
                processingEnv
                        .getMessager()
                        .printMessage(Diagnostic.Kind.ERROR, "Error writing file: " + e.getMessage());
            }
        }

        return true; // Indicate that annotations are claimed by this processor
    }

    private void writeFile(String content) throws IOException {
        // Create the file META-INF/maven/org.apache.maven.api.di.Inject
        FileObject fileObject = processingEnv
                .getFiler()
                .createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/maven/org.apache.maven.api.di.Inject");

        try (Writer writer = fileObject.openWriter()) {
            writer.write(content);
        }
    }
}
