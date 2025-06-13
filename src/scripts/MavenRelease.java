///usr/bin/env jbang "$0" "$@" ; exit $?

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
//   GMAIL_USERNAME      - Your Gmail address (for authentication)
//   GMAIL_APP_PASSWORD  - Your Gmail app password (not regular password)
//   GMAIL_SENDER_ADDRESS - Email address to use as sender (optional, defaults to GMAIL_USERNAME)
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
import java.nio.file.StandardOpenOption;
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
             MavenRelease.SetupCommand.class,
             MavenRelease.StartVoteCommand.class,
             MavenRelease.PublishCommand.class,
             MavenRelease.CancelCommand.class,
             MavenRelease.StatusCommand.class,
             CommandLine.HelpCommand.class
         })
public class MavenRelease implements Callable<Integer> {

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
    private static final String GMAIL_SENDER_ADDRESS = System.getenv("GMAIL_SENDER_ADDRESS");

    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir"));
    private static final Path TARGET_DIR = PROJECT_ROOT.resolve("target");
    private static final Path LOGS_DIR = TARGET_DIR.resolve("release-logs");

    // Release step tracking
    enum ReleaseStep {
        VALIDATION("validation"),
        BLOCKER_CHECK("blocker-check"),
        MILESTONE_INFO("milestone-info"),
        BUILD_TEST("build-test"),
        SITE_CHECK("site-check"),
        PREPARE_RELEASE("prepare-release"),
        STAGE_ARTIFACTS("stage-artifacts"),
        STAGE_DOCS("stage-docs"),
        COPY_DIST("copy-dist"),
        GENERATE_EMAIL("generate-email"),
        SAVE_INFO("save-info"),
        COMPLETED("completed");

        private final String stepName;

        ReleaseStep(String stepName) {
            this.stepName = stepName;
        }

        public String getStepName() {
            return stepName;
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MavenRelease()).execute(args);
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
        System.out.println("  status <version>         Check release status and logs");
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
        System.out.println("  GMAIL_USERNAME       Your Gmail address for authentication (optional)");
        System.out.println("  GMAIL_APP_PASSWORD   Your Gmail app password (optional)");
        System.out.println("  GMAIL_SENDER_ADDRESS Email address to use as sender (optional)");
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

    // Enhanced logging and step tracking methods
    static void initializeLogging(String version) throws IOException {
        Files.createDirectories(LOGS_DIR);
        Path logFile = LOGS_DIR.resolve("release-" + version + ".log");

        // Create or append to log file
        String timestamp = java.time.LocalDateTime.now().toString();
        String header = "\n=== Maven Release Log for " + version + " - " + timestamp + " ===\n";
        Files.writeString(logFile, header, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        logInfo("Logging initialized: " + logFile);
    }

    static void logToFile(String version, String step, String message) {
        try {
            // Ensure logs directory exists
            Files.createDirectories(LOGS_DIR);

            Path logFile = LOGS_DIR.resolve("release-" + version + ".log");
            String timestamp = java.time.LocalDateTime.now().toString();
            String logEntry = "[" + timestamp + "] [" + step + "] " + message + "\n";
            Files.writeString(logFile, logEntry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            logWarning("Failed to write to log file: " + LOGS_DIR.resolve("release-" + version + ".log"));
        }
    }

    static void saveCurrentStep(String version, ReleaseStep step) {
        try {
            // Ensure both target and logs directories exist
            Files.createDirectories(TARGET_DIR);
            Files.createDirectories(LOGS_DIR);

            Path stepFile = TARGET_DIR.resolve("current-step-" + version);
            Files.writeString(stepFile, step.getStepName());
            logToFile(version, "STEP", "Starting step: " + step.getStepName());
        } catch (IOException e) {
            logWarning("Failed to save current step: " + e.getMessage());
        }
    }

    static ReleaseStep getCurrentStep(String version) {
        try {
            Path stepFile = TARGET_DIR.resolve("current-step-" + version);
            if (Files.exists(stepFile)) {
                String stepName = Files.readString(stepFile).trim();
                for (ReleaseStep step : ReleaseStep.values()) {
                    if (step.getStepName().equals(stepName)) {
                        return step;
                    }
                }
            }
        } catch (IOException e) {
            logWarning("Failed to read current step: " + e.getMessage());
        }
        return ReleaseStep.VALIDATION; // Default to first step
    }

    static boolean isStepCompleted(String version, ReleaseStep step) {
        ReleaseStep currentStep = getCurrentStep(version);
        return currentStep.ordinal() > step.ordinal();
    }

    // Enhanced command execution with detailed logging
    static ProcessResult runCommandSimple(String... command) throws IOException, InterruptedException {
        return runCommandWithLogging(null, null, command);
    }

    static ProcessResult runCommandWithLogging(String version, String step, String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(PROJECT_ROOT.toFile());

        // Log command execution
        String commandStr = String.join(" ", command);
        if (version != null && step != null) {
            logToFile(version, step, "Executing: " + commandStr);
        }

        Process process = pb.start();

        String output = new String(process.getInputStream().readAllBytes());
        String error = new String(process.getErrorStream().readAllBytes());
        int exitCode = process.waitFor();

        // Log detailed results
        if (version != null && step != null) {
            logToFile(version, step, "Command exit code: " + exitCode);
            if (!output.isEmpty()) {
                logToFile(version, step, "STDOUT:\n" + output);
            }
            if (!error.isEmpty()) {
                logToFile(version, step, "STDERR:\n" + error);
            }

            // Also save command output to separate files for long outputs
            if (output.length() > 1000 || error.length() > 1000) {
                try {
                    // Ensure logs directory exists
                    Files.createDirectories(LOGS_DIR);

                    String safeCommand = commandStr.replaceAll("[^a-zA-Z0-9-_]", "_");
                    if (output.length() > 1000) {
                        Path outputFile = LOGS_DIR.resolve(step + "-" + safeCommand + "-output.log");
                        Files.writeString(outputFile, output);
                        logToFile(version, step, "Full output saved to: " + outputFile.getFileName());
                    }
                    if (error.length() > 1000) {
                        Path errorFile = LOGS_DIR.resolve(step + "-" + safeCommand + "-error.log");
                        Files.writeString(errorFile, error);
                        logToFile(version, step, "Full error output saved to: " + errorFile.getFileName());
                    }
                } catch (IOException e) {
                    logWarning("Failed to save detailed command output: " + LOGS_DIR);
                }
            }
        }

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
                ProcessResult result = runCommandSimple("which", tool);
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
            ProcessResult gitStatus = runCommandSimple("git", "status", "--porcelain");
            if (!gitStatus.output.trim().isEmpty()) {
                logError("Working directory not clean");
                System.out.println(gitStatus.output);
                return false;
            }

            // Check branch
            ProcessResult branchResult = runCommandSimple("git", "branch", "--show-current");
            String currentBranch = branchResult.output.trim();
            if (!"master".equals(currentBranch)) {
                logWarning("Not on master branch (currently on: " + currentBranch + ")");
                System.out.print("Do you want to continue with release from branch '" + currentBranch + "'? (y/N): ");
                Scanner scanner = new Scanner(System.in);
                String response = scanner.nextLine();
                if (!response.equalsIgnoreCase("y")) {
                    logError("Release cancelled - not on master branch");
                    return false;
                }
                logInfo("Proceeding with release from branch: " + currentBranch);
            }

            // Check if up to date
            runCommandSimple("git", "fetch", "origin", "master");
            ProcessResult localCommit = runCommandSimple("git", "rev-parse", "HEAD");
            ProcessResult remoteCommit = runCommandSimple("git", "rev-parse", "origin/master");
            
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
            ProcessResult ghAuth = runCommandSimple("gh", "auth", "status");
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
            ProcessResult gpgCheck = runCommandSimple("gpg", "--list-secret-keys", GPG_KEY_ID);
            if (!gpgCheck.isSuccess()) {
                // Try checking if it's a subkey
                ProcessResult subkeyCheck = runCommandSimple("gpg", "--list-secret-keys", "--with-subkey-fingerprints");
                if (!subkeyCheck.output.contains(GPG_KEY_ID)) {
                    logError("GPG key " + GPG_KEY_ID + " not found in secret keyring (neither as primary key nor as subkey)");
                    return false;
                }
                logSuccess("GPG subkey " + GPG_KEY_ID + " found");
            } else {
                logSuccess("GPG key " + GPG_KEY_ID + " found");
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
                if (GMAIL_SENDER_ADDRESS != null && !GMAIL_SENDER_ADDRESS.isEmpty()) {
                    logInfo("Custom sender address: " + GMAIL_SENDER_ADDRESS);
                } else {
                    logInfo("Using Gmail username as sender address: " + GMAIL_USERNAME);
                }
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
                ProcessResult ghAuth = runCommandSimple("gh", "auth", "status");
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
                System.out.println("  export GMAIL_SENDER_ADDRESS=your-sender@domain.org  # optional");
                System.out.println("See: https://support.google.com/accounts/answer/185833");
            } else {
                logSuccess("Gmail credentials are set");
                if (GMAIL_SENDER_ADDRESS != null && !GMAIL_SENDER_ADDRESS.isEmpty()) {
                    logInfo("Custom sender address configured: " + GMAIL_SENDER_ADDRESS);
                }
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
            System.out.println("   export GMAIL_SENDER_ADDRESS=your-sender@domain.org  # optional");
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

        @Option(names = {"-s", "--skip-tests"}, description = "Skip tests during build phase (faster execution)")
        private boolean skipTests = false;

        @Option(names = {"-d", "--skip-dry-run"}, description = "Skip dry-run phase (fastest execution, but riskier)")
        private boolean skipDryRun = false;

        @Override
        public Integer call() {
            System.out.println("🚀 Starting Maven release vote for version " + version);
            System.out.println("📁 Project root: " + PROJECT_ROOT);

            try {
                // Initialize logging
                initializeLogging(version);

                // Check if we're resuming from a previous run
                ReleaseStep currentStep = getCurrentStep(version);
                if (currentStep != ReleaseStep.VALIDATION) {
                    logInfo("Resuming from step: " + currentStep.getStepName());
                    System.out.print("Do you want to resume from step '" + currentStep.getStepName() + "'? (y/N): ");
                    Scanner scanner = new Scanner(System.in);
                    String response = scanner.nextLine();
                    if (!response.equalsIgnoreCase("y")) {
                        logInfo("Starting fresh release process");
                        currentStep = ReleaseStep.VALIDATION;
                    }
                }

                // Step 1: Validation
                if (!isStepCompleted(version, ReleaseStep.VALIDATION)) {
                    saveCurrentStep(version, ReleaseStep.VALIDATION);
                    logStep("Validating environment and credentials...");
                    if (!validateTools() || !validateEnvironment() || !validateCredentials()) {
                        return 1;
                    }
                    if (!validateVersion(version)) {
                        return 1;
                    }
                    logToFile(version, "VALIDATION", "All validations passed");
                } else {
                    logInfo("Skipping validation (already completed)");
                }

                // Step 2: Check for blocker issues
                if (!isStepCompleted(version, ReleaseStep.BLOCKER_CHECK)) {
                    saveCurrentStep(version, ReleaseStep.BLOCKER_CHECK);
                    logStep("Checking for blocker issues...");
                    ProcessResult blockerCheck = runCommandWithLogging(version, "BLOCKER_CHECK", "gh", "issue", "list", "--label", "blocker",
                        "--state", "open", "--json", "number", "--jq", "length");

                    int blockerCount = Integer.parseInt(blockerCheck.output.trim());
                    if (blockerCount > 0) {
                        logWarning("Found " + blockerCount + " open blocker issues");
                        ProcessResult blockerList = runCommandWithLogging(version, "BLOCKER_CHECK", "gh", "issue", "list", "--label", "blocker",
                            "--state", "open", "--json", "number,title", "--jq", ".[] | \"  #\\(.number): \\(.title)\"");
                        System.out.println(blockerList.output);

                        System.out.println();
                        System.out.print("Do you want to continue anyway? (y/N): ");
                        Scanner scanner = new Scanner(System.in);
                        String response = scanner.nextLine();
                        if (!response.equalsIgnoreCase("y")) {
                            logError("Release cancelled due to blocker issues");
                            logToFile(version, "BLOCKER_CHECK", "Release cancelled due to blocker issues");
                            return 1;
                        }
                    }
                    logToFile(version, "BLOCKER_CHECK", "Blocker check completed");
                } else {
                    logInfo("Skipping blocker check (already completed)");
                }

                // Step 3: Get milestone and release notes
                String milestoneInfo = "";
                String releaseNotes = "";
                if (!isStepCompleted(version, ReleaseStep.MILESTONE_INFO)) {
                    saveCurrentStep(version, ReleaseStep.MILESTONE_INFO);
                    logStep("Getting GitHub milestone and release notes...");
                    milestoneInfo = getMilestoneInfo(version);
                    releaseNotes = getReleaseNotes(version);
                    logToFile(version, "MILESTONE_INFO", "Milestone and release notes retrieved");
                } else {
                    logInfo("Skipping milestone info (already completed)");
                    // Load from saved files if available
                    milestoneInfo = loadMilestoneInfo(version);
                }

                // Step 4: Build and test
                if (!isStepCompleted(version, ReleaseStep.BUILD_TEST)) {
                    saveCurrentStep(version, ReleaseStep.BUILD_TEST);
                    if (skipTests) {
                        logInfo("Using --skip-tests option for faster execution");
                    }
                    buildAndTest(version, skipTests);
                } else {
                    logInfo("Skipping build and test (already completed)");
                }

                // Step 5: Site compilation check
                if (!isStepCompleted(version, ReleaseStep.SITE_CHECK)) {
                    saveCurrentStep(version, ReleaseStep.SITE_CHECK);
                    checkSiteCompilation(version);
                } else {
                    logInfo("Skipping site check (already completed)");
                }

                // Step 6: Prepare release
                if (!isStepCompleted(version, ReleaseStep.PREPARE_RELEASE)) {
                    saveCurrentStep(version, ReleaseStep.PREPARE_RELEASE);
                    if (skipDryRun) {
                        logWarning("Using --skip-dry-run option - this is faster but riskier!");
                    }
                    prepareRelease(version, skipDryRun);
                } else {
                    logInfo("Skipping release preparation (already completed)");
                }

                // Step 7: Stage artifacts
                String stagingRepo = "";
                if (!isStepCompleted(version, ReleaseStep.STAGE_ARTIFACTS)) {
                    saveCurrentStep(version, ReleaseStep.STAGE_ARTIFACTS);
                    stagingRepo = stageArtifacts(version);
                    if (stagingRepo == null || stagingRepo.isEmpty()) {
                        logError("Failed to get staging repository ID");
                        return 1;
                    }
                } else {
                    logInfo("Skipping artifact staging (already completed)");
                    stagingRepo = loadStagingRepo(version);
                    if (stagingRepo == null || stagingRepo.isEmpty()) {
                        logError("Could not load staging repository ID from previous run");
                        return 1;
                    }
                }

                // Step 8: Stage documentation
                if (!isStepCompleted(version, ReleaseStep.STAGE_DOCS)) {
                    saveCurrentStep(version, ReleaseStep.STAGE_DOCS);
                    stageDocumentation(version);
                } else {
                    logInfo("Skipping documentation staging (already completed)");
                }

                // Step 9: Copy to dist area
                if (!isStepCompleted(version, ReleaseStep.COPY_DIST)) {
                    saveCurrentStep(version, ReleaseStep.COPY_DIST);
                    copyToDistArea(version);
                } else {
                    logInfo("Skipping dist area copy (already completed)");
                }

                // Step 10: Generate vote email
                if (!isStepCompleted(version, ReleaseStep.GENERATE_EMAIL)) {
                    saveCurrentStep(version, ReleaseStep.GENERATE_EMAIL);
                    generateVoteEmail(version, stagingRepo, milestoneInfo, releaseNotes);
                } else {
                    logInfo("Skipping vote email generation (already completed)");
                }

                // Step 11: Save staging info
                if (!isStepCompleted(version, ReleaseStep.SAVE_INFO)) {
                    saveCurrentStep(version, ReleaseStep.SAVE_INFO);
                    saveStagingInfo(version, stagingRepo, milestoneInfo);
                } else {
                    logInfo("Skipping save staging info (already completed)");
                }

                // Mark as completed
                saveCurrentStep(version, ReleaseStep.COMPLETED);

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
            ProcessResult tagCheck = runCommandSimple("git", "tag", "-l");
            if (tagCheck.output.contains("maven-" + version)) {
                logError("Tag maven-" + version + " already exists");
                return false;
            }

            // Check current version is SNAPSHOT
            ProcessResult versionCheck = runCommandSimple("mvn", "help:evaluate",
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
            ProcessResult result = runCommandSimple("gh", "api", "repos/apache/maven/milestones",
                "--jq", ".[] | select(.title == \"" + version + "\")");

            if (result.output.trim().isEmpty()) {
                // Try partial match
                result = runCommandSimple("gh", "api", "repos/apache/maven/milestones",
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
            ProcessResult result = runCommandSimple("gh", "api", "repos/apache/maven/releases",
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

    static void buildAndTest(String version) throws Exception {
        buildAndTest(version, false);
    }

    static void buildAndTest(String version, boolean skipTests) throws Exception {
        if (skipTests) {
            logStep("Building (skipping tests for faster execution)...");
            ProcessResult result = runCommandWithLogging(version, "BUILD_TEST", "mvn", "clean", "compile", "-Papache-release", "-Dgpg.skip=true");
            if (!result.isSuccess()) {
                logToFile(version, "BUILD_TEST", "Build failed with exit code: " + result.exitCode);
                throw new RuntimeException("Build failed. Check logs at: " + LOGS_DIR.resolve("release-" + version + ".log") +
                    "\nError: " + result.error);
            }
            logSuccess("Build completed (tests skipped)");
            logToFile(version, "BUILD_TEST", "Build completed successfully (tests skipped)");
        } else {
            logStep("Building and testing...");
            ProcessResult result = runCommandWithLogging(version, "BUILD_TEST", "mvn", "clean", "verify", "-Papache-release", "-Dgpg.skip=true");
            if (!result.isSuccess()) {
                logToFile(version, "BUILD_TEST", "Build failed with exit code: " + result.exitCode);
                throw new RuntimeException("Build and test failed. Check logs at: " + LOGS_DIR.resolve("release-" + version + ".log") +
                    "\nError: " + result.error);
            }
            logSuccess("Build and tests completed");
            logToFile(version, "BUILD_TEST", "Build and tests completed successfully");
        }
    }

    static void checkSiteCompilation(String version) throws Exception {
        logStep("Checking site compilation...");
        ProcessResult result = runCommandWithLogging(version, "SITE_CHECK", "mvn", "-Preporting", "site", "site:stage");
        if (!result.isSuccess()) {
            logToFile(version, "SITE_CHECK", "Site compilation failed with exit code: " + result.exitCode);
            throw new RuntimeException("Site compilation failed. Check logs at: " + LOGS_DIR.resolve("release-" + version + ".log") +
                "\nError: " + result.error);
        }
        logSuccess("Site compilation successful");
        logToFile(version, "SITE_CHECK", "Site compilation completed successfully");
    }

    static void prepareRelease(String version) throws Exception {
        prepareRelease(version, false);
    }

    static void prepareRelease(String version, boolean skipDryRun) throws Exception {
        logStep("Preparing release " + version + "...");

        if (!skipDryRun) {
            // Dry run first
            logInfo("Running release:prepare in dry-run mode...");
            ProcessResult dryRun = runCommandWithLogging(version, "PREPARE_RELEASE", "mvn", "release:prepare", "-DdryRun=true",
                "-Dtag=maven-" + version, "-DreleaseVersion=" + version,
                "-DdevelopmentVersion=" + version + "-SNAPSHOT");

            if (!dryRun.isSuccess()) {
                logToFile(version, "PREPARE_RELEASE", "Dry run failed with exit code: " + dryRun.exitCode);
                throw new RuntimeException("Release prepare dry run failed. Check logs at: " + LOGS_DIR.resolve("release-" + version + ".log") +
                    "\nError: " + dryRun.error);
            }

            logInfo("Dry run successful. Proceeding with actual preparation...");
            runCommandWithLogging(version, "PREPARE_RELEASE", "mvn", "release:clean");

            logInfo("Skipping tests during actual release:prepare since dry-run already validated them");
            ProcessResult actual = runCommandWithLogging(version, "PREPARE_RELEASE", "mvn", "release:prepare",
                "-Dtag=maven-" + version, "-DreleaseVersion=" + version,
                "-DdevelopmentVersion=" + version + "-SNAPSHOT",
                "-DskipTests=true");

            if (!actual.isSuccess()) {
                logToFile(version, "PREPARE_RELEASE", "Release prepare failed with exit code: " + actual.exitCode);
                throw new RuntimeException("Release prepare failed. Check logs at: " + LOGS_DIR.resolve("release-" + version + ".log") +
                    "\nError: " + actual.error);
            }
        } else {
            logWarning("Skipping dry-run as requested - proceeding directly to release:prepare");
            logInfo("Running release:prepare with tests (since no dry-run validation was done)");
            ProcessResult actual = runCommandWithLogging(version, "PREPARE_RELEASE", "mvn", "release:prepare",
                "-Dtag=maven-" + version, "-DreleaseVersion=" + version,
                "-DdevelopmentVersion=" + version + "-SNAPSHOT");

            if (!actual.isSuccess()) {
                logToFile(version, "PREPARE_RELEASE", "Release prepare failed with exit code: " + actual.exitCode);
                throw new RuntimeException("Release prepare failed. Check logs at: " + LOGS_DIR.resolve("release-" + version + ".log") +
                    "\nError: " + actual.error);
            }
        }

        logSuccess("Release prepared");
        logToFile(version, "PREPARE_RELEASE", "Release preparation completed successfully");
    }

    static String stageArtifacts(String version) throws Exception {
        logStep("Staging artifacts to Nexus...");
        ProcessResult result = runCommandWithLogging(version, "STAGE_ARTIFACTS", "mvn", "release:perform",
            "-Dgoals=deploy nexus-staging:close",
            "-DstagingDescription=VOTE Maven " + version);

        if (!result.isSuccess()) {
            logToFile(version, "STAGE_ARTIFACTS", "Artifact staging failed with exit code: " + result.exitCode);
            throw new RuntimeException("Artifact staging failed. Check logs at: " + LOGS_DIR.resolve("release-" + version + ".log") +
                "\nError: " + result.error);
        }

        // Get staging repository ID - try multiple methods
        String stagingRepo = findStagingRepository(version);
        if (stagingRepo != null && !stagingRepo.isEmpty()) {
            logSuccess("Artifacts staged to repository: " + stagingRepo);
            logToFile(version, "STAGE_ARTIFACTS", "Artifacts staged to repository: " + stagingRepo);
            return stagingRepo;
        } else {
            logToFile(version, "STAGE_ARTIFACTS", "Could not find staging repository ID using any method");
            throw new RuntimeException("Could not find staging repository ID. Check the release:perform output for manual staging repo identification.");
        }
    }

    static void stageDocumentation(String version) throws Exception {
        logStep("Staging documentation...");

        Path checkoutDir = PROJECT_ROOT.resolve("target/checkout");
        ProcessBuilder pb = new ProcessBuilder("mvn", "scm-publish:publish-scm", "-Preporting");
        pb.directory(checkoutDir.toFile());

        logToFile(version, "STAGE_DOCS", "Executing: mvn scm-publish:publish-scm -Preporting in " + checkoutDir);
        Process process = pb.start();

        String output = new String(process.getInputStream().readAllBytes());
        String error = new String(process.getErrorStream().readAllBytes());
        int exitCode = process.waitFor();

        logToFile(version, "STAGE_DOCS", "Documentation staging exit code: " + exitCode);
        if (!output.isEmpty()) {
            logToFile(version, "STAGE_DOCS", "STDOUT:\n" + output);
        }
        if (!error.isEmpty()) {
            logToFile(version, "STAGE_DOCS", "STDERR:\n" + error);
        }

        if (exitCode != 0) {
            throw new RuntimeException("Documentation staging failed. Check logs at: " + LOGS_DIR.resolve("release-" + version + ".log") +
                "\nError: " + error);
        }

        logSuccess("Documentation staged");
        logToFile(version, "STAGE_DOCS", "Documentation staging completed successfully");
    }

    static String findStagingRepository(String version) {
        logInfo("Searching for staging repository ID...");

        // Method 1: Try nexus-staging plugin if available
        try {
            ProcessResult repoList = runCommandWithLogging(version, "STAGE_ARTIFACTS", "mvn", "nexus-staging:rc-list", "-q");
            if (repoList.isSuccess()) {
                Pattern pattern = Pattern.compile("orgapachemaven-[0-9]+");
                java.util.regex.Matcher matcher = pattern.matcher(repoList.output);
                if (matcher.find()) {
                    String stagingRepo = matcher.group();
                    logToFile(version, "STAGE_ARTIFACTS", "Found staging repo via nexus-staging:rc-list: " + stagingRepo);
                    return stagingRepo;
                }
            }
        } catch (Exception e) {
            logToFile(version, "STAGE_ARTIFACTS", "nexus-staging:rc-list failed: " + e.getMessage());
        }

        // Method 2: Parse the release:perform output for staging repository mentions
        try {
            Path performLog = LOGS_DIR.resolve("STAGE_ARTIFACTS-mvn_release_perform_-Dgoals_deploy_nexus-staging_close_-DstagingDescription_VOTE_Maven_" + version.replace(".", "_").replace("-", "_") + "-output.log");
            if (Files.exists(performLog)) {
                String content = Files.readString(performLog);

                // Look for staging repository patterns in the output
                Pattern[] patterns = {
                    Pattern.compile("Staging repository '(orgapachemaven-[0-9]+)'"),
                    Pattern.compile("stagingRepositoryId=(orgapachemaven-[0-9]+)"),
                    Pattern.compile("Repository ID: (orgapachemaven-[0-9]+)"),
                    Pattern.compile("\\[INFO\\].*?(orgapachemaven-[0-9]+).*?closed")
                };

                for (Pattern pattern : patterns) {
                    java.util.regex.Matcher matcher = pattern.matcher(content);
                    if (matcher.find()) {
                        String stagingRepo = matcher.group(1);
                        logToFile(version, "STAGE_ARTIFACTS", "Found staging repo in release:perform output: " + stagingRepo);
                        return stagingRepo;
                    }
                }
            }
        } catch (Exception e) {
            logToFile(version, "STAGE_ARTIFACTS", "Failed to parse release:perform output: " + e.getMessage());
        }

        // Method 3: Manual prompt as fallback
        logWarning("Could not automatically detect staging repository ID");
        logInfo("Please check the release:perform output manually and look for lines like:");
        logInfo("  'Staging repository 'orgapachemaven-XXXX' created'");
        logInfo("  'Repository ID: orgapachemaven-XXXX'");

        System.out.print("Enter staging repository ID (e.g., orgapachemaven-1234) or press Enter to skip: ");
        Scanner scanner = new Scanner(System.in);
        String manualRepo = scanner.nextLine().trim();

        if (!manualRepo.isEmpty()) {
            logToFile(version, "STAGE_ARTIFACTS", "Using manually entered staging repo: " + manualRepo);
            return manualRepo;
        }

        return null;
    }

    static void copyToDistArea(String version) throws Exception {
        logStep("Copying source release to Apache distribution area...");

        Path sourceZip = PROJECT_ROOT.resolve("target/checkout/target/maven-" + version + "-source-release.zip");
        Path sourceAsc = PROJECT_ROOT.resolve("target/checkout/target/maven-" + version + "-source-release.zip.asc");

        if (!Files.exists(sourceZip) || !Files.exists(sourceAsc)) {
            throw new RuntimeException("Source release files not found");
        }

        // Generate SHA512
        ProcessResult sha512Result = runCommandSimple("sha512sum", sourceZip.toString());
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
            runCommandSimple("svn", "checkout", "https://dist.apache.org/repos/dist/release/maven",
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
        ProcessResult lastTagResult = runCommandSimple("git", "describe", "--tags", "--abbrev=0", "--match=maven-*");
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
                // Simple JSON parsing without ObjectMapper for now
                if (milestoneInfo.contains("\"closed_issues\":")) {
                    String[] parts = milestoneInfo.split("\"closed_issues\":");
                    if (parts.length > 1) {
                        String numberPart = parts[1].split(",")[0].trim();
                        closedIssues = numberPart.replaceAll("[^0-9]", "");
                    }
                }
                if (milestoneInfo.contains("\"html_url\":")) {
                    String[] parts = milestoneInfo.split("\"html_url\":\"");
                    if (parts.length > 1) {
                        String urlPart = parts[1].split("\"")[0];
                        milestoneUrl = urlPart + "?closed=1";
                    }
                }
            } catch (Exception e) {
                logWarning("Failed to parse milestone info: " + e.getMessage());
            }
        }

        // Calculate SHA512
        Path sourceZip = PROJECT_ROOT.resolve("target/checkout/target/maven-" + version + "-source-release.zip");
        String sha512 = "[SHA512 will be calculated]";
        if (Files.exists(sourceZip)) {
            ProcessResult sha512Result = runCommandSimple("sha512sum", sourceZip.toString());
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

            // Determine sender address - use custom sender if configured, otherwise use Gmail username
            String senderAddress = (GMAIL_SENDER_ADDRESS != null && !GMAIL_SENDER_ADDRESS.isEmpty())
                ? GMAIL_SENDER_ADDRESS
                : GMAIL_USERNAME;

            // Create email content with headers
            StringBuilder email = new StringBuilder();
            email.append("To: ").append(to).append("\n");
            if (cc != null && !cc.isEmpty()) {
                email.append("Cc: ").append(cc).append("\n");
            }
            email.append("Subject: ").append(subject).append("\n");
            email.append("From: ").append(senderAddress).append("\n\n");
            email.append(body);

            // Use curl to send via Gmail SMTP
            ProcessResult result = runCommandSimple("curl", "-s", "--url", "smtps://smtp.gmail.com:465",
                "--ssl-reqd", "--mail-from", senderAddress, "--mail-rcpt", to,
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
        ProcessResult result = runCommandSimple("mvn", "nexus-staging:promote",
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

        ProcessResult result = runCommandSimple("svnmucc", "-m", "Publish Maven " + version + " documentation",
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
                // Simple parsing without ObjectMapper
                if (milestoneInfo.contains("\"number\":") && milestoneInfo.contains("\"state\":\"open\"")) {
                    String[] parts = milestoneInfo.split("\"number\":");
                    if (parts.length > 1) {
                        String numberPart = parts[1].split(",")[0].trim();
                        String milestoneNumber = numberPart.replaceAll("[^0-9]", "");

                        String currentDate = java.time.Instant.now().toString();
                        ProcessResult result = runCommandSimple("gh", "api", "repos/apache/maven/milestones/" + milestoneNumber,
                            "--method", "PATCH",
                            "--field", "state=closed",
                            "--field", "due_on=" + currentDate);

                        if (result.isSuccess()) {
                            logSuccess("Milestone closed");
                        }
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
                ProcessResult result = runCommandSimple("gh", "api", "repos/apache/maven/milestones",
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
        ProcessResult draftResult = runCommandSimple("gh", "api", "repos/apache/maven/releases",
            "--jq", ".[] | select(.draft == true and (.tag_name == \"maven-" + version +
            "\" or .tag_name == \"" + version + "\" or .name | contains(\"" + version + "\"))) | .id");

        if (!draftResult.output.trim().isEmpty()) {
            String releaseId = draftResult.output.trim();

            // Update tag if needed and publish
            ProcessResult result = runCommandSimple("gh", "api", "repos/apache/maven/releases/" + releaseId,
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

            ProcessResult result = runCommandSimple("gh", "release", "create", "maven-" + version,
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
            ProcessResult result = runCommandSimple("gh", "release", "view", "maven-" + version,
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

            ProcessResult result = runCommandSimple("mvn", "nexus-staging:drop",
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
            ProcessResult tagCheck = runCommandSimple("git", "tag", "-l");
            if (tagCheck.output.contains("maven-" + version)) {
                logInfo("Removing release tag: maven-" + version);
                runCommandSimple("git", "tag", "-d", "maven-" + version);
            }

            // Clean up release plugin files
            if (Files.exists(PROJECT_ROOT.resolve("pom.xml.releaseBackup"))) {
                logInfo("Cleaning up Maven release plugin files");
                runCommandSimple("mvn", "release:clean");
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

    // Status Command
    @Command(name = "status", description = "Check release status and logs")
    static class StatusCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Release version")
        private String version;

        @Override
        public Integer call() {
            System.out.println("📊 Checking status for Maven release " + version);
            System.out.println("📁 Project root: " + PROJECT_ROOT);

            try {
                // Check if logs directory exists
                if (!Files.exists(LOGS_DIR)) {
                    logWarning("No logs directory found: " + LOGS_DIR);
                    return 0;
                }

                // Check current step
                ReleaseStep currentStep = getCurrentStep(version);
                System.out.println();
                logInfo("Current step: " + currentStep.getStepName());

                // Show step progress
                System.out.println();
                System.out.println("📋 Release Steps Progress:");
                for (ReleaseStep step : ReleaseStep.values()) {
                    if (step == ReleaseStep.COMPLETED) continue;

                    String status;
                    if (isStepCompleted(version, step)) {
                        status = GREEN + "✅ COMPLETED" + NC;
                    } else if (step == currentStep) {
                        status = YELLOW + "🔄 IN PROGRESS" + NC;
                    } else {
                        status = "⏳ PENDING";
                    }
                    System.out.println("  " + step.getStepName() + ": " + status);
                }

                // Check for log files
                Path logFile = LOGS_DIR.resolve("release-" + version + ".log");
                if (Files.exists(logFile)) {
                    System.out.println();
                    logInfo("Main log file: " + logFile);

                    // Show last few log entries
                    try {
                        List<String> lines = Files.readAllLines(logFile);
                        System.out.println();
                        System.out.println("📄 Last 10 log entries:");
                        int start = Math.max(0, lines.size() - 10);
                        for (int i = start; i < lines.size(); i++) {
                            System.out.println("  " + lines.get(i));
                        }
                    } catch (IOException e) {
                        logWarning("Could not read log file: " + e.getMessage());
                    }
                } else {
                    logWarning("No main log file found: " + logFile);
                }

                // List other log files
                try {
                    List<Path> logFiles = Files.list(LOGS_DIR)
                        .filter(p -> p.getFileName().toString().contains(version))
                        .filter(p -> !p.equals(logFile))
                        .sorted()
                        .collect(java.util.stream.Collectors.toList());

                    if (!logFiles.isEmpty()) {
                        System.out.println();
                        System.out.println("📁 Additional log files:");
                        for (Path file : logFiles) {
                            System.out.println("  " + file.getFileName());
                        }
                    }
                } catch (IOException e) {
                    logWarning("Could not list log files: " + e.getMessage());
                }

                // Check for staging info
                String stagingRepo = loadStagingRepo(version);
                if (stagingRepo != null && !stagingRepo.isEmpty()) {
                    System.out.println();
                    logInfo("Staging repository: " + stagingRepo);
                }

                // Show helpful commands
                System.out.println();
                System.out.println("🔧 Helpful commands:");
                System.out.println("  View full log: cat " + logFile);
                System.out.println("  View logs directory: ls -la " + LOGS_DIR);
                if (currentStep != ReleaseStep.COMPLETED) {
                    System.out.println("  Resume release: jbang " + MavenRelease.class.getSimpleName() + ".java start-vote " + version);
                }
                System.out.println("  Cancel release: jbang " + MavenRelease.class.getSimpleName() + ".java cancel " + version);

                return 0;

            } catch (Exception e) {
                logError("Failed to check status: " + e.getMessage());
                e.printStackTrace();
                return 1;
            }
        }
    }
}
