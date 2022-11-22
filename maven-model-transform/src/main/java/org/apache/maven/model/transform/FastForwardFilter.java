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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import org.apache.maven.model.transform.pull.BufferingParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * This filter will bypass all following filters and write directly to the output.
 * Should be used in case of a DOM that should not be effected by other filters,
 * even though the elements match.
 *
 * @author Robert Scholte
 * @author Guillaume Nodet
 * @since 4.0.0
 */
class FastForwardFilter extends BufferingParser {
    /**
     * DOM elements of pom
     *
     * <ul>
     *  <li>execution.configuration</li>
     *  <li>plugin.configuration</li>
     *  <li>plugin.goals</li>
     *  <li>profile.reports</li>
     *  <li>project.reports</li>
     *  <li>reportSet.configuration</li>
     * <ul>
     */
    private final Deque<String> state = new ArrayDeque<>();

    private int domDepth = 0;

    FastForwardFilter(XmlPullParser xmlPullParser) {
        super(xmlPullParser);
    }

    @Override
    public int next() throws XmlPullParserException, IOException {
        int event = super.next();
        filter();
        return event;
    }

    @Override
    public int nextToken() throws XmlPullParserException, IOException {
        int event = super.nextToken();
        filter();
        return event;
    }

    protected void filter() throws XmlPullParserException, IOException {
        if (xmlPullParser.getEventType() == START_TAG) {
            String localName = xmlPullParser.getName();
            if (domDepth > 0) {
                domDepth++;
            } else {
                final String key = state.peekLast() + '/' + localName;
                switch (key) {
                    case "execution/configuration":
                    case "plugin/configuration":
                    case "plugin/goals":
                    case "profile/reports":
                    case "project/reports":
                    case "reportSet/configuration":
                        if (domDepth == 0) {
                            bypass(true);
                        }
                        domDepth++;
                        break;
                    default:
                        break;
                }
            }
            state.add(localName);
        } else if (xmlPullParser.getEventType() == END_TAG) {
            if (domDepth > 0) {
                if (--domDepth == 0) {
                    bypass(false);
                }
            }
            state.removeLast();
        }
    }

    @Override
    public void bypass(boolean bypass) {
        this.bypass = bypass;
    }
}
