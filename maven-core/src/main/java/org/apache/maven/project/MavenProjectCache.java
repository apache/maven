package org.apache.maven.project;

public interface MavenProjectCache
{
    MavenProject get( String absolutePath );

    void put( String absolutePath, MavenProject project );
    
    int size();
}
