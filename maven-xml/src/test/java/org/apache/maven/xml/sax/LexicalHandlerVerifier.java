package org.apache.maven.xml.sax;

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

import static org.xmlunit.assertj.XmlAssert.assertThat;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.maven.xml.Factories;
import org.apache.maven.xml.sax.filter.AbstractSAXFilter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Some tests to help understand the chain of events regarding XMLFilters, XMLReaders, LexicalHandlers and Transformers
 * 
 * @author Robert Scholte
 *
 */
public class LexicalHandlerVerifier
{
    private static final String SAX_PROPERTIES_LEXICAL_HANDLER = "http://xml.org/sax/properties/lexical-handler";
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void parseXmlReader() throws Exception
    {
        expectedException.expect( UnsupportedOperationException.class );
        expectedException.expectMessage( "LexicalHandlerVerifier" );

        XMLReader reader = Factories.newXMLReader();
        reader.setProperty( SAX_PROPERTIES_LEXICAL_HANDLER, new UnsupportedOperationExceptionLexicalHandler() );

        InputSource inputSource = new InputSource( new StringReader( "<root><!-- COMMENT --></root>" ) ); 
        reader.parse( inputSource );
    }

    @Test
    public void parseXmlFilter() throws Exception
    {
        expectedException.expect( UnsupportedOperationException.class );
        expectedException.expectMessage( "LexicalHandlerVerifier" );

        XMLReader reader = Factories.newXMLReader();
        reader.setProperty( SAX_PROPERTIES_LEXICAL_HANDLER, new UnsupportedOperationExceptionLexicalHandler() );
        
        XMLFilter filter = new XMLFilterImpl( reader );

        InputSource inputSource = new InputSource( new StringReader( "<root><!-- COMMENT --></root>" ) ); 
        filter.parse( inputSource );
    }
    
    @Test
    public void transformXmlReader() throws Exception
    {
        Writer writer = new StringWriter();
        StreamResult result = new StreamResult( writer );

        SAXTransformerFactory transformerFactory = (SAXTransformerFactory) Factories.newTransformerFactory();
        TransformerHandler transformerHandler = transformerFactory.newTransformerHandler();
        transformerHandler.setResult( result );
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
        
        Source xmlSource = new StreamSource( new StringReader( "<root><!--COMMENT--></root>" ) );
        
        SAXResult transformResult = new SAXResult( transformerHandler );
        transformResult.setLexicalHandler( new SortCommentLexicalHandler( transformerHandler ) );
        transformer.transform( xmlSource, transformResult );
        
        assertThat( writer.toString() ).and( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<root><!--CEMMNOT--></root>" ).areIdentical();
    }

    @Test
    public void transformXmlFilter() throws Exception
    {
        Writer writer = new StringWriter();
        StreamResult result = new StreamResult( writer );

        SAXTransformerFactory transformerFactory = (SAXTransformerFactory) Factories.newTransformerFactory();
        TransformerHandler transformerHandler = transformerFactory.newTransformerHandler();
        transformerHandler.getTransformer().setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
        transformerHandler.setResult( result );
        Transformer transformer = transformerFactory.newTransformer();

        XMLReader reader = Factories.newXMLReader();
        // in wrong order result will be CONNENT -->
        AbstractSAXFilter filter2 = new M2NLexicalXMLFilter();
        filter2.setParent( reader );
        filter2.setLexicalHandler( transformerHandler );
        
        AbstractSAXFilter filter1 = new N2CLexicalXMLFilter( filter2 );

        SAXSource transformSource = new SAXSource( filter1, new InputSource( new StringReader( "<root><!-- COMMENT --></root>" ) ) );

        SAXResult transformResult = new SAXResult( transformerHandler );
        transformResult.setLexicalHandler( filter1 );
        transformer.transform( transformSource, transformResult );

        assertThat( writer.toString() ).and( "<root><!-- CONNECT --></root>" ).areIdentical();
    }

    /**
     * A LexicalHandler that'll throw an UnsupportedOperationException on every call.
     * 
     * @author Robert Scholte
     */
    static class UnsupportedOperationExceptionLexicalHandler implements LexicalHandler
    {
        @Override
        public void startDTD( String name, String publicId, String systemId )
            throws SAXException
        {
            throw new UnsupportedOperationException( "LexicalHandlerVerifier" );
        }

        @Override
        public void endDTD()
            throws SAXException
        {
            throw new UnsupportedOperationException( "LexicalHandlerVerifier" );
        }

        @Override
        public void startEntity( String name )
            throws SAXException
        {
            throw new UnsupportedOperationException( "LexicalHandlerVerifier" );
        }

        @Override
        public void endEntity( String name )
            throws SAXException
        {
            throw new UnsupportedOperationException( "LexicalHandlerVerifier" );
        }

        @Override
        public void startCDATA()
            throws SAXException
        {
            throw new UnsupportedOperationException( "LexicalHandlerVerifier" );
        }

        @Override
        public void endCDATA()
            throws SAXException
        {
            throw new UnsupportedOperationException( "LexicalHandlerVerifier" );
        }

        @Override
        public void comment( char[] ch, int start, int length )
            throws SAXException
        {
            throw new UnsupportedOperationException( "LexicalHandlerVerifier" );
        }
    }
    
    /**
     * Sorts the comment chars,will throw an UnsupportedOperationException on every other method
     * 
     * @author Robert Scholte
     *
     */
    static class SortCommentLexicalHandler extends UnsupportedOperationExceptionLexicalHandler
    {
        private final LexicalHandler lexicalHandler;
        
        public SortCommentLexicalHandler( LexicalHandler lexicalHandler )
        {
            this.lexicalHandler = lexicalHandler;
        }


        @Override
        public void comment( char[] ch, int start, int length )
            throws SAXException
        {
            char[] chars = new String( ch, start, length ).toCharArray();
            Arrays.sort(chars);
            lexicalHandler.comment( chars, 0, chars.length );
        }
    }
    
    /**
     * AbstractSAXFilter implements both XMLReader and LexicalHandler
     * 
     * @author Robert Scholte
     *
     */
    static class N2CLexicalXMLFilter extends AbstractSAXFilter
    {
        public N2CLexicalXMLFilter()
        {
            super( null );
        }
        
        public <T extends XMLReader & LexicalHandler> N2CLexicalXMLFilter( T parent )
        {
            super( parent );
        }

        @Override
        public void comment( char[] ch, int start, int length )
            throws SAXException
        {
            super.comment( new String( ch, start, length ).replace( 'N', 'C' ).toCharArray(), start, length );
        }
    }
    
    /**
     * AbstractSAXFilter implements both XMLReader and LexicalHandler
     * 
     * @author Robert Scholte
     *
     */
    static class M2NLexicalXMLFilter extends AbstractSAXFilter
    {
        public M2NLexicalXMLFilter()
        {
            super( null );
        }
        
        public <T extends XMLReader & LexicalHandler> M2NLexicalXMLFilter( T parent )
        {
            super( parent );
        }

        @Override
        public void comment( char[] ch, int start, int length )
            throws SAXException
        {
            super.comment( new String( ch, start, length ).replace( 'M', 'N' ).toCharArray(), start, length );
        }
    }
}
