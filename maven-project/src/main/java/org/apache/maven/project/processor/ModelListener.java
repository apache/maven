package org.apache.maven.project.processor;

public interface ModelListener
{
    void register( Object xmlNode );

    void fire( Object object );

    boolean isRegistered( Object object );
}
