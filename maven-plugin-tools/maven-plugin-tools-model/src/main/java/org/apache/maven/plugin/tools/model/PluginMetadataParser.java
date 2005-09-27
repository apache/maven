package org.apache.maven.plugin.tools.model;

import org.apache.maven.plugin.descriptor.DuplicateParameterException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.tools.model.io.xpp3.PluginMetadataXpp3Reader;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class PluginMetadataParser
{
    
    public Set parseMojoDescriptors( File metadataFile )
        throws PluginMetadataParseException
    {
        Set descriptors = new HashSet();
        
        Reader reader = null;
        
        try
        {
            reader = new FileReader( metadataFile );
            
            PluginMetadataXpp3Reader metadataReader = new PluginMetadataXpp3Reader();
            
            PluginMetadata pluginMetadata = metadataReader.read( reader );
            
            List mojos = pluginMetadata.getMojos();
            
            if ( mojos != null && !mojos.isEmpty() )
            {
                for ( Iterator it = mojos.iterator(); it.hasNext(); )
                {
                    Mojo mojo = (Mojo) it.next();
                    
                    MojoDescriptor descriptor = asDescriptor( metadataFile, mojo );
                    
                    descriptors.add( descriptor );
                }
            }
        }
        catch ( IOException e )
        {
            throw new PluginMetadataParseException( metadataFile, "Cannot parse plugin metadata from file.", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new PluginMetadataParseException( metadataFile, "Cannot parse plugin metadata from file.", e );
        }
        finally
        {
            IOUtil.close( reader );
        }
        
        return descriptors;
    }

    private MojoDescriptor asDescriptor( File metadataFile, Mojo mojo )
        throws PluginMetadataParseException
    {
        MojoDescriptor descriptor = new MojoDescriptor();
        
        descriptor.setImplementation( mojo.getCall() );
        
        descriptor.setGoal( mojo.getGoal() );
        descriptor.setPhase( mojo.getPhase() );
        descriptor.setDependencyResolutionRequired( mojo.getRequiresDependencyResolution() );
        descriptor.setAggregator( mojo.isAggregator() );
        descriptor.setInheritedByDefault( mojo.isInheritByDefault() );
        descriptor.setDirectInvocationOnly( mojo.isRequiresDirectInvocation() );
        descriptor.setOnlineRequired( mojo.isRequiresOnline() );
        descriptor.setProjectRequired( mojo.isRequiresProject() );
        descriptor.setRequiresReports( mojo.isRequiresReports() );
        descriptor.setDescription( mojo.getDescription() );
        descriptor.setDeprecated( mojo.getDeprecation() );
        
        LifecycleExecution le = mojo.getExecution();
        if ( le != null )
        {
            descriptor.setExecuteLifecycle( le.getLifecycle() );
            descriptor.setExecutePhase( le.getPhase() );
        }
        
        List parameters = mojo.getParameters();
        
        if ( parameters != null && !parameters.isEmpty() )
        {
            for ( Iterator it = parameters.iterator(); it.hasNext(); )
            {
                org.apache.maven.plugin.tools.model.Parameter param = (org.apache.maven.plugin.tools.model.Parameter) it.next();
                
                Parameter dParam = new Parameter();
                dParam.setAlias( param.getAlias() );
                dParam.setDeprecated( param.getDeprecation() );
                dParam.setDescription( param.getDescription() );
                dParam.setEditable( !param.isReadonly() );
                dParam.setExpression( param.getExpression() );
                
                String property = param.getProperty();
                if ( StringUtils.isNotEmpty( property ) )
                {
                    dParam.setName( property );
                }
                else
                {
                    dParam.setName( param.getName() );
                }
                
                if ( StringUtils.isEmpty( dParam.getName() ) )
                {
                    throw new PluginMetadataParseException( metadataFile, "Mojo: \'" + mojo.getGoal() + "\' has a parameter without either property or name attributes. Please specify one." );
                }

                dParam.setRequired( param.isRequired() );
                dParam.setType( param.getType() );
                
                try
                {
                    descriptor.addParameter( dParam );
                }
                catch ( DuplicateParameterException e )
                {
                    throw new PluginMetadataParseException( metadataFile, "Duplicate parameters detected for mojo: " + mojo.getGoal(), e );
                }
            }
        }
        
        List components = mojo.getComponents();
        
        if ( components != null && !components.isEmpty() )
        {
            for ( Iterator it = components.iterator(); it.hasNext(); )
            {
                Component component = (Component) it.next();
                
                ComponentRequirement cr = new ComponentRequirement();
                cr.setRole( component.getRole() );
                cr.setRoleHint( component.getHint() );
                
                descriptor.addRequirement( cr );
            }
        }
        
        return descriptor;
    }

}
