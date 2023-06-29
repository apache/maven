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
package org.apache.maven.model.transform;

import javax.xml.stream.XMLStreamReader;

import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import org.apache.maven.model.transform.stax.NodeBufferingParser;

/**
 * Will apply the version if the dependency is part of the reactor
 *
 * @author Robert Scholte
 * @author Guillaume Nodet
 * @since 4.0.0
 */
public class ReactorDependencyXMLFilter extends NodeBufferingParser {
    private final BiFunction<String, String, String> reactorVersionMapper;

    private static final Pattern S_FILTER = Pattern.compile("\\s+");

    public ReactorDependencyXMLFilter(
            XMLStreamReader delegate, BiFunction<String, String, String> reactorVersionMapper) {
        super(delegate, "dependency");
        this.reactorVersionMapper = reactorVersionMapper;
    }

    protected void process(List<Event> buffer) {
        // whiteSpace after <dependency>, to be used to position <version>
        String dependencyWhitespace = "";
        boolean hasVersion = false;
        String groupId = null;
        String artifactId = null;
        String tagName = null;
        for (int i = 0; i < buffer.size(); i++) {
            Event event = buffer.get(i);
            if (event.event == START_ELEMENT) {
                tagName = event.name;
                hasVersion |= "version".equals(tagName);
            } else if (event.event == CHARACTERS) {
                if (S_FILTER.matcher(event.text).matches()) {
                    if (dependencyWhitespace.isEmpty()) {
                        dependencyWhitespace = event.text;
                    }
                } else if ("groupId".equals(tagName)) {
                    groupId = nullSafeAppend(groupId, event.text);
                } else if ("artifactId".equals(tagName)) {
                    artifactId = nullSafeAppend(artifactId, event.text);
                }
            } else if (event.event == END_ELEMENT && "dependency".equals(event.name)) {
                String version = reactorVersionMapper.apply(groupId, artifactId);
                if (!hasVersion && version != null) {
                    int pos = buffer.get(i - 1).event == CHARACTERS ? i - 1 : i;
                    Event e = new Event();
                    e.event = CHARACTERS;
                    e.text = dependencyWhitespace;
                    buffer.add(pos++, e);
                    e = new Event();
                    e.event = START_ELEMENT;
                    e.namespace = buffer.get(0).namespace;
                    e.prefix = buffer.get(0).prefix;
                    e.name = "version";
                    buffer.add(pos++, e);
                    e = new Event();
                    e.event = CHARACTERS;
                    e.text = version;
                    buffer.add(pos++, e);
                    e = new Event();
                    e.event = END_ELEMENT;
                    e.name = "version";
                    e.namespace = buffer.get(0).namespace;
                    e.prefix = buffer.get(0).prefix;
                    buffer.add(pos++, e);
                }
                break;
            }
        }
        buffer.forEach(this::pushEvent);
    }
}
