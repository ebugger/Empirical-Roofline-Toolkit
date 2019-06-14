## Roofline Visualizer ##

The Roofline Visualizer is implemented in JavaFX and requires Java version 8 or greater to run. Launch the visualizer from the command line by running the script RooflineVisualizer.sh inside the directory containing RooflineChart.jar. Alternatively run the jar directoy with the command "java -jar \{path to jar\}/RooflineChart.jar".

After launching the Roofline Visualizer you can load Roofline .json files from the local filesystem with the Load button. Roofline .json files are generated when the ERT is run. Alternatively you may load the remote Roofline repository hosted by the University of Oregon by clicking on the "Load Remote" button. The remote repository contains a selection of oofline data collected from an array of computing platforms. 

Sources with multiple Roofline datasets will populate the drop-down list in the upper left part of the window. The Filter button will open a dialog where you can select specific metadata values for the available metadata fields in the loaded Roofline data. Only Rooflines that match the selected metadata values will be shown.

The Roofline chart will display the Roofline metrics, labeled according to the key below the chart. The exact values at the intersection points may be seen by mousing over the points on the chart.

Some Roofline datasets may include heatmap data. When one of these is loaded in addition to the Roofline chart a heatmap chart tab will also be available. Mouse over individual grid cells in the heatmap to see the precise data values.

## Source Files ##

The source code in the RooflineViewPart subdirectory comprises an Eclipse project. In addition to generating the standalone jar for the Roofline Visualizer this project may be used to build an Eclipse plug-in which will incorporate the visualizer into an Eclipse workspace. This functionality relies on the E(fx)clipse project to add JavaFX integration support and the Eclipse C Development Tools (CDT) SDK to provide C/C++ project integration. Eclipse integration is not yet officially supported.

## Contact Information ##
Please contact [Wyatt Spear](mailto:wspear@cs.uoregon.edu) with any questions, problems, feature requests, etc.
