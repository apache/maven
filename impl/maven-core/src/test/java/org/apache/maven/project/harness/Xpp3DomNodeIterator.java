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

import org.apache.commons.jxpath.ri.Compiler;
import org.apache.commons.jxpath.ri.compiler.NodeNameTest;
import org.apache.commons.jxpath.ri.compiler.NodeTest;
import org.apache.commons.jxpath.ri.compiler.NodeTypeTest;
import org.apache.commons.jxpath.ri.model.NodeIterator;
import org.apache.commons.jxpath.ri.model.NodePointer;
import org.apache.maven.api.xml.XmlNode;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * A node iterator for JXPath to support <code>Xpp3Dom</code>.
 *
 */
class Xpp3DomNodeIterator implements NodeIterator {

    private NodePointer parent;

    private NodeTest test;

    private XmlNode node;

    private List<XmlNode> children;

    private List<XmlNode> filteredChildren = new ArrayList<>();

    private int filteredIndex;

    private XmlNode child;

    private int position;

    public Xpp3DomNodeIterator(NodePointer parent, NodeTest test, boolean reverse, NodePointer startWith) {
        this.parent = parent;
        this.node = (XmlNode) parent.getNode();
        this.children = this.node.getChildren();
        if (startWith != null) {
            Xpp3Dom startWithNode = (Xpp3Dom) startWith.getNode();
            for (; filteredIndex < children.size(); filteredIndex++) {
                if (startWithNode.equals(children.get(filteredIndex))) {
                    filteredIndex++;
                    break;
                }
            }
        }
        this.test = test;
        if (reverse) {
            throw new UnsupportedOperationException();
        }
    }

    public NodePointer getNodePointer() {
        if (position == 0) {
            setPosition(1);
        }
        return (child == null) ? null : new Xpp3DomNodePointer(parent, child);
    }

    public int getPosition() {
        return position;
    }

    public boolean setPosition(int position) {
        this.position = position;
        filterChildren(position);
        child = (position > 0 && position <= filteredChildren.size()) ? filteredChildren.get(position - 1) : null;
        return child != null;
    }

    private void filterChildren(int position) {
        for (; position > filteredChildren.size() && filteredIndex < children.size(); filteredIndex++) {
            XmlNode child = children.get(filteredIndex);
            if (testNode(child)) {
                filteredChildren.add(child);
            }
        }
    }

    private boolean testNode(XmlNode node) {
        if (test == null) {
            return true;
        }
        if (test instanceof NodeNameTest) {
            String nodeName = node.getName();
            if (nodeName == null || nodeName.isEmpty()) {
                return false;
            }

            NodeNameTest nodeNameTest = (NodeNameTest) test;
            String namespaceURI = nodeNameTest.getNamespaceURI();
            boolean wildcard = nodeNameTest.isWildcard();
            String testName = nodeNameTest.getNodeName().getName();
            String testPrefix = nodeNameTest.getNodeName().getPrefix();
            if (wildcard && testPrefix == null) {
                return true;
            }
            if (wildcard || testName.equals(nodeName)) {
                return (namespaceURI == null || namespaceURI.isEmpty()) || (testPrefix == null || testPrefix.isEmpty());
            }
            return false;
        }
        if (test instanceof NodeTypeTest) {
            switch (((NodeTypeTest) test).getNodeType()) {
                case Compiler.NODE_TYPE_NODE:
                    return true;
                case Compiler.NODE_TYPE_TEXT:
                    return node.getValue() != null;
                default:
                    return false;
            }
        }
        return false;
    }
}
