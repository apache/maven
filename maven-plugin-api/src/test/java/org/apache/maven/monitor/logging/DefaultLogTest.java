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

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.function.Supplier;

public class DefaultLogTest
{
  private static final String EXPECTED_MESSAGE = "expected message";

  @Test
  public void testLazyMessageIsEvaluatedForActiveLogLevels() 
  {
    Supplier messageSupplier = Mockito.mock(Supplier.class);
    Mockito.when(messageSupplier.get()).thenReturn(EXPECTED_MESSAGE);

    Logger mockPlexusLogger = Mockito.mock(Logger.class);
    Mockito.when(mockPlexusLogger.isDebugEnabled()).thenReturn(Boolean.TRUE);
    Mockito.when(mockPlexusLogger.isInfoEnabled()).thenReturn(Boolean.TRUE);
    Mockito.when(mockPlexusLogger.isWarnEnabled()).thenReturn(Boolean.TRUE);
    Mockito.when(mockPlexusLogger.isErrorEnabled()).thenReturn(Boolean.TRUE);

    Log logger = new DefaultLog(mockPlexusLogger);
    logger.debug(messageSupplier);
    logger.info(messageSupplier);
    logger.warn(messageSupplier);
    logger.error(messageSupplier);

    Mockito.verify(messageSupplier, Mockito.times(4)).get();
  }
  
  @Test
  public void testLazyMessageIsNotEvaluatedForNonActiveLogLevels()
  {
    Supplier messageSupplier = Mockito.mock(Supplier.class);
    Mockito.when(messageSupplier.get()).thenReturn(EXPECTED_MESSAGE);

    Logger mockPlexusLogger = Mockito.mock(Logger.class);
    Mockito.when(mockPlexusLogger.isDebugEnabled()).thenReturn(Boolean.FALSE);
    Mockito.when(mockPlexusLogger.isInfoEnabled()).thenReturn(Boolean.FALSE);
    Mockito.when(mockPlexusLogger.isWarnEnabled()).thenReturn(Boolean.FALSE);
    Mockito.when(mockPlexusLogger.isErrorEnabled()).thenReturn(Boolean.FALSE);

    Log logger = new DefaultLog(mockPlexusLogger);
    logger.debug(messageSupplier);
    logger.info(messageSupplier);
    logger.warn(messageSupplier);
    logger.error(messageSupplier);

    Mockito.verify(messageSupplier, Mockito.never()).get();
  }
}
