package org.apache.maven.repository;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Repository;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:michal@codehaus.org">Michal Maczka</a>
 * @version $Id$
 */
public class RepositoryUtils
{
    public static Set mavenToWagon( List repositories )
    {
        Set repos = new HashSet();

        for ( Iterator i = repositories.iterator(); i.hasNext(); )
        {
            repos.add( mavenRepositoryToWagonRepository( (Repository) i.next() ) );
        }

        return repos;
    }

    public static ArtifactRepository
        mavenRepositoryToWagonRepository( Repository mavenRepository )
    {
        ArtifactRepository retValue = new ArtifactRepository();

        retValue.setUrl( mavenRepository.getUrl() );

        return retValue;
    }
}
