package org.apache.maven.xml.sax.filter;

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
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.xml.Factories;
import org.apache.maven.xml.sax.filter.AbstractSAXFilter;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public abstract class AbstractXMLFilterTests
{
    public AbstractXMLFilterTests()
    {
        super();
    }
    
    protected abstract AbstractSAXFilter getFilter() throws TransformerException, SAXException, ParserConfigurationException;
    
    private void setParent( AbstractSAXFilter filter ) throws SAXException, ParserConfigurationException
    {
        if( filter.getParent() == null )
        {
            XMLReader r = Factories.newXMLReader();
            
            filter.setParent( r );
            filter.setFeature( "http://xml.org/sax/features/namespaces", true );
        }
    }
    
    protected String omitXmlDeclaration() {
        return "yes";
    }
    
    protected String indentAmount() {
        return null;
    }

    protected String transform( String input )
        throws TransformerException, SAXException, ParserConfigurationException
    {
        return transform( new StringReader( input ) );
    }

    protected String transform( Reader input ) throws TransformerException, SAXException, ParserConfigurationException
    {
        AbstractSAXFilter filter = getFilter();
        setParent( filter );

        return transform( input, filter );
    }
    
    protected String transform( String input, AbstractSAXFilter filter ) 
        throws TransformerException, SAXException, ParserConfigurationException
    {
        setParent( filter );
        return transform( new StringReader( input ), filter );
    }

    protected String transform( Reader input, AbstractSAXFilter filter )
        throws TransformerException, SAXException, ParserConfigurationException
    {
        Writer writer = new StringWriter();
        StreamResult result = new StreamResult( writer );

        SAXTransformerFactory transformerFactory = (SAXTransformerFactory) Factories.newTransformerFactory();
        TransformerHandler transformerHandler = transformerFactory.newTransformerHandler();
        filter.setLexicalHandler( transformerHandler );
        transformerHandler.getTransformer().setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, omitXmlDeclaration() );
        if ( indentAmount() != null )
        {
            transformerHandler.getTransformer().setOutputProperty( OutputKeys.INDENT, "yes" );
            transformerHandler.getTransformer().setOutputProperty( "{http://xml.apache.org/xslt}indent-amount",
                                                                   indentAmount() );
        }
        transformerHandler.setResult( result );
        Transformer transformer = transformerFactory.newTransformer();

        SAXSource transformSource = new SAXSource( filter, new InputSource( input ) );

        SAXResult transformResult = new SAXResult( transformerHandler );
        transformResult.setLexicalHandler( filter );
        transformer.transform( transformSource, transformResult );

        return writer.toString();
    }
}