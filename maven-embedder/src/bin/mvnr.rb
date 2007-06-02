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
