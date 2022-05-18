package org.apache.maven.it;

import com.google.common.io.ByteStreams;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.log.StdErrLog;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;

/**
 * An HTTP server that handles all requests on a given port from a specified source, optionally secured using BASIC auth
 * by providing a username and password. The source can either be a URL or a directory. When a request is made the
 * request is satisfied from the provided source.
 *
 * @author Jason van Zyl
 */
public class HttpServer
{
    static {
        Log.initialized();
        Logger rootLogger = Log.getRootLogger();
        if ( rootLogger instanceof StdErrLog )
        {
            ( (StdErrLog) rootLogger ).setLevel( StdErrLog.LEVEL_WARN );
        }
    }

    private final Server server;

    private final StreamSource source;

    private final String username;

    private final String password;

    public HttpServer( int port, String username, String password, StreamSource source )
    {
        this.username = username;
        this.password = password;
        this.source = source;
        this.server = server( port );
    }

    public void start()
        throws Exception
    {
        server.start();
        // server.join();
    }

	public boolean isFailed()
    {
        return server.isFailed();
    }

    public void stop()
        throws Exception
    {
        server.stop();
    }

    public void join()
        throws Exception
    {
        server.join();
    }

    public int port()
    {
        return ( (NetworkConnector) server.getConnectors()[0] ).getLocalPort();
    }

    private Server server( int port )
    {
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads( 500 );
        Server server = new Server( threadPool );
        server.setConnectors( new Connector[]{ new ServerConnector( server ) } );
        server.addBean( new ScheduledExecutorScheduler() );

        ServerConnector connector = (ServerConnector) server.getConnectors()[0];
        connector.setIdleTimeout( 30_000L );
        connector.setPort( port );

        StreamSourceHandler handler = new StreamSourceHandler( source );

        if ( username != null && password != null )
        {
            HashLoginService loginService = new HashLoginService( "Test Server" );
            loginService.putUser( username, new Password( password ), new String[]{ "user" } );
            server.addBean( loginService );

            ConstraintSecurityHandler security = new ConstraintSecurityHandler();
            server.setHandler( security );

            Constraint constraint = new Constraint();
            constraint.setName( "auth" );
            constraint.setAuthenticate( true );
            constraint.setRoles( new String[]{ "user", "admin" } );

            ConstraintMapping mapping = new ConstraintMapping();
            mapping.setPathSpec( "/*" );
            mapping.setConstraint( constraint );

            security.setConstraintMappings( Collections.singletonList( mapping ) );
            security.setAuthenticator( new BasicAuthenticator() );
            security.setLoginService( loginService );
            security.setHandler( handler );
        }
        else
        {
            server.setHandler( handler );
        }
        return server;
    }

    public static HttpServerBuilder builder()
    {
        return new HttpServerBuilder();
    }

    public static class HttpServerBuilder
    {

        private int port;

        private String username;

        private String password;

        private StreamSource source;

        public HttpServerBuilder port( int port )
        {
            this.port = port;
            return this;
        }

        public HttpServerBuilder username( String username )
        {
            this.username = username;
            return this;
        }

        public HttpServerBuilder password( String password )
        {
            this.password = password;
            return this;
        }

        public HttpServerBuilder source( final String source )
        {
            this.source = new StreamSource()
            {
                @Override
                public InputStream stream( String path )
                    throws IOException
                {
                    return new URL( String.format( "%s/%s", source, path ) ).openStream();
                }
            };
            return this;
        }

        public HttpServerBuilder source( final File source )
        {
            this.source = new StreamSource()
            {
                @Override
                public InputStream stream( String path )
                    throws IOException
                {
                    return new FileInputStream( new File( source, path ) );
                }
            };
            return this;
        }

        public HttpServer build()
        {
            return new HttpServer( port, username, password, source );
        }
    }

    public interface StreamSource
    {
        InputStream stream( String path )
            throws IOException;
    }

    public static class StreamSourceHandler
        extends AbstractHandler
    {

        private final StreamSource source;

        public StreamSourceHandler( StreamSource source )
        {
            this.source = source;
        }

        @Override
        public void handle( String target, Request baseRequest, HttpServletRequest request,
                            HttpServletResponse response )
            throws IOException, ServletException
        {
            response.setContentType( "application/octet-stream" );
            response.setStatus( HttpServletResponse.SC_OK );
            try ( InputStream in = source.stream(
                target.substring( 1 ) ); OutputStream out = response.getOutputStream() )
            {
                ByteStreams.copy( in, out );
            }
            baseRequest.setHandled( true );
        }
    }

    public static void main( String[] args )
        throws Exception
    {
        HttpServer server = HttpServer.builder() //
            .port( 0 ) //
            .username( "maven" ) //
            .password( "secret" ) //
            .source( new File( "/tmp/repo" ) ) //
            .build();
        server.start();
    }
}
