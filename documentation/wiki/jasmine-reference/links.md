# Links

Links are connections between two different objects in the simulation. Examples are husband-to-wife (a **one-to-one** relationship, in most legal systems), parent-to-offspring (a **one-to-many** relationship, or a many-to-one if looked from the other side), and firms-to-workers (a many-to-many relationship, as a firm can employ multiple workers, and a worker can be employed at multiple firms at the same time).

When an object has more than one link with objects belonging to the same entity (eg. a parent to his/her offspring, a firm to its employees, a worker to the firms he/she is working for), a collection is generally used to store the linked objects’ id (eg. a list of children, a list of employees, a list of firms).

A problem then arises if one wants to keep record of all the links, as (i) the list size is not a priori defined, (ii) the list size can change over time. The solution envisaged in the JAS-mine architecture is to fully exploit the potentiality of the underlying relational database, and model the link as an object in itself, which will then be stored in a separate table with the identifiers of the two connected nodes and the time.

The JAS-mine demo model [Applications](https://www.microsimulation.ac.uk/jas-mine/demo/job-applications) exemplifies this type of relationship in practice.
