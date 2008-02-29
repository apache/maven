package org.apache.maven.monitor.event;

public interface MavenEmbedderLifecycleMonitor
{

    public void embedderInitialized( long timestamp );

    public void embedderStopped( long timestamp );

    public void embedderMethodStarted( String method,
                                          long timestamp );

    public void embedderMethodEnded( String method,
                                        long timestamp );

}