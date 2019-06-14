## Roofline Visualizer ##

The Roofline Visualizer is implemented in JavaFX and requires Java version 8 or greater to run. Launch the visualizer from the command line
by running the script rooflineVisualizer.sh inside the directory containing RooflineChart.jar. Alternatively run the jar directoy with the
command "java -jar /{path to jar}/RooflineChart.jar.

After launching the Roofline Visualizer you can load Roofline .json files with the load button. Roofline .json files are generated when the
ERT is run. Alternatively you may load the remote Roofline repository (currently containing a small sample of Roofline data) hosted by the
University of Oregon by clicking on the "Load Remote" button. Sources with multiple Roofline datasets will populate the drop-down list in the
upper left.

The Roofline chart will display the Roofline metrics, labeled according to the key below. The exact values at the intersection points may be
seen by mousing over the points on the chart.

If you have access to a TAU profile database with data corresponding to a loaded Roofline you may select the database, database view and trial
and then select a function from the application to plot on the Roofline. Note that this feature is not yet officially supported. The application
metrics selected for display on the Roofline chart may not correspond to the metrics used in the system Roofline.

## Source Files ##

The source code in the RooflineViewPart subdirectory comprises an Eclipse project. In addition to generating the standalone jar for the Roofline
Visualizer this project may be used to build an Eclipse plug-in which will incorporate the visualizer into an Eclipse workspace.
This functionality is not yet officially supported.

## Known Issues ##

A persistent Java segmentation fault in the Nouveau driver on Linux it may be avoided by using Java 8 release 05.

## Contact Information ##
Please contact [Wyatt Spear](mailto:wspear@cs.uoregon.edu) with any questions, problems, feature requests, etc.
