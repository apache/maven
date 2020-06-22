package org.apache.maven.xml;

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

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;

import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * Creates XML related factories with OWASP advices applied
 * 
 * @author Robert Scholte
 * @since 3.7.0
 */
public final class Factories
{
    private Factories()
    {
    }
    
    /**
     * 
     * @return
     * @see https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html#transformerfactory
     */
    public static TransformerFactory newTransformerFactory() 
    {
        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setAttribute( XMLConstants.ACCESS_EXTERNAL_DTD, "" );
        tf.setAttribute( XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "" );

        return tf;
    }
    
    public static SAXParserFactory newSAXParserFactory()
    {
        SAXParserFactory spf = SAXParserFactory.newInstance();

        try
        {
            // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-general-entities
            // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-general-entities
   
            // Using the SAXParserFactory's setFeature
            spf.setFeature( "http://xml.org/sax/features/external-general-entities", false );
            
            // Xerces 2 only - http://xerces.apache.org/xerces-j/features.html#external-general-entities
            spf.setFeature( "http://apache.org/xml/features/disallow-doctype-decl", true );
        }
        catch ( ParserConfigurationException e )
        {
            // Tried an unsupported feature.
        }
        catch ( SAXNotRecognizedException e )
        {
            // Tried an unknown feature.
        }
        catch ( SAXNotSupportedException e )
        {
            // Tried a feature known to the parser but unsupported.
        }
        return spf;
    }
    
    public static SAXParser newSAXParser() throws ParserConfigurationException, SAXException
    {
        SAXParser saxParser = newSAXParserFactory().newSAXParser();
        
        return saxParser;
    }
    
    public static XMLReader newXMLReader() throws SAXException, ParserConfigurationException
    {
        XMLReader reader = newSAXParser().getXMLReader();
        
        try
        {
            // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-general-entities
            // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-general-entities
       
            // Using the XMLReader's setFeature
            reader.setFeature( "http://xml.org/sax/features/external-general-entities", false );
        }
        catch ( SAXNotRecognizedException e )
        {
            // Tried an unknown feature.
        }
        catch ( SAXNotSupportedException e )
        {
            // Tried a feature known to the parser but unsupported.
        }
        return reader;
    }
}
