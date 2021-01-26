package org.apache.maven.monitor.logging;

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

import org.apache.maven.plugin.logging.CountingCallsSupplier;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultLogTest
{
  private static final String EXPECTED_MESSAGE = "expected message";

  @Test
  public void testLazyMessageIsEvaluatedForActiveLogLevels() 
  {
    CountingCallsSupplier<String> messageSupplier = CountingCallsSupplier.of(EXPECTED_MESSAGE);
    
    Throwable fakeError = new RuntimeException();

    Log logger = new DefaultLog(new NoOpLogger(Logger.LEVEL_DEBUG));
    
    logger.debug(messageSupplier);
    logger.info(messageSupplier);
    logger.warn(messageSupplier);
    logger.error(messageSupplier);

    logger.debug(messageSupplier, fakeError);
    logger.info(messageSupplier, fakeError);
    logger.warn(messageSupplier, fakeError);
    logger.error(messageSupplier, fakeError);

    // all log calls should have lead to a supplier call
    assertEquals(8, messageSupplier.getNumberOfCalls(), "wrong number of calls to the message supplier");
  }
  
  @Test
  public void testLazyMessageIsNotEvaluatedForNonActiveLogLevels()
  {
    CountingCallsSupplier<String> messageSupplier = CountingCallsSupplier.of(EXPECTED_MESSAGE);

    Throwable fakeError = new RuntimeException();

    Log logger = new DefaultLog(new NoOpLogger(Logger.LEVEL_DISABLED));
    
    logger.debug(messageSupplier);
    logger.info(messageSupplier);
    logger.warn(messageSupplier);
    logger.error(messageSupplier);

    logger.debug(messageSupplier, fakeError);
    logger.info(messageSupplier, fakeError);
    logger.warn(messageSupplier, fakeError);
    logger.error(messageSupplier, fakeError);

    // no log calls should have lead to any supplier call
    assertEquals(0, messageSupplier.getNumberOfCalls(), "wrong number of calls to the message supplier");
  }
}
