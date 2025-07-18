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
package org.apache.maven.configuration.internal;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.inject.TypeLiteral;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.ConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.ParameterizedConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.eclipse.sisu.bean.DeclaredMembers;
import org.eclipse.sisu.bean.DeclaredMembers.View;
import org.eclipse.sisu.plexus.TypeArguments;

/**
 * Optimized version of CompositeBeanHelper with caching for improved performance.
 * This implementation caches method and field lookups to avoid repeated reflection operations.
 */
public final class EnhancedCompositeBeanHelper {

    // Cache for method lookups: Class -> PropertyName -> MethodInfo
    private static final ConcurrentMap<Class<?>, Map<String, MethodInfo>> METHOD_CACHE = new ConcurrentHashMap<>();

    // Cache for field lookups: Class -> FieldName -> Field
    private static final ConcurrentMap<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    // Cache for accessible fields to avoid repeated setAccessible calls
    private static final ConcurrentMap<Field, Boolean> ACCESSIBLE_FIELD_CACHE = new ConcurrentHashMap<>();

    private final ConverterLookup lookup;
    private final ClassLoader loader;
    private final ExpressionEvaluator evaluator;
    private final ConfigurationListener listener;

    /**
     * Holds information about a method including its parameter type.
     */
    private record MethodInfo(Method method, Type parameterType) {}

    public EnhancedCompositeBeanHelper(
            ConverterLookup lookup, ClassLoader loader, ExpressionEvaluator evaluator, ConfigurationListener listener) {
        this.lookup = lookup;
        this.loader = loader;
        this.evaluator = evaluator;
        this.listener = listener;
    }

    /**
     * Calls the default "set" method on the bean; re-converts the configuration if necessary.
     */
    public void setDefault(Object bean, Object defaultValue, PlexusConfiguration configuration)
            throws ComponentConfigurationException {

        Class<?> beanType = bean.getClass();

        // Find the default "set" method
        MethodInfo setterInfo = findCachedMethod(beanType, "", null);
        if (setterInfo == null) {
            // Look for any method named "set" with one parameter
            Map<String, MethodInfo> classMethodCache = METHOD_CACHE.computeIfAbsent(beanType, this::buildMethodCache);
            setterInfo = classMethodCache.get("set");
        }

        if (setterInfo == null) {
            throw new ComponentConfigurationException(configuration, "Cannot find default setter in " + beanType);
        }

        Object value = defaultValue;
        TypeLiteral<?> paramType = TypeLiteral.get(setterInfo.parameterType);

        if (!paramType.getRawType().isInstance(value)) {
            if (configuration.getChildCount() > 0) {
                throw new ComponentConfigurationException(
                        "Basic element '" + configuration.getName() + "' must not contain child elements");
            }
            value = convertProperty(beanType, paramType.getRawType(), paramType.getType(), configuration);
        }

        if (value != null) {
            try {
                if (listener != null) {
                    listener.notifyFieldChangeUsingSetter("", value, bean);
                }
                setterInfo.method.invoke(bean, value);
            } catch (IllegalAccessException | InvocationTargetException | LinkageError e) {
                throw new ComponentConfigurationException(configuration, "Cannot set default", e);
            }
        }
    }

    /**
     * Sets a property in the bean using cached lookups for improved performance.
     */
    public void setProperty(Object bean, String propertyName, Class<?> valueType, PlexusConfiguration configuration)
            throws ComponentConfigurationException {

        Class<?> beanType = bean.getClass();

        // Try setter/adder methods first
        MethodInfo methodInfo = findCachedMethod(beanType, propertyName, valueType);
        if (methodInfo != null) {
            try {
                Object value = convertPropertyForMethod(beanType, methodInfo, valueType, configuration);
                if (value != null) {
                    if (listener != null) {
                        listener.notifyFieldChangeUsingSetter(propertyName, value, bean);
                    }
                    methodInfo.method.invoke(bean, value);
                    return;
                }
            } catch (IllegalAccessException | InvocationTargetException | LinkageError e) {
                // Fall through to field access
            }
        }

        // Try field access
        Field field = findCachedField(beanType, propertyName);
        if (field != null) {
            try {
                Object value = convertPropertyForField(beanType, field, valueType, configuration);
                if (value != null) {
                    if (listener != null) {
                        listener.notifyFieldChangeUsingReflection(propertyName, value, bean);
                    }
                    setFieldValue(bean, field, value);
                    return;
                }
            } catch (IllegalAccessException | LinkageError e) {
                // Continue to error handling
            }
        }

        // If we get here, we couldn't set the property
        if (methodInfo == null && field == null) {
            throw new ComponentConfigurationException(
                    configuration, "Cannot find '" + propertyName + "' in " + beanType);
        }
    }

    /**
     * Find method using cache for improved performance.
     */
    private MethodInfo findCachedMethod(Class<?> beanType, String propertyName, Class<?> valueType) {
        Map<String, MethodInfo> classMethodCache = METHOD_CACHE.computeIfAbsent(beanType, this::buildMethodCache);

        String title = Character.toTitleCase(propertyName.charAt(0)) + propertyName.substring(1);

        // Try setter first
        MethodInfo setter = classMethodCache.get("set" + title);
        if (setter != null && isMethodCompatible(setter.method, valueType)) {
            return setter;
        }

        // Try adder
        MethodInfo adder = classMethodCache.get("add" + title);
        if (adder != null && isMethodCompatible(adder.method, valueType)) {
            return adder;
        }

        // Return first found for backward compatibility
        return setter != null ? setter : adder;
    }

    /**
     * Build method cache for a class.
     */
    private Map<String, MethodInfo> buildMethodCache(Class<?> beanType) {
        Map<String, MethodInfo> methodMap = new HashMap<>();

        for (Method method : beanType.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 1) {
                Type[] paramTypes = method.getGenericParameterTypes();
                methodMap.putIfAbsent(method.getName(), new MethodInfo(method, paramTypes[0]));
            }
        }

        return methodMap;
    }

    /**
     * Check if method is compatible with value type.
     */
    private boolean isMethodCompatible(Method method, Class<?> valueType) {
        if (valueType == null) {
            return true;
        }
        return method.getParameterTypes()[0].isAssignableFrom(valueType);
    }

    /**
     * Find field using cache for improved performance.
     */
    private Field findCachedField(Class<?> beanType, String fieldName) {
        Map<String, Field> classFieldCache = FIELD_CACHE.computeIfAbsent(beanType, this::buildFieldCache);
        return classFieldCache.get(fieldName);
    }

    /**
     * Build field cache for a class.
     */
    private Map<String, Field> buildFieldCache(Class<?> beanType) {
        Map<String, Field> fieldMap = new HashMap<>();

        for (Object member : new DeclaredMembers(beanType, View.FIELDS)) {
            Field field = (Field) member;
            if (!Modifier.isStatic(field.getModifiers())) {
                fieldMap.put(field.getName(), field);
            }
        }

        return fieldMap;
    }

    /**
     * Convert property value for method parameter.
     */
    private Object convertPropertyForMethod(
            Class<?> beanType, MethodInfo methodInfo, Class<?> valueType, PlexusConfiguration configuration)
            throws ComponentConfigurationException {

        TypeLiteral<?> paramType = TypeLiteral.get(methodInfo.parameterType);
        return convertProperty(beanType, valueType, configuration, paramType);
    }

    /**
     * Convert property value for field.
     */
    private Object convertPropertyForField(
            Class<?> beanType, Field field, Class<?> valueType, PlexusConfiguration configuration)
            throws ComponentConfigurationException {

        TypeLiteral<?> fieldType = TypeLiteral.get(field.getGenericType());
        return convertProperty(beanType, valueType, configuration, fieldType);
    }

    private Object convertProperty(
            Class<?> beanType, Class<?> valueType, PlexusConfiguration configuration, TypeLiteral<?> paramType)
            throws ComponentConfigurationException {
        Class<?> rawPropertyType = paramType.getRawType();

        if (valueType != null && rawPropertyType.isAssignableFrom(valueType)) {
            rawPropertyType = valueType; // pick more specific type
        }

        return convertProperty(beanType, rawPropertyType, paramType.getType(), configuration);
    }

    /**
     * Convert property using appropriate converter.
     */
    private Object convertProperty(
            Class<?> beanType, Class<?> rawPropertyType, Type genericPropertyType, PlexusConfiguration configuration)
            throws ComponentConfigurationException {

        ConfigurationConverter converter = lookup.lookupConverterForType(rawPropertyType);

        if (!(genericPropertyType instanceof Class) && converter instanceof ParameterizedConfigurationConverter) {
            Type[] propertyTypeArgs = TypeArguments.get(genericPropertyType);
            return ((ParameterizedConfigurationConverter) converter)
                    .fromConfiguration(
                            lookup,
                            configuration,
                            rawPropertyType,
                            propertyTypeArgs,
                            beanType,
                            loader,
                            evaluator,
                            listener);
        }

        return converter.fromConfiguration(
                lookup, configuration, rawPropertyType, beanType, loader, evaluator, listener);
    }

    /**
     * Set field value with cached accessibility.
     */
    private void setFieldValue(Object bean, Field field, Object value) throws IllegalAccessException {
        Boolean isAccessible = ACCESSIBLE_FIELD_CACHE.get(field);
        if (isAccessible == null) {
            isAccessible = field.canAccess(bean);
            if (!isAccessible) {
                field.setAccessible(true);
                isAccessible = true;
            }
            ACCESSIBLE_FIELD_CACHE.put(field, isAccessible);
        } else if (!isAccessible) {
            field.setAccessible(true);
            ACCESSIBLE_FIELD_CACHE.put(field, true);
        }

        field.set(bean, value);
    }

    /**
     * Clear all caches. Useful for testing or memory management.
     */
    public static void clearCaches() {
        METHOD_CACHE.clear();
        FIELD_CACHE.clear();
        ACCESSIBLE_FIELD_CACHE.clear();
    }
}
