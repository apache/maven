/* Created on Jul 14, 2004 */
package org.apache.maven.lifecycle.phase;

import junit.framework.TestCase;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.decoration.DefaultGoalDecorator;
import org.apache.maven.decoration.GoalDecoratorBindings;
import org.apache.maven.lifecycle.MavenGoalExecutionContext;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.embed.Embedder;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author jdcasey
 */
public class GoalResolutionPhaseTest extends TestCase
{
    /*
    <!-- Test main with preGoal and postGoal -->
    <mojo>
      <id>t1:preGoal</id>
      <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin</implementation>
      <instantiationStrategy>singleton</instantiationStrategy>
    </mojo>
    <mojo>
      <id>t1:main</id>
      <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin</implementation>
      <instantiationStrategy>singleton</instantiationStrategy>
    </mojo>
    <mojo>
      <id>t1:postGoal</id>
      <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin</implementation>
      <instantiationStrategy>singleton</instantiationStrategy>
    </mojo>
    <!-- End of test -->
    */
    public void testT1_ShouldFind_PreGoal_MainGoal_PostGoal(  )
        throws Exception
    {
        String mainGoal = "t1:main";
        String preGoal = "t1:preGoal";
        String postGoal = "t1:postGoal";

        GoalDecoratorBindings bindings = new GoalDecoratorBindings(  );

        bindings.addPreGoal( new DefaultGoalDecorator( mainGoal, preGoal ) );
        bindings.addPostGoal( new DefaultGoalDecorator( mainGoal, postGoal ) );

        Map messages = new TreeMap(  );

        messages.put( mainGoal, "Main goal is missing." );
        messages.put( preGoal, "preGoal is missing" );
        messages.put( postGoal, "postGoal is missing" );

        List order = new ArrayList(  );

        order.add( preGoal );
        order.add( mainGoal );
        order.add( postGoal );

        runTest( mainGoal, bindings, order, messages );
    }

    /*
    <!-- Test main with prereq -->
    <mojo>
      <id>t2:prereq</id>
      <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin</implementation>
      <instantiationStrategy>singleton</instantiationStrategy>
    </mojo>
    <mojo>
      <id>t2:main</id>
      <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin</implementation>
      <instantiationStrategy>singleton</instantiationStrategy>
      <prereqs>
        <prereq>t2:prereq</prereq>
      </prereqs>
    </mojo>
    <!-- End of test -->
    */
    public void testT2_ShouldFind_Prereq_MainGoal(  )
        throws Exception
    {
        String mainGoal = "t2:main";
        String prereq = "t2:prereq";

        GoalDecoratorBindings bindings = new GoalDecoratorBindings(  );

        Map messages = new TreeMap(  );

        messages.put( mainGoal, "Main goal is missing." );
        messages.put( prereq, "prereq is missing" );

        List order = new ArrayList(  );

        order.add( prereq );
        order.add( mainGoal );

        runTest( mainGoal, bindings, order, messages );
    }

    /*
    <!-- Test main with prereq, preGoal and postGoal -->
    <mojo>
      <id>t3:preGoal</id>
      <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin</implementation>
      <instantiationStrategy>singleton</instantiationStrategy>
    </mojo>
    <mojo>
      <id>t3:prereq</id>
      <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin</implementation>
      <instantiationStrategy>singleton</instantiationStrategy>
    </mojo>
    <mojo>
      <id>t3:main</id>
      <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin</implementation>
      <instantiationStrategy>singleton</instantiationStrategy>
      <prereqs>
        <prereq>t3:prereq</prereq>
      </prereqs>
    </mojo>
    <mojo>
      <id>t3:postGoal</id>
      <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin</implementation>
      <instantiationStrategy>singleton</instantiationStrategy>
    </mojo>
    <!-- End of test -->
    */
    public void testT3_ShouldFind_PreGoal_Prereq_MainGoal_PostGoal(  )
        throws Exception
    {
        String mainGoal = "t3:main";
        String prereq = "t3:prereq";
        String preGoal = "t3:preGoal";
        String postGoal = "t3:postGoal";

        GoalDecoratorBindings bindings = new GoalDecoratorBindings(  );

        bindings.addPreGoal( new DefaultGoalDecorator( mainGoal, preGoal ) );
        bindings.addPostGoal( new DefaultGoalDecorator( mainGoal, postGoal ) );

        Map messages = new TreeMap(  );

        messages.put( mainGoal, "Main goal is missing." );
        messages.put( prereq, "prereq is missing" );
        messages.put( preGoal, "preGoal is missing" );
        messages.put( postGoal, "postGoal is missing" );

        List order = new ArrayList(  );

        order.add( preGoal );
        order.add( prereq );
        order.add( mainGoal );
        order.add( postGoal );

        runTest( mainGoal, bindings, order, messages );
    }

    /*
    <!-- Test main with prereq which has preGoal and postGoal -->
    <mojo>
      <id>t4:prereq-preGoal</id>
      <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin</implementation>
      <instantiationStrategy>singleton</instantiationStrategy>
    </mojo>
    <mojo>
      <id>t4:prereq</id>
      <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin</implementation>
      <instantiationStrategy>singleton</instantiationStrategy>
    </mojo>
    <mojo>
      <id>t4:main</id>
      <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin</implementation>
      <instantiationStrategy>singleton</instantiationStrategy>
      <prereqs>
        <prereq>t4:prereq</prereq>
      </prereqs>
    </mojo>
    <mojo>
      <id>t4:prereq-postGoal</id>
      <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin</implementation>
      <instantiationStrategy>singleton</instantiationStrategy>
    </mojo>
    <!-- End of test -->
    */
    public void testT4_ShouldFind_PreGoal_Prereq_PostGoal_MainGoal(  )
        throws Exception
    {
        String mainGoal = "t4:main";
        String prereq = "t4:prereq";
        String preGoal = "t4:prereq-preGoal";
        String postGoal = "t4:prereq-postGoal";

        GoalDecoratorBindings bindings = new GoalDecoratorBindings(  );

        bindings.addPreGoal( new DefaultGoalDecorator( prereq, preGoal ) );
        bindings.addPostGoal( new DefaultGoalDecorator( prereq, postGoal ) );

        Map messages = new TreeMap(  );

        messages.put( mainGoal, "Main goal is missing." );
        messages.put( prereq, "prereq is missing" );
        messages.put( preGoal, "preGoal is missing" );
        messages.put( postGoal, "postGoal is missing" );

        List order = new ArrayList(  );

        order.add( preGoal );
        order.add( prereq );
        order.add( postGoal );
        order.add( mainGoal );

        runTest( mainGoal, bindings, order, messages );
    }

    /*
    <!-- Test main with prereq and preGoal which has the same prereq -->
    <mojo>
      <id>t5:prereq</id>
      <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin</implementation>
      <instantiationStrategy>singleton</instantiationStrategy>
    </mojo>
    <mojo>
      <id>t5:preGoal</id>
      <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin</implementation>
      <instantiationStrategy>singleton</instantiationStrategy>
      <prereqs>
        <prereq>t5:prereq</prereq>
      </prereqs>
    </mojo>
    <mojo>
      <id>t5:main</id>
      <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin</implementation>
      <instantiationStrategy>singleton</instantiationStrategy>
      <prereqs>
        <prereq>t5:prereq</prereq>
      </prereqs>
    </mojo>
    <!-- End of test -->
    */
    public void testT5_ShouldFind_Prereq_PreGoal_MainGoal(  )
        throws Exception
    {
        String mainGoal = "t5:main";
        String prereq = "t5:prereq";
        String preGoal = "t5:preGoal";

        GoalDecoratorBindings bindings = new GoalDecoratorBindings(  );

        bindings.addPreGoal( new DefaultGoalDecorator( mainGoal, preGoal ) );

        Map messages = new TreeMap(  );

        messages.put( mainGoal, "Main goal is missing." );
        messages.put( prereq, "prereq is missing" );
        messages.put( preGoal, "preGoal is missing" );

        List order = new ArrayList(  );

        order.add( prereq );
        order.add( preGoal );
        order.add( mainGoal );

        runTest( mainGoal, bindings, order, messages );
    }

    private void runTest( String mainGoal, GoalDecoratorBindings bindings,
        List expectedOrder, Map messages )
        throws Exception
    {
        MavenProject project = new MavenProject( new Model(  ) );

        project.setFile( new File( new File( "./resolution-test" ),
                "project.xml" ) );

        Embedder embedder = new Embedder(  );

        embedder.start(  );

        PluginManager pluginManager = ( PluginManager ) embedder.lookup( PluginManager.ROLE );

        MojoDescriptor descriptor = pluginManager.getMojoDescriptor( mainGoal );

        ArtifactRepository localRepository = new ArtifactRepository();

         MavenGoalExecutionContext context = new MavenGoalExecutionContext( embedder.getContainer(),
                                                                    project,
                                                                    descriptor,
                                                                    localRepository );

        context.setGoalDecoratorBindings( bindings );

        GoalResolutionPhase phase = new GoalResolutionPhase(  );

        phase.execute( context );

        List goals = context.getResolvedGoals(  );

        System.out.println( "Resolved goals: " + goals );

        assertNotNull( goals );

        assertEquals( expectedOrder.size(  ), goals.size(  ) );

        int index = 0;

        for ( Iterator it = expectedOrder.iterator(  ); it.hasNext(  ); )
        {
            String goal = ( String ) it.next(  );
            String failureMessage = ( String ) messages.get( goal );

            String resolvedGoal = ( String ) goals.get( index++ );

            assertEquals( failureMessage, goal, resolvedGoal );
        }
    }
}
