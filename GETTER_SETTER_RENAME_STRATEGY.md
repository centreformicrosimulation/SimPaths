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
| Remaining `Person` education/social-care flags | Planned | Not run | Candidate method list below. |

## Naming Convention

Use standard JavaBean-style accessor names:

- `fieldName` -> `getFieldName()` / `setFieldName(...)`
- Boolean fields may use either the existing project convention or `isFieldName()` if already established locally.
- Lag fields should preserve the field suffix:
  - `eduHighestC4L1` -> `getEduHighestC4L1()` / `setEduHighestC4L1(...)`
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

## Planned: Remaining Person Education And Social-Care Flags

Candidate remaining `Person` accessors that still use survey-code-like names or do not yet match backing fields.

| Current method | New method | Backing field |
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

Before starting this batch, check whether social-care method names should use the `care...` field names exactly or retain the more descriptive `SocialCare...` terms for readability.

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
