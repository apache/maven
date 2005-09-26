import org.apache.maven.embedder.*;

public class Plugin
{
    public Plugin()
        throws Exception
    {
        MavenEmbedder maven = new MavenEmbedder();
        
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        maven.setClassLoader( classLoader );
        
        maven.start();
        
        System.out.println( "Happy happy joy joy!" );
    }
    
    public static void main( String[] args )
        throws Exception
    {
        Plugin plugin = new Plugin();
    }
}
