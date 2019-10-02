package org.apache.maven.xml.filter;

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

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.xml.Factories;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;

public abstract class AbstractXMLFilterTests
{
    public AbstractXMLFilterTests()
    {
        super();
    }
    
    protected abstract XMLFilter getFilter() throws TransformerException, SAXException, ParserConfigurationException;
    
    private void setParent( XMLFilter filter ) throws SAXException, ParserConfigurationException
    {
        if( filter.getParent() == null )
        {
            filter.setParent( Factories.newXMLReader() );
            filter.setFeature( "http://xml.org/sax/features/namespaces", true );
        }
    }

    protected String transform( String input )
        throws TransformerException, SAXException, ParserConfigurationException
    {
        return transform( new StringReader( input ) );
    }

    protected String transform( Reader input ) throws TransformerException, SAXException, ParserConfigurationException
    {
        XMLFilter filter = getFilter();
        setParent( filter );
        return transform( input, filter );
    }
    
    protected String transform( String input, XMLFilter filter ) 
        throws TransformerException, SAXException, ParserConfigurationException
    {
        setParent( filter );
        return transform( new StringReader( input ), filter );
    }

    protected String transform( Reader input, XMLFilter filter )
        throws TransformerException, SAXException, ParserConfigurationException
    {

        Writer writer = new StringWriter();
        StreamResult result = new StreamResult( writer );

        TransformerFactory transformerFactory = Factories.newTransformerFactory();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );

        SAXSource transformSource = new SAXSource( filter, new InputSource( input ) );

        transformer.transform( transformSource, result );

        return writer.toString();
    }
}