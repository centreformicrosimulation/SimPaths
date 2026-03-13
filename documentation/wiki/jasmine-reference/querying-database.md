# Querying the Database

# 1. Querying the database at run-time

The database can be queried at runtime to provide inputs for the simulation. The representation of the sample population is fully adherent to the standards used in IT systems to store entities and relations between entities. Consequently, population modelling can be performed according to standard strategies for modelling object classes and their persistence on database. In particular, ORM requires relationships between objects to be implicitly modelled. The ORM engine translates these relationships into foreign keys in the relational model.

erDiagram
    FIRM ||--o{ WORKER : employs
    
    FIRM {
        int firm_id PK
        string name
        string address
        string industry
        date founded_date
        int employee_count
    }
    
    WORKER {
        int worker_id PK
        int firm_id FK
        string first_name
        string last_name
        date birth_date
        string position
        decimal salary
        date hire_date
    }

For instance, in the Entity/Relationship (E/R) diagram above, only the firm_id field containing the primary key of the firm table requires specification. In case all records concerning workers related to one firm need to be obtained, without using ORM a specific SQL query should be created, then run, to extract and insert data in an object intended to represent the connected entity. When using ORM the persistence engine is simply requested to get object of the *Firm* class corresponding to the desired identifier and the object's relational graph is loaded automatically, including related workers (objects of the *Worker* class). For example using the notation `worker.getFirm().getName()` will read from the database the name of the company where a worker is employed without the need of defining any SQL query, not differently from what one would do for reading the same information from the *Firm* object itself, accessed through a specific pointer in the Worker class.

Since Java 5 annotations were introduced to represent attributes/adjectives assigned to specific parts of code as classes or properties. Annotations decorate the elements they are associated to, in the sense that they attribute meanings that can be used to add collateral information to objects.

Annotations make the definition and the use of coefficients tables more powerful and flexible. For example, a table is created to represent and manage the mapping of two characteristics –minimum retirement age and expected residual lifetime– for each sex-age group of the simulated population. The table contains four fields: age, sex, retirementAge and residualLifeTime. These fields have in fact different semantics: the first two correspond to research keys in a key-value dictionary, while the last two represent specific values.

ORM allows the construction of a Java class, for example called CoefficientA, that contains the four properties corresponding to the table fields; their values can then be read by the ORM engine. In order to populate the dictionary automatically the properties of the CoefficientA class can be "decorated" using the JAS-mine ad-hoc CoefficientMapping annotation.

```java
@Entity   
@CoefficientMapping(keys={"age", "sex"},values={"retirementAge", "residualLifetime"})   
public class CoefficientA {   
    private Integer age;   
    private Sex sex;   
    private Integer retirementAge;   
    private Double residualLifeTime;   
    […]   
}
```

The Entity annotation informs the ORM engine that the CoefficientA class corresponds to a table in the database which bears the same name as the class and contains the fields corresponding to the object's properties. A JAS-mine library will then request the ORM engine to read the data contained in the table and to include them in a key-value structure that can be easily queried using an instruction like the following:

```java
MultiKeyCoefficientMap coefficientA = DatabaseUtils.loadCoefficientMap(CoefficientA.class);   
int retirementAge = coefficientA.get(30, Sex.Female, "retirementAge");   
double residualLifetime = coefficientA.get(30, Sex.Female, "residualLifetime");
```

where the first two parameters of the get function are the two keys and the last two (retirementAge, residualLifetime) represent the name of the value variable.

This method for accessing parameter tables may appear convoluted and cumbersome. The same result can be achieved more rapidly by placing the map values in an excel sheet.

The parameters are then loaded using a specific JAS-mine interface:

```java
MultiKeyCoefficientMap coefficientA = ExcelAssistant.loadCoefficientMap("input/coeffA.xls", "Sheet1", 2, 2);
```

Only the number of key columns and "value" columns need to be specified. Clearly this process is much easier but it does not allow for significant parameter typification (since Excel is not as rigid as a database). Moreover, it is more error prone as accidental modifications to the Excel sheet might lead to incorrect parameter loading.

# 2. Inspecting the database before or after a simulation has completed

The user may wish to access the input database before or simulation has been executed or afterwards to view the output database. A simple way to inspect the database is via the 'Database explorer', which can be opened via the 'Tools' tab in menu of the JAS-mine Graphical User Interface (GUI). Another slightly more complicated way involves downloading and installing Hibernate's H2 Console and specifying the full location of the database to be inspected. Both methods open a web browser interface that allows the data from the database to be accessed via SQL-style commands.