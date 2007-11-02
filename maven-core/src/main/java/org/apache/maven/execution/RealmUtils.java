package org.apache.maven.execution;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

public final class RealmUtils
{

    private RealmUtils()
    {
    }

    public static String createExtensionRealmId( Artifact extensionArtifact )
    {
        return "/extensions/" + extensionArtifact.getGroupId() + ":"
               + extensionArtifact.getArtifactId() + ":" + extensionArtifact.getVersion();
    }

    public static String createProjectId( String projectGroupId,
                                          String projectArtifactId,
                                          String projectVersion )
    {
        return "/projects/" + projectGroupId + ":" + projectArtifactId + ":" + projectVersion;
    }

    public static String createPluginRealmId( Plugin plugin )
    {
        StringBuffer id = new StringBuffer().append( "/plugins/" )
                                            .append( plugin.getGroupId() )
                                            .append( ':' )
                                            .append( plugin.getArtifactId() )
                                            .append( ':' )
                                            .append( plugin.getVersion() );

        StringBuffer depId = new StringBuffer();

        Collection dependencies = plugin.getDependencies();
        if ( ( dependencies != null ) && !dependencies.isEmpty() )
        {
            dependencies = new LinkedHashSet( dependencies );

            for ( Iterator it = dependencies.iterator(); it.hasNext(); )
            {
                Dependency dep = (Dependency) it.next();

                depId.append( dep.getGroupId() )
                     .append( ':' )
                     .append( dep.getArtifactId() )
                     .append( ';' )
                     .append( dep.getVersion() );

                if ( it.hasNext() )
                {
                    depId.append( ',' );
                }
            }
        }
        else
        {
            depId.append( '0' );
        }

        id.append( '@' ).append( depId.toString().hashCode() );

        return id.toString();
    }

}
