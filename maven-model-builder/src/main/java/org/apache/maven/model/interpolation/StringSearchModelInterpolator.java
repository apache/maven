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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.interpolation.InterpolationPostProcessor;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Component( role = ModelInterpolator.class )
public class StringSearchModelInterpolator
    extends AbstractStringBasedModelInterpolator
{

    private static final Map<Class<?>, Field[]> fieldsByClass = new WeakHashMap<Class<?>, Field[]>();
    private static final Map<Class<?>, Boolean> fieldIsPrimitiveByClass = new WeakHashMap<Class<?>, Boolean>();

    public Model interpolateModel( Model model, File projectDir, ModelBuildingRequest config,
                                   ModelProblemCollector problems )
    {
        interpolateObject( model, model, projectDir, config, problems );

        return model;
    }

    protected void interpolateObject( Object obj, Model model, File projectDir, ModelBuildingRequest config, ModelProblemCollector problems )
    {
        try
        {
            List<? extends ValueSource> valueSources = createValueSources( model, projectDir, config, problems );
            List<? extends InterpolationPostProcessor> postProcessors = createPostProcessors( model, projectDir, config );

            InterpolateObjectAction action =
                new InterpolateObjectAction( obj, valueSources, postProcessors, this, problems );

            AccessController.doPrivileged( action );
        }
        finally
        {
            getInterpolator().clearAnswers();
        }
    }

    protected Interpolator createInterpolator()
    {
        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.setCacheAnswers( true );

        return interpolator;
    }

    private static final class InterpolateObjectAction implements PrivilegedAction<Object>
    {

        private final LinkedList<Object> interpolationTargets;
        private final StringSearchModelInterpolator modelInterpolator;
        private final List<? extends ValueSource> valueSources;
        private final List<? extends InterpolationPostProcessor> postProcessors;
        private final ModelProblemCollector problems;

        public InterpolateObjectAction( Object target, List<? extends ValueSource> valueSources,
                                        List<? extends InterpolationPostProcessor> postProcessors,
                                        StringSearchModelInterpolator modelInterpolator, ModelProblemCollector problems )
        {
            this.valueSources = valueSources;
            this.postProcessors = postProcessors;

            this.interpolationTargets = new LinkedList<Object>();
            interpolationTargets.add( target );

            this.modelInterpolator = modelInterpolator;

            this.problems = problems;
        }

        public Object run()
        {
            while ( !interpolationTargets.isEmpty() )
            {
                Object obj = interpolationTargets.removeFirst();

                traverseObjectWithParents( obj.getClass(), obj );
            }

            return null;
        }

        @SuppressWarnings("unchecked")
        private void traverseObjectWithParents( Class<?> cls, Object target )
        {
            if ( cls == null )
            {
                return;
            }


            if ( cls.isArray() )
            {
                evaluateArray( target );
            }
            else if ( isQualifiedForInterpolation( cls ) )
            {
                Field[] fields = fieldsByClass.get( cls );
                if ( fields == null )
                {
                    fields = cls.getDeclaredFields();
                    fieldsByClass.put( cls, fields );
                }

                for ( int i = 0; i < fields.length; i++ )
                {
                    Class<?> type = fields[i].getType();
                    if ( isQualifiedForInterpolation( fields[i], type ) )
                    {
                        boolean isAccessible = fields[i].isAccessible();
                        fields[i].setAccessible( true );
                        try
                        {
                            try
                            {
                                if ( String.class == type )
                                {
                                    String value = (String) fields[i].get( target );
                                    if ( value != null )
                                    {
                                        String interpolated =
                                            modelInterpolator.interpolateInternal( value, valueSources, postProcessors,
                                                                                   problems );

                                        if ( !interpolated.equals( value ) )
                                        {
                                            fields[i].set( target, interpolated );
                                        }
                                    }
                                }
                                else if ( Collection.class.isAssignableFrom( type ) )
                                {
                                    Collection<Object> c = (Collection<Object>) fields[i].get( target );
                                    if ( c != null && !c.isEmpty() )
                                    {
                                        List<Object> originalValues = new ArrayList<Object>( c );
                                        try
                                        {
                                            c.clear();
                                        }
                                        catch ( UnsupportedOperationException e )
                                        {
                                            continue;
                                        }

                                        for ( Object value : originalValues )
                                        {
                                            if ( value != null )
                                            {
                                                if ( String.class == value.getClass() )
                                                {
                                                    String interpolated =
                                                        modelInterpolator.interpolateInternal( (String) value,
                                                                                               valueSources,
                                                                                               postProcessors, problems );

                                                    if ( !interpolated.equals( value ) )
                                                    {
                                                        c.add( interpolated );
                                                    }
                                                    else
                                                    {
                                                        c.add( value );
                                                    }
                                                }
                                                else
                                                {
                                                    c.add( value );
                                                    if ( value.getClass().isArray() )
                                                    {
                                                        evaluateArray( value );
                                                    }
                                                    else
                                                    {
                                                        interpolationTargets.add( value );
                                                    }
                                                }
                                            }
                                            else
                                            {
                                                // add the null back in...not sure what else to do...
                                                c.add( value );
                                            }
                                        }
                                    }
                                }
                                else if ( Map.class.isAssignableFrom( type ) )
                                {
                                    Map<Object, Object> m = (Map<Object, Object>) fields[i].get( target );
                                    if ( m != null && !m.isEmpty() )
                                    {
                                        for ( Map.Entry<Object, Object> entry : m.entrySet() )
                                        {
                                            Object value = entry.getValue();

                                            if ( value != null )
                                            {
                                                if ( String.class == value.getClass() )
                                                {
                                                    String interpolated =
                                                        modelInterpolator.interpolateInternal( (String) value,
                                                                                               valueSources,
                                                                                               postProcessors, problems );

                                                    if ( !interpolated.equals( value ) )
                                                    {
                                                        try
                                                        {
                                                            entry.setValue( interpolated );
                                                        }
                                                        catch ( UnsupportedOperationException e )
                                                        {
                                                            continue;
                                                        }
                                                    }
                                                }
                                                else
                                                {
                                                    if ( value.getClass().isArray() )
                                                    {
                                                        evaluateArray( value );
                                                    }
                                                    else
                                                    {
                                                        interpolationTargets.add( value );
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                else
                                {
                                    Object value = fields[i].get( target );
                                    if ( value != null )
                                    {
                                        if ( fields[i].getType().isArray() )
                                        {
                                            evaluateArray( value );
                                        }
                                        else
                                        {
                                            interpolationTargets.add( value );
                                        }
                                    }
                                }
                            }
                            catch ( IllegalArgumentException e )
                            {
                                problems.add( Severity.ERROR, "Failed to interpolate field: " + fields[i]
                                    + " on class: " + cls.getName(), e );
                            }
                            catch ( IllegalAccessException e )
                            {
                                problems.add( Severity.ERROR, "Failed to interpolate field: " + fields[i]
                                    + " on class: " + cls.getName(), e );
                            }
                        }
                        finally
                        {
                            fields[i].setAccessible( isAccessible );
                        }
                    }
                }

                traverseObjectWithParents( cls.getSuperclass(), target );
            }
        }

        private boolean isQualifiedForInterpolation( Class<?> cls )
        {
            return !cls.getName().startsWith( "java" );
        }

        private boolean isQualifiedForInterpolation( Field field, Class<?> fieldType )
        {
            Boolean primitive = fieldIsPrimitiveByClass.get( fieldType );
            if ( primitive == null )
            {
                primitive = Boolean.valueOf( fieldType.isPrimitive() );
                fieldIsPrimitiveByClass.put( fieldType, primitive );
            }
            if ( primitive.booleanValue() )
            {
                return false;
            }

            if ( "parent".equals( field.getName() ) )
            {
                return false;
            }

            if ( Modifier.isFinal( field.getModifiers() ) )
            {
                return false;
            }

            return true;
        }

        private void evaluateArray( Object target )
        {
            int len = Array.getLength( target );
            for ( int i = 0; i < len; i++ )
            {
                Object value = Array.get( target, i );
                if ( value != null )
                {
                    if ( String.class == value.getClass() )
                    {
                        String interpolated =
                            modelInterpolator.interpolateInternal( (String) value, valueSources, postProcessors,
                                                                   problems );

                        if ( !interpolated.equals( value ) )
                        {
                            Array.set( target, i, interpolated );
                        }
                    }
                    else
                    {
                        interpolationTargets.add( value );
                    }
                }
            }
        }
    }

}
