package org.apache.maven.artifact.transform;

/**
 * @author <a href="oleg@codehaus.org">Oleg Gusakov</a>
 *
 */
public class MetadataGraphTransformationException
    extends Exception
{
	private static final long serialVersionUID = -4029897098314019152L;

	public MetadataGraphTransformationException()
	{
	}

	public MetadataGraphTransformationException(String message)
	{
		super(message);
	}

	public MetadataGraphTransformationException(Throwable cause)
	{
		super(cause);
	}

	public MetadataGraphTransformationException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
