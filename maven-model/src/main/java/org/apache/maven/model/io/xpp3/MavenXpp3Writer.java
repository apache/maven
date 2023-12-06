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
package org.apache.maven.model.io.xpp3;

import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.v4.MavenStaxWriter;

/**
 * @deprecated Use MavenStaxWriter instead
 */
@Deprecated
public class MavenXpp3Writer {
    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    private final MavenStaxWriter delegate = new MavenStaxWriter();

    // -----------/
    // - Methods -/
    // -----------/

    public MavenXpp3Writer() {
        this(false);
    }

    protected MavenXpp3Writer(boolean addLocationInformation) {
        delegate.setAddLocationInformation(addLocationInformation);
    }

    /**
     * Method setFileComment.
     *
     * @param fileComment a fileComment object.
     */
    public void setFileComment(String fileComment) {
        delegate.setFileComment(fileComment);
    } // -- void setFileComment( String )

    /**
     * Method setStringFormatter.
     *
     * @param stringFormatter
     */
    public void setStringFormatter(InputLocation.StringFormatter stringFormatter) {
        delegate.setStringFormatter(
                stringFormatter != null
                        ? new org.apache.maven.api.model.InputLocation.StringFormatter() {
                            @Override
                            public String toString(org.apache.maven.api.model.InputLocation location) {
                                return stringFormatter.toString(new InputLocation(location));
                            }
                        }
                        : null);
    } // -- void setStringFormatter( InputLocation.StringFormatter )

    /**
     * Method write.
     *
     * @param writer a writer object.
     * @param model a model object.
     * @throws IOException java.io.IOException if any.
     */
    public void write(Writer writer, Model model) throws IOException {
        try {
            delegate.write(writer, model.getDelegate());
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    } // -- void write( Writer, Model )

    /**
     * Method write.
     *
     * @param stream a stream object.
     * @param model a model object.
     * @throws IOException java.io.IOException if any.
     */
    public void write(OutputStream stream, Model model) throws IOException {
        try {
            delegate.write(stream, model.getDelegate());
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    } // -- void write( OutputStream, Model )
}
