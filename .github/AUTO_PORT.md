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

The auto-port system automatically creates and updates cherry-pick PRs to port changes between branches when:
1. PRs with port labels are opened, updated, or merged
2. Labels are added or removed from PRs

This system uses only built-in GitHub Actions to comply with Apache Software Foundation policies that prohibit external actions.

**Key Feature**: Backport/forward-port PRs are created and updated automatically as you work on the original PR, even before it's merged!

## Labels

### `backport-to-4.0.x`
- **Purpose**: Backport changes from `master` to `maven-4.0.x`
- **Usage**: Apply this label to PRs targeting `master` that should be backported to the 4.0.x release branch
- **Color**: Blue (#0052cc)
- **Trigger**: When a PR with this label is opened, updated, labeled, or merged

### `forward-port-to-master`
- **Purpose**: Forward-port changes from `maven-4.0.x` to `master`
- **Usage**: Apply this label to PRs targeting `maven-4.0.x` that should be forward-ported to master
- **Color**: Green (#0e8a16)
- **Trigger**: When a PR with this label is opened, updated, labeled, or merged

### `auto-port`
- **Purpose**: Identifies automatically created port PRs
- **Usage**: Automatically applied by the system, do not apply manually
- **Color**: Light orange (#f9d0c4)

## Automatic Operation

The system works automatically based on labels - no manual commands needed!

### How It Works
1. **Add a Label**: Apply `backport-to-4.0.x` or `forward-port-to-master` to your PR
2. **Automatic Creation**: A port PR is created immediately
3. **Automatic Updates**: Every time you push commits to your original PR, the port PR is updated
4. **Conflict Handling**: If conflicts occur, the port PR becomes a draft with clear instructions

## How It Works

### Automatic Triggering
1. **PR Events**: When a PR with a port label is opened, updated, labeled, or merged
2. **Real-time Updates**: Port PRs are created and updated automatically as you work

### Branch Creation and Updates
The system automatically creates and updates branches with the pattern:
- **Backport branches**: `backport-{pr-number}-to-maven-4.0.x`
- **Forward-port branches**: `backport-{pr-number}-to-master`

When you update your original PR, the existing port branch is deleted and recreated with the latest commits.

### Cherry-pick Process
The auto-port system handles the cherry-picking:
1. Creates a new branch from the target branch
2. Cherry-picks commits using `git cherry-pick -x` for traceability
3. Works with commits from the PR branch directly (no merge commit needed)
4. Creates a pull request with proper title and description
5. Updates the port PR whenever the original PR changes

### Conflict Handling
When cherry-pick conflicts occur:
- A **draft PR** is created with the first conflict committed
- Clear instructions are provided on how to resolve conflicts
- The original PR receives a comment with the conflict status
- Manual resolution is required to complete the port

## Examples

### Scenario 1: Backporting a Bug Fix
1. Create a PR targeting `master` with a bug fix
2. Add the `backport-to-4.0.x` label
3. **Immediately**: A backport PR to `maven-4.0.x` is created
4. Make additional commits to your original PR
5. **Automatically**: The backport PR is updated with your new commits
6. Review and merge both PRs when ready

### Scenario 2: Forward-porting a Feature
1. Create a PR targeting `maven-4.0.x` with a new feature
2. Add the `forward-port-to-master` label
3. **Immediately**: A forward-port PR to `master` is created
4. Continue developing in your original PR
5. **Automatically**: The forward-port PR stays in sync
6. Review and merge both PRs when ready

### Scenario 3: Resolving Conflicts
1. A port PR is created but has conflicts (marked as draft)
2. Check out the port branch locally
3. Resolve conflicts and push changes
4. Convert from draft to ready for review
5. Merge the port PR

### Scenario 4: Adding Labels Later
1. Create a PR without port labels
2. Later, add the `backport-to-4.0.x` label
3. **Immediately**: A backport PR is created with all current commits
4. Continue working normally

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

### Implementation
- **Workflow File**: `.github/workflows/auto-port.yml`
- **Actions Used**: Built-in GitHub Actions only (`actions/checkout@v4`, `actions/github-script@v7`)
- **Trigger**: `pull_request_target` (for opened, synchronize, reopened, closed, labeled, unlabeled events)

### Permissions Required
- `contents: write` - For creating branches and commits
- `pull-requests: write` - For creating and updating PRs
- `issues: write` - For adding labels and comments

### Security
- Uses `pull_request_target` for secure handling of forks
- Permission checks for comment commands
- Respects branch protection rules

### Advantages of Custom Implementation
- **ASF Compliant**: Uses only built-in GitHub Actions as required by Apache Software Foundation
- **Real-time Updates**: Port PRs are created and updated as you work, not just when merged
- **Transparent**: All logic is visible in the workflow file
- **Flexible**: Supports conflict resolution with draft PRs
- **Secure**: No external dependencies or third-party actions
- **Developer Friendly**: See port results immediately, catch conflicts early

For questions or issues with the auto-port system, please create an issue or contact the maintainers.
