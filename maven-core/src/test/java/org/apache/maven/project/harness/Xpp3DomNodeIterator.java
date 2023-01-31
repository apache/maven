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
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * A node iterator for JXPath to support <code>Xpp3Dom</code>.
 *
 * @author Benjamin Bentmann
 */
class Xpp3DomNodeIterator implements NodeIterator {

    private NodePointer parent;

    private NodeTest test;

    private Xpp3Dom node;

    private Xpp3Dom[] children;

    private List<Xpp3Dom> filteredChildren = new ArrayList<>();

    private int filteredIndex;

    private Xpp3Dom child;

    private int position;

    public Xpp3DomNodeIterator(NodePointer parent, NodeTest test, boolean reverse, NodePointer startWith) {
        this.parent = parent;
        this.node = (Xpp3Dom) parent.getNode();
        this.children = this.node.getChildren();
        if (startWith != null) {
            Xpp3Dom startWithNode = (Xpp3Dom) startWith.getNode();
            for (; filteredIndex < children.length; filteredIndex++) {
                if (startWithNode.equals(children[filteredIndex])) {
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
        for (; position > filteredChildren.size() && filteredIndex < children.length; filteredIndex++) {
            Xpp3Dom child = children[filteredIndex];
            if (testNode(child)) {
                filteredChildren.add(child);
            }
        }
    }

    private boolean testNode(Xpp3Dom node) {
        if (test == null) {
            return true;
        }
        if (test instanceof NodeNameTest) {
            String nodeName = node.getName();
            if (StringUtils.isEmpty(nodeName)) {
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
                return StringUtils.isEmpty(namespaceURI) || StringUtils.isEmpty(testPrefix);
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
