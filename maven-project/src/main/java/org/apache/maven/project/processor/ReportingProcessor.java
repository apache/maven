package org.apache.maven.project.processor;

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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class ReportingProcessor extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        
        Model t = (Model) target, c = (Model) child, p = (Model) parent;
        if(p != null && p.getReporting() != null)
        {
            if(t.getReporting() == null)
            {
                t.setReporting( new Reporting() );
            }
            
            copy(p.getReporting(), t.getReporting());
        } 
        
        if(c.getReporting() != null)
        {
            if(t.getReporting() == null)
            {
                t.setReporting( new Reporting() );
            }
            
            copy(c.getReporting(), t.getReporting());
        }     
    }
    
    private static void copy(Reporting source, Reporting target)
    {
        if(source.getOutputDirectory() != null)
        {
            target.setOutputDirectory( source.getOutputDirectory() );

        } 
        
        target.setExcludeDefaults( source.isExcludeDefaults() );
        
        for ( ReportPlugin plugin : source.getPlugins() )
        {
            ReportPlugin match = contains(plugin, target.getPlugins());
            if(match == null)
            {
                target.addPlugin( copyNewPlugin( plugin ) );    
            }
            else
            {
                copyPluginToPlugin(plugin, match);
            }
            
        }
    }
    
    private static ReportPlugin contains(ReportPlugin plugin, List<ReportPlugin> list)
    {
        for(ReportPlugin p :list)
        {
            if(match(p, plugin))
            {
                return p;
            }
        }
        return null;
    }
    
    private static void copyPluginToPlugin(ReportPlugin source, ReportPlugin target)
    {
        if(source.getInherited() != null)
        {
            target.setInherited( source.getInherited() );
        }
        
        if(source.getVersion() != null)
        {
            target.setVersion( source.getVersion() );
        }
        
        if(source.getConfiguration() != null)
        {
            if(target.getConfiguration() != null)
            {
                target.setConfiguration( Xpp3Dom.mergeXpp3Dom( (Xpp3Dom) source.getConfiguration(), (Xpp3Dom) target.getConfiguration() ));     
            }
            else
            {
                target.setConfiguration( source.getConfiguration() );
            }                       
        }
        
        for(ReportSet rs : source.getReportSets())
        {
            ReportSet r = new ReportSet();
            r.setId( rs.getId() );
            r.setInherited( rs.getInherited() );
            r.setReports( new ArrayList<String>(rs.getReports()) );
            r.setConfiguration( rs.getConfiguration() );
            target.addReportSet( r );
        }        
    }
    
    private static ReportPlugin copyNewPlugin(ReportPlugin plugin)
    {
        ReportPlugin rp = new ReportPlugin();
        rp.setArtifactId( plugin.getArtifactId() );
        rp.setGroupId( plugin.getGroupId() );
        rp.setInherited( plugin.getInherited() );
        rp.setVersion( plugin.getVersion() );
        rp.setConfiguration( plugin.getConfiguration() );
        
        for(ReportSet rs : plugin.getReportSets())
        {
            ReportSet r = new ReportSet();
            r.setId( rs.getId() );
            r.setInherited( rs.getInherited() );
            r.setReports( new ArrayList<String>(rs.getReports()) );
            r.setConfiguration( rs.getConfiguration() );
            rp.addReportSet( r );
        }
        return rp;
    }
    
    private static boolean match( ReportPlugin d1, ReportPlugin d2 )
    {
        return getId( d1 ).equals( getId( d2 ));
    }

    private static String getId( ReportPlugin d )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( d.getGroupId() ).append( ":" ).append( d.getArtifactId() ).append( ":" );
        return sb.toString();
    }      
}
