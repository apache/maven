package org.apache.maven.model.transform.sax;

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

import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A small example of a pipeline of 2 XML Filters, to understand how to get the expected result
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
public class ChainedFilterTest
{

    @Test
    public void test()
        throws Exception
    {
        String input = "<project><!-- aBc --><name>dEf</name></project>";

        SAXTransformerFactory transformerFactory = (SAXTransformerFactory) Factories.newTransformerFactory();
        TransformerHandler transformerHandler = transformerFactory.newTransformerHandler();

        Writer writer = new StringWriter();
        StreamResult result = new StreamResult( writer );
        transformerHandler.setResult( result );

        SAXResult transformResult = new SAXResult( transformerHandler );

        // Watch the order of filters! In reverse order the values would be 'AweSome'
        AbstractSAXFilter filter = new Awesome();

        // AbstractSAXFilter doesn't have a constructor with XMLReader, otherwise the LexicalHandler pipeline will be broken
        filter.setParent( Factories.newXMLReader() );

        // LexicalHandler of transformerResult must be the first filter
        transformResult.setLexicalHandler( filter );

        filter = new ChangeCase( filter );
        // LexicalHandler on last filter must be the transformerHandler
        filter.setLexicalHandler( transformerHandler );

        SAXSource transformSource = new SAXSource( filter, new InputSource( new StringReader( input ) ) );

        Transformer transformer = transformerFactory.newTransformer();
        transformer.transform( transformSource, transformResult );

        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project><!--AWESOME--><name>awesome</name></project>";
        assertThat( writer.toString() ).and( expected ).areIdentical();
    }

    static class ChangeCase
        extends AbstractSAXFilter
    {

        public ChangeCase()
        {
            super();
        }

        public ChangeCase( AbstractSAXFilter parent )
        {
            super( parent );
        }

        @Override
        public void comment( char[] ch, int start, int length )
            throws SAXException
        {
            String s = new String( ch, start, length ).toUpperCase();
            super.comment( s.toCharArray(), 0, s.length() );
        }

        @Override
        public void characters( char[] ch, int start, int length )
            throws SAXException
        {
            String s = new String( ch, start, length ).toLowerCase();
            super.characters( s.toCharArray(), 0, s.length() );
        }
    }

    static class Awesome
        extends AbstractSAXFilter
    {

        public Awesome()
        {
            super();
        }

        public Awesome( AbstractSAXFilter parent )
        {
            super( parent );
        }

        @Override
        public void comment( char[] ch, int start, int length )
            throws SAXException
        {
            String s = "AweSome";
            super.comment( s.toCharArray(), 0, s.length() );
        }

        @Override
        public void characters( char[] ch, int start, int length )
            throws SAXException
        {
            String s = "AweSome";
            super.characters( s.toCharArray(), 0, s.length() );
        }
    }

}
