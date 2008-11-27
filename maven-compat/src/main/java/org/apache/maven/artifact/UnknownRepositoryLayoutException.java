package org.apache.maven.artifact;

import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * Exception which is meant to occur when a layout specified for a particular
 * repository doesn't have a corresponding {@link ArtifactRepositoryLayout}
 * component in the current container.
 *
 * @author jdcasey
 */
public class UnknownRepositoryLayoutException
    extends InvalidRepositoryException
{

    private final String layoutId;

    public UnknownRepositoryLayoutException( String repositoryId,
                                             String layoutId )
    {
        super( "Cannot find ArtifactRepositoryLayout instance for: " + layoutId, repositoryId );
        this.layoutId = layoutId;
    }

    public UnknownRepositoryLayoutException( String repositoryId,
                                             String layoutId,
                                             ComponentLookupException e )
    {
        super( "Cannot find ArtifactRepositoryLayout instance for: " + layoutId, repositoryId, e );
        this.layoutId = layoutId;
    }

    public String getLayoutId()
    {
        return layoutId;
    }

}
