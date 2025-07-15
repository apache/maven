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
package org.apache.maven.internal.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.xml.XmlNode;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * A PlexusConfiguration implementation that wraps an XmlNode instead of copying its entire hierarchy.
 * This provides better performance by avoiding deep copying of the XML structure.
 *
 * <p>This implementation supports both read and write operations. When write operations are performed,
 * new XmlNode instances are created to maintain immutability, and internal caches are cleared.</p>
 */
public class XmlPlexusConfiguration implements PlexusConfiguration {
    private XmlNode xmlNode;
    private PlexusConfiguration[] childrenCache;

    public static PlexusConfiguration toPlexusConfiguration(XmlNode node) {
        return new XmlPlexusConfiguration(node);
    }

    public XmlPlexusConfiguration(XmlNode xmlNode) {
        this.xmlNode = xmlNode;
    }

    /**
     * Clears the internal cache when the XML structure is modified.
     */
    private synchronized void clearCache() {
        this.childrenCache = null;
    }

    /**
     * Converts a PlexusConfiguration to an XmlNode.
     */
    private XmlNode convertToXmlNode(PlexusConfiguration config) {
        // Convert attributes
        Map<String, String> attributes = new HashMap<>();
        for (String attrName : config.getAttributeNames()) {
            String attrValue = config.getAttribute(attrName);
            if (attrValue != null) {
                attributes.put(attrName, attrValue);
            }
        }

        // Convert children
        List<XmlNode> children = new ArrayList<>();
        for (PlexusConfiguration child : config.getChildren()) {
            children.add(convertToXmlNode(child));
        }

        return XmlNode.newInstance(config.getName(), config.getValue(), attributes, children, null);
    }

    @Override
    public String getName() {
        return xmlNode.name();
    }

    public synchronized void setName(String name) {
        this.xmlNode = XmlNode.newBuilder()
                .name(name)
                .value(xmlNode.value())
                .attributes(xmlNode.attributes())
                .children(xmlNode.children())
                .namespaceUri(xmlNode.namespaceUri())
                .prefix(xmlNode.prefix())
                .inputLocation(xmlNode.inputLocation())
                .build();
        clearCache();
    }

    public String getValue() {
        return xmlNode.value();
    }

    public String getValue(String defaultValue) {
        String value = xmlNode.value();
        return value != null ? value : defaultValue;
    }

    public synchronized void setValue(String value) {
        this.xmlNode = XmlNode.newBuilder()
                .name(xmlNode.name())
                .value(value)
                .attributes(xmlNode.attributes())
                .children(xmlNode.children())
                .namespaceUri(xmlNode.namespaceUri())
                .prefix(xmlNode.prefix())
                .inputLocation(xmlNode.inputLocation())
                .build();
        clearCache();
    }

    public PlexusConfiguration setValueAndGetSelf(String value) {
        setValue(value);
        return this;
    }

    public synchronized void setAttribute(String name, String value) {
        Map<String, String> newAttributes = new HashMap<>(xmlNode.attributes());
        if (value == null) {
            newAttributes.remove(name);
        } else {
            newAttributes.put(name, value);
        }
        this.xmlNode = XmlNode.newBuilder()
                .name(xmlNode.name())
                .value(xmlNode.value())
                .attributes(newAttributes)
                .children(xmlNode.children())
                .namespaceUri(xmlNode.namespaceUri())
                .prefix(xmlNode.prefix())
                .inputLocation(xmlNode.inputLocation())
                .build();
        clearCache();
    }

    public String[] getAttributeNames() {
        return xmlNode.attributes().keySet().toArray(new String[0]);
    }

    public String getAttribute(String paramName) {
        return xmlNode.attribute(paramName);
    }

    public String getAttribute(String name, String defaultValue) {
        String value = xmlNode.attribute(name);
        return value != null ? value : defaultValue;
    }

    public PlexusConfiguration getChild(String child) {
        XmlNode childNode = xmlNode.child(child);
        if (childNode != null) {
            return new XmlPlexusConfiguration(childNode);
        } else {
            // Return an empty configuration object to match DefaultPlexusConfiguration behavior
            XmlNode emptyNode = XmlNode.newInstance(child, null, null, null, null);
            return new XmlPlexusConfiguration(emptyNode);
        }
    }

    public PlexusConfiguration getChild(int i) {
        List<XmlNode> children = xmlNode.children();
        if (i >= 0 && i < children.size()) {
            return new XmlPlexusConfiguration(children.get(i));
        }
        return null;
    }

    public synchronized PlexusConfiguration getChild(String child, boolean createChild) {
        XmlNode childNode = xmlNode.child(child);
        if (childNode == null) {
            if (createChild) {
                // Create a new child node
                XmlNode newChild = XmlNode.newInstance(child);
                List<XmlNode> newChildren = new ArrayList<>(xmlNode.children());
                newChildren.add(newChild);

                this.xmlNode = XmlNode.newBuilder()
                        .name(xmlNode.name())
                        .value(xmlNode.value())
                        .attributes(xmlNode.attributes())
                        .children(newChildren)
                        .namespaceUri(xmlNode.namespaceUri())
                        .prefix(xmlNode.prefix())
                        .inputLocation(xmlNode.inputLocation())
                        .build();
                clearCache();

                return new XmlPlexusConfiguration(newChild);
            } else {
                return null; // Return null when child doesn't exist and createChild=false
            }
        }
        return new XmlPlexusConfiguration(childNode);
    }

    public synchronized PlexusConfiguration[] getChildren() {
        if (childrenCache == null) {
            List<XmlNode> children = xmlNode.children();
            childrenCache = new PlexusConfiguration[children.size()];
            for (int i = 0; i < children.size(); i++) {
                childrenCache[i] = new XmlPlexusConfiguration(children.get(i));
            }
        }
        return childrenCache.clone();
    }

    public PlexusConfiguration[] getChildren(String name) {
        List<PlexusConfiguration> result = new ArrayList<>();
        for (XmlNode child : xmlNode.children()) {
            if (name.equals(child.name())) {
                result.add(new XmlPlexusConfiguration(child));
            }
        }
        return result.toArray(new PlexusConfiguration[0]);
    }

    public synchronized void addChild(PlexusConfiguration configuration) {
        // Convert PlexusConfiguration to XmlNode
        XmlNode newChild = convertToXmlNode(configuration);
        List<XmlNode> newChildren = new ArrayList<>(xmlNode.children());
        newChildren.add(newChild);

        this.xmlNode = XmlNode.newBuilder()
                .name(xmlNode.name())
                .value(xmlNode.value())
                .attributes(xmlNode.attributes())
                .children(newChildren)
                .namespaceUri(xmlNode.namespaceUri())
                .prefix(xmlNode.prefix())
                .inputLocation(xmlNode.inputLocation())
                .build();
        clearCache();
    }

    public synchronized PlexusConfiguration addChild(String name) {
        XmlNode newChild = XmlNode.newInstance(name);
        List<XmlNode> newChildren = new ArrayList<>(xmlNode.children());
        newChildren.add(newChild);

        this.xmlNode = XmlNode.newBuilder()
                .name(xmlNode.name())
                .value(xmlNode.value())
                .attributes(xmlNode.attributes())
                .children(newChildren)
                .namespaceUri(xmlNode.namespaceUri())
                .prefix(xmlNode.prefix())
                .inputLocation(xmlNode.inputLocation())
                .build();
        clearCache();

        return new XmlPlexusConfiguration(newChild);
    }

    public synchronized PlexusConfiguration addChild(String name, String value) {
        XmlNode newChild = XmlNode.newInstance(name, value);
        List<XmlNode> newChildren = new ArrayList<>(xmlNode.children());
        newChildren.add(newChild);

        this.xmlNode = XmlNode.newBuilder()
                .name(xmlNode.name())
                .value(xmlNode.value())
                .attributes(xmlNode.attributes())
                .children(newChildren)
                .namespaceUri(xmlNode.namespaceUri())
                .prefix(xmlNode.prefix())
                .inputLocation(xmlNode.inputLocation())
                .build();
        clearCache();

        return new XmlPlexusConfiguration(newChild);
    }

    public int getChildCount() {
        return xmlNode.children().size();
    }

    public String toString() {
        final StringBuilder buf = new StringBuilder().append('<').append(getName());
        for (final String a : getAttributeNames()) {
            buf.append(' ').append(a).append("=\"").append(getAttribute(a)).append('"');
        }
        if (getChildCount() > 0) {
            buf.append('>');
            for (int i = 0, size = getChildCount(); i < size; i++) {
                buf.append(getChild(i));
            }
            buf.append("</").append(getName()).append('>');
        } else if (null != getValue()) {
            buf.append('>').append(getValue()).append("</").append(getName()).append('>');
        } else {
            buf.append("/>");
        }
        return buf.append('\n').toString();
    }
}
