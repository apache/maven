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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;

public class MavenXpp3WriterEx {
    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    /**
     * Field fileComment.
     */
    private String fileComment = null;

    /**
     * Field stringFormatter.
     */
    protected InputLocation.StringFormatter stringFormatter;

    // -----------/
    // - Methods -/
    // -----------/

    /**
     * Method setFileComment.
     *
     * @param fileComment a fileComment object.
     */
    public void setFileComment(String fileComment) {
        this.fileComment = fileComment;
    } // -- void setFileComment( String )

    /**
     * Method setStringFormatter.
     *
     * @param stringFormatter
     */
    public void setStringFormatter(InputLocation.StringFormatter stringFormatter) {
        this.stringFormatter = stringFormatter;
    } // -- void setStringFormatter( InputLocation.StringFormatter )

    /**
     * Method write.
     *
     * @param writer a writer object.
     * @param model a model object.
     * @throws IOException java.io.IOException if any.
     */
    public void write(Writer writer, Model model) throws IOException {
        org.apache.maven.model.v4.MavenXpp3WriterEx xw = new org.apache.maven.model.v4.MavenXpp3WriterEx();
        xw.setFileComment(fileComment);
        xw.setStringFormatter(
                stringFormatter != null
                        ? new org.apache.maven.api.model.InputLocation.StringFormatter() {
                            @Override
                            public String toString(org.apache.maven.api.model.InputLocation location) {
                                return stringFormatter.toString(new InputLocation(location));
                            }
                        }
                        : null);
        xw.write(writer, model.getDelegate());
    } // -- void write( Writer, Model )

    /**
     * Method write.
     *
     * @param stream a stream object.
     * @param model a model object.
     * @throws IOException java.io.IOException if any.
     */
    public void write(OutputStream stream, Model model) throws IOException {
        org.apache.maven.model.v4.MavenXpp3WriterEx xw = new org.apache.maven.model.v4.MavenXpp3WriterEx();
        xw.setFileComment(fileComment);
        xw.write(stream, model.getDelegate());
    } // -- void write( OutputStream, Model )
}
