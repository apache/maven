package org.apache.maven.exception;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.component.annotations.Component;

/*

All Possible Errors
- bad command line parameter
- malformed settings
- malformed POM
- local repository not writable
- remote repositories not available
- artifact metadata missing
- extension metadata missing
- extension artifact missing
- artifact metadata retrieval problem
- version range violation
- circular dependency
- artifact missing
- artifact retrieval exception
- plugin metadata missing
- plugin metadata retrieval problem
- plugin artifact missing
- plugin artifact retrieval problem
- plugin dependency metadata missing
- plugin dependency metadata retrieval problem
- plugin configuration problem
- plugin execution failure due to something that is know to possibly go wrong (like compilation failure)
- plugin execution error due to something that is not expected to go wrong (the compiler executable missing)
- md5 checksum doesn't match for local artifact, need to redownload this

brett:
- transitive dependency problems - tracking down
- invalid lifecycle phase (maybe same as bad CLI param, though you were talking about embedder too)
- <module> specified is not found
- POM doesn't exist for a goal that requires one
- goal not found in a plugin (probably could list the ones that are)
- parent POM missing (in both the repository + relative path)
brian:
- component not found
- missing goal in plugin
- removing the scripting options from the core

 */

@Component(role=ExceptionHandler.class)
public class DefaultExceptionHandler
    implements ExceptionHandler
{
    public ExceptionSummary handleException( Exception exception )
    {
        String message;
        
        String reference = "http://";
        
        if ( exception instanceof MojoFailureException )
        {
            message = ((MojoFailureException)exception).getLongMessage();
        }
        else if ( exception instanceof MojoExecutionException )
        {
            message = ((MojoExecutionException)exception).getLongMessage();
        }
        else
        {
            message = exception.getMessage();
        }        
        
        return new ExceptionSummary( exception, message, reference );
    }
}
