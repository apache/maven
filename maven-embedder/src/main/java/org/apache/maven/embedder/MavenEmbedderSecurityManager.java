package org.apache.maven.embedder;

/** @author Jason van Zyl */
public class MavenEmbedderSecurityManager
    extends SecurityManager
{
    public void checkPropertyAccess( String key )
    {
        super.checkPropertyAccess( key );

        throw new SecurityException( "You cannot modify any System properties!" );
    }
}
