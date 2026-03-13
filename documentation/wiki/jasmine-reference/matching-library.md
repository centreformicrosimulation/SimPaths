# The JAS-mine Matching Library

JAS-mine has a specific library that performs matching between two collections of agents, based on some specific criterion. The matching methods are called from outside the agents to be matched, for instance by the Model. The simplest algorithm is a one-way matching procedure (the agents in one collection – say females – choose, while the agents in the other collection – say males – remain passive) implemented in the class `SimpleMatching`:

```java
matching(collection1, filter1, comparator1, collection2, filter2, matchingSClosure, matchingClosure);
```

This is invoked as

```java
SimpleMatching.getInstance().matching(...)
```

# 1. Method Arguments

The method requires 7 arguments:

1. **collection1**: the first collection (e.g. all individuals in the population);
2. **filter1**: a filter to be applied to the first collection (e.g. all females with the toCouple flag on in the Demo07 sample model);
3. **comparator1**: a comparator to sort the filtered collection, which determines the order that the agents in the filtered collection will be matched.
4. **collection2**: the second collection, which can be the same as collection1 (e.g. all individuals in the population) or a different one; the two collections do not need to have the same size;
5. **filter2**: a filter to be applied to the second collection (e.g. all males with the toCouple flag on in the Demo07 sample model);
6. **matchingScoreClosure**: a piece of code that assigns, for every element of the filtered collection1, a double value to each element of the filtered collection2, as a measure of the quality of the match between every pair;
7. **matchingClosure**: a piece of code that determines what to do upon matching.

# 2. Understanding Closures

The use of **closures**, which are relatively new to the Java language, allows a great simplification of the code. A closure is a function written by another function. Closures are so called because they enclose the environment of the parent function, and can access all variables and parameters in that function. This is useful because it allows us to have two levels of parameters. One level of parameters (the parent) controls how the function works. The other level (the child) does the work. While it is not required that the user knows about closures, it is interesting to understand why they are so useful. 

In the example, suppose that the females in the population are sorted according to some criterion, say beauty: the prettiest woman is the first to choose a partner, the second prettiest comes second, etc. The matchingScoreClosure sorts all possible mates according to some other criterion, say wealth. Hence, the prettiest woman gets the richest man, the second prettiest gets the second richest, etc. In such a case, a comparator would suffice to order the males in the population, as the ranking is the same irrespective of the female who is evaluating them. But suppose now that the attractiveness of a man depends on the age differential between himself and the potential partner: in such a case, the ranking is specific to each woman in the population. A simple comparator would still do the job, but the comparator should be able to access the identity of the woman who is making the evaluation as an argument, which requires a lot of not-so-straightforward coding. Closures allow to bypass this technical requirement – that has very little to do with modelling issues – because they can pass a functionality as an argument to another method; in other words, they treat functionality as method argument, or code as data. 

Technically, a closure is a function that refers to free variables in its lexical context. A free variable is an identifier (a name, the identity of the woman who is evaluating the men in the population, for instance) that has a definition outside the closure; it is not defined by the closure, but it is used by the closure. In other words, these free variables inside the closure have the same meaning they would have had outside the closure.

## Example Implementation

Closures in the *matching*() method are easier used than explained. An example is found in the *Demo07* sample model. The 7 arguments are:

### collection1
The whole population

```java
persons
```

### filter1
A subset of the female population

```java
new FemaleToCoupleFilter()
```

### comparator1
A comparator that assigns priority to the individual that has a lower difficulty in matching (this is determined by an individual's age in relation to the average)

```java
new Comparator<Person>() {
    @Override
    public int compare(Person female1, Person female2) {
        return (int) Math.signum((Math.abs(female1.getAge() - averageAge.getAverage()) - 
        Math.abs(female2.getAge() - averageAge.getAverage())));
    }
}
```

### collection2
Same as collection1

```java
persons
```

### filter2
A subset of the male population

```java
new MaleToCoupleFilter()
```

### matchingScoreClosure
A closure that, given a specific female, computes for every male in the population a matching score

```java
new MatchingScoreClosure<Person>() {
    @Override
    public Double getValue(Person female, Person male) {
        return female.getMarriageScore(male);
    }
}
```

### matchingClosure
A closure that creates a link between a specific female and a specific male, and sets up a new household

```java
new MatchingClosure<Person>() {

    @Override
    public void match(Person female, Person male) {

        female.marry(male);
        male.marry(female);
    }
}
```

# 3. Complete Method Example

Hence, the whole method looks like:

```java
SimpleMatching.getInstance().matching(

    // collection1: the whole population
    persons,

    // filter1
    new FemaleToCoupleFilter(),

    // comparator1: a comparator that assigns priority to the individual that has a lower difficulty
    // in matchingm (this is determined by an individual's age in relation to the average)

    new Comparator<Person>() {

        @Override
        public int compare(Person female1, Person female2) {

            return (int) Math.signum(
                Math.abs(female1.getAge() - averageAge.getAverage()) -
                Math.abs(female2.getAge() - averageAge.getAverage()));

        }
    },

    // collection2: same as collection1
    persons,

    // filter2
    new MaleToCoupleFilter(),

    // MatchingScoreClosure: a closure that, given a specific female,
    // computes for every male in the population a matching score

    new MatchingScoreClosure<Person>() {

        @Override
        public Double getValue(Person female, Person male) {

            return female.getMarriageScore(male);

        }
    },

    // matchingClosure: a closure that creates a link between a specific
    // female and a specific male, and sets up a new household.

    new MatchingClosure<Person>() {

        @Override
        public void match(Person female, Person male) {

            female.marry(male);
            male.marry(female);

        }
    }
);
```
