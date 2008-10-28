package org.apache.maven.project.interpolation;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.path.PathTranslator;

/**
 * @author jdcasey
 * @version $Id$
 */
public class StringSearchModelInterpolatorTest
    extends AbstractModelInterpolatorTest
{
    protected ModelInterpolator createInterpolator( PathTranslator translator )
        throws Exception
    {
        StringSearchModelInterpolator interpolator = new StringSearchModelInterpolator( translator );
        interpolator.initialize();

        return interpolator;
    }

    protected ModelInterpolator createInterpolator()
        throws Exception
    {
        StringSearchModelInterpolator interpolator = new StringSearchModelInterpolator();
        interpolator.initialize();

        return interpolator;
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

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setExecutionProperties( p );

        interpolator.interpolateObject( values, model, new File( "." ), config, true );

        assertEquals( "value", values[0] );
        assertEquals( "value2", values[1] );
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

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setExecutionProperties( p );

        interpolator.interpolateObject( obj, model, new File( "." ), config, true );

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

        List values = new ArrayList();
        values.add( "${key}" );
        values.add( "${key2}" );

        ObjectWithListField obj = new ObjectWithListField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setExecutionProperties( p );

        interpolator.interpolateObject( obj, model, new File( "." ), config, true );

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

        List values = new ArrayList();
        values.add( "key" );
        values.add( "${key2}" );

        ObjectWithListField obj = new ObjectWithListField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setExecutionProperties( p );

        interpolator.interpolateObject( obj, model, new File( "." ), config, true );

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

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setExecutionProperties( p );

        interpolator.interpolateObject( obj, model, new File( "." ), config, true );

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

        List values = new ArrayList();
        values.add( new String[] { "${key}", "${key2}" } );
        values.add( new String[] { "${key3}", "${key4}" } );

        ObjectWithListField obj = new ObjectWithListField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setExecutionProperties( p );

        interpolator.interpolateObject( obj, model, new File( "." ), config, true );

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

        Map values = new HashMap();
        values.put( "key", "${key}" );
        values.put( "key2", "${key2}" );

        ObjectWithMapField obj = new ObjectWithMapField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setExecutionProperties( p );

        interpolator.interpolateObject( obj, model, new File( "." ), config, true );

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

        Map values = new HashMap();
        values.put( "key", "val" );
        values.put( "key2", "${key2}" );

        ObjectWithMapField obj = new ObjectWithMapField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setExecutionProperties( p );

        interpolator.interpolateObject( obj, model, new File( "." ), config, true );

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

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setExecutionProperties( p );

        interpolator.interpolateObject( obj, model, new File( "." ), config, true );

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

        Map values = new HashMap();
        values.put( "key", new String[] { "${key}", "${key2}" } );
        values.put( "key2", new String[] { "${key3}", "${key4}" } );

        ObjectWithMapField obj = new ObjectWithMapField( values );

        StringSearchModelInterpolator interpolator = (StringSearchModelInterpolator) createInterpolator();

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setExecutionProperties( p );

        interpolator.interpolateObject( obj, model, new File( "." ), config, true );

        assertEquals( "value", ( (String[]) obj.values.get( "key" ) )[0] );
        assertEquals( "value2", ( (String[]) obj.values.get( "key" ) )[1] );
        assertEquals( "value3", ( (String[]) obj.values.get( "key2" ) )[0] );
        assertEquals( "value4", ( (String[]) obj.values.get( "key2" ) )[1] );
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

}
