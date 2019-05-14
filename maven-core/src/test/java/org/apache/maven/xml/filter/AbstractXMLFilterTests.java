package org.apache.maven.xml.filter;

import java.io.Reader;

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

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class AbstractXMLFilterTests
{

    public AbstractXMLFilterTests()
    {
        super();
    }

    protected String transform( String input, XMLFilter filter )
        throws TransformerException, SAXException
    {
        return transform( new StringReader( input ), filter );
    }
    
    protected String transform( Reader input, XMLFilter filter )
        throws TransformerException, SAXException
    {
        XMLReader reader = XMLReaderFactory.createXMLReader();

        XMLFilter parent = filter;
        while ( parent.getParent() instanceof XMLFilter )
        {
            parent = (XMLFilter) parent.getParent();
        }
        parent.setParent( reader );

        Writer writer = new StringWriter();
        StreamResult result = new StreamResult( writer );

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );

        SAXSource transformSource = new SAXSource( filter, new InputSource( input ) );

        transformer.transform( transformSource, result );

        return writer.toString();
    }
}