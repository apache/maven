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
package org.apache.maven.api.services.model;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;

/**
 * ModelProcessor
 */
public interface ModelProcessor {

    /**
     * Returns the file containing the pom to be parsed or null if a pom can not be found
     * at the given file or in the given directory.
     */
    @Nullable
    Path locateExistingPom(@Nonnull Path project);

    /**
     * Reads the model from the specified byte stream. The stream will be automatically closed before the method
     * returns.
     *
     * @param request The reader request to deserialize the model, must not be {@code null}.
     * @return The deserialized model, never {@code null}.
     * @throws IOException If the model could not be deserialized.
     * @throws XmlReaderException If the input format could not be parsed.
     */
    Model read(XmlReaderRequest request) throws IOException, XmlReaderException;
}
