# mpjsons internals documentation

This directory is the working knowledge base for engineers and AI agents changing `mpjsons`
(`io.mpjsons`, version `0.6.51`, Scala `2.13.16`). It was produced on 13/10/2026 by reading
every source and test file and by running targeted experiments against the real code
(all claims marked **verified** were observed by running code on this checkout; everything
else is derived from reading the source and is marked accordingly).

Read in this order:

| File | What it answers |
|------|-----------------|
| [architecture.md](architecture.md) | How a `serialize`/`deserialize` call flows through the code; every component, cache, and reflection trick; class-by-class map. |
| [json-format.md](json-format.md) | The exact wire format produced/accepted for every supported type, including parser leniency. Needed for any size-minimisation or format change. |
| [known-issues.md](known-issues.md) | Confirmed bugs (with repro and root cause `file:line`), limitations, sharp edges, and JDK-evolution risks. |
| [performance.md](performance.md) | Measured baseline numbers, hot-path analysis with per-component costs, and a prioritised optimisation plan with expected gains and risks. |
| [roadmap.md](roadmap.md) | Design notes for the planned work: `null` literal support, field naming / JSON size reduction, raw performance; prerequisite fixes; compatibility strategy; open decisions for the owner. |

Conventions used in these documents:

- Paths are relative to the repository root; `file.scala:NN` points at the line in this checkout (commit `d9fe778` on branch `scala_2.13`).
- "Bean" means any class handled by `BeanSerializer`/`BeanDeserializer`: case classes, plain classes with `var`/`val` fields.
- "Cold path" = code that runs once per type (factory dispatch, reflection). "Hot path" = code that runs for every value on every call.
- Timing numbers come from in-process micro-benchmarks run inside ScalaTest on JDK 25 / Windows 11 (see `performance.md` for the harness). They are good for ratios and order of magnitude, not for absolute claims; use JMH before accepting or rejecting an optimisation.

Keeping these docs useful: when a bug listed in `known-issues.md` is fixed, replace its body with a one-line "FIXED: ... covered by `XSpec`" note so the numbering other documents refer to stays stable; when a format decision from `roadmap.md` is taken, move the result into `json-format.md`.
