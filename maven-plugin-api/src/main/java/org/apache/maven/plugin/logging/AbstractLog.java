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

import java.util.function.Supplier;

/**
 * Abstract class providing some defaults shareable implementation of some Log methods.
 */
public abstract class AbstractLog implements Log 
{
  /**
   * @see org.apache.maven.plugin.logging.Log#debug(Supplier) 
   */
  public void debug( Supplier<String> messageSupplier )
  {
    if ( isDebugEnabled() )
    {
      debug( messageSupplier.get() );
    }
  }

  /**
   * @see org.apache.maven.plugin.logging.Log#debug(Supplier, Throwable)  
   */
  public void debug( Supplier<String> messageSupplier, Throwable error )
  {
    if ( isDebugEnabled() )
    {
      debug( messageSupplier.get(), error );
    }
  }

  /**
   * @see org.apache.maven.plugin.logging.Log#info(Supplier)
   */
  public void info( Supplier<String> messageSupplier )
  {
    if ( isInfoEnabled() )
    {
      info( messageSupplier.get() );
    }
  }

  /**
   * @see org.apache.maven.plugin.logging.Log#info(Supplier, Throwable)
   */
  public void info( Supplier<String> messageSupplier, Throwable error )
  {
    if ( isInfoEnabled() )
    {
      info( messageSupplier.get(), error );
    }
  }

  /**
   * @see org.apache.maven.plugin.logging.Log#warn(Supplier)
   */
  public void warn( Supplier<String> messageSupplier )
  {
    if ( isWarnEnabled() )
    {
      warn( messageSupplier.get() );
    }
  }

  /**
   * @see org.apache.maven.plugin.logging.Log#warn(Supplier, Throwable)
   */
  public void warn( Supplier<String> messageSupplier, Throwable error )
  {
    if ( isWarnEnabled() )
    {
      warn( messageSupplier.get(), error );
    }
  }

  /**
   * @see org.apache.maven.plugin.logging.Log#error(Supplier)
   */
  public void error( Supplier<String> messageSupplier )
  {
    if ( isErrorEnabled() )
    {
      error( messageSupplier.get() );
    }
  }

  /**
   * @see org.apache.maven.plugin.logging.Log#error(Supplier, Throwable)
   */
  public void error( Supplier<String> messageSupplier, Throwable error )
  {
    if ( isErrorEnabled() )
    {
      error( messageSupplier.get(), error );
    }
  }
}
