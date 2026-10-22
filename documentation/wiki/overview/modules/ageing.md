---
title: Ageing
---

<div class="module-detail" markdown>

# Ageing

<p class="module-detail__lead">The ageing module advances the simulated population through time, applies mortality risks, aligns population totals to official projections and determines whether adult children leave the parental home.</p>

## Annual age transition and mortality

The first simulated process in each period increments the age of every simulated person by one year. A dependent child who reaches the assumed age of independence - 18 in the UK parameterisation - is moved from the parental benefit unit into a new benefit unit.

Individuals are then subject to a risk of death based on age-, gender- and year-specific probabilities reported in official population projections. Death is simulated at individual level, except in single-parent benefit units, where it is omitted to avoid creating orphans.

## Population alignment

Population alignment adjusts the simulated population to national projections by age, gender, region and year. Alignment proceeds from the youngest to the oldest age covered by the projections.

Within each age-gender-region-year subgroup, simulated population counts are compared with the corresponding targets. Regions with too few simulated people are separated from those with too many. Net domestic migration is then represented by moving benefit units from regions above target to regions below target, using the youngest benefit-unit member as the reference person, until the available domestic moves are exhausted.

Remaining differences are treated as net international immigration when the simulated population is too small, or as emigration and death when it is too large. These transitions are also applied at benefit-unit level with reference to the youngest member. Above an assumed threshold - 65 in the UK parameterisation - death is used in preference to international emigration.

Apart from the distinctions by age, gender, region and year, alignment transitions are distributed randomly. The model therefore does not distinguish, for example, prior immigrants when projecting emigration. Immigration is represented by cloning existing benefit units and does not impose systematic differences between domestic and migrant populations, including differences in financial circumstances.

## Leaving the parental home

People who have reached the age of independence and have been moved into separate benefit units are evaluated to determine whether they leave the parental home. Those still in education remain in the parental household.

For adult children who are not in education, the probability of leaving is described by a probit model conditional on gender, age, education, lagged employment status, lagged household-income quintile, region and year. Those projected to remain may leave in a later year.

</div>
