Adept Task Learning 1.1
=======================

Documentation
-------------

Please see AdeptOverview.html in this folder for a description of Adept.
Please see InstallationGuide.html in this folder for installation instructions.
Please see UserGuide.html in this folder for instructions on how to operate Adept
  and its sample application.
Please see EditorGuide.html in this folder for instructions on how to operate
  the Adept Procedure Editor.
Please see ClientApplicationGuide.html in this folder for instructions on how
  to enble an application for Adept.

Major Changes
-------------

* The Adept Procedure Editor is now available.
* A new Adept sample application, Novo, is provided, along with its source code.
* The Mozilla Firefox and Thunderbird plugins for Adept are no longer supported or provided.
* Adept now uses JavaFX 2.
* The MacOS platform is not supported, due to lack of support in JavaFX 2.
  Future MacOS support is dependent on JavaFX support for MacOS.
* Numerous extensions to the action model have been added to permit more control
  of how procedures are learned.
* Adept applications may now define their own knowledge-producing actions.

Bugs and Limitations
--------------------

* On some Windows 7 systems, it may be necessary to manually set write
  permission on the folder %APPDATA%\AdeptTaskLearning\ and the folders it
  contains.  This is required if Adept fails upon execution with an error in
  creating or reading this folder.

* Type equivalencing is not supported for structure types.

* Context actions are not removed from learned procedures
  even if they prove useless in completing dataflow. 
