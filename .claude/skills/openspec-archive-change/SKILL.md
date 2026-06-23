---
name: openspec-archive-change
description: Archive a completed change in the experimental workflow. Use when the user wants to finalize and archive a change after implementation is complete.
license: MIT
compatibility: Requires openspec CLI.
metadata:
  author: openspec
  version: "1.0"
  generatedBy: "1.2.0"
---

Archive a completed change in the experimental workflow.

**Input**: Optionally specify a change name. If omitted, check if it can be inferred from conversation context. If vague or ambiguous you MUST prompt for available changes.

**Steps**

1. **If no change name provided, prompt for selection**

   Run `openspec list --json` to get available changes. Use the **AskUserQuestion tool** to let the user select.

   Show only active changes (not already archived).
   Include the schema used for each change if available.

   **IMPORTANT**: Do NOT guess or auto-select a change. Always let the user choose.

2. **Check artifact completion status**

   Run `openspec status --change "<name>" --json` to check artifact completion.

   Parse the JSON to understand:
   - `schemaName`: The workflow being used
   - `artifacts`: List of artifacts with their status (`done` or other)

   **If any artifacts are not `done`:**
   - Display warning listing incomplete artifacts
   - Use **AskUserQuestion tool** to confirm user wants to proceed
   - Proceed if user confirms

3. **Check task completion status**

   Read the tasks file (typically `tasks.md`) to check for incomplete tasks.

   Count tasks marked with `- [ ]` (incomplete) vs `- [x]` (complete).

   **If incomplete tasks found:**
   - Display warning showing count of incomplete tasks
   - Use **AskUserQuestion tool** to confirm user wants to proceed
   - Proceed if user confirms

   **If no tasks file exists:** Proceed without task-related warning.

4. **Assess delta spec sync state**

   Check for delta specs at `openspec/changes/<name>/specs/`. If none exist, proceed without sync prompt.

   **If delta specs exist:**
   - Compare each delta spec with its corresponding main spec at `openspec/specs/<capability>/spec.md`
   - Determine what changes would be applied (adds, modifications, removals, renames)
   - Show a combined summary before prompting

   **Prompt options:**
   - If changes needed: "Sync now (recommended)", "Archive without syncing"
   - If already synced: "Archive now", "Sync anyway", "Cancel"

   If user chooses sync, use Task tool (subagent_type: "general-purpose", prompt: "Use Skill tool to invoke openspec-sync-specs for change '<name>'. Delta spec analysis: <include the analyzed delta spec summary>"). Proceed to archive regardless of choice.

5. **Perform the archive**

   Create the archive directory if it doesn't exist:
   ```bash
   mkdir -p openspec/changes/archive
   ```

   Generate target name using current date: `YYYY-MM-DD-<change-name>`

   **Check if target already exists:**
   - If yes: Fail with error, suggest renaming existing archive or using different date
   - If no: Move the change directory to archive

   ```bash
   mv openspec/changes/<name> openspec/changes/archive/YYYY-MM-DD-<name>
   ```

6. **Update plan.md with completion status**

   Read `openspec/plan.md` to find the phase/section corresponding to this change.
   
   **Update the status markers in plan.md:**
   - If the change name matches a phase entry (e.g., "recurso-catalogo" → Fase 1), update the percentage: `[100% ✅]`
   - Mark all tasks in that phase section with `[x]` checkboxes
   - Add a reference to the merged PR if available (e.g., "PR #17 mergeado")
   - Update the "Última actualización" date in the plan header
   
   **Example transformation:**
   ```
   Before:
   ### Fase 1 — Fundación: recurso reservable (`recurso-catalogo`) **[0%]**
   
   After:
   ### Fase 1 — Fundación: recurso reservable (`recurso-catalogo`) **[100% ✅]**
   **Cambio:** PR #17 mergeado ✅
   ```

7. **Update docs/ documentation if needed**

   Check if this change requires documentation updates:
   
   **Scan for doc-related artifacts:**
   - Look for references to specific docs files in the change's `proposal.md` or `design.md`
   - Check if delta specs indicate changes that affect documentation (new entities, endpoints, security patterns, etc.)
   
   **Automatically update based on change content:**
   - **If data model changes** (new entities, renamed fields): Update `docs/data-model.md` with new/modified entity descriptions
   - **If new APIs/endpoints**: Update `docs/openapi.yaml` reference (note: regenerate via backend if full spec needed)
   - **If nomenclature changes**: Update `docs/GLOSARIO-nomenclatura-ES-EN.md` with new translations
   - **If testing strategy changes**: Update `docs/TESTING-STRATEGY.md` with new test requirements or coverage thresholds
   - **If security patterns change**: Update `docs/security-design.md` with new RBAC/auth rules
   - **Always update**: Add change name to the "Changelog" or "History" section at top of docs (if it exists)
   
   **Process for each doc update:**
   1. Read the relevant docs file
   2. Identify sections that need updates based on delta specs or design decisions
   3. Merge changes intelligently (don't overwrite unrelated content)
   4. Add timestamp and change reference (e.g., "Updated by PR #17 — refactor-nomenclatura-ingles")
   5. Preserve formatting and structure consistency
   
   **If uncertain about docs updates:**
   - Don't guess or over-modify documentation
   - Log a warning: "No automatic doc updates detected for <change-name> — verify docs manually"
   - Proceed with archive without blocking

8. **Display summary**

   Show archive completion summary including:
   - Change name
   - Schema that was used
   - Archive location
   - Whether specs were synced (if applicable)
   - Plan.md updated with completion status
   - Docs files updated (list which files were modified, if any)
   - Note about any warnings (incomplete artifacts/tasks/undetected doc needs)

**Output On Success**

```
## Archive Complete

**Change:** <change-name>
**Schema:** <schema-name>
**Archived to:** openspec/changes/archive/YYYY-MM-DD-<name>/
**Specs:** ✓ Synced to main specs (or "No delta specs" or "Sync skipped")
**Plan:** ✓ openspec/plan.md updated — Fase X marked [100% ✅]
**Docs:** ✓ Updated: docs/data-model.md, docs/GLOSARIO-nomenclatura-ES-EN.md
         (or "No automatic doc updates detected" if no changes needed)

All artifacts complete. All tasks complete.
```

**Mapping: Change Content → Documentation Updates**

| Delta Spec Signal | Documentation File | Update Action |
|---|---|---|
| New entities, field renames, type changes | `docs/data-model.md` | Add/update entity definitions, update schema section |
| New/renamed endpoints, route changes | `docs/openapi.yaml` | Reference to backend-architect output; note in docs/PROJECT.md API_PATH |
| Spanish↔English translations, nomenclature changes | `docs/GLOSARIO-nomenclatura-ES-EN.md` | Add new translation pairs, note when applied |
| New test scenarios, coverage changes | `docs/TESTING-STRATEGY.md` | Update test requirements section, adjust coverage thresholds if applicable |
| New auth rules, RBAC patterns, security decisions | `docs/security-design.md` | Update auth/RBAC matrix, add new threat model entries |
| New business rules (RN-xx) | `docs/PROJECT.md` or a separate `docs/business-rules.md` | Add/update RN descriptions and rationale |
| Frontend routes, UI patterns | `docs/ui-screens.md` (if exists) or UI design docs | Update screen references, interaction flows |
| Database migrations, schema changes | `docs/data-model.md` + Flyway version history | Document new migration versions and their purpose |

**Certainty thresholds for docs updates:**
- **High confidence (proceed)**: Delta spec explicitly lists requirements for a doc section (e.g., "Requirement: Entidad Employee com campos X, Y, Z")
- **Medium confidence (log warning)**: Design.md mentions architectural decisions that impact a doc (e.g., "usar JOINED inheritance")
- **Low confidence (don't update)**: Vague or implied changes that could be wrong if automated (e.g., guessing which security rules apply)

**Guardrails**
- Always prompt for change selection if not provided
- Use artifact graph (openspec status --json) for completion checking
- Don't block archive on warnings - just inform and confirm
- Preserve .openspec.yaml when moving to archive (it moves with the directory)
- Show clear summary of what happened
- If sync is requested, use openspec-sync-specs approach (agent-driven)
- If delta specs exist, always run the sync assessment and show the combined summary before prompting
- **ALWAYS update openspec/plan.md after archiving** — mark the phase as [100% ✅] and update task checkboxes
- When updating plan.md, search for phase/capability name in the file to ensure correct section is updated
- If change name doesn't match any phase section, log warning but don't block archive
- Update "Última actualización" date header in plan.md to current date
- **ALWAYS scan for docs/ updates needed** — read delta specs and design.md to detect which docs need updates
- **Docs update policy: Conservative** — only modify docs if you have high confidence from delta specs/design artifacts
- If uncertain whether a doc needs updates, log a warning and let the user verify manually (don't guess)
- When updating docs, preserve formatting, cross-references, and structure consistency
- Add change reference (PR #, change name, timestamp) when modifying docs so future readers know what changed and why
- Don't block archive on docs updates — if docs need updates but can't be automated, proceed and note it in summary
