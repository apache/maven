# Maven Release Script

This directory contains a JBang-based script to automate the Apache Maven release process, providing a "2-click" release workflow while maintaining security and following Apache procedures.

## Implementation

- **`Release.java`** - JBang-based release automation script
- Modern Java implementation with proper dependency management
- Uses Picocli for professional command-line interface
- Full IDE support and debugging capabilities
- Familiar to Maven developers who work with Java daily

## Quick Start

```bash
# Install JBang if not already installed
curl -Ls https://sh.jbang.dev | bash -s - app setup

# Run the release script
jbang src/scripts/Release.java setup
jbang src/scripts/Release.java start-vote 4.0.0-rc-4
jbang src/scripts/Release.java publish 4.0.0-rc-4
```

## Overview

The release process is managed with two main commands:

1. **Start Release Vote** - Prepares and stages the release, then generates vote email
2. **Publish Release** - After successful vote, publishes the release

## Prerequisites

### Required Tools
- JBang (for running the script)
- Maven 3.3.9+
- Java 17+ (for building Maven 4.x)
- GPG (for signing)
- Subversion (for Apache dist area)
- GitHub CLI (`gh`)
- `jq` (for JSON processing)

### Required Permissions
- Apache committer access
- Maven PMC membership (for some operations)
- Apache Nexus staging permissions
- Apache SVN commit access to dist area

### Environment Setup

1. **Install GitHub CLI and authenticate:**
   ```bash
   # Install gh: https://cli.github.com/
   gh auth login
   ```

2. **Set environment variables:**
   ```bash
   export APACHE_USERNAME="your-apache-id"
   export GPG_KEY_ID="your-gpg-key-id"

   # Optional: For automatic email sending
   export GMAIL_USERNAME="your-email@gmail.com"
   export GMAIL_APP_PASSWORD="your-app-password"
   ```

3. **Configure Maven settings:**
   - Set up `~/.m2/settings.xml` with Apache credentials
   - See: https://maven.apache.org/developers/release/maven-project-release-procedure.html

4. **Configure Gmail (Optional - for automatic email sending):**
   - Enable 2-factor authentication on your Gmail account
   - Generate an app password: https://support.google.com/accounts/answer/185833
   - Set environment variables with your Gmail address and app password

5. **Run setup script:**
   ```bash
   jbang src/scripts/Release.java setup
   ```

## Usage

### Starting a Release Vote

```bash
jbang src/scripts/Release.java start-vote 4.0.0-rc-4
```

This script will:
- Validate release readiness
- Build and test the project
- Prepare the release using Maven release plugin
- Stage artifacts to Apache Nexus
- Stage documentation
- Copy source release to Apache dist area (staged, not committed)
- Generate vote email with GitHub milestone and release notes

**Output:**
- `vote-email-<version>.txt` - Email to send to dev@maven.apache.org
- Staged artifacts in Nexus
- Staged documentation
- Source release staged in Apache dist area
- Staging info saved in `target/staging-repo-<version>` and `target/milestone-info-<version>`

### Publishing a Release (After Successful Vote)

```bash
jbang src/scripts/Release.java publish 4.0.0-rc-4
```

This script will:
- Validate vote results (interactive prompts)
- Promote staging repository to Maven Central
- Commit source release to Apache dist area
- Deploy versioned website documentation
- Close GitHub milestone and create next one
- Publish GitHub release from draft
- Generate announcement email

**Output:**
- `announcement-<version>.txt` - Email to send for announcement
- Published artifacts in Maven Central
- Published documentation
- GitHub release published
- Optional: Automatic email sending via Gmail

### Cancelling a Release Vote

```bash
jbang src/scripts/Release.java cancel 4.0.0-rc-4
```

This command will:
- Prompt for cancellation reason
- Generate and optionally send cancel email to dev@maven.apache.org
- Drop the staging repository from Nexus
- Clean up staged files from Apache dist area
- Remove Git release tags and Maven release plugin files
- Clean up staging info files

**Output:**
- `cancel-email-<version>.txt` - Email to send for cancellation
- All staging artifacts and files removed

## Commands

### Available Commands

- **`setup`** - One-time environment setup and validation
- **`start-vote <version>`** - Start release vote (Click 1)
- **`publish <version> [staging-repo-id]`** - Publish release after vote (Click 2)
- **`cancel <version>`** - Cancel release vote and clean up
- **`help`** - Show help information

### Command Details

All functionality is contained within the single `release.sh` script. The script automatically handles:

- Environment validation
- GitHub milestone and release notes integration
- Maven release preparation and staging
- Apache distribution area management
- Website deployment
- GitHub release publishing
- Email generation and sending (via Gmail)

## GitHub Integration

The scripts are designed to work with:

- **GitHub Issues** (instead of JIRA)
- **GitHub Milestones** for tracking releases
- **Release Drafter** for generating release notes
- **GitHub Releases** for publishing releases

### Milestone Management

- Scripts automatically find milestones by version number
- Closed milestones show resolved issues count
- New milestones are created for next version
- Supports both exact matches and partial matches

### Release Notes

- Release notes are extracted from GitHub release drafts
- Release Drafter should be configured to maintain draft releases
- Manual release notes can be added to drafts before starting vote

### Email Automation

The script can automatically send emails via Gmail SMTP:

- **Vote emails** are sent to `dev@maven.apache.org`
- **Announcement emails** are sent to appropriate lists based on release type:
  - RC releases: `announce@maven.apache.org`, `users@maven.apache.org`
  - Final releases: `announce@apache.org`, `announce@maven.apache.org`, `users@maven.apache.org`
- **Gmail setup required:**
  - Enable 2-factor authentication
  - Generate app password: https://support.google.com/accounts/answer/185833
  - Set `GMAIL_USERNAME` and `GMAIL_APP_PASSWORD` environment variables
- **Fallback:** If Gmail not configured, emails are generated as files for manual sending

## JBang Benefits

- **Familiar to Java developers** - Uses Java syntax and libraries Maven developers know
- **Better IDE support** - Full IntelliJ/Eclipse support with debugging, refactoring, etc.
- **Dependency management** - Automatic dependency resolution via Maven coordinates
- **Type safety** - Compile-time error checking and better error messages
- **Modern CLI** - Uses Picocli for professional command-line interface
- **JSON handling** - Native Jackson support for GitHub API responses
- **HTTP client** - Modern Apache HttpClient for email sending
- **Maintainability** - Easier to extend and modify for Java developers

### Staging Repository Management

- Staging repository ID is automatically saved to `target/staging-repo-<version>`
- Milestone info is saved to `target/milestone-info-<version>`
- Files persist across Maven builds (target directory)
- `publish` command automatically finds staging repo ID
- Can still provide staging repo ID as argument if files are lost
- `cancel` command automatically finds and drops staging repository

## Security

- All credentials stay local (no shared secrets)
- Personal GPG keys used for signing
- Personal Apache credentials for staging/publishing
- GitHub CLI handles authentication securely

## Workflow Example

```bash
# One-time setup
export APACHE_USERNAME="myapacheid"
export GPG_KEY_ID="ABCD1234"
export GMAIL_USERNAME="myemail@gmail.com"  # Optional
export GMAIL_APP_PASSWORD="myapppassword"  # Optional

# Release workflow
jbang src/scripts/Release.java setup
jbang src/scripts/Release.java start-vote 4.0.0-rc-4
# Wait 72+ hours for vote results...
jbang src/scripts/Release.java publish 4.0.0-rc-4
# OR cancel if issues found: jbang src/scripts/Release.java cancel 4.0.0-rc-4
```

## Troubleshooting

### Common Issues

1. **GPG signing fails**
   - Ensure GPG_KEY_ID is set correctly
   - Check GPG key is in secret keyring: `gpg --list-secret-keys`

2. **Nexus staging fails**
   - Check Maven settings.xml has correct Apache credentials
   - Verify Nexus permissions

3. **SVN commit fails**
   - Ensure Apache SVN credentials are configured
   - Check SVN client is authenticated

4. **GitHub CLI issues**
   - Re-authenticate: `gh auth login`
   - Check repository access permissions

### Manual Recovery

If the script fails partway through:

1. **After start-vote fails:**
   - Clean up with: `mvn release:clean`
   - Drop staging repository in Nexus UI
   - Remove any staged files from dist area
   - Re-run: `jbang src/scripts/Release.java start-vote <version>`

2. **After publish fails:**
   - Check what steps completed successfully
   - Manual steps can be performed via GitHub UI
   - Re-run: `jbang src/scripts/Release.java publish <version> <staging-repo-id>`

## Customization

Scripts can be customized for different Apache projects by:

- Updating repository URLs
- Modifying email templates
- Adjusting milestone naming conventions
- Changing documentation deployment paths

## Contributing

When modifying these scripts:

1. Test thoroughly with dry-run modes where available
2. Follow Apache license headers
3. Update this README for any new features
4. Consider backward compatibility
