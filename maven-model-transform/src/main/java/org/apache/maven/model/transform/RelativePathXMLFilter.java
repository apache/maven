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

/**
 * Remove relativePath element, has no value for consumer pom
 *
 * @author Robert Scholte
 * @author Guillaume Nodet
 * @since 4.0.0
 */
public class RelativePathXMLFilter extends NodeBufferingParser {

    private static final Pattern S_FILTER = Pattern.compile("\\s+");

    public RelativePathXMLFilter(XmlPullParser xmlPullParser) {
        super(xmlPullParser, "parent");
    }

    protected void process(List<Event> buffer) {
        boolean skip = false;
        Event prev = null;
        for (Event event : buffer) {
            if (event.event == START_TAG && "relativePath".equals(event.name)) {
                skip = true;
                if (prev != null
                        && prev.event == TEXT
                        && S_FILTER.matcher(prev.text).matches()) {
                    prev = null;
                }
                event = null;
            } else if (event.event == END_TAG && "relativePath".equals(event.name)) {
                skip = false;
                event = null;
            } else if (skip) {
                event = null;
            }
            if (prev != null) {
                pushEvent(prev);
            }
            prev = event;
        }
        pushEvent(prev);
    }
}
