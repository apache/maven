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

import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.model.transform.pull.NodeBufferingParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;

public class ModelVersionXMLFilter extends NodeBufferingParser {

    private static final Pattern S_FILTER = Pattern.compile("\\s+");
    public static final String NAMESPACE_PREFIX = "http://maven.apache.org/POM/";

    public ModelVersionXMLFilter(XmlPullParser xmlPullParser) {
        super(xmlPullParser, "project");
    }

    @Override
    protected void process(List<Event> buffer) {
        if (buffer.stream().noneMatch(e -> e.event == XmlPullParser.START_TAG && "modelVersion".equals(e.name))) {
            String namespace = null;
            for (int pos = 0; pos < buffer.size(); pos++) {
                Event e = buffer.get(pos);
                if (namespace != null) {
                    if (e.event == XmlPullParser.START_TAG) {
                        Event prev = buffer.get(pos - 1);
                        if (prev.event != TEXT || !S_FILTER.matcher(prev.text).matches()) {
                            prev = null;
                        }
                        Event pmse = new Event();
                        pmse.event = START_TAG;
                        pmse.name = "modelVersion";
                        pmse.namespace = namespace;
                        buffer.add(pos++, pmse);
                        Event pmve = new Event();
                        pmve.event = TEXT;
                        pmve.text = namespace.substring(NAMESPACE_PREFIX.length());
                        buffer.add(pos++, pmve);
                        Event pmee = new Event();
                        pmee.event = END_TAG;
                        pmee.name = "modelVersion";
                        pmee.namespace = namespace;
                        buffer.add(pos++, pmee);
                        if (prev != null) {
                            buffer.add(pos++, prev);
                        }
                        break;
                    }
                } else if (e.event == XmlPullParser.START_TAG
                        && "project".equals(e.name)
                        && e.namespace != null
                        && e.namespace.startsWith(NAMESPACE_PREFIX)) {
                    namespace = e.namespace;
                }
            }
        }
        buffer.forEach(this::pushEvent);
    }
}
