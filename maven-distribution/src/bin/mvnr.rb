# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

require 'java'

class Maven
  def initialize goals
    @goals = goals
  end

  include_class 'java.io.File'
  include_class 'org.apache.maven.embedder.MavenEmbedder'
  include_class 'org.apache.maven.embedder.DefaultConfiguration'
  include_class 'org.apache.maven.execution.DefaultMavenExecutionRequest'

  def run        
    configuration = DefaultConfiguration.new    
    maven = MavenEmbedder.new(configuration)    
    r = DefaultMavenExecutionRequest.new
    r.setBaseDirectory( File.new( "." ) ) 
    r.setGoals( @goals )                                                                                                                          
    result = maven.execute( r );   
  end
end

m = Maven.new( ["clean"] ).run
