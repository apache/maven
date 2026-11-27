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
package org.apache.maven.its.gh13911;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * Annotation processor for the GH-13911 integration test.
 * <p>
 * When it finds any type annotated with {@link Gh13911Generated}, it writes a resource file
 * {@code META-INF/generated-by-gh13911.txt} to the class output directory.  The client module's
 * IT verifier checks for this file to confirm the processor ran successfully from the same reactor.
 */
@SupportedAnnotationTypes("org.apache.maven.its.gh13911.Gh13911Generated")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class Gh13911Processor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty() || roundEnv.processingOver()) {
            return false;
        }
        try {
            FileObject resource = processingEnv
                    .getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/generated-by-gh13911.txt");
            try (Writer writer = resource.openWriter()) {
                writer.write("[GH-13911] annotation processor ran successfully\n");
            }
        } catch (IOException e) {
            processingEnv
                    .getMessager()
                    .printMessage(Diagnostic.Kind.ERROR, "[GH-13911] failed to write marker: " + e.getMessage());
        }
        return true;
    }
}
