/*
 * Copyright (c) 2004 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.archetype;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ArchetypeDescriptorException
    extends Exception
{
    public ArchetypeDescriptorException( String message )
    {
        super( message );
    }

    public ArchetypeDescriptorException( Throwable cause )
    {
        super( cause );
    }

    public ArchetypeDescriptorException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
