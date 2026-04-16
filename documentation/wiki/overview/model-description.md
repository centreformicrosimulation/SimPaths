# Model Description

SimPaths is a fully open-source structural dynamic microsimulation framework, designed to facilitate experimentation with alternative model assumptions. It is coded in Java using the [JAS-mine core](https://github.com/jasmineRepo/JAS-mine-core) and [JAS-mine GUI](https://github.com/jasmineRepo/JAS-mine-gui) simulation libraries. SimPaths models are currently estimated for the United Kingdom and Italy, and are under development for Hungary, Poland, and Greece. 

SimPaths implements a hierarchical architecture where individuals are organised in benefit units (for fiscal purposes), and benefit units are organised in households. The model projects data at yearly intervals, reflecting the yearly frequency of the survey data used to estimate model parameters. The model is composed of eleven modules: 

1. Ageing
2. Education
3. Health
4. Family composition
5. Social care
6. Investment income
7. Labour income
8. Disposable income
9. Consumption
10. Mental health
11. Statistical display

Each module is composed of one or more processes; for example, the ageing module contains ageing, mortality, child maturation, and population alignment processes. Empirical specification of dynamic processes makes extensive use of cross-module characteristics (state variables). A graphical representation of the simulated modules is shown below:

![model_structure](https://github.com/simpaths/SimPaths/assets/56582427/d4c773a2-b720-4546-bca6-c76d07282dc4)