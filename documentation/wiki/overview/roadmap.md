---
hide:
  - toc
---

# Development Roadmap

<div class="roadmap-page" markdown>

<p class="roadmap-lede">This public roadmap groups selected SimPaths development priorities into work underway, planned work, and capabilities in the pipeline.</p>

<div class="roadmap-status">
  <span>Last reviewed August 2026</span>
  <span aria-hidden="true">·</span>
  <span>Reviewed quarterly</span>
  <span aria-hidden="true">·</span>
  <span>Later horizons are directional rather than delivery commitments</span>
</div>

<nav class="roadmap-sequence" aria-label="Roadmap stages">
  <a href="#working-on-now"><span class="roadmap-sequence__name">Working on now</span></a>
  <a href="#planned-work"><span class="roadmap-sequence__name">Planned work</span></a>
  <a href="#capabilities-in-the-pipeline"><span class="roadmap-sequence__name">Capabilities in the pipeline</span></a>
</nav>

<div class="roadmap-stage roadmap-stage--now" markdown>

<div class="roadmap-stage__rail" aria-hidden="true"><span>01</span></div>

<div class="roadmap-stage__content" markdown>

<div class="roadmap-stage__heading" markdown>

## Working on now

These items have active development tasks and the clearest near-term scope.

</div>

<div class="roadmap-stage__items" markdown>

<div class="roadmap-item" markdown>

### Wealth across the life course

Model how the assets and debts held by individuals and households evolve over the life course, including housing wealth, mortgages, pensions, savings, and unsecured debt, and how these interact with income and spending. This will enable SimPaths to analyse financial resilience, housing and debt pressures, resources available in retirement, and how wealth inequalities develop over time.

<div class="roadmap-meta" markdown>[Issue #447](https://github.com/simpaths/SimPaths/issues/447)</div>

</div>

<div class="roadmap-item" markdown>

### Validation against source data

Compare model estimates on simulated populations with their input-data equivalents to identify drift, implementation differences, and processes requiring recalibration. This strengthens confidence that estimated relationships remain intact once combined in the full simulation.

<div class="roadmap-meta" markdown>[Issue #192](https://github.com/simpaths/SimPaths/issues/192)</div>

</div>

<div class="roadmap-item" markdown>

### Parameter uncertainty

Expose regression-coefficient bootstrapping through configuration, command-line options, and documentation. This will allow repeated runs to represent parameter uncertainty consistently and make the resulting evidence easier to interpret.

<div class="roadmap-meta" markdown>[Issue #329](https://github.com/simpaths/SimPaths/issues/329)</div>

</div>

<div class="roadmap-item" markdown>

### Employment histories and health

Add modifiers estimated from longitudinal employment histories to the health modules. Connecting accumulated labour-market experience with later physical and mental health will support analysis of health inequalities across the life course.

<div class="roadmap-meta" markdown>[Issue #143](https://github.com/simpaths/SimPaths/issues/143)</div>

</div>

</div>

</div>

</div>

<div class="roadmap-stage" markdown>

<div class="roadmap-stage__rail" aria-hidden="true"><span>02</span></div>

<div class="roadmap-stage__content" markdown>

<div class="roadmap-stage__heading" markdown>

## Planned work

These priorities have defined development tasks, but their timing and order depend on data, estimation work, and related model changes.

</div>

<div class="roadmap-stage__items" markdown>

<div class="roadmap-item" markdown>

### Labour supply, mental health, and childcare

Re-estimate labour supply with individual effects and incorporate mental health, Universal Credit, care time, and childcare costs where supported by the data. This will improve analysis of employment responses that vary with health, caring responsibilities, and benefit receipt.

<div class="roadmap-meta" markdown>[Issues #191](https://github.com/simpaths/SimPaths/issues/191), [#193](https://github.com/simpaths/SimPaths/issues/193), and [#433](https://github.com/simpaths/SimPaths/issues/433)</div>

</div>

<div class="roadmap-item" markdown>

### Multidimensional wellbeing

Integrate multidimensional wellbeing outcomes into the model, update the initial populations, and validate the new processes. This would extend analysis beyond income and employment.

<div class="roadmap-meta" markdown>[Issues #491](https://github.com/simpaths/SimPaths/issues/491), [#492](https://github.com/simpaths/SimPaths/issues/492), [#493](https://github.com/simpaths/SimPaths/issues/493), and [#494](https://github.com/simpaths/SimPaths/issues/494)</div>

</div>

<div class="roadmap-item" markdown>

### Migration and population representation

Define immigrant status consistently, clarify its relationship with ethnicity, and assess where it should enter behavioural processes. Clearer population definitions will improve comparisons across groups and prepare the model for later migration and synthetic-population developments.

<div class="roadmap-meta" markdown>[Issues #303](https://github.com/simpaths/SimPaths/issues/303) and [#304](https://github.com/simpaths/SimPaths/issues/304)</div>

</div>

<div class="roadmap-item" markdown>

### Regional matching and performance

Extend tax-benefit donor matching by Government Office Region, make partnership matching scalable through population partitions, and improve tax-donor parsing and yearly simulation performance. Together these changes will support regional analysis, larger populations, and repeated simulation runs.

<div class="roadmap-meta" markdown>[Issues #156](https://github.com/simpaths/SimPaths/issues/156), [#157](https://github.com/simpaths/SimPaths/issues/157), [#253](https://github.com/simpaths/SimPaths/issues/253), and [#301](https://github.com/simpaths/SimPaths/issues/301)</div>

</div>

<div class="roadmap-item" markdown>

### Alignment and model state

Estimate alignment adjustments once for reuse across scenarios, compare alternative alignment approaches, and make the initialisation and updating of lagged variables consistent. This will produce cleaner comparisons between baseline and policy scenarios.

<div class="roadmap-meta" markdown>[Issues #137](https://github.com/simpaths/SimPaths/issues/137), [#139](https://github.com/simpaths/SimPaths/issues/139), [#119](https://github.com/simpaths/SimPaths/issues/119), and [#273](https://github.com/simpaths/SimPaths/issues/273)</div>

</div>

</div>

</div>

</div>

<div class="roadmap-stage roadmap-stage--pipeline" markdown>

<div class="roadmap-stage__rail" aria-hidden="true"><span>03</span></div>

<div class="roadmap-stage__content" markdown>

<div class="roadmap-stage__heading" markdown>

## Capabilities in the pipeline

These capabilities would materially extend the questions SimPaths can address, but their scope and timing are not yet fixed.

</div>

<div class="roadmap-stage__items" markdown>

<div class="roadmap-item" markdown>

### Unemployment and retirement transitions

Identify unemployment before labour-supply choices, incorporate health into the utility function, allow selected post-retirement employment, and re-estimate wages for the European models. This would provide a richer account of worklessness, return-to-work decisions, and work after retirement.

<div class="roadmap-meta" markdown>[Issue #402](https://github.com/simpaths/SimPaths/issues/402)</div>

</div>

<div class="roadmap-item" markdown>

### Migration and synthetic populations

Develop explicit migration processes and starting populations that reduce dependence on restricted microdata where appropriate. This would support clearer demographic scenarios and make adaptation to new settings more practical.

<div class="roadmap-meta" markdown>Related groundwork: [Issues #303](https://github.com/simpaths/SimPaths/issues/303) and [#304](https://github.com/simpaths/SimPaths/issues/304)</div>

</div>

<div class="roadmap-item" markdown>

### Macroeconomic context and transfers between households

Represent wider economic conditions and transfers between households beyond the immediate benefit unit. This would enable analysis of economic shocks, family support, and redistribution between households.

</div>

<div class="roadmap-item" markdown>

### SIPHER-7 outcomes and health-sensitive mortality

Add SIPHER-7 measures and a mortality process that reflects health differentials. This would show how health inequalities accumulate over time.

<div class="roadmap-meta" markdown>[Issue #505](https://github.com/simpaths/SimPaths/issues/505)</div>

</div>

</div>

</div>

</div>

<div class="roadmap-contact" markdown>

## Suggest a priority or collaborate

To suggest a priority, contribute evidence, or discuss collaboration, [open a GitHub issue](https://github.com/simpaths/SimPaths/issues/new) or contact the SimPaths team at [info@simpaths.org](mailto:info@simpaths.org).

</div>

</div>
