package org.apache.maven.model.building.transform;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Buffer events while parsing a given element to allow some post-processing.
 *
 * @author Guillaume Nodet
 * @since 4.0.0
 */
public abstract class XmlNodeBufferingParser extends XmlBufferingParser
{

    private final List<Event> buffer = new ArrayList<>();

    private final String nodeName;

    private boolean buffering;

    public XmlNodeBufferingParser( XmlPullParser xmlPullParser, String nodeName )
    {
        super( xmlPullParser );
        this.nodeName = Objects.requireNonNull( nodeName );
    }

    @Override
    protected boolean accept() throws XmlPullParserException, IOException
    {
        if ( nodeName.equals( xmlPullParser.getName() ) )
        {
            if ( xmlPullParser.getEventType() == START_TAG && !buffering )
            {
                buffer.add( bufferEvent() );
                buffering = true;
                return false;
            }
            if ( xmlPullParser.getEventType() == END_TAG && buffering )
            {
                buffer.add( bufferEvent() );
                process( buffer );
                buffering = false;
                buffer.clear();
                return false;
            }
        }
        else if ( buffering )
        {
            buffer.add( bufferEvent() );
            return false;
        }
        return true;
    }

    protected abstract void process( List<Event> buffer );

}
