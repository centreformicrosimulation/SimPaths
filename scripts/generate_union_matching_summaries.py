from __future__ import annotations

import argparse
import csv
from collections import defaultdict
from pathlib import Path
from typing import Dict, Iterable, Optional, Tuple


def parse_bool(value: str) -> bool:
    return str(value).strip().lower() == "true"


def parse_optional_int(value: str) -> Optional[int]:
    text = str(value).strip()
    if text in {"", "null", "None"}:
        return None
    return int(float(text))


def parse_optional_float(value: str) -> Optional[float]:
    text = str(value).strip()
    if text in {"", "null", "None"}:
        return None
    return float(text)


def normalize_year(value: str) -> str:
    return str(int(float(value)))


def new_basic_aggregate() -> Dict[str, float]:
    return {
        "pool_count": 0,
        "entered_count_raw": 0,
        "matched_count": 0,
        "unmatched_count": 0,
        "new_entrant_count": 0,
        "carry_over_entrant_count": 0,
        "unmatched_years_at_entry_sum": 0.0,
        "unmatched_years_at_entry_count": 0,
        "continuous_unmatched_years_sum": 0.0,
        "continuous_unmatched_years_count": 0,
    }


def new_pressure_aggregate() -> Dict[str, float]:
    return {
        "pool_count": 0,
        "entered_count_raw": 0,
        "matched_count": 0,
        "unmatched_count": 0,
        "desired_age_diff_sum": 0.0,
        "desired_age_diff_count": 0,
        "desired_earnings_diff_sum": 0.0,
        "desired_earnings_diff_count": 0,
        "eligible_same_region_sum": 0.0,
        "eligible_same_region_count": 0,
        "eligible_all_regions_sum": 0.0,
        "eligible_all_regions_count": 0,
        "best_age_mismatch_same_region_sum": 0.0,
        "best_age_mismatch_same_region_count": 0,
        "best_age_mismatch_all_regions_sum": 0.0,
        "best_age_mismatch_all_regions_count": 0,
        "best_earnings_mismatch_same_region_sum": 0.0,
        "best_earnings_mismatch_same_region_count": 0,
        "best_earnings_mismatch_all_regions_sum": 0.0,
        "best_earnings_mismatch_all_regions_count": 0,
        "best_score_same_region_sum": 0.0,
        "best_score_same_region_count": 0,
        "best_score_all_regions_sum": 0.0,
        "best_score_all_regions_count": 0,
    }


def add_basic_stats(
    aggregate: Dict[str, float],
    *,
    entered: bool,
    matched: bool,
    unmatched: bool,
    new_entrant: bool,
    carry_over_entrant: bool,
    unmatched_years_at_entry: Optional[int],
    continuous_unmatched_years: Optional[int],
) -> None:
    aggregate["pool_count"] += 1
    aggregate["entered_count_raw"] += int(entered)
    aggregate["matched_count"] += int(matched)
    aggregate["unmatched_count"] += int(unmatched)
    aggregate["new_entrant_count"] += int(new_entrant)
    aggregate["carry_over_entrant_count"] += int(carry_over_entrant)
    if unmatched_years_at_entry is not None:
        aggregate["unmatched_years_at_entry_sum"] += unmatched_years_at_entry
        aggregate["unmatched_years_at_entry_count"] += 1
    if continuous_unmatched_years is not None:
        aggregate["continuous_unmatched_years_sum"] += continuous_unmatched_years
        aggregate["continuous_unmatched_years_count"] += 1


def add_pressure_stats(
    aggregate: Dict[str, float],
    *,
    entered: bool,
    matched: bool,
    unmatched: bool,
    desired_age_diff: Optional[float],
    desired_earnings_diff: Optional[float],
    eligible_same_region: Optional[int],
    eligible_all_regions: Optional[int],
    best_age_mismatch_same_region: Optional[float],
    best_age_mismatch_all_regions: Optional[float],
    best_earnings_mismatch_same_region: Optional[float],
    best_earnings_mismatch_all_regions: Optional[float],
    best_score_same_region: Optional[float],
    best_score_all_regions: Optional[float],
) -> None:
    aggregate["pool_count"] += 1
    aggregate["entered_count_raw"] += int(entered)
    aggregate["matched_count"] += int(matched)
    aggregate["unmatched_count"] += int(unmatched)

    for value, prefix in [
        (desired_age_diff, "desired_age_diff"),
        (desired_earnings_diff, "desired_earnings_diff"),
        (eligible_same_region, "eligible_same_region"),
        (eligible_all_regions, "eligible_all_regions"),
        (best_age_mismatch_same_region, "best_age_mismatch_same_region"),
        (best_age_mismatch_all_regions, "best_age_mismatch_all_regions"),
        (best_earnings_mismatch_same_region, "best_earnings_mismatch_same_region"),
        (best_earnings_mismatch_all_regions, "best_earnings_mismatch_all_regions"),
        (best_score_same_region, "best_score_same_region"),
        (best_score_all_regions, "best_score_all_regions"),
    ]:
        if value is not None:
            aggregate[f"{prefix}_sum"] += value
            aggregate[f"{prefix}_count"] += 1


def avg(sum_value: float, count: float) -> Optional[float]:
    if not count:
        return None
    return round(sum_value / count, 6)


def ratio(numerator: float, denominator: float) -> Optional[float]:
    if not denominator:
        return None
    return round(numerator / denominator, 6)


def build_region_map(benefit_unit_path: Path) -> Dict[Tuple[str, str], str]:
    region_by_key: Dict[Tuple[str, str], str] = {}
    with benefit_unit_path.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            region_by_key[(normalize_year(row["time"]), row["id_BenefitUnit"])] = row["region"]
    return region_by_key


def write_csv(path: Path, rows: Iterable[dict]) -> None:
    rows = list(rows)
    if not rows:
        raise RuntimeError(f"No rows generated for {path}")
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)


def convert_basic_rows(store: Dict[Tuple[str, ...], Dict[str, float]], dimensions: Tuple[str, ...]) -> Iterable[dict]:
    for key in sorted(store):
        aggregate = store[key]
        row = {dimensions[i]: key[i] for i in range(len(dimensions))}
        row.update(
            {
                "pool_count": int(aggregate["pool_count"]),
                "entered_count_raw": int(aggregate["entered_count_raw"]),
                "matched_count": int(aggregate["matched_count"]),
                "unmatched_count": int(aggregate["unmatched_count"]),
                "new_entrant_count": int(aggregate["new_entrant_count"]),
                "carry_over_entrant_count": int(aggregate["carry_over_entrant_count"]),
                "matched_share_of_pool": ratio(aggregate["matched_count"], aggregate["pool_count"]),
                "unmatched_share_of_pool": ratio(aggregate["unmatched_count"], aggregate["pool_count"]),
                "matched_share_of_entered_raw": ratio(aggregate["matched_count"], aggregate["entered_count_raw"]),
                "unmatched_share_of_entered_raw": ratio(aggregate["unmatched_count"], aggregate["entered_count_raw"]),
                "avg_unmatched_years_at_entry": avg(
                    aggregate["unmatched_years_at_entry_sum"], aggregate["unmatched_years_at_entry_count"]
                ),
                "avg_continuous_unmatched_years_current": avg(
                    aggregate["continuous_unmatched_years_sum"], aggregate["continuous_unmatched_years_count"]
                ),
            }
        )
        yield row


def convert_pressure_rows(store: Dict[Tuple[str, ...], Dict[str, float]]) -> Iterable[dict]:
    for key in sorted(store):
        aggregate = store[key]
        yield {
            "year": key[0],
            "entry_type": key[1],
            "outcome": key[2],
            "pool_count": int(aggregate["pool_count"]),
            "entered_count_raw": int(aggregate["entered_count_raw"]),
            "matched_count": int(aggregate["matched_count"]),
            "unmatched_count": int(aggregate["unmatched_count"]),
            "avg_desired_age_diff": avg(aggregate["desired_age_diff_sum"], aggregate["desired_age_diff_count"]),
            "avg_desired_earnings_diff": avg(
                aggregate["desired_earnings_diff_sum"], aggregate["desired_earnings_diff_count"]
            ),
            "avg_eligible_partners_same_region": avg(
                aggregate["eligible_same_region_sum"], aggregate["eligible_same_region_count"]
            ),
            "avg_eligible_partners_all_regions": avg(
                aggregate["eligible_all_regions_sum"], aggregate["eligible_all_regions_count"]
            ),
            "avg_best_age_mismatch_same_region": avg(
                aggregate["best_age_mismatch_same_region_sum"], aggregate["best_age_mismatch_same_region_count"]
            ),
            "avg_best_age_mismatch_all_regions": avg(
                aggregate["best_age_mismatch_all_regions_sum"], aggregate["best_age_mismatch_all_regions_count"]
            ),
            "avg_best_earnings_mismatch_same_region": avg(
                aggregate["best_earnings_mismatch_same_region_sum"],
                aggregate["best_earnings_mismatch_same_region_count"],
            ),
            "avg_best_earnings_mismatch_all_regions": avg(
                aggregate["best_earnings_mismatch_all_regions_sum"],
                aggregate["best_earnings_mismatch_all_regions_count"],
            ),
            "avg_best_score_same_region": avg(
                aggregate["best_score_same_region_sum"], aggregate["best_score_same_region_count"]
            ),
            "avg_best_score_all_regions": avg(
                aggregate["best_score_all_regions_sum"], aggregate["best_score_all_regions_count"]
            ),
        }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--run-output-dir", required=True)
    parser.add_argument("--output-dir", default=r"D:\CeMPA\SimPaths")
    args = parser.parse_args()

    run_output_dir = Path(args.run_output_dir)
    output_dir = Path(args.output_dir)

    person_path = run_output_dir / "Person.csv"
    benefit_unit_path = run_output_dir / "BenefitUnit.csv"
    region_by_key = build_region_map(benefit_unit_path)

    overall = defaultdict(new_basic_aggregate)
    age_band = defaultdict(new_basic_aggregate)
    entry_type = defaultdict(new_basic_aggregate)
    unmatched_streak = defaultdict(new_basic_aggregate)
    sex = defaultdict(new_basic_aggregate)
    region = defaultdict(new_basic_aggregate)
    pressure = defaultdict(new_pressure_aggregate)

    rows_processed = 0
    rows_included = 0
    with person_path.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            rows_processed += 1
            year = normalize_year(row["time"])
            entered = parse_bool(row["enteredUnionMatchingThisYear"])
            matched = parse_bool(row["matchedUnionMatchingThisYear"])
            unmatched = parse_bool(row["unmatchedUnionMatchingThisYear"])
            new_entrant = parse_bool(row["newUnionMatchingEntrantThisYear"])
            carry_over_entrant = parse_bool(row["carryOverUnionMatchingEntrantThisYear"])

            if not (entered or matched or unmatched or new_entrant or carry_over_entrant):
                continue
            rows_included += 1

            unmatched_years_at_entry = parse_optional_int(row["unionMatchingUnmatchedYearsAtEntryThisYear"])
            continuous_unmatched_years = parse_optional_int(row["unionMatchingContinuousUnmatchedYears"])
            region_label = region_by_key.get((year, row["idBu"]), "UNKNOWN")
            entry_type_label = "carry_over" if carry_over_entrant else "new" if new_entrant else "unknown"
            streak_label = "unknown" if unmatched_years_at_entry is None else str(unmatched_years_at_entry)

            basic_kwargs = dict(
                entered=entered,
                matched=matched,
                unmatched=unmatched,
                new_entrant=new_entrant,
                carry_over_entrant=carry_over_entrant,
                unmatched_years_at_entry=unmatched_years_at_entry,
                continuous_unmatched_years=continuous_unmatched_years,
            )
            add_basic_stats(overall[(year,)], **basic_kwargs)
            add_basic_stats(age_band[(year, row["unionMatchingAgeBandThisYear"])], **basic_kwargs)
            add_basic_stats(entry_type[(year, entry_type_label)], **basic_kwargs)
            add_basic_stats(unmatched_streak[(year, streak_label)], **basic_kwargs)
            add_basic_stats(sex[(year, row["demMaleFlag"])], **basic_kwargs)
            add_basic_stats(region[(year, region_label)], **basic_kwargs)

            pressure_kwargs = dict(
                entered=entered,
                matched=matched,
                unmatched=unmatched,
                desired_age_diff=parse_optional_float(row["unionMatchingDesiredAgeDiffThisYear"]),
                desired_earnings_diff=parse_optional_float(row["unionMatchingDesiredEarningsDiffThisYear"]),
                eligible_same_region=parse_optional_int(row["unionMatchingEligiblePartnersSameRegionThisYear"]),
                eligible_all_regions=parse_optional_int(row["unionMatchingEligiblePartnersAllRegionsThisYear"]),
                best_age_mismatch_same_region=parse_optional_float(
                    row["unionMatchingBestAgeMismatchSameRegionThisYear"]
                ),
                best_age_mismatch_all_regions=parse_optional_float(
                    row["unionMatchingBestAgeMismatchAllRegionsThisYear"]
                ),
                best_earnings_mismatch_same_region=parse_optional_float(
                    row["unionMatchingBestEarningsMismatchSameRegionThisYear"]
                ),
                best_earnings_mismatch_all_regions=parse_optional_float(
                    row["unionMatchingBestEarningsMismatchAllRegionsThisYear"]
                ),
                best_score_same_region=parse_optional_float(row["unionMatchingBestScoreSameRegionThisYear"]),
                best_score_all_regions=parse_optional_float(row["unionMatchingBestScoreAllRegionsThisYear"]),
            )
            add_pressure_stats(pressure[(year, entry_type_label, "all_entered")], **pressure_kwargs)
            if matched:
                add_pressure_stats(pressure[(year, entry_type_label, "matched")], **pressure_kwargs)
            if unmatched:
                add_pressure_stats(pressure[(year, entry_type_label, "unmatched")], **pressure_kwargs)

    print(f"Processed {rows_processed} person rows; included {rows_included} union-matching rows")

    write_csv(output_dir / "union_matching_by_year_overall.csv", convert_basic_rows(overall, ("year",)))
    write_csv(output_dir / "union_matching_by_year_age_band.csv", convert_basic_rows(age_band, ("year", "age_band")))
    write_csv(output_dir / "union_matching_by_year_entry_type.csv", convert_basic_rows(entry_type, ("year", "entry_type")))
    write_csv(
        output_dir / "union_matching_by_year_unmatched_streak.csv",
        convert_basic_rows(unmatched_streak, ("year", "unmatched_streak_at_entry")),
    )
    write_csv(output_dir / "union_matching_by_year_sex.csv", convert_basic_rows(sex, ("year", "sex")))
    write_csv(output_dir / "union_matching_by_year_region.csv", convert_basic_rows(region, ("year", "region")))
    write_csv(output_dir / "union_matching_by_year_pressure.csv", convert_pressure_rows(pressure))


if __name__ == "__main__":
    main()
