# CS Roofline Toolkit #
 ![alt text](https://raw.githubusercontent.com/ebugger/Empirical-Roofline-Toolkit/master/Roofline-intro.png)
Welcome to the CS Roofline Toolkit Bitbucket site.  The Empirical Roofline Tool and Roofline Visualizer are currently available here.
In the future additional tools will be hosted here.  

For an overview of the Roofline Performance Model and this software's place in that context see
[https://crd.lbl.gov/departments/computer-science/performance-and-algorithms-research/research/roofline/](https://crd.lbl.gov/departments/computer-science/performance-and-algorithms-research/research/roofline/).

## Empirical Roofline Tool ##

The Empirical Roofline Tool, ERT, automatically generates a roofline data
for a given computer.  This includes the maximum bandwidth for the various
levels of the memory hierarchy and the maximum gflop rate.  This data is
obtained using a variety of "micro-kernels".

The ERT comes with a set of configuration files for a number of
computers/architectures.  These configuration file can be adapted to your
local environment and needs to better measure the roofline parameters of
your computer(s).

This is version 1.1.0 of the ERT -- the second public release.

For details about the ERT, please refer to the User's Manual in the
repository under the "Empirical_Roofline_Tool-1.1.0" directory.

## Roofline Visualizer ##

The Roofline Visualizer can visualize the roofline performance data
generated locally by the ERT or stored on a remote Roofline repository.

This is version 1.1.0 of the Roofline Visualizer -- the second public release.

For details about the Roofline Visualizer, please refer to the "README.md"
file in the repository under the "Roofline_Visualizer-1.1.0" directory.

## Contact Information ##

Please contact [Charlene Yang](mailto:CJYang@lbl.gov) with any questions, problems, corrections, suggestions, etc.
