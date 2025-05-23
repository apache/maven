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
package org.apache.maven.api.services.xml;

import org.apache.maven.api.annotations.Nullable;

public interface Location {

    /**
     * Return the line number where the current event ends,
     * returns -1 if none is available.
     * @return the current line number
     */
    int getLineNumber();

    /**
     * Return the column number where the current event ends,
     * returns -1 if none is available.
     * @return the current column number
     */
    int getColumnNumber();

    /**
     * Return the byte or character offset into the input source this location
     * is pointing to. If the input source is a file or a byte stream then
     * this is the byte offset into that stream, but if the input source is
     * a character media then the offset is the character offset.
     * Returns -1 if there is no offset available.
     * @return the current offset
     */
    int getCharacterOffset();

    /**
     * Returns the public ID of the XML
     * @return the public ID, or null if not available
     */
    @Nullable
    String getPublicId();

    /**
     * Returns the system ID of the XML
     * @return the system ID, or null if not available
     */
    @Nullable
    String getSystemId();
}
