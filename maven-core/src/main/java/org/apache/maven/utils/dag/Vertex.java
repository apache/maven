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
package org.apache.maven.utils.dag;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
public class Vertex implements Cloneable, Serializable {
    // ------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------
    private String label = null;

    List<Vertex> children = new ArrayList<>();

    List<Vertex> parents = new ArrayList<>();

    // ------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------

    public Vertex(final String label) {
        this.label = label;
    }

    // ------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------

    public String getLabel() {
        return label;
    }

    public void addEdgeTo(final Vertex vertex) {
        children.add(vertex);
    }

    public void removeEdgeTo(final Vertex vertex) {
        children.remove(vertex);
    }

    public void addEdgeFrom(final Vertex vertex) {
        parents.add(vertex);
    }

    public void removeEdgeFrom(final Vertex vertex) {
        parents.remove(vertex);
    }

    public List<Vertex> getChildren() {
        return children;
    }

    /**
     * Get the labels used by the most direct children.
     *
     * @return the labels used by the most direct children
     */
    public List<String> getChildLabels() {
        final List<String> retValue = new ArrayList<>(children.size());

        for (Vertex vertex : children) {
            retValue.add(vertex.getLabel());
        }
        return retValue;
    }

    /**
     * Get the list the most direct ancestors (parents).
     *
     * @return list of parents
     */
    public List<Vertex> getParents() {
        return parents;
    }

    /**
     * Get the labels used by the most direct ancestors (parents).
     *
     * @return the labels used parents
     */
    public List<String> getParentLabels() {
        final List<String> retValue = new ArrayList<>(parents.size());

        for (Vertex vertex : parents) {
            retValue.add(vertex.getLabel());
        }
        return retValue;
    }

    /**
     * Indicates if given vertex has no child
     *
     * @return <code>true</code> if this vertex has no child, <code>false</code> otherwise
     */
    public boolean isLeaf() {
        return children.size() == 0;
    }

    /**
     * Indicates if given vertex has no parent
     *
     * @return <code>true</code> if this vertex has no parent, <code>false</code> otherwise
     */
    public boolean isRoot() {
        return parents.size() == 0;
    }

    /**
     * Indicates if there is at least one edee leading to or from given vertex
     *
     * @return <code>true</code> if this vertex is connected with other vertex,<code>false</code> otherwise
     */
    public boolean isConnected() {
        return isRoot() || isLeaf();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        // this is what's failing..
        final Object retValue = super.clone();

        return retValue;
    }

    @Override
    public String toString() {
        return "Vertex{" + "label='" + label + "'" + "}";
    }
}
