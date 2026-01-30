Context:
This code is part of a production system.

Goal:

- Simplify the existing logic
- Remove redundant or duplicated code
- Improve readability without changing behavior

Hard Constraints (must not be violated):

- Do NOT change public method signatures
- Do NOT modify API contracts
- Do NOT change business logic or side effects
- Do NOT introduce new dependencies
- Do NOT rename variables or methods unless strictly necessary

Allowed:

- Extract private helper methods
- Remove dead code
- Reduce nested conditionals

Non-goals:

- No performance optimization
- No architectural changes

Output:

- Provide the updated code only