package org.codehaus.doxia.site.renderer;
import java.io.PrintStream;
import java.io.PrintWriter;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Emmanuel Venisse
 */
public class RendererException
    extends org.apache.maven.doxia.siterenderer.RendererException
{
    private org.apache.maven.doxia.siterenderer.RendererException re;

    public RendererException( org.apache.maven.doxia.siterenderer.RendererException re )
    {
        super( "" );
        
        this.re = re;
    }
    
    public RendererException( String message )
    {
        super( message );
    }

    public RendererException( String message, Throwable t )
    {
        super( message, t );
    }

    public synchronized Throwable fillInStackTrace()
    {
        if ( re != null )
        {
            return re.fillInStackTrace();
        }
        
        return super.fillInStackTrace();
    }

    public Throwable getCause()
    {
        if ( re != null )
        {
            return re.getCause();
        }
        
        return super.getCause();
    }

    public String getLocalizedMessage()
    {
        if ( re != null )
        {
            return re.getLocalizedMessage();
        }
        
        return super.getLocalizedMessage();
    }

    public String getMessage()
    {
        if ( re != null )
        {
            return re.getMessage();
        }
        
        return super.getMessage();
    }

    public StackTraceElement[] getStackTrace()
    {
        if ( re != null )
        {
            return re.getStackTrace();
        }
        
        return super.getStackTrace();
    }

    public synchronized Throwable initCause( Throwable cause )
    {
        if ( re != null )
        {
            return re.initCause( cause );
        }
        
        return super.initCause( cause );
    }

    public void printStackTrace()
    {
        if ( re != null )
        {
            re.printStackTrace();
        }
        else
        {
            super.printStackTrace();
        }
    }

    public void printStackTrace( PrintStream stream )
    {
        if ( re != null )
        {
            re.printStackTrace( stream );
        }
        else
        {
            super.printStackTrace( stream );
        }
    }

    public void printStackTrace( PrintWriter writer )
    {
        if ( re != null )
        {
            re.printStackTrace( writer );
        }
        else
        {
            super.printStackTrace( writer );
        }
    }

    public void setStackTrace( StackTraceElement[] stackTrace )
    {
        if ( re != null )
        {
            re.setStackTrace(stackTrace);
        }
        else
        {
            super.setStackTrace(stackTrace);
        }
    }

    public String toString()
    {
        if ( re != null )
        {
            return re.toString();
        }
        else
        {
            return super.toString();
        }
    }
}
