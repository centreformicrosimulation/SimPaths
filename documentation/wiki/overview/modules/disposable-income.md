---
title: Disposable income
---

<div class="module-detail" markdown>

# Disposable income

<p class="module-detail__lead">Disposable income is simulated by matching each benefit unit in each projected period to a donor benefit unit in a tax-benefit reference database, then imputing taxes, benefits and disposable income from that match.</p>

## Tax-benefit donor data

The reference database contains taxes and benefits alongside demographic characteristics and private incomes for a sample of benefit units. It may be populated from different sources. The method was originally developed for output from the UK version of EUROMOD, UKMOD, and later extended to support projections from any EUROMOD country.

The approach follows [van de Ven et al. (2022)](https://www.iser.essex.ac.uk/wp-content/uploads/files/working-papers/cempa/cempa3-22.pdf).

## Matching procedure

The procedure first applies coarsened exact matching to discrete characteristics, then nearest-neighbour matching to continuous characteristics. Nearest neighbours are selected using Mahalanobis distances evaluated jointly over the continuous variables.

The default discrete characteristics include the age of the benefit-unit reference person, partnership status, numbers of children by age, each adult's hours of work, disability and informal-care provision. The default continuous characteristics include original income before taxes and benefits, second income to represent income splitting within couples, and formal childcare costs.

## Imputation

After a donor is matched, disposable income is imputed in one of two ways. Above a specified poverty threshold, the simulated benefit unit's original income is multiplied by the donor's ratio of disposable to original income. Below the threshold, disposable income is set to the donor's disposable income after applying the relevant growth adjustment.

Public subsidies for formal social-care costs are evaluated separately through functions programmed within SimPaths. This is necessary because those subsidies are not consistently represented in the tax-benefit databases used as donor sources.

</div>
