package org.apache.maven.plugin.logging;

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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * A custom Supplier implementation allowing to count how many times a constant value is delivered.
 * @param <T> the type of the value to deliver
 */
public class CountingCallsSupplier<T> implements Supplier<T> {
  private final T value;
  private final AtomicInteger counter;

  private CountingCallsSupplier(T valueToSupply)
  {
    this.value = valueToSupply;
    this.counter = new AtomicInteger(0);
  }

  @Override
  public T get()
  {
    counter.incrementAndGet();
    return value;
  }
  
  public int getNumberOfCalls()
  {
    return counter.get();
  }
  
  public static <T> CountingCallsSupplier<T> of( T value )
  {
    return new CountingCallsSupplier<>( value );
  } 
}
