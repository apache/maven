/*
 * Copyright (c) 2004 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.archetype;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ArchetypeNotFoundException
    extends Exception
{
    public ArchetypeNotFoundException( String message )
    {
        super( message );
    }

    public ArchetypeNotFoundException( Throwable cause )
    {
        super( cause );
    }

    public ArchetypeNotFoundException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
