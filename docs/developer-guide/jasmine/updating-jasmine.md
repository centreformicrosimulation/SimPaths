# Updating JAS-mine

# 1. Using Apache Maven

The easiest way to update the JAS-mine libraries in your project is to use Apache Maven. Maven now comes pre-installed with most IDEs. Details slightly change based on the IDE, the instructions below refer to Eclipse (version Luna). 

Open the `pom.xml` file in Eclipse and go to the Dependencies tab. Select the libraries to update. Click on the Manage button.

![JAS-mine dependencies](https://www.microsimulation.ac.uk/wp-content/uploads/2019/06/JAS-mine-dependencies.png)

The dependency will now display on the right column, under the "Dependency Management" heading:

![JAS-mine dependencies managed](https://www.microsimulation.ac.uk/wp-content/uploads/2019/06/JAS-mine-dependencies-managed.png)

Select it, and click on the Properties button. Then, update the version and press OK.

![JAS-mine dependency properties](https://www.microsimulation.ac.uk/wp-content/uploads/2019/06/JAS-mine-dependency-properties.png)

You should now see the new version of the JAS-mine library in the dependencies list:

![JAS-mine dependencies changed](https://www.microsimulation.ac.uk/wp-content/uploads/2019/06/JAS-mine-dependencies-changed.png)

To update the .jars in the project, you may have to get Maven to update them. This is done by right clicking on the project in Eclipse's 'Package Explorer' window, then choosing the 'Maven / Update Project' menu, as in the screenshot below. Click OK on the window that pops up, and Maven should automatically download the new JAS-mine libraries and add them to the project.

![JAS-mine maven update](https://www.microsimulation.ac.uk/wp-content/uploads/documentation/JAS-mine-maven-update.png)


# 2. Manual update

An alternative and more involved way to update the JAS-mine libraries is to manually update an existing project to a new version of JAS-mine. This is done by manually downloading the new `JAS-mine-core-with-dependencies` and `JAS-mine-gui-with-dependencies` .jar files from the [JAS-mine download area](https://sourceforge.net/projects/jas-mine/files/Libraries/), and referring to them in your project. In order to do it, right-click on the project you want to update. Select Properties, then go to the Java Build Path tab on the vertical menu and on the Libraries tab on the horizontal menu. Select the old JAS-mine .jars, and remove them. After removal, no JAS-mine libraries should be present in the Libraries tab, as in the screenshot below:

![JAS-mine build path](https://www.microsimulation.ac.uk/wp-content/uploads/2019/06/JAS-mine-build-path.png)

After [downloading](https://github.com/jasmineRepo) the JAS-mine libraries, copy these files to the 'libs' folder in the Eclipse project (create a 'libs' folder if there isn't one in the project). Then select both JAS-mine .jar files and right click, select 'Build Path / Add To Build Path':

![JAS-mine add build path](https://www.microsimulation.ac.uk/wp-content/uploads/2019/06/JAS-mine-add-build-path.png)

You can check the JAS-mine .jars are on the build path by right-clicking on the project and selecting 'Build Path / Configure Build Path'. Check the new .jars are there in the Libraries tab. Note that your IDE should no longer show any Errors in your project related to missing JAS-mine classes. You should see the following:

![JAS-mine build path configured](https://www.microsimulation.ac.uk/wp-content/uploads/2019/06/JAS-mine-build-path-1.png)