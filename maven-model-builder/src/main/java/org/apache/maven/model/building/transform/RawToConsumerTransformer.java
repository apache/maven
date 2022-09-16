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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Parent;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamWriter;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import static org.codehaus.plexus.util.StringUtils.isBlank;

public class RawToConsumerTransformer
{

    /**
     * Transforms a raw model to a consumer model
     */
    public Model transform( Model model )
    {
        // Remove all modules, this is just buildtime information
        if ( !model.getModules().isEmpty() )
        {
            model = model.withModules( Collections.emptyList() );
        }
        // Remove relativePath element, has no value for consumer pom
        if ( model.getParent() != null && model.getParent().getRelativePath() != null )
        {
            model = model.withParent( Parent.newBuilder( model.getParent(), true )
                    .relativePath( null ).build() );
        }
        // Done !
        return model;
    }

    /**
     * Reads a raw model from the given input stream and returns an input stream which
     * will contain the corresponding consumer model.
     */
    public InputStream transform( InputStream input ) throws IOException, XmlPullParserException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transform( input, baos );
        return new ByteArrayInputStream( baos.toByteArray() );
    }

    /**
     * Reads a raw model from the given input stream and writes the corresponding
     * consumer model to the output stream.
     */
    public void transform( InputStream input, OutputStream output ) throws IOException, XmlPullParserException
    {
        try ( Writer writer = new XmlStreamWriter( output ) )
        {
            XmlPullParser parser = new MXParser();
            parser.setInput( ReaderFactory.newXmlReader( input ) );

            XmlPullParser filter =
                    // Remove <modules/> element
                    new ModulesXMLFilter(
                            // Remove <relativePath/> elements
                            new RelativePathXMLFilter(
                                    // Ensure that xs:any elements aren't touched by next filters
                                    new FastForwardFilter( parser ) ) );

            XmlUtils.writeDocument( filter, writer );
        }
    }

    /**
     * Remove all modules, this is just buildtime information
     *
     * @author Robert Scholte
     * @author Guillaume Nodet
     * @since 4.0.0
     */
    static class ModulesXMLFilter
            extends XmlNodeBufferingParser
    {
        ModulesXMLFilter( XmlPullParser xmlPullParser )
        {
            super( xmlPullParser, "modules" );
        }

        protected void process( List<Event> buffer )
        {
            // Do nothing, as we want to delete those nodes completely
        }

    }

    /**
     * Remove relativePath element, has no value for consumer pom
     *
     * @author Robert Scholte
     * @author Guillaume Nodet
     * @since 4.0.0
     */
    static class RelativePathXMLFilter extends XmlNodeBufferingParser
    {

        RelativePathXMLFilter( XmlPullParser xmlPullParser )
        {
            super( xmlPullParser, "parent" );
        }

        protected void process( List<Event> buffer )
        {
            boolean skip = false;
            Event prev = null;
            for ( Event event : buffer )
            {
                if ( event.event == START_TAG && "relativePath".equals( event.name ) )
                {
                    skip = true;
                    if ( prev != null && prev.event == TEXT && isBlank( prev.text ) )
                    {
                        prev = null;
                    }
                    event = null;
                }
                else if ( event.event == END_TAG && "relativePath".equals( event.name ) )
                {
                    skip = false;
                    event = null;
                }
                else if ( skip )
                {
                    event = null;
                }
                if ( prev != null )
                {
                    pushEvent( prev );
                }
                prev = event;
            }
            pushEvent( prev );
        }

    }

    /**
     * This filter will bypass all following filters and write directly to the output.
     * Should be used in case of a DOM that should not be effected by other filters,
     * even though the elements match.
     *
     * @author Robert Scholte
     * @author Guillaume Nodet
     * @since 4.0.0
     */
    class FastForwardFilter extends XmlBufferingParser
    {
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

        FastForwardFilter( XmlPullParser xmlPullParser )
        {
            super( xmlPullParser );
        }

        @Override
        public int next() throws XmlPullParserException, IOException
        {
            int event = super.next();
            filter();
            return event;
        }

        @Override
        public int nextToken() throws XmlPullParserException, IOException
        {
            int event = super.nextToken();
            filter();
            return event;
        }

        protected void filter() throws XmlPullParserException, IOException
        {
            if ( xmlPullParser.getEventType() == START_TAG )
            {
                String localName = xmlPullParser.getName();
                if ( domDepth > 0 )
                {
                    domDepth++;
                }
                else
                {
                    final String key = state.peekLast() + '/' + localName;
                    switch ( key )
                    {
                        case "execution/configuration":
                        case "plugin/configuration":
                        case "plugin/goals":
                        case "profile/reports":
                        case "project/reports":
                        case "reportSet/configuration":
                            if ( domDepth == 0 )
                            {
                                bypass( true );
                            }
                            domDepth++;
                            break;
                        default:
                            break;
                    }
                }
                state.add( localName );
            }
            else if ( xmlPullParser.getEventType() == END_TAG )
            {
                if ( domDepth > 0 )
                {
                    if ( --domDepth == 0 )
                    {
                        bypass( false );
                    }
                }
                state.removeLast();
            }
        }

        @Override
        public void bypass( boolean bypass )
        {
            this.bypass = bypass;
        }
    }

}
