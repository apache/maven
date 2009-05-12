package org.apache.maven.model.io;

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import org.apache.maven.model.Model;

/**
 * Handles deserialization of a model from some kind of textual format like XML.
 * 
 * @author Benjamin Bentmann
 */
public interface ModelReader
{

    /**
     * The key for the option to enable strict parsing. This option is of type {@link Boolean} and defaults to {@code
     * true}. If {@code false}, unknown elements will be ignored instead of causing a failure.
     */
    static final String IS_STRICT = "org.apache.maven.model.io.isStrict";

    /**
     * Reads the model from the specified file.
     * 
     * @param input The file to deserialize the model from, must not be {@code null}.
     * @param options The options to use for deserialization, may be {@code null} to use the default values.
     * @return The deserialized model, never {@code null}.
     * @throws IOException If the model could not be deserialized.
     */
    Model read( File input, Map<String, Object> options )
        throws IOException;

    /**
     * Reads the model from the specified character reader.
     * 
     * @param input The reader to deserialize the model from, must not be {@code null}.
     * @param options The options to use for deserialization, may be {@code null} to use the default values.
     * @return The deserialized model, never {@code null}.
     * @throws IOException If the model could not be deserialized.
     */
    Model read( Reader input, Map<String, Object> options )
        throws IOException;

    /**
     * Reads the model from the specified byte stream.
     * 
     * @param input The stream to deserialize the model from, must not be {@code null}.
     * @param options The options to use for deserialization, may be {@code null} to use the default values.
     * @return The deserialized model, never {@code null}.
     * @throws IOException If the model could not be deserialized.
     */
    Model read( InputStream input, Map<String, Object> options )
        throws IOException;

}
