---
title: Social care
---

<div class="module-detail" markdown>

# Social care

<p class="module-detail__lead">The social care module projects the receipt and provision of care for people in need because of poor health or advanced age. It distinguishes formal and informal care, the relationships involved in informal care, providers' time costs and recipients' formal-care costs.</p>

## Receipt of social care

The model distinguishes people above and below an age threshold when projecting receipt of social care. This reflects the higher prevalence of care among older people and the more detailed information usually available for that population.

### Older people

For people aged 65 or over in the UK parameterisation, the model first determines whether care is needed. A probit equation conditions on gender, education, partnership status, whether care was needed in the preceding year, self-reported health and age. A similar set of characteristics is used to project whether care is received.

For a person projected to receive care, a multinomial logit distinguishes among informal care only, formal and informal care, and formal care only. The equation also includes education, partnership status, age band and a lagged dependent variable.

The source of informal care is selected in stages. For a person with a partner, the first stage determines whether that partner provides care. If so, a multinomial logit determines whether care is also received from a daughter, a son or someone else. If a partner does not provide care, another multinomial logit selects among six alternatives allowing for up to two carers drawn from daughters, sons and other people.

Log-linear equations then project the hours received from each identified carer. Hours of formal care are converted to a cost using the year-specific mean hourly wage of social-care workers.

### Younger people

For people below the age threshold, the model uses a more stylised approach because less detailed data are available. It focuses on informal care received by people simulated to be long-term sick or disabled.

When a person enters a disabled state, a probit equation determines whether informal care is received. That status persists while the person remains disabled. For recipients below age 65, hours of care are described by a log-linear equation.

## Provision of informal care

The model projects care provided informally; the characteristics of formal-sector providers are outside its current scope. For older recipients, the care-receipt process already identifies providers' relationships to recipients, care hours and the persistence of those relationships.

Starting-population datasets generally do not record the wider social links implied by informal care, apart from links between partners. In particular, links between adult children and parents, and wider informal-care networks, are often absent. The provision model is designed to work within those data constraints.

People are classified into four groups: no informal care provided; care provided only to a partner; care provided to a partner and someone else; and care provided only to non-partners. For people already identified as caring for a partner, a probit distinguishes partner-only care from care for a partner and someone else. For everyone else, another probit distinguishes no provision from care supplied only to non-partners. A log-linear equation then projects hours of care provided.

</div>
