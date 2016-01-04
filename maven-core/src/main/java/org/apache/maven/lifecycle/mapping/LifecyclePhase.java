package org.apache.maven.lifecycle.mapping;

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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Comma delimited multiple goals. The goal may be specified using:
 * <ul>
 * <li>&lt;groupId&gt;:&lt;artifactId&gt;:&lt;goal&gt;</li>
 * <li>&lt;groupId&gt;:&lt;artifactId&gt;:&lt;version&gt;:&lt;goal&gt;
 * <li>&lt;groupId&gt;:&lt;artifactId&gt;:&lt;version&gt;:&lt;goal&gt;[&lt;configuration&gt;]
 * </li>
 */
public class LifecyclePhase
{
    private List<LifecycleMojo> mojos;
    
    public LifecyclePhase()
    {
    }
    
    public LifecyclePhase( String goals )
    {
        set( goals );
    }
    
    public List<LifecycleMojo> getMojos()
    {
        return mojos;
    }
    
    public void setMojos( List<LifecycleMojo> mojos )
    {
        this.mojos = mojos;
    }
    
    public void set( String goals )
    {
        mojos = new ArrayList<>();
        
        if ( StringUtils.isNotEmpty( goals ) )
        {
            // match <groupId>:<artifactId>:<version>:<goal>
            String goalRegex = "(?<goal>[^:\\[,\\s]+(?::[^:\\[,\\s]+){2,3})";

            // match [<configuration>]
            String configurationRegex = "(?:\\[(?<configuration>(?:[^\\]]|(?:(?<=/)\\]))*)\\])?";

            Pattern pattern = Pattern.compile( goalRegex + configurationRegex );
            Matcher matcher = pattern.matcher( goals );

            while ( matcher.find() )
            {
                String goal = matcher.group( "goal" );
                String configuration = matcher.group( "configuration" );

                LifecycleMojo lifecycleMojo = new LifecycleMojo();
                lifecycleMojo.setGoal( goal );

                if ( configuration != null )
                {
                    // replace escaped /]
                    configuration = configuration.replace( "/]", "]" );

                    // add root configuration tag
                    configuration = "<configuration>" + configuration + "</configuration>";

                    try
                    {
                        lifecycleMojo.setConfiguration( Xpp3DomBuilder.build( new StringReader( configuration ) ) );
                    }
                    catch ( XmlPullParserException | IOException e )
                    {
                        throw new IllegalArgumentException( e );
                    }
                }
                mojos.add( lifecycleMojo );
            }
        }
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        List<LifecycleMojo> mojos = getMojos();
        if ( mojos != null )
        {
            for ( LifecycleMojo mojo: mojos )
            {
                if ( first )
                {
                    first = false;
                }
                else
                {
                    sb.append( "," );
                }
                sb.append( mojo.getGoal() );
            }
        }
        return sb.toString();
    }
    
    @Deprecated
    public static Map<String, String> toLegacyMap( Map<String, LifecyclePhase> lifecyclePhases )
    {
        if ( lifecyclePhases == null )
        {
            return null;
        }
        
        if ( lifecyclePhases.isEmpty() )
        {
            return Collections.emptyMap();
        }
        
        Map<String, String> phases = new LinkedHashMap<>();
        for ( Map.Entry<String, LifecyclePhase> e: lifecyclePhases.entrySet() )
        {
            phases.put( e.getKey(), e.getValue().toString() );
        }
        return phases;
    }
    
}
