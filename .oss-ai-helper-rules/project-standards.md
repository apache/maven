# Project Standards

This rule file contains build tools, commands, and code style constraints for the project. Commands read this file to determine how to build, test, and format code.

- **Build tool:** Maven
- **Build command:** `mvn verify`
- **Test command:** `mvn test`
- **Format command:** _(none configured)_
- **Module-specific build:** yes (multi-module Maven project)
- **Parallelized Maven:** no
- **Code style restrictions:**
  - Only use spaces for indentation
  - Create minimal diffs — avoid reformatting or reorganizing imports
  - Check for unnecessary whitespace with `git diff --check` before committing

## Version
2ca89b220c71785fca0b66ae9fcb78141aa0e488
