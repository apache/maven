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
package org.apache.maven.stax.xinclude;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

class FragmentReader extends StreamReaderDelegate {

    private final XMLStreamReader delegate;
    private int depth;
    private int current = START_DOCUMENT;
    private int state = 0;

    FragmentReader(XMLStreamReader delegate) {
        this.delegate = delegate;
        this.depth = 1;
    }

    @Override
    public int next() throws XMLStreamException {
        if (state == 0) {
            current = getDelegate().getEventType();
            state++;
        } else {
            if (depth == 0) {
                current = END_DOCUMENT;
            } else {
                current = super.next();
                if (current == START_ELEMENT) {
                    depth++;
                } else if (current == END_ELEMENT) {
                    depth--;
                }
            }
        }
        return current;
    }

    @Override
    public int getEventType() {
        return current;
    }

    @Override
    public boolean hasNext() throws XMLStreamException {
        return current != END_DOCUMENT;
    }

    @Override
    protected XMLStreamReader getDelegate() {
        return delegate;
    }
}
