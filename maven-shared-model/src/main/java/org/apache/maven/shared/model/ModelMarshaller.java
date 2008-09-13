package org.apache.maven.shared.model;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides methods for marshalling and unmarshalling XML that does not contain attributes.
 */
public final class ModelMarshaller
{

    /**
     * Private Constructor
     */
    private ModelMarshaller()
    {
    }

    /**
     * Returns list of model properties transformed from the specified input stream.
     *
     * @param inputStream input stream containing the xml document. May not be null.
     * @param baseUri     the base uri of every model property. May not be null or empty.
     * @param collections set of uris that are to be treated as a collection (multiple entries). May be null.
     * @return list of model properties transformed from the specified input stream.
     * @throws IOException if there was a problem doing the transform
     */
    public static List<ModelProperty> marshallXmlToModelProperties( InputStream inputStream, String baseUri,
                                                                    Set<String> collections )
        throws IOException
    {
        if ( inputStream == null )
        {
            throw new IllegalArgumentException( "inputStream: null" );
        }

        if ( baseUri == null || baseUri.trim().length() == 0 )
        {
            throw new IllegalArgumentException( "baseUri: null" );
        }

        if ( collections == null )
        {
            collections = Collections.emptySet();
        }

        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, "false");
        xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, "false");

        Uri uri = new Uri( baseUri );
        String tagName = baseUri;
        String tagValue = null;

        int depth = 0;
        XMLStreamReader xmlStreamReader = null;
        try
        {
            xmlStreamReader = xmlInputFactory.createXMLStreamReader( inputStream );
            
            Map<String, String> attributes = new HashMap<String, String>();
            for ( ; ; xmlStreamReader.next() )
            {
                int type = xmlStreamReader.getEventType();
                switch ( type )
                {

                    case XMLStreamConstants.CHARACTERS:
                    {
                        String tmp = xmlStreamReader.getText();
                        if ( tmp != null && tmp.trim().length() != 0 )
                        {
                            tagValue = tmp;
                        }
                        break;
                    }

                    case XMLStreamConstants.START_ELEMENT:
                    {
                        depth++;
                        if ( !tagName.equals( baseUri ) )
                        {
                            modelProperties.add( new ModelProperty( tagName, tagValue ) );
                            if ( !attributes.isEmpty() )
                            {
                                for ( Map.Entry<String, String> e : attributes.entrySet() )
                                {
                                    modelProperties.add( new ModelProperty( e.getKey(), e.getValue() ) );
                                }
                                attributes.clear();
                            }
                        }

                        tagName = uri.getUriFor( xmlStreamReader.getName().getLocalPart(), depth );
                        if ( collections.contains( tagName + "#collection" ) )
                        {
                            tagName = tagName + "#collection";
                            uri.addTag( xmlStreamReader.getName().getLocalPart() + "#collection" );
                        }
                        else if(collections.contains( tagName + "#set" ))
                        {
                            tagName = tagName + "#set";
                            uri.addTag( xmlStreamReader.getName().getLocalPart() + "#set" );
                        }
                        else
                        {
                            uri.addTag( xmlStreamReader.getName().getLocalPart() );
                        }
                        tagValue = null;

                    }
                    case XMLStreamConstants.ATTRIBUTE:
                    {
                        for ( int i = 0; i < xmlStreamReader.getAttributeCount(); i++ )
                        {

                            attributes.put(
                                tagName + "#property/" + xmlStreamReader.getAttributeName( i ).getLocalPart(),
                                xmlStreamReader.getAttributeValue( i ) );
                        }
                        break;
                    }
                    case XMLStreamConstants.END_ELEMENT:
                    {
                        depth--;
                        if ( tagValue == null )
                        {
                            tagValue = "";
                        }
                        break;
                    }
                    case XMLStreamConstants.END_DOCUMENT:
                    {
                        modelProperties.add( new ModelProperty( tagName, tagValue ) );
                        if ( !attributes.isEmpty() )
                        {
                            for ( Map.Entry<String, String> e : attributes.entrySet() )
                            {
                                modelProperties.add( new ModelProperty( e.getKey(), e.getValue() ) );
                            }
                            attributes.clear();
                        }
                        return modelProperties;
                    }
                }
            }
        }
        catch ( XMLStreamException e )
        {
            throw new IOException( ":" + e.toString() );
        }
        finally
        {
            if ( xmlStreamReader != null )
            {
                try
                {
                    xmlStreamReader.close();
                }
                catch ( XMLStreamException e )
                {
                    e.printStackTrace();
                }
            }
            try
            {
                inputStream.close();
            }
            catch ( IOException e )
            {

            }
        }
    }

    /**
     * Returns XML string unmarshalled from the specified list of model properties
     *
     * @param modelProperties the model properties to unmarshal. May not be null or empty
     * @param baseUri         the base uri of every model property. May not be null or empty.
     * @return XML string unmarshalled from the specified list of model properties
     * @throws IOException if there was a problem with unmarshalling
     */
    public static String unmarshalModelPropertiesToXml( List<ModelProperty> modelProperties, String baseUri )
        throws IOException
    {
        if ( modelProperties == null || modelProperties.isEmpty() )
        {
            throw new IllegalArgumentException( "modelProperties: null or empty" );
        }

        if ( baseUri == null || baseUri.trim().length() == 0 )
        {
            throw new IllegalArgumentException( "baseUri: null or empty" );
        }

        final int basePosition = baseUri.length();

        StringBuffer sb = new StringBuffer();
        List<String> lastUriTags = new ArrayList<String>();
        int n = 1;
        for ( ModelProperty mp : modelProperties )
        {
            String uri = mp.getUri();
            if ( uri.contains( "#property" ) )
            {
                continue;
            }

            //String val = (mp.getResolvedValue() != null) ? "\"" + mp.getResolvedValue() + "\"" : null;
            //   System.out.println("new ModelProperty(\"" + mp.getUri() +"\" , " + val +"),");
            if ( !uri.startsWith( baseUri ) )
            {
                throw new IllegalArgumentException(
                    "Passed in model property that does not match baseUri: Property URI = " + uri + ", Base URI = " +
                        baseUri );
            }
            List<String> tagNames = getTagNamesFromUri( basePosition, uri );
            if ( lastUriTags.size() > tagNames.size() )
            {
                for ( int i = lastUriTags.size() - 1; i >= tagNames.size(); i-- )
                {
                    sb.append( toEndTag( lastUriTags.get( i - 1 ) ) );
                }
            }
            String tag = tagNames.get( tagNames.size() - 1 );

            List<ModelProperty> attributes = new ArrayList<ModelProperty>();
            for(int peekIndex = modelProperties.indexOf( mp ) + 1; peekIndex < modelProperties.size(); peekIndex++)
            {
                if ( peekIndex <= modelProperties.size() - 1 )
                {
                    ModelProperty peekProperty = modelProperties.get( peekIndex );
                    if ( peekProperty.getUri().contains( "#property" ) )
                    {
                        attributes.add(peekProperty);
                    }
                    else
                    {
                        break;
                    }
                }
                else
                {
                    break;
                }
            }

            sb.append( toStartTag( tag, attributes ) );
            if ( mp.getResolvedValue() != null )
            {
                sb.append( mp.getResolvedValue() );
                sb.append( toEndTag( tag ) );
                n = 2;
            }
            else if(!attributes.isEmpty())
            {
                int pi = modelProperties.indexOf( mp ) + attributes.size() + 1;
                if ( pi <= modelProperties.size() - 1 )
                {
                    ModelProperty peekProperty = modelProperties.get( pi );
                    if ( !peekProperty.getUri().startsWith(mp.getUri()) )
                    {
                        if( mp.getResolvedValue() != null )
                        {
                            sb.append( mp.getResolvedValue() );
                        }
                        sb.append( toEndTag( tag ) );
                        n = 2;
                    }
                }
            }
            else
            {
                n = 1;
            }
            lastUriTags = tagNames;
        }
        for ( int i = lastUriTags.size() - n; i >= 1; i-- )
        {
            sb.append( toEndTag( lastUriTags.get( i ) ) );
        }
        return sb.toString();
    }

    /**
     * Returns list of tag names parsed from the specified uri. All #collection parts of the tag are removed from the
     * tag names.
     *
     * @param basePosition the base position in the specified URI to start the parse
     * @param uri          the uri to parse for tag names
     * @return list of tag names parsed from the specified uri
     */
    private static List<String> getTagNamesFromUri( int basePosition, String uri )
    {
        return Arrays.asList( uri.substring( basePosition ).replaceAll( "#collection", "" )
                .replaceAll("#set", "").split( "/" ) );
    }

    /**
     * Returns the XML formatted start tag for the specified value and the specified attribute.
     *
     * @param value     the value to use for the start tag
     * @param attributes the attribute to use in constructing of start tag
     * @return the XML formatted start tag for the specified value and the specified attribute
     */
    private static String toStartTag( String value, List<ModelProperty> attributes )
    {
        StringBuffer sb = new StringBuffer(); //TODO: Support more than one attribute
        sb.append( "\r\n<" ).append( value );
        if ( attributes != null )
        {
            for(ModelProperty attribute : attributes)
            {
                sb.append( " " ).append(
                    attribute.getUri().substring( attribute.getUri().indexOf( "#property/" ) + 10 ) ).append( "=\"" )
                    .append( attribute.getResolvedValue() ).append( "\" " );
            }
        }
        sb.append( ">" );
        return sb.toString();
    }

    /**
     * Returns XML formatted end tag for the specified value.
     *
     * @param value the value to use for the end tag
     * @return xml formatted end tag for the specified value
     */
    private static String toEndTag( String value )
    {
        if ( value.trim().length() == 0 )
        {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        sb.append( "</" ).append( value ).append( ">" );
        return sb.toString();
    }

    /**
     * Class for storing information about URIs.
     */
    private static class Uri
    {

        List<String> uris;

        Uri( String baseUri )
        {
            uris = new LinkedList<String>();
            uris.add( baseUri );
        }

        String getUriFor( String tag, int depth )
        {
            setUrisToDepth( depth );
            StringBuffer sb = new StringBuffer();
            for ( String tagName : uris )
            {
                sb.append( tagName ).append( "/" );
            }
            sb.append( tag );
            return sb.toString();
        }

        void addTag( String tag )
        {
            uris.add( tag );
        }

        void setUrisToDepth( int depth )
        {
            uris = new LinkedList<String>( uris.subList( 0, depth ) );
        }
    }
}
