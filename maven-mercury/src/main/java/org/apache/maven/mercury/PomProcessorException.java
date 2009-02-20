package org.apache.maven.mercury;

public class PomProcessorException
    extends Exception
{
    static final long serialVersionUID = 980457843528974352L;

    /**
     * Default constructor
     */
    public PomProcessorException()
    {
        super();
    }

    /**
     * Constructor
     *
     * @param message exception message
     */
    public PomProcessorException( String message )
    {
        super( message );
    }

    /**
     * Constructor
     *
     * @param message exception message
     */
    public PomProcessorException( String message, Exception e )
    {
        super( message, e );
    }

}
