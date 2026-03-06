# GUI Guide

The GUI is available in single-run and multi-run workflows when enabled.

## Enable GUI

Single run:

```bash
java -jar singlerun.jar -g true
```

Multi run:

```bash
java -jar multirun.jar -config default.yml -g true
```

## Screenshots

Main GUI:

![SimPaths GUI](figures/SimPaths%20GUI.png)

Control buttons:

![SimPaths Buttons](figures/SimPaths-Buttons.png)

Parameter selection:

![SimPaths Parameters](figures/SimPaths%20parameters.png)

Charts overview:

![Charts](figures/Charts.png)

Chart properties:

![Chart Properties](figures/Chart%20Properties.png)

Chart zoom example:

![Chart Zoom](figures/SimPaths-Chart-Zoom.png)

Output stream panel:

![Output Stream](figures/Output%20stream.png)

## Headless note

In remote servers or CI, run with `-g false`.
