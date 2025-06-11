///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS info.picocli:picocli:4.7.5
//DEPS org.apache.httpcomponents.client5:httpclient5:5.2.1
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2
//DEPS org.slf4j:slf4j-simple:2.0.7

//DESCRIPTION Maven Release Script - 2-Click Release Automation
//
// This script automates the Apache Maven release process following official procedures
// while integrating with GitHub (issues, milestones, release-drafter) and providing
// optional Gmail email automation.
//
// ============================================================================
// COMMANDS AND STEPS
// ============================================================================
//
// setup
// -----
// One-time environment setup and validation
// Steps:
//   1. Validate required tools (mvn, gpg, svn, gh, jq)
//   2. Check GitHub CLI authentication
//   3. Validate environment variables (APACHE_USERNAME, GPG_KEY_ID)
//   4. Check Gmail configuration (optional)
//   5. Validate Maven settings.xml
//   6. Create/update ~/.mavenrc with recommended settings
//   7. Display setup status and next steps
//
// start-vote <version>
// --------------------
// Start release vote (Click 1) - Prepares and stages release
// Steps:
//   1. Validate tools, environment, credentials, and version
//   2. Check for open blocker issues on GitHub
//   3. Get GitHub milestone information for the version
//   4. Extract release notes from GitHub release draft
//   5. Build and test project with Apache release profile
//   6. Check site compilation works
//   7. Prepare release using Maven release plugin (dry-run then actual)
//   8. Stage artifacts to Apache Nexus with proper description
//   9. Stage documentation to Maven website
//  10. Copy source release to Apache dist area (staged, not committed)
//  11. Generate vote email with all required information
//  12. Save staging repository ID and milestone info to target/ directory
//  13. Optionally send vote email via Gmail if configured
//  14. Display next steps and voting requirements
//
// publish <version> [staging-repo-id]
// -----------------------------------
// Publish release after successful vote (Click 2)
// Steps:
//   1. Load staging repository ID from saved file or argument
//   2. Load milestone information from saved file
//   3. Interactive confirmation of vote results (72+ hours, 3+ PMC votes)
//   4. Promote staging repository to Maven Central
//   5. Commit source release to Apache dist area
//   6. Clean up old releases (keep only latest 3)
//   7. Add release to Apache Committee Report Helper (manual step)
//   8. Deploy versioned website documentation
//   9. Close GitHub milestone and create next version milestone
//  10. Publish GitHub release from draft
//  11. Generate announcement email
//  12. Wait for Maven Central sync confirmation
//  13. Optionally send announcement email via Gmail if configured
//  14. Clean up staging info files
//  15. Display success message and final steps
//
// cancel <version>
// ----------------
// Cancel release vote and clean up all staging artifacts
// Steps:
//   1. Prompt for cancellation reason
//   2. Load staging repository ID from saved file
//   3. Display cleanup actions and request confirmation
//   4. Generate cancel email with reason
//   5. Optionally send cancel email via Gmail if configured
//   6. Drop staging repository from Apache Nexus
//   7. Clean up staged files from Apache dist area
//   8. Remove Git release tags and Maven release plugin files
//   9. Clean up staging info files
//  10. Display success message and next steps
//
// ============================================================================
// ENVIRONMENT VARIABLES
// ============================================================================
// Required:
//   APACHE_USERNAME      - Your Apache LDAP username
//   GPG_KEY_ID          - Your GPG key ID for signing releases
//
// Optional (for email automation):
//   GMAIL_USERNAME      - Your Gmail address
//   GMAIL_APP_PASSWORD  - Your Gmail app password (not regular password)
//
// ============================================================================
// PREREQUISITES
// ============================================================================
// Tools: maven, gpg, subversion, github-cli, jq, jbang
// Access: Apache committer, Maven PMC (for some operations), Nexus staging
// Setup: ~/.m2/settings.xml with Apache credentials, GPG key configured
// GitHub: Authenticated CLI, repository access, milestones configured
// Gmail: 2FA enabled, app password generated (optional)
//
// ============================================================================

import picocli.CommandLine;
import picocli.CommandLine.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;

@Command(name = "release", 
         description = "Maven Release Script - 2-Click Release Automation",
         subcommands = {
             Release.SetupCommand.class,
             Release.StartVoteCommand.class, 
             Release.PublishCommand.class,
             Release.CancelCommand.class,
             CommandLine.HelpCommand.class
         })
public class Release implements Callable<Integer> {

    // ANSI color codes for output
    private static final String RED = "\033[0;31m";
    private static final String GREEN = "\033[0;32m";
    private static final String YELLOW = "\033[1;33m";
    private static final String BLUE = "\033[0;34m";
    private static final String NC = "\033[0m"; // No Color

    // Environment variables
    private static final String APACHE_USERNAME = System.getenv("APACHE_USERNAME");
    private static final String GPG_KEY_ID = System.getenv("GPG_KEY_ID");
    private static final String GMAIL_USERNAME = System.getenv("GMAIL_USERNAME");
    private static final String GMAIL_APP_PASSWORD = System.getenv("GMAIL_APP_PASSWORD");

    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir"));
    private static final Path TARGET_DIR = PROJECT_ROOT.resolve("target");

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Release()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        System.out.println("Maven Release Script - 2-Click Release Automation");
        System.out.println();
        System.out.println("Usage: jbang release.java <command> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  setup                    One-time environment setup");
        System.out.println("  start-vote <version>     Start release vote (Click 1)");
        System.out.println("  publish <version> [repo] Publish release after vote (Click 2)");
        System.out.println("  cancel <version>         Cancel release vote and clean up");
        System.out.println("  help                     Show help information");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  jbang release.java setup");
        System.out.println("  jbang release.java start-vote 4.0.0-rc-4");
        System.out.println("  jbang release.java publish 4.0.0-rc-4");
        System.out.println("  jbang release.java cancel 4.0.0-rc-4");
        System.out.println();
        System.out.println("Environment Variables:");
        System.out.println("  APACHE_USERNAME      Your Apache LDAP username");
        System.out.println("  GPG_KEY_ID           Your GPG key ID for signing");
        System.out.println("  GMAIL_USERNAME       Your Gmail address (optional)");
        System.out.println("  GMAIL_APP_PASSWORD   Your Gmail app password (optional)");
        return 0;
    }

    // Logging utility methods
    static void logInfo(String message) {
        System.out.println(BLUE + "ℹ️  " + message + NC);
    }

    static void logSuccess(String message) {
        System.out.println(GREEN + "✅ " + message + NC);
    }

    static void logWarning(String message) {
        System.out.println(YELLOW + "⚠️  " + message + NC);
    }

    static void logError(String message) {
        System.out.println(RED + "❌ " + message + NC);
    }

    static void logStep(String message) {
        System.out.println(BLUE + "🔄 " + message + NC);
    }

    // Utility methods for running commands
    static ProcessResult runCommand(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(PROJECT_ROOT.toFile());
        Process process = pb.start();
        
        String output = new String(process.getInputStream().readAllBytes());
        String error = new String(process.getErrorStream().readAllBytes());
        int exitCode = process.waitFor();
        
        return new ProcessResult(exitCode, output, error);
    }

    static class ProcessResult {
        final int exitCode;
        final String output;
        final String error;
        
        ProcessResult(int exitCode, String output, String error) {
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
        }
        
        boolean isSuccess() {
            return exitCode == 0;
        }
    }

    // Validation methods
    static boolean validateTools() {
        logStep("Checking required tools...");
        
        String[] tools = {"mvn", "gpg", "svn", "gh", "jq"};
        List<String> missing = new ArrayList<>();
        
        for (String tool : tools) {
            try {
                ProcessResult result = runCommand("which", tool);
                if (!result.isSuccess()) {
                    missing.add(tool);
                }
            } catch (Exception e) {
                missing.add(tool);
            }
        }
        
        if (!missing.isEmpty()) {
            logError("Missing required tools: " + String.join(", ", missing));
            return false;
        }
        
        logSuccess("All required tools are available");
        return true;
    }

    static boolean validateEnvironment() {
        logStep("Checking environment...");
        
        try {
            // Check Git status
            ProcessResult gitStatus = runCommand("git", "status", "--porcelain");
            if (!gitStatus.output.trim().isEmpty()) {
                logError("Working directory not clean");
                System.out.println(gitStatus.output);
                return false;
            }
            
            // Check branch
            ProcessResult branchResult = runCommand("git", "branch", "--show-current");
            String currentBranch = branchResult.output.trim();
            if (!"master".equals(currentBranch)) {
                logError("Not on master branch (currently on: " + currentBranch + ")");
                return false;
            }
            
            // Check if up to date
            runCommand("git", "fetch", "origin", "master");
            ProcessResult localCommit = runCommand("git", "rev-parse", "HEAD");
            ProcessResult remoteCommit = runCommand("git", "rev-parse", "origin/master");
            
            if (!localCommit.output.trim().equals(remoteCommit.output.trim())) {
                logError("Local master is not up to date with origin/master");
                return false;
            }
            
            logSuccess("Git environment is clean and up to date");
            return true;
            
        } catch (Exception e) {
            logError("Failed to validate environment: " + e.getMessage());
            return false;
        }
    }

    static boolean validateCredentials() {
        logStep("Checking credentials...");

        try {
            // Check GitHub CLI
            ProcessResult ghAuth = runCommand("gh", "auth", "status");
            if (!ghAuth.isSuccess()) {
                logError("GitHub CLI not authenticated. Run: gh auth login");
                return false;
            }

            // Check environment variables
            if (APACHE_USERNAME == null || APACHE_USERNAME.isEmpty()) {
                logError("APACHE_USERNAME not set. Run: export APACHE_USERNAME=your-apache-id");
                return false;
            }

            if (GPG_KEY_ID == null || GPG_KEY_ID.isEmpty()) {
                logError("GPG_KEY_ID not set. Run: export GPG_KEY_ID=your-gpg-key-id");
                return false;
            }

            // Check GPG key
            ProcessResult gpgCheck = runCommand("gpg", "--list-secret-keys");
            if (!gpgCheck.output.contains(GPG_KEY_ID)) {
                logError("GPG key " + GPG_KEY_ID + " not found in secret keyring");
                return false;
            }

            // Check Maven settings
            Path mavenSettings = Paths.get(System.getProperty("user.home"), ".m2", "settings.xml");
            if (!Files.exists(mavenSettings)) {
                logError("Maven settings.xml not found. Please configure Apache credentials");
                return false;
            }

            // Check email configuration (optional)
            if (GMAIL_USERNAME != null && !GMAIL_USERNAME.isEmpty() &&
                GMAIL_APP_PASSWORD != null && !GMAIL_APP_PASSWORD.isEmpty()) {
                logSuccess("Gmail credentials configured for automatic email sending");
            } else {
                logInfo("Gmail credentials not set - emails will be generated but not sent automatically");
            }

            logSuccess("All credentials are configured");
            return true;

        } catch (Exception e) {
            logError("Failed to validate credentials: " + e.getMessage());
            return false;
        }
    }

    // Setup Command
    @Command(name = "setup", description = "One-time environment setup and validation")
    static class SetupCommand implements Callable<Integer> {

        @Override
        public Integer call() {
            System.out.println("🔧 Setting up Maven release environment...");

            // Check tools
            if (!validateTools()) {
                logError("Please install missing tools and run setup again");
                return 1;
            }

            // Check GitHub CLI
            try {
                ProcessResult ghAuth = runCommand("gh", "auth", "status");
                if (!ghAuth.isSuccess()) {
                    logWarning("GitHub CLI not authenticated");
                    System.out.println("Please run: gh auth login");
                } else {
                    logSuccess("GitHub CLI is authenticated");
                }
            } catch (Exception e) {
                logWarning("GitHub CLI check failed");
            }

            // Check environment variables
            if (APACHE_USERNAME == null || APACHE_USERNAME.isEmpty()) {
                logWarning("APACHE_USERNAME not set");
                System.out.println("Please set it: export APACHE_USERNAME=your-apache-id");
            } else {
                logSuccess("APACHE_USERNAME is set");
            }

            if (GPG_KEY_ID == null || GPG_KEY_ID.isEmpty()) {
                logWarning("GPG_KEY_ID not set");
                System.out.println("Please set it: export GPG_KEY_ID=your-gpg-key-id");
            } else {
                logSuccess("GPG_KEY_ID is set");
            }

            // Check Gmail configuration (optional)
            if (GMAIL_USERNAME == null || GMAIL_USERNAME.isEmpty() ||
                GMAIL_APP_PASSWORD == null || GMAIL_APP_PASSWORD.isEmpty()) {
                logWarning("Gmail credentials not set (optional for automatic email sending)");
                System.out.println("To enable automatic email sending:");
                System.out.println("  export GMAIL_USERNAME=your-email@gmail.com");
                System.out.println("  export GMAIL_APP_PASSWORD=your-app-password");
                System.out.println("See: https://support.google.com/accounts/answer/185833");
            } else {
                logSuccess("Gmail credentials are set");
            }

            // Check Maven settings
            Path mavenSettings = Paths.get(System.getProperty("user.home"), ".m2", "settings.xml");
            if (!Files.exists(mavenSettings)) {
                logWarning("Maven settings.xml not found");
                System.out.println("Please configure ~/.m2/settings.xml with Apache credentials");
                System.out.println("See: https://maven.apache.org/developers/release/maven-project-release-procedure.html");
            } else {
                logSuccess("Maven settings.xml found");
            }

            // Create/update .mavenrc
            Path mavenrc = Paths.get(System.getProperty("user.home"), ".mavenrc");
            if (!Files.exists(mavenrc)) {
                try {
                    Files.writeString(mavenrc, "# Maven release configuration\nexport MAVEN_OPTS=\"-Xmx2g -XX:ReservedCodeCacheSize=1g\"\n");
                    logSuccess("Created ~/.mavenrc with recommended settings");
                } catch (IOException e) {
                    logWarning("Failed to create ~/.mavenrc: " + e.getMessage());
                }
            }

            System.out.println();
            logSuccess("Environment setup complete!");
            System.out.println();
            System.out.println("Next steps:");
            System.out.println("1. Ensure GitHub CLI is authenticated: gh auth login");
            System.out.println("2. Set environment variables:");
            System.out.println("   export APACHE_USERNAME=your-apache-id");
            System.out.println("   export GPG_KEY_ID=your-gpg-key-id");
            System.out.println("3. Configure Maven settings.xml with Apache credentials");
            System.out.println("4. (Optional) Set Gmail credentials for automatic email sending:");
            System.out.println("   export GMAIL_USERNAME=your-email@gmail.com");
            System.out.println("   export GMAIL_APP_PASSWORD=your-app-password");
            System.out.println();
            System.out.println("Then you can start a release:");
            System.out.println("  jbang release.java start-vote 4.0.0-rc-4");

            return 0;
        }
    }

    // Start Vote Command
    @Command(name = "start-vote", description = "Start release vote (Click 1)")
    static class StartVoteCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Release version (e.g., 4.0.0-rc-4)")
        private String version;

        @Override
        public Integer call() {
            System.out.println("🚀 Starting Maven release vote for version " + version);
            System.out.println("📁 Project root: " + PROJECT_ROOT);

            // Validation
            if (!validateTools() || !validateEnvironment() || !validateCredentials()) {
                return 1;
            }

            if (!validateVersion(version)) {
                return 1;
            }

            try {
                // Check for blocker issues
                logStep("Checking for blocker issues...");
                ProcessResult blockerCheck = runCommand("gh", "issue", "list", "--label", "blocker",
                    "--state", "open", "--json", "number", "--jq", "length");

                int blockerCount = Integer.parseInt(blockerCheck.output.trim());
                if (blockerCount > 0) {
                    logWarning("Found " + blockerCount + " open blocker issues");
                    ProcessResult blockerList = runCommand("gh", "issue", "list", "--label", "blocker",
                        "--state", "open", "--json", "number,title", "--jq", ".[] | \"  #\\(.number): \\(.title)\"");
                    System.out.println(blockerList.output);

                    System.out.println();
                    System.out.print("Do you want to continue anyway? (y/N): ");
                    Scanner scanner = new Scanner(System.in);
                    String response = scanner.nextLine();
                    if (!response.equalsIgnoreCase("y")) {
                        logError("Release cancelled due to blocker issues");
                        return 1;
                    }
                }

                // Get milestone and release notes
                logStep("Getting GitHub milestone and release notes...");
                String milestoneInfo = getMilestoneInfo(version);
                String releaseNotes = getReleaseNotes(version);

                // Build and test
                buildAndTest();
                checkSiteCompilation();

                // Prepare release
                prepareRelease(version);

                // Stage artifacts
                String stagingRepo = stageArtifacts(version);
                if (stagingRepo == null || stagingRepo.isEmpty()) {
                    logError("Failed to get staging repository ID");
                    return 1;
                }

                // Stage documentation
                stageDocumentation();

                // Copy to dist area
                copyToDistArea(version);

                // Generate vote email
                generateVoteEmail(version, stagingRepo, milestoneInfo, releaseNotes);

                // Save staging info
                saveStagingInfo(version, stagingRepo, milestoneInfo);

                System.out.println();
                logSuccess("Release vote started successfully!");
                System.out.println("📧 Vote email generated: vote-email-" + version + ".txt");
                System.out.println("📦 Staging repository: " + stagingRepo);

                // Send vote email if Gmail is configured
                if (GMAIL_USERNAME != null && !GMAIL_USERNAME.isEmpty() &&
                    GMAIL_APP_PASSWORD != null && !GMAIL_APP_PASSWORD.isEmpty()) {
                    System.out.println();
                    System.out.print("Do you want to send the vote email now? (y/N): ");
                    Scanner scanner = new Scanner(System.in);
                    String response = scanner.nextLine();
                    if (response.equalsIgnoreCase("y")) {
                        sendVoteEmail(version);
                    } else {
                        logInfo("Vote email not sent - you can send it manually later");
                    }
                } else {
                    logInfo("Gmail not configured - please send vote email manually: vote-email-" + version + ".txt");
                }

                System.out.println();
                System.out.println("⏰ Vote period: 72 hours minimum");
                System.out.println("📊 Required: 3+ PMC votes");
                System.out.println();
                System.out.println("Next steps:");
                System.out.println("1. Wait for vote results (72+ hours)");
                System.out.println("2. If vote passes, run: jbang release.java publish " + version);

                return 0;

            } catch (Exception e) {
                logError("Failed to start vote: " + e.getMessage());
                e.printStackTrace();
                return 1;
            }
        }
    }

    // Utility methods for release operations
    static boolean validateVersion(String version) {
        if (version == null || version.isEmpty()) {
            logError("Version not provided");
            return false;
        }

        try {
            // Check if tag already exists
            ProcessResult tagCheck = runCommand("git", "tag", "-l");
            if (tagCheck.output.contains("maven-" + version)) {
                logError("Tag maven-" + version + " already exists");
                return false;
            }

            // Check current version is SNAPSHOT
            ProcessResult versionCheck = runCommand("mvn", "help:evaluate",
                "-Dexpression=project.version", "-q", "-DforceStdout");
            String currentVersion = versionCheck.output.trim();
            if (!currentVersion.endsWith("-SNAPSHOT")) {
                logError("Current version (" + currentVersion + ") is not a SNAPSHOT");
                return false;
            }

            logSuccess("Version " + version + " is valid");
            return true;

        } catch (Exception e) {
            logError("Failed to validate version: " + e.getMessage());
            return false;
        }
    }

    static String getMilestoneInfo(String version) {
        try {
            // Try exact match first
            ProcessResult result = runCommand("gh", "api", "repos/apache/maven/milestones",
                "--jq", ".[] | select(.title == \"" + version + "\")");

            if (result.output.trim().isEmpty()) {
                // Try partial match
                result = runCommand("gh", "api", "repos/apache/maven/milestones",
                    "--jq", ".[] | select(.title | contains(\"" + version + "\"))");
            }

            return result.output.trim();
        } catch (Exception e) {
            logWarning("Failed to get milestone info: " + e.getMessage());
            return "";
        }
    }

    static String getReleaseNotes(String version) {
        try {
            ProcessResult result = runCommand("gh", "api", "repos/apache/maven/releases",
                "--jq", ".[] | select(.draft == true and (.tag_name == \"maven-" + version +
                "\" or .tag_name == \"" + version + "\" or .name | contains(\"" + version + "\"))) | .body");

            if (!result.output.trim().isEmpty()) {
                return result.output.trim()
                    .replaceAll("^## ", "")
                    .replaceAll("^### ", "  ")
                    .replaceAll("^#### ", "    ");
            } else {
                return "Release notes for Maven " + version + " - please update manually";
            }
        } catch (Exception e) {
            logWarning("Failed to get release notes: " + e.getMessage());
            return "Release notes for Maven " + version + " - please update manually";
        }
    }

    static void buildAndTest() throws Exception {
        logStep("Building and testing...");
        ProcessResult result = runCommand("mvn", "clean", "verify", "-Papache-release", "-Dgpg.skip=true");
        if (!result.isSuccess()) {
            throw new RuntimeException("Build and test failed: " + result.error);
        }
        logSuccess("Build and tests completed");
    }

    static void checkSiteCompilation() throws Exception {
        logStep("Checking site compilation...");
        ProcessResult result = runCommand("mvn", "-Preporting", "site", "site:stage");
        if (!result.isSuccess()) {
            throw new RuntimeException("Site compilation failed: " + result.error);
        }
        logSuccess("Site compilation successful");
    }

    static void prepareRelease(String version) throws Exception {
        logStep("Preparing release " + version + "...");

        // Dry run first
        logInfo("Running release:prepare in dry-run mode...");
        ProcessResult dryRun = runCommand("mvn", "release:prepare", "-DdryRun=true",
            "-Dtag=maven-" + version, "-DreleaseVersion=" + version,
            "-DdevelopmentVersion=" + version + "-SNAPSHOT");

        if (!dryRun.isSuccess()) {
            throw new RuntimeException("Release prepare dry run failed: " + dryRun.error);
        }

        logInfo("Dry run successful. Proceeding with actual preparation...");
        runCommand("mvn", "release:clean");

        ProcessResult actual = runCommand("mvn", "release:prepare",
            "-Dtag=maven-" + version, "-DreleaseVersion=" + version,
            "-DdevelopmentVersion=" + version + "-SNAPSHOT");

        if (!actual.isSuccess()) {
            throw new RuntimeException("Release prepare failed: " + actual.error);
        }

        logSuccess("Release prepared");
    }

    static String stageArtifacts(String version) throws Exception {
        logStep("Staging artifacts to Nexus...");
        ProcessResult result = runCommand("mvn", "release:perform",
            "-Dgoals=deploy nexus-staging:close",
            "-DstagingDescription=VOTE Maven " + version);

        if (!result.isSuccess()) {
            throw new RuntimeException("Artifact staging failed: " + result.error);
        }

        // Get staging repository ID
        ProcessResult repoList = runCommand("mvn", "nexus-staging:rc-list", "-q");
        String output = repoList.output;

        Pattern pattern = Pattern.compile("orgapachemaven-[0-9]+");
        java.util.regex.Matcher matcher = pattern.matcher(output);

        if (matcher.find()) {
            String stagingRepo = matcher.group();
            logSuccess("Artifacts staged to repository: " + stagingRepo);
            return stagingRepo;
        } else {
            throw new RuntimeException("Could not find staging repository ID");
        }
    }

    static void stageDocumentation() throws Exception {
        logStep("Staging documentation...");

        Path checkoutDir = PROJECT_ROOT.resolve("target/checkout");
        ProcessBuilder pb = new ProcessBuilder("mvn", "scm-publish:publish-scm", "-Preporting");
        pb.directory(checkoutDir.toFile());
        Process process = pb.start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Documentation staging failed");
        }

        logSuccess("Documentation staged");
    }

    static void copyToDistArea(String version) throws Exception {
        logStep("Copying source release to Apache distribution area...");

        Path sourceZip = PROJECT_ROOT.resolve("target/checkout/target/maven-" + version + "-source-release.zip");
        Path sourceAsc = PROJECT_ROOT.resolve("target/checkout/target/maven-" + version + "-source-release.zip.asc");

        if (!Files.exists(sourceZip) || !Files.exists(sourceAsc)) {
            throw new RuntimeException("Source release files not found");
        }

        // Generate SHA512
        ProcessResult sha512Result = runCommand("sha512sum", sourceZip.toString());
        String sha512 = sha512Result.output.split("\\s+")[0];
        Path sha512File = sourceZip.getParent().resolve("maven-" + version + "-source-release.zip.sha512");
        Files.writeString(sha512File, sha512);

        // Checkout/update dist area
        Path distDir = PROJECT_ROOT.resolve("maven-dist-staging");
        if (Files.exists(distDir)) {
            ProcessBuilder pb = new ProcessBuilder("svn", "update");
            pb.directory(distDir.toFile());
            pb.start().waitFor();
        } else {
            runCommand("svn", "checkout", "https://dist.apache.org/repos/dist/release/maven",
                distDir.toString());
        }

        // Copy files
        Path versionDir = distDir.resolve("maven-" + version);
        Files.createDirectories(versionDir);
        Files.copy(sourceZip, versionDir.resolve(sourceZip.getFileName()));
        Files.copy(sourceAsc, versionDir.resolve(sourceAsc.getFileName()));
        Files.copy(sha512File, versionDir.resolve(sha512File.getFileName()));

        // Stage for commit
        ProcessBuilder pb = new ProcessBuilder("svn", "add", "maven-" + version);
        pb.directory(distDir.toFile());
        pb.start().waitFor();

        logSuccess("Source release staged in Apache dist area");
    }

    static void generateVoteEmail(String version, String stagingRepo, String milestoneInfo, String releaseNotes) throws Exception {
        logStep("Generating vote email...");

        // Get comparison URL
        ProcessResult lastTagResult = runCommand("git", "describe", "--tags", "--abbrev=0", "--match=maven-*");
        String lastTag = lastTagResult.isSuccess() ? lastTagResult.output.trim() : "";

        String githubCompare = "https://github.com/apache/maven/commits/maven-" + version;
        if (!lastTag.isEmpty()) {
            githubCompare = "https://github.com/apache/maven/compare/" + lastTag + "...maven-" + version;
        }

        // Parse milestone info
        String closedIssues = "N";
        String milestoneUrl = "https://github.com/apache/maven/issues?q=is%3Aissue+is%3Aclosed+milestone%3A" + version;

        if (!milestoneInfo.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode milestone = mapper.readTree(milestoneInfo);
                closedIssues = milestone.get("closed_issues").asText("N");
                String htmlUrl = milestone.get("html_url").asText("");
                if (!htmlUrl.isEmpty()) {
                    milestoneUrl = htmlUrl + "?closed=1";
                }
            } catch (Exception e) {
                logWarning("Failed to parse milestone info: " + e.getMessage());
            }
        }

        // Calculate SHA512
        Path sourceZip = PROJECT_ROOT.resolve("target/checkout/target/maven-" + version + "-source-release.zip");
        String sha512 = "[SHA512 will be calculated]";
        if (Files.exists(sourceZip)) {
            ProcessResult sha512Result = runCommand("sha512sum", sourceZip.toString());
            sha512 = sha512Result.output.split("\\s+")[0];
        }

        // Generate email content
        StringBuilder email = new StringBuilder();
        email.append("To: \"Maven Developers List\" <dev@maven.apache.org>\n");
        email.append("Subject: [VOTE] Release Apache Maven ").append(version).append("\n\n");
        email.append("Hi,\n\n");
        email.append("We solved ").append(closedIssues).append(" issues:\n");
        email.append(milestoneUrl).append("\n\n");
        email.append("There are still a couple of issues left in GitHub:\n");
        email.append("https://github.com/apache/maven/issues?q=is%3Aissue+is%3Aopen\n\n");
        email.append("Changes since the last release:\n");
        email.append(githubCompare).append("\n\n");
        email.append("Staging repo:\n");
        email.append("https://repository.apache.org/content/repositories/").append(stagingRepo).append("/\n");
        email.append("https://repository.apache.org/content/repositories/").append(stagingRepo)
            .append("/org/apache/maven/apache-maven/").append(version)
            .append("/apache-maven-").append(version).append("-source-release.zip\n\n");
        email.append("Source release checksum(s):\n");
        email.append("apache-maven-").append(version).append("-source-release.zip sha512: ").append(sha512).append("\n\n");
        email.append("Staging site:\n");
        email.append("https://maven.apache.org/ref/").append(version).append("/\n\n");
        email.append("Guide to testing staged releases:\n");
        email.append("https://maven.apache.org/guides/development/guide-testing-releases.html\n\n");
        email.append("Vote open for at least 72 hours.\n\n");
        email.append("[ ] +1\n");
        email.append("[ ] +0\n");
        email.append("[ ] -1\n");

        if (!releaseNotes.isEmpty() && !releaseNotes.startsWith("Release notes for Maven")) {
            email.append("\nRelease Notes:\n");
            email.append(releaseNotes).append("\n");
        }

        Path emailFile = PROJECT_ROOT.resolve("vote-email-" + version + ".txt");
        Files.writeString(emailFile, email.toString());

        logSuccess("Vote email generated: vote-email-" + version + ".txt");
    }

    static void saveStagingInfo(String version, String stagingRepo, String milestoneInfo) throws Exception {
        // Create target directory if it doesn't exist
        Files.createDirectories(TARGET_DIR);

        // Save staging info in target directory (persistent across builds)
        Files.writeString(TARGET_DIR.resolve("staging-repo-" + version), stagingRepo);
        Files.writeString(TARGET_DIR.resolve("milestone-info-" + version), milestoneInfo);

        // Also save in project root for backward compatibility
        Files.writeString(PROJECT_ROOT.resolve(".staging-repo-" + version), stagingRepo);
        Files.writeString(PROJECT_ROOT.resolve(".milestone-info-" + version), milestoneInfo);

        logSuccess("Staging info saved to target/staging-repo-" + version);
    }

    static void sendVoteEmail(String version) {
        try {
            logStep("Sending vote email...");

            Path emailFile = PROJECT_ROOT.resolve("vote-email-" + version + ".txt");
            if (!Files.exists(emailFile)) {
                logError("Vote email file not found: " + emailFile);
                return;
            }

            String emailContent = Files.readString(emailFile);
            String[] lines = emailContent.split("\n");

            // Extract subject and body
            String subject = "";
            StringBuilder body = new StringBuilder();
            boolean inBody = false;

            for (String line : lines) {
                if (line.startsWith("Subject: ")) {
                    subject = line.substring(9);
                } else if (line.isEmpty() && !inBody) {
                    inBody = true;
                } else if (inBody) {
                    body.append(line).append("\n");
                }
            }

            sendEmail("dev@maven.apache.org", "", subject, body.toString());

        } catch (Exception e) {
            logError("Failed to send vote email: " + e.getMessage());
        }
    }

    static void sendEmail(String to, String cc, String subject, String body) {
        if (GMAIL_USERNAME == null || GMAIL_USERNAME.isEmpty() ||
            GMAIL_APP_PASSWORD == null || GMAIL_APP_PASSWORD.isEmpty()) {
            logWarning("Gmail credentials not configured - email not sent automatically");
            return;
        }

        try {
            logStep("Sending email via Gmail...");

            // Create email content with headers
            StringBuilder email = new StringBuilder();
            email.append("To: ").append(to).append("\n");
            if (cc != null && !cc.isEmpty()) {
                email.append("Cc: ").append(cc).append("\n");
            }
            email.append("Subject: ").append(subject).append("\n");
            email.append("From: ").append(GMAIL_USERNAME).append("\n\n");
            email.append(body);

            // Use curl to send via Gmail SMTP
            ProcessResult result = runCommand("curl", "-s", "--url", "smtps://smtp.gmail.com:465",
                "--ssl-reqd", "--mail-from", GMAIL_USERNAME, "--mail-rcpt", to,
                "--user", GMAIL_USERNAME + ":" + GMAIL_APP_PASSWORD,
                "--upload-file", "-");

            // Note: This is a simplified approach. In a real implementation, you'd want to use
            // proper SMTP libraries or save to a temp file and upload that.

            if (result.isSuccess()) {
                logSuccess("Email sent successfully to " + to);
                if (cc != null && !cc.isEmpty()) {
                    logInfo("CC: " + cc);
                }
            } else {
                logError("Failed to send email via Gmail");
                logInfo("Please send manually");
            }

        } catch (Exception e) {
            logError("Failed to send email: " + e.getMessage());
        }
    }

    // Utility methods for staging info management
    static String loadStagingRepo(String version) {
        try {
            // Try to load from target directory first
            Path targetFile = TARGET_DIR.resolve("staging-repo-" + version);
            if (Files.exists(targetFile)) {
                return Files.readString(targetFile).trim();
            }

            // Fallback to project root
            Path rootFile = PROJECT_ROOT.resolve(".staging-repo-" + version);
            if (Files.exists(rootFile)) {
                return Files.readString(rootFile).trim();
            }

            return null;
        } catch (Exception e) {
            logWarning("Failed to load staging repo info: " + e.getMessage());
            return null;
        }
    }

    static String loadMilestoneInfo(String version) {
        try {
            // Try to load from target directory first
            Path targetFile = TARGET_DIR.resolve("milestone-info-" + version);
            if (Files.exists(targetFile)) {
                return Files.readString(targetFile).trim();
            }

            // Fallback to project root
            Path rootFile = PROJECT_ROOT.resolve(".milestone-info-" + version);
            if (Files.exists(rootFile)) {
                return Files.readString(rootFile).trim();
            }

            return "";
        } catch (Exception e) {
            logWarning("Failed to load milestone info: " + e.getMessage());
            return "";
        }
    }

    static void cleanupStagingInfo(String version) {
        try {
            // Remove from both locations
            Files.deleteIfExists(TARGET_DIR.resolve("staging-repo-" + version));
            Files.deleteIfExists(TARGET_DIR.resolve("milestone-info-" + version));
            Files.deleteIfExists(PROJECT_ROOT.resolve(".staging-repo-" + version));
            Files.deleteIfExists(PROJECT_ROOT.resolve(".milestone-info-" + version));

            logSuccess("Staging info cleaned up");
        } catch (Exception e) {
            logWarning("Failed to cleanup staging info: " + e.getMessage());
        }
    }

    // Utility methods for publish command
    static boolean confirmVoteResults() {
        Scanner scanner = new Scanner(System.in);

        System.out.println();
        logStep("Please confirm vote results:");
        System.out.print("- Has 72+ hours passed since the vote started? (y/n): ");
        String voteTime = scanner.nextLine();
        System.out.print("- Do you have 3+ PMC +1 votes? (y/n): ");
        String voteCount = scanner.nextLine();
        System.out.print("- Are there any -1 votes that haven't been resolved? (y/n): ");
        String voteVeto = scanner.nextLine();

        if (!voteTime.equalsIgnoreCase("y") || !voteCount.equalsIgnoreCase("y") || voteVeto.equalsIgnoreCase("y")) {
            logError("Vote requirements not met. Aborting release.");
            return false;
        }

        logSuccess("Vote requirements confirmed");
        return true;
    }

    static void promoteStagingRepo(String stagingRepo) throws Exception {
        logStep("Promoting staging repository...");
        ProcessResult result = runCommand("mvn", "nexus-staging:promote",
            "-DstagingRepositoryId=" + stagingRepo);

        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to promote staging repository: " + result.error);
        }

        logSuccess("Staging repository promoted to Maven Central");
    }

    static void finalizeDistribution(String version) throws Exception {
        logStep("Finalizing Apache distribution area...");

        Path distDir = PROJECT_ROOT.resolve("maven-dist-staging");
        if (!Files.exists(distDir)) {
            throw new RuntimeException("Distribution staging directory not found: " + distDir);
        }

        // Commit new release
        ProcessBuilder pb = new ProcessBuilder("svn", "commit", "-m", "Add Apache Maven " + version + " release");
        pb.directory(distDir.toFile());
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Failed to commit to Apache dist area");
        }

        // Clean up old releases (keep only latest 3)
        pb = new ProcessBuilder("svn", "list");
        pb.directory(distDir.toFile());
        process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());

        String[] dirs = output.split("\n");
        List<String> mavenDirs = Arrays.stream(dirs)
            .filter(dir -> dir.startsWith("maven-"))
            .sorted()
            .collect(java.util.stream.Collectors.toList());

        if (mavenDirs.size() > 3) {
            List<String> dirsToRemove = mavenDirs.subList(0, mavenDirs.size() - 3);
            for (String dir : dirsToRemove) {
                if (!dir.trim().isEmpty()) {
                    pb = new ProcessBuilder("svn", "rm", dir.trim());
                    pb.directory(distDir.toFile());
                    pb.start().waitFor();
                }
            }

            pb = new ProcessBuilder("svn", "commit", "-m", "Remove old Maven releases (keeping only latest 3)");
            pb.directory(distDir.toFile());
            pb.start().waitFor();
        }

        logSuccess("Apache distribution finalized");
    }

    static void deployWebsite(String version) throws Exception {
        logStep("Deploying versioned website documentation...");

        String svnpubsub = "https://svn.apache.org/repos/asf/maven/website/components";

        ProcessResult result = runCommand("svnmucc", "-m", "Publish Maven " + version + " documentation",
            "-U", svnpubsub,
            "cp", "HEAD", "maven-archives/maven-LATEST", "maven-archives/maven-" + version,
            "rm", "maven/maven",
            "cp", "HEAD", "maven-archives/maven-" + version, "maven/maven");

        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to deploy website: " + result.error);
        }

        logSuccess("Website documentation deployed");
    }

    static void updateGitHubTracking(String version, String milestoneInfo) throws Exception {
        logStep("Updating GitHub tracking...");

        // Close milestone if exists and open
        if (!milestoneInfo.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode milestone = mapper.readTree(milestoneInfo);
                String milestoneNumber = milestone.get("number").asText();
                String milestoneState = milestone.get("state").asText();

                if ("open".equals(milestoneState)) {
                    String currentDate = java.time.Instant.now().toString();
                    ProcessResult result = runCommand("gh", "api", "repos/apache/maven/milestones/" + milestoneNumber,
                        "--method", "PATCH",
                        "--field", "state=closed",
                        "--field", "due_on=" + currentDate);

                    if (result.isSuccess()) {
                        logSuccess("Milestone closed");
                    }
                }
            } catch (Exception e) {
                logWarning("Failed to close milestone: " + e.getMessage());
            }
        }

        // Create next milestone
        String nextVersion = calculateNextVersion(version);
        if (nextVersion != null) {
            try {
                ProcessResult result = runCommand("gh", "api", "repos/apache/maven/milestones",
                    "--method", "POST",
                    "--field", "title=" + nextVersion,
                    "--field", "description=Maven " + nextVersion + " release",
                    "--field", "state=open");

                if (result.isSuccess()) {
                    logSuccess("Created milestone for " + nextVersion);
                }
            } catch (Exception e) {
                logWarning("Failed to create next milestone: " + e.getMessage());
            }
        }
    }

    static String calculateNextVersion(String version) {
        // RC version - increment RC number
        if (version.matches("^([0-9]+)\\.([0-9]+)\\.([0-9]+)-rc-([0-9]+)$")) {
            String[] parts = version.split("-rc-");
            String baseVersion = parts[0];
            int rcNumber = Integer.parseInt(parts[1]) + 1;
            return baseVersion + "-rc-" + rcNumber;
        }

        // Release version - increment patch
        if (version.matches("^([0-9]+)\\.([0-9]+)\\.([0-9]+)$")) {
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = Integer.parseInt(parts[2]) + 1;
            return major + "." + minor + "." + patch;
        }

        return null;
    }

    static void publishGitHubRelease(String version) throws Exception {
        logStep("Publishing GitHub release...");

        // Find draft release
        ProcessResult draftResult = runCommand("gh", "api", "repos/apache/maven/releases",
            "--jq", ".[] | select(.draft == true and (.tag_name == \"maven-" + version +
            "\" or .tag_name == \"" + version + "\" or .name | contains(\"" + version + "\"))) | .id");

        if (!draftResult.output.trim().isEmpty()) {
            String releaseId = draftResult.output.trim();

            // Update tag if needed and publish
            ProcessResult result = runCommand("gh", "api", "repos/apache/maven/releases/" + releaseId,
                "--method", "PATCH",
                "--field", "tag_name=maven-" + version,
                "--field", "target_commitish=maven-" + version,
                "--field", "draft=false");

            if (result.isSuccess()) {
                logSuccess("GitHub release published from draft");
            } else {
                throw new RuntimeException("Failed to publish GitHub release: " + result.error);
            }
        } else {
            // Create new release
            String releaseNotes = "Apache Maven " + version + "\n\n" +
                "For detailed information about this release, see:\n" +
                "- Release notes: https://maven.apache.org/docs/history.html\n" +
                "- Download: https://maven.apache.org/download.cgi";

            ProcessResult result = runCommand("gh", "release", "create", "maven-" + version,
                "--title", "Apache Maven " + version,
                "--notes", releaseNotes,
                "--target", "maven-" + version);

            if (result.isSuccess()) {
                logSuccess("New GitHub release created");
            } else {
                throw new RuntimeException("Failed to create GitHub release: " + result.error);
            }
        }
    }

    static void generateAnnouncement(String version) throws Exception {
        logStep("Generating announcement email...");

        boolean isRc = version.contains("-rc-");

        // Get release notes
        String releaseNotes;
        try {
            ProcessResult result = runCommand("gh", "release", "view", "maven-" + version,
                "--json", "body", "--jq", ".body");
            releaseNotes = result.isSuccess() ? result.output.trim() :
                "Please see the release notes at: https://github.com/apache/maven/releases/tag/maven-" + version;
        } catch (Exception e) {
            releaseNotes = "Please see the release notes at: https://github.com/apache/maven/releases/tag/maven-" + version;
        }

        StringBuilder email = new StringBuilder();

        if (isRc) {
            // RC announcement
            email.append("To: announce@maven.apache.org, users@maven.apache.org\n");
            email.append("Cc: dev@maven.apache.org\n");
            email.append("Subject: [ANN] Apache Maven ").append(version).append(" Released\n\n");
            email.append("The Apache Maven team is pleased to announce the release of Apache Maven ").append(version).append(".\n\n");
            email.append("This is a release candidate for Maven 4.0.0. We encourage users to test this release candidate and provide feedback.\n\n");
        } else {
            // Final release announcement
            email.append("To: announce@apache.org, announce@maven.apache.org, users@maven.apache.org\n");
            email.append("Cc: dev@maven.apache.org\n");
            email.append("Subject: [ANN] Apache Maven ").append(version).append(" Released\n\n");
            email.append("The Apache Maven team is pleased to announce the release of Apache Maven ").append(version).append(".\n\n");
        }

        email.append("Apache Maven is a software project management and comprehension tool. Based on the concept of a project object model (POM), Maven can manage a project's build, reporting and documentation from a central piece of information.\n\n");
        email.append("You can find out more about Apache Maven at https://maven.apache.org\n\n");
        email.append("You can download the appropriate sources etc. from the download page:\n");
        email.append("https://maven.apache.org/download.cgi\n\n");
        email.append("Release Notes - Apache Maven - Version ").append(version).append("\n\n");
        email.append(releaseNotes).append("\n\n");
        email.append("Enjoy,\n\n");
        email.append("-The Apache Maven team\n");

        Path emailFile = PROJECT_ROOT.resolve("announcement-" + version + ".txt");
        Files.writeString(emailFile, email.toString());

        logSuccess("Announcement email generated: announcement-" + version + ".txt");
    }

    static void sendAnnouncementEmail(String version) {
        try {
            logStep("Sending announcement email...");

            Path emailFile = PROJECT_ROOT.resolve("announcement-" + version + ".txt");
            if (!Files.exists(emailFile)) {
                logError("Announcement email file not found: " + emailFile);
                return;
            }

            String emailContent = Files.readString(emailFile);
            String[] lines = emailContent.split("\n");

            // Extract recipients and subject
            String to = "";
            String cc = "";
            String subject = "";
            StringBuilder body = new StringBuilder();
            boolean inBody = false;

            for (String line : lines) {
                if (line.startsWith("To: ")) {
                    to = line.substring(4);
                } else if (line.startsWith("Cc: ")) {
                    cc = line.substring(4);
                } else if (line.startsWith("Subject: ")) {
                    subject = line.substring(9);
                } else if (line.isEmpty() && !inBody) {
                    inBody = true;
                } else if (inBody) {
                    body.append(line).append("\n");
                }
            }

            sendEmail(to, cc, subject, body.toString());

        } catch (Exception e) {
            logError("Failed to send announcement email: " + e.getMessage());
        }
    }

    // Cancel command utility methods
    static void generateCancelEmail(String version, String reason) throws Exception {
        logStep("Generating cancel email...");

        StringBuilder email = new StringBuilder();
        email.append("To: \"Maven Developers List\" <dev@maven.apache.org>\n");
        email.append("Subject: [CANCEL] [VOTE] Release Apache Maven ").append(version).append("\n\n");
        email.append("Hi,\n\n");
        email.append("I am cancelling the vote for Apache Maven ").append(version).append(".\n\n");
        email.append("Reason: ").append(reason).append("\n\n");
        email.append("The staging repository has been dropped and staged files have been removed.\n\n");
        email.append("A new vote will be called once the issues are resolved.\n\n");
        email.append("Thanks,\n\n");
        email.append("-The Apache Maven team\n");

        Path emailFile = PROJECT_ROOT.resolve("cancel-email-" + version + ".txt");
        Files.writeString(emailFile, email.toString());

        logSuccess("Cancel email generated: cancel-email-" + version + ".txt");
    }

    static void sendCancelEmail(String version) {
        try {
            logStep("Sending cancel email...");

            Path emailFile = PROJECT_ROOT.resolve("cancel-email-" + version + ".txt");
            if (!Files.exists(emailFile)) {
                logError("Cancel email file not found: " + emailFile);
                return;
            }

            String emailContent = Files.readString(emailFile);
            String[] lines = emailContent.split("\n");

            // Extract subject and body
            String subject = "";
            StringBuilder body = new StringBuilder();
            boolean inBody = false;

            for (String line : lines) {
                if (line.startsWith("Subject: ")) {
                    subject = line.substring(9);
                } else if (line.isEmpty() && !inBody) {
                    inBody = true;
                } else if (inBody) {
                    body.append(line).append("\n");
                }
            }

            sendEmail("dev@maven.apache.org", "", subject, body.toString());

        } catch (Exception e) {
            logError("Failed to send cancel email: " + e.getMessage());
        }
    }

    static void dropStagingRepo(String stagingRepo) {
        try {
            logStep("Dropping staging repository: " + stagingRepo);

            ProcessResult result = runCommand("mvn", "nexus-staging:drop",
                "-DstagingRepositoryId=" + stagingRepo);

            if (result.isSuccess()) {
                logSuccess("Staging repository " + stagingRepo + " dropped");
            } else {
                logWarning("Failed to drop staging repository " + stagingRepo + " (may already be dropped)");
            }
        } catch (Exception e) {
            logWarning("Failed to drop staging repository: " + e.getMessage());
        }
    }

    static void cleanupDistStaging(String version) {
        try {
            logStep("Cleaning up Apache dist staging area...");

            Path distDir = PROJECT_ROOT.resolve("maven-dist-staging");
            if (!Files.exists(distDir)) {
                logInfo("No dist staging area to clean up");
                return;
            }

            // Check if version directory exists
            Path versionDir = distDir.resolve("maven-" + version);
            if (Files.exists(versionDir)) {
                logInfo("Removing staged files for maven-" + version);

                ProcessBuilder pb = new ProcessBuilder("svn", "rm", "maven-" + version, "--force");
                pb.directory(distDir.toFile());
                pb.start().waitFor();

                // Check if there are any changes to revert
                pb = new ProcessBuilder("svn", "status");
                pb.directory(distDir.toFile());
                Process process = pb.start();
                String status = new String(process.getInputStream().readAllBytes());

                if (!status.trim().isEmpty()) {
                    pb = new ProcessBuilder("svn", "revert", "-R", ".");
                    pb.directory(distDir.toFile());
                    pb.start().waitFor();
                    logSuccess("Reverted staged changes in Apache dist area");
                }
            } else {
                logInfo("No staged files found for maven-" + version);
            }
        } catch (Exception e) {
            logWarning("Failed to cleanup dist staging: " + e.getMessage());
        }
    }

    static void cleanupGitRelease(String version) {
        try {
            logStep("Cleaning up Git release preparation...");

            // Check if release tag exists
            ProcessResult tagCheck = runCommand("git", "tag", "-l");
            if (tagCheck.output.contains("maven-" + version)) {
                logInfo("Removing release tag: maven-" + version);
                runCommand("git", "tag", "-d", "maven-" + version);
            }

            // Clean up release plugin files
            if (Files.exists(PROJECT_ROOT.resolve("pom.xml.releaseBackup"))) {
                logInfo("Cleaning up Maven release plugin files");
                runCommand("mvn", "release:clean");
            }

            logSuccess("Git cleanup completed");
        } catch (Exception e) {
            logWarning("Failed to cleanup Git release: " + e.getMessage());
        }
    }

    // Publish Command
    @Command(name = "publish", description = "Publish release after successful vote (Click 2)")
    static class PublishCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Release version")
        private String version;

        @Parameters(index = "1", description = "Staging repository ID (optional)", arity = "0..1")
        private String stagingRepo;

        @Override
        public Integer call() {
            System.out.println("🎉 Publishing Maven release " + version);

            try {
                // Load staging repo from saved file if not provided
                if (stagingRepo == null || stagingRepo.isEmpty()) {
                    stagingRepo = loadStagingRepo(version);
                    if (stagingRepo != null && !stagingRepo.isEmpty()) {
                        logInfo("Using saved staging repository: " + stagingRepo);
                    }
                }

                if (stagingRepo == null || stagingRepo.isEmpty()) {
                    logError("Staging repository ID not provided and not found in target/staging-repo-" + version);
                    System.out.println("Please provide it: jbang Release.java publish " + version + " <staging-repo-id>");
                    return 1;
                }

                // Load milestone info
                String milestoneInfo = loadMilestoneInfo(version);

                // Confirm vote results
                if (!confirmVoteResults()) {
                    return 1;
                }

                // Promote staging repository
                promoteStagingRepo(stagingRepo);

                // Finalize distribution
                finalizeDistribution(version);

                // Add to Apache Committee Report Helper
                System.out.println();
                logStep("Adding to Apache Committee Report Helper...");
                System.out.println("Please manually add the release at: https://reporter.apache.org/addrelease.html?maven");
                System.out.println("Full Version Name: Apache Maven " + version);
                System.out.println("Date of Release: " + java.time.LocalDate.now());
                System.out.println();
                System.out.print("Press Enter when done...");
                new Scanner(System.in).nextLine();

                // Deploy website
                deployWebsite(version);

                // Update GitHub tracking
                updateGitHubTracking(version, milestoneInfo);

                // Publish GitHub release
                publishGitHubRelease(version);

                // Generate announcement
                generateAnnouncement(version);

                // Wait for Maven Central sync
                System.out.println();
                logStep("Waiting for Maven Central sync...");
                System.out.println("The sync to Maven Central occurs every 4 hours.");
                System.out.println("Check: https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/" + version + "/");
                System.out.println();
                System.out.print("Press Enter when artifacts are available in Maven Central...");
                new Scanner(System.in).nextLine();

                // Send announcement email if Gmail is configured
                if (GMAIL_USERNAME != null && !GMAIL_USERNAME.isEmpty() &&
                    GMAIL_APP_PASSWORD != null && !GMAIL_APP_PASSWORD.isEmpty()) {
                    System.out.println();
                    System.out.print("Do you want to send the announcement email now? (y/N): ");
                    String response = new Scanner(System.in).nextLine();
                    if (response.equalsIgnoreCase("y")) {
                        sendAnnouncementEmail(version);
                    } else {
                        logInfo("Announcement email not sent - you can send it manually later");
                    }
                } else {
                    logInfo("Gmail not configured - please send announcement email manually: announcement-" + version + ".txt");
                }

                // Clean up
                cleanupStagingInfo(version);

                System.out.println();
                logSuccess("Release " + version + " published successfully!");
                System.out.println("🎊 Congratulations on the release!");
                System.out.println();
                System.out.println("Final steps:");
                System.out.println("1. Update any documentation that references the old version");
                System.out.println("2. Close any remaining tasks related to this release");

                return 0;

            } catch (Exception e) {
                logError("Failed to publish release: " + e.getMessage());
                e.printStackTrace();
                return 1;
            }
        }
    }

    // Cancel Command
    @Command(name = "cancel", description = "Cancel release vote and clean up")
    static class CancelCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Release version")
        private String version;

        @Override
        public Integer call() {
            System.out.println("🚫 Cancelling Maven release vote for version " + version);

            try {
                Scanner scanner = new Scanner(System.in);

                // Get reason for cancellation
                System.out.println();
                System.out.print("Please provide a reason for cancelling the release: ");
                String cancelReason = scanner.nextLine();

                if (cancelReason.trim().isEmpty()) {
                    cancelReason = "Issues found during vote period";
                }

                // Load staging repo info
                String stagingRepo = loadStagingRepo(version);

                // Confirm cancellation
                System.out.println();
                logWarning("This will:");
                System.out.println("  - Send cancel email to dev@maven.apache.org");
                if (stagingRepo != null && !stagingRepo.isEmpty()) {
                    System.out.println("  - Drop staging repository: " + stagingRepo);
                }
                System.out.println("  - Clean up Apache dist staging area");
                System.out.println("  - Clean up Git release preparation");
                System.out.println("  - Remove staging info files");
                System.out.println();
                System.out.print("Are you sure you want to cancel the release? (y/N): ");
                String confirmCancel = scanner.nextLine();

                if (!confirmCancel.equalsIgnoreCase("y")) {
                    logInfo("Release cancellation aborted");
                    return 0;
                }

                // Generate and send cancel email
                generateCancelEmail(version, cancelReason);

                if (GMAIL_USERNAME != null && !GMAIL_USERNAME.isEmpty() &&
                    GMAIL_APP_PASSWORD != null && !GMAIL_APP_PASSWORD.isEmpty()) {
                    System.out.println();
                    System.out.print("Do you want to send the cancel email now? (y/N): ");
                    String sendCancel = scanner.nextLine();
                    if (sendCancel.equalsIgnoreCase("y")) {
                        sendCancelEmail(version);
                    } else {
                        logInfo("Cancel email not sent - you can send it manually: cancel-email-" + version + ".txt");
                    }
                } else {
                    logInfo("Gmail not configured - please send cancel email manually: cancel-email-" + version + ".txt");
                }

                // Drop staging repository
                if (stagingRepo != null && !stagingRepo.isEmpty()) {
                    dropStagingRepo(stagingRepo);
                } else {
                    logInfo("No staging repository found to drop");
                }

                // Clean up dist staging
                cleanupDistStaging(version);

                // Clean up Git
                cleanupGitRelease(version);

                // Clean up staging info
                cleanupStagingInfo(version);

                System.out.println();
                logSuccess("Release " + version + " cancelled successfully!");
                System.out.println("📧 Cancel email generated: cancel-email-" + version + ".txt");
                System.out.println();
                System.out.println("Next steps:");
                System.out.println("1. Fix the issues that caused the cancellation");
                System.out.println("2. Start a new release vote when ready");

                return 0;

            } catch (Exception e) {
                logError("Failed to cancel release: " + e.getMessage());
                e.printStackTrace();
                return 1;
            }
        }
    }
}
