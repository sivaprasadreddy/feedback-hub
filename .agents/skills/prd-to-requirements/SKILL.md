---
name: prd-to-requirements
description: Generate or regenerate a requirements.md use-case specification from a product requirements document (prd.md). Use when translating a PRD into implementable use cases, dependencies, statuses, and testable acceptance criteria; do not use for implementation or implementation-status audits.
disable-model-invocation: true
---

# PRD to Requirements

Create `requirements.md` from `prd.md`, treating the PRD as the source of truth and the repository's existing requirements document as the format reference.

## Inputs and output

- Honor paths supplied by the user. Otherwise use `docs/prd.md` as input and `docs/requirements.md` as output.
- Before writing, read the entire PRD and, when present, the entire existing `docs/requirements.md` to learn the repository's current format and conventions.
- Treat the existing requirements document as a structural example, not as authority for product scope. Resolve content against the PRD.
- Write only the requested requirements file. Do not implement features or modify the PRD.

## Derive the requirements

Translate every explicit, in-scope capability in the PRD into an independently implementable use case. 
Include actors, authorization boundaries, validation rules, state transitions, privacy constraints, failure behavior, ordering, and other observable business rules stated by the PRD.

- Exclude items explicitly marked optional, future, later, or out of scope.
- Do not invent product behavior to fill gaps. Preserve meaningful ambiguity in neutral language or call it out briefly in the document when it prevents a testable requirement.
- Split capabilities when they can be implemented or verified independently; combine duplicate statements that describe the same behavior.
- Order foundational use cases before dependent ones and assign stable sequential IDs as `UC-001`, `UC-002`, and so on.
- Set each newly derived use case to `NOT_IMPLEMENTED`. Do not infer implementation status from the codebase unless the user explicitly asks for a status audit. Possible values of status are `NOT_IMPLEMENTED`, `IN_PROGRESS`, `COMPLETED`. 
- List only direct logical prerequisites in `Depends On`; use `None` when there are none. Never reference an undefined use-case ID.
- Make acceptance criteria atomic, observable, and testable. Cover both success and relevant rejection or boundary behavior stated by the PRD.

## Required document shape

1. `# <Product Name> Requirements`
2. A short introduction linking to the actual PRD path, explaining scope exclusions and status meaning.
3. `## Use Case Summary` containing a Markdown table with `UseCase ID`, `Use Case Title`, and `Status` columns.
4. `## Detailed Use Cases` containing one section per summary row:

```markdown
### UC-001 — Concise Verb-Led Title

- **Status:** NOT_IMPLEMENTED
- **Depends On:** None
- **Description:** One concise paragraph describing the actor, intent, and outcome.
- **Acceptance Criteria:**
  - An observable, testable condition.
```

Keep summary and detail titles, IDs, order, and statuses identical. Use repository-relative Markdown links with the source file's real filename and casing.

## Verify before finishing

Confirm that every in-scope PRD capability is represented, excluded scope was not promoted into requirements, all dependencies resolve, IDs are unique and sequential, summary and detail sections agree, and each acceptance criterion is supported by the PRD.
