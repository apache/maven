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
import java.util.stream.Stream;

import org.apache.maven.model.transform.pull.BufferingParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Remove the root attribute on the model
 *
 * @author Guillaume Nodet
 * @since 4.0.0
 */
class RootXMLFilter extends BufferingParser {
    RootXMLFilter(XmlPullParser xmlPullParser) {
        super(xmlPullParser);
    }

    @Override
    protected boolean accept() throws XmlPullParserException, IOException {
        if (xmlPullParser.getEventType() == XmlPullParser.START_TAG) {
            if (xmlPullParser.getDepth() == 1 && "project".equals(xmlPullParser.getName())) {
                Event event = bufferEvent();
                event.attributes = Stream.of(event.attributes)
                        .filter(a -> !"root".equals(a.name))
                        .toArray(Attribute[]::new);
                pushEvent(event);
                return false;
            }
        }
        return true;
    }
}
