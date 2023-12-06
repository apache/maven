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

/**
 * @deprecated Use MavenStaxWriter instead
 */
@Deprecated
public class MavenXpp3WriterEx extends MavenXpp3Writer {

    // -----------/
    // - Methods -/
    // -----------/

    public MavenXpp3WriterEx() {
        super(true);
    }

    /**
     * Method setFileComment.
     *
     * @param fileComment a fileComment object.
     */
    public void setFileComment(String fileComment) {
        super.setFileComment(fileComment);
    } // -- void setFileComment( String )

    /**
     * Method setStringFormatter.
     *
     * @param stringFormatter
     */
    public void setStringFormatter(InputLocation.StringFormatter stringFormatter) {
        super.setStringFormatter(stringFormatter);
    } // -- void setStringFormatter( InputLocation.StringFormatter )

    /**
     * Method write.
     *
     * @param writer a writer object.
     * @param model a model object.
     * @throws IOException java.io.IOException if any.
     */
    public void write(Writer writer, Model model) throws IOException {
        super.write(writer, model);
    } // -- void write( Writer, Model )

    /**
     * Method write.
     *
     * @param stream a stream object.
     * @param model a model object.
     * @throws IOException java.io.IOException if any.
     */
    public void write(OutputStream stream, Model model) throws IOException {
        super.write(stream, model);
    } // -- void write( OutputStream, Model )
}
