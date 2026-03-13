# The JAS-mine Alignment Library

Alignment is a technique widely used in (dynamic) microsimulation modelling to ensure that the simulated totals conform to some exogenously specified targets, or aggregate projections (Baekgaard, 2002; Klevmarken, 2002, Li and O’Donoghue, 2014). Alignment is a way to incorporate additional information which is not available in the estimation data. The underlying assumption is that the microsimulation model is a poor(er) model of the aggregate, but a good model of individual heterogeneity: by forcing the microsimulation outcomes to match the targets in a way that is as least distortive as possible, the microsimulation model is left with the task of distributing the totals in the population. In general, the above assumption is very dangerous and unwarranted, and alignment should be looked at with great suspicion.

One important thing to note is that the processes to be aligned are executed at an individual level, while alignment always takes place at the population level. That is, individual outcomes or probabilities are determined for each individual based on the chosen econometric specification and the estimated coefficients. This in general leads to a mismatch between the simulated (provisional) totals and the aggregate targets, which can of course be assessed only at the population level. The alignment algorithm then directly modifies the individual outcomes or probabilities of occurrence. 

# 1. Common Arguments

All alignment methods in JAS-mine require 4 arguments (see the code blocks below for examples on how to use the methods in a JAS-mine model):

1. **collection**: a collection of individuals whose outcome or probability of an event has to be aligned (e.g. all the population);
2. **filter**: a filter to be applied to the collection (e.g. all females selected to divorce);
3. **AlignmentProbabilityClosure** or **AlignmentOutcomeClosure**: a piece of code that i) for each element of the filtered collection computes a probability for the event (in the case that the alignment method is aligning probabilities, as in the SBD algorithm) or an outcome (in the case that the alignment method is aligning outcomes), and ii) applies to each element of the filtered collection the specific instructions coming from the alignment method used. (In the case of multiple choice alignment such as Logit Scaling, an '**AlignmentMultiProbabilityClosure**' is used instead, which handles a set of probabilities for the many possible event outcomes.);
4. **targetShare** or **targetNumber**: the share or number of elements in the filtered collection that are expected to experience the transition. (In the case of multiple choice alignment, this is an array of targetShare proportions, containing the share for each potential outcome).

We introduce the Multiple Choice alignment methods available in JAS-mine (section A), followed by a description of the binary alignment methods available (section B), concluding with an introduction to alignment with variable agent weightings (section C).

# 2. Multiple Choice Alignment

The earliest alignment techniques implemented in JAS-mine fall into the category of binary alignment. Version 3.2.0 of JAS-mine introduced the possibility of **multiple choice alignment** by implementing the Logit Scaling alignment method of Stephensen (2016).

## 2.1 Logit Scaling (LS)

**Logit Scaling** (LS) is an alignment technique that is theoretically optimal in that it minimizes the information loss i.e. distortion (as measured by the relative entropy) in the process of aligning probabilities to given targets. In addition, it is computationally efficient and quick to run. The method is implemented using the 'Bi-Proportional Scaling' algorithm that quickly converges to the solution of the problem. This involves representing the set of state probabilities for all individuals in a population as a two-dimensional matrix, with each row representing an individual 'i', and each column representing a particular state 'a' (the choice or outcome of the process to be aligned). The matrix undergoes an iterative process whereby: 

1. The sum of each column of probabilities is scaled to match the alignment target, which is the expected (mean) number of individuals in the state 'a' that the column represents; 
2. The sum of each row is then scaled to equal 1 (as the sum of state probabilities should always equal 1, i.e. the individual must be in one particular state at any moment in time). 

Steps 1) and 2) are then repeated in sequence until the system converges. Not only does Logit Scaling alignment minimize the distortion to the probability distributions of the individuals while obtaining the alignment targets, but it has a number of useful features including the ability to retain zero probabilities (i.e. impossible events), a symmetric formulation where neither outcome (or choice) has a favoured status in the algorithm, and moreover the ability to handle more than just two choices (which the following alignment algorithms are all restricted to doing). Logit Scaling is the clear choice for any user wanting to perform multiple choice alignment in JAS-mine.

### Example: Multiple Choice Logit Scaling

Here is an example of how the multiple choice Logit Scaling alignment could be implemented in a JAS-mine model where there are three potential outcomes (note the last argument is an array containing three elements that sum to 1).

```java
new LogitScalingAlignment<Agent>().align(  
  
    // collection  
    persons,   
      
    // filter   
    new FemaleToDivorce(ageFrom, ageTo),   
  
    // alignment probability closure for multiple choice alignment  
    new AlignmentMultiProbabilityClosure<Agent>() {  
  
        @Override  
        public double[] getProbability(Agent agent) {  
            return agent.getProb();  
        }  
  
        @Override  
        public void align(Agent agent, double[] alignedProabability) {  
                    agent.setChoice(RegressionUtils.event(Choice.class, alignedProabability));  
        }  
    },  
  
    new double[]{targetShareA, targetShareB, (1.- targetShareA - targetShareB)});  
  
}
```

# 2. Binary Choice Alignment

In addition to Logit Scaling alignment, there are six binary alignment methods implemented in JAS-mine:

1. Multiplicative Scaling (MS),
2. Sidewalk (SW),
3. Sorting by the difference between predicted probability and random number (SBD), and Sorting by the difference between logistic adjusted predicted probability and random number (SBDL),
4. Resampling (RS).


Implementation of (1)-(4) is based on Li and O'Donoghue (2014) (Jinjing Li kindly provided the Stata code used in that paper), while implementation of (5) closely follows Richiardi and Poggi (2014) and Leombruni and Richiardi (2006). 

## 2.1 Multiplicative Scaling (MS)

**Multiplicative scaling** involves undertaking an unaligned simulation using Monte Carlo techniques and then comparing the proportion of transitions with the external control total. The average ratio between the desired transition rate and the actual transition is used as a scaling factor for the simulated probabilities. The method ensures that the average scaled simulated probability is the same as the desired transition rate. The method, however, is criticized by Morrison (2006) as probabilities are not guaranteed to stay in the range 0-1 after scaling, though the problem is rare in practice as the multiplicative ratio tends to be small.

## 2.2 Sidewalk (SW)

**The Sidewalk** method was first introduced as a variance reduction technique, which was also used as an alternative to the random number based Monte Carlo simulation. It keeps a record of the accumulated probability from the first modelled binary outcome to the last. As long as there is a change of the integer part of the accumulated probability, the observation is assigned with an outcome value of 1.

## 2.3 Sorting based alignment algorithms

Sorting based alignment algorithms involve sorting of the predicted probability adjusted with a stochastic component, and selects desired number of events according to the sorting order: **SBD** sorts by the difference between the predicted probability and a random number in (0,1), while
**SBDL** sorts by a logistic transformation of the predicted probability.


Both SBD and SBDL introduce a significant distortion in the estimated probabilities and their use is deprecated. However, they are included for replication exercises. 

## 2.4 Resampling (RS)

**Resampling** involves drawing again the event, without altering the predicted probabilities, either for agents who have experienced the transition (if too many transitions have occurred) or for agents that have not experienced the transition (if too few events have occurred), until the target is reached. 

## 2.5 Binary Logit Scaling (LSb)

Implementation of **binary Logit Scaling** (LSb) closely follows Stephensen (2016) and the description of the Bi-Proportional Scaling algorithm above, however it exploits the two-state property of the system to simplify the algorithm.

## 2.6 Other Binary Alignment Algorithms

Li and O'Donoghue (2014) analyse three other binary alignment algorithms:

1. Sidewalk with nonlinear transformation (SNT),
2. The central limit theorem approach (CLT) and
3. Sorting by predicted probability (SBP).

SNT and CLT have not been implemented yet in JAS-mine as they are relatively more complicated and run much slower than the other methods; SBP has not been implemented due to its theoretical shortcomings and poor empirical performances (see Li and O'Donoghue, 2014).

## 2.7 Example: Binary Choice Alignment (SBD)

```java
new SBDAlignment<Person>().align(   
      
    // collection   
    persons,   
      
    // filter   
    new FemaleToDivorce(ageFrom, ageTo),   
      
    // alignmentProbabilityClosure  
    new AlignmentProbabilityClosure<Person>() {   
          
        // i) compute the probability of divorce   
        @Override   
        public double getProbability(Person agent) {   
            return agent.computeDivorceProb();   
        }   
          
        // ii) determine what to do with the aligned probabilities   
        @Override   
        public void align(Person agent, double alignedProbability) {   
            boolean divorce = RegressionUtils.event(alignedProbability, SimulationEngine.getRnd());
            agent.setToDivorce(divorce);   
        }   
    },   
      
    // targetShare  
    divorceTarget  
);
```

# 3. Alignment with Weighting

Another new feature introduced in version 3.2.0 of JAS-mine is that alignment can now be done on an agent population where each agent carries a weighting that defines the number of individuals it represents. For example, an agent with a 'weighting' variable equal to 4 means that the agent should be considered to represent four individuals. These weightings need to be taken into account when alignment occurs as not all agents are considered to represent the same number of individuals.

The JAS-mine alignment classes that allow for variable agent weightings rely upon the agent implementing the 'Weighting' interface, which means that the agent class contains a public method called 'getWeighting()'.

### Available Weighted Alignment Classes

The following alignment classes catering for variable agent weightings are available in JAS-mine:

* **ResamplingWeightedAlignment** – the resampling alignment algorithm for agents implementing the Weighting interface,
* **LogitScalingWeightedAlignment** – the multiple choice Logit Scaling alignment algorithm for agents implementing the Weighting interface,
* **LogitScalingBinaryWeightedAlignment** – the binary choice Logit Scaling (LSb) alignment algorithm for agents implementing the Weighting interface.


# 4. References

Baekgaard H (2002). “Micro-macro linkage and the alignment of transition processes: some issues, techniques and examples”. National Centre for Social and Economic Modelling (NATSEM) Technical paper No. 25.

Klevmarken A (2002). “Statistical inference in micro-simulation models: incorporating external information”. Mathematics and Computers in Simulation, 59: 255-265.

Leombruni R, Richiardi M (2006). "LABORsim: An Agent-Based Microsimulation of Labour Supply. An application to Italy." Computational Economics, vol. 27, no. 1, pp. 63-88

Li J, O'Donoghue C (2014). "Evaluating Binary Alignment Methods in Microsimulation Models". Journal of Artificial Societies and Social Simulation, 17(1): art. 15.

Richiardi M, Poggi A (2014). "Imputing Individual Effects in Dynamic Microsimulation Models. An application to household formation and labor market participation in Italy." International Journal of Microsimulation, 7(2), pp. 3-39.

Stephensen P (2016). "Logit Scaling: A General Method for Alignment in Microsimulation models." International Journal of Microsimulation, 9(3), pp. 89-102.

