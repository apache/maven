package org.apache.maven.converter;

/*
 * LICENSE
 */

import java.io.File;
import java.util.Iterator;

import org.apache.maven.model.Model;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public interface MavenRepository
{
    String ROLE = MavenRepository.class.getName();

    void setRepository( File repository );

    File getRepository();

    Iterator getArtifactsByType( String type )
        throws Exception;

    String getPomForArtifact( String artifactPath )
        throws Exception;

    public void installArtifact( File artifact, Model model )
        throws Exception;
}
