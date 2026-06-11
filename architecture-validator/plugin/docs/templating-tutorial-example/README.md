# Templating Tutorial Example Source

This directory accompanies `docs/templating-tutorial.md`.
It shows the end-state source files that would exist after implementing the tutorial's layered built-in template support.

Important scope note:

- This is an illustrative source snapshot for the tutorial.
- It is not a second runnable plugin project.
- It focuses only on the files that change when the plugin moves from one built-in template to two built-in templates.

The intended end state is:

- `hexagonal` remains the built-in template for port-and-adapter style modules.
- `layered` becomes a second built-in template for traditional layered modules.
- A multi-module build can choose either template per subproject.

The files in this directory cover that outcome:

- `src/main/java/.../ArchitectureValidatorExtension.java`
- `src/main/java/.../LayeredArchitectureExtension.java`
- `src/main/java/.../ArchitectureValidatorPlugin.java`
- `src/main/java/.../GenerateArchitectureTestsTask.java`
- `src/main/resources/templates/LayeredArchitectureTest.java.template`
- `src/test/java/.../GenerateArchitectureTestsTaskTest.java`

Read these files alongside the tutorial when you want to see the final shape instead of the incremental steps.