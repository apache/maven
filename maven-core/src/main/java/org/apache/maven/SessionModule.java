package org.apache.maven;

import javax.inject.Named;

import org.apache.maven.execution.MavenSession;

import com.google.inject.AbstractModule;

@Named
public class SessionModule extends AbstractModule
{            
    @Override
    protected void configure()
    {
        SessionScope scope = new SessionScope();
        bindScope( SessionScoped.class, scope );
        bind( SessionScope.class).toInstance( scope );
        bind( MavenSession.class ).toProvider( SessionScope.<MavenSession> seededKeyProvider() ).in( scope );
    }
}
