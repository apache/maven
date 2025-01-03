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
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.api.di.Named;

@SupportedAnnotationTypes("org.apache.maven.api.di.Named")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class DiIndexProcessor extends AbstractProcessor {

    private final Set<String> processedClasses = new HashSet<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        logMessage(
                Diagnostic.Kind.NOTE, "Processing " + roundEnv.getRootElements().size() + " classes");

        for (Element element : roundEnv.getElementsAnnotatedWith(Named.class)) {
            if (element instanceof TypeElement typeElement) {
                String className = getFullClassName(typeElement);
                processedClasses.add(className);
            }
        }

        if (roundEnv.processingOver()) {
            try {
                updateFileIfChanged();
            } catch (Exception e) {
                logError("Error updating file", e);
            }
        }

        return true;
    }

    private String getFullClassName(TypeElement typeElement) {
        StringBuilder className = new StringBuilder(typeElement.getSimpleName());
        Element enclosingElement = typeElement.getEnclosingElement();

        while (enclosingElement instanceof TypeElement enclosingTypeElement) {
            className.insert(0, "$").insert(0, enclosingTypeElement.getSimpleName());
            enclosingElement = enclosingElement.getEnclosingElement();
        }

        if (enclosingElement instanceof PackageElement packageElement) {
            className.insert(0, ".").insert(0, packageElement.getQualifiedName());
        }

        return className.toString();
    }

    private void updateFileIfChanged() throws IOException {
        String path = "META-INF/maven/org.apache.maven.api.di.Inject";
        Set<String> existingClasses = new TreeSet<>(); // Using TreeSet for natural ordering
        String existingContent = "";

        // Try to read existing content
        try {
            FileObject inputFile = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", path);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputFile.openInputStream()))) {
                String line;
                StringBuilder contentBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().startsWith("#")) {
                        existingClasses.add(line.trim());
                    }
                    contentBuilder.append(line).append("\n");
                }
                existingContent = contentBuilder.toString();
            }
        } catch (IOException e) {
            logMessage(Diagnostic.Kind.NOTE, "Unable to read existing file. Proceeding with empty content.");
        }

        Set<String> allClasses = new TreeSet<>(existingClasses); // Using TreeSet for natural ordering
        allClasses.addAll(processedClasses);

        StringBuilder newContentBuilder = new StringBuilder();
        for (String className : allClasses) {
            newContentBuilder.append(className).append("\n");
        }
        String newContent = newContentBuilder.toString();

        if (!newContent.equals(existingContent)) {
            logMessage(Diagnostic.Kind.NOTE, "Content has changed. Updating file.");
            try {
                FileObject outputFile =
                        processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", path);
                try (Writer writer = outputFile.openWriter()) {
                    writer.write(newContent);
                }
            } catch (IOException e) {
                logError("Failed to write to file", e);
                throw e; // Re-throw to ensure the compilation fails
            }
        } else {
            logMessage(Diagnostic.Kind.NOTE, "Content unchanged. Skipping file update.");
        }
    }

    private void logMessage(Diagnostic.Kind kind, String message) {
        processingEnv.getMessager().printMessage(kind, message);
    }

    private void logError(String message, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();

        String fullMessage = message + "\n" + "Exception: "
                + e.getClass().getName() + "\n" + "Message: "
                + e.getMessage() + "\n" + "Stack trace:\n"
                + stackTrace;

        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, fullMessage);
    }
}
