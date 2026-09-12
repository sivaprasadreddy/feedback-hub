---
name: update-usecase
description: Update one documented UC-### use case so its description, acceptance criteria, dependencies, and status accurately reflect the current implementation. Use after human or ad hoc code changes fill gaps, alter behavior, or correct an implementation; do not use to implement code or draft new product scope.
disable-model-invocation: true
---

# Update Use Case

Synchronize one use case with the behavior demonstrably present in the current worktree while preserving the PRD's product intent.

## Input and boundaries

- Require exactly one use-case ID in `UC-###` form. Match case-insensitively and preserve the document's canonical ID.
- Honor a user-supplied requirements path; otherwise use `docs/requirements.md`. Use `docs/prd.md` as the default product-intent reference when it exists.
- If the ID is missing or unknown, multiple IDs were supplied, or the summary and detailed section disagree, ask the user to resolve the issue before editing.
- Update documentation only. Do not change production code, tests, migrations, or configuration under this skill.
- Do not update other use cases merely because related code was discovered. Report those candidates separately.

## Establish the actual behavior

Read the selected use case, its direct dependencies, and the relevant PRD passages. Inspect the current worktree, including uncommitted changes, and trace the complete behavior through applicable entry points, services, persistence, authorization, error handling, configuration, migrations, and automated tests.

Base claims on concrete evidence. A code path alone may show an implementation exists, but passing behavior-focused tests provide stronger evidence that an acceptance criterion is complete. Run focused relevant tests when practical; testing is read-only verification, not authorization to alter test code.

Compare the evidence with every existing acceptance criterion and identify:

- behavior that is fully implemented and verified;
- partial or missing behavior;
- observable behavior implemented by recent or current code but absent from the use case;
- documented behavior that the implementation intentionally replaced or corrected;
- contradictions between the implementation, tests, requirements, and PRD.

## Resolve ambiguity before editing

Do not assume every implementation detail is intended product behavior. Ask the user concise, decision-oriented questions before modifying `requirements.md` when:

- code contradicts the PRD or removes a documented guarantee;
- code and tests disagree;
- a behavior may be an incidental implementation detail or defect;
- evidence does not reveal which behavior is authoritative;
- a change would materially expand scope, alter actors or authorization, or affect another use case.

Wait for answers before editing. If the implementation clearly fills a documentation gap without changing product intent, proceed without unnecessary questions.

## Update the use case

Change only fields supported by the evidence:

- Keep the existing use-case ID stable.
- Revise the title only when the established responsibility changed and the new title is necessary for accuracy.
- Revise `Depends On` only for direct behavioral prerequisites, using existing canonical IDs.
- Describe the actor, intent, and observable outcome rather than internal classes or algorithms.
- Add, remove, or revise acceptance criteria so each is atomic, testable, and reflects intentional observable behavior. Do not turn framework choices or incidental internals into product requirements.
- Preserve valid requirements that remain part of product intent even when implementation is incomplete.

Set status consistently in both the summary row and detailed section:

- `NOT_IMPLEMENTED` when no meaningful part of the use case exists.
- `IN_PROGRESS` when behavior is partial, criteria remain unmet, relevant verification fails, or completion cannot be established.
- `COMPLETED` only when all updated acceptance criteria are implemented and relevant verification passes.

Keep the summary and detailed sections identical for ID, title, and status. Do not change any other use-case status.

## Verify and report

Review the documentation diff against the inspected implementation and PRD. Confirm that all claims have evidence, dependencies resolve, Markdown structure remains valid, and no unrelated content changed.

Report the updated fields and status, the implementation and tests used as evidence, verification results, and any related use cases that may need a separate update. If no documentation change is needed, say so and leave the file untouched.
