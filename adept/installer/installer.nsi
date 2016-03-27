# define name of installer
outFile "installer.exe"

# Set the title of the installer window
Caption "Adept installer"

# define installation & target directories
installDir $PROGRAMFILES\SRI\Adept

# start default section
section
  
    !define dir_stage build\stage\*
    !define itl_ui_dir ..\..\pal-ui
    !define image_dir ..\applications\imageloader\data\*

    # Check for a previous version of Adept
	IfFileExists $INSTDIR\AdeptTaskLearning CheckForOldVersion CarryOn 
	CheckForOldVersion:
	    #MessageBox MB_OK "Checking for old version" 
        IfFileExists $INSTDIR\AdeptTaskLearning1.0 CarryOn MoveOldVersion
		
    MoveOldVersion:
	#MessageBox MB_OK "Moving old version..."
    Rename $INSTDIR\AdeptTaskLearning $INSTDIR\AdeptTaskLearning1.0 
	 
	CarryOn:
	#MessageBox MB_OK "Carrying on..." 

    # Put the actionmodels into the correct directory
    setOutPath $INSTDIR\AdeptTaskLearning
    File /r /x ".svn" ${itl_ui_dir}\action_models

    setOutPath $INSTDIR\AdeptImageLoader
    File /r /x ".svn" ${image_dir}\

    # Set the current output path to the install dir
    setOutPath $INSTDIR
    File /r ${dir_stage}

    # Create a shortcut to the startup batchfile
	SetShellVarContext all
    createDirectory "$SMPROGRAMS\SRI"
    createDirectory "$SMPROGRAMS\SRI\Adept"
    createShortCut "$SMPROGRAMS\SRI\Adept\Start Adept.lnk" "$INSTDIR\Adept.bat"
    createShortCut "$SMPROGRAMS\SRI\Adept\Doc.lnk" "$INSTDIR\doc"
    createShortCut "$SMPROGRAMS\SRI\Adept\Example Application.lnk" "$INSTDIR\example_applications"
    createDirectory "$SMPROGRAMS\SRI\Adept\Applications"
    createShortCut "$SMPROGRAMS\SRI\Adept\Applications\Start Image Loader.lnk" "$INSTDIR\ImageLoader.bat"
    createShortCut "$SMPROGRAMS\SRI\Adept\Applications\Start Novo.lnk" "$INSTDIR\Novo.bat"

    # Create the action model directories on the target machine to ensure the permissions are correct
    createDirectory "$APPData\AdeptTaskLearning\action_models\CPOFake"
    createDirectory "$APPData\AdeptTaskLearning\action_models\Desktop"
    createDirectory "$APPData\AdeptTaskLearning\action_models\imageloader"
    createDirectory "$APPData\AdeptTaskLearning\action_models\mozilla"

    # Copy the action model data into the new directories
    copyFiles "$INSTDIR\AdeptTaskLearning\action_models\CPOFake\*" "$APPData\AdeptTaskLearning\action_models\CPOFake"
    copyFiles "$INSTDIR\AdeptTaskLearning\action_models\Desktop\*" "$APPData\AdeptTaskLearning\action_models\Desktop"
    copyFiles "$INSTDIR\AdeptTaskLearning\action_models\imageloader\*" "$APPData\AdeptTaskLearning\action_models\imageloader"
    copyFiles "$INSTDIR\AdeptTaskLearning\action_models\mozilla\*" "$APPData\AdeptTaskLearning\action_models\mozilla"

    # create the uninstaller
    writeUninstaller "$INSTDIR\uninstall.exe"

    # create a shortcut named "Uninstall Adept" in the start menu programs directory
    # point the new shortcut at the program uninstaller
    createShortCut "$SMPROGRAMS\SRI\Adept\Uninstall Adept.lnk" "$INSTDIR\uninstall.exe"

    # read the value from the registry into the $0 register
    readRegStr $0 HKLM "SOFTWARE\JavaSoft\JavaFX" FXVersion

    # Compare actual version to the one required, jump 3 lines if it is what we want
    strCmp $0 "2.0.0" +3

    MessageBox MB_OK "JavaFX Version 2.0 is not currently installed. Please quit this installer and install it before continuing."
       return

    # print the results in a popup message box
    MessageBox MB_OK "JavaFX 2.0 Detected, ok to continue"

sectionEnd


# uninstaller section start
section "uninstall"

    # first, delete the uninstaller
    delete "$INSTDIR\uninstall.exe"

    # next, delete the adept data
    rmdir /r "$INSTDIR"
    rmdir /r "$APPData\AdeptTaskLearning"

    # lastly, remove the link from the start menu
	SetShellVarContext all
    delete "$SMPROGRAMS\SRI\Adept\Uninstall Adept.lnk"
    delete "$SMPROGRAMS\SRI\Adept\Start Adept.lnk"
    delete "$SMPROGRAMS\SRI\Adept\Doc.lnk"
    delete "$SMPROGRAMS\SRI\Adept\Example Application.lnk"
    delete "$SMPROGRAMS\SRI\Adept\Applications\Start Image Loader.lnk"
    delete "$SMPROGRAMS\SRI\Adept\Applications\Start Novo.lnk"

    rmdir "$SMPROGRAMS\SRI\Adept\Applications"
    rmdir "$SMPROGRAMS\SRI\Adept"
    rmdir "$SMPROGRAMS\SRI"

# uninstaller section end
sectionEnd
