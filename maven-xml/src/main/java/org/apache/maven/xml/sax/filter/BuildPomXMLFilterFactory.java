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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.apache.maven.xml.Factories;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

/**
 * Base implementation for providing the BuildPomXML.
 * 
 * @author Robert Scholte
 * @since 3.7.0
 */
public class BuildPomXMLFilterFactory
{
    private final Consumer<LexicalHandler> lexicalHandlerConsumer;
    
    public BuildPomXMLFilterFactory()
    {
        this( null ); 
    }
    
    public BuildPomXMLFilterFactory( Consumer<LexicalHandler> lexicalHandlerConsumer )
    {
        this.lexicalHandlerConsumer = lexicalHandlerConsumer;
    }

    /**
     * 
     * @param projectFile will be used by ConsumerPomXMLFilter to get the right filter
     * @return
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws TransformerConfigurationException
     */
    public final BuildPomXMLFilter get( Path projectFile )
        throws SAXException, ParserConfigurationException, TransformerConfigurationException
    {
        AbstractSAXFilter parent = new AbstractSAXFilter();
        parent.setParent( getXMLReader() );
        if ( lexicalHandlerConsumer != null )
        {
            lexicalHandlerConsumer.accept( parent );
        }

        if ( getDependencyKeyToVersionMapper() != null )
        {
            ReactorDependencyXMLFilter reactorDependencyXMLFilter =
                new ReactorDependencyXMLFilter( getDependencyKeyToVersionMapper() );
            reactorDependencyXMLFilter.setParent( parent );
            parent.setLexicalHandler( reactorDependencyXMLFilter );
            parent = reactorDependencyXMLFilter;
        }

        if ( getRelativePathMapper() != null )
        {
            ParentXMLFilter parentFilter = new ParentXMLFilter( getRelativePathMapper() );
            parentFilter.setProjectPath( projectFile.getParent() );
            parentFilter.setParent( parent );
            parent.setLexicalHandler( parentFilter );
            parent = parentFilter;
        }

        return new BuildPomXMLFilter( parent );
    }
    
    private XMLReader getXMLReader() throws SAXException, ParserConfigurationException 
    {
        XMLReader xmlReader = Factories.newXMLReader();
        xmlReader.setFeature( "http://xml.org/sax/features/namespaces", true );
        return xmlReader;
    }
    
    /**
     * @return the mapper or {@code null} if relativePaths don't need to be mapped
     */
    protected Function<Path, Optional<RelativeProject>> getRelativePathMapper()
    {
        return null;
    }
    
    protected BiFunction<String, String, String> getDependencyKeyToVersionMapper()
    {
        return null;
    }
}
