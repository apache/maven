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
package org.apache.maven.plugin.internal;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.TypeAwareExpressionEvaluator;

/** Configures a Maven 4 mojo directly from the public Maven XML model. */
public final class V4MojoConfigurator {
    private static final Object NOT_SCALAR = new Object();

    private final ExpressionEvaluator evaluator;
    private final ClassLoader classLoader;
    private final PluginDescriptor pluginDescriptor;

    public V4MojoConfigurator(
            ExpressionEvaluator evaluator, ClassLoader classLoader, PluginDescriptor pluginDescriptor) {
        this.evaluator = evaluator;
        this.classLoader = classLoader;
        this.pluginDescriptor = pluginDescriptor;
    }

    public void configure(Object mojo, XmlNode configuration) throws PluginConfigurationException {
        if (configuration == null) {
            return;
        }
        for (XmlNode child : configuration.children()) {
            try {
                set(mojo, child.name(), child);
            } catch (Exception e) {
                throw new PluginConfigurationException(
                        pluginDescriptor,
                        "Unable to configure '" + child.name() + "' on "
                                + mojo.getClass().getName(),
                        e);
            }
        }
    }

    private void set(Object bean, String name, XmlNode node) throws Exception {
        String title = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        for (Method method : bean.getClass().getMethods()) {
            if ((method.getName().equals("set" + title) || method.getName().equals("add" + title))
                    && method.getParameterCount() == 1) {
                if (!method.canAccess(bean)) {
                    method.setAccessible(true);
                }
                method.invoke(bean, convert(node, method.getGenericParameterTypes()[0]));
                return;
            }
        }
        for (Class<?> type = bean.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(bean, convert(node, field.getGenericType()));
                return;
            } catch (NoSuchFieldException ignored) {
                // Continue through the class hierarchy.
            }
        }
        throw new NoSuchFieldException(name);
    }

    private Object convert(XmlNode node, Type target) throws Exception {
        Class<?> type = rawType(target);
        if (type == XmlNode.class) {
            return node;
        }
        Object scalar = convertScalar(node, type);
        if (scalar != NOT_SCALAR) {
            return scalar;
        }
        if (type.isArray()) {
            List<XmlNode> children = node.children();
            Object array = Array.newInstance(type.getComponentType(), children.size());
            for (int i = 0; i < children.size(); i++) {
                Array.set(array, i, convert(children.get(i), type.getComponentType()));
            }
            return array;
        }
        if (Collection.class.isAssignableFrom(type)) {
            Type itemType = target instanceof ParameterizedType p ? p.getActualTypeArguments()[0] : Object.class;
            Collection<Object> values =
                    Set.class.isAssignableFrom(type) ? new java.util.LinkedHashSet<>() : new ArrayList<>();
            for (XmlNode child : node.children()) {
                values.add(convert(child, itemType));
            }
            return values;
        }
        if (Map.class.isAssignableFrom(type)) {
            Type valueType = target instanceof ParameterizedType p ? p.getActualTypeArguments()[1] : Object.class;
            if (Properties.class.isAssignableFrom(type)) {
                Properties properties = new Properties();
                for (XmlNode child : node.children()) {
                    properties.put(child.name(), convert(child, valueType));
                }
                return properties;
            }
            Map<String, Object> values = new LinkedHashMap<>();
            for (XmlNode child : node.children()) {
                values.put(child.name(), convert(child, valueType));
            }
            return values;
        }
        type = resolveImplementation(node, type);
        String value = evaluateNodeAsString(node);
        if (value != null && node.children().isEmpty()) {
            Method setter = findDefaultSetter(type);
            if (setter != null) {
                Object instance = instantiate(type);
                if (!setter.canAccess(instance)) {
                    setter.setAccessible(true);
                }
                setter.invoke(
                        instance, convert(XmlNode.newInstance("value", value), setter.getGenericParameterTypes()[0]));
                return instance;
            }
        }
        if (type.isRecord()) {
            return instantiateRecord(node, type);
        }
        Object instance = instantiate(type);
        for (XmlNode child : node.children()) {
            set(instance, child.name(), child);
        }
        return instance;
    }

    private Object convertScalar(XmlNode node, Class<?> type) throws Exception {
        if (type == String.class || type == Object.class) {
            return evaluateNode(node, type);
        }
        if (type == Path.class || type == File.class) {
            Object evaluated = evaluateNode(node, type);
            if (evaluated instanceof Path p) {
                return type == File.class ? p.toFile() : p;
            }
            if (evaluated instanceof File f) {
                return type == File.class ? f : f.toPath();
            }
            String value = evaluated != null ? String.valueOf(evaluated) : null;
            if (value == null) {
                return null;
            }
            Path path = Paths.get(value.replace('/', File.separatorChar));
            if (evaluator instanceof TypeAwareExpressionEvaluator typed) {
                path = typed.alignToBaseDirectory(path.toFile()).toPath();
            }
            return type == File.class ? path.toFile() : path;
        }
        if (type == URI.class) {
            Object evaluated = evaluateNode(node, URI.class);
            if (evaluated instanceof URI uri) {
                return uri;
            }
            String s = evaluated != null ? String.valueOf(evaluated) : null;
            return s == null ? null : URI.create(s);
        }
        if (type == boolean.class || type == Boolean.class) {
            String s = evaluateNodeAsString(node);
            if (s == null) {
                return type == boolean.class ? Boolean.FALSE : null;
            }
            return Boolean.valueOf(s);
        }
        if (type == int.class || type == Integer.class) {
            String s = evaluateNodeAsString(node);
            if (s == null) {
                return type == int.class ? Integer.valueOf(0) : null;
            }
            return Integer.valueOf(s);
        }
        if (type == long.class || type == Long.class) {
            String s = evaluateNodeAsString(node);
            if (s == null) {
                return type == long.class ? Long.valueOf(0L) : null;
            }
            return Long.valueOf(s);
        }
        if (type == double.class || type == Double.class) {
            String s = evaluateNodeAsString(node);
            if (s == null) {
                return type == double.class ? Double.valueOf(0.0d) : null;
            }
            return Double.valueOf(s);
        }
        if (type == float.class || type == Float.class) {
            String s = evaluateNodeAsString(node);
            if (s == null) {
                return type == float.class ? Float.valueOf(0.0f) : null;
            }
            return Float.valueOf(s);
        }
        if (type == short.class || type == Short.class) {
            String s = evaluateNodeAsString(node);
            if (s == null) {
                return type == short.class ? Short.valueOf((short) 0) : null;
            }
            return Short.valueOf(s);
        }
        if (type == byte.class || type == Byte.class) {
            String s = evaluateNodeAsString(node);
            if (s == null) {
                return type == byte.class ? Byte.valueOf((byte) 0) : null;
            }
            return Byte.valueOf(s);
        }
        if (type == char.class || type == Character.class) {
            String s = evaluateNodeAsString(node);
            if (s == null || s.isEmpty()) {
                return type == char.class ? Character.valueOf('\0') : null;
            }
            return s.charAt(0);
        }
        if (type.isEnum()) {
            String s = evaluateNodeAsString(node);
            if (s == null) {
                return null;
            }
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object val = Enum.valueOf((Class) type, s);
            return val;
        }
        return NOT_SCALAR;
    }

    private Class<?> resolveImplementation(XmlNode node, Class<?> type) throws Exception {
        String implementation = node.attribute("implementation");
        if (implementation != null && !implementation.isEmpty()) {
            try {
                return Class.forName(implementation, true, classLoader);
            } catch (ClassNotFoundException e) {
                if (!type.isSealed()) {
                    throw e;
                }
                String hint = implementation;
                List<Class<?>> matches = java.util.Arrays.stream(type.getPermittedSubclasses())
                        .filter(candidate ->
                                hint.equals(candidate.getSimpleName()) || hint.equals(candidate.getCanonicalName()))
                        .toList();
                if (matches.size() != 1) {
                    throw e;
                }
                return matches.get(0);
            }
        }
        return type;
    }

    private Object instantiateRecord(XmlNode node, Class<?> type) throws Exception {
        RecordComponent[] components = type.getRecordComponents();
        Class<?>[] parameterTypes = new Class<?>[components.length];
        Object[] arguments = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            parameterTypes[i] = component.getType();
            XmlNode child = node.child(component.getName());
            arguments[i] =
                    child == null ? defaultValueFor(component.getType()) : convert(child, component.getGenericType());
        }
        var constructor = type.getDeclaredConstructor(parameterTypes);
        if (!constructor.canAccess(null)) {
            constructor.setAccessible(true);
        }
        return constructor.newInstance(arguments);
    }

    private static Object instantiate(Class<?> type) throws ReflectiveOperationException {
        var ctor = type.getDeclaredConstructor();
        if (!ctor.canAccess(null)) {
            ctor.setAccessible(true);
        }
        return ctor.newInstance();
    }

    private static Object defaultValueFor(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == double.class) {
            return 0.0d;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }

    private Object evaluateNode(XmlNode node, Class<?> type) throws Exception {
        String val = node.value();
        Object evaluated = (val != null && !val.isEmpty()) ? evaluate(val, type) : null;
        if (evaluated == null) {
            String defaultValue = node.attribute("default-value");
            if (defaultValue != null && !defaultValue.isEmpty()) {
                evaluated = evaluate(defaultValue, type);
            }
        }
        return evaluated;
    }

    private String evaluateNodeAsString(XmlNode node) throws Exception {
        Object evaluated = evaluateNode(node, String.class);
        return evaluated != null ? String.valueOf(evaluated) : null;
    }

    private Object evaluate(String value, Class<?> type) throws Exception {
        if (value == null) {
            return null;
        }
        return evaluator instanceof TypeAwareExpressionEvaluator typed
                ? typed.evaluate(value, type)
                : evaluator.evaluate(value);
    }

    private static Method findDefaultSetter(Class<?> type) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals("set") && method.getParameterCount() == 1) {
                return method;
            }
        }
        return null;
    }

    private static Class<?> rawType(Type type) {
        return type instanceof Class<?> c
                ? c
                : type instanceof ParameterizedType p ? (Class<?>) p.getRawType() : Object.class;
    }
}
