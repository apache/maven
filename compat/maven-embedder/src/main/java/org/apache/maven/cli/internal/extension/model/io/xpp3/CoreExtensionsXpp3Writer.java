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
package org.apache.maven.cli.internal.extension.model.io.xpp3;

// ---------------------------------/
// - Imported classes and packages -/
// ---------------------------------/

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Iterator;

import org.apache.maven.cli.internal.extension.model.CoreExtension;
import org.apache.maven.cli.internal.extension.model.CoreExtensions;
import org.codehaus.plexus.util.xml.pull.MXSerializer;
import org.codehaus.plexus.util.xml.pull.XmlSerializer;

/**
 * Class CoreExtensionsXpp3Writer.
 *
 * @deprecated use {@code org.apache.maven.cling.internal.extension.io.CoreExtensionsStaxWriter}
 */
@Deprecated
@SuppressWarnings("all")
public class CoreExtensionsXpp3Writer {

    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    /**
     * Field NAMESPACE.
     */
    private static final String NAMESPACE = null;

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
     * @param coreExtensions a coreExtensions object.
     * @throws IOException IOException if any.
     */
    public void write(Writer writer, CoreExtensions coreExtensions) throws IOException {
        XmlSerializer serializer = new MXSerializer();
        serializer.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-indentation", "  ");
        serializer.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-line-separator", "\n");
        serializer.setOutput(writer);
        serializer.startDocument(coreExtensions.getModelEncoding(), null);
        writeCoreExtensions(coreExtensions, "extensions", serializer);
        serializer.endDocument();
    } // -- void write( Writer, CoreExtensions )

    /**
     * Method write.
     *
     * @param stream a stream object.
     * @param coreExtensions a coreExtensions object.
     * @throws IOException IOException if any.
     */
    public void write(OutputStream stream, CoreExtensions coreExtensions) throws IOException {
        XmlSerializer serializer = new MXSerializer();
        serializer.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-indentation", "  ");
        serializer.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-line-separator", "\n");
        serializer.setOutput(stream, coreExtensions.getModelEncoding());
        serializer.startDocument(coreExtensions.getModelEncoding(), null);
        writeCoreExtensions(coreExtensions, "extensions", serializer);
        serializer.endDocument();
    } // -- void write( OutputStream, CoreExtensions )

    /**
     * Method writeCoreExtension.
     *
     * @param coreExtension a coreExtension object.
     * @param serializer a serializer object.
     * @param tagName a tagName object.
     * @throws IOException IOException if any.
     */
    private void writeCoreExtension(CoreExtension coreExtension, String tagName, XmlSerializer serializer)
            throws IOException {
        serializer.startTag(NAMESPACE, tagName);
        if (coreExtension.getGroupId() != null) {
            serializer
                    .startTag(NAMESPACE, "groupId")
                    .text(coreExtension.getGroupId())
                    .endTag(NAMESPACE, "groupId");
        }
        if (coreExtension.getArtifactId() != null) {
            serializer
                    .startTag(NAMESPACE, "artifactId")
                    .text(coreExtension.getArtifactId())
                    .endTag(NAMESPACE, "artifactId");
        }
        if (coreExtension.getVersion() != null) {
            serializer
                    .startTag(NAMESPACE, "version")
                    .text(coreExtension.getVersion())
                    .endTag(NAMESPACE, "version");
        }
        if ((coreExtension.getClassLoadingStrategy() != null)
                && !coreExtension.getClassLoadingStrategy().equals("self-first")) {
            serializer
                    .startTag(NAMESPACE, "classLoadingStrategy")
                    .text(coreExtension.getClassLoadingStrategy())
                    .endTag(NAMESPACE, "classLoadingStrategy");
        }
        serializer.endTag(NAMESPACE, tagName);
    } // -- void writeCoreExtension( CoreExtension, String, XmlSerializer )

    /**
     * Method writeCoreExtensions.
     *
     * @param coreExtensions a coreExtensions object.
     * @param serializer a serializer object.
     * @param tagName a tagName object.
     * @throws IOException IOException if any.
     */
    private void writeCoreExtensions(CoreExtensions coreExtensions, String tagName, XmlSerializer serializer)
            throws IOException {
        if (this.fileComment != null) {
            serializer.comment(this.fileComment);
        }
        serializer.setPrefix("", "http://maven.apache.org/EXTENSIONS/1.1.0");
        serializer.setPrefix("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        serializer.startTag(NAMESPACE, tagName);
        serializer.attribute(
                "",
                "xsi:schemaLocation",
                "http://maven.apache.org/EXTENSIONS/1.1.0 https://maven.apache.org/xsd/core-extensions-1.1.0.xsd");
        if ((coreExtensions.getExtensions() != null)
                && (coreExtensions.getExtensions().size() > 0)) {
            for (Iterator iter = coreExtensions.getExtensions().iterator(); iter.hasNext(); ) {
                CoreExtension o = (CoreExtension) iter.next();
                writeCoreExtension(o, "extension", serializer);
            }
        }
        serializer.endTag(NAMESPACE, tagName);
    } // -- void writeCoreExtensions( CoreExtensions, String, XmlSerializer )
}
