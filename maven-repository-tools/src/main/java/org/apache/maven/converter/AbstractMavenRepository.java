package org.apache.maven.converter;

import java.io.File;

/*
 * LICENSE
 */

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public abstract class AbstractMavenRepository
    implements MavenRepository
{
    private File repository;

    public void setRepository( File repository )
    {
        this.repository = repository;
    }

    public File getRepository()
    {
        return repository;
    }
}
