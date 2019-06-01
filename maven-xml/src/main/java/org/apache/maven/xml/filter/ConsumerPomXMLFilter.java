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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import org.xml.sax.helpers.XMLFilterImpl;

/**
 * XML Filter to transform pom.xml to consumer pom.
 * This often means stripping of build-specific information.
 * When extra information is required during filtering it is probably a member of the BuildPomXMLFilter
 * 
 * This filter is used at 2 locations:
 * - {@link org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory} when publishing pom files.
 * - TODO ???Class when a reactor module is used as dependency. This ensures consistency of dependency handling
 * 
 * @author Robert Scholte
 * @since 3.7.0
 */
public class ConsumerPomXMLFilter extends XMLFilterImpl
{
    private final XMLFilter rootFilter;

    // only for testing purpose
    ConsumerPomXMLFilter() throws SAXException, ParserConfigurationException
    {
        this( SAXParserFactory.newInstance().newSAXParser().getXMLReader() );
    }
    
    // only for testing purpose
    ConsumerPomXMLFilter( XMLReader parent )
    {
        this.rootFilter = new XMLFilterImpl( parent );
        
        applyFilters();
    }
    
    public ConsumerPomXMLFilter( BuildPomXMLFilter buildPomXMLFilter )
    {
        this.rootFilter = buildPomXMLFilter;
        
        applyFilters();
    }
    
    private void applyFilters()
    {
        // Ensure that xs:any elements aren't touched by next filters
        XMLFilter filter = new FastForwardFilter( rootFilter );
        
        // Strip modules
        filter = new ModulesXMLFilter( filter );
        // Adjust relativePath
        filter = new RelativePathXMLFilter( filter );
        
        // maybe more to follow
        
        super.setParent( filter );
    }
    
    @Override
    public void setParent( XMLReader parent )
    {
        rootFilter.setParent( parent );
    }
}
