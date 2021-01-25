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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.PrintStream;
import java.util.function.Supplier;

/**
 * Tests for {@link SystemStreamLog} 
 */
public class SystemStreamLogTest 
{
  private static final String EXPECTED_MESSAGE = "expected message";
  private static PrintStream outStream;
  private static PrintStream errStream;
  private static PrintStream mockOut;
  private static PrintStream mockErr;

  /*
   * As SystemStreamLog info/warn/error log levels are active, this test checks that
   * a message supplier is really called/executed when logging at those levels 
   */
  @Test
  public void testLazyMessageIsEvaluatedForActiveLogLevels()
  {
    Supplier messageSupplier = Mockito.mock(Supplier.class);
    Mockito.when(messageSupplier.get()).thenReturn(EXPECTED_MESSAGE);

    Log logger = new SystemStreamLog();
    logger.info(messageSupplier);
    logger.warn(messageSupplier);
    logger.error(messageSupplier);
    
    Mockito.verify(messageSupplier, Mockito.times(3)).get();
  }

  /*
   * As SystemStreamLog debug log level is inactive, this test checks that
   * a message supplier is not called/executed when logging at debug level
   */
  @Test
  public void testDebugLazyMessageIsNotEvaluated()
  {
    Supplier messageSupplier = Mockito.mock(Supplier.class);
    Mockito.when(messageSupplier.get()).thenReturn(EXPECTED_MESSAGE);

    Log logger = new SystemStreamLog();
    logger.debug(messageSupplier);

    Mockito.verify(messageSupplier, Mockito.never()).get();
  }

  @BeforeAll
  public static void initialize()
  {
    outStream = System.out;
    errStream = System.err;

    mockOut = Mockito.mock(PrintStream.class);
    System.setOut(mockOut);
    mockErr = Mockito.mock(PrintStream.class);
    System.setErr(mockErr);
  }
  
  @AfterAll
  public static void cleanup()
  {
    System.setOut(outStream);
    System.setErr(errStream);
  }
}
