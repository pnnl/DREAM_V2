<h3>Designs for Risk Evaluation and Management (DREAM) Version 2.0</h3>
<p>This manual provides a brief guide for the use of the Designs for Risk Evaluation and Management (DREAM) tool Version 2.0, developed as part of the effort to quantify the risk of geologic storage of carbon dioxide (CO2) under the U.S. Department of Energy’s (DOE) National Risk Assessment Partnership (NRAP). DREAM is an optimization tool created to identify optimal monitoring schemes that minimize the time to first detection of CO2 leakage from a subsurface storage formation. DREAM optimizes across user-provided output from subsurface leakage simulations or using outputs from reduced order models. While DREAM was developed for CO2 leakage scenarios, it is applicable to any subsurface leakage simulation of the same output format.</p>
<p>The DREAM tool is comprised of four main components: (1) a Java wizard used to configure and execute the simulations, (2) a visualization tool to view the domain space and optimization results, and (3) plotting scripts used to analyze the results, and (4) a Java application to aid users in converting common American Standard Code for Information Interchange (ASCII) output data to the standard DREAM hierarchical data format (HDF5).</p>
<p>DREAM employs a simulated annealing approach that searches the solution space by iteratively mutating potential monitoring schemes built of various configurations of monitoring locations and leak detection parameters. This approach has proven to be orders of magnitude faster than an exhaustive search of the entire solution space. This user’s manual illustrates the program graphical user interface (GUI), describes the tool inputs, and includes an example application.</p>

## Table of Contents

- [Introduction](#introduction)
- [Software Installation and Requirements](#software-installation-and-requirements)
- [User Interface](#user-interface)
- [Simulated Annealing Algorithm](#simulated-annealing-algorithm)
- [DREAM Applicability](#dream-applicability)

## Introduction

<p>The Designs for Risk Evaluation and Management (DREAM) tool was developed at Pacific Northwest National Laboratory (PNNL) as a tool to assist in determining optimal placement of monitoring devices in order to detect carbon dioxide (CO2) leakage from storage formations. The National Risk Assessment Partnership (NRAP) identified the need for a user-friendly tool with the ability to design site-specific risk-based monitoring strategies. NRAP is a U.S. Department of Energy (DOE) project tasked with conducting risk and uncertainty analysis in the areas of reservoir performance, natural leakage pathways, wellbore integrity, groundwater protection, monitoring, and systems level modeling. Monitoring designs produced by DREAM may be used by stakeholders and regulators to assist in compliance with regulatory requirements developed to ensure the safety of U.S. underground sources of drinking water (USDW) and ultimately lead to safe, permanent geologic CO2 storage. Further, site-specific designs generated by DREAM allow for potential generalizations to other sites, as well as comparisons between risk-based monitoring designs and monitoring designs for other purposes, if such designs should already exist (e.g., from a Regional Carbon Sequestration Partnership [RCSP] site).</p>
<p>DREAM optimizes across user-provided output from subsurface leakage simulations, with the objective of configuring monitoring schemes that minimize time to first detection of user-specified leakage indicators. DREAM employs a simulated annealing approach that searches the solution space by iteratively mutating potential monitoring schemes built of various configurations of monitoring locations and leak detection parameters. Leakage indicators may include pressure, temperature, gas saturation, dissolved component concentrations, pH, or any other quantity that can be modeled in a physics-based simulation of porous media fluid transport. These variables are constrained by location and budget, where placement must be in a user-defined suitable location and the budget includes both the number of monitoring devices and the number of wells. The simulated annealing approach has proven to be orders of magnitude faster than an exhaustive search of the entire solution space (Yonkofski et al., 2016). While DREAM was designed with applications to CO2 leakage in mind, this flexibility allows DREAM to determine optimal monitoring locations for any contaminant transport scenario.</p>
<p>Successful use of this software requires both a proper implementation of the mathematics by the tool developers and a proper application of the tool by the user. It is strongly recommended that the user develops an understanding of the leakage system in terms of the relevant hydrogeologic behavior and chemical properties as well as the practical aspects of site-specific monitoring prior to use the DREAM tool. Successful application of the DREAM tool facilitates decision support through an enhanced understanding of the leakage system and associated solution monitoring configurations. Unsuccessful application may lead to a false understanding of the leakage system and suboptimal monitoring configurations.</p>
<p>Development of DREAM began in 2012 as part of the NRAP project, with the first code version being released in 2016. The current version of the code began development during the second phase of the NRAP program. Subsequent versions, when completed, are planned to add flexibility in the objective function, allow for a broader range of input formats, and improve existing capabilities (e.g. faster computational speeds, additional geophysical monitoring technologies).</p>

## Software Installation and Requirements

<p>DREAM requires that you have the most recent release of the Java Platform, currently version 8. If you already have Java installed, search for “About Java” to find your version. It is also recommended that the user installs an HDF5 reader, such as HDFView, for reading converted hierarchical data format (HDF5). Finally, it is recommended that the user has Python 3 with “numpy”, “h5py”, “matplotlib”, and “pandas” packages installed for some post-processing scripts to work. If you already have Python installed, you can check your version by typing “python --version” into a command prompt. Download locations for these files are shown in Table 1.
<table>
  <caption>Table 1: Supplementary Software</caption>
  <tr>
    <th>Software</th>
    <th>Website</th>
    <th>Required?</th>
  </tr>
  <tr>
    <td>Java SE 8</td>
    <td>https://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html</td>
    <td>Required</td>
  </tr>
  <tr>
    <td>HDF5View 3.1</td>
    <td>https://portal.hdfgroup.org/display/support/Download+HDFView</td>
    <td>Optional for viewing and editing HDF5 files</td>
  </tr>
  <tr>
    <td>Python 3</td>
    <td>https://www.python.org/downloads/</td>
    <td>Optional for post-processing scripts</td>
  </tr>
</table>
<p>DREAM version 2.0 has been released within the NRAP Tools Beta Test Release Collaborative Workspace on the NETL Energy Data Exchange (EDX). Sign into EDX with an account that belongs to the workspace and download the DREAM tool: https://edx.netl.doe.gov/organization/nrap-tools/folder/080f6c7e-7a40-43c2-9c44-551bcc16ed7d. There are separate distributions for the Windows and Mac versions. Two sample input datasets are provided with the DREAM distribution at the link above:
<ul>
  <li>DREAM_2.0.jar (14.8 MB) – A runnable JAR file, which packages all the necessary libraries, images, and documentation into an executable program.</li>
  <li>HDF5_Example.zip (160 MB) – HDF5 files for 5 scenarios from modeling associated with Sminchak et al. (2014).</li>
  <li>IAM_Example.zip (2.7 MB) – Results from 480 scenarios generated with Open-IAM associated with Bacon et al. (2019).</li>
</ul>
<strong>Instructions</strong>
<ol>
  <li>Download the files and unzip the example folders.</li>
  <li>Double click the JAR file to start DREAM and select an unzipped example folder during the Input Directory page.</li>
</ol>
</p>

## User Interface

<p>The DREAM tool is comprised of three main components: 1) a Java wizard used to configure and execute the optimization algorithm; 2) a file converter to translate leakage simulation output from American Standard Code for Information Interchange (ASCII) data to the standard DREAM HDF5; and 3) a results directory to plot best configurations and analyze the performance of the iterative procedure.</p>
<p>The example will guide the user through the DREAM Java Wizard GUI while demonstrating an application to 19 randomly selected leakage scenarios generated for an NRAP Second-Generation Reduced-Order Model study (Carroll et al., 2014b). For context, a brief summary of the model set up from Carroll et al. (2014b) is provided below.</p>
<p>The NUFT numerical model (Figure 1) was comprised of a 3D heterogeneous domain that represented an unconsolidated aquifer consisting of layers of permeable sand and impermeable shale layers based on the lithology of the High Plains Aquifer. The aquifer was underlain by a hypothetical CO2 storage reservoir and both aquifer and reservoir were penetrated by leaking wells. The model domain encompassed 10 km × 5 km × 240 m with 1 to 5 leakage sources per scenario placed at a depth of 198 m based on 48 known well locations. The wells were a mix of domestic, feedlot, irrigation, public water supply, and oil field water supply wells. Leakage rates were varied based on uncertainties in hydrogeologic properties.</p>
<p>The included beta test files contain output data at specified times across all nodes representing hypothetical leakage scenarios from the CO2 storage formation. DREAM will be used to optimize monitoring configurations that minimize the estimated time to first detection (TTD) of CO2 leakage.</p>
<ol>
  <li>DREAM Welcome Page</li>
  <li>Input Directory</li>
  <li>Scenario Weighting</li>
  <li>Leakage Criteria</li>
  <li>Detection Criteria</li>
  <li>Configuration Settings</li>
  <li>Exclude Locations</li>
  <li>Run DREAM</li>
</ol>

## Simulated Annealing Algorithm

<p>DREAM is intentionally designed to run on a personal computer. Therefore, a complete enumeration of the solution space that tests all possible combinations and placements of monitoring technologies is not feasible. As an alternative, DREAM uses an optimization algorithm to approximate the optimal monitoring configurations. In the future, DREAM may provide multiple algorithm options.</p>
<p>Simulated annealing is the chosen algorithm for DREAM, found by Matott et al. (2011) to perform moderately well among evaluated algorithms for geoscience application and found by Bangerth et al. (2006) to be efficient at finding near-optimal solutions. Simulated annealing is an iterative search heuristic analogous to the physical process of annealing. At each iteration, the configuration is randomly mutated with one of the following actions:
<ul>
  <li>Add a random monitoring technology to a valid location</li>
  <li>Remove an existing monitoring location</li>
  <li>Swap an existing location with another valid monitoring technology</li>
  <li>Move an existing monitoring technology to another valid location</li>
  <li>Move an entire well to another valid location</li>
  <li>Shuffle all the monitoring locations with a single well</li>
</ul></p>
<p>Each of the listed actions are limited by prior user inputs such as cost constraints, available locations, and the maximum number of wells. The first action (add a location) is weighted more heavily so that the iterations approach a maximized budget which is assumed to yield the best results, though randomization can result in configurations using less budget. All other actions are equally weighted. If one action is attempted and fails due to constraints (i.e. add a location when no budget is available), a new action is randomly selected until an action is successful.</p>
<p>Simulated annealing comes into play as the algorithm decides whether to keep the previous configuration or the mutated configuration at each iteration. Simply taking the better configuration every time is likely to trap the algorithm at a locally optimized result rather than the globally optimized result. Simulated annealing uses a temperature value that exponentially degrades from 1 to 0 through the iterations (Figure 21), used to determine the likelihood of keeping the worst of the two configurations. This allows the algorithm to act “risky” towards the start of the run and transition towards more “stable” changes as the solution converges on an optimized location at the end of the run. Best configurations are saved during the entire process.</p>
<p>An objective function is used to value each configuration for the above simulated annealing algorithm. Currently, DREAM optimizes exclusively on the shortest time to detection of leaks. Value is assigned based on the shortest time to detection for all scenarios, with a large penalty assessed if a leak is not detected.</p>

## DREAM Applicability

<p>DREAM version 2.0 is designed as a tool for additional analysis of subsurface CO2 leakage simulations. However, it can be applied to any dataset of the formats described in Section 3.2. Users should take note of the following limitations:
<ul>
  <li>The only objective function solved is the time to first detection of leakage.</li>
  <li>The cost of monitoring devices is treated as a one-time fee, as opposed to incurring costs during operation.</li>
  <li>The constraint on the number of sensors is determined by the total monitoring budget compared to the cost of each technology. Costs for wells and remediation are not factored in the optimization but are post-processed for the best configurations.</li>
  <li>The monitoring configuration results produced by the simulated annealing algorithm are a function of the scenarios provided to DREAM as well as the number of iterative procedures and number of configurations tested. Users are encouraged to vary each of these parameters to determine the sensitivity of their results.</li>
  <li>DREAM was developed for use on PC and MAC and takes advantage of threading for some processes; therefore the speed of the tool is dependent on the capability of the specific PC or MAC to handle the size of the datasets.</li>
  <li>The interactive map feature (Section 3.1.7) cannot work without an internet connection.</li>
</ul></p>
