package org.apache.maven.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

// TODO: Delete this class
public class DomainModel
{

    /**
     * Bytes containing the underlying model
     */
    private byte[] inputBytes;

    private String id;

    private File file;

    private File parentFile;

    private File projectDirectory;
    
    private int lineageCount;
    
    private boolean isMostSpecialized = false;

    private String parentGroupId = null, parentArtifactId = null, parentVersion = null, parentId = null, parentRelativePath;
    
    protected Model model;

    public Model getModel()
    {
        return model;        
    }   

    private void initializeProperties(Model model)
    {
    	String groupId = null, artifactId = null, version = null;

    	groupId = model.getGroupId();
    	artifactId = model.getArtifactId();
    	version = model.getVersion();
    	
    	if( model.getParent() != null)
    	{
    		parentArtifactId =model.getParent().getArtifactId();
    		parentGroupId = model.getParent().getGroupId();
    		parentVersion = model.getParent().getVersion();
    		parentRelativePath = model.getParent().getRelativePath();
    		
    		if( groupId == null && parentGroupId != null)
    		{
    			groupId = parentGroupId;
    		}
    		if( artifactId == null && parentArtifactId != null)
    		{
    			artifactId = parentArtifactId;
    		}
    		if( version == null && parentVersion != null )
    		{
    			version = parentVersion;
    		}
    		
        	if(parentGroupId != null && parentArtifactId != null && parentVersion != null)
        	{
        		parentId = parentGroupId + ":" + parentArtifactId + ":" + parentVersion;
        	}
    	}

    	if(parentRelativePath == null)
    	{
    		parentRelativePath = ".." + File.separator + "pom.xml";
    	}

    	id = groupId + ":" + artifactId + ":" + version;
    }

    public DomainModel( File file )
        throws IOException
    {
    	this( new FileInputStream( file ) );
        this.file = file;
        this.model.setPomFile( file );
    }
    
    public DomainModel( InputStream is )
    	throws IOException
    {
	    this.inputBytes = IOUtil.toByteArray( is);
	    
	    MavenXpp3Reader reader = new MavenXpp3Reader();
	    try
	    {
	        model =  reader.read( new ByteArrayInputStream( inputBytes ), false ) ;
	    }
	    catch ( XmlPullParserException e )
	    {
	        throw new IOException( e.getMessage() );
	    }  
	    
	    initializeProperties( model );
    }    

    public DomainModel(Model model) throws IOException {
    	this (model, false);
	}

	public DomainModel(Model model, boolean b) throws IOException {
		this.model = model;
		this.isMostSpecialized = b;
		

        initializeProperties( model );
        
    }

	public File getParentFile()
    {
        return parentFile;
    }

    public void setParentFile( File parentFile )
    {
        this.parentFile = parentFile;
    }

    public String getParentGroupId() {
        return parentGroupId;
    }

    public String getParentArtifactId() {
        return parentArtifactId;
    }

    public String getParentVersion() {
        return parentVersion;
    }

    /**
     * This should only be set for projects that are in the build. Setting for poms in the repo may cause unstable behavior.
     * 
     * @param projectDirectory
     */
    public void setProjectDirectory(File projectDirectory)
    {
        this.projectDirectory = projectDirectory;
    }

    public File getProjectDirectory()
    {
        return projectDirectory;
    }

    public boolean isPomInBuild()
    {
        return projectDirectory != null;
    }

    public String getParentId() throws IOException
    {
        return parentId;
    }

    public String getRelativePathOfParent()
    {
        return parentRelativePath;
    }

    public String getId() throws IOException
    {
        return id;
    }
    
    public boolean matchesParentOf( DomainModel domainModel ) throws IOException
    {
        if ( domainModel == null )
        {
            throw new IllegalArgumentException( "domainModel: null" );
        }

        return getId().equals(domainModel.getParentId());
    }

    /**
     * Returns XML model as string
     *
     * @return XML model as string
     */
    public String asString() throws IOException
    {
	    return IOUtil.toString( ReaderFactory.newXmlReader( getInputStream() ) );
    }

    /**
     * @see org.apache.maven.shared.model.InputStreamDomainModel#getInputStream()
     */
    public InputStream getInputStream() throws IOException
    {
    	if(inputBytes != null)
    	{
            byte[] copy = new byte[inputBytes.length];
            System.arraycopy( inputBytes, 0, copy, 0, inputBytes.length );
            return new ByteArrayInputStream( copy );    		
    	}
    	else
    	{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Writer out = null;
            MavenXpp3Writer writer = new MavenXpp3Writer();
            try
            {
                out = WriterFactory.newXmlWriter( baos );
                writer.write( out, model );
            }
            finally
            {
                if ( out != null )
                {
                    out.close();
                }
            }
            inputBytes = baos.toByteArray();
            return new ByteArrayInputStream(inputBytes);
    	}
    }

    /**
     * @return file of pom. May be null.
     */
    public File getFile()
    {
        return file;
    }

    public int getLineageCount()
    {
        return lineageCount;
    }

    public void setLineageCount( int lineageCount )
    {
        this.lineageCount = lineageCount;
    }

    /**
     * Returns true if this.asString.equals(o.asString()), otherwise false.
     *
     * @param o domain model
     * @return true if this.asString.equals(o.asString()), otherwise false.
     */
    public boolean equals( Object o )
    {
        try {
            return o instanceof DomainModel && getId().equals( ( (DomainModel) o ).getId() );
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isMostSpecialized()
    {
        return isMostSpecialized;
    }

    public void setMostSpecialized( boolean isMostSpecialized )
    {
        this.isMostSpecialized = isMostSpecialized;
    }

    @Override
    public String toString()
    {
        return String.valueOf( id );
    }
    
 
}
