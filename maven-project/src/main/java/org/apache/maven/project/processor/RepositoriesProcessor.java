package org.apache.maven.project.processor;

import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;

public class RepositoriesProcessor extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        
        Model t = (Model) target, c = (Model) child, p = (Model) parent;
        copy(c.getPluginRepositories(), t.getPluginRepositories());
        
        copy( c.getRepositories(), t.getRepositories() );
        if(p != null)
        {
            copy( p.getRepositories(), t.getRepositories() );   
            copy( p.getPluginRepositories(), t.getPluginRepositories() );  
        }     
    }
    
    private static void copy(List<Repository> sources, List<Repository> targets)
    {
        for(Repository repository : sources)
        {
            Repository r = new Repository();
            r.setId( repository.getId() );
            r.setLayout( repository.getLayout() );
            r.setName( repository.getName() );
            r.setUrl( repository.getUrl() );
            if(repository.getReleases() != null)
            {
                r.setReleases( copy(repository.getReleases()) );
            }
            if(repository.getSnapshots() != null)       
            {
                r.setSnapshots( copy(repository.getSnapshots()) );
            }  
            
            targets.add( r );
        }
    }
    
    private static RepositoryPolicy copy(RepositoryPolicy policy)
    {
        RepositoryPolicy p = new RepositoryPolicy();
        p.setChecksumPolicy( policy.getChecksumPolicy() );
        p.setEnabled( policy.isEnabled() );
        p.setUpdatePolicy( policy.getUpdatePolicy() );
        return p;
    }
}
