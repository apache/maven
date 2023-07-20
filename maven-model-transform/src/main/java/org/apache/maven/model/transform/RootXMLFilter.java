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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Stream;

import org.apache.maven.model.transform.stax.BufferingParser;

/**
 * Remove the root attribute on the model
 *
 * @author Guillaume Nodet
 * @since 4.0.0
 */
class RootXMLFilter extends BufferingParser {

    final Deque<String> elements = new ArrayDeque<>();

    RootXMLFilter(XMLStreamReader delegate) {
        super(delegate);
    }

    @Override
    protected boolean accept() throws XMLStreamException {
        if (delegate.getEventType() == START_ELEMENT) {
            elements.push(delegate.getLocalName());
            if (elements.size() == 1 && "project".equals(delegate.getLocalName())) {
                Event event = bufferEvent();
                event.attributes = Stream.of(event.attributes)
                        .filter(a -> !"root".equals(a.name))
                        .toArray(Attribute[]::new);
                pushEvent(event);
                return false;
            }
        } else if (delegate.getEventType() == END_ELEMENT) {
            elements.pop();
        }
        return true;
    }
}
