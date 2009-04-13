package org.apache.maven.project.builder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.builder.interpolator.DomainModel;
import org.apache.maven.project.builder.interpolator.ModelProperty;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class PomClassicDomainModel implements DomainModel
{

    /**
     * Bytes containing the underlying model
     */
    private byte[] inputBytes;

    private String id;

    private File file;

    private File parentFile;

    private File projectDirectory;

    private List<ModelProperty> modelProperties;
    
    private int lineageCount;
    
    private boolean isMostSpecialized = false;

    private String parentGroupId = null, parentArtifactId = null, parentVersion = null, parentId = null, parentRelativePath;
    
    private Model model;

    public Model getModel() throws IOException
    {
        return model;        
    }   

    private void initializeProperties(List<ModelProperty> modelProperties)
    {
        String groupId = null, artifactId = null, version = null;
        for(ModelProperty mp : modelProperties)
        {
            if(mp.getUri().equals(ProjectUri.groupId))
            {
                groupId = mp.getResolvedValue();
            }
            else if(mp.getUri().equals(ProjectUri.artifactId))
            {
                artifactId = mp.getResolvedValue();
            }
            else if(mp.getUri().equals(ProjectUri.version))
            {
                version = mp.getResolvedValue();
            }
            else if(mp.getUri().equals(ProjectUri.Parent.artifactId))
            {
                parentArtifactId = mp.getResolvedValue();
            }
            else if(mp.getUri().equals(ProjectUri.Parent.groupId))
            {
                parentGroupId = mp.getResolvedValue();
            }
            else if(mp.getUri().equals(ProjectUri.Parent.version))
            {
                parentVersion = mp.getResolvedValue();
            }
            else if(mp.getUri().equals(ProjectUri.Parent.relativePath))
            {
                parentRelativePath = mp.getResolvedValue();
            }

            if(groupId != null && artifactId != null && version != null && parentGroupId != null &&
                    parentArtifactId != null && parentVersion != null & parentRelativePath != null)
            {
                break;
            }
        }
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

        if(parentRelativePath == null)
        {
            parentRelativePath = ".." + File.separator + "pom.xml";
        }

        id = groupId + ":" + artifactId + ":" + version;
    }

    public PomClassicDomainModel( File file )
        throws IOException
    {
    	this( new FileInputStream( file ) );
        this.file = file;
    }
    
    public PomClassicDomainModel( InputStream is )
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
	    
	    modelProperties = getModelProperties();
	    initializeProperties( modelProperties );

    }    

    public PomClassicDomainModel(Model model) throws IOException {
    	this (model, false);
	}

	public PomClassicDomainModel(Model model, boolean b) throws IOException {
		this.model = model;
		this.isMostSpecialized = b;
		
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


    public boolean matchesParentOf( PomClassicDomainModel domainModel ) throws IOException
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
    public String asString()
    {
        try
        {
            return IOUtil.toString( ReaderFactory.newXmlReader( new ByteArrayInputStream( inputBytes ) ) );
        }
        catch ( IOException ioe )
        {
            // should not occur: everything is in-memory
            return "";
        }
    }

    /**
     * @see org.apache.maven.shared.model.InputStreamDomainModel#getInputStream()
     */
    public InputStream getInputStream()
    {
        byte[] copy = new byte[inputBytes.length];
        System.arraycopy( inputBytes, 0, copy, 0, inputBytes.length );
        return new ByteArrayInputStream( copy );
    }

    /**
     * @return file of pom. May be null.
     */
    public File getFile()
    {
        return file;
    }

    public List<ModelProperty> getModelProperties() throws IOException
    {
        if(modelProperties == null)
        {
            Set<String> s = new HashSet<String>();
            //TODO: Should add all collections from ProjectUri
            s.addAll(URIS);
            s.add(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.xUri);
            s.add(ProjectUri.DependencyManagement.Dependencies.Dependency.Exclusions.xUri);
            s.add(ProjectUri.Dependencies.Dependency.Exclusions.xUri);
            s.add(ProjectUri.Build.Plugins.Plugin.Executions.xUri);
            s.add(ProjectUri.Build.Plugins.Plugin.Executions.Execution.Goals.xURI);
            s.add(ProjectUri.Reporting.Plugins.Plugin.ReportSets.xUri);
            s.add(ProjectUri.Reporting.Plugins.Plugin.ReportSets.ReportSet.configuration);
            s.add(ProjectUri.Build.Plugins.Plugin.Executions.Execution.configuration);
            //TODO: More profile info
            s.add(ProjectUri.Profiles.Profile.Build.PluginManagement.Plugins.Plugin.Executions.xUri);
            s.add(ProjectUri.Profiles.Profile.DependencyManagement.Dependencies.Dependency.Exclusions.xUri);
            s.add(ProjectUri.Profiles.Profile.Dependencies.Dependency.Exclusions.xUri);
            s.add(ProjectUri.Profiles.Profile.Build.Plugins.Plugin.Executions.xUri);
            s.add(ProjectUri.Profiles.Profile.Build.Plugins.Plugin.Executions.Execution.Goals.xURI);
            s.add(ProjectUri.Profiles.Profile.Reporting.Plugins.Plugin.ReportSets.xUri);
            s.add(ProjectUri.Profiles.Profile.Reporting.Plugins.Plugin.ReportSets.ReportSet.configuration);
            s.add(ProjectUri.Profiles.Profile.Build.Plugins.Plugin.Executions.Execution.configuration);
            s.add(ProjectUri.Profiles.Profile.properties);
            s.add(ProjectUri.Profiles.Profile.modules);
            s.add(ProjectUri.Profiles.Profile.Dependencies.xUri);
            s.add(ProjectUri.Profiles.Profile.Build.Plugins.Plugin.configuration);
            
            modelProperties = marshallXmlToModelProperties(getInputStream(), ProjectUri.baseUri, s );
        }
        return new ArrayList<ModelProperty>(modelProperties);
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
            return o instanceof PomClassicDomainModel && getId().equals( ( (PomClassicDomainModel) o ).getId() );
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
    
    private static final Set<String> URIS = Collections.unmodifiableSet(new HashSet<String>( Arrays.asList(  ProjectUri.Build.Extensions.xUri,
            ProjectUri.Build.PluginManagement.Plugins.xUri,
            ProjectUri.Build.PluginManagement.Plugins.Plugin.configuration,
            ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.xUri,
            ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.Goals.xURI,
            ProjectUri.Build.PluginManagement.Plugins.Plugin.Dependencies.xUri,
            ProjectUri.Build.PluginManagement.Plugins.Plugin.Dependencies.Dependency.Exclusions.xUri,
            ProjectUri.Build.Plugins.xUri,
            ProjectUri.properties,
            ProjectUri.Build.Plugins.Plugin.configuration,
            ProjectUri.Reporting.Plugins.xUri,
            ProjectUri.Reporting.Plugins.Plugin.configuration,
            ProjectUri.Build.Plugins.Plugin.Dependencies.xUri,
            ProjectUri.Build.Resources.xUri,
            ProjectUri.Build.Resources.Resource.includes,
            ProjectUri.Build.Resources.Resource.excludes,
            ProjectUri.Build.TestResources.xUri,
            ProjectUri.Build.Filters.xUri,
            ProjectUri.CiManagement.Notifiers.xUri,
            ProjectUri.Contributors.xUri,
            ProjectUri.Dependencies.xUri,
            ProjectUri.DependencyManagement.Dependencies.xUri,
            ProjectUri.Developers.xUri,
            ProjectUri.Developers.Developer.roles,
            ProjectUri.Licenses.xUri,
            ProjectUri.MailingLists.xUri,
            ProjectUri.Modules.xUri,
            ProjectUri.PluginRepositories.xUri,
            ProjectUri.Profiles.xUri,
            ProjectUri.Profiles.Profile.Build.Plugins.xUri,
            ProjectUri.Profiles.Profile.Build.Plugins.Plugin.Dependencies.xUri,
            ProjectUri.Profiles.Profile.Build.Plugins.Plugin.Executions.xUri,
            ProjectUri.Profiles.Profile.Build.Resources.xUri,
            ProjectUri.Profiles.Profile.Build.TestResources.xUri,
            ProjectUri.Profiles.Profile.Dependencies.xUri,
            ProjectUri.Profiles.Profile.DependencyManagement.Dependencies.xUri,
            ProjectUri.Profiles.Profile.PluginRepositories.xUri,
            ProjectUri.Profiles.Profile.Reporting.Plugins.xUri,
            ProjectUri.Profiles.Profile.Repositories.xUri,
            ProjectUri.Profiles.Profile.Build.PluginManagement.Plugins.xUri,
            ProjectUri.Profiles.Profile.Build.PluginManagement.Plugins.Plugin.Dependencies.xUri,
            ProjectUri.Reporting.Plugins.xUri,
            ProjectUri.Repositories.xUri) ));    
    
   /**
    * Returns list of model properties transformed from the specified input stream.
    *
    * @param inputStream input stream containing the xml document. May not be null.
    * @param baseUri     the base uri of every model property. May not be null or empty.
    * @param collections set of uris that are to be treated as a collection (multiple entries). May be null.
    * @return list of model properties transformed from the specified input stream.
    * @throws IOException if there was a problem doing the transform
    */
    public static List<ModelProperty> marshallXmlToModelProperties( InputStream inputStream, String baseUri,
            Set<String> collections )
			throws IOException {
		if (inputStream == null) {
			throw new IllegalArgumentException("inputStream: null");
		}

		if (baseUri == null || baseUri.trim().length() == 0) {
			throw new IllegalArgumentException("baseUri: null");
		}

		if (collections == null) {
			collections = Collections.emptySet();
		}

		List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
		XMLInputFactory xmlInputFactory = new com.ctc.wstx.stax.WstxInputFactory();
		xmlInputFactory.setProperty(
				XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
		xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE,
				Boolean.FALSE);

		Uri uri = new Uri(baseUri);
		String tagName = baseUri;
		StringBuilder tagValue = new StringBuilder(256);

		int depth = 0;
		int depthOfTagValue = depth;
		XMLStreamReader xmlStreamReader = null;
		try {
			xmlStreamReader = xmlInputFactory
					.createXMLStreamReader(inputStream);

			Map<String, String> attributes = new HashMap<String, String>();
			for (;; xmlStreamReader.next()) {
				int type = xmlStreamReader.getEventType();
				switch (type) {

				case XMLStreamConstants.CDATA:
				case XMLStreamConstants.CHARACTERS: {
					if (depth == depthOfTagValue) {
						tagValue.append(xmlStreamReader.getTextCharacters(),
								xmlStreamReader.getTextStart(), xmlStreamReader
										.getTextLength());
					}
					break;
				}

				case XMLStreamConstants.START_ELEMENT: {
					if (!tagName.equals(baseUri)) {
						String value = null;
						if (depth < depthOfTagValue) {
							value = tagValue.toString().trim();
						}
						modelProperties.add(new ModelProperty(tagName, value));
						if (!attributes.isEmpty()) {
							for (Map.Entry<String, String> e : attributes
									.entrySet()) {
								modelProperties.add(new ModelProperty(e
										.getKey(), e.getValue()));
							}
							attributes.clear();
						}
					}

					depth++;
					tagName = uri.getUriFor(xmlStreamReader.getName()
							.getLocalPart(), depth);
					if (collections.contains(tagName + "#collection")) {
						tagName = tagName + "#collection";
						uri.addTag(xmlStreamReader.getName().getLocalPart()
								+ "#collection");
					} else if (collections.contains(tagName + "#set")) {
						tagName = tagName + "#set";
						uri.addTag(xmlStreamReader.getName().getLocalPart()
								+ "#set");
					} else {
						uri.addTag(xmlStreamReader.getName().getLocalPart());
					}
					tagValue.setLength(0);
					depthOfTagValue = depth;
				}
				case XMLStreamConstants.ATTRIBUTE: {
					for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++) {

						attributes.put(tagName
								+ "#property/"
								+ xmlStreamReader.getAttributeName(i)
										.getLocalPart(), xmlStreamReader
								.getAttributeValue(i));
					}
					break;
				}
				case XMLStreamConstants.END_ELEMENT: {
					depth--;
					break;
				}
				case XMLStreamConstants.END_DOCUMENT: {
					modelProperties.add(new ModelProperty(tagName, tagValue
							.toString().trim()));
					if (!attributes.isEmpty()) {
						for (Map.Entry<String, String> e : attributes
								.entrySet()) {
							modelProperties.add(new ModelProperty(e.getKey(), e
									.getValue()));
						}
						attributes.clear();
					}
					return modelProperties;
				}
				}
			}
		} catch (XMLStreamException e) {
			throw new IOException(":" + e.toString());
		} finally {
			if (xmlStreamReader != null) {
				try {
					xmlStreamReader.close();
				} catch (XMLStreamException e) {
					e.printStackTrace();
				}
			}
			try {
				inputStream.close();
			} catch (IOException e) {

			}
		}
	}
   /**
    * Class for storing information about URIs.
    */
   private static class Uri
   {

       List<String> uris;

       Uri( String baseUri )
       {
           uris = new LinkedList<String>();
           uris.add( baseUri );
       }

       String getUriFor( String tag, int depth )
       {
           setUrisToDepth( depth );
           StringBuffer sb = new StringBuffer();
           for ( String tagName : uris )
           {
               sb.append( tagName ).append( "/" );
           }
           sb.append( tag );
           return sb.toString();
       }

       void addTag( String tag )
       {
           uris.add( tag );
       }

       void setUrisToDepth( int depth )
       {
           uris = new LinkedList<String>( uris.subList( 0, depth ) );
       }
   }
}
