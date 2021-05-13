package org.apache.maven.internal.aether;

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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import org.apache.maven.model.building.AbstractModelSourceTransformer;
import org.apache.maven.model.building.DefaultBuildPomXMLFilterFactory;
import org.apache.maven.model.building.TransformerContext;
import org.apache.maven.model.transform.sax.AbstractSAXFilter;
import org.apache.maven.xml.internal.DefaultConsumerPomXMLFilterFactory;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

class ConsumerModelSourceTransformer extends AbstractModelSourceTransformer
{
    @Override
    protected AbstractSAXFilter getSAXFilter( Path pomFile,
                                              TransformerContext context,
                                              Consumer<LexicalHandler> lexicalHandlerConsumer )
        throws TransformerConfigurationException, SAXException, ParserConfigurationException
    {
        return new DefaultConsumerPomXMLFilterFactory( new DefaultBuildPomXMLFilterFactory( context,
                                                                        lexicalHandlerConsumer, true ) ).get( pomFile );
    }

    /**
     * This transformer will ensure that encoding and version are kept.
     * However, it cannot prevent:
     * <ul>
     *   <li>attributes will be on one line</li>
     *   <li>Unnecessary whitespace before the rootelement will be removed</li>
     * </ul>
     */
    @Override
    protected TransformerHandler getTransformerHandler( Path pomFile )
        throws IOException, org.apache.maven.model.building.TransformerException
    {
        final TransformerHandler transformerHandler;

        final SAXTransformerFactory transformerFactory = getTransformerFactory();

        // Keep same encoding+version
        try ( InputStream input = Files.newInputStream( pomFile ) )
        {
            XMLStreamReader streamReader =
                XMLInputFactory.newFactory().createXMLStreamReader( input );

            transformerHandler = transformerFactory.newTransformerHandler();

            final String encoding = streamReader.getCharacterEncodingScheme();
            final String version = streamReader.getVersion();

            Transformer transformer = transformerHandler.getTransformer();
            transformer.setOutputProperty( OutputKeys.METHOD, "xml" );
            if ( encoding == null && version == null )
            {
                transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
            }
            else
            {
                transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "no" );

                if ( encoding != null )
                {
                    transformer.setOutputProperty( OutputKeys.ENCODING, encoding );
                }
                if ( version != null )
                {
                    transformer.setOutputProperty( OutputKeys.VERSION, version );
                }
            }
        }
        catch ( XMLStreamException | TransformerConfigurationException e )
        {
            throw new org.apache.maven.model.building.TransformerException(
                               "Failed to detect XML encoding and version", e );
        }
        return transformerHandler;
    }

}
