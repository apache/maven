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
import java.net.ContentHandler;

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
            
            AbstractSAXFilter perChar = new PerCharXMLFilter();
            perChar.setParent( r );
            
            filter.setParent( perChar );
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

        filter = new PerCharXMLFilter( filter );

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
    
    /**
     * From {@link ContentHandler}
     * <q>Your code should not assume that algorithms using char-at-a-time idioms will be working in characterunits; 
     * in some cases they will split characters. This is relevant wherever XML permits arbitrary characters, such as 
     * attribute values,processing instruction data, and comments as well as in data reported from this method. It's 
     * also generally relevant whenever Java code manipulates internationalized text; the issue isn't unique to XML.</q>
     *  
     * @author Robert Scholte
     */
    class PerCharXMLFilter
        extends AbstractSAXFilter
    {
        public PerCharXMLFilter()
        {
            super();
        }
        
        public PerCharXMLFilter( AbstractSAXFilter parent )
        {
            super( parent );
        }

        @Override
        public void characters( char[] ch, int start, int length )
            throws SAXException
        {
            for ( int i = 0; i < length; i++ )
            {
                super.characters( ch, start + i, 1 );
            }
        }
        
        @Override
        public void comment( char[] ch, int start, int length )
            throws SAXException
        {
            for ( int i = 0; i < length; i++ )
            {
                super.comment( ch, start + i, 1 );
            }
        }
        
        @Override
        public void ignorableWhitespace( char[] ch, int start, int length )
            throws SAXException
        {
            for ( int i = 0; i < length; i++ )
            {
                super.ignorableWhitespace( ch, start + i, 1 );
            }
        }
    }
}