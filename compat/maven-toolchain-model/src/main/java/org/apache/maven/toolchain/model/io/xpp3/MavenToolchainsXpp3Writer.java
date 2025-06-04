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
package org.apache.maven.toolchain.model.io.xpp3;

import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.v4.MavenToolchainsStaxWriter;

/**
 *
 * @deprecated use MavenToolchainsStaxWriter.
 */
@Deprecated(since = "4.0.0")
public class MavenToolchainsXpp3Writer {
    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    private final MavenToolchainsStaxWriter delegate;

    public MavenToolchainsXpp3Writer() {
        delegate = new MavenToolchainsStaxWriter();
        delegate.setAddLocationInformation(false);
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
     * @param writer a writer object.
     * @param persistedToolchains a persistedToolchains object.
     * @throws IOException java.io.IOException if any.
     */
    public void write(Writer writer, PersistedToolchains persistedToolchains) throws IOException {
        try {
            delegate.write(writer, persistedToolchains.getDelegate());
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    /**
     * Method write.
     *
     * @param stream a stream object.
     * @param persistedToolchains a persistedToolchains object.
     * @throws IOException java.io.IOException if any.
     */
    public void write(OutputStream stream, PersistedToolchains persistedToolchains) throws IOException {
        try {
            delegate.write(stream, persistedToolchains.getDelegate());
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }
}
