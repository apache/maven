package org.apache.maven.cli;

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.cli.Option;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;

/**
 * Pseudo test to generate documentation fragment about supported CLI options. TODO such documentation generation code
 * should not be necessary as unit test but should be run during site generation (Velocity? Doxia macro?)
 */
public class CLIManagerDocumentationTest
{
    private final static String LS = System.lineSeparator();

    private static class OptionComparator
        implements Comparator<Option>
    {
        public int compare( Option opt1, Option opt2 )
        {
            String s1 = opt1.getOpt() != null ? opt1.getOpt() : opt1.getLongOpt();
            String s2 = opt2.getOpt() != null ? opt2.getOpt() : opt2.getLongOpt();
            return s1.compareToIgnoreCase( s2 );
        }
    }

    private static class CLIManagerExtension
        extends CLIManager
    {
        public Collection<Option> getOptions()
        {
            List<Option> optList = new ArrayList<>( options.getOptions() );
            Collections.sort( optList, new OptionComparator() );
            return optList;
        }
    }

    public String getOptionsAsHtml()
    {
        StringBuilder sb = new StringBuilder( 512 );
        boolean a = true;
        sb.append( "<table border='1' class='zebra-striped'><tr class='a'><th><b>Options</b></th><th><b>Description</b></th></tr>" );
        for ( Option option : new CLIManagerExtension().getOptions() )
        {
            a = !a;
            sb.append( "<tr class='" ).append( a ? 'a' : 'b' ).append( "'><td><code>-<a name='" );
            sb.append( option.getOpt() );
            sb.append( "'>" );
            sb.append( option.getOpt() );
            sb.append( "</a>,--<a name='" );
            sb.append( option.getLongOpt() );
            sb.append( "'>" );
            sb.append( option.getLongOpt() );
            sb.append( "</a>" );
            if ( option.hasArg() )
            {
                if ( option.hasArgName() )
                {
                    sb.append( " &lt;" ).append( option.getArgName() ).append( "&gt;" );
                }
                else
                {
                    sb.append( ' ' );
                }
            }
            sb.append( "</code></td><td>" );
            sb.append( option.getDescription() );
            sb.append( "</td></tr>" );
            sb.append( LS );
        }
        sb.append( "</table>" );
        return sb.toString();
    }

    @Test
    public void testOptionsAsHtml()
        throws IOException
    {
        File options = new File( "target/test-classes/options.html" );
        FileUtils.fileWrite( options, "UTF-8", getOptionsAsHtml() );
    }

}
