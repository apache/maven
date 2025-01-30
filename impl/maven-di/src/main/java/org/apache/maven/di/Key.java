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
package org.apache.maven.di;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.di.impl.ReflectionUtils;
import org.apache.maven.di.impl.Types;

/**
 * A binding key that uniquely identifies a dependency in the injection system.
 * <p>
 * Keys combine a type with an optional qualifier to uniquely identify dependencies
 * within the injection system. They also serve as type tokens, allowing preservation
 * of generic type information at runtime.
 * <p>
 * Example usage:
 * <pre>
 * // Simple key for a type
 * Key&lt;Service&gt; simple = Key.of(Service.class);
 *
 * // Key with generic type information
 * Key&lt;List&lt;String&gt;&gt; generic = new Key&lt;List&lt;String&gt;&gt;(){};
 *
 * // Key with qualifier
 * Key&lt;Service&gt; qualified = Key.of(Service.class, "primary");
 * </pre>
 *
 * @param <T> The type this key represents
 * @since 4.0.0
 */
public abstract class Key<T> {
    private final Type type;
    private final @Nullable Object qualifier;

    private int hash;

    protected Key() {
        this(null);
    }

    protected Key(@Nullable Object qualifier) {
        this.type = Types.simplifyType(getTypeParameter());
        this.qualifier = qualifier;
    }

    protected Key(Type type, @Nullable Object qualifier) {
        this.type = Types.simplifyType(type);
        this.qualifier = qualifier;
    }

    static final class KeyImpl<T> extends Key<T> {
        KeyImpl(Type type, Object qualifier) {
            super(type, qualifier);
        }
    }

    /**
     * Creates a new Key instance for the specified type.
     *
     * @param <T> the type parameter
     * @param type the Class object representing the type
     * @return a new Key instance
     * @throws NullPointerException if type is null
     */
    public static <T> Key<T> of(Class<T> type) {
        return new KeyImpl<>(type, null);
    }

    /**
     * Creates a new Key instance for the specified type with a qualifier.
     *
     * @param <T> the type parameter
     * @param type the Class object representing the type
     * @param qualifier the qualifier object (typically an annotation instance)
     * @return a new Key instance
     * @throws NullPointerException if type is null
     */
    public static <T> Key<T> of(Class<T> type, @Nullable Object qualifier) {
        return new KeyImpl<>(type, qualifier);
    }

    public static <T> Key<T> ofType(Type type) {
        return new KeyImpl<>(type, null);
    }

    public static <T> Key<T> ofType(Type type, @Nullable Object qualifier) {
        return new KeyImpl<>(type, qualifier);
    }

    private Type getTypeParameter() {
        // this cannot possibly fail so not even a check here
        Type typeArgument = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        Object outerInstance = ReflectionUtils.getOuterClassInstance(this);
        //		// the outer instance is null in static context
        return outerInstance != null
                ? Types.bind(typeArgument, Types.getAllTypeBindings(outerInstance.getClass()))
                : typeArgument;
    }

    /**
     * Returns the actual type represented by this key.
     * <p>
     * This includes full generic type information if available.
     *
     * @return the type represented by this key
     */
    public Type getType() {
        return type;
    }

    /**
     * A shortcut for <code>{@link Types#getRawType(Type)}(key.getType())</code>.
     * Also casts the result to a properly parameterized class.
     */
    @SuppressWarnings("unchecked")
    public Class<T> getRawType() {
        return (Class<T>) Types.getRawType(type);
    }

    /**
     * Returns a type parameter of the underlying type wrapped as a key with no qualifier.
     *
     * @throws IllegalStateException when underlying type is not a parameterized one.
     */
    public <U> Key<U> getTypeParameter(int index) {
        if (type instanceof ParameterizedType parameterizedType) {
            return new KeyImpl<>(parameterizedType.getActualTypeArguments()[index], null);
        }
        throw new IllegalStateException("Expected type from key " + getDisplayString() + " to be parameterized");
    }

    /**
     * Returns the qualifier associated with this key, if any.
     *
     * @return the qualifier object or null if none exists
     */
    public @Nullable Object getQualifier() {
        return qualifier;
    }

    /**
     * Returns an underlying type with display string formatting (package names stripped)
     * and prepended qualifier display string if this key has a qualifier.
     */
    public String getDisplayString() {
        StringBuilder result = new StringBuilder();
        if (qualifier instanceof String s) {
            if (s.isEmpty()) {
                result.append("@Named ");
            } else {
                result.append("@Named(\"").append(s).append("\") ");
            }
        } else if (qualifier != null) {
            ReflectionUtils.getDisplayString(result, qualifier);
            result.append(" ");
        }
        result.append(ReflectionUtils.getDisplayName(type));
        return result.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Key<?> that) {
            return type.equals(that.type) && Objects.equals(qualifier, that.qualifier);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hashCode = hash;
        if (hashCode == 0) {
            hash = 31 * type.hashCode() + (qualifier == null ? 0 : qualifier.hashCode());
        }
        return hash;
    }

    @Override
    public String toString() {
        return getDisplayString();
    }
}
