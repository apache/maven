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
package org.apache.maven.settings.io.xpp3;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import org.apache.maven.settings.Settings;

public class SettingsXpp3Writer {
    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    /**
     * Field fileComment.
     */
    private String fileComment = null;

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
     * Method write.
     *
     * @param writer a writer object.
     * @param settings a settings object.
     * @throws IOException java.io.IOException if any.
     */
    public void write(Writer writer, Settings settings) throws IOException {
        org.apache.maven.settings.v4.SettingsXpp3Writer xw = new org.apache.maven.settings.v4.SettingsXpp3Writer();
        xw.setFileComment(fileComment);
        xw.write(writer, settings.getDelegate());
    } // -- void write( Writer, Model )

    /**
     * Method write.
     *
     * @param stream a stream object.
     * @param settings a settings object.
     * @throws IOException java.io.IOException if any.
     */
    public void write(OutputStream stream, Settings settings) throws IOException {
        org.apache.maven.settings.v4.SettingsXpp3Writer xw = new org.apache.maven.settings.v4.SettingsXpp3Writer();
        xw.setFileComment(fileComment);
        xw.write(stream, settings.getDelegate());
    } // -- void write( OutputStream, Model )
}
