/*
 * Example demonstrating the new cache configuration functionality.
 * 
 * This example shows how to configure cache behavior using CSS-like selectors
 * in Maven session user properties.
 */

import java.util.HashMap;
import java.util.Map;

public class CacheConfigurationExample {
    
    public static void main(String[] args) {
        System.out.println("Maven Cache Configuration Enhancement Example");
        System.out.println("============================================");
        
        // Example 1: Basic configuration
        System.out.println("\n1. Basic Configuration:");
        System.out.println("   User property: maven.cache.config");
        System.out.println("   Value: 'ModelBuilderRequest { scope: session, ref: hard }'");
        System.out.println("   Effect: All ModelBuilderRequest instances use session-scoped cache with hard references");
        
        // Example 2: Parent-child configuration
        System.out.println("\n2. Parent-Child Configuration:");
        System.out.println("   Value: 'ModelBuildRequest ModelBuilderRequest { ref: soft }'");
        System.out.println("   Effect: ModelBuilderRequest with ModelBuildRequest parent uses soft references");

        // Example 3: Wildcard configuration
        System.out.println("\n3. Wildcard Configuration:");
        System.out.println("   Value: '* VersionRangeRequest { scope: persistent, ref: weak }'");
        System.out.println("   Effect: VersionRangeRequest with any parent uses persistent cache with weak references");

        // Example 4: Partial configuration with merging
        System.out.println("\n4. Partial Configuration with Merging:");
        String partialConfig = """
            ModelBuilderRequest { scope: session }
            * ModelBuilderRequest { ref: hard }
            ModelBuildRequest ModelBuilderRequest { ref: soft }
            """;
        System.out.println("   Configuration:");
        System.out.println(partialConfig);
        System.out.println("   Effect: Base scope=session, default ref=hard, but ModelBuildRequest parent uses ref=soft");

        // Example 5: Multiple selectors with priority
        System.out.println("\n5. Multiple Selectors (ordered by specificity):");
        String multiConfig = """
            ArtifactResolutionRequest { scope: session, ref: soft }
            ModelBuildRequest { scope: request, ref: soft }
            ModelBuilderRequest VersionRangeRequest { ref: hard }
            ModelBuildRequest * { ref: hard }
            """;
        System.out.println("   Configuration:");
        System.out.println(multiConfig);
        
        // Example 6: Command line usage
        System.out.println("\n6. Command Line Usage:");
        System.out.println("   mvn clean install -Dmaven.cache.config=\"ModelBuilderRequest { scope: session, ref: hard }\"");
        System.out.println("   mvn clean install -Dmaven.cache.config=\"ModelBuilderRequest { scope: session }\"");
        System.out.println("   mvn clean install -Dmaven.cache.config=\"* { ref: weak }\"");

        // Example 7: Available options
        System.out.println("\n7. Available Options:");
        System.out.println("   Scopes: session, request, persistent, disabled");
        System.out.println("   Reference Types: soft, hard, weak, none");
        System.out.println("   Note: Both scope and ref are optional - missing values use defaults or merge from other selectors");

        // Example 8: Backward compatibility
        System.out.println("\n8. Backward Compatibility:");
        System.out.println("   - Requests implementing CacheMetadata still work");
        System.out.println("   - Default behavior unchanged when no configuration provided");
        System.out.println("   - ProtoSession requests automatically skip caching");
        
        System.out.println("\nFor more details, see the implementation in:");
        System.out.println("- impl/maven-impl/src/main/java/org/apache/maven/impl/cache/");
    }
}
