package org.apache.maven.cli;

import org.apache.maven.embedder.AbstractMavenEmbedderLogger;

import java.util.ArrayList;
import java.util.List;

public class TestEmbedderLogger
    extends AbstractMavenEmbedderLogger
{

    private List debugMessages = new ArrayList();

    private List errorMessages = new ArrayList();

    private List fatalErrorMessages = new ArrayList();

    private List infoMessages = new ArrayList();

    private List warnMessages = new ArrayList();

    private List debugErrors = new ArrayList();

    private List errors = new ArrayList();

    private List fatalErrors = new ArrayList();

    private List infoErrors = new ArrayList();

    private List warnErrors = new ArrayList();

    public void close()
    {
    }

    public void debug( String message,
                       Throwable throwable )
    {
        log( "[debug] ", message, throwable, debugMessages, debugErrors );
    }

    private void log( String header,
                      String message, Throwable throwable,
                      List messages,
                      List errors )
    {
        if ( message != null )
        {
            messages.add( message );
            System.out.println( header + message );
        }

        if ( throwable != null )
        {
            errors.add( throwable );
            throwable.printStackTrace( System.out );
        }
    }

    public void error( String message,
                       Throwable throwable )
    {
        log( "[error] ", message, throwable, errorMessages, errors );
    }

    public void fatalError( String message,
                            Throwable throwable )
    {
        log( "[fatal] ", message, throwable, fatalErrorMessages, fatalErrors );
    }

    public void info( String message,
                      Throwable throwable )
    {
        log( "[info] ", message, throwable, infoMessages, infoErrors );
    }

    public void warn( String message,
                      Throwable throwable )
    {
        log( "[warn] ", message, throwable, warnMessages, warnErrors );
    }

    public List getDebugMessages()
    {
        return debugMessages;
    }

    public List getErrorMessages()
    {
        return errorMessages;
    }

    public List getFatalErrorMessages()
    {
        return fatalErrorMessages;
    }

    public List getInfoMessages()
    {
        return infoMessages;
    }

    public List getWarnMessages()
    {
        return warnMessages;
    }

    public List getDebugErrors()
    {
        return debugErrors;
    }

    public List getErrorErrors()
    {
        return errors;
    }

    public List getFatalErrors()
    {
        return fatalErrors;
    }

    public List getInfoErrors()
    {
        return infoErrors;
    }

    public List getWarnErrors()
    {
        return warnErrors;
    }

}
