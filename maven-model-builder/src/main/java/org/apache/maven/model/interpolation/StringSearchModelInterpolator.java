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
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * StringSearchModelInterpolator
 */
@Component( role = ModelInterpolator.class )
public class StringSearchModelInterpolator
    extends AbstractStringBasedModelInterpolator
{

    private static final Map<Class<?>, InterpolateObjectAction.CacheItem> CACHED_ENTRIES =
        new ConcurrentHashMap<>( 80, 0.75f, 2 );
    // Empirical data from 3.x, actual =40


    @Override
    public Model interpolateModel( Model model, File projectDir, ModelBuildingRequest config,
                                   ModelProblemCollector problems )
    {
        interpolateObject( model, model, projectDir, config, problems );

        return model;
    }

    protected void interpolateObject( Object obj, Model model, File projectDir, ModelBuildingRequest config,
                                      ModelProblemCollector problems )
    {
        try
        {
            List<? extends ValueSource> valueSources = createValueSources( model, projectDir, config, problems );
            List<? extends InterpolationPostProcessor> postProcessors =
                createPostProcessors( model, projectDir, config );

            InterpolateObjectAction action =
                new InterpolateObjectAction( obj, valueSources, postProcessors, this, problems );

            AccessController.doPrivileged( action );
        }
        finally
        {
            getInterpolator().clearAnswers();
        }
    }

    @Override
    protected Interpolator createInterpolator()
    {
        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.setCacheAnswers( true );

        return interpolator;
    }

    private static final class InterpolateObjectAction
        implements PrivilegedAction<Object>
    {

        private final LinkedList<Object> interpolationTargets;

        private final StringSearchModelInterpolator modelInterpolator;

        private final List<? extends ValueSource> valueSources;

        private final List<? extends InterpolationPostProcessor> postProcessors;

        private final ModelProblemCollector problems;

        InterpolateObjectAction( Object target, List<? extends ValueSource> valueSources,
                                 List<? extends InterpolationPostProcessor> postProcessors,
                                 StringSearchModelInterpolator modelInterpolator, ModelProblemCollector problems )
        {
            this.valueSources = valueSources;
            this.postProcessors = postProcessors;

            this.interpolationTargets = new LinkedList<>();
            interpolationTargets.add( target );

            this.modelInterpolator = modelInterpolator;

            this.problems = problems;
        }

        @Override
        public Object run()
        {
            while ( !interpolationTargets.isEmpty() )
            {
                Object obj = interpolationTargets.removeFirst();

                traverseObjectWithParents( obj.getClass(), obj );
            }

            return null;
        }


        private String interpolate( String value )
        {
            return modelInterpolator.interpolateInternal( value, valueSources, postProcessors, problems );
        }

        private void traverseObjectWithParents( Class<?> cls, Object target )
        {
            if ( cls == null )
            {
                return;
            }

            CacheItem cacheEntry = getCacheEntry( cls );
            if ( cacheEntry.isArray() )
            {
                evaluateArray( target, this );
            }
            else if ( cacheEntry.isQualifiedForInterpolation )
            {
                cacheEntry.interpolate( target, this );

                traverseObjectWithParents( cls.getSuperclass(), target );
            }
        }


        private CacheItem getCacheEntry( Class<?> cls )
        {
            CacheItem cacheItem = CACHED_ENTRIES.get( cls );
            if ( cacheItem == null )
            {
                cacheItem = new CacheItem( cls );
                CACHED_ENTRIES.put( cls, cacheItem );
            }
            return cacheItem;
        }

        private static void evaluateArray( Object target, InterpolateObjectAction ctx )
        {
            int len = Array.getLength( target );
            for ( int i = 0; i < len; i++ )
            {
                Object value = Array.get( target, i );
                if ( value != null )
                {
                    if ( String.class == value.getClass() )
                    {
                        String interpolated = ctx.interpolate( (String) value );

                        if ( !interpolated.equals( value ) )
                        {
                            Array.set( target, i, interpolated );
                        }
                    }
                    else
                    {
                        ctx.interpolationTargets.add( value );
                    }
                }
            }
        }

        private static class CacheItem
        {
            private final boolean isArray;

            private final boolean isQualifiedForInterpolation;

            private final CacheField[] fields;

            private boolean isQualifiedForInterpolation( Class<?> cls )
            {
                return !cls.getName().startsWith( "java" );
            }

            private boolean isQualifiedForInterpolation( Field field, Class<?> fieldType )
            {
                if ( Map.class.equals( fieldType ) && "locations".equals( field.getName() ) )
                {
                    return false;
                }

                //noinspection SimplifiableIfStatement
                if ( fieldType.isPrimitive() )
                {
                    return false;
                }

                return !"parent".equals( field.getName() );
            }

            CacheItem( Class clazz )
            {
                this.isQualifiedForInterpolation = isQualifiedForInterpolation( clazz );
                this.isArray = clazz.isArray();
                List<CacheField> fields = new ArrayList<>();
                for ( Field currentField : clazz.getDeclaredFields() )
                {
                    Class<?> type = currentField.getType();
                    if ( isQualifiedForInterpolation( currentField, type ) )
                    {
                        if ( String.class == type )
                        {
                            if ( !Modifier.isFinal( currentField.getModifiers() ) )
                            {
                                fields.add( new StringField( currentField ) );
                            }
                        }
                        else if ( List.class.isAssignableFrom( type ) )
                        {
                            fields.add( new ListField( currentField ) );
                        }
                        else if ( Collection.class.isAssignableFrom( type ) )
                        {
                            throw new RuntimeException( "We dont interpolate into collections, use a list instead" );
                        }
                        else if ( Map.class.isAssignableFrom( type ) )
                        {
                            fields.add( new MapField( currentField ) );
                        }
                        else
                        {
                            fields.add( new ObjectField( currentField ) );
                        }
                    }
                }
                this.fields = fields.toArray( new CacheField[0] );

            }

            public void interpolate( Object target, InterpolateObjectAction interpolateObjectAction )
            {
                for ( CacheField field : fields )
                {
                    field.interpolate( target, interpolateObjectAction );
                }
            }

            public boolean isArray()
            {
                return isArray;
            }
        }

        abstract static class CacheField
        {
            protected final Field field;

            CacheField( Field field )
            {
                this.field = field;
            }

            void interpolate( Object target, InterpolateObjectAction interpolateObjectAction )
            {
                synchronized ( field )
                {
                    boolean isAccessible = field.isAccessible();
                    field.setAccessible( true );
                    try
                    {
                        doInterpolate( target, interpolateObjectAction );
                    }
                    catch ( IllegalArgumentException e )
                    {
                        interpolateObjectAction.problems.add(
                            new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE ).setMessage(
                                "Failed to interpolate field3: " + field + " on class: "
                                    + field.getType().getName() ).setException(
                                e ) ); // TODO Not entirely the same message
                    }
                    catch ( IllegalAccessException e )
                    {
                        interpolateObjectAction.problems.add(
                            new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE ).setMessage(
                                "Failed to interpolate field4: " + field + " on class: "
                                    + field.getType().getName() ).setException( e ) );
                    }
                    finally
                    {
                        field.setAccessible( isAccessible );
                    }
                }


            }

            abstract void doInterpolate( Object target, InterpolateObjectAction ctx )
                throws IllegalAccessException;
        }

        static final class StringField
            extends CacheField
        {
            StringField( Field field )
            {
                super( field );
            }

            @Override
            void doInterpolate( Object target, InterpolateObjectAction ctx )
                throws IllegalAccessException
            {
                String value = (String) field.get( target );
                if ( value == null )
                {
                    return;
                }

                String interpolated = ctx.interpolate( value );

                if ( !interpolated.equals( value ) )
                {
                    field.set( target, interpolated );
                }
            }
        }

        static final class ListField
            extends CacheField
        {
            ListField( Field field )
            {
                super( field );
            }

            @Override
            void doInterpolate( Object target, InterpolateObjectAction ctx )
                throws IllegalAccessException
            {
                @SuppressWarnings( "unchecked" ) List<Object> c = (List<Object>) field.get( target );
                if ( c == null )
                {
                    return;
                }

                int size = c.size();
                Object value;
                for ( int i = 0; i < size; i++ )
                {

                    value = c.get( i );

                    if ( value != null )
                    {
                        if ( String.class == value.getClass() )
                        {
                            String interpolated = ctx.interpolate( (String) value );

                            if ( !interpolated.equals( value ) )
                            {
                                try
                                {
                                    c.set( i, interpolated );
                                }
                                catch ( UnsupportedOperationException e )
                                {
                                    return;
                                }
                            }
                        }
                        else
                        {
                            if ( value.getClass().isArray() )
                            {
                                evaluateArray( value, ctx );
                            }
                            else
                            {
                                ctx.interpolationTargets.add( value );
                            }
                        }
                    }
                }
            }
        }

        static final class MapField
            extends CacheField
        {
            MapField( Field field )
            {
                super( field );
            }

            @Override
            void doInterpolate( Object target, InterpolateObjectAction ctx )
                throws IllegalAccessException
            {
                @SuppressWarnings( "unchecked" ) Map<Object, Object> m = (Map<Object, Object>) field.get( target );
                if ( m == null || m.isEmpty() )
                {
                    return;
                }

                for ( Map.Entry<Object, Object> entry : m.entrySet() )
                {
                    Object value = entry.getValue();

                    if ( value == null )
                    {
                        continue;
                    }

                    if ( String.class == value.getClass() )
                    {
                        String interpolated = ctx.interpolate( (String) value );

                        if ( !interpolated.equals( value ) )
                        {
                            try
                            {
                                entry.setValue( interpolated );
                            }
                            catch ( UnsupportedOperationException ignore )
                            {
                                // nop
                            }
                        }
                    }
                    else if ( value.getClass().isArray() )
                    {
                        evaluateArray( value, ctx );
                    }
                    else
                    {
                        ctx.interpolationTargets.add( value );
                    }
                }
            }
        }

        static final class ObjectField
            extends CacheField
        {
            private final boolean isArray;

            ObjectField( Field field )
            {
                super( field );
                this.isArray = field.getType().isArray();
            }

            @Override
            void doInterpolate( Object target, InterpolateObjectAction ctx )
                throws IllegalAccessException
            {
                Object value = field.get( target );
                if ( value != null )
                {
                    if ( isArray )
                    {
                        evaluateArray( value, ctx );
                    }
                    else
                    {
                        ctx.interpolationTargets.add( value );
                    }
                }
            }
        }

    }

}
