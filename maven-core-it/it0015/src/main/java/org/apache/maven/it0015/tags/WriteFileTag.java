package org.apache.maven.it0015.tags;

import org.codehaus.marmalade.model.AbstractMarmaladeTag;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;
import org.codehaus.marmalade.runtime.TagExecutionException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author jdcasey
 */
public class WriteFileTag
    extends AbstractMarmaladeTag
{
    
    public static final String FILE_ATTR = "path";

    protected boolean alwaysProcessChildren()
    {
        return false;
    }
    
    protected void doExecute( MarmaladeExecutionContext context ) throws MarmaladeExecutionException
    {
        String content = (String) getBody(context, String.class);
        String filename = (String) requireTagAttribute(FILE_ATTR, String.class, context);
        
        File file = new File(filename);
        File dir = file.getParentFile();
        if(dir != null && !dir.exists())
        {
            dir.mkdirs();
        }
        
        FileOutputStream fOut = null;
        try
        {
            fOut = new FileOutputStream(file);
            fOut.write(content.getBytes());
        }
        catch ( IOException e )
        {
            throw new TagExecutionException(getTagInfo(), "Cannot write content to file: " + file, e);
        }
        finally
        {
            if(fOut != null)
            {
                try
                {
                    fOut.flush();
                    fOut.close();
                }
                catch(Exception e)
                {
                }
            }
        }
    }
}
