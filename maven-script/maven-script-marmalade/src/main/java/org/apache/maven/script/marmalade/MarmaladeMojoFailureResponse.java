package org.apache.maven.script.marmalade;

import org.apache.maven.plugin.FailureResponse;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author jdcasey
 *
 * Created on Feb 8, 2005
 */
public class MarmaladeMojoFailureResponse extends FailureResponse
{
    
    private final String scriptLocation;
    private final MarmaladeExecutionException error;

    public MarmaladeMojoFailureResponse( String scriptLocation, MarmaladeExecutionException error )
    {
        super(scriptLocation);
        
        this.scriptLocation = scriptLocation;
        this.error = error;
    }

    public String shortMessage()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Script: ").append(scriptLocation).append(" failed to execute.");
        buffer.append("\nError: ").append(error.getLocalizedMessage());
        return buffer.toString();
    }

    public String longMessage()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Script: ").append(scriptLocation).append(" failed to execute.");
        buffer.append("\nError:\n");
        
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter(sWriter);
        
        error.printStackTrace(pWriter);
        
        buffer.append(sWriter.toString());
        
        pWriter.close();
        
        return buffer.toString();
    }

}
