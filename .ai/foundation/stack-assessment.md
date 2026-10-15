---
project: mpjsons
assessed_at: 2026-09-16T00:00:00Z
agent_readiness: ready-with-compensation
context_type: brownfield
stack_components:
  language: Scala 2.13.16
  framework: null
  build_tool: sbt 1.11.7
  test_runner: ScalaTest 3.2.15
  package_manager: sbt
  ci_provider: null
  deployment_target: null
gates_passed: 4
gates_failed: 1
---

## Stack Components

**Language**: Scala 2.13.16 (`build.sbt:9`).

**Framework**: none — `mpjsons` is not built on an application or serialization framework. It *is* the library: a reflection-based Scala object ⇄ JSON converter using `scala.reflect.runtime.universe` at runtime (`CLAUDE.md`, "What this is"). No comparable off-the-shelf framework dependency to score against the four gates, so this row is marked not-applicable in the matrix — but the underlying **technique** (runtime reflection) is assessed separately below since it's the single most agent-relevant fact about this codebase.

**Build tool**: sbt 1.11.7 (`project/build.properties`), single-module.

**Test runner**: ScalaTest 3.2.15 (`build.sbt:20-22`, FlatSpec + MustMatchers).

**CI/CD**: no CI config file found in the repo tree.

## Quality Gate Assessment

| Component  | Typed | Convention | Training Data | Documented | Verdict    |
|------------|-------|------------|----------------|------------|------------|
| Language   | ✓     | —          | —              | —          | pass       |
| Framework  | —     | —          | —              | —          | n/a        |
| Build tool | —     | ✓          | ✓              | ~          | pass       |
| Test runner| —     | —          | ✓              | ✓          | pass       |

Legend: ✓ = pass, ✗ = fail, ~ = partial, — = not applicable

### Gate Details

**Language — typed: pass.** Scala, statically typed. Evidence: `build.sbt:9`.

**Build tool — sbt: pass with a documentation caveat.** Standard single-module sbt layout, well represented in Scala training data. Official docs exist and are versioned but are commonly reported as less approachable than Maven/Gradle equivalents — scored partial on documentation alone, not enough to fail the component given `CLAUDE.md`'s own "Build & test commands" section spells out the exact `sbt compile` / `sbt test` / `sbt "testOnly <Spec>"` invocations needed.

**Test runner — ScalaTest: pass.** Dominant Scala test framework, well documented, common in training data.

**Reflection-based serialization technique — not a quality-gate row, but the load-bearing risk.** `mpjsons` builds per-type (de)serializers at runtime via `scala.reflect.runtime.universe`, rather than the compile-time macro approach used by more training-data-common Scala JSON libraries (circe, play-json, upickle). This isn't a "typed" gate failure — the *language* is fully typed — but an agent reasoning about `mpjsons` from general Scala JSON-library training data is more likely to have internalized macro-based idioms (`derives Codec`, `Json.format[T]`-style boilerplate) than mpjsons' own API (`mpjsons.serialize[T]` / `deserialize[T]` with reflection-built serializers under the hood). This is a training-data-adjacent risk specific to *this* library's technique, not one of the four scored gates, so it's called out here rather than folded into the matrix.

## Gaps & Compensation

**Reflection-technique unfamiliarity.** An agent extending or debugging `mpjsons` internals (not just calling its public API) may default to macro-based mental models that don't apply here — e.g. assuming compile-time-generated codecs exist to inspect, when the actual serializers are built and cached at runtime via reflection.

### Recommended Instruction File Additions

`CLAUDE.md` already opens with a clear technique statement ("Scala 2.13 runtime reflection... to inspect types and build per-type (de)serializers, which are then cached"). One addition worth making, if not already present further down the file, is an explicit anti-pattern warning:

```markdown
## Don't reach for macro-based patterns here

mpjsons builds serializers at runtime via `scala.reflect.runtime.universe`, not compile-time macros.
There is no `derives Codec`, no macro-annotation, and no generated companion-object boilerplate to
edit. If a serialization bug looks like it needs a new codec, look for the runtime serializer-building
logic (reflection-driven), not a macro expansion.
```

(Skip this addition if an equivalent warning already exists elsewhere in `CLAUDE.md` — verify before pasting.)

## Summary

Overall verdict: **ready-with-compensation**. Scala's typing, sbt's ubiquity, and ScalaTest's maturity are all agent-friendly out of the box, and there's no framework layer to score. The one real friction point is technique-level, not stack-level: mpjsons' runtime-reflection approach to serialization is less common in training data than the compile-time-macro libraries an agent is more likely to have internalized, which is worth one explicit anti-pattern note in `CLAUDE.md` if not already covered.

Next step: `/mario:health-check`.
