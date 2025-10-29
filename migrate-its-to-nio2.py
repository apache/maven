#!/usr/bin/env python3
"""
Script to migrate Maven Integration Tests from File to Path API.
This script performs automated migration of common patterns.
"""

import os
import re
import sys
from pathlib import Path

def migrate_file_content(content):
    """Migrate file content from File to Path API."""
    
    # Track if we need to add Path import
    needs_path_import = False
    needs_paths_import = False
    
    # Pattern 1: File testDir = extractResources(...) -> Path testDir = extractResourcesAsPath(...)
    pattern1 = r'File\s+(\w+)\s*=\s*extractResources\s*\('
    if re.search(pattern1, content):
        content = re.sub(pattern1, r'Path \1 = extractResourcesAsPath(', content)
        needs_path_import = True
    
    # Pattern 2: new File(testDir, "subdir") -> testDir.resolve("subdir")
    pattern2 = r'new\s+File\s*\(\s*(\w+)\s*,\s*([^)]+)\)'
    matches = re.findall(pattern2, content)
    for match in matches:
        var_name, path_arg = match
        # Only replace if the variable looks like a directory variable
        if any(name in var_name.lower() for name in ['dir', 'path', 'base']):
            old_expr = f'new File({var_name}, {path_arg})'
            new_expr = f'{var_name}.resolve({path_arg})'
            content = content.replace(old_expr, new_expr)
            needs_path_import = True

    # Pattern 2b: Fix File variables that are assigned from Path.resolve()
    pattern2b = r'File\s+(\w+)\s*=\s*(\w+)\.resolve\('
    if re.search(pattern2b, content):
        content = re.sub(pattern2b, r'Path \1 = \2.resolve(', content)
        needs_path_import = True
    
    # Pattern 3: testDir.getAbsolutePath() -> testDir.toString()
    pattern3 = r'(\w+)\.getAbsolutePath\(\)'
    matches = re.findall(pattern3, content)
    for var_name in matches:
        # Only replace if the variable looks like a Path variable
        if any(name in var_name.lower() for name in ['dir', 'path', 'base']):
            old_expr = f'{var_name}.getAbsolutePath()'
            new_expr = f'{var_name}.toString()'
            content = content.replace(old_expr, new_expr)

    # Pattern 3b: Fix .toString() calls on variables that should be Path but are still File
    pattern3b = r'(\w+)\.toString\(\)'
    matches = re.findall(pattern3b, content)
    for var_name in matches:
        # Check if this variable is declared as File but used with Path operations
        if any(name in var_name.lower() for name in ['dir', 'path', 'base']):
            # Look for File declaration and replace with Path
            file_decl_pattern = f'File\\s+{re.escape(var_name)}\\s*='
            if re.search(file_decl_pattern, content):
                content = re.sub(file_decl_pattern, f'Path {var_name} =', content)
                needs_path_import = True
    
    # Pattern 4: File -> Path in variable declarations that use extractResourcesAsPath
    pattern4 = r'File\s+(\w+)\s*=\s*extractResourcesAsPath\s*\('
    if re.search(pattern4, content):
        content = re.sub(pattern4, r'Path \1 = extractResourcesAsPath(', content)
        needs_path_import = True

    # Pattern 5: extractResources(...).toPath() -> extractResourcesAsPath(...)
    pattern5 = r'extractResources\s*\([^)]+\)\.toPath\(\)'
    if re.search(pattern5, content):
        content = re.sub(r'extractResources\s*\(([^)]+)\)\.toPath\(\)', r'extractResourcesAsPath(\1)', content)
        needs_path_import = True
    
    # Add imports if needed
    if needs_path_import and 'import java.nio.file.Path;' not in content:
        # Find the import section and add Path import
        import_pattern = r'(import\s+java\.io\.File;)'
        if re.search(import_pattern, content):
            content = re.sub(import_pattern, r'\1\nimport java.nio.file.Path;', content)
        else:
            # Add after other java.io imports
            io_import_pattern = r'(import\s+java\.io\.[^;]+;)'
            matches = list(re.finditer(io_import_pattern, content))
            if matches:
                last_match = matches[-1]
                insert_pos = last_match.end()
                content = content[:insert_pos] + '\nimport java.nio.file.Path;' + content[insert_pos:]
    
    if needs_paths_import and 'import java.nio.file.Paths;' not in content:
        # Add Paths import if needed (though we're not using it in this migration)
        pass
    
    return content

def migrate_file(file_path):
    """Migrate a single Java file."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            original_content = f.read()
        
        # Skip files that don't extend AbstractMavenIntegrationTestCase
        if 'extends AbstractMavenIntegrationTestCase' not in original_content:
            return False
        
        # Skip files that don't use extractResources
        if 'extractResources(' not in original_content:
            return False
        
        migrated_content = migrate_file_content(original_content)
        
        # Only write if content changed
        if migrated_content != original_content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(migrated_content)
            print(f"Migrated: {file_path}")
            return True
        else:
            return False
            
    except Exception as e:
        print(f"Error processing {file_path}: {e}")
        return False

def main():
    """Main migration function."""
    if len(sys.argv) > 1:
        base_dir = Path(sys.argv[1])
    else:
        base_dir = Path('its/core-it-suite/src/test/java/org/apache/maven/it')
    
    if not base_dir.exists():
        print(f"Directory {base_dir} does not exist")
        return 1
    
    java_files = list(base_dir.glob('*.java'))
    migrated_count = 0
    
    for java_file in java_files:
        if migrate_file(java_file):
            migrated_count += 1
    
    print(f"Migration complete. Migrated {migrated_count} files out of {len(java_files)} total files.")
    return 0

if __name__ == '__main__':
    sys.exit(main())
