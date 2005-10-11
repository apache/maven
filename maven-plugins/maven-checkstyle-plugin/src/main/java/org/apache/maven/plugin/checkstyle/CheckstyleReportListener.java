package org.apache.maven.plugin.checkstyle;

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

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.AutomaticBean;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.Map;
import java.util.List;
import java.io.File;

import org.codehaus.plexus.util.StringUtils;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id: DependenciesReport.java,v 1.2 2005/02/23 00:08:02 brett Exp $
 */
public class CheckstyleReportListener
    extends AutomaticBean
    implements AuditListener
{
    private File sourceDirectory;

    private Map files;

    private String currentFile;

    private List events;

    private SeverityLevel severityLevel;

    public CheckstyleReportListener( File sourceDirectory )
    {
        this.sourceDirectory = sourceDirectory;
    }

    public void setSeverityLevelFilter( SeverityLevel severityLevel )
    {
        this.severityLevel = severityLevel;
    }

    public SeverityLevel getSeverityLevelFilter()
    {
        return severityLevel;
    }

    public void auditStarted( AuditEvent event )
    {
        setFiles( new TreeMap() );
    }

    public void auditFinished( AuditEvent event )
    {
        //do nothing
    }

    public void fileStarted( AuditEvent event )
    {
        currentFile = StringUtils.substring( event.getFileName(), sourceDirectory.getPath().length() + 1 );
        currentFile = StringUtils.replace( currentFile, "\\", "/" );

        if ( !getFiles().containsKey( currentFile ) )
            getFiles().put( currentFile, new LinkedList() );

        events = (LinkedList) getFiles().get( currentFile );
    }

    public void fileFinished( AuditEvent event )
    {
        getFiles().put( currentFile, events );
        currentFile = null;
    }

    public void addError( AuditEvent event )
    {
        if ( SeverityLevel.IGNORE.equals( event.getSeverityLevel() ) ) return;

        if ( severityLevel == null || severityLevel.equals( event.getSeverityLevel() ) )
        {
            events.add( event );
        }
    }

    public void addException( AuditEvent event, Throwable throwable )
    {
        //Do Nothing
    }

    public Map getFiles()
    {
        return files;
    }

    public void setFiles( Map files )
    {
        this.files = files;
    }
}

