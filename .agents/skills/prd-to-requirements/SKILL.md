---
name: prd-to-requirements
description: Generate or regenerate a requirements.md use-case summary from a product requirements document (PRD.md). Use when translating a PRD into implementable use cases and statuses; do not use for implementation or implementation-status audits.
disable-model-invocation: true
---

# PRD to Requirements

Create `requirements.md` from `prd.md`, treating the PRD as the source of truth and the repository's existing requirements document as the format reference.

## Inputs and output

- Honor paths supplied by the user. Otherwise, use `../../../docs/PRD.md` as input and `docs/requirements.md` as output.
- Before writing, read the entire PRD and, when present, the entire existing `docs/requirements.md` to learn the repository's current format and conventions.
- Treat the existing requirements document as a structural example, not as authority for product scope. Resolve content against the PRD.
- Write only the requested requirements file. Do not implement features or modify the PRD.

## Derive the requirements

- Identify every explicit, in-scope capability in the PRD and represent it as a summary table row with an ID, a concise verb-led title, and a status.
- Exclude items explicitly marked optional, future, later, or out of scope.
- Keep titles grounded in the PRD without inventing behavior to fill gaps.
- Use separate rows for independently implementable capabilities; combine duplicate statements that describe the same capability.
- Order foundational use cases before dependent ones and assign stable sequential IDs as `UC-001`, `UC-002`, and so on.
- Set each newly derived use case to `NOT_IMPLEMENTED`. Do not infer implementation status from the codebase unless the user explicitly asks for a status audit. Possible values of status are `NOT_IMPLEMENTED`, `IN_PROGRESS`, `COMPLETED`. 

## Required document shape

1. `# <Product Name> Requirements`
2. A short introduction linking to the actual PRD path, explaining scope exclusions and status meaning.
3. `## Use Case Summary` containing a Markdown table with `UseCase ID`, `Use Case Title`, and `Status` columns.

## Verify before finishing

Confirm that every in-scope PRD capability is represented in the summary, excluded scope was not promoted into requirements, IDs are unique and sequential.
