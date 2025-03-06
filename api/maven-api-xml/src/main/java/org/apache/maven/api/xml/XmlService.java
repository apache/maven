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
package org.apache.maven.api.xml;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ServiceLoader;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * Comprehensive service interface for XML operations including node creation,
 * merging, reading, and writing.
 *
 * <p>This class provides XML merging functionality for Maven's XML handling
 * and specifies the combination modes that control how XML elements are merged.</p>
 *
 * <p>The merger supports two main types of combinations:</p>
 * <ul>
 *   <li>Children combination: Controls how child elements are combined</li>
 *   <li>Self combination: Controls how the element itself is combined</li>
 * </ul>
 *
 * <p>Children combination modes (specified by {@code combine.children} attribute):</p>
 * <ul>
 *   <li>{@code merge} (default): Merges elements with matching names</li>
 *   <li>{@code append}: Adds elements as siblings</li>
 * </ul>
 *
 * <p>Self combination modes (specified by {@code combine.self} attribute):</p>
 * <ul>
 *   <li>{@code merge} (default): Merges attributes and values</li>
 *   <li>{@code override}: Completely replaces the element</li>
 *   <li>{@code remove}: Removes the element</li>
 * </ul>
 *
 * <p>For complex XML structures, combining can also be done based on:</p>
 * <ul>
 *   <li>ID: Using the {@code combine.id} attribute</li>
 *   <li>Keys: Using the {@code combine.keys} attribute with comma-separated key names</li>
 * </ul>
 *
 * @since 4.0.0
 */
public abstract class XmlService {

    /** Attribute name controlling how child elements are combined */
    public static final String CHILDREN_COMBINATION_MODE_ATTRIBUTE = "combine.children";
    /** Value indicating children should be merged based on element names */
    public static final String CHILDREN_COMBINATION_MERGE = "merge";
    /** Value indicating children should be appended as siblings */
    public static final String CHILDREN_COMBINATION_APPEND = "append";
    /**
     * Default mode for combining children DOMs during merge.
     * When element names match, the process will try to merge the element data,
     * rather than putting the dominant and recessive elements as siblings.
     */
    public static final String DEFAULT_CHILDREN_COMBINATION_MODE = CHILDREN_COMBINATION_MERGE;

    /** Attribute name controlling how the element itself is combined */
    public static final String SELF_COMBINATION_MODE_ATTRIBUTE = "combine.self";
    /** Value indicating the element should be completely overridden */
    public static final String SELF_COMBINATION_OVERRIDE = "override";
    /** Value indicating the element should be merged */
    public static final String SELF_COMBINATION_MERGE = "merge";
    /** Value indicating the element should be removed */
    public static final String SELF_COMBINATION_REMOVE = "remove";
    /**
     * Default mode for combining a DOM node during merge.
     * When element names match, the process will try to merge element attributes
     * and values, rather than overriding the recessive element completely.
     */
    public static final String DEFAULT_SELF_COMBINATION_MODE = SELF_COMBINATION_MERGE;

    /** Attribute name for ID-based combination mode */
    public static final String ID_COMBINATION_MODE_ATTRIBUTE = "combine.id";
    /**
     * Attribute name for key-based combination mode.
     * Value should be a comma-separated list of attribute names.
     */
    public static final String KEYS_COMBINATION_MODE_ATTRIBUTE = "combine.keys";

    /**
     * Convenience method to merge two XML nodes using default settings.
     */
    @Nullable
    public static XmlNode merge(XmlNode dominant, XmlNode recessive) {
        return merge(dominant, recessive, null);
    }

    /**
     * Merges two XML nodes.
     */
    @Nullable
    public static XmlNode merge(
            @Nullable XmlNode dominant, @Nullable XmlNode recessive, @Nullable Boolean childMergeOverride) {
        return getService().doMerge(dominant, recessive, childMergeOverride);
    }

    /**
     * Reads an XML node from an input stream.
     */
    @Nonnull
    public static XmlNode read(InputStream input, @Nullable InputLocationBuilder locationBuilder)
            throws XMLStreamException {
        return getService().doRead(input, locationBuilder);
    }

    /**
     * Reads an XML node from a reader.
     */
    @Nonnull
    public static XmlNode read(Reader reader, @Nullable InputLocationBuilder locationBuilder)
            throws XMLStreamException {
        return getService().doRead(reader, locationBuilder);
    }

    /**
     * Reads an XML node from an XMLStreamReader.
     */
    @Nonnull
    public static XmlNode read(XMLStreamReader reader, @Nullable InputLocationBuilder locationBuilder)
            throws XMLStreamException {
        return getService().doRead(reader, locationBuilder);
    }

    /**
     * Writes an XML node to a writer.
     */
    public static void write(XmlNode node, Writer writer) throws IOException {
        getService().doWrite(node, writer);
    }

    /**
     * Interface for building input locations during XML parsing.
     */
    public interface InputLocationBuilder {
        Object toInputLocation(XMLStreamReader parser);
    }

    protected abstract XmlNode doRead(InputStream input, InputLocationBuilder locationBuilder)
            throws XMLStreamException;

    protected abstract XmlNode doRead(Reader reader, InputLocationBuilder locationBuilder) throws XMLStreamException;

    protected abstract XmlNode doRead(XMLStreamReader reader, InputLocationBuilder locationBuilder)
            throws XMLStreamException;

    protected abstract void doWrite(XmlNode node, Writer writer) throws IOException;

    protected abstract XmlNode doMerge(XmlNode dominant, XmlNode recessive, Boolean childMergeOverride);

    private static XmlService getService() {
        return Holder.INSTANCE;
    }

    /** Holder class for lazy initialization of the default instance */
    private static final class Holder {
        static final XmlService INSTANCE = ServiceLoader.load(XmlService.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No XmlService implementation found"));

        private Holder() {}
    }
}
