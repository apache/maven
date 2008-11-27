package org.apache.maven.artifact.resolver.metadata;

public enum MetadataResolutionRequestTypeEnum
{
      tree( 1 )
    , graph( 2 )
    , classpathCompile( 3 )
    , classpathTest( 4 )
    , classpathRuntime( 5 )
    , versionedGraph( 6 )
    , scopedGraph( 7 )
    ;

    private int id;

    // Constructor
    MetadataResolutionRequestTypeEnum( int id )
    {
        this.id = id;
    }

    int getId()
    {
        return id;
    }
}
