package org.apache.maven.model.interpolation;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.SimpleProblemCollector;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * @author jdcasey
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class StringSearchModelInterpolatorTest
    extends AbstractModelInterpolatorTest 
{


    protected ModelInterpolator interpolator;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        interpolator =  lookup(ModelInterpolator.class);
    }


    protected ModelInterpolator createInterpolator( org.apache.maven.model.path.PathTranslator translator )
        throws Exception
    {
        return this.interpolator;
    }

    protected ModelInterpolator createInterpolator()
        throws Exception
    {
        return this.interpolator;
    }

    public void testInterpolateStringArray()
        throws Exception
    {
        Model model = new Model();

        Properties p = new Properties();
        p.setProperty( "key", "value" );
        p.setProperty( "key2", "value2" );

        String[] values = { "${key}", "${key2}" };

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest(p);

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( values, model, new File( "." ), config, collector );
        assertProblemFree(  collector );
        

        assertEquals( "value", values[0] );
        assertEquals( "value2", values[1] );
    }

    private ModelBuildingRequest createModelBuildingRequest(Properties p) {
        ModelBuildingRequest config = new DefaultModelBuildingRequest();
        config.setSystemProperties( p);
        return config;
    }

    public void testInterpolateObjectWithStringArrayField()
        throws Exception
    {
        Model model = new Model();

        Properties p = new Properties();
        p.setProperty( "key", "value" );
        p.setProperty( "key2", "value2" );

        String[] values = { "${key}", "${key2}" };

        ObjectWithStringArrayField obj = new ObjectWithStringArrayField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest(p);

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree(  collector );
        

        assertEquals( "value", obj.values[0] );
        assertEquals( "value2", obj.values[1] );
    }

    public void testInterpolateObjectWithStringListField()
        throws Exception
    {
        Model model = new Model();

        Properties p = new Properties();
        p.setProperty( "key", "value" );
        p.setProperty( "key2", "value2" );

        List<String> values = new ArrayList<String>();
        values.add( "${key}" );
        values.add( "${key2}" );

        ObjectWithListField obj = new ObjectWithListField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest(p);

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree(  collector );
        

        assertEquals( "value", obj.values.get( 0 ) );
        assertEquals( "value2", obj.values.get( 1 ) );
    }

    public void testInterpolateObjectWithStringListFieldAndOneLiteralValue()
        throws Exception
    {
        Model model = new Model();

        Properties p = new Properties();
        p.setProperty( "key", "value" );
        p.setProperty( "key2", "value2" );

        List<String> values = new ArrayList<String>();
        values.add( "key" );
        values.add( "${key2}" );

        ObjectWithListField obj = new ObjectWithListField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest(p);

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree(  collector );
        

        assertEquals( "key", obj.values.get( 0 ) );
        assertEquals( "value2", obj.values.get( 1 ) );
    }

    public void testInterpolateObjectWithUnmodifiableStringListField()
        throws Exception
    {
        Model model = new Model();

        Properties p = new Properties();
        p.setProperty( "key", "value" );
        p.setProperty( "key2", "value2" );

        List values = Collections.unmodifiableList( Collections.singletonList( "${key}" ) );

        ObjectWithListField obj = new ObjectWithListField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest(p);

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree(  collector );
        

        assertEquals( "${key}", obj.values.get( 0 ) );
    }

    public void testInterpolateObjectWithStringArrayListField()
        throws Exception
    {
        Model model = new Model();

        Properties p = new Properties();
        p.setProperty( "key", "value" );
        p.setProperty( "key2", "value2" );
        p.setProperty( "key3", "value3" );
        p.setProperty( "key4", "value4" );

        List<String[]> values = new ArrayList<String[]>();
        values.add( new String[] { "${key}", "${key2}" } );
        values.add( new String[] { "${key3}", "${key4}" } );

        ObjectWithListField obj = new ObjectWithListField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest(p);

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree(  collector );
        

        assertEquals( "value", ( (String[]) obj.values.get( 0 ) )[0] );
        assertEquals( "value2", ( (String[]) obj.values.get( 0 ) )[1] );
        assertEquals( "value3", ( (String[]) obj.values.get( 1 ) )[0] );
        assertEquals( "value4", ( (String[]) obj.values.get( 1 ) )[1] );
    }

    public void testInterpolateObjectWithStringToStringMapField()
        throws Exception
    {
        Model model = new Model();

        Properties p = new Properties();
        p.setProperty( "key", "value" );
        p.setProperty( "key2", "value2" );

        Map<String, String> values = new HashMap<String, String>();
        values.put( "key", "${key}" );
        values.put( "key2", "${key2}" );

        ObjectWithMapField obj = new ObjectWithMapField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest(p);

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree(  collector );
        

        assertEquals( "value", obj.values.get( "key" ) );
        assertEquals( "value2", obj.values.get( "key2" ) );
    }

    public void testInterpolateObjectWithStringToStringMapFieldAndOneLiteralValue()
        throws Exception
    {
        Model model = new Model();

        Properties p = new Properties();
        p.setProperty( "key", "value" );
        p.setProperty( "key2", "value2" );

        Map<String, String> values = new HashMap<String, String>();
        values.put( "key", "val" );
        values.put( "key2", "${key2}" );

        ObjectWithMapField obj = new ObjectWithMapField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest(p);

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree(  collector );
        

        assertEquals( "val", obj.values.get( "key" ) );
        assertEquals( "value2", obj.values.get( "key2" ) );
    }

    public void testInterpolateObjectWithUnmodifiableStringToStringMapField()
        throws Exception
    {
        Model model = new Model();

        Properties p = new Properties();
        p.setProperty( "key", "value" );
        p.setProperty( "key2", "value2" );

        Map values = Collections.unmodifiableMap( Collections.singletonMap( "key", "${key}" ) );

        ObjectWithMapField obj = new ObjectWithMapField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest(p);

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree(  collector );
        

        assertEquals( "${key}", obj.values.get( "key" ) );
    }

    public void testInterpolateObjectWithStringToStringArrayMapField()
        throws Exception
    {
        Model model = new Model();

        Properties p = new Properties();
        p.setProperty( "key", "value" );
        p.setProperty( "key2", "value2" );
        p.setProperty( "key3", "value3" );
        p.setProperty( "key4", "value4" );

        Map<String, String[]> values = new HashMap<String, String[]>();
        values.put( "key", new String[] { "${key}", "${key2}" } );
        values.put( "key2", new String[] { "${key3}", "${key4}" } );

        ObjectWithMapField obj = new ObjectWithMapField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest(p);

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree(  collector );
        

        assertEquals( "value", ( (String[]) obj.values.get( "key" ) )[0] );
        assertEquals( "value2", ( (String[]) obj.values.get( "key" ) )[1] );
        assertEquals( "value3", ( (String[]) obj.values.get( "key2" ) )[0] );
        assertEquals( "value4", ( (String[]) obj.values.get( "key2" ) )[1] );
    }


    public void testConcurrentInterpolation() throws Exception {
        final Model model = new Model();

        Properties p = new Properties();
        p.setProperty( "key", "value" );
        p.setProperty( "key2", "value2" );
        p.setProperty( "key3", "value3" );
        p.setProperty( "key4", "value4" );

        List<String[]> values = new ArrayList<String[]>();

        values.add( new String[] { "${key}", "${key2}" } );
        values.add( new String[] { "${key3}", "${key4}" } );
        List values2 = new ArrayList();
        values.add( new String[] { "${key}", "${key2}" } );
        values.add( new String[] { "${key3}", "${key4}" } );
        List values3 = new ArrayList();
        values.add( new String[] { "${key}", "${key2}" } );
        values.add( new String[] { "${key3}", "${key4}" } );

        // There is an interesting issue here; if I send three identical collections into the three Lists in "obj",
        // like this:
        // final ObjectWithMixedProtection obj = new ObjectWithMixedProtection( values, values, values );
        // I will have concurrency issues on the interpolation of the individual collections, since current
        // synchronization is per-field and not per-underlying object.
        // If this turns out to be a realistic use case, we will need to synchronize on the underlying collection
        // in the interpolate method.

        final ObjectWithMixedProtection obj = new ObjectWithMixedProtection( values, values2, values3 );
        final StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();
        final ModelBuildingRequest config = createModelBuildingRequest(p);


        int numItems = 250;
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        List<Future<SimpleProblemCollector>>  futures = new ArrayList<Future<SimpleProblemCollector>>();
        for (int i = 0; i < numItems; i++){
            Callable<SimpleProblemCollector> future = new Callable<SimpleProblemCollector>() {
                public SimpleProblemCollector call() throws Exception {
                    countDownLatch.await();
                    final SimpleProblemCollector collector = new SimpleProblemCollector();
                    interpolator.interpolateObject( obj, model, new File( "." ), config, collector);
                    return collector;
                }
            };
            FutureTask<SimpleProblemCollector> task = new FutureTask<SimpleProblemCollector>(future);
            futures.add ( task);
            new Thread( task).start();
        }
        countDownLatch.countDown(); // Start all the threads
        for(Future<SimpleProblemCollector> result : futures){
            result.get(); // ArrayIndexOutOfBoundsException are typical indication of threading issues
        }
    }


    private static final class ObjectWithStringArrayField
    {
        private final String[] values;

        public ObjectWithStringArrayField( String[] values )
        {
            this.values = values;
        }
    }

    private static final class ObjectWithListField
    {
        private final List values;

        public ObjectWithListField( List values )
        {
            this.values = values;
        }
    }

    private static final class ObjectWithMapField
    {
        private final Map values;

        public ObjectWithMapField( Map values )
        {
            this.values = values;
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private static final class ObjectWithMixedProtection
    {
        private List values1;
        protected List values2;
        List values3;

        private ObjectWithMixedProtection(List values1, List values2, List values3) {
            this.values1 = values1;
            this.values2 = values2;
            this.values3 = values3;
        }
    }
    
    public void testFinalFieldsExcludedFromInterpolation()
    {
        Properties props = new Properties();
        props.setProperty( "expression", "value" );
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setUserProperties( props );

        SimpleProblemCollector problems = new SimpleProblemCollector();
        StringSearchModelInterpolator interpolator = new StringSearchModelInterpolator();
        interpolator.interpolateObject( new ClassWithFinalField(), new Model(), null, request, problems );

        assertProblemFree(  problems );
    }

    @SuppressWarnings({"UnusedDeclaration"})
    static class ClassWithFinalField
    {
        public static final String CONSTANT = "${expression}";
    }
}
