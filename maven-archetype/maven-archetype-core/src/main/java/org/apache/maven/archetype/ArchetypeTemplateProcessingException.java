/*
 * Copyright (c) 2004 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.archetype;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ArchetypeTemplateProcessingException
    extends Exception
{
    public ArchetypeTemplateProcessingException( String message )
    {
        super( message );
    }

    public ArchetypeTemplateProcessingException( Throwable cause )
    {
        super( cause );
    }

    public ArchetypeTemplateProcessingException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
