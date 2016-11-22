
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

dir = new File( basedir, 'target/generated-sources/slf4j-simple/org/slf4j/impl' );

file = new File( dir, 'StaticLoggerBinder.java' );
content = file.text;

// check if already patched
if ( content.contains( 'MavenSimpleLoggerFactory' ) )
{
  println '    slf4j-simple already patched';
  return;
}


println '    patching StaticLoggerBinder.java';
content = content.replaceAll( 'SimpleLoggerFactory', 'MavenSimpleLoggerFactory' );
file.write( content );


println '    patching SimpleLogger.java';
file = new File( dir, 'SimpleLogger.java' );
content = file.text;
content = content.replaceAll( 'private static final int LOG_LEVEL_', 'protected static final int LOG_LEVEL_' );
content = content.replaceAll( 't.printStackTrace(TARGET_STREAM)', 'renderThrowable(t, TARGET_STREAM);' );

index = content.indexOf( 'switch (level) {' );
end = content.indexOf( '}', index ) + 1;
content = content.substring( 0, index ) + 'buf.append(renderLevel(level));' + content.substring( end );

content = content.substring( 0, content.lastIndexOf( '}' ) );
content += '  protected void renderThrowable(Throwable t, PrintStream stream) {}\n';
content += '  protected String renderLevel(int level) { return ""; }\n}\n';

file.write( content );
