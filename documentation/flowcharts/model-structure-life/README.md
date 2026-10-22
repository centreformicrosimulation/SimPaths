# Model-structure diagram

## Source and preserved originals

The user supplied `model structure - LIFe.ccd` (ClickCharts). Related Box source: https://app.box.com/file/2004450803752, last modified 15 October 2026. The supplied file has not been verified byte-for-byte against Box.

- `original.ccd`: unmodified supplied file.
- `web.ccd`: original geometry with the two left-hand explanatory text blocks removed.
- `uk-current.ccd`: native working copy with UK implementation colours updated on 22 October 2026. Geometry and module labels are preserved. Its legend is supplied by the website; dashed extension outlines are applied in the SVG renderer.
- `geometry.json`: object centres and half-dimensions extracted from `web.ccd`.
- `render-current-diagram.cjs`: independent SVG rendering of the source geometry, labels and connections. This avoids the trial watermark on subsequent ClickCharts exports and provides scalable text. It adds a dated key and dashed outlines for unimplemented extensions. Website labels use a larger text scale for readability; childcare is labelled as costs and the macro heading as wage-growth assumptions to reflect the implemented scope. Two narrow boxes are widened slightly to retain text padding.
- Website asset: `../../wiki/assets/images/model-structure-uk.svg`.
- Earlier historical ClickCharts PNG: `../../wiki/assets/images/model-structure-life.png` (Summer 2025 country-status colours; no longer used by the overview).

Regenerate the SVG from this directory:

```sh
node render-current-diagram.cjs geometry.json ../../wiki/assets/images/model-structure-uk.svg
```

## Status evidence

Reviewed on 22 October 2026 against `origin/develop`, commit `971d050c521e9d1aa6b86308655bc5ae7d8d8741` (15 October 2026). The key describes UK development implementation, not release readiness, validation or cross-country availability.

- `SimPathsModel.buildSchedule()` schedules pension/non-pension wealth, housing wealth, unsecured debt, total wealth, inheritance settlement and release of mature trusts. `WealthModule`, `WealthNonPension`, `WealthHousing`, `WealthFinancial`, `PrivatePension` and `InheritanceModule` supply the implementations. Wealth projections are conditional on Parameters switches.
- `SimPathsModel.populationAlignment()` handles internal redistribution and international population adjustments. This is not a behavioural migration module.
- `Parameters` reads wage-growth assumptions from `economic_time_series.xlsx` (`wage_growth`). No endogenous macroeconomic model is implied.
- `BenefitUnit` and `Parameters` implement formal-childcare incidence/expenditure and costs, not general childcare time allocation.
- No implementation of general home production, inter-vivos transfers or dedicated pension-rule imputation was identified in this revision. Existing public-pension income representation remains distinct from that proposed extension.
- The October 2026 LIFe presentation (Box file 2536644460533) changes country tiers but does not establish process-level implementation in each country. Old country tiers have therefore been replaced with an explicitly UK-scoped key.

The source's arrows are conceptual connections, not the exact UK event schedule. For example, mortality/alignment and mental-health/life-satisfaction updates occur later in the actual schedule than their positions in this diagram suggest. The overview retains this distinction and explains execution order beneath the diagram.
