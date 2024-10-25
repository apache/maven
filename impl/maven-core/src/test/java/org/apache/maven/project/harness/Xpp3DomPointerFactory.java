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
package org.apache.maven.project.harness;

import java.util.Locale;

import org.apache.commons.jxpath.ri.QName;
import org.apache.commons.jxpath.ri.model.NodePointer;
import org.apache.commons.jxpath.ri.model.NodePointerFactory;
import org.apache.maven.api.xml.XmlNode;

/**
 * A node pointer factory for JXPath to support <code>Xpp3Dom</code>.
 *
 */
public class Xpp3DomPointerFactory implements NodePointerFactory {

    public int getOrder() {
        return 200;
    }

    public NodePointer createNodePointer(QName name, Object object, Locale locale) {
        if (object instanceof org.codehaus.plexus.util.xml.Xpp3Dom) {
            object = ((org.codehaus.plexus.util.xml.Xpp3Dom) object).getDom();
        }
        if (object instanceof XmlNode) {
            return new Xpp3DomNodePointer((XmlNode) object);
        }
        return null;
    }

    public NodePointer createNodePointer(NodePointer parent, QName name, Object object) {
        if (object instanceof org.codehaus.plexus.util.xml.Xpp3Dom) {
            object = ((org.codehaus.plexus.util.xml.Xpp3Dom) object).getDom();
        }
        if (object instanceof XmlNode) {
            return new Xpp3DomNodePointer(parent, (XmlNode) object);
        }
        return null;
    }
}
