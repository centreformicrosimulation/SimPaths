# Working in GitHub

# 1. Introduction 

In this page, the various steps that are necessary to make changes to SimPaths and for them to be correctly implemented in the code and committed via GitHub (_i.e._, made available to other users) are explained.

**Requirements**
- GitHub account
- Java Development Kit (JDK)
- IDE (Integrated Development Environment)

In the following sections, explanatory screenshots are presented from both the GitHub browser and GitHub Desktop. While the latter is not required, GitHub Desktop provides a very user-friendly graphical user interface (GUI). It clearly visualises commits, branches, changes, and merge conflicts, and it is ideal for beginners or those who prefer not to use the command line. Additionally, it is designed for a quick setup and a seamless GitHub account integration. Thus, for the sake of contributing to SimPaths development, GitHub Desktop is a complete tool. However, it is not necessary; the user can entirely operate with the browser version in combination with the IDE or any other Git-integrated tool, _e.g._, GitKraken (GUI) or Git CLI (Command Line).

As an IDE, this guide uses [IntelliJ IDEA](https://www.jetbrains.com/idea/) (free Community edition), which is also our recommendation (especially for beginners). A valuable alternative is [Eclipse](https://eclipseide.org/), whose GUI is less intuitive, but it is fully open-source and highly extensible. Other alternatives exists, including [Theia](https://theia-ide.org/), a rapidly developing AI-native open-source cloud and desktop IDE.
Please ensure that your GitHub account is active and already connected to the chosen IDE (and GitHub Desktop, if you wish to use it).


# 2. Branches and Forks in GitHub Repositories

A GitHub repository (repo) is characterised by _branches_ and _forks_. While they are both tools for parallel work and development, they operationally differ.
A branch is a parallel version of the code within the same repository. To integrate changes made in a branch into another, it suffices to merge it back into the original branch via a pull request (PR) or direct merge.
A fork is a copy of an entire repository (including its history) under a different GitHub account. It creates a completely separate project, where changes can be made without affecting the original repo. To integrate these changes into the original repository, a PR is submitted to the repo owners/maintainers. Once the PR is approved, the changes are merged into the target branch of the original repo.


# 3. SimPaths GitHub Repository

SimPaths code is stored in the public [SimPaths GitHub repository](https://github.com/simpaths/SimPaths). The operative branches are `main`, which contains the most stable release, and `develop`, where modifications and updates are implemented. As outlined in the diagram below, to make changes in SimPaths, users are requested to: 1) fork the original repository under their GitHub account; 2) carry out all the modifications on a new branch originated from the `develop` branch of their forked repository; 3) commit and send a pull request to the maintainers.

![image](https://www.dropbox.com/scl/fi/0dhv5z8rbcqfwi58khjgj/scheme.png?rlkey=dk735e2nrcz35pqltsxfkvmgm&raw=1)

These steps are detailed below.

## 3.1 Forking SimPaths Repo

1. On the [SimPaths GitHub repository homepage](https://github.com/simpaths/SimPaths) (see below), click on the top-right button "Fork". When hovering over it, the message _Fork your own copy of simpaths/SimPaths_ will automatically appear.

![image](https://www.dropbox.com/scl/fi/ql0ac9lpfc7olpttjidxv/Capture-d-cran-2025-07-16-15.54.25.png?rlkey=mlwykxxixgbxyvkihkoaw191l&st=ientbqmv&raw=1)

2. Once clicked on "Fork," the following page will open. It is recommendable to give a distinguishable name to the repository (_e.g._, "SimPathsFork"). Regardless of the name, please ensure that the box _Copy the_ `main` _branch only_ is unticked (as in the image below). Then, click on the green button "Create fork".

_Screenshot omitted here: GitHub's **Create fork** page, where you can name the fork and leave **Copy the `main` branch only** unticked._

After the fork is created, a page identical to the SimPaths GitHub repository homepage will open automatically. The only difference is that, instead of the white-and-purple SimPaths logo, there will be the user GitHub account icon/picture, with text below saying _forked from simpaths/SimPaths_. This confirms the creation of a copy of the entire SimPaths repository under the user GitHub account.

3. At this point, the user is ready to clone the code by clicking on the green button "<> Code", and then on the icon with the two overlapping squares to the right of the url (see image below). When hovering over it, an automatic message _Copy url to clipboard_ will appear, which will turn automatically in _Copied!_, once clicked.

_Screenshot omitted here: GitHub's **Code** menu showing the repository URL and the copy-to-clipboard button._

4. Now that the link is copied, the user is ready to open the cloned repository. This can be done either in the IDE (IntelliJ IDEA, in our case) or on GitHub Desktop. In both cases, expect a few-minute time for the repository to be cloned.

<br>

**IntelliJ IDEA**
On the IntelliJ IDEA homepage, there are three buttons at the top-right corner. The user should click on the rightmost "Clone Repository", which will open the window below.
In the default option tab "Repository URL", it is sufficient to paste the copied url in the corresponding "URL:" box. The "Directory:" will automatically pre-compile, but the user can change it. In particular, they should make sure that the name identifies the forked repository (_e.g._, SimPathsFork).
Alternatively, if the user has connected their GitHub account to the IDE, by clicking on the option "GitHub" (the second in the left column), the repository will automatically appear in the list of the user's repository and can be selected from there.

![image](https://www.dropbox.com/scl/fi/scw9ycgx64vg7yrpap20l/Capture-d-cran-2025-07-16-17.17.40.png?rlkey=usmv7q2xu7w4uo0zhewsyz73t&st=ve8m0oxw&raw=1)  

Once cloned, the repository will appear in the Projects list of IntelliJ IDEA, where it can be open by simply clicking on it. After opening the project, check whether the **Maven** tool window (Maven icon) is visible in the right-hand toolbar. If the Maven tool window is not present, import the Maven project manually:\
   (1) Go to File → Project Structure → Modules \
   (2) Click `+` → Import Module\
   (3) Select the pom.xml file in the local _SimPaths_ repository\
   (4) Complete the **Import from Maven** wizard using the default settings\
After the import completes, the Maven tool window should appear and the project will be correctly configured.

The landing page will be as the one below. By default, the current branch is set to `main`, from which the user should switch to `develop` by clicking on it and then "Checkout" (see image below).

![image](https://www.dropbox.com/scl/fi/q2p7hhkhqa36g4pjgxev2/Capture-d-cran-2025-07-17-11.42.05.png?rlkey=xk1tl7a073clo9kwp9yte3bku&st=jjz7svem&raw=1)


**GitHub Desktop**
On the GitHub Desktop homepage, select "Clone Repository...". Depending on the operating system and whether it is the first time GitHub Desktop is opened, this option can be selected in different ways. For example, at the first use of GitHub Desktop on macOS, the option would appear (second) on a main list as "Clone a Repository from the Internet...". Regardless, when clicking on it, the following tab will appear, from which the user can select the forked repository called "user\_name/SimPaths". In the Local Path tab, it is necessary to give a name that is not already attributed to any other repository (_e.g._, "SimPathsFork"). For example, if the SimPaths repository has already been cloned without forking it, say, to test the model, the user will not be able to use simply "SimPaths" as a name. Once this is sorted out, the user can clone the repository by clicking on the blue "Clone" button:

![image](https://www.dropbox.com/scl/fi/ok4j3mp9hvlugrivtzdld/Capture-d-cran-2025-07-17-10.29.17.png?rlkey=jd43c3de2n8k1loh5sxlsqb3h&st=gv7r5b53&raw=1)  

At the end of the cloning, an automatic message will pop up saying: _This repository is a fork. How do you plan to use it?_. The user should select the first option "To contribute to the parent project", and then "Continue". The landing page will be as the one below. By default, the "Current Branch" is set to `main`, from which the user should switch to `develop`. While it should automatically be up to date, it is always recommended to "Fetch origin" (at the righthand side of menu tab) to be sure that the branch is aligned with the latest updates. It is also important to keep the repo's branches up-to-date. Namely, from the forked repo on the user's account in GitHub browser, the user must simply click on "Sync fork" on the right before starting working on it to be sure the forked repo is up-to-date with the original one (see image below).

![image](https://www.dropbox.com/scl/fi/4o61fg7cpdqs7u731ffpk/Capture-d-cran-2025-07-23-18.28.07.png?rlkey=770zypkcps95yz5oncv9o0zbj&st=3lrexyzs&raw=1)

![image](https://www.dropbox.com/scl/fi/u7tqx50ets4iy3wrkldrh/Capture-d-cran-2025-07-17-11.21.50.png?rlkey=vwzh4zhqtfxgnjabb5gzy00pa&st=veeq21ls&raw=1)

Now that the forked repository has been created ("SimPathsFork" in our case), the user can open the project in the IDE. On the IntelliJ IDEA homepage, there are three buttons at the top-right corner. The user should click on the second one "Open". The window that opens allows selection of the folder of the forked repository, which will be located at the Local Path selected during the cloning. In our case, reading from the image above "Clone Repository", it will be /Users/UserName/Documents/GitHub/SimPathsFork. At this point, it is sufficient to select the folder and click on the blue button "Open". The user will land automatically on the project open on the `develop`branch.

At the end of these four steps, the user will have their forked repository under their GitHub account.

## 3.2 Making Changes
1. From the develop branch in the new forked repository, the user should create a new branch devoted to the modifications they wish to make. Again, this can be done either from GitHub Desktop or directly from the IDE (IntelliJ IDEA). In any case, the branch should be named according to the [branch naming conventions in the Repository Guide](repository-guide.md#7-version-control). The first part of the name should indicate why that branch was created. For example, if the purpose is to fix a bug, this first part could be called "bug-fix". The following parts should get into the details of the broader objective of the branch indicated in the first part. Continuing with the same example, if the bug to fix is related to a specific class of the model, the second part of the name could be the name of the class. Every part of the name should be separated by a "/" symbol. Therefore, in our example, the branch would be called "bug-fix/class_name". While other details can be added as additional parts of the branch name (separated by the "/" symbol), we suggest to keep the branch names relatively short.
**IntelliJ IDEA**
In our forked project ("SimPathsFork" in our case), follow the exact same path illustrated to switch branch from `main` to `develop` (Section 1. "Forking SimPaths Repo"; point 4; second image). In this case, however, in the drop-down menu, select "+ New Branch...". In the small window that opens, rename the branch, make sure that the option "Checkout branch" is ticked, and then click on "Create". The user will land automatically on the new branch of the forked repository.
**GitHub Desktop**
On the GitHub Desktop homepage of the forked repository ("SimPathsFork" in our case), select "Current Branch" (second entry of the menu tab at the top) and then "New Branch". Ensure to _Create branch based on..._ "develop", as shown in the image below, and rename the branch properly (here, assuming to make a change that entails the addition of a new "ethnicity" variable, it is called "add-variable/ethnicity"). Then click the "Create Branch" blue button.  

_Screenshot omitted here: GitHub Desktop's **New Branch** dialog, with the branch created from `develop`._

When opening the forked project on IntelliJ IDEA, it will be already set on the new `add-variable/ethnicity` branch.
Whether it is directly via IntelliJ IDEA or through GitHub, at the end of this first step, the user will be set in the IDE on their new branch of their forked repository, which is the starting point to make any change or modification.

2. At this point, the user is free to make the desired changes. When they are done, it is of paramount importance that the model is tested after the editing. If the model compiles and runs correctly without error messages, the user can proceed to the next and final step. 

## 3.3 Committing Changes  
Once all the modifications have been implemented and the model has been tested to function correctly, the updated version of the model can be made available to everyone. This is done via a two-step procedure. First, the changes must be committed, _i.e._, "sent" and fully integrated into the corresponding branch.[1](#footnote-1) After the commit, the newly published branch will contain all these modifications. Second, the branch must be merged, via a pull request, with the `develop` branch of the public repo simpaths/SimPaths, so that everyone may access the updated version of the model. 
As in the previous points, this can be done either from GitHub Desktop or directly from the IDE (IntelliJ IDEA). For the sake of simplicity, in this case, only the procedure using GitHub Desktop is presented.

1. The GitHub Desktop homepage should look as follows.:

![image](https://www.dropbox.com/scl/fi/yj6g7s2tv9svq56rhk0i6/Capture-d-cran-2025-07-23-17.49.27.png?rlkey=tl4p19noc3i0725gviybs4v0z&st=4wfgmk11&raw=1)

In the left column, all the modifications that have been made are listed. If we click on any of them, the actual changes appear on the righ-hand side of the window (red: what has been deleted; green: what has been added). Before committing the changes, it is very important to: i) untick any modifications that entail the upload of data (data cannot be uploaded on GitHub for confidentiality reasons); ii) add a relevant summary and description to the commit in the bottom-left corner. When both these operations are completed, the user is ready to commit the changes by clicking the blue button at the end of the page "Commit X files to branch-name". After committing, the branch should be published by clicking on "Publish branch" (third entry of the menu tab on the top).

2. At this point, the user should switch to GitHub on their browser and access the [Pull requests](https://github.com/simpaths/SimPaths/pulls) section of simpaths/SimPaths, where they shall select the green button "New pull request" on the right. Before being able to send the pull request, the user will be asked to choose the branches to compare, as illustrated below:

![image](https://www.dropbox.com/scl/fi/fqcpwa93rqgyyf6gaoeid/Capture-d-cran-2025-07-23-18.09.10.png?rlkey=qycxsye2t2pzk8cms4wwrrg7j&st=op2rc1t3&raw=1)

On the left-hand side, the base repository should always be set to simpaths/SimPaths, and the branch on `develop`. On the right-hand side, the user should select their forked repository and the branch where they made all the changes. At this point, the changes will automatically appear underneath and the user will be able to click on the green button on the right "Create pull request". In the window that opens, the title and description will be precompiled from the commit. If they are already self-explanatory, the user can simply click again on the green button on the right "Create pull request" to complete the operation. At this point, an automatic system of tests will be launched to run the model on the server and double-check it works, but no actions are required by the user.
The last step is simply to add one or more reviewers in the "Reviewers" tab at the top right of the page. The reviewer(s) will receive a notification and can review the changes committed before merging them into the simpaths/SimPaths repository.

# 4. Further changes

Following the procedure presented in this page, any user should be able to carry out modifications to the model without any risk of jeopardising it. It is worth noting that, while Steps 2 and 3 must be followed for any new change, once the forked repository is created, it remains available for future use (_i.e._, it is not necessary to go through Step 1 again).

<a name="footnote-1">[1]</a> Committing changes only saves them locally; to "send" them to GitHub it is necessary to "push" changes.
