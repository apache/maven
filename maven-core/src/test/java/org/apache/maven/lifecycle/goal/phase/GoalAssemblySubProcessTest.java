/* Created on Jul 14, 2004 */
package org.apache.maven.lifecycle.goal.phase;

import org.apache.maven.MavenTestCase;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.lifecycle.goal.MavenGoalExecutionContext;
import org.apache.maven.lifecycle.session.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.PostGoal;
import org.apache.maven.model.PreGoal;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** The point of this test class is to check out the functioning of the 
 * plugin resolution, goal mapping, and goal resolution phases. These are 
 * intertwined here to make testing easier, but should be separated into their
 * own unit tests.
 * 
 * @author jdcasey
 */
public class GoalAssemblySubProcessTest
    extends MavenTestCase
{
    /*
     * <!-- Test main with preGoal and postGoal --> <mojo>
     * <id>resolveTest:t1-preGoal </id>
     * <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin
     * </implementation> <instantiationStrategy>singleton
     * </instantiationStrategy> </mojo> <mojo> <id>resolveTest:t1-main </id>
     * <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin
     * </implementation> <instantiationStrategy>singleton
     * </instantiationStrategy> </mojo> <mojo> <id>resolveTest:t1-postGoal </id>
     * <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin
     * </implementation> <instantiationStrategy>singleton
     * </instantiationStrategy> </mojo> <!-- End of test -->
     */
    public void testT1_ShouldFind_PreGoal_MainGoal_PostGoal() throws Exception
    {
        String mainGoal = "resolveTest:t1-main";
        String preGoal = "resolveTest:t1-preGoal";
        String postGoal = "resolveTest:t1-postGoal";

        PreGoal pg = new PreGoal();
        pg.setAttain( preGoal );
        pg.setName( mainGoal );

        List preGoals = new LinkedList();
        preGoals.add( pg );

        PostGoal pog = new PostGoal();
        pog.setAttain( postGoal );
        pog.setName( mainGoal );

        List postGoals = new LinkedList();
        postGoals.add( pog );

        Map messages = new TreeMap();

        messages.put( mainGoal, "Main goal is missing." );
        messages.put( preGoal, "preGoal is missing" );
        messages.put( postGoal, "postGoal is missing" );

        List order = new ArrayList();

        order.add( preGoal );
        order.add( mainGoal );
        order.add( postGoal );

        runTest( mainGoal, preGoals, postGoals, order, messages );
    }

    /*
     * <!-- Test main with prereq --> <mojo> <id>resolveTest:t2-prereq </id>
     * <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin
     * </implementation> <instantiationStrategy>singleton
     * </instantiationStrategy> </mojo> <mojo> <id>resolveTest:t2-main </id>
     * <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin
     * </implementation> <instantiationStrategy>singleton
     * </instantiationStrategy> <prereqs> <prereq>resolveTest:t2-prereq
     * </prereq> </prereqs> </mojo> <!-- End of test -->
     */
    public void testT2_ShouldFind_Prereq_MainGoal() throws Exception
    {
        String mainGoal = "resolveTest:t2-main";
        String prereq = "resolveTest:t2-prereq";

        Map messages = new TreeMap();

        messages.put( mainGoal, "Main goal is missing." );
        messages.put( prereq, "prereq is missing" );

        List order = new ArrayList();

        order.add( prereq );
        order.add( mainGoal );

        runTest( mainGoal, Collections.EMPTY_LIST, Collections.EMPTY_LIST, order, messages );
    }

    /*
     * <!-- Test main with prereq, preGoal and postGoal --> <mojo>
     * <id>resolveTest:t3-preGoal </id>
     * <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin
     * </implementation> <instantiationStrategy>singleton
     * </instantiationStrategy> </mojo> <mojo> <id>resolveTest:t3-prereq </id>
     * <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin
     * </implementation> <instantiationStrategy>singleton
     * </instantiationStrategy> </mojo> <mojo> <id>resolveTest:t3-main </id>
     * <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin
     * </implementation> <instantiationStrategy>singleton
     * </instantiationStrategy> <prereqs> <prereq>resolveTest:t3-prereq
     * </prereq> </prereqs> </mojo> <mojo> <id>resolveTest:t3-postGoal </id>
     * <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin
     * </implementation> <instantiationStrategy>singleton
     * </instantiationStrategy> </mojo> <!-- End of test -->
     */
    public void testT3_ShouldFind_PreGoal_Prereq_MainGoal_PostGoal() throws Exception
    {
        String mainGoal = "resolveTest:t3-main";
        String prereq = "resolveTest:t3-prereq";
        String preGoal = "resolveTest:t3-preGoal";
        String postGoal = "resolveTest:t3-postGoal";

        PreGoal pg = new PreGoal();
        pg.setAttain( preGoal );
        pg.setName( mainGoal );

        List preGoals = new LinkedList();
        preGoals.add( pg );

        PostGoal pog = new PostGoal();
        pog.setAttain( postGoal );
        pog.setName( mainGoal );

        List postGoals = new LinkedList();
        postGoals.add( pog );

        Map messages = new TreeMap();

        messages.put( mainGoal, "Main goal is missing." );
        messages.put( prereq, "prereq is missing" );
        messages.put( preGoal, "preGoal is missing" );
        messages.put( postGoal, "postGoal is missing" );

        List order = new ArrayList();

        order.add( preGoal );
        order.add( prereq );
        order.add( mainGoal );
        order.add( postGoal );

        runTest( mainGoal, preGoals, postGoals, order, messages );
    }

    /*
     * <!-- Test main with prereq which has preGoal and postGoal --> <mojo>
     * <id>resolveTest:t4-prereq-preGoal </id>
     * <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin
     * </implementation> <instantiationStrategy>singleton
     * </instantiationStrategy> </mojo> <mojo> <id>resolveTest:t4-prereq </id>
     * <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin
     * </implementation> <instantiationStrategy>singleton
     * </instantiationStrategy> </mojo> <mojo> <id>resolveTest:t4-main </id>
     * <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin
     * </implementation> <instantiationStrategy>singleton
     * </instantiationStrategy> <prereqs> <prereq>resolveTest:t4-prereq
     * </prereq> </prereqs> </mojo> <mojo> <id>resolveTest:t4-prereq-postGoal
     * </id>
     * <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin
     * </implementation> <instantiationStrategy>singleton
     * </instantiationStrategy> </mojo> <!-- End of test -->
     */
    public void testT4_ShouldFind_PreGoal_Prereq_PostGoal_MainGoal() throws Exception
    {
        String mainGoal = "resolveTest:t4-main";
        String prereq = "resolveTest:t4-prereq";
        String preGoal = "resolveTest:t4-prereq-preGoal";
        String postGoal = "resolveTest:t4-prereq-postGoal";

        PreGoal pg = new PreGoal();
        pg.setAttain( preGoal );
        pg.setName( prereq );

        List preGoals = new LinkedList();
        preGoals.add( pg );

        PostGoal pog = new PostGoal();
        pog.setAttain( postGoal );
        pog.setName( prereq );

        List postGoals = new LinkedList();
        postGoals.add( pog );

        Map messages = new TreeMap();

        messages.put( mainGoal, "Main goal is missing." );
        messages.put( prereq, "prereq is missing" );
        messages.put( preGoal, "preGoal is missing" );
        messages.put( postGoal, "postGoal is missing" );

        List order = new ArrayList();

        order.add( preGoal );
        order.add( prereq );
        order.add( postGoal );
        order.add( mainGoal );

        runTest( mainGoal, preGoals, postGoals, order, messages );
    }

    /*
     * <!-- Test main with prereq and preGoal which has the same prereq -->
     * <mojo> <id>resolveTest:t5-prereq </id>
     * <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin
     * </implementation> <instantiationStrategy>singleton
     * </instantiationStrategy> </mojo> <mojo> <id>resolveTest:t5-preGoal </id>
     * <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin
     * </implementation> <instantiationStrategy>singleton
     * </instantiationStrategy> <prereqs> <prereq>resolveTest:t5-prereq
     * </prereq> </prereqs> </mojo> <mojo> <id>resolveTest:t5-main </id>
     * <implementation>org.apache.maven.plugin.GoalDecorationAndResolutionTestPlugin
     * </implementation> <instantiationStrategy>singleton
     * </instantiationStrategy> <prereqs> <prereq>resolveTest:t5-prereq
     * </prereq> </prereqs> </mojo> <!-- End of test -->
     */
    public void testT5_ShouldFind_Prereq_PreGoal_MainGoal() throws Exception
    {
        String mainGoal = "resolveTest:t5-main";
        String prereq = "resolveTest:t5-prereq";
        String preGoal = "resolveTest:t5-preGoal";

        PreGoal pg = new PreGoal();
        pg.setAttain( preGoal );
        pg.setName( mainGoal );

        List preGoals = new LinkedList();
        preGoals.add( pg );

        Map messages = new TreeMap();

        messages.put( mainGoal, "Main goal is missing." );
        messages.put( prereq, "prereq is missing" );
        messages.put( preGoal, "preGoal is missing" );

        List order = new ArrayList();

        order.add( prereq );
        order.add( preGoal );
        order.add( mainGoal );

        runTest( mainGoal, preGoals, Collections.EMPTY_LIST, order, messages );
    }

    private void runTest( String mainGoal, List preGoals, List postGoals, List expectedOrder, Map messages )
        throws Exception
    {
        ArtifactRepository localRepository = new ArtifactRepository( "local", getTestRepoURL() );

        Model model = new Model();
        model.setPreGoals( preGoals );
        model.setPostGoals( postGoals );

        MavenProject project = new MavenProject( model );

        MavenGoalExecutionContext context = createGoalExecutionContext( project, localRepository, mainGoal );
        context.setGoalName( mainGoal );

        PluginResolutionPhase pluginPhase = new PluginResolutionPhase();
        GoalMappingPhase mappingPhase = new GoalMappingPhase();
        GoalResolutionPhase goalPhase = new GoalResolutionPhase();

        pluginPhase.execute( context );
        mappingPhase.execute( context );
        goalPhase.execute( context );

        List goals = context.getResolvedGoals();
        
        //System.out.println("Expected chain: " + expectedOrder);
        //System.out.println("Actual chain: " + goals);

        assertNotNull( goals );

        assertEquals( expectedOrder.size(), goals.size() );

        int index = 0;
        
        for ( Iterator it = expectedOrder.iterator(); it.hasNext(); )
        {
            String goal = (String) it.next();

            String failureMessage = (String) messages.get( goal );

            String resolvedGoal = (String) goals.get( index++ );

            assertEquals( failureMessage, goal, resolvedGoal );
        }
    }
}
