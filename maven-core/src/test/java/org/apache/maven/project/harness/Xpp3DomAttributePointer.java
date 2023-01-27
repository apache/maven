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

import java.util.Map;

import org.apache.commons.jxpath.ri.QName;
import org.apache.commons.jxpath.ri.model.NodePointer;

/**
 * An attribute pointer for JXPath to support <code>Xpp3Dom</code>.
 *
 * @author Benjamin Bentmann
 */
class Xpp3DomAttributePointer extends NodePointer {

    private Map.Entry<String, String> attrib;

    public Xpp3DomAttributePointer(NodePointer parent, Map.Entry<String, String> attrib) {
        super(parent);
        this.attrib = attrib;
    }

    @Override
    public int compareChildNodePointers(NodePointer pointer1, NodePointer pointer2) {
        // should never happen because attributes have no children
        return 0;
    }

    @Override
    public Object getValue() {
        return attrib.getValue();
    }

    @Override
    public Object getBaseValue() {
        return attrib;
    }

    @Override
    public Object getImmediateNode() {
        return attrib;
    }

    @Override
    public int getLength() {
        return 1;
    }

    @Override
    public QName getName() {
        return new QName(null, attrib.getKey());
    }

    @Override
    public boolean isActual() {
        return true;
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public void setValue(Object value) {
        throw new UnsupportedOperationException();
    }
}
