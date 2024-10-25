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
package org.apache.maven.model.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;

import org.apache.maven.api.model.Model;

/**
 * Handles serialization of a model into some kind of textual format like XML.
 *
 * @deprecated use {@link org.apache.maven.api.services.xml.ModelXmlFactory} instead
 */
@Deprecated(since = "4.0.0")
public interface ModelWriter {

    /**
     * Writes the supplied model to the specified file. Any non-existing parent directories of the output file will be
     * created automatically.
     *
     * @param output The file to serialize the model to, must not be {@code null}.
     * @param options The options to use for serialization, may be {@code null} to use the default values.
     * @param model The model to serialize, must not be {@code null}.
     * @throws IOException If the model could not be serialized.
     */
    void write(File output, Map<String, Object> options, Model model) throws IOException;

    /**
     * Writes the supplied model to the specified character writer. The writer will be automatically closed before the
     * method returns.
     *
     * @param output The writer to serialize the model to, must not be {@code null}.
     * @param options The options to use for serialization, may be {@code null} to use the default values.
     * @param model The model to serialize, must not be {@code null}.
     * @throws IOException If the model could not be serialized.
     */
    void write(Writer output, Map<String, Object> options, Model model) throws IOException;

    /**
     * Writes the supplied model to the specified byte stream. The stream will be automatically closed before the method
     * returns.
     *
     * @param output The stream to serialize the model to, must not be {@code null}.
     * @param options The options to use for serialization, may be {@code null} to use the default values.
     * @param model The model to serialize, must not be {@code null}.
     * @throws IOException If the model could not be serialized.
     */
    void write(OutputStream output, Map<String, Object> options, Model model) throws IOException;

    /**
     * Writes the supplied model to the specified file. Any non-existing parent directories of the output file will be
     * created automatically.
     *
     * @param output The file to serialize the model to, must not be {@code null}.
     * @param options The options to use for serialization, may be {@code null} to use the default values.
     * @param model The model to serialize, must not be {@code null}.
     * @throws IOException If the model could not be serialized.
     */
    void write(File output, Map<String, Object> options, org.apache.maven.model.Model model) throws IOException;

    /**
     * Writes the supplied model to the specified character writer. The writer will be automatically closed before the
     * method returns.
     *
     * @param output The writer to serialize the model to, must not be {@code null}.
     * @param options The options to use for serialization, may be {@code null} to use the default values.
     * @param model The model to serialize, must not be {@code null}.
     * @throws IOException If the model could not be serialized.
     */
    void write(Writer output, Map<String, Object> options, org.apache.maven.model.Model model) throws IOException;

    /**
     * Writes the supplied model to the specified byte stream. The stream will be automatically closed before the method
     * returns.
     *
     * @param output The stream to serialize the model to, must not be {@code null}.
     * @param options The options to use for serialization, may be {@code null} to use the default values.
     * @param model The model to serialize, must not be {@code null}.
     * @throws IOException If the model could not be serialized.
     */
    void write(OutputStream output, Map<String, Object> options, org.apache.maven.model.Model model) throws IOException;
}
