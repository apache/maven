<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
# Auto Port System

This document describes the automated porting system for Maven that helps maintain fixes between the `maven-4.0.x` branch (for 4.0.x releases) and the `master` branch (for 4.1.0 development).

## Overview

The auto-port system automatically creates cherry-pick PRs to port changes between branches when:
1. Labels are applied to PRs
2. PRs with port labels are merged
3. Comment commands are used

## Labels

### `backport-to-4.0.x`
- **Purpose**: Backport changes from `master` to `maven-4.0.x`
- **Usage**: Apply this label to PRs targeting `master` that should be backported to the 4.0.x release branch
- **Color**: Blue (#0052cc)

### `forward-port-to-master`
- **Purpose**: Forward-port changes from `maven-4.0.x` to `master`
- **Usage**: Apply this label to PRs targeting `maven-4.0.x` that should be forward-ported to master
- **Color**: Green (#0e8a16)

### `auto-port`
- **Purpose**: Identifies automatically created port PRs
- **Usage**: Automatically applied by the system, do not apply manually
- **Color**: Light orange (#f9d0c4)

## Comment Commands

You can trigger porting actions by commenting on PRs with these commands:

### `/backport [target-branch]`
- **Purpose**: Create a backport to the specified branch (defaults to `maven-4.0.x`)
- **Examples**:
  - `/backport` - Backports to `maven-4.0.x`
  - `/backport maven-4.0.x` - Backports to `maven-4.0.x`
- **Permissions**: Requires write access to the repository

### `/forward-port [target-branch]`
- **Purpose**: Create a forward-port to the specified branch (defaults to `master`)
- **Examples**:
  - `/forward-port` - Forward-ports to `master`
  - `/forward-port master` - Forward-ports to `master`
- **Permissions**: Requires write access to the repository

### `/recreate-port`
- **Purpose**: Recreate an existing port PR (useful when conflicts need to be resolved)
- **Usage**: Comment on the original PR to recreate the port
- **Permissions**: Requires write access to the repository

## How It Works

### Automatic Triggering
1. **Label Application**: When you apply a port label to a PR, the system immediately attempts to create the port
2. **PR Merge**: When a PR with a port label is merged, the system creates the port PR
3. **Comment Commands**: When you use a comment command, the system processes it immediately

### Branch Creation
- **Backport branches**: `backport/pr-{number}-to-4.0.x`
- **Forward-port branches**: `forward-port/pr-{number}-to-master`

### Cherry-pick Process
1. The system creates a new branch from the target branch
2. Cherry-picks the merge commit using `git cherry-pick -x` for traceability
3. If conflicts occur, creates a draft PR with conflict markers
4. Links the original and port PRs with comments

### Conflict Handling
When cherry-pick conflicts occur:
- A draft PR is created with conflict markers
- The original PR receives a comment explaining the situation
- Manual resolution is required in the port PR
- Once conflicts are resolved, the draft status can be removed

## Examples

### Scenario 1: Backporting a Bug Fix
1. Create a PR targeting `master` with a bug fix
2. Add the `backport-to-4.0.x` label
3. The system automatically creates a backport PR to `maven-4.0.x`
4. Review and merge both PRs

### Scenario 2: Forward-porting a Feature
1. Create a PR targeting `maven-4.0.x` with a new feature
2. Add the `forward-port-to-master` label
3. The system automatically creates a forward-port PR to `master`
4. Review and merge both PRs

### Scenario 3: Manual Port Command
1. On an existing merged PR, comment `/backport`
2. The system creates a backport PR to `maven-4.0.x`
3. Review and merge the port PR

### Scenario 4: Resolving Conflicts
1. A port PR is created but has conflicts (marked as draft)
2. Check out the port branch locally
3. Resolve conflicts and push changes
4. Convert from draft to ready for review
5. Merge the port PR

## Best Practices

### When to Use Backports
- Critical bug fixes that affect 4.0.x users
- Security fixes
- Documentation improvements
- Small, safe improvements

### When to Use Forward-ports
- Features developed in 4.0.x that should be in 4.1.0
- Bug fixes made directly to 4.0.x
- Configuration or build improvements

### Avoiding Conflicts
- Keep changes small and focused
- Avoid large refactoring in port candidates
- Test ports in feature branches when unsure
- Consider manual porting for complex changes

## Troubleshooting

### Port PR Not Created
- Check that the original PR is merged
- Verify you have the correct labels applied
- Ensure the target branch exists
- Check GitHub Actions logs for errors

### Cherry-pick Conflicts
- Review the draft PR created by the system
- Clone the repository and check out the port branch
- Resolve conflicts manually
- Push changes and convert from draft

### Permission Errors
- Ensure you have write access to the repository
- Comment commands require collaborator permissions
- Contact repository maintainers if needed

## Technical Details

### Workflow File
The auto-port system is implemented in `.github/workflows/auto-port.yml`

### Permissions Required
- `contents: write` - For creating branches and commits
- `pull-requests: write` - For creating and updating PRs
- `issues: write` - For adding labels and comments

### Branch Protection
The system respects branch protection rules and will fail if:
- Target branches require reviews
- Status checks are required
- Push restrictions are in place

For questions or issues with the auto-port system, please create an issue or contact the maintainers.
