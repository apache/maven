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
package org.apache.maven.artifact.repository.metadata.io.xpp3;

import java.io.OutputStream;
import java.io.Writer;

import org.apache.maven.artifact.repository.metadata.Metadata;

/**
 * Provide public methods from {@link org.apache.maven.artifact.repository.metadata.io.MetadataXpp3Writer}
 *
 * @deprecated Maven 3 compatability
 */
@Deprecated
public class MetadataXpp3Writer {

    private final org.apache.maven.artifact.repository.metadata.io.MetadataXpp3Writer delegate;

    /**
     * Default constructor
     */
    public MetadataXpp3Writer() {
        delegate = new org.apache.maven.artifact.repository.metadata.io.MetadataXpp3Writer();
    }

    /**
     * Method setFileComment.
     *
     * @param fileComment a fileComment object.
     */
    public void setFileComment(String fileComment) {
        delegate.setFileComment(fileComment);
    }

    /**
     * Method write.
     *
     * @param writer   a writer object
     * @param metadata a Metadata object
     * @throws java.io.IOException java.io.IOException if any
     */
    public void write(Writer writer, Metadata metadata) throws java.io.IOException {
        delegate.write(writer, metadata.getDelegate());
    }

    /**
     * Method write.
     *
     * @param stream a stream object
     * @param metadata a Metadata object
     * @throws java.io.IOException java.io.IOException if any
     */
    public void write(OutputStream stream, Metadata metadata) throws java.io.IOException {
        delegate.write(stream, metadata.getDelegate());
    }
}
