package org.apache.maven.execution;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ReactorManager
{
    
    public static final String FAIL_FAST = "fail-fast";

    public static final String FAIL_AT_END = "fail-at-end";

    public static final String FAIL_NEVER = "fail-never";
    
    private DAG reactorDag;
    
    private Map projectMap;
    
    private List projectsByDependency;

    private List blackList = new ArrayList();

    private MavenProject topLevelProject;
    
    private Map buildFailuresByProject = new HashMap();
    
    private String failureBehavior = FAIL_FAST;
    
    public ReactorManager( List projects )
        throws CycleDetectedException
    {
        reactorDag = new DAG();

        projectMap = new HashMap();

        for ( Iterator i = projects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            String id = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

            reactorDag.addVertex( id );

            projectMap.put( id, project );
        }

        for ( Iterator i = projects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            String id = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

            for ( Iterator j = project.getDependencies().iterator(); j.hasNext(); )
            {
                Dependency dependency = (Dependency) j.next();

                String dependencyId = ArtifactUtils.versionlessKey( dependency.getGroupId(), dependency.getArtifactId() );

                if ( reactorDag.getVertex( dependencyId ) != null )
                {
                    project.addProjectReference( (MavenProject) projectMap.get( dependencyId ) );

                    reactorDag.addEdge( id, dependencyId );
                }
            }

            MavenProject parent = project.getParent();
            if ( parent != null )
            {
                String parentId = ArtifactUtils.versionlessKey( parent.getGroupId(), parent.getArtifactId() );
                if ( reactorDag.getVertex( parentId ) != null )
                {
                    reactorDag.addEdge( id, parentId );
                }
            }

            List buildPlugins = project.getBuildPlugins();
            if ( buildPlugins != null )
            {
                for ( Iterator j = buildPlugins.iterator(); j.hasNext(); )
                {
                    Plugin plugin = (Plugin) j.next();
                    String pluginId = ArtifactUtils.versionlessKey( plugin.getGroupId(), plugin.getArtifactId() );
                    if ( reactorDag.getVertex( pluginId ) != null )
                    {
                        reactorDag.addEdge( id, pluginId );
                    }
                }
            }

            List reportPlugins = project.getReportPlugins();
            if ( reportPlugins != null )
            {
                for ( Iterator j = reportPlugins.iterator(); j.hasNext(); )
                {
                    ReportPlugin plugin = (ReportPlugin) j.next();
                    String pluginId = ArtifactUtils.versionlessKey( plugin.getGroupId(), plugin.getArtifactId() );
                    if ( reactorDag.getVertex( pluginId ) != null )
                    {
                        reactorDag.addEdge( id, pluginId );
                    }
                }
            }

            for ( Iterator j = project.getBuildExtensions().iterator(); j.hasNext(); )
            {
                Extension extension = (Extension) j.next();
                String extensionId = ArtifactUtils.versionlessKey( extension.getGroupId(), extension.getArtifactId() );
                if ( reactorDag.getVertex( extensionId ) != null )
                {
                    reactorDag.addEdge( id, extensionId );
                }
            }
        }

        projectsByDependency = new ArrayList();

        for ( Iterator i = TopologicalSorter.sort( reactorDag ).iterator(); i.hasNext(); )
        {
            String id = (String) i.next();

            projectsByDependency.add( projectMap.get( id ) );
        }
        
        projectsByDependency = Collections.unmodifiableList( projectsByDependency );
    }
    
    public void setFailureBehavior( String failureBehavior )
    {
        if ( FAIL_FAST.equals( failureBehavior ) || FAIL_AT_END.equals( failureBehavior ) || FAIL_NEVER.equals( failureBehavior ) )
        {
            this.failureBehavior = failureBehavior;
        }
        else
        {
            throw new IllegalArgumentException( "Invalid failure behavior (must be one of: \'" + FAIL_FAST + "\', \'"
                + FAIL_AT_END + "\', \'" + FAIL_NEVER + "\')." );
        }
    }
    
    public String getFailureBehavior()
    {
        return failureBehavior;
    }
    
    public List getProjectsSortedByDependency()
    {
        return projectsByDependency;
    }
    
    // TODO: !![jc; 28-jul-2005] check this; if we're using '-r' and there are aggregator tasks, this will result in weirdness.
    public MavenProject getTopLevelProject()
    {
        if ( topLevelProject == null )
        {
            List projectsByFile = new ArrayList( projectsByDependency );
            
            Collections.sort(projectsByFile, new ByProjectFileComparator() );
            
            topLevelProject = (MavenProject) projectsByFile.get( 0 );
        }
        
        return topLevelProject;
    }
    
    public void blackList( String id )
    {
        if ( !blackList.contains( id ) )
        {
            blackList.add( id );

            List dependents = reactorDag.getParentLabels( id );

            if ( dependents != null && !dependents.isEmpty() )
            {
                for ( Iterator it = dependents.iterator(); it.hasNext(); )
                {
                    String dependentId = (String) it.next();

                    blackList( dependentId );
                }
            }
        }
    }
    
    public boolean isBlackListed( String id )
    {
        return blackList.contains( id );
    }
    
    public void registerBuildFailure( MavenProject project, Exception error, String task )
    {
        buildFailuresByProject.put( project.getId(), new BuildFailure( error, task ) );
    }
    
    public boolean hasBuildFailures()
    {
        return !buildFailuresByProject.isEmpty();
    }
    
    public boolean hasBuildFailure( String id )
    {
        return buildFailuresByProject.containsKey( id );
    }
    
    public boolean hasMultipleProjects()
    {
        return projectsByDependency.size() > 1;
    }
    
    private static class ByProjectFileComparator implements Comparator
    {

        public int compare( Object first, Object second )
        {
            MavenProject p1 = (MavenProject) first;
            MavenProject p2 = (MavenProject) second;
            
            String p1Path = p1.getFile().getAbsolutePath();
            String p2Path = p2.getFile().getAbsolutePath();
            
            int comparison = p1Path.length() - p2Path.length();
            
            if ( comparison > 0 )
            {
                return 1;
            }
            else if ( comparison < 0 )
            {
                return -1;
            }
            else
            {
                return 0;
            }
        }
    }
    
    private static class BuildFailure
    {
        private Exception cause;
        private String task;
        
        BuildFailure( Exception cause, String task )
        {
            this.cause = cause;
            this.task = task;
        }
        
        String getTask()
        {
            return task;
        }
        
        Exception getCause()
        {
            return cause;
        }
    }
}
