# How to Introduce a New Variable

# 1. An application using "Ethnicity"

This page explains how to add a new variable to the SimPaths model. A similar approach can be used to add other variables. This example concerns the insertion of an additional variable, "Ethnicity", named `dot01`, defined in six categories, and discussed in [SimPaths issue #212 on ethnicity](https://github.com/simpaths/SimPaths/issues/212).

# 2. Update the input data

Before starting, the user must ensure that they have the updated version of the data for the model, which shall include the variable(s) to be added to the code. If not, they should get the [Understanding Society UK Household Longitudinal Study (UKHLS)](https://beta.ukdataservice.ac.uk/datacatalogue/studies/study?id=6914) and the [Wealth and Assets Survey (WAS)](https://beta.ukdataservice.ac.uk/datacatalogue/studies/study?id=7215) from the [UK Data Service](https://ukdataservice.ac.uk/) and generate the input data for the model using the [InitialPopulations compile do-files on GitHub](https://github.com/simpaths/SimPaths/tree/main/input/InitialPopulations/compile).


# 3. Load the `dot01` variable in SimPaths

## 3.1 Define a 6-category Ethnicity enum  
In the enums folder, create an [enum class](https://www.microsimulation.ac.uk/jas-mine/resources/key-java-concepts/enums/) for the variable, as illustrated in the image below:

![image](https://www.dropbox.com/scl/fi/80yt50stokc57jgext4hl/Capture-d-cran-2025-07-21-11.47.05.png?rlkey=9boedmt4vezy5k1s2x7t2ue55&st=la9nsni0&raw=1)

## 3.2 Add the `dot01` variable to the list of individual characteristics 
In the Parameters.java file, there are string arrays that define which variables are loaded from the donor populations and the initial populations files. As `dot01` is available only in the initial population files, it has to be added to the person variable initial array (`PERSON_VARIABLES_INITIAL`). See image below:

![image](https://www.dropbox.com/scl/fi/zes4qd1nq4qnk35k8lsrl/Capture-d-cran-2025-07-21-12.14.35.png?rlkey=mlxi0cq5bfm94uxpwgxs0n9dt&st=6pr9ss1c&raw=1)


# 4. Modify the SQL Tables

At this point, the code that generates the SQL tables needs to be modified to make sure that these tables include the new categorical variable `dot01`. To this end, the DataParser.java file in the startingpop folder must be updated. In particular, the new chunk of code has to be added to the `parse()` method (see image below):

![image](https://www.dropbox.com/scl/fi/sqyb6648vd6irbcnydi9o/Capture-d-cran-2025-07-21-14.30.50.png?rlkey=98wkyfkqbrkgrlyimpqyamjh0&st=c60k8v9t&raw=1)

When the method is expanded, in the block `try{}`, there are the SQL commands to insert the various persons' characteristics in the tables. Here, the lines to insert the ethnicity should be added, paying attention to use the same categorical specification given in the Ethnicity enum class (see image below):

![image](https://www.dropbox.com/scl/fi/skct50a3g348jkfowpgix/Capture-d-cran-2025-07-21-15.07.16.png?rlkey=chlxw1vvlx0bod5ppiussfrtw&st=zzctbn8b&raw=1)


# 5. Define the Ethnicity variable in the Person class

The Person class is one of the core parts of the model, as it is the blueprint of individuals in SimPaths. Here, the new variable should be `@Enumerated()`, which is a Java annotation used on enum fields in classes to tell the persistence provider (like _Hibernate_ or _Jakarta_) how to save the enum into a database column. In other words, it allows to map the Java object/instance into the SQL table.
Practically, the code will look like in the image below:

![image](https://www.dropbox.com/scl/fi/pkl98w4frp91t72416o9l/Capture-d-cran-2025-07-21-17.11.29.png?rlkey=wa7k5h19rvwk9zgqighcdvec5&st=8ng1yqg6&raw=1)

Now that the variable has been added to the Person class, it should be provided with a getter/setter. To do so, it is sufficient to right-click on the variable name`dot01`, then "Generate" > "Getter"/"Setter" (see image below):

![image](https://www.dropbox.com/scl/fi/uw70ocxzz2w1v0mw0muoj/Capture-d-cran-2025-07-21-17.45.48.png?rlkey=1hib889nubsen1jl7uc7cdeb7&st=2vaspz49&raw=1)

Once added, the Getter and Setter should look as in the image below, and they should be moved at the end of the file together with those of other variables:

_Screenshot omitted here: generated getter and setter methods for `dot01` in the `Person` class._

After the variable is inserted with getters and setters, the [constructors](https://www.digitalocean.com/community/tutorials/constructor-in-java#constructor-overloading-in-java) in the Person class must be updated to include this new variable. For Ethnicity, there are two constructors at play. The first one, `public Person (Person originalPerson, long seed, SampleEntry sampleEntry) {...}`, is the one that is used to clone the person. The second one, `public Person(Gender gender, Person mother) {...}`, is the one for the new born, which takes as argument the gender and the mother and "creates" a child. As ethincity is assumed to be taken after the mother's, it will be sufficient to add a line `dot01 = mother.getDot01();`, where it is stated so (_i.e._, the person's ethnicity - `dot01` - is equal to the person's mother's one `mother.getDot01()`).
<!-- When the initial populations are created, the user can select the number of observations. Hence, the simulation samples households with replacement using sample weights until the desired number is reached. This means that some households can be generated based on the existing ones in the initial survey data. To carry out this process, the model uses constructors, which are coded in the namesake section of the Person class. -->


# 6. Update the Regressors List class

The regressions' estimates for education, fertility, health, etc. stored in /SimPaths/input (originally obtained by running the do files in /SimPaths/input/InitialPopulations/compile/RegressionEstimates) indicate what covariates enter each regression. As Ethnicity is now part of the initial populations' variables, it can also be used for one or more of these regressions. Therefore, similarly to all the other covariates, it must be listed in the `public enum DoublesVariables {}` list (in the Section _implements IDoubleSource for use with Regression classes_) in the Person.java file.[1](#footnote-1)
First, the user should make sure that the variable is coded exactly as it is inserted in the regressions. For example, in our case, Ethnicity enters the regressions in the form of four dummy variables (whose the first one is excluded as residual):
`Ethn_White,` (=1 if the individual is White; =0 otherwise)
`Ethn_Asian,` (=1 if the individual is Asian; =0 otherwise)
`Ethn_Black,` (=1 if the individual is Black; =0 otherwise)
`Ethn_Other,` (=1 in all the other cases; =0 otherwise)
Thus, this list should be coded identically under the `public enum DoublesVariables {}` list in the Person class.
Secondly, the user has to "populate" the values of these variables, following the specification defined in the Ethnicity enum created earlier. Below the `public enum DoublesVariables {}` list in the Person class, there is a method defined as follows, where this procedure is carried out for all the variables in the `DoublesVariables {}` list.

```java
    public double getDoubleValue(Enum<?> variableID) {    
        switch ((DoublesVariables) variableID) {...} }
```
In particular, the functioning of the four binary variables defined above should be elaborated within the `switch` control list, as illustrated below.:

![image](https://www.dropbox.com/scl/fi/8dtyshltvfjodhx8931xl/Capture-d-cran-2025-07-23-14.24.29.png?rlkey=nrp5xf0slnvovl09xpy9v9ry3&raw=1)


# 7. Conclusions

After having completed all these steps, the variable will be available in SimPaths. It is the user's responsibility to run the model until the final simulation step to make sure that all the changes have been implemented correctly and that the model functions with the new variable.

<a name="footnote-1">[1]</a> When a regression is run on benefit units (rather than persons), the corresponding list is `public enum Regressors {}` in the BenefitUnit.java file.
