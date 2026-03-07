# The JAS-mine Statistical Package

The statistics package is the built-in library of JAS-mine specifically designed to collect data in a simulation context. Since data sets collected from simulations are frequently updated and sometimes data structures change at runtime, the code is optimized to reduce memory occupancy and CPU time consumption.

The present guide shows step by step the package features and their use.

The package structure is composed of three sections:

1. the **statistics** package contains the main interfaces and classes;
2. the **statistics.reflectors** package contains classes that retrieve data from common java objects;
3. the **statistics.functions** package contains the functions that compute statistics on data sets. The statistics computing algorithms are mainly based on the `cern.jet.stat` package.

# 1. How JAS-mine retrieves data from objects

In order to compute statistics, a statistical object must be able to dynamically collect data from simulation objects. This represents a problem, since the statistical library classes do not know the structure of the target objects (designed by users) and so they cannot access their internal data using instructions like `myObject.getDatum()`.

The easiest solution to solve this problem is the use of reflectors, which are provided by the reflectors package. These classes use Java Reflection to inspect dynamically the target objects' structure and data.

Let's consider an example. An agent represented by the class `MyAgent` contains two integer variables called `age` and `income`, as described by the following code:
```java
public class MyAgent {   
  
    int age;   
    double income;   
  
}
```

Suppose that the user needs to create a series containing readings from the variable `income` for this agent. A typical instruction would be:
```java
MyAgent myAgent = new MyAgent();   
Series.Double seriesIncome = new Series.Double(myAgent, "income", false);
```

The last argument, `false`, signals that the value is not obtained from a method but must be retrieved using reflection. The constructor of the `Series.Double` class then automatically creates a `DoubleInvoker` object that reads the `income` variable within an instance of the `MyAgent` class. This way, every time the series is updated (with the `updateSource()` method, see below), the current value of the agent's income is appended to the `seriesIncome` internal data array.

The reflection mechanism is very simple and elegant but, unfortunately, very inefficient, since it is about 20 time slower than a native direct access! So, in order to increase the speed, we need to access objects natively.

JAS-mine defines a method for direct access, based on the `I*Source` and `I*ArraySource` interfaces, where the \* corresponds to the type of data to be provided:

| **Single value output** | **Multiple value output (array)** |
| --- | --- |
| [IDoubleSource](https://raw.githack.com/jasmineRepo/JAS-mine-core/master/microsim-core/doc/microsim/statistics/IDoubleSource.html) | [IDoubleArraySource](https://raw.githack.com/jasmineRepo/JAS-mine-core/master/microsim-core/doc/microsim/statistics/IDoubleArraySource.html) |
| [IFloatSource](https://raw.githack.com/jasmineRepo/JAS-mine-core/master/microsim-core/doc/microsim/statistics/IFloatSource.html) | [IFloatArraySource](https://raw.githack.com/jasmineRepo/JAS-mine-core/master/microsim-core/doc/microsim/statistics/IFloatArraySource.html) |
| [ILongSource](https://raw.githack.com/jasmineRepo/JAS-mine-core/master/microsim-core/doc/microsim/statistics/ILongSource.html) | [ILongArraySource](https://raw.githack.com/jasmineRepo/JAS-mine-core/master/microsim-core/doc/microsim/statistics/ILongArraySource.html) |
| [IIntSource](https://raw.githack.com/jasmineRepo/JAS-mine-core/master/microsim-core/doc/microsim/statistics/IIntSource.html) | [IIntArraySource](https://raw.githack.com/jasmineRepo/JAS-mine-core/master/microsim-core/doc/microsim/statistics/IIntArraySource.html) |

Each object containing interesting data to be collected should therefore implement one or more of these interfaces, according to the data type.

In order to use these interfaces to natively access data inside the `MyAgent` class, the code has to be modified as follows:
```java
public class MyAgent implements IDoubleSource {   
  
    public enum Variables {   
        Income;   
    }   
    int age;   
    double income;   
  
    public double getDblValue(Enum<?> variable) {   
  
        if (variable.equals(Variables.Income))   
            return income;   
        else   
            throw new IllegalArgumentException("Unsupported variable");   
  
    }   
}
```

The series object previously defined can be now created using the following instructions:
```java
MyAgent myAgent = new MyAgent();   
Series series = new Series.Integer(myAgent, MyAgent.Variables.Income);
```

This way, the series object will now access the target object's variables through its `IDoubleSource` interface, simply by passing to its `getIntValue` method the right enum (`MyAgent.Variables.Income`).

Although boring, this mechanism is more efficient than the previous one, and it is recommended for 'large' (or 'long') simulations. However, the choice between using the reflection or the native access is left to the user.


# 2. Series and time series

A `Series` is a time memory data collector. It requires a single source and append at each update a new reading from the object to the list of stored values. If the source object implement the `I*Source` interface data are read directly, otherwise they are collected through a type specific reflector (`*Invoker`).

The `Series` class provides four implementations to support natively the main Java data types. The available implementations are:

* `Series.Double`, which implements the `IDoubleArraySource` interface;
* `Series.Float`, which implements the `IFloatArraySource` interface;
* `Series.Integer`, which implements the `IIntArraySource` interface;
* `Series.Long`, which implements the `ILongArraySource` interface.

For instance, a series reading long values must be created using the `Series.Long` constructor.

Each of the four classes implements a specific `I*ArraySource` interface, meaning that the series is able to return the data array of the specific data type, for subsequent use by another statistical object (see below the encapsulation mechanism).

The series in not yet a time series, because it does not record the time when the data have been stored. In order to have a time series, the user has to append the series to a `TimeSeries` object, which can contain more than one series, synchronizing them with time.


# 3. Cross section objects

A `CrossSection` object retrieves values from each agent or object contained in a Java collection. If these agents or objects implement the `I*Source` interface data are read directly, otherwise they are collected through a type specific reflector (`*Invoker`).

At every update the cross section refreshes its current data cache and creates dynamically a new array of values, with the same dimension of the source collection. Differently from a `Series`, no memory of the old readings is preserved.

The `CrossSection` class provides four implementations to natively support the main Java data types. The available implementations are:

* `CrossSection.Double`, which implements the `IDoubleArraySource` interface;
* `CrossSection.Float`, which implements the `IFloatArraySource` interface;
* `CrossSection.Integer`, which implements the `IIntArraySource` interface;
* `CrossSection.Long`, which implements the `ILongArraySource` interface.

So, for instance, a cross section reading float values has to be created using the `CrossSection.Float` constructor. Each of the four classes implements a specific `I*ArraySource` interface, in order to provide an array of the specific data type for further manipulation by other statistical objects (see below the encapsulation mechanism).

If the user wants to collect data only from agents with particular characteristics, she can adopt the `ICollectionFilter` interface. Passing to the cross section an object with the `ICollectionFilter` interface (via the `setFilter()` method), it collects only the values from the agents filtered by the custom filter.

If, for instance, we would like to compute the average income of the only "adult" agents in the agent list, we have to define a filter as follows:
```java
public class Filter implements ICollectionFilter {   
  
    public boolean isFiltered(Object object) {   
  
        return ( ((MyAgent) object).age >= 18 );   
  
    }   
}
```

Passing an instance of the `Filter` class to the cross section, we will obtain an array representing the age of the "adult" agents only.


# 4. Functions

A data source can be processed by a `*Function` object, which applies the function and return a value, via an `I*Source` interface.

The functions contained by the `it.zero11.microsim.statistics.functions` package are divided in two main groups:

1. The `*ArrayFunction` objects work with `I*ArraySource` sources which are refreshed at every `updateSource()` call.
2. The `*TraceFunction` objects work with single value sources (`I*Source`). Obviously a single value cannot be used to create a statistics, so these functions trace the value over time. For instance, the `MeanTraceFunction` computes the average value, by storing the sum and the count of the values it receives over time.

As an example, the following table describes some Array functions which operate on array of source values:

| **Function** | **Description** |
| --- | --- |
| [MinArrayFunction](https://raw.githack.com/jasmineRepo/JAS-mine-core/master/microsim-core/doc/microsim/statistics/functions/MinArrayFunction.html) | Finds the lowest value in the array. |
| [MaxArrayFunction](https://raw.githack.com/jasmineRepo/JAS-mine-core/master/microsim-core/doc/microsim/statistics/functions/MaxArrayFunction.html) | Finds the highest value in the array. |
| [MeanVarianceArrayFunction](https://raw.githack.com/jasmineRepo/JAS-mine-core/master/microsim-core/doc/microsim/statistics/functions/MeanVarianceArrayFunction.html) | Computes the average and the variance for the values in the array. |

Finally, the following table gives an example of Trace functions which operate on single source values over time:

| **Function** | **Description** |
| --- | --- |
| [MinTraceFunction](https://raw.githack.com/jasmineRepo/JAS-mine-core/master/microsim-core/doc/microsim/statistics/functions/MinTraceFunction.html) | Checks the source value over time keeping the lowest value ever received. |
| [MaxTraceFunction](https://raw.githack.com/jasmineRepo/JAS-mine-core/master/microsim-core/doc/microsim/statistics/functions/MaxTraceFunction.html) | Checks the source value over time keeping the highest value ever received. |
| [MultiTraceFunction](https://raw.githack.com/jasmineRepo/JAS-mine-core/master/microsim-core/doc/microsim/statistics/functions/MultiTraceFunction.html) | Computes the minimum, maximum, sum, mean and variance of the present and past readings by storing the sums and the count of the values received over time. |


# 5. The encapsulation system

The `I*Source` and `I*ArraySource` interfaces are used to sequentially encapsulate different computational operations. Every time an object implements one of those interfaces it can be inserted in the encapsulation stack as a source of data used by a subsequent object in the stack. The encapsulation allows an infinite number of operations to be sequentially executed, with a single update operation (see below).

It is important to point out that if an object requires a single value as input, it must receive data from an `I*Source` object, while if an object requires an array as input, it must receive data from an `I*ArraySource` object. Thus, Series and CrossSections must follow in the stack an `I*Source` object, while can provide data to Functions.

As an example, suppose you want to compute, at every simulation time step, the moving average of the mean value of the agents' income. This value might be useful, for instance, to understand if the simulation has reached a stationary state.

In order to obtain the moving average we need to perform the following tasks, at each simulation time step:

1. collect data from all the agents contained in a list;
2. compute the average value of the collected data;
3. store the value into a time series object;
4. using the series, compute the current moving average.

Thanks to the encapsulation system we can create a stack of operations and then obtain the value simply invoking one method.

The figure below shows how to build the moving average computer:

![JAS-mine stats encapsulation](https://www.microsimulation.ac.uk/wp-content/uploads/2019/06/JAS-mine-stats.png)

Don't worry! The code to build this operation is simpler than its visual representation, as shown by the following instructions:
```java
CrossSection.Double crossIncome = new CrossSection.Double(agentList, "income", false);  
Series.Double seriesMeanIncome = new Series.Double(new MeanFunction(csIncome));  
MovingAverageArrayFunction fMAIncome = new MovingAverageArrayFunction(seriesMeanIncome, 3 /*moving average window*/);
```


# 6. How statistics are updated

If user had to update all the elements in the encapsulation system, the system would be very complex to manage. In the previous example, the reader should update the `crossSection` object, than the series and finally the `ma` objects, to obtain a new reading of the moving average.

Fortunately, JAS-mine automatically updates all statistical objects, using the `IUpdatableSource` interface. Each statistical object that retrieves data from an `I*Source` object checks if the source implements the `IUpdatableSource` interface and, if it does, updates it before reading the data.

Through this method, each object in the stack is recursively updated. This makes statistics very easy to manage, but it may cause some problems when the same source is used more than once in the stack, as it would be updated twice or more. Imagine a situation in which a time series is forced to be updated twice in the same simulation step: it would append twice the current data. Even worse, if a series included in a `TimeSeries` object is updated twice in the same simulation step, it will go out of synchronization with the `TimeSeries`, and result in a compilation error.

JAS-mine takes care of this problem by checking the simulation time before invoking the `updateSource()`, and ignoring objects which have been already updated. Obviously this choice does not permit to refresh data more than once per simulation step. In order to bypass this constraint, the user has to explicitly set to false the `checkingTime` property of the statistical object:
```java
Series.Long s = new Series.Long(anAgent, "aLongVariable", false);   
s.setCheckingTime(false);
```

Summarizing the updating mechanism, we can enumerate the following rules of thumb:

1. Each statistical object checks if the source implements the `IUpdatableSource` interface and, if it does, invokes the `updateSource()` method before reading the data.
2. When updated, each statistical object checks the current simulation time and performs the update only if the time is different from the last update time.
3. In order to force a statistical object to bypass time checking, its `checkingTime` property must be explicitly set to false, using the `setCheckingTime(false)` instruction.