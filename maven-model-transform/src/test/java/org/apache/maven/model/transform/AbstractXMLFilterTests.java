package org.apache.maven.model.transform;

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
import java.util.function.Consumer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.model.transform.sax.AbstractSAXFilter;
import org.apache.maven.model.transform.sax.Factories;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

public abstract class AbstractXMLFilterTests
{
    protected AbstractSAXFilter getFilter() throws TransformerException, SAXException, ParserConfigurationException
    {
        throw new UnsupportedOperationException( "Override one of the getFilter() methods" );
    }

    protected AbstractSAXFilter getFilter( Consumer<LexicalHandler> result )  throws TransformerException, SAXException, ParserConfigurationException
    {
        return getFilter();
    }

    protected String omitXmlDeclaration()
    {
        return "yes";
    }

    protected String indentAmount()
    {
        return null;
    }

    protected String transform( String input )
        throws TransformerException, SAXException, ParserConfigurationException
    {
        return transform( new StringReader( input ) );
    }

    /**
     * Use this method only for testing a single filter.
     *
     * @param input
     * @param filter
     * @return
     * @throws TransformerException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    protected String transform( String input, AbstractSAXFilter filter )
        throws TransformerException, SAXException, ParserConfigurationException
    {
        setParent( filter );

        SAXTransformerFactory transformerFactory = (SAXTransformerFactory) Factories.newTransformerFactory();
        TransformerHandler transformerHandler = transformerFactory.newTransformerHandler();

        transformerHandler.getTransformer().setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, omitXmlDeclaration() );
        if ( indentAmount() != null )
        {
            transformerHandler.getTransformer().setOutputProperty( OutputKeys.INDENT, "yes" );
            transformerHandler.getTransformer().setOutputProperty( "{http://xml.apache.org/xslt}indent-amount",
                                                                   indentAmount() );
        }

        Transformer transformer = transformerFactory.newTransformer();

        Writer writer = new StringWriter();
        StreamResult result = new StreamResult( writer );
        transformerHandler.setResult( result );

        SAXResult transformResult = new SAXResult( transformerHandler );
        SAXSource transformSource = new SAXSource( filter, new InputSource( new StringReader( input ) ) );

        transformResult.setLexicalHandler( filter );
        transformer.transform( transformSource, transformResult );

        return writer.toString();

    }

    protected String transform( Reader input )
        throws TransformerException, SAXException, ParserConfigurationException
    {
        SAXTransformerFactory transformerFactory = (SAXTransformerFactory) Factories.newTransformerFactory();
        TransformerHandler transformerHandler = transformerFactory.newTransformerHandler();

        transformerHandler.getTransformer().setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, omitXmlDeclaration() );
        if ( indentAmount() != null )
        {
            transformerHandler.getTransformer().setOutputProperty( OutputKeys.INDENT, "yes" );
            transformerHandler.getTransformer().setOutputProperty( "{http://xml.apache.org/xslt}indent-amount",
                                                                   indentAmount() );
        }

        Transformer transformer = transformerFactory.newTransformer();

        Writer writer = new StringWriter();
        StreamResult result = new StreamResult( writer );
        transformerHandler.setResult( result );

        SAXResult transformResult = new SAXResult( transformerHandler );

        AbstractSAXFilter filter = getFilter( l -> transformResult.setLexicalHandler( l ) );
        setParent( filter );

        filter = new PerCharXMLFilter( filter );

        filter.setLexicalHandler( transformerHandler );

        SAXSource transformSource = new SAXSource( filter, new InputSource( input ) );

        transformer.transform( transformSource, transformResult );

        return writer.toString();
    }

    private void setParent( AbstractSAXFilter filter )
        throws SAXException, ParserConfigurationException
    {
        if ( filter.getParent() == null )
        {
            XMLReader r = Factories.newXMLReader();

            AbstractSAXFilter perChar = new PerCharXMLFilter();
            perChar.setParent( r );

            filter.setParent( perChar );
            filter.setFeature( "http://xml.org/sax/features/namespaces", true );
        }
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