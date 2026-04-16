# Enums

An **enum** is a special class that represents a group of constants (unchangeable variables). One way to think about enums for users familiar with, for example, Stata, is as of categorical variables – each category that the variable can take must be specified in the enum. In general, enums are well suited for use with values that we know are not going to change.

For example, to define a variable `Gender` that can take either `Male` or `Female` values, we define an enum `Gender` in the `model.enums` package:

![Gender Enum Example](https://www.microsimulation.ac.uk/wp-content/uploads/2020/06/image.png)

We can then, for example, specify a variable `gender` in `Person` class, which will only ever take one of the values specified in the enum:

![Gender Variable Example](https://www.microsimulation.ac.uk/wp-content/uploads/2020/06/image-1.png)

For a more detailed tutorial on enums we recommend following [W3Schools](https://www.w3schools.com/java/java_enums.asp) and [Oracle's tutorial](https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html).