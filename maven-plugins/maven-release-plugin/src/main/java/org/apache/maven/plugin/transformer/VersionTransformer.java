package org.apache.maven.plugin.transformer;

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

import java.util.Iterator;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.dom4j.Element;
import org.dom4j.Node;

/**
 *
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 *
 * @version $Id: VersionTransformer.java 115421 2004-06-01 02:20:18Z dion $
 */
public class VersionTransformer
    extends AbstractPomTransformer
{
    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public String selectProjectNodeXPathExpression()
    {
        return "/project";
    }

    public String selectDependenciesNodesXPathExpression()
    {
        return "/project/dependencies/dependency";
    }

    public String selectPluginsNodesXPathExpression()
    {
        return "/project/build/plugins/plugin";
    }

    public void transformNode( Node node )
    {
        if ( selectProjectNodeXPathExpression().equals( node.getPath() ) )
        {
            Element project = (Element) node;

            Node version = node.selectSingleNode( "version" );

            if ( version != null )
            {
                version.setText( getUpdatedModel().getVersion() );
            }
            else
            {
                project.addElement( "version" ).addText( getUpdatedModel().getVersion() );
            }
        }
        else if ( selectDependenciesNodesXPathExpression().equals( node.getPath() ) )
        {
            Element dependency = (Element) node;

            Node groupId = node.selectSingleNode( "groupId" );

            Node artifactId = node.selectSingleNode( "artifactId" );

            Node type = node.selectSingleNode( "type" );

            String typeText = "jar";

            if ( type != null )
            {
                typeText = type.getText();
            }
            Node version = node.selectSingleNode( "version" );

            if ( version != null )
            {
                version.setText( getDependency( groupId.getText(), artifactId.getText(), typeText ).getVersion() );
            }
            else
            {
                dependency.addElement( "version" ).addText( getDependency( groupId.getText(), artifactId.getText(),
                                                                           type.getText() ).getVersion() );
            }
        }
        else
        {
            Element plugin = (Element) node;

            Node groupId = node.selectSingleNode( "groupId" );

            String groupIdText = "org.apache.maven.plugins";

            if ( groupId != null )
            {
                groupIdText = groupId.getText();
            }

            Node artifactId = node.selectSingleNode( "artifactId" );

            Node version = node.selectSingleNode( "version" );

            Plugin p = getPlugin( groupIdText, artifactId.getText() );

            if ( groupId != null )
            {
                groupId.setText( p.getGroupId() );
            }
            else
            {
                plugin.addElement( "groupId" ).addText( p.getGroupId() );
            }
            if ( version != null )
            {
                version.setText( p.getVersion() );
            }
            else
            {
                plugin.addElement( "version" ).addText( p.getVersion() );
            }
        }
    }

    private Dependency getDependency( String groupId, String artifactId, String type )
    {
        for ( Iterator i = getUpdatedModel().getDependencies().iterator(); i.hasNext(); )
        {
            Dependency dependency = (Dependency) i.next();

            if ( dependency.getGroupId().equals( groupId ) && dependency.getArtifactId().equals( artifactId )
                && dependency.getType().equals( type ) )
            {
                return dependency;
            }
        }

        return null;
    }

    private Plugin getPlugin( String groupId, String artifactId )
    {
        for ( Iterator i = getUpdatedModel().getBuild().getPlugins().iterator(); i.hasNext(); )
        {
            Plugin plugin = (Plugin) i.next();

            if ( plugin.getGroupId().equals( groupId ) && plugin.getArtifactId().equals( artifactId ) )
            {
                return plugin;
            }
        }

        return null;
    }

    public Node getTransformedNode( Node node )
        throws Exception
    {
        throw new UnsupportedOperationException( "getTransformedNode not implemented" );
    }
}
