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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.jxpath.ri.QName;
import org.apache.commons.jxpath.ri.model.NodeIterator;
import org.apache.commons.jxpath.ri.model.NodePointer;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * An attribute iterator for JXPath to support <code>Xpp3Dom</code>.
 *
 * @author Benjamin Bentmann
 */
class Xpp3DomAttributeIterator implements NodeIterator {

    private NodePointer parent;

    private Xpp3Dom node;

    private List<Map.Entry<String, String>> attributes;

    private Map.Entry<String, String> attribute;

    private int position;

    public Xpp3DomAttributeIterator(NodePointer parent, QName qname) {
        this.parent = parent;
        this.node = (Xpp3Dom) parent.getNode();

        Map<String, String> map = new LinkedHashMap<>();
        for (String name : this.node.getAttributeNames()) {
            if (name.equals(qname.getName()) || "*".equals(qname.getName())) {
                String value = this.node.getAttribute(name);
                map.put(name, value);
            }
        }
        this.attributes = new ArrayList<>(map.entrySet());
    }

    public NodePointer getNodePointer() {
        if (position == 0) {
            setPosition(1);
        }
        return (attribute == null) ? null : new Xpp3DomAttributePointer(parent, attribute);
    }

    public int getPosition() {
        return position;
    }

    public boolean setPosition(int position) {
        this.position = position;
        attribute = (position > 0 && position <= attributes.size()) ? attributes.get(position - 1) : null;
        return attribute != null;
    }
}
