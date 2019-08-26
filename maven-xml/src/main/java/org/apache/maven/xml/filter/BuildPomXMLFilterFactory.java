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
import java.util.function.Function;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * 
 * @author Robert Scholte
 * 
 * @since 3.7.0
 */
public abstract class BuildPomXMLFilterFactory
{
    public final BuildPomXMLFilter get( Path projectFile )
        throws SAXException, ParserConfigurationException
    {
        XMLReader parent = getParent();
        
        if ( getRelativePathMapper() != null )
        {
            ParentXMLFilter parentFilter = new ParentXMLFilter( getRelativePathMapper() );
            parentFilter.setProjectPath( projectFile.getParent() );
            parentFilter.setParent( parent );
            parent = parentFilter;
        }
        
        CiFriendlyXMLFilter ciFriendlyFilter = new CiFriendlyXMLFilter();
        getChangelist().ifPresent( ciFriendlyFilter::setChangelist  );
        getRevision().ifPresent( ciFriendlyFilter::setRevision );
        getSha1().ifPresent( ciFriendlyFilter::setSha1 );
        
        if ( ciFriendlyFilter.isSet() )
        {
            ciFriendlyFilter.setParent( parent );
            parent = ciFriendlyFilter;
        }
        
        return new BuildPomXMLFilter( parent );
    }
    
    protected XMLReader getParent() throws SAXException, ParserConfigurationException 
    {
        XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        xmlReader.setFeature( "http://xml.org/sax/features/namespaces", true );
        return xmlReader;
    }
    
    // For CIFriendly
    protected abstract Optional<String> getChangelist();
        
    protected abstract Optional<String> getRevision();
    
    protected abstract Optional<String> getSha1();
    
    /**
     * @return the mapper or {@code null} if relativePaths don't need to be mapped
     */
    protected abstract Function<Path, Optional<RelativeProject>> getRelativePathMapper();
}
