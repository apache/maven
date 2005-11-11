package org.apache.maven.bootstrap.settings;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.bootstrap.util.AbstractReader;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Settings definition.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class Settings
{
    private String localRepository;

    private List mirrors = new ArrayList();

    private List proxies = new ArrayList();

    private Proxy activeProxy = null;

    public Settings()
    {
        localRepository = System.getProperty( "maven.repo.local" );
    }

    public String getLocalRepository()
    {
        return localRepository;
    }

    public void setLocalRepository( String localRepository )
    {
        this.localRepository = localRepository;
    }

    public void addProxy( Proxy proxy )
    {
        proxies.add( proxy );
    }

    public void addMirror( Mirror mirror )
    {
        mirrors.add( mirror );
    }

    public Proxy getActiveProxy()
    {
        if ( activeProxy == null )
        {
            for ( Iterator it = proxies.iterator(); it.hasNext() && activeProxy == null; )
            {
                Proxy proxy = (Proxy) it.next();
                if ( proxy.isActive() )
                {
                    activeProxy = proxy;
                }
            }
        }
        return activeProxy;
    }

    public static Settings read( String userHome, File file )
        throws IOException, ParserConfigurationException, SAXException
    {
        return new Reader( userHome ).parseSettings( file );
    }

    public List getMirrors()
    {
        return mirrors;
    }

    private static class Reader
        extends AbstractReader
    {
        private Proxy currentProxy = null;

        private StringBuffer currentBody = new StringBuffer();

        private Mirror currentMirror;

        private final Settings settings = new Settings();

        private final String userHome;

        private Reader( String userHome )
        {
            this.userHome = userHome;
        }

        public void characters( char[] ch, int start, int length )
            throws SAXException
        {
            currentBody.append( ch, start, length );
        }

        public void endElement( String uri, String localName, String rawName )
            throws SAXException
        {
            if ( "localRepository".equals( rawName ) )
            {
                if ( notEmpty( currentBody.toString() ) )
                {
                    String localRepository = currentBody.toString().trim();
                    if ( settings.getLocalRepository() == null )
                    {
                        settings.setLocalRepository( localRepository );
                    }
                }
                else
                {
                    throw new SAXException(
                        "Invalid profile entry. Missing one or more " + "fields: {localRepository}." );
                }
            }
            else if ( "proxy".equals( rawName ) )
            {
                if ( notEmpty( currentProxy.getHost() ) && notEmpty( currentProxy.getPort() ) )
                {
                    settings.addProxy( currentProxy );
                    currentProxy = null;
                }
                else
                {
                    throw new SAXException( "Invalid proxy entry. Missing one or more fields: {host, port}." );
                }
            }
            else if ( currentProxy != null )
            {
                if ( "active".equals( rawName ) )
                {
                    currentProxy.setActive( Boolean.valueOf( currentBody.toString().trim() ).booleanValue() );
                }
                else if ( "host".equals( rawName ) )
                {
                    currentProxy.setHost( currentBody.toString().trim() );
                }
                else if ( "port".equals( rawName ) )
                {
                    currentProxy.setPort( currentBody.toString().trim() );
                }
                else if ( "username".equals( rawName ) )
                {
                    currentProxy.setUserName( currentBody.toString().trim() );
                }
                else if ( "password".equals( rawName ) )
                {
                    currentProxy.setPassword( currentBody.toString().trim() );
                }
                else if ( "protocol".equals( rawName ) )
                {
                }
                else if ( "nonProxyHosts".equals( rawName ) )
                {
                }
                else
                {
                    throw new SAXException( "Illegal element inside proxy: \'" + rawName + "\'" );
                }
            }
            else if ( "mirror".equals( rawName ) )
            {
                if ( notEmpty( currentMirror.getId() ) && notEmpty( currentMirror.getMirrorOf() ) &&
                    notEmpty( currentMirror.getUrl() ) )
                {
                    settings.addMirror( currentMirror );
                    currentMirror = null;
                }
                else
                {
                    throw new SAXException( "Invalid mirror entry. Missing one or more fields: {id, mirrorOf, url}." );
                }
            }
            else if ( currentMirror != null )
            {
                if ( "id".equals( rawName ) )
                {
                    currentMirror.setId( currentBody.toString().trim() );
                }
                else if ( "mirrorOf".equals( rawName ) )
                {
                    currentMirror.setMirrorOf( currentBody.toString().trim() );
                }
                else if ( "url".equals( rawName ) )
                {
                    currentMirror.setUrl( currentBody.toString().trim() );
                }
                else if ( "name".equals( rawName ) )
                {
                }
                else
                {
                    throw new SAXException( "Illegal element inside proxy: \'" + rawName + "\'" );
                }
            }

            currentBody = new StringBuffer();
        }

        private boolean notEmpty( String test )
        {
            return test != null && test.trim().length() > 0;
        }

        public void startElement( String uri, String localName, String rawName, Attributes attributes )
            throws SAXException
        {
            if ( "proxy".equals( rawName ) )
            {
                currentProxy = new Proxy();
            }
            else if ( "mirror".equals( rawName ) )
            {
                currentMirror = new Mirror();
            }
        }

        public void reset()
        {
            this.currentBody = null;
            this.currentMirror = null;
        }

        public Settings parseSettings( File settingsXml )
            throws IOException, ParserConfigurationException, SAXException
        {
            if ( settingsXml.exists() )
            {
                parse( settingsXml );
            }
            if ( settings.getLocalRepository() == null )
            {
                String m2LocalRepoPath = "/.m2/repository";

                File repoDir = new File( userHome, m2LocalRepoPath );
                if ( !repoDir.exists() )
                {
                    repoDir.mkdirs();
                }

                settings.setLocalRepository( repoDir.getAbsolutePath() );

                System.out.println(
                    "You SHOULD have a ~/.m2/settings.xml file and must contain at least the following information:" );
                System.out.println();

                System.out.println( "<settings>" );
                System.out.println( "  <localRepository>/path/to/your/repository</localRepository>" );
                System.out.println( "</settings>" );

                System.out.println();

                System.out.println( "Alternatively, you can specify -Dmaven.repo.local=/path/to/m2/repository" );

                System.out.println();

                System.out.println( "HOWEVER, since you did not specify a repository path, maven will use: " +
                    repoDir.getAbsolutePath() + " to store artifacts locally." );
            }
            return settings;
        }
    }
}
