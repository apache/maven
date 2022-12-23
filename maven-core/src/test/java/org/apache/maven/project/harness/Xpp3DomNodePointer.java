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
import java.util.List;

import org.apache.commons.jxpath.ri.QName;
import org.apache.commons.jxpath.ri.compiler.NodeTest;
import org.apache.commons.jxpath.ri.model.NodeIterator;
import org.apache.commons.jxpath.ri.model.NodePointer;
import org.apache.maven.api.xml.Dom;

/**
 * A node pointer for JXPath to support <code>Xpp3Dom</code>.
 *
 * @author Benjamin Bentmann
 */
class Xpp3DomNodePointer extends NodePointer {

    private Dom node;

    public Xpp3DomNodePointer(Dom node) {
        super(null);
        this.node = node;
    }

    public Xpp3DomNodePointer(NodePointer parent, Dom node) {
        super(parent);
        this.node = node;
    }

    @Override
    public int compareChildNodePointers(NodePointer pointer1, NodePointer pointer2) {
        Dom node1 = (Dom) pointer1.getBaseValue();
        Dom node2 = (Dom) pointer2.getBaseValue();
        if (node1 == node2) {
            return 0;
        }
        for (Dom child : node.getChildren()) {
            if (child == node1) {
                return -1;
            }
            if (child == node2) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public Object getValue() {
        return getValue(node);
    }

    private static Object getValue(Dom node) {
        if (node.getValue() != null) {
            return node.getValue();
        } else {
            List<Object> children = new ArrayList<>();
            for (Dom child : node.getChildren()) {
                children.add(getValue(child));
            }
            return children;
        }
    }

    @Override
    public Object getBaseValue() {
        return node;
    }

    @Override
    public Object getImmediateNode() {
        return node;
    }

    @Override
    public int getLength() {
        return 1;
    }

    @Override
    public QName getName() {
        return new QName(null, node.getName());
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public boolean isLeaf() {
        return node.getChildren().isEmpty();
    }

    @Override
    public void setValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator childIterator(NodeTest test, boolean reverse, NodePointer startWith) {
        return new Xpp3DomNodeIterator(this, test, reverse, startWith);
    }

    @Override
    public NodeIterator attributeIterator(QName qname) {
        return new Xpp3DomAttributeIterator(this, qname);
    }
}
