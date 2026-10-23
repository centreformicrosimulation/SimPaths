# Model-structure diagram

## Source and preserved originals

The user supplied `model structure - LIFe.ccd` (ClickCharts). Related Box source: https://app.box.com/file/2004450803752, last modified 15 October 2026. The supplied file has not been verified byte-for-byte against Box.

- `original.ccd`: unmodified supplied file.
- `web.ccd`: original geometry with the two left-hand explanatory text blocks removed.
- `uk-current.ccd`: native working copy with UK implementation colours updated on 22 October 2026. Geometry and module labels are preserved. Its legend is supplied by the website; dashed extension outlines are applied in the SVG renderer.
- `geometry.json`: object centres and half-dimensions extracted from `web.ccd`.
- `render-current-diagram.cjs`: independent SVG rendering of the source geometry, labels and connections. This avoids the trial watermark on subsequent ClickCharts exports and provides scalable text. It adds a dated heading and a single dashed-line key for components under development. Website labels use a larger text scale for readability. Two narrow boxes are widened slightly to retain text padding.
- Website asset: `../../wiki/assets/images/model-structure-uk.svg`.
- Earlier historical ClickCharts PNG: `../../wiki/assets/images/model-structure-life.png` (Summer 2025 country-status colours; no longer used by the overview).

Regenerate the SVG from this directory:

```sh
node render-current-diagram.cjs geometry-main-20261023.json ../../wiki/assets/images/model-structure-uk.svg
```

## Main status evidence

The main website renderer is checked against `b649c6967db2d9cced068c8878a2979725dc39e0`. `SimPathsModel.buildSchedule()` schedules `BenefitUnit.Processes.UpdateWealth` when `enableIntertemporalOptimisations` is enabled; `BenefitUnit.updateWealth()` adds disposable income less discretionary and non-discretionary expenditure to aggregate wealth. Main does not contain the separate asset-projection classes and inheritance settlement listed below. The renderer therefore marks those separate processes as development extensions. This does not imply that main has no wealth representation.

`uk-current.ccd` remains the preserved development working copy. Regenerate the main website SVG using this branch's renderer, not by exporting that native development copy.

## Preserved development status evidence

Reviewed on 22 October 2026 against `origin/develop`, commit `971d050c521e9d1aa6b86308655bc5ae7d8d8741` (15 October 2026). The key describes UK development implementation, not release readiness, validation or cross-country availability.

- `SimPathsModel.buildSchedule()` schedules pension/non-pension wealth, housing wealth, unsecured debt, total wealth, inheritance settlement and release of mature trusts. `WealthModule`, `WealthNonPension`, `WealthHousing`, `WealthFinancial`, `PrivatePension` and `InheritanceModule` supply the implementations. Wealth projections are conditional on Parameters switches.
- `SimPathsModel.populationAlignment()` handles internal redistribution and international population adjustments. This is not a behavioural migration module.
- `Parameters` reads wage-growth assumptions from `economic_time_series.xlsx` (`wage_growth`). No endogenous macroeconomic model is implied.
- `BenefitUnit` and `Parameters` implement formal-childcare incidence/expenditure and costs, not general childcare time allocation.
- No implementation of general home production, inter-vivos transfers or dedicated pension-rule imputation was identified in this revision. Existing public-pension income representation remains distinct from that proposed extension.
- The October 2026 LIFe presentation (Box file 2536644460533) changes country tiers but does not establish process-level implementation in each country. Old country tiers have therefore been replaced with an explicitly UK-scoped key.

The source's arrows are conceptual connections, not the exact UK event schedule. For example, mortality/alignment and mental-health/life-satisfaction updates occur later in the actual schedule than their positions in this diagram suggest. The overview retains this distinction and explains execution order beneath the diagram.

## Main diagram revision, 23 October 2026

The preview incorporates Matteo’s discussion: life satisfaction moves to a separate well-being module; other well-being outcomes are explicitly proposed, not implemented. Demand for care is labelled Care needs. The time-transfer grouping and unimplemented home production are omitted; implemented childcare costs move to the income/consumption grouping. Aggregate wealth accounting is explicitly shown alongside the dashed separate-asset extensions. Private monetary transfers remain dashed for main.

`geometry-main-20261023.json` is the revised editable geometry. `geometry.json` and the native ClickCharts files preserve the earlier layout; they do not represent this revision. The JavaScript renderer and JSON geometry are the editable source for the website SVG and require no proprietary editor.

Checked against main c62e487e42f5b658af3e91718c794154eec4a846. Model source is unchanged from b649c6967. LifeSatisfaction1/2 are scheduled; no additional dedicated well-being-outcomes module is claimed. The connecting path is conceptual, not an assertion of execution order.

## Publishing-source change, 23 October 2026

The website now follows develop (model code checked at 11bec2bd95b9caff1562509b32dcb375a6cbe15d). The revised geometry and explanatory notes are retained, but separate asset projections and bequest settlement are solid implemented processes again. Additional well-being outcomes, inter-vivos transfers and dedicated pension-rule imputation remain dashed. Release availability is explained in the website notes. Earlier main-specific status evidence above is historical.

## Matteo’s diagram corrections, 23 October 2026

The website SVG incorporates Matteo’s subsequent email corrections. The implemented-process and alignment/assumptions/costs legend entries are removed; those outlines now share the standard charcoal styling. The only legend item is **Under development**, shown with a purple dashed line. The wealth module contains Housing, Private pensions, Other assets and Debt, replacing the Aggregate wealth box. Debt is supported by the scheduled `UpdateUnsecuredDebt` process.

The wage-growth group is renamed **Macro module**, with **Long term growth** and **Short term fluctuations** boxes. Long term growth denotes the supplied economic time-series assumptions (`Parameters` loads `economic_time_series.xlsx`, including `wage_growth`); it does not claim an endogenous growth model. Short term fluctuations has a dashed outline because no dedicated implementation was identified in the current source. The other dashed components retain their existing status. Original ClickCharts files and earlier status evidence above are preserved as history.
