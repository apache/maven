/* Created on Aug 7, 2004 */
package org.apache.maven.plugin.loader.marmalade;

import org.apache.maven.plugin.MavenMojoDescriptor;
import org.apache.maven.plugin.MavenPluginDependency;
import org.apache.maven.plugin.MavenPluginDescriptor;
import org.apache.maven.plugin.descriptor.Dependency;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.loader.marmalade.tags.MojoTag;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.marmalade.el.ognl.OgnlExpressionEvaluator;
import org.codehaus.marmalade.metamodel.MarmaladeTaglibResolver;
import org.codehaus.marmalade.model.MarmaladeScript;
import org.codehaus.marmalade.model.MarmaladeTag;
import org.codehaus.marmalade.parsetime.CachingScriptParser;
import org.codehaus.marmalade.parsetime.DefaultParsingContext;
import org.codehaus.marmalade.parsetime.MarmaladeModelBuilderException;
import org.codehaus.marmalade.parsetime.MarmaladeParsetimeException;
import org.codehaus.marmalade.parsetime.MarmaladeParsingContext;
import org.codehaus.marmalade.parsetime.ScriptBuilder;
import org.codehaus.marmalade.parsetime.ScriptParser;
import org.codehaus.marmalade.runtime.DefaultContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;
import org.codehaus.marmalade.util.RecordingReader;
import org.codehaus.plexus.component.discovery.AbstractComponentDiscoverer;
import org.codehaus.plexus.component.factory.ComponentInstantiationException;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * @author jdcasey
 */
public class MarmaladePluginDiscoverer extends AbstractComponentDiscoverer
{
    private static final String FILE_PROTO = "file";
    private static final String MARMALADE_MOJO_PATH_PATTERN = ".*\\.mmld";
    private static final String MARMALADE_COMPONENT_FACTORY = "marmalade";

    protected String getComponentDescriptorLocation(  )
    {
        return null;
    }

    protected ComponentSetDescriptor createComponentDescriptors( 
        Reader reader, String source )
        throws Exception
    {
        return null;
    }

    public List findComponents( ClassRealm classRealm )
    {
        URL[] candidateLocations = classRealm.getConstituents(  );
        
        List componentSetDescriptors = new LinkedList(  );

        for ( int i = 0; i < candidateLocations.length; i++ )
        {
            URL url = candidateLocations[i];

            String urlProto = url.getProtocol(  );

            try
            {
                PluginDescriptor pluginDescriptor = null;

                if ( FILE_PROTO.equals( urlProto ) )
                {
                    // scan as directory...
                    pluginDescriptor = scanAsDirectory( url.getPath(  ),
                            classRealm );
                }
                else
                {
                    // Skip this one...not sure what to do with it.
                }

                if ( pluginDescriptor != null )
                {
                    MavenPluginDescriptor descriptor = new MavenPluginDescriptor( pluginDescriptor );

                    // Add the dependencies
                    List dependencies = new ArrayList();

                    for ( Iterator it = pluginDescriptor.getDependencies().iterator(); it.hasNext() ; )
                    {
                        Dependency dependency = (Dependency)it.next();
                        dependencies.add( new MavenPluginDependency( dependency ) );
                    }

                    descriptor.setDependencies( dependencies );
                    
                    List mojoDescriptors = pluginDescriptor.getMojos(  );
                    List componentDescriptors = new LinkedList(  );

                    for ( Iterator it = mojoDescriptors.iterator(  );
                        it.hasNext(  ); )
                    {
                        MojoDescriptor mojoDescriptor = ( MojoDescriptor ) it
                            .next(  );
                        
                        mojoDescriptor.setImplementation(url.getPath());
                        
                        ComponentDescriptor mmDesc = new MavenMojoDescriptor( mojoDescriptor );

                        mmDesc.setComponentFactory( MARMALADE_COMPONENT_FACTORY );

                        componentDescriptors.add( mmDesc );
                    }

                    descriptor.setComponents( componentDescriptors );

                    componentSetDescriptors.add( descriptor );
                }
            }
            catch ( Exception e )
            {
                // TODO Log and what...skip???
                e.printStackTrace(  );
            }
        }

        return componentSetDescriptors;
    }

    private PluginDescriptor scanAsJar( String path, ClassRealm classRealm )
        throws Exception
    {
        String jarPart = path;

        int bangIdx = jarPart.indexOf( "!" );

        if ( bangIdx > -1 )
        {
            jarPart = jarPart.substring( 0, bangIdx );
        }

        File file = new File(path);
        JarInputStream jis = new JarInputStream( new FileInputStream( file ) );
        
        List mojoDescriptors = new LinkedList(  );
        List dependencies = new LinkedList(  );

        JarEntry je = null;

        while ( ( je = jis.getNextJarEntry(  ) ) != null )
        {
            String entryName = je.getName(  );

            if ( entryName.matches( MARMALADE_MOJO_PATH_PATTERN ) )
            {
                mojoDescriptors.add( loadMojoDescriptor( entryName,
                        dependencies, classRealm ) );
            }
        }

        PluginDescriptor descriptor = null;

        if ( !mojoDescriptors.isEmpty(  ) )
        {
            descriptor = new PluginDescriptor(  );
            descriptor.setMojos( mojoDescriptors );
            descriptor.setDependencies( dependencies );
        }

        return descriptor;
    }

    private PluginDescriptor scanAsDirectory( String path, ClassRealm classRealm )
        throws Exception
    {
        List dependencies = new LinkedList(  );
        
        File file = new File(path);
        
        PluginDescriptor descriptor = null;
        
        if(!file.isDirectory() && path.endsWith(".jar")) {
            // try to scan it as a jar...
            descriptor = scanAsJar(path, classRealm);
        }
        else {
            List mojoDescriptors = scanDir( path, file,
                    new LinkedList(  ), dependencies, classRealm );
    
            if ( !mojoDescriptors.isEmpty(  ) )
            {
                descriptor = new PluginDescriptor(  );
                descriptor.setMojos( mojoDescriptors );
                descriptor.setDependencies( dependencies );
            }
        }
        
        return descriptor;
    }

    private List scanDir( String basePath, File parent, List results,
        List dependencies, ClassRealm classRealm )
        throws Exception
    {
        if ( parent.isDirectory(  ) )
        {
            String[] subPaths = parent.list(  );

            for ( int i = 0; i < subPaths.length; i++ )
            {
                File file = new File( parent, subPaths[i] );

                if ( file.isDirectory(  ) )
                {
                    results = scanDir( basePath, file, results, dependencies,
                            classRealm );
                }
                else if ( file.getPath(  ).matches( MARMALADE_MOJO_PATH_PATTERN ) )
                {
                    String scriptResource = file.getAbsolutePath(  ).substring( basePath
                            .length(  ) );

                    results.add( loadMojoDescriptor( scriptResource,
                            dependencies, classRealm ) );
                }
            }
        }

        return results;
    }

    private MojoDescriptor loadMojoDescriptor( String entryName,
        List dependencies, ClassRealm classRealm )
        throws Exception
    {
        MarmaladeParsingContext context = buildParsingContext( entryName,
                classRealm );
        MarmaladeScript script = getScriptInstance( context );

        MojoDescriptor desc = getDescriptor( script, dependencies );
        
        return desc;
    }

    private MojoDescriptor getDescriptor( MarmaladeScript script,
        List dependencies ) throws ComponentInstantiationException
    {
        MojoTag root = ( MojoTag ) script.getRoot(  );

        root.describeOnly( true );

        MarmaladeExecutionContext execCtx = new DefaultContext(  );

        try
        {
            script.execute( execCtx );
        }
        catch ( MarmaladeExecutionException e )
        {
            throw new ComponentInstantiationException( 
                "failed to execute component script: " + script.getLocation(  ),
                e );
        }

        MojoDescriptor descriptor = root.getMojoDescriptor(  );

        dependencies = root.addDependencies( dependencies );

        return descriptor;
    }

    private MarmaladeScript getScriptInstance( 
        MarmaladeParsingContext parsingContext )
        throws Exception
    {
        ScriptBuilder builder = null;

        try
        {
            builder = new ScriptParser().parse( parsingContext );
        }
        catch ( MarmaladeParsetimeException e )
        {
            throw new Exception( "failed to parse component script: "
                + parsingContext.getInputLocation(  ), e );
        }
        catch ( MarmaladeModelBuilderException e )
        {
            throw new Exception( "failed to parse component script: "
                + parsingContext.getInputLocation(  ), e );
        }

        MarmaladeScript script = null;

        try
        {
            script = builder.build(  );
        }
        catch ( MarmaladeModelBuilderException e )
        {
            throw new Exception( "failed to build component script: "
                + parsingContext.getInputLocation(  ), e );
        }

        MarmaladeTag root = script.getRoot(  );

        if ( !( root instanceof MojoTag ) )
        {
            throw new Exception( 
                "marmalade script does not contain a mojo header" );
        }

        return script;
    }

    private MarmaladeParsingContext buildParsingContext( String scriptName,
        ClassRealm classRealm )
        throws Exception
    {
        InputStream scriptResource = classRealm.getResourceAsStream( scriptName );

        if ( scriptResource == null )
        {
            throw new Exception( "can't get script from classpath: "
                + scriptName );
        }

        RecordingReader scriptIn = new RecordingReader( new InputStreamReader( 
                    scriptResource ) );

        MarmaladeTaglibResolver resolver = new MarmaladeTaglibResolver( MarmaladeTaglibResolver.DEFAULT_STRATEGY_CHAIN );
        
        OgnlExpressionEvaluator el = new OgnlExpressionEvaluator(  );

        MarmaladeParsingContext context = new DefaultParsingContext(  );

        context.setDefaultExpressionEvaluator( el );
        context.setInput( scriptIn );
        context.setInputLocation( scriptName );
        context.setTaglibResolver( resolver );

        return context;
    }
}
