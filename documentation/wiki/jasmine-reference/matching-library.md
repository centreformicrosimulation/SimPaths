# The JAS-mine Matching Library

Matching pairs agents from candidate collections using a score and an action to perform when a pair is accepted. In SimPaths, partnership matching is coordinated by `SimPathsModel` and implemented by [UnionMatching](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/model/UnionMatching.java). See [Family composition](../overview/modules/family-composition.md) for the model assumptions.

This is distinct from tax-benefit donor matching, which is implemented in `simpaths.model.taxes`.

## Inputs to matching {#1-method-arguments}

The library interfaces distinguish:

- candidate collections, with optional eligibility filters;
- a comparator where an algorithm requires a candidate order;
- a `MatchingScoreClosure` that evaluates a potential pair;
- a `MatchingClosure` that applies an accepted match.

These arguments differ between algorithms. Do not copy the argument list of a simple or iterative matcher into a global matcher.

## Scoring and applying a pair {#2-understanding-closures}

The score callback can use characteristics of both candidates. The matching callback performs the state changes after a pair is accepted. Keeping those responsibilities separate allows the algorithm to evaluate alternatives without forming partnerships prematurely.

In `UnionMatching`, `localGetValue()` evaluates age and potential-earnings differences relative to the candidates' desired differences. It rejects pairs outside the configured bounds or relationship checks. `localMatch()` updates the unmatched sets and either marks a test partnership during alignment or forms the new benefit unit for an actual match.

Do not replace that action with two generic `marry()` calls: SimPaths must maintain benefit-unit membership, region and other partnership state consistently.

## Current SimPaths example {#3-complete-method-example}

The global-matching path begins:

```java
var gm = new GlobalMatching<Person>();
unmatched = gm.matching(
    unmatched.getFirst(),
    null,
    unmatched.getSecond(),
    null,
    new MatchingScoreClosure<Person>() {
        @Override
        public Double getValue(Person male, Person female) {
            return localGetValue(male, female);
        }
    },
    new MatchingClosure<Person>() {
        @Override
        public void match(Person male, Person female) {
            localMatch(male, female);
        }
    }
);
```

This is an excerpt from `UnionMatching.evaluateGM()`, not a standalone program. The candidate sets and helper methods must already exist. The class also implements an iterative-random path. Consult the calling model configuration and schedule to determine which path is used in a particular run.

When changing matching, test unmatched counts, candidate eligibility, deterministic behaviour under the same seed, and consistency of household and benefit-unit relationships after matching.
