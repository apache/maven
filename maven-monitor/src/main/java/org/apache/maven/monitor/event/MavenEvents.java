package org.apache.maven.monitor.event;

/**
 * @author jdcasey
 */
public final class MavenEvents
{

    public static final String PHASE_EXECUTION = "phase-execute";
    public static final String MOJO_EXECUTION = "mojo-execute";
    public static final String PROJECT_EXECUTION = "project-execute";
    public static final String REACTOR_EXECUTION = "reactor-execute";
    
    public static final String[] ALL_EVENTS = {
        PHASE_EXECUTION,
        MOJO_EXECUTION,
        PROJECT_EXECUTION,
        REACTOR_EXECUTION
    };
    
    public static final String[] NO_EVENTS = {};
    
    private MavenEvents()
    {
    }

}