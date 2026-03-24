param(
    [string]$RunOutputDir = "D:\CeMPA\SimPaths\output\20260318092643_606_0\csv",
    [string]$OutputDir = "D:\CeMPA\SimPaths"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName Microsoft.VisualBasic

function New-Aggregate {
    @{
        pool_count = 0
        entered_count = 0
        matched_count = 0
        unmatched_count = 0
        new_entrant_count = 0
        carry_over_entrant_count = 0
        unmatched_years_at_entry_sum = 0.0
        unmatched_years_at_entry_count = 0
        continuous_unmatched_years_sum = 0.0
        continuous_unmatched_years_count = 0
    }
}

function New-PressureAggregate {
    @{
        pool_count = 0
        entered_count = 0
        matched_count = 0
        unmatched_count = 0
        desired_age_diff_sum = 0.0
        desired_age_diff_count = 0
        desired_earnings_diff_sum = 0.0
        desired_earnings_diff_count = 0
        eligible_same_region_sum = 0.0
        eligible_same_region_count = 0
        eligible_all_regions_sum = 0.0
        eligible_all_regions_count = 0
        best_age_mismatch_same_region_sum = 0.0
        best_age_mismatch_same_region_count = 0
        best_age_mismatch_all_regions_sum = 0.0
        best_age_mismatch_all_regions_count = 0
        best_earnings_mismatch_same_region_sum = 0.0
        best_earnings_mismatch_same_region_count = 0
        best_earnings_mismatch_all_regions_sum = 0.0
        best_earnings_mismatch_all_regions_count = 0
        best_score_same_region_sum = 0.0
        best_score_same_region_count = 0
        best_score_all_regions_sum = 0.0
        best_score_all_regions_count = 0
    }
}

function Get-OrCreateAggregate {
    param(
        [hashtable]$Store,
        [string]$Key,
        [scriptblock]$Factory
    )

    if (-not $Store.ContainsKey($Key)) {
        $Store[$Key] = & $Factory
    }
    return $Store[$Key]
}

function Parse-Bool {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value) -or $Value -eq "null" -or $Value -eq "None") {
        return $false
    }
    return [System.Convert]::ToBoolean($Value)
}

function Parse-NullableDouble {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value) -or $Value -eq "null" -or $Value -eq "None") {
        return $null
    }
    return [double]::Parse($Value, [System.Globalization.CultureInfo]::InvariantCulture)
}

function Parse-NullableInt {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value) -or $Value -eq "null" -or $Value -eq "None") {
        return $null
    }
    return [int]::Parse($Value, [System.Globalization.CultureInfo]::InvariantCulture)
}

function Format-NullableAverage {
    param(
        [double]$Sum,
        [int]$Count
    )

    if ($Count -eq 0) {
        return $null
    }
    return [math]::Round($Sum / $Count, 6)
}

function Add-BasicStats {
    param(
        [hashtable]$Aggregate,
        [bool]$Entered,
        [bool]$Matched,
        [bool]$Unmatched,
        [bool]$NewEntrant,
        [bool]$CarryOverEntrant,
        [Nullable[int]]$UnmatchedYearsAtEntry,
        [Nullable[int]]$ContinuousUnmatchedYears
    )

    $Aggregate.pool_count++
    if ($Entered) { $Aggregate.entered_count++ }
    if ($Matched) { $Aggregate.matched_count++ }
    if ($Unmatched) { $Aggregate.unmatched_count++ }
    if ($NewEntrant) { $Aggregate.new_entrant_count++ }
    if ($CarryOverEntrant) { $Aggregate.carry_over_entrant_count++ }
    if ($null -ne $UnmatchedYearsAtEntry) {
        $Aggregate.unmatched_years_at_entry_sum += $UnmatchedYearsAtEntry
        $Aggregate.unmatched_years_at_entry_count++
    }
    if ($null -ne $ContinuousUnmatchedYears) {
        $Aggregate.continuous_unmatched_years_sum += $ContinuousUnmatchedYears
        $Aggregate.continuous_unmatched_years_count++
    }
}

function Add-PressureStats {
    param(
        [hashtable]$Aggregate,
        [bool]$Entered,
        [bool]$Matched,
        [bool]$Unmatched,
        [Nullable[double]]$DesiredAgeDiff,
        [Nullable[double]]$DesiredEarningsDiff,
        [Nullable[int]]$EligiblePartnersSameRegion,
        [Nullable[int]]$EligiblePartnersAllRegions,
        [Nullable[double]]$BestAgeMismatchSameRegion,
        [Nullable[double]]$BestAgeMismatchAllRegions,
        [Nullable[double]]$BestEarningsMismatchSameRegion,
        [Nullable[double]]$BestEarningsMismatchAllRegions,
        [Nullable[double]]$BestScoreSameRegion,
        [Nullable[double]]$BestScoreAllRegions
    )

    $Aggregate.pool_count++
    if ($Entered) { $Aggregate.entered_count++ }
    if ($Matched) { $Aggregate.matched_count++ }
    if ($Unmatched) { $Aggregate.unmatched_count++ }

    if ($null -ne $DesiredAgeDiff) {
        $Aggregate.desired_age_diff_sum += $DesiredAgeDiff
        $Aggregate.desired_age_diff_count++
    }
    if ($null -ne $DesiredEarningsDiff) {
        $Aggregate.desired_earnings_diff_sum += $DesiredEarningsDiff
        $Aggregate.desired_earnings_diff_count++
    }
    if ($null -ne $EligiblePartnersSameRegion) {
        $Aggregate.eligible_same_region_sum += $EligiblePartnersSameRegion
        $Aggregate.eligible_same_region_count++
    }
    if ($null -ne $EligiblePartnersAllRegions) {
        $Aggregate.eligible_all_regions_sum += $EligiblePartnersAllRegions
        $Aggregate.eligible_all_regions_count++
    }
    if ($null -ne $BestAgeMismatchSameRegion) {
        $Aggregate.best_age_mismatch_same_region_sum += $BestAgeMismatchSameRegion
        $Aggregate.best_age_mismatch_same_region_count++
    }
    if ($null -ne $BestAgeMismatchAllRegions) {
        $Aggregate.best_age_mismatch_all_regions_sum += $BestAgeMismatchAllRegions
        $Aggregate.best_age_mismatch_all_regions_count++
    }
    if ($null -ne $BestEarningsMismatchSameRegion) {
        $Aggregate.best_earnings_mismatch_same_region_sum += $BestEarningsMismatchSameRegion
        $Aggregate.best_earnings_mismatch_same_region_count++
    }
    if ($null -ne $BestEarningsMismatchAllRegions) {
        $Aggregate.best_earnings_mismatch_all_regions_sum += $BestEarningsMismatchAllRegions
        $Aggregate.best_earnings_mismatch_all_regions_count++
    }
    if ($null -ne $BestScoreSameRegion) {
        $Aggregate.best_score_same_region_sum += $BestScoreSameRegion
        $Aggregate.best_score_same_region_count++
    }
    if ($null -ne $BestScoreAllRegions) {
        $Aggregate.best_score_all_regions_sum += $BestScoreAllRegions
        $Aggregate.best_score_all_regions_count++
    }
}

function Convert-BasicAggregatesToRows {
    param(
        [hashtable]$Store,
        [string[]]$DimensionNames
    )

    foreach ($entry in $Store.GetEnumerator() | Sort-Object Name) {
        $parts = $entry.Key -split '\|', ($DimensionNames.Count + 1)
        $aggregate = $entry.Value
        $row = [ordered]@{}
        for ($i = 0; $i -lt $DimensionNames.Count; $i++) {
            $row[$DimensionNames[$i]] = $parts[$i]
        }
        $row["pool_count"] = $aggregate.pool_count
        $row["entered_count_raw"] = $aggregate.entered_count
        $row["matched_count"] = $aggregate.matched_count
        $row["unmatched_count"] = $aggregate.unmatched_count
        $row["new_entrant_count"] = $aggregate.new_entrant_count
        $row["carry_over_entrant_count"] = $aggregate.carry_over_entrant_count
        $row["matched_share_of_pool"] = if ($aggregate.pool_count -gt 0) { [math]::Round($aggregate.matched_count / $aggregate.pool_count, 6) } else { $null }
        $row["unmatched_share_of_pool"] = if ($aggregate.pool_count -gt 0) { [math]::Round($aggregate.unmatched_count / $aggregate.pool_count, 6) } else { $null }
        $row["matched_share_of_entered_raw"] = if ($aggregate.entered_count -gt 0) { [math]::Round($aggregate.matched_count / $aggregate.entered_count, 6) } else { $null }
        $row["unmatched_share_of_entered_raw"] = if ($aggregate.entered_count -gt 0) { [math]::Round($aggregate.unmatched_count / $aggregate.entered_count, 6) } else { $null }
        $row["avg_unmatched_years_at_entry"] = Format-NullableAverage -Sum $aggregate.unmatched_years_at_entry_sum -Count $aggregate.unmatched_years_at_entry_count
        $row["avg_continuous_unmatched_years_current"] = Format-NullableAverage -Sum $aggregate.continuous_unmatched_years_sum -Count $aggregate.continuous_unmatched_years_count
        [pscustomobject]$row
    }
}

function Convert-PressureAggregatesToRows {
    param([hashtable]$Store)

    foreach ($entry in $Store.GetEnumerator() | Sort-Object Name) {
        $parts = $entry.Key -split '\|', 3
        $aggregate = $entry.Value
        [pscustomobject][ordered]@{
            year = $parts[0]
            entry_type = $parts[1]
            outcome = $parts[2]
            pool_count = $aggregate.pool_count
            entered_count_raw = $aggregate.entered_count
            matched_count = $aggregate.matched_count
            unmatched_count = $aggregate.unmatched_count
            avg_desired_age_diff = Format-NullableAverage -Sum $aggregate.desired_age_diff_sum -Count $aggregate.desired_age_diff_count
            avg_desired_earnings_diff = Format-NullableAverage -Sum $aggregate.desired_earnings_diff_sum -Count $aggregate.desired_earnings_diff_count
            avg_eligible_partners_same_region = Format-NullableAverage -Sum $aggregate.eligible_same_region_sum -Count $aggregate.eligible_same_region_count
            avg_eligible_partners_all_regions = Format-NullableAverage -Sum $aggregate.eligible_all_regions_sum -Count $aggregate.eligible_all_regions_count
            avg_best_age_mismatch_same_region = Format-NullableAverage -Sum $aggregate.best_age_mismatch_same_region_sum -Count $aggregate.best_age_mismatch_same_region_count
            avg_best_age_mismatch_all_regions = Format-NullableAverage -Sum $aggregate.best_age_mismatch_all_regions_sum -Count $aggregate.best_age_mismatch_all_regions_count
            avg_best_earnings_mismatch_same_region = Format-NullableAverage -Sum $aggregate.best_earnings_mismatch_same_region_sum -Count $aggregate.best_earnings_mismatch_same_region_count
            avg_best_earnings_mismatch_all_regions = Format-NullableAverage -Sum $aggregate.best_earnings_mismatch_all_regions_sum -Count $aggregate.best_earnings_mismatch_all_regions_count
            avg_best_score_same_region = Format-NullableAverage -Sum $aggregate.best_score_same_region_sum -Count $aggregate.best_score_same_region_count
            avg_best_score_all_regions = Format-NullableAverage -Sum $aggregate.best_score_all_regions_sum -Count $aggregate.best_score_all_regions_count
        }
    }
}

function Normalize-TimeToYear {
    param([string]$TimeValue)

    if ([string]::IsNullOrWhiteSpace($TimeValue)) {
        return $TimeValue
    }
    return [int][double]::Parse($TimeValue, [System.Globalization.CultureInfo]::InvariantCulture)
}

$personPath = Join-Path $RunOutputDir "Person.csv"
$benefitUnitPath = Join-Path $RunOutputDir "BenefitUnit.csv"

if (-not (Test-Path $personPath)) {
    throw "Person.csv not found: $personPath"
}
if (-not (Test-Path $benefitUnitPath)) {
    throw "BenefitUnit.csv not found: $benefitUnitPath"
}

Write-Host "Loading BenefitUnit region map from $benefitUnitPath"
$benefitUnitRegionByKey = @{}
$benefitParser = New-Object Microsoft.VisualBasic.FileIO.TextFieldParser($benefitUnitPath)
$benefitParser.TextFieldType = [Microsoft.VisualBasic.FileIO.FieldType]::Delimited
$benefitParser.SetDelimiters(",")
$benefitParser.HasFieldsEnclosedInQuotes = $true
$benefitHeader = $benefitParser.ReadFields()
$benefitIndex = @{}
for ($i = 0; $i -lt $benefitHeader.Length; $i++) {
    $benefitIndex[$benefitHeader[$i]] = $i
}
while (-not $benefitParser.EndOfData) {
    $fields = $benefitParser.ReadFields()
    $time = Normalize-TimeToYear $fields[$benefitIndex["time"]]
    $idBenefitUnit = $fields[$benefitIndex["id_BenefitUnit"]]
    $region = $fields[$benefitIndex["region"]]
    $benefitUnitRegionByKey["$time|$idBenefitUnit"] = $region
}
$benefitParser.Close()

$overall = @{}
$ageBand = @{}
$entryType = @{}
$unmatchedStreak = @{}
$sex = @{}
$region = @{}
$pressure = @{}

Write-Host "Streaming Person.csv from $personPath"
$personParser = New-Object Microsoft.VisualBasic.FileIO.TextFieldParser($personPath)
$personParser.TextFieldType = [Microsoft.VisualBasic.FileIO.FieldType]::Delimited
$personParser.SetDelimiters(",")
$personParser.HasFieldsEnclosedInQuotes = $true
$personHeader = $personParser.ReadFields()
$personIndex = @{}
for ($i = 0; $i -lt $personHeader.Length; $i++) {
    $personIndex[$personHeader[$i]] = $i
}

$rowsProcessed = 0
$rowsIncluded = 0
while (-not $personParser.EndOfData) {
    $fields = $personParser.ReadFields()
    $rowsProcessed++

    $year = Normalize-TimeToYear $fields[$personIndex["time"]]
    $entered = Parse-Bool $fields[$personIndex["enteredUnionMatchingThisYear"]]
    $matched = Parse-Bool $fields[$personIndex["matchedUnionMatchingThisYear"]]
    $unmatched = Parse-Bool $fields[$personIndex["unmatchedUnionMatchingThisYear"]]
    $newEntrant = Parse-Bool $fields[$personIndex["newUnionMatchingEntrantThisYear"]]
    $carryOverEntrant = Parse-Bool $fields[$personIndex["carryOverUnionMatchingEntrantThisYear"]]

    if (-not ($entered -or $matched -or $unmatched -or $newEntrant -or $carryOverEntrant)) {
        continue
    }
    $rowsIncluded++

    $ageBandLabel = $fields[$personIndex["unionMatchingAgeBandThisYear"]]
    $sexLabel = $fields[$personIndex["demMaleFlag"]]
    $unmatchedYearsAtEntry = Parse-NullableInt $fields[$personIndex["unionMatchingUnmatchedYearsAtEntryThisYear"]]
    $continuousUnmatchedYears = Parse-NullableInt $fields[$personIndex["unionMatchingContinuousUnmatchedYears"]]
    $idBu = $fields[$personIndex["idBu"]]
    $regionKey = "$year|$idBu"
    $regionLabel = if ($benefitUnitRegionByKey.ContainsKey($regionKey)) { $benefitUnitRegionByKey[$regionKey] } else { "UNKNOWN" }

    $desiredAgeDiff = Parse-NullableDouble $fields[$personIndex["unionMatchingDesiredAgeDiffThisYear"]]
    $desiredEarningsDiff = Parse-NullableDouble $fields[$personIndex["unionMatchingDesiredEarningsDiffThisYear"]]
    $eligibleSameRegion = Parse-NullableInt $fields[$personIndex["unionMatchingEligiblePartnersSameRegionThisYear"]]
    $eligibleAllRegions = Parse-NullableInt $fields[$personIndex["unionMatchingEligiblePartnersAllRegionsThisYear"]]
    $bestAgeMismatchSameRegion = Parse-NullableDouble $fields[$personIndex["unionMatchingBestAgeMismatchSameRegionThisYear"]]
    $bestAgeMismatchAllRegions = Parse-NullableDouble $fields[$personIndex["unionMatchingBestAgeMismatchAllRegionsThisYear"]]
    $bestEarningsMismatchSameRegion = Parse-NullableDouble $fields[$personIndex["unionMatchingBestEarningsMismatchSameRegionThisYear"]]
    $bestEarningsMismatchAllRegions = Parse-NullableDouble $fields[$personIndex["unionMatchingBestEarningsMismatchAllRegionsThisYear"]]
    $bestScoreSameRegion = Parse-NullableDouble $fields[$personIndex["unionMatchingBestScoreSameRegionThisYear"]]
    $bestScoreAllRegions = Parse-NullableDouble $fields[$personIndex["unionMatchingBestScoreAllRegionsThisYear"]]

    $entryTypeLabel = if ($carryOverEntrant) { "carry_over" } elseif ($newEntrant) { "new" } else { "unknown" }
    $streakLabel = if ($null -eq $unmatchedYearsAtEntry) { "unknown" } else { [string]$unmatchedYearsAtEntry }

    foreach ($target in @(
        @{ Store = $overall; Key = "$year" },
        @{ Store = $ageBand; Key = "$year|$ageBandLabel" },
        @{ Store = $entryType; Key = "$year|$entryTypeLabel" },
        @{ Store = $unmatchedStreak; Key = "$year|$streakLabel" },
        @{ Store = $sex; Key = "$year|$sexLabel" },
        @{ Store = $region; Key = "$year|$regionLabel" }
    )) {
        $aggregate = Get-OrCreateAggregate -Store $target.Store -Key $target.Key -Factory ${function:New-Aggregate}
        Add-BasicStats -Aggregate $aggregate -Entered $entered -Matched $matched -Unmatched $unmatched `
            -NewEntrant $newEntrant -CarryOverEntrant $carryOverEntrant `
            -UnmatchedYearsAtEntry $unmatchedYearsAtEntry -ContinuousUnmatchedYears $continuousUnmatchedYears
    }

    $pressureOutcomes = @("all_entered")
    if ($matched) { $pressureOutcomes += "matched" }
    if ($unmatched) { $pressureOutcomes += "unmatched" }
    foreach ($outcome in $pressureOutcomes) {
        $pressureAggregate = Get-OrCreateAggregate -Store $pressure -Key "$year|$entryTypeLabel|$outcome" -Factory ${function:New-PressureAggregate}
        Add-PressureStats -Aggregate $pressureAggregate -Entered $entered -Matched $matched -Unmatched $unmatched `
            -DesiredAgeDiff $desiredAgeDiff -DesiredEarningsDiff $desiredEarningsDiff `
            -EligiblePartnersSameRegion $eligibleSameRegion -EligiblePartnersAllRegions $eligibleAllRegions `
            -BestAgeMismatchSameRegion $bestAgeMismatchSameRegion -BestAgeMismatchAllRegions $bestAgeMismatchAllRegions `
            -BestEarningsMismatchSameRegion $bestEarningsMismatchSameRegion -BestEarningsMismatchAllRegions $bestEarningsMismatchAllRegions `
            -BestScoreSameRegion $bestScoreSameRegion -BestScoreAllRegions $bestScoreAllRegions
    }

    if (($rowsProcessed % 500000) -eq 0) {
        Write-Host "Processed $rowsProcessed person rows; included $rowsIncluded union-matching rows"
    }
}
$personParser.Close()

Write-Host "Processed $rowsProcessed person rows; included $rowsIncluded union-matching rows"

$outputs = @(
    @{ Path = (Join-Path $OutputDir "union_matching_by_year_overall.csv"); Rows = (Convert-BasicAggregatesToRows -Store $overall -DimensionNames @("year")) },
    @{ Path = (Join-Path $OutputDir "union_matching_by_year_age_band.csv"); Rows = (Convert-BasicAggregatesToRows -Store $ageBand -DimensionNames @("year", "age_band")) },
    @{ Path = (Join-Path $OutputDir "union_matching_by_year_entry_type.csv"); Rows = (Convert-BasicAggregatesToRows -Store $entryType -DimensionNames @("year", "entry_type")) },
    @{ Path = (Join-Path $OutputDir "union_matching_by_year_unmatched_streak.csv"); Rows = (Convert-BasicAggregatesToRows -Store $unmatchedStreak -DimensionNames @("year", "unmatched_streak_at_entry")) },
    @{ Path = (Join-Path $OutputDir "union_matching_by_year_sex.csv"); Rows = (Convert-BasicAggregatesToRows -Store $sex -DimensionNames @("year", "sex")) },
    @{ Path = (Join-Path $OutputDir "union_matching_by_year_region.csv"); Rows = (Convert-BasicAggregatesToRows -Store $region -DimensionNames @("year", "region")) },
    @{ Path = (Join-Path $OutputDir "union_matching_by_year_pressure.csv"); Rows = (Convert-PressureAggregatesToRows -Store $pressure) }
)

foreach ($output in $outputs) {
    $output.Rows | Export-Csv -Path $output.Path -NoTypeInformation
    Write-Host "Wrote $($output.Path)"
}
