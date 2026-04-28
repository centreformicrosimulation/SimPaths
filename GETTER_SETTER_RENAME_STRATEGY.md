# Getter and Setter Rename Strategy

## Goal

Align Java getter and setter method names with the backing field names they expose or update.

Example in `simpaths.model.Person`:

```java
private Education eduHighestC4;

public Education getEduHighestC4() {
    return eduHighestC4;
}

public void setEduHighestC4(Education eduHighestC4) {
    this.eduHighestC4 = eduHighestC4;
}
```

## Core Rule

Treat each getter and setter as a Java API rename.

For each accessor:

1. Confirm the backing field used by the method body.
2. Rename the method suffix to match the backing field in UpperCamelCase.
3. Rename all Java call sites in the same batch.
4. Compile after the batch.
5. Search for the old method name and resolve any remaining references.

Avoid global text replacement across the repository. Some old names may also be survey variable names, database columns, CSV headers, comments, or external configuration values, and those should not be changed unless they are genuinely Java method references.

## Progress Summary

| Batch | Status | Validation | Notes |
| --- | --- | --- | --- |
| `Person` education fields | Completed | `mvn test -DskipTests` and `mvn -Dtest=PersonTest test` passed on 2026-04-28 | No compatibility wrappers retained. Old method references removed from Java source and tests. |
| `Person` labour status fields | Completed | `mvn test -DskipTests` and `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test` passed on 2026-04-28 | No compatibility wrappers retained. Old method references removed from Java source, tests, and reflective getter strings. |
| `Person` health fields | Completed | `mvn test -DskipTests` and `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test` passed on 2026-04-28 | No compatibility wrappers retained. Old `Person` method references removed from Java source and reflective getter strings. Non-`Person` `States` and tax donor `getDlltsd()` APIs were intentionally retained. |
| `Person` income fields | Completed | `mvn test -DskipTests` and `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test` passed on 2026-04-28 | No compatibility wrappers retained. Old income method references removed from Java source, tests, XML/properties, and reflective getter strings. |
| `Person` demographic fields | Completed | `mvn test -DskipTests` and `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test` passed on 2026-04-28 | No compatibility wrappers retained. Old `Person` demographic method references removed from Java source and reflective getter strings. Non-`Person` `States.getDcpst()` API was intentionally retained. |
| Remaining `Person` education/social-care flags | Completed | `mvn test -DskipTests` and `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test` passed on 2026-04-28 | Strict field-name rule used for social-care methods. No compatibility wrappers retained. Old method references removed from Java source, tests, XML, and properties. |
| `BenefitUnit` local proxy state | Completed | `mvn test -DskipTests` and `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test` passed on 2026-04-28 | Updated to preserve the `I_` prefix for methods setting `i_...` fields. `Person.setYearLocal(...)` intentionally retained outside this batch. |
| `BenefitUnit` children and poverty lags | Completed | `mvn test -DskipTests` and `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test` passed on 2026-04-28 | `BenefitUnit` poverty API renamed to `yPvrtyFlag` field style. `Person.getAtRiskOfPoverty()` intentionally retained as a person-level derived API. |
| `BenefitUnit` household income and quintile fields | Completed | `mvn test -DskipTests` and checkpoint `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test` passed on 2026-04-28 | Reflective collector string updated. Statistics percentile methods such as `getYdses_p20()` intentionally retained. |
| `BenefitUnit` wealth and costs | Completed | `mvn test -DskipTests` and checkpoint `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test` passed on 2026-04-28 | Renamed only `BenefitUnit` APIs and direct `BenefitUnit` call sites. Similar `Person`, `Parameters`, decision-grid, and tax-key APIs intentionally retained. |
| `Household` original ID accessor | Completed | `mvn test -DskipTests` passed on 2026-04-28 | Renamed `getIdOriginalHH()` to `getIdHhOriginal()`. |
| Original household ID accessors | Completed | `mvn test -DskipTests` and local checkpoint `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test` passed on 2026-04-28 | Renamed `Person` and `BenefitUnit` `getIdOriginalHH()` to `getIdHhOriginal()`. |
| Original person/benefit-unit ID accessors | Completed | `mvn test -DskipTests` and local checkpoint `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test` passed on 2026-04-28 | Renamed `getIdOriginalPerson()` and `getIdOriginalBU()` to match `idPersOriginal` and `idBuOriginal`. |
| `i_` accessor naming correction | Completed | `mvn test -DskipTests` passed on 2026-04-28 | Corrected completed `BenefitUnit` `i_...` accessors to keep `I_`, including `getI_yNonBenHhGrossAsinh...` and local proxy setters. |

## Naming Convention

Use standard JavaBean-style accessor names:

- `fieldName` -> `getFieldName()` / `setFieldName(...)`
- Boolean fields may use either the existing project convention or `isFieldName()` if already established locally.
- Lag fields should preserve the field suffix:
  - `eduHighestC4L1` -> `getEduHighestC4L1()` / `setEduHighestC4L1(...)`
- Fields whose names begin with `i_` should keep the `I_` prefix in accessor names:
  - `i_yNonBenHhGrossAsinh` -> `getI_yNonBenHhGrossAsinh()` / `setI_yNonBenHhGrossAsinh(...)`
- Local/transient helper accessors should keep meaningful qualifiers:
  - `setRegionLocal(...)` can remain distinct from `setRegion(...)` where both have different behavior.

## Completed: Person Education Fields

Completed education-related accessor renames in `src/main/java/simpaths/model/Person.java` and Java call sites under `src/main/java` and `src/test/java`.

Completed renames:

| Old method | New method | Backing field |
| --- | --- | --- |
| `getDeh_c4()` | `getEduHighestC4()` | `eduHighestC4` |
| `setDeh_c4(...)` | `setEduHighestC4(...)` | `eduHighestC4` |
| `setDeh_c4_lag1(...)` | `setEduHighestC4L1(...)` | `eduHighestC4L1` |
| `getDehm_c4()` | `getEduHighestMotherC4()` | `eduHighestMotherC4` |
| `setDehm_c4(...)` | `setEduHighestMotherC4(...)` | `eduHighestMotherC4` |
| `getDehf_c4()` | `getEduHighestFatherC4()` | `eduHighestFatherC4` |
| `setDehf_c4(...)` | `setEduHighestFatherC4(...)` | `eduHighestFatherC4` |
| `getDed()` | `getEduSpellFlag()` | `eduSpellFlag` |
| `setDed(...)` | `setEduSpellFlag(...)` | `eduSpellFlag` |
| `getDed_lag1()` | `getEduSpellFlagL1()` | `eduSpellFlagL1` |
| `setDed_lag1(...)` | `setEduSpellFlagL1(...)` | `eduSpellFlagL1` |
| `setDehsp_c4_lag1(...)` | `setEduHighestPartnerC4L1(...)` | `eduHighestPartnerC4L1` |

Validation:

- Compile command: `mvn test -DskipTests`
- Result: passed
- Focused test command: `mvn -Dtest=PersonTest test`
- Focused test result: passed, 25 tests run
- Remaining old method references in Java source/tests: none
- Compatibility wrappers: none retained

## Completed: Person Labour Status Fields

Completed labour-status accessor renames in `src/main/java/simpaths/model/Person.java` and Java call sites under `src/main/java` and `src/test/java`.

Completed renames:

| Old method | New method | Backing field |
| --- | --- | --- |
| `getLes_c4()` | `getLabC4()` | `labC4` |
| `setLes_c4(...)` | `setLabC4(...)` | `labC4` |
| `getLes_c4_lag1()` | `getLabC4L1()` | `labC4L1` |
| `setLes_c4_lag1(...)` | `setLabC4L1(...)` | `labC4L1` |
| `getLes_c7_covid()` | `getLabC7Covid()` | `labC7Covid` |
| `setLes_c7_covid(...)` | `setLabC7Covid(...)` | `labC7Covid` |
| `getLes_c7_covid_lag1()` | `getLabC7CovidL1()` | `labC7CovidL1` |
| `setLes_c7_covid_lag1(...)` | `setLabC7CovidL1(...)` | `labC7CovidL1` |
| `getLesdf_c4()` | `getLabStatusPartnerAndOwnC4()` | computed from `labC4` and partner `labC4` |
| `getLesdf_c4_lag1()` | `getLabStatusPartnerAndOwnC4L1()` | `labStatusPartnerAndOwnC4L1` |
| `setLesdf_c4_lag1(...)` | `setLabStatusPartnerAndOwnC4L1(...)` | `labStatusPartnerAndOwnC4L1` |

Validation:

- Compile command: `mvn test -DskipTests`
- Result: passed
- Focused test command: `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test`
- Focused test result: passed, 29 tests run
- Remaining old method references in Java source/tests/XML/properties: none
- Reflective getter strings updated: `@Lag(getter="getLabStatusPartnerAndOwnC4")`
- Compatibility wrappers: none retained

Work-history accessors such as `getLiwwh()` / `setLiwwh(...)` were not included in this batch. Handle them in a separate labour-history batch if needed.

## Completed: Person Health Fields

Completed health-related accessor renames in `src/main/java/simpaths/model/Person.java` and Java call sites under `src/main/java`.

Completed renames:

| Old method | New method | Backing field |
| --- | --- | --- |
| `getDhe()` | `getHealthSelfRated()` | `healthSelfRated` |
| `setDhe(...)` | `setHealthSelfRated(...)` | `healthSelfRated` |
| `getDheValue()` | `getHealthSelfRatedValue()` | computed from `healthSelfRated` |
| `setDhe_lag1(...)` | `setHealthSelfRatedL1(...)` | `healthSelfRatedL1` |
| `getDlltsd()` | `getHealthDsblLongtermFlag()` | `healthDsblLongtermFlag` |
| `setDlltsd(...)` | `setHealthDsblLongtermFlag(...)` | `healthDsblLongtermFlag` |
| `getDlltsd_lag1()` | `getHealthDsblLongtermFlagL1()` | `healthDsblLongtermFlagL1` |
| `setDlltsd_lag1(...)` | `setHealthDsblLongtermFlagL1(...)` | `healthDsblLongtermFlagL1` |
| `setDhesp_lag1(...)` | `setHealthPartnerSelfRatedL1(...)` | `healthPartnerSelfRatedL1` |

Validation:

- Compile command: `mvn test -DskipTests`
- Result: passed
- Focused test command: `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test`
- Focused test result: passed, 29 tests run
- Remaining old `Person` method references in Java source/tests/XML/properties: none
- Reflective getter strings updated: `"getHealthSelfRatedValue"` and `"getHealthSelfRated"`
- Compatibility wrappers: none retained

Intentional retained names:

- `States.getDlltsd()` remains unchanged because it is a decision-state API, not a `Person` accessor.
- Tax donor classes such as `DonorPerson.getDlltsd()` remain unchanged because they are outside this `Person` model batch.
- Health-statistics setters such as `setDhe_mcs_mean(...)` remain unchanged because they do not expose `Person` fields.

Social-care fields were not included in this batch. Handle them in a separate social-care batch if needed.

## Completed: Person Income Fields

Completed income-related accessor renames in `src/main/java/simpaths/model/Person.java` and Java call sites under `src/main/java`.

Completed renames:

| Old method | New method | Backing field |
| --- | --- | --- |
| `getyNonBenPersGrossMonth()` | `getYNonBenPersGrossMonth()` | `yNonBenPersGrossMonth` |
| `setyNonBenPersGrossMonth(...)` | `setYNonBenPersGrossMonth(...)` | `yNonBenPersGrossMonth` |
| `getyNonBenPersGrossMonthL1()` | `getYNonBenPersGrossMonthL1()` | `yNonBenPersGrossMonthL1` |
| `getyMiscPersGrossMonth()` | `getYMiscPersGrossMonth()` | `yMiscPersGrossMonth` |
| `getyMiscPersGrossMonthL1()` | `getYMiscPersGrossMonthL1()` | `yMiscPersGrossMonthL1` |
| `getyCapitalPersMonth()` | `getYCapitalPersMonth()` | `yCapitalPersMonth` |
| `getyPensPersGrossMonth()` | `getYPensPersGrossMonth()` | `yPensPersGrossMonth` |
| `getyEmpPersGrossMonth()` | `getYEmpPersGrossMonth()` | `yEmpPersGrossMonth` |
| `setyEmpPersGrossMonth(...)` | `setYEmpPersGrossMonth(...)` | `yEmpPersGrossMonth` |
| `getyEmpPersGrossMonthL1()` | `getYEmpPersGrossMonthL1()` | `yEmpPersGrossMonthL1` |
| `getyEmpPersGrossMonthL2()` | `getYEmpPersGrossMonthL2()` | `yEmpPersGrossMonthL2` |
| `getyEmpPersGrossMonthL3()` | `getYEmpPersGrossMonthL3()` | `yEmpPersGrossMonthL3` |
| `setYptciihs_dv(...)` | `setYMiscPersGrossMonth(...)` | `yMiscPersGrossMonth` |
| `getYnbcpdf_dv_lag1()` | `getYPersAndPartnerGrossDiffMonthL1()` | `yPersAndPartnerGrossDiffMonthL1` |
| `setYnbcpdf_dv_lag1(...)` | `setYPersAndPartnerGrossDiffMonthL1(...)` | `yPersAndPartnerGrossDiffMonthL1` |
| `setYpnbihs_dv_lag1(...)` | `setYNonBenPersGrossMonthL1(...)` | `yNonBenPersGrossMonthL1` |

Validation:

- Compile command: `mvn test -DskipTests`
- Result: passed
- Focused test command: `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test`
- Focused test result: passed, 29 tests run
- Remaining old income method references in Java source/tests/XML/properties: none
- Reflective getter strings updated: `getYNonBenPersGrossMonth`, `getYCapitalPersMonth`, `getYPensPersGrossMonth`, `getYMiscPersGrossMonth`, and `getYEmpPersGrossMonth`
- Compatibility wrappers: none retained

The `Lag` annotation example was also updated from `getyEmpPersGrossMonth` to `getYEmpPersGrossMonth`.

## Completed: Person Demographic Fields

Completed demographic-related accessor renames in `src/main/java/simpaths/model/Person.java` and Java call sites under `src/main/java`.

Completed renames:

| Old method | New method | Backing field |
| --- | --- | --- |
| `getDot01()` | `getDemEthnC6()` | `demEthnC6` |
| `setDot01(...)` | `setDemEthnC6(...)` | `demEthnC6` |
| `getDcpst()` | `getDemPartnerStatus()` | computed from partner state or `i_demPartnerStatus` |
| `setDcpstLocal(...)` | `setDemPartnerStatusLocal(...)` | `i_demPartnerStatus` |
| `getDcpagdf()` | `getDemAgePartnerDiff()` | computed from `demAge` and partner `demAge` |
| `setDcpst_lag1(...)` | `setDemPartnerStatusL1(...)` | `demPartnerStatusL1` |
| `setDcpagdf_lag1(...)` | `setDemAgePartnerDiffL1(...)` | `demAgePartnerDiffL1` |

Validation:

- Compile command: `mvn test -DskipTests`
- Result: passed
- Focused test command: `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test`
- Focused test result: passed, 29 tests run
- Remaining old `Person` demographic method references in Java source/tests/XML/properties: none
- Reflective getter strings updated: `@Lag(getter="getDemPartnerStatus")`
- Compatibility wrappers: none retained

Intentional retained names:

- `States.getDcpst()` remains unchanged because it is a decision-state API, not a `Person` accessor.

## Completed: Remaining Person Education And Social-Care Flags

Completed remaining `Person` accessor renames that still used survey-code-like names or did not yet match backing fields. Social-care methods used the strict field-name rule for consistency.

Completed renames:

| Old method | New method | Backing field |
| --- | --- | --- |
| `getDer()` | `getEduReturnFlag()` | `eduReturnFlag` |
| `setDer(...)` | `setEduReturnFlag(...)` | `eduReturnFlag` |
| `getSedex()` | `getEduExitSampleFlag()` | `eduExitSampleFlag` |
| `setSedex(...)` | `setEduExitSampleFlag(...)` | `eduExitSampleFlag` |
| `getNeedSocialCare()` | `getCareNeedFlag()` | `careNeedFlag` |
| `setNeedSocialCare(...)` | `setCareNeedFlag(...)` | `careNeedFlag` |
| `setSocialCareFromOther(...)` | `setCareFromInformalFlag(...)` | `careFromInformalFlag` |
| `setCareHoursFromOtherWeekly_lag1(...)` | `setCareHrsInformalWeekL1(...)` | `careHrsInformalWeekL1` |
| `setCareHoursFromFormalWeekly_lag1(...)` | `setCareHrsFormalWeekL1(...)` | `careHrsFormalWeekL1` |
| `setSocialCareProvision_lag1(...)` | `setCareProvidedFlagL1(...)` | `careProvidedFlagL1` |

Validation:

- Compile command: `mvn test -DskipTests`
- Result: passed
- Focused test command: `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test`
- Focused test result: passed, 29 tests run
- Remaining old method references in Java source/tests/XML/properties: none
- Compatibility wrappers: none retained

## Completed: BenefitUnit Accessors

Completed broad `BenefitUnit` accessor pass in small controlled batches rather than renaming every accessor at once.

Inspection notes:

- `BenefitUnit` has many computed/domain accessors that already read naturally and should not be renamed just because they do not map one-to-one to a field. Examples: `getWeight()`, `getChildren()`, `getOccupancy()`, `getCoupleBoolean()`, `getRefPersonForDecisions()`, and regression helper methods.
- Prioritise methods that expose a backing field but still use survey-code names, inconsistent lag suffixes, or temporary/proxy names.
- For proxy/regression-state setters backed by transient `i_...` fields, prefer the strict `I_...` field-name form.
- Update `@Lag(getter=...)` strings in the same batch as the getter rename.

Completion checkpoint:

- Completed batches: local proxy state; children and poverty lags; household income and quintile fields; wealth and costs.
- Compile validation: `mvn test -DskipTests` passed after each `BenefitUnit` batch.
- Focused checkpoint command: `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test`
- Focused checkpoint result: passed on 2026-04-28, 29 tests run, 0 failures, 0 errors, 0 skipped.
- Compatibility wrappers: none retained for the renamed `BenefitUnit` APIs.
- Deliberately retained APIs: computed/domain `BenefitUnit` methods such as `getRegion()`, `getOccupancy()`, `getDemCompHhC4()`, and non-`BenefitUnit` similarly named APIs documented below.

### Completed: BenefitUnit Local Proxy State

These are used by `Expectations` to initialise proxy objects for decision/regression evaluation. The final names keep the `I_` prefix because the backing fields begin with `i_`.

Completed renames:

| Old method | New method | Backing field |
| --- | --- | --- |
| `setDeh_c4Local(...)` | `setI_eduHighestC4(...)` | `i_eduHighestC4` |
| `setOccupancyLocal(...)` | `setI_demOccupancy(...)` | `i_demOccupancy` |
| `setLabourHoursWeekly1Local(...)` | `setI_labHrsWork1Week(...)` | `i_labHrsWork1Week` |
| `setLabourHoursWeekly2Local(...)` | `setI_labHrsWork2Week(...)` | `i_labHrsWork2Week` |
| `setYearLocal(...)` | `setI_demYear(...)` | `i_demYear` |

Validation:

- Compile command: `mvn test -DskipTests`
- Result: passed
- Focused test command: `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test`
- Focused test result: passed, 29 tests run
- Remaining old `BenefitUnit` local proxy method references in Java source/tests/XML/properties: none

Intentional retained names:

- `Person` proxy setters backed by `i_...` fields are handled separately in the planned `Person Local Proxy Setters` batch.

### Completed: BenefitUnit Children And Poverty Lags

These methods exposed lag fields but used `_lag1` names or compact child-age names. They were renamed to backing-field style.

Completed renames:

| Old method | New method | Backing field |
| --- | --- | --- |
| `getNumberChildrenAll_lag1()` | `getNumberChildrenAllL1()` | `numberChildrenAll_lag1` |
| `getNumberChildren02_lag1()` | `getNumberChildren02L1()` | `numberChildren02_lag1` |
| `getIndicatorChildren03_lag1()` | `getDem0to3L1()` | `dem0to3L1` |
| `getIndicatorChildren412_lag1()` | `getDem4to12L1()` | `dem4to12L1` |
| `getAtRiskOfPoverty_lag1()` | `getYPvrtyFlagL1()` | `yPvrtyFlagL1` |
| `getAtRiskOfPoverty()` | `getYPvrtyFlag()` | `yPvrtyFlag`; updated `@Lag(getter = "getYPvrtyFlag")`. |
| `setAtRiskOfPoverty(...)` | `setYPvrtyFlag(...)` | `yPvrtyFlag` |

Validation:

- Compile command: `mvn test -DskipTests`
- Result: passed
- Focused test command: `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test`
- Focused test result: passed, 29 tests run
- Remaining old `BenefitUnit` children/poverty method references in Java source/tests/XML/properties: none
- Compatibility wrappers: none retained

Intentional retained names:

- `Person.getAtRiskOfPoverty()` remains unchanged because it is a person-level derived API that delegates to `benefitUnit.getYPvrtyFlag()`.
- `Person.class, "getAtRiskOfPoverty"` reflective strings in poverty plot setup remain unchanged because they target the retained `Person` method, not `BenefitUnit`.

### Completed: BenefitUnit Household Income And Quintile Fields

These were mostly survey-code-like names or temporary names around household income/quintiles.

Completed renames:

| Old method | New method | Backing field |
| --- | --- | --- |
| `getYdses_c5()` | `getYHhQuintilesMonthC5()` | `yHhQuintilesMonthC5` |
| `getYdses_c5_lag1()` | `getYHhQuintilesMonthC5L1()` | `yHhQuintilesMonthC5L1` |
| `getTmpHHYpnbihs_dv_asinhNoNull()` | `getI_yNonBenHhGrossAsinhNoNull()` | `i_yNonBenHhGrossAsinh` |
| `getTmpHHYpnbihs_dv_asinh()` | `getI_yNonBenHhGrossAsinh()` | `i_yNonBenHhGrossAsinh` |
| `setTmpHHYpnbihs_dv_asinh(...)` | `setI_yNonBenHhGrossAsinh(...)` | `i_yNonBenHhGrossAsinh` |

Related private helper renames:

- `Person.getYdses_c5_lag1()` -> `Person.getYHhQuintilesMonthC5L1()`
- `Person.getYdses_c5_current()` -> `Person.getYHhQuintilesMonthC5Current()`

Validation:

- Compile command: `mvn test -DskipTests`
- Result: passed
- Focused checkpoint command: `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test`
- Focused checkpoint result: passed on 2026-04-28, 29 tests run
- Remaining old household income/quintile method references in Java source/tests/XML/properties: none
- Compatibility wrappers: none retained

Intentional retained names:

- Statistics percentile methods such as `getYdses_p20()` remain unchanged because they are not `BenefitUnit` field accessors in this batch.

### Completed: BenefitUnit Wealth And Costs

These names were domain-readable but did not match backing fields. They were renamed using the strict field-name rule for consistency with the rest of this branch.

Completed renames:

| Old method | New method | Backing field |
| --- | --- | --- |
| `getLiquidWealth(...)` | `getWealthTotValue(...)` | `wealthTotValue` |
| `getPensionWealth(...)` | `getWealthPensValue(...)` | `wealthPensValue` |
| `setPensionWealth(...)` | `setWealthPensValue(...)` | `wealthPensValue` |
| `getHousingWealth(...)` | `getWealthPrptyValue(...)` | `wealthPrptyValue` |
| `setHousingWealth(...)` | `setWealthPrptyValue(...)` | `wealthPrptyValue` |
| `getChildcareCostPerWeek(...)` | `getXChildCareWeek(...)` | `xChildCareWeek` |
| `getSocialCareCostPerWeek(...)` | `getXCareWeek(...)` | `xCareWeek` |

Validation:

- Compile command: `mvn test -DskipTests`
- Result: passed
- Focused checkpoint command: `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test`
- Focused checkpoint result: passed on 2026-04-28, 29 tests run
- Compatibility wrappers: none retained

Intentional retained names:

- `Person.getLiquidWealth()` remains unchanged because it is a person-level wealth accessor.
- `Parameters.getLiquidWealthDiscount(...)`, `Parameters.getPensionWealthDiscount(...)`, and `Parameters.getHousingWealthDiscount(...)` remain unchanged because they are parameter APIs, not `BenefitUnit` getters.
- Decision-grid wealth APIs such as `States.getLiquidWealth()` and `WriteGridsBean.getLiquidWealth()` remain unchanged.
- Tax-key methods such as `DonorKeys.getChildcareCostPerWeek()` and `KeyFunction.getChildcareCostPerWeek()` remain unchanged because they are outside the `BenefitUnit` model batch.

### Do Not Rename In Initial BenefitUnit Pass

- `getRegion()` / `setRegion(...)`: already direct and widely used across `Person`, filters, matching, and migration logic.
- `getOccupancy()`: computed domain value, not a direct getter for `i_demOccupancy`.
- `getDemCompHhC4()`: computed household composition matching the existing field and `@Lag` usage.
- Regression enum values such as `MaleLeisure_MaleDeh_c3_Low`: these are model variable IDs, not getter/setter APIs.
- Non-`BenefitUnit` similarly named methods, especially decision-state APIs.

## Completed: Household Accessors

Inspection of `src/main/java/simpaths/model/Household.java` found one clear accessor mismatch. The class is small, and most accessors are either computed/domain APIs or framework identifiers.

### Completed: Household Original ID Accessor

Completed rename:

| Old method | New method | Backing field |
| --- | --- | --- |
| `getIdOriginalHH()` | `getIdHhOriginal()` | `idHhOriginal` |

Validation:

- Compile command: `mvn test -DskipTests`
- Result: passed
- Remaining `Household.getIdOriginalHH()` references in Java source/tests/XML/properties: none
- Compatibility wrappers: none retained

Do not rename in this pass:

- `getId()` returns the simulation/panel key ID, not `idHhOriginal`.
- `getBenefitUnits()` matches the `benefitUnits` collection.
- `setProcessed(...)` sets `processed` and also updates `key.workingId`; the current name is appropriate for the backing relationship.
- `setHouseholdIdCounter(...)` is a static counter API, not an instance-field accessor.
- `getWeight()`, `setWeight(...)`, and `getEquivalisedDisposableIncomeYearly()` are computed/domain APIs.

## Completed: Original Household ID Accessors

After completing `Household`, the same field-name mismatch remained in nearby model classes. This batch was coherent, low-risk, and limited to copy-constructor/internal call sites.

Completed renames:

| Old method | New method | Backing field | Class |
| --- | --- | --- | --- |
| `getIdOriginalHH()` | `getIdHhOriginal()` | `idHhOriginal` | `Person` |
| `getIdOriginalHH()` | `getIdHhOriginal()` | `idHhOriginal` | `BenefitUnit` |

Validation:

- Compile command: `mvn test -DskipTests`
- Result: passed
- Local focused checkpoint command: `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test`
- Local focused checkpoint result: passed on 2026-04-28 at 14:58:36, 29 tests run, 0 failures, 0 errors, 0 skipped
- Remaining `getIdOriginalHH()` references in Java source/tests/XML/properties: none
- Compatibility wrappers: none retained

## Completed: Original Person And Benefit-Unit ID Accessors

Completed remaining original-ID accessors in `Person` and `BenefitUnit`. They had direct backing-field mismatches and limited constructor call sites.

Completed renames:

| Old method | New method | Backing field | Class |
| --- | --- | --- | --- |
| `getIdOriginalPerson()` | `getIdPersOriginal()` | `idPersOriginal` | `Person` |
| `getIdOriginalBU()` | `getIdBuOriginal()` | `idBuOriginal` | `Person` |
| `getIdOriginalBU()` | `getIdBuOriginal()` | `idBuOriginal` | `BenefitUnit` |

Validation:

- Compile command: `mvn test -DskipTests`
- Result: passed
- Local focused checkpoint command: `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test`
- Local focused checkpoint result: passed on 2026-04-28 at 14:58:36, 29 tests run, 0 failures, 0 errors, 0 skipped
- Remaining old original person/benefit-unit ID method references in Java source/tests/XML/properties: none
- Compatibility wrappers: none retained

## Completed: ID Accessor Checkpoint

The ID accessor batches for `Household`, `Person`, and `BenefitUnit` have a shared focused validation checkpoint.

- Scope: `Household.getIdHhOriginal()`, `Person.getIdHhOriginal()`, `BenefitUnit.getIdHhOriginal()`, `Person.getIdPersOriginal()`, `Person.getIdBuOriginal()`, and `BenefitUnit.getIdBuOriginal()`.
- Local focused checkpoint command: `mvn "-Dtest=PersonTest,EmploymentHistoryFilterTest,EmploymentStatisticsTest" test`
- Local focused checkpoint result: passed on 2026-04-28 at 14:58:36, 29 tests run, 0 failures, 0 errors, 0 skipped
- Maven result: `BUILD SUCCESS`

## Planned: Remaining Person Underscore-Style Accessors

Inspection of `src/main/java/simpaths/model/Person.java` found remaining public accessors with underscores. Split them into direct field-backed renames and computed helper APIs. Direct field-backed methods can continue through the normal branch workflow. Computed helpers are labelled "to be determined" because they do not have a clear one-to-one backing-field rename.

### Planned: Person Direct Field-Backed Residuals

These have clear backing fields and can be renamed under the strict field-name rule.

| Current method | New method | Backing field |
| --- | --- | --- |
| `getHousehold_status_lag()` | `getDemStatusHhL1()` | `demStatusHhL1` |
| `setNewWorkHours_lag1(...)` | `setLabHrsWorkNewL1(...)` | `labHrsWorkNewL1` |

Expected call-site area:

- `BenefitUnit` Covid/labour-supply code.
- Internal `Person` regressor switch.

### Planned: Person Benefit Receipt Lag Flags

| Current method | New method | Backing field |
| --- | --- | --- |
| `setReceivesBenefitsFlag_L1(...)` | `setYBenReceivedFlagL1(...)` | `yBenReceivedFlagL1` |
| `setReceivesBenefitsFlagUC_L1(...)` | `setYBenUCReceivedFlagL1(...)` | `yBenUCReceivedFlagL1` |
| `setReceivesBenefitsFlagNonUC_L1(...)` | `setYBenNonUCReceivedFlagL1(...)` | `yBenNonUCReceivedFlagL1` |

Excluded from this batch:

- Boolean `is...` methods such as `isReceivesBenefitsFlag_L1()`, `isReceivesBenefitsFlagUC_L1()`, and `isReceivesBenefitsFlagNonUC_L1()` are excluded because the current branch scope is getter/setter method names, not boolean predicate APIs.

### Planned: Person Local Proxy Setters

These are used by decision expectation proxy objects. Under the strict `i_` rule, methods for fields beginning with `i_` should keep the `I_` prefix.

| Current method | New method | Backing field |
| --- | --- | --- |
| `setRegionLocal(...)` | `setI_demRgn(...)` | `i_demRgn` |
| `setDemPartnerStatusLocal(...)` | `setI_demPartnerStatus(...)` | `i_demPartnerStatus` |
| `setYearLocal(...)` | `setI_demYear(...)` | `i_demYear` |
| `setNumberChildren017Local(...)` | `setI_demNchild0to17(...)` | `i_demNchild0to17` |
| `setIndicatorChildren02Local(...)` | `setI_demNChild0to2(...)` | `i_demNChild0to2` |
| `setNumberChildrenAllLocal(...)` | `setI_demNchild(...)` | `i_demNchild` |
| `setI_yHhQuintilesC5(...)` | no rename needed | `i_yHhQuintilesC5` |
| `setI_demCompHhC4L1(...)` | no rename needed | `i_demCompHhC4L1` |
| `setNumberChildrenAllLocal_lag1(...)` | `setI_demNchildL1(...)` | `i_demNchildL1` |
| `setNumberChildren02Local_lag1(...)` | `setI_demNchild0to2L1(...)` | `i_demNchild0to2L1` |

Expected call-site area:

- `Expectations`.
- `ExpectationsFactory`.

### To Be Determined: Person Computed Helper APIs

These methods do not directly expose a backing field or they add fallback/defaulting/domain logic. Do not rename them until the team agrees a naming rule for computed helper methods.

| Current method | Reason to defer |
| --- | --- |
| `getEmployed_Lag1()` | computed indicator from `labC4L1`, not a direct `employedL1` field |
| `getNonwork_Lag1()` | computed indicator from `labC4L1`, not a direct `nonworkL1` field |
| `getCovidModuleGrossLabourIncome_Baseline()` | returns `covidYLabGross` with defaulting; no `covidYLabGrossBaseline` backing field |
| `setCovidModuleGrossLabourIncome_Baseline(...)` | sets `covidYLabGross`, but the "Baseline" method name is domain-specific |
| `getHoursFormalSocialCare_L1()` | returns `careHrsFormalWeekL1` with zero floor/defaulting |
| `getHoursInformalSocialCare_L1()` | returns `careHrsInformalWeekL1` with zero floor/defaulting |
| `getTotalHoursSocialCare_L1()` | computed formal + informal lag hours |
| `getCareHoursFromParent_L1()` | computed helper, currently zero |
| `getCareHoursFromPartner_L1()` | computed from partner status and informal lag hours |
| `getCareHoursFromDaughter_L1()` | computed helper, currently zero |
| `getCareHoursFromSon_L1()` | computed helper, currently zero |
| `getCareHoursFromOther_L1()` | computed from partner status and informal lag hours |
| `getYnbcpdf_dv()` | computed current difference between own and partner non-benefit gross personal income; used as a `@Lag` source |

## Suggested Batch Order

1. `Person` education fields.
2. `Person` labour status fields.
3. `Person` health fields.
4. `Person` income fields.
5. `Person` demographic fields.
6. `BenefitUnit` accessors.
7. Other model classes.
8. Filters, statistics, and decision-model call sites that remain after model class batches.

Keep each batch small enough that compile errors clearly point to missed call sites from that batch.

## Validation

After each batch:

```powershell
mvn test
```

If the full test suite is too slow, at minimum run the normal compile target used by the project:

```powershell
mvn test -DskipTests
```

Then search for old method names, for example:

```powershell
rg -n "getDeh_c4|setDeh_c4|setDeh_c4_lag1" src/main/java
```

If `rg` is unavailable, use PowerShell:

```powershell
Get-ChildItem -Path src/main/java -Recurse -Filter *.java |
    Select-String -Pattern "getDeh_c4|setDeh_c4|setDeh_c4_lag1"
```

## Review Checklist

- [ ] Method declaration renamed.
- [ ] Java call sites renamed.
- [ ] No unintended resource, SQL, input-column, or survey-code changes.
- [ ] Old method name no longer appears in Java source, unless deliberately retained as a temporary deprecated wrapper.
- [ ] Project compiles.
- [ ] Relevant tests pass or skipped tests are explicitly noted.

## Temporary Compatibility Wrappers

Prefer complete renames without wrappers on this branch.

Only add deprecated wrapper methods if another active branch or external integration needs temporary compatibility:

```java
@Deprecated
public Education getDeh_c4() {
    return getEduHighestC4();
}
```

Remove wrappers before the final cleanup unless there is a documented reason to keep them.
