package org.apache.maven.plugins.release.helpers;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

public class ReleaseProgressTracker
{

    private static final String RELEASE_PROPERTIES = "release.properties";

    private static final String USERNAME = "maven.username";

    private static final String SCM_TAG = "scm.tag";

    private static final String SCM_URL = "scm.url";

    private static final String SCM_TAG_BASE = "scm.tag-base";

    private static final String SCM_PASSWORD = "scm.password";

    private static final String CHECKPOINT_PREFIX = "checkpoint.";

    public static final String CP_INITIALIZED = "initialized";

    public static final String CP_LOCAL_MODIFICATIONS_CHECKED = "local-modifications-checked";

    public static final String CP_SNAPSHOTS_CHECKED = "snapshots-checked";

    public static final String CP_POM_TRANSFORMED_FOR_RELEASE = "transformed-pom-for-release";

    public static final String CP_GENERATED_RELEASE_POM = "generated-release-pom";

    public static final String CP_CHECKED_IN_RELEASE_VERSION = "checked-in-release-version";

    public static final String CP_TAGGED_RELEASE = "tagged-release";

    public static final String CP_POM_TRANSORMED_FOR_DEVELOPMENT = "transform-pom-for-development";

    public static final String CP_REMOVED_RELEASE_POM = "removed-release-pom";

    public static final String CP_CHECKED_IN_DEVELOPMENT_VERSION = "check-in-development-version";

    public static final String CP_PREPARED_RELEASE = "prepared-release";

    private Properties releaseProperties;

    private boolean resumeAtCheckpoint = false;

    private ReleaseProgressTracker()
    {
    }

    public static ReleaseProgressTracker loadOrCreate( String basedir )
        throws IOException
    {
        ReleaseProgressTracker tracker = null;

        if ( new File( basedir, RELEASE_PROPERTIES ).exists() )
        {
            tracker = load( basedir );
        }
        else
        {
            tracker = new ReleaseProgressTracker();
        }

        return tracker;
    }

    public static ReleaseProgressTracker load( String basedir )
        throws IOException
    {
        File releasePropertiesFile = new File( basedir, RELEASE_PROPERTIES );

        ReleaseProgressTracker tracker = new ReleaseProgressTracker();

        InputStream inStream = null;

        try
        {
            inStream = new FileInputStream( releasePropertiesFile );

            Properties rp = new Properties();

            rp.load( inStream );

            tracker.releaseProperties = rp;
        }
        finally
        {
            IOUtil.close( inStream );
        }

        return tracker;
    }

    public static String getReleaseProgressFilename()
    {
        return RELEASE_PROPERTIES;
    }

    private void checkInitialized()
    {
        if ( releaseProperties == null )
        {
            releaseProperties = new Properties();
        }
    }

    private void checkLoaded()
    {
        if ( releaseProperties == null )
        {
            throw new IllegalStateException( "You must load this instance before reading from it." );
        }
    }

    public void setUsername( String username )
    {
        checkInitialized();

        releaseProperties.setProperty( USERNAME, username );
    }

    public String getUsername()
    {
        checkLoaded();

        return releaseProperties.getProperty( USERNAME );
    }

    public void setScmTag( String scmTag )
    {
        checkInitialized();

        releaseProperties.setProperty( SCM_TAG, scmTag );
    }

    public String getScmTag()
    {
        checkLoaded();

        return releaseProperties.getProperty( SCM_TAG );
    }

    public void setScmUrl( String scmUrl )
    {
        checkInitialized();

        releaseProperties.setProperty( SCM_URL, scmUrl );
    }

    public String getScmUrl()
    {
        checkLoaded();

        return releaseProperties.getProperty( SCM_URL );
    }

    public void setScmTagBase( String tagBase )
    {
        checkInitialized();

        releaseProperties.setProperty( SCM_TAG_BASE, tagBase );
    }

    public String getScmTagBase()
    {
        checkLoaded();

        return releaseProperties.getProperty( SCM_TAG_BASE );
    }

    public void setPassword( String password )
    {
        checkInitialized();

        releaseProperties.setProperty( SCM_PASSWORD, password );
    }

    public String getPassword()
    {
        checkInitialized();

        return releaseProperties.getProperty( SCM_PASSWORD );
    }

    public void verifyResumeCapable()
        throws MojoExecutionException
    {
        if ( getUsername() == null || getScmTag() == null || getScmTagBase() == null || getScmUrl() == null )
        {
            throw new MojoExecutionException( "Missing release preparation information. Failed to resume" );
        }
    }

    public void checkpoint( String basedir, String pointName )
        throws IOException
    {
        setCheckpoint( pointName );

        File releasePropertiesFile = new File( basedir, RELEASE_PROPERTIES );

        FileOutputStream outStream = null;

        try
        {
            outStream = new FileOutputStream( releasePropertiesFile );

            releaseProperties.store( outStream, "Generated by Release Plugin on: " + new Date() );
        }
        finally
        {
            IOUtil.close( outStream );
        }
    }

    private void setCheckpoint( String pointName )
    {
        checkInitialized();

        releaseProperties.setProperty( CHECKPOINT_PREFIX + pointName, "OK" );
    }

    public boolean verifyCheckpoint( String pointName )
    {
        checkLoaded();

        return resumeAtCheckpoint && "OK".equals( releaseProperties.getProperty( CHECKPOINT_PREFIX + pointName ) );
    }

    public void setResumeAtCheckpoint( boolean resumeAtCheckpoint )
    {
        this.resumeAtCheckpoint = resumeAtCheckpoint;
    }

}
