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
package ${package};

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

class WrapperList<T, U> extends AbstractList<T> {
    public interface SerializableConsumer<T> extends java.util.function.Consumer<T>, java.io.Serializable {}
    public interface SerializableFunction<U, T> extends java.util.function.Function<U, T>, java.io.Serializable {}
    public interface SerializableSupplier<T> extends java.util.function.Supplier<T>, java.io.Serializable {}


    private final SerializableSupplier<List<U>> getter;
    private final SerializableConsumer<List<U>> setter;
    private final SerializableFunction<U, T> mapper;
    private final SerializableFunction<T, U> revMapper;

    WrapperList(List<U> list, SerializableFunction<U, T> mapper, SerializableFunction<T, U> revMapper) {
        this(() -> list, null, mapper, revMapper);
    }

    WrapperList(SerializableSupplier<List<U>> getter, SerializableConsumer<List<U>> setter, SerializableFunction<U, T> mapper, SerializableFunction<T, U> revMapper) {
        this.getter = getter;
        this.setter = setter;
        this.mapper = mapper;
        this.revMapper = revMapper;
    }

    @Override
    public T get(int index) {
        return mapper.apply(getter.get().get(index));
    }

    @Override
    public int size() {
        return getter.get().size();
    }

    @Override
    public boolean add(T t) {
        Objects.requireNonNull(t);
        if (setter != null) {
            List<U> list = new ArrayList<>(getter.get());
            boolean ret = list.add(revMapper.apply(t));
            setter.accept(list);
            return ret;
        } else {
            return getter.get().add(revMapper.apply(t));
        }
    }

    @Override
    public T set(int index, T element) {
        Objects.requireNonNull(element);
        if (setter != null) {
            List<U> list = new ArrayList<>(getter.get());
            U ret = list.set(index, revMapper.apply(element));
            setter.accept(list);
            return mapper.apply(ret);
        } else {
            return mapper.apply(getter.get().set(index, revMapper.apply(element)));
        }
    }

    @Override
    public void add(int index, T element) {
        Objects.requireNonNull(element);
        if (setter != null) {
            List<U> list = new ArrayList<>(getter.get());
            list.add(index, revMapper.apply(element));
            setter.accept(list);
        } else {
            getter.get().add(index, revMapper.apply(element));
        }
    }

    @Override
    public T remove(int index) {
        if (setter != null) {
            List<U> list = new ArrayList<>(getter.get());
            U ret = list.remove(index);
            setter.accept(list);
            return mapper.apply(ret);
        } else {
            return mapper.apply(getter.get().remove(index));
        }
    }
}
