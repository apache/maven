package org.apache.maven.repository;

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

public abstract class Event 
{
   int code;

   public final int getCode()
   {
      return code;
   }

   public void toStringHelper( StringBuilder sb ) 
   {
      sb.append( "" );
   }

   public class Initiated extends Event 
   {
      final int code = 0;

      public void toStringHelper( StringBuilder sb ) 
      {
         sb.append( "INITIATED" );
      }
   }

   public class Started extends Event 
   {

      final int code = 1;

      public void toStringHelper( StringBuilder sb ) 
      {
         sb.append( "STARTED" );
      }
   }

   public class Completed extends Event 
   {
      final int code = 2;

      public void toStringHelper( StringBuilder sb ) 
      {
         sb.append( "COMPLETED" );
      }
   }

   public class Error extends Event 
   {
      final int code = 3;

      public void toStringHelper( StringBuilder sb ) 
      {
         sb.append( "PROGRESS" );
      }
   }

   public class Progress extends Event 
   {
      final int code = 4;

      public void toStringHelper( StringBuilder sb ) 
      {
         sb.append( "ERROR" );
      }
   }

   public class Get extends Event 
   {
      final int code = 5;

      public void toStringHelper( StringBuilder sb ) 
      {
         sb.append( "GET" );
      }
   }

   public class Put extends Event 
   {
      final int code = 6;

      public void toStringHelper( StringBuilder sb ) 
      {
         sb.append( "PUT" );
      }
   }

}