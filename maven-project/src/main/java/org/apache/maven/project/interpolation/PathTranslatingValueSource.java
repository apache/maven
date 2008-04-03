package org.apache.maven.project.interpolation;

import org.apache.maven.project.path.PathTranslator;
import org.codehaus.plexus.interpolation.AbstractFunctionValueSourceWrapper;
import org.codehaus.plexus.interpolation.ValueSource;

import java.io.File;
import java.util.List;

public class PathTranslatingValueSource
    extends AbstractFunctionValueSourceWrapper
{

    private final List unprefixedPathKeys;
    private final File projectDir;
    private final PathTranslator pathTranslator;

    protected PathTranslatingValueSource( ValueSource valueSource, List unprefixedPathKeys, File projectDir, PathTranslator pathTranslator )
    {
        super( valueSource );
        this.unprefixedPathKeys = unprefixedPathKeys;
        this.projectDir = projectDir;
        this.pathTranslator = pathTranslator;
    }

    protected Object executeFunction( String expression,
                                      Object value )
    {
        if ( projectDir != null && value != null && unprefixedPathKeys.contains( expression ) )
        {
            return pathTranslator.alignToBaseDirectory( String.valueOf( value ), projectDir );
        }

        return value;
    }

}
