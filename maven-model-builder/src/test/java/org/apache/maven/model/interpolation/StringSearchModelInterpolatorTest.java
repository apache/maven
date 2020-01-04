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

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
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

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.powermock.reflect.Whitebox.getField;
import static org.powermock.reflect.Whitebox.getInternalState;

/**
 * @author jdcasey
 * @author Benjamin Bentmann
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
        interpolator = new StringSearchModelInterpolator();
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
        assertProblemFree( collector );

        assertEquals( "value", values[0] );
        assertEquals( "value2", values[1] );
    }

    private ModelBuildingRequest createModelBuildingRequest( Properties p )
    {
        ModelBuildingRequest config = new DefaultModelBuildingRequest();
        config.setSystemProperties( p );
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

        ModelBuildingRequest config = createModelBuildingRequest( p );

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree( collector );

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

        List<String> values = new ArrayList<>();
        values.add( "${key}" );
        values.add( "${key2}" );

        ObjectWithListField obj = new ObjectWithListField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest( p );

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree( collector );

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

        List<String> values = new ArrayList<>();
        values.add( "key" );
        values.add( "${key2}" );

        ObjectWithListField obj = new ObjectWithListField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest( p );

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree( collector );

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

        List<String> values = Collections.unmodifiableList( Collections.singletonList( "${key}" ) );

        ObjectWithListField obj = new ObjectWithListField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest( p );

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree( collector );

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

        List<String[]> values = new ArrayList<>();
        values.add( new String[] { "${key}", "${key2}" } );
        values.add( new String[] { "${key3}", "${key4}" } );

        ObjectWithListField obj = new ObjectWithListField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest( p );

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree( collector );

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

        Map<String, String> values = new HashMap<>();
        values.put( "key", "${key}" );
        values.put( "key2", "${key2}" );

        ObjectWithMapField obj = new ObjectWithMapField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest( p );

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree( collector );

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

        Map<String, String> values = new HashMap<>();
        values.put( "key", "val" );
        values.put( "key2", "${key2}" );

        ObjectWithMapField obj = new ObjectWithMapField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest( p );

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree( collector );

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

        Map<String, String> values = Collections.unmodifiableMap( Collections.singletonMap( "key", "${key}" ) );

        ObjectWithMapField obj = new ObjectWithMapField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest( p );

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree( collector );

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

        Map<String, String[]> values = new HashMap<>();
        values.put( "key", new String[] { "${key}", "${key2}" } );
        values.put( "key2", new String[] { "${key3}", "${key4}" } );

        ObjectWithMapField obj = new ObjectWithMapField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest( p );

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree( collector );

        assertEquals( "value", ( (String[]) obj.values.get( "key" ) )[0] );
        assertEquals( "value2", ( (String[]) obj.values.get( "key" ) )[1] );
        assertEquals( "value3", ( (String[]) obj.values.get( "key2" ) )[0] );
        assertEquals( "value4", ( (String[]) obj.values.get( "key2" ) )[1] );
    }

    public void testInterpolateObjectWithPomFile()
            throws Exception
    {
        Model model = new Model();
        model.setPomFile( new File( System.getProperty( "user.dir" ), "pom.xml" ) );
        File baseDir = model.getProjectDirectory();

        Properties p = new Properties();

        Map<String, String> values = new HashMap<>();
        values.put( "key", "${project.basedir}" + File.separator + "target" );

        ObjectWithMapField obj = new ObjectWithMapField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest( p );

        SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree( collector );

        assertThat( baseDir.getCanonicalPath(), is( System.getProperty( "user.dir" ) ) );
        assertThat( obj.values.size(), is( 1 ) );
        assertThat( (String) obj.values.get( "key" ), is( anyOf(
                is( System.getProperty( "user.dir" ) + File.separator + "target" ),
                // TODO why MVN adds dot /./ in paths???
                is( System.getProperty( "user.dir" ) + File.separator + '.' + File.separator + "target" )
        ) ) );
    }

    public void testNotInterpolateObjectWithFile()
            throws Exception
    {
        Model model = new Model();

        File baseDir = new File( System.getProperty( "user.dir" ) );

        Properties p = new Properties();

        ObjectWithNotInterpolatedFile obj = new ObjectWithNotInterpolatedFile( baseDir );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest( p );

        SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
        assertProblemFree( collector );

        //noinspection unchecked
        Map<Class<?>, ?> cache =
                (Map<Class<?>, ?>) getField( StringSearchModelInterpolator.class, "CACHED_ENTRIES" )
                        .get( null );

        Object objCacheItem = cache.get( Object.class );
        Object fileCacheItem = cache.get( File.class );

        assertNotNull( objCacheItem );
        assertNotNull( fileCacheItem );

        assertThat( ( (Object[]) getInternalState( objCacheItem, "fields" ) ).length, is( 0 ) );
        assertThat( ( (Object[]) getInternalState( fileCacheItem, "fields" ) ).length, is( 0 ) );
    }

    public void testNotInterpolateFile()
            throws Exception
    {
        Model model = new Model();

        File baseDir = new File( System.getProperty( "user.dir" ) );

        Properties p = new Properties();

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ModelBuildingRequest config = createModelBuildingRequest( p );

        SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateObject( baseDir, model, new File( "." ), config, collector );
        assertProblemFree( collector );

        //noinspection unchecked
        Map<Class<?>, ?> cache =
                (Map<Class<?>, ?>) getField( StringSearchModelInterpolator.class, "CACHED_ENTRIES" )
                        .get( null );

        Object fileCacheItem = cache.get( File.class );

        assertNotNull( fileCacheItem );

        assertThat( ( (Object[]) getInternalState( fileCacheItem, "fields" ) ).length, is( 0 ) );
    }


    public void testConcurrentInterpolation()
        throws Exception
    {
        final Model model = new Model();

        final Properties p = new Properties();
        p.setProperty( "key", "value" );
        p.setProperty( "key2", "value2" );
        p.setProperty( "key3", "value3" );
        p.setProperty( "key4", "value4" );
        p.setProperty( "key5", "value5" );

        final StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();


        int numItems = 100;
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        List<Future<SimpleProblemCollector>>  futures = new ArrayList<>();
        for ( int i = 0; i < numItems; i++ )
        {
            Callable<SimpleProblemCollector> future = () -> {
                final ObjectWithMixedProtection obj = getValueList();
                final ModelBuildingRequest config = createModelBuildingRequest( p );

                countDownLatch.await();
                final SimpleProblemCollector collector = new SimpleProblemCollector();
                interpolator.interpolateObject( obj, model, new File( "." ), config, collector );
                return collector;
            };
            FutureTask<SimpleProblemCollector> task = new FutureTask<>( future );
            futures.add( task );
            new Thread( task ).start();
        }
        countDownLatch.countDown(); // Start all the threads
        for ( Future<SimpleProblemCollector> result : futures )
        {
            SimpleProblemCollector problemCollector = result.get(); // ArrayIndexOutOfBoundsException are typical indication of threading issues
            assertProblemFree( problemCollector );
        }
    }

    private ObjectWithMixedProtection getValueList()
    {
        List<String[]> values = new ArrayList<>();

        values.add( new String[] { "${key}", "${key2}" } );
        values.add( new String[] { "${key3}", "${key4}" } );
        List<String> values2 = new ArrayList<>();
        values.add( new String[] { "${key}", "${key2}" } );
        values.add( new String[] { "${key3}", "${key4}" } );
        List<String> values3 = new ArrayList<>();
        values.add( new String[] { "${key}", "${key2}" } );
        values.add( new String[] { "${key3}", "${key4}" } );

        return new ObjectWithMixedProtection( values, values2, values3, "${key5}" );
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
        private final List<?> values;

        public ObjectWithListField( List<?> values )
        {
            this.values = values;
        }
    }

    private static final class ObjectWithMapField
    {
        private final Map<?, ?> values;

        public ObjectWithMapField( Map<?, ?> values )
        {
            this.values = values;
        }
    }

    private static final class ObjectWithNotInterpolatedFile
    {
        private final File f;

        ObjectWithNotInterpolatedFile( File f )
        {
            this.f = f;
        }
    }

    @SuppressWarnings( "unused" )
    private static final class ObjectWithMixedProtection
    {
        private List<?> values1;
        protected List<?> values2;
        List<?> values3;
        private String fooBar;

        private ObjectWithMixedProtection( List<?> values1, List<?> values2, List<?> values3 )
        {
            this.values1 = values1;
            this.values2 = values2;
            this.values3 = values3;
        }

        private ObjectWithMixedProtection( List<?> values1, List<?> values2, List<?> values3, String fooBar )
        {
            this.values1 = values1;
            this.values2 = values2;
            this.values3 = values3;
            this.fooBar = fooBar;
        }

        public String getFooBar()
        {
            return fooBar;
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

    static class ClassWithFinalField
    {
        public static final String CONSTANT = "${expression}";
    }

    public void testLocationTrackerShouldBeExcludedFromInterpolation()
    {
        Properties props = new Properties();
        props.setProperty( "expression", "value" );
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setUserProperties( props );

        InputSource source = new InputSource();
        source.setLocation( "${expression}" );
        source.setModelId( "${expression}" );
        Model model = new Model();
        model.setLocation( "", new InputLocation( 1, 1, source ) );

        SimpleProblemCollector problems = new SimpleProblemCollector();
        StringSearchModelInterpolator interpolator = new StringSearchModelInterpolator();
        interpolator.interpolateObject( model, model, null, request, problems );

        assertProblemFree( problems );
        assertEquals( "${expression}", source.getLocation() );
        assertEquals( "${expression}", source.getModelId() );
    }

}
