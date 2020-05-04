# IntelliJ Setup

1. At start of IntelliJ, browse to the root `pom.xml` and open it as project.
2. Enable checkstyle:
  - Install the [IntelliJ CheckStyle-IDEA Plugin](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea). It can be found via plug-in repository  
  (File > Settings > Plugins > Marketplace; **Mac**: IntelliJ IDEA > Preferences > Plugins > Marketplace).  
    ![checkstyle](graphics/checkstyle.PNG)
    
  - Install the CheckStyle-IDEA Plugin, click "Apply" and restart the project upon request.
  - Repeat the previous steps for the Lombok Plugin
  - Open the Settings (by pressing <kbd>Ctrl</kbd> + <kbd>Alt</kbd> + <kbd>S</kbd>; **Mac**: <kbd>command</kbd> + <kbd>,</kbd>)
  - Go to "Other Settings > Checkstyle".
  - Click on "+" under Configuration File and add `checkstyle.xml`. It is located in `docs/dev/config/IntelliJ IDEA`. Confirm.
  
    ![checkstyle](graphics/checkstyle-config.PNG)
    
  - Activate the settings and confirm:
  
    ![checkstyle](graphics/checkstyle-active.PNG)  
   
3. Configure the code style (Source: <https://youtrack.jetbrains.com/issue/IDEA-61520#comment=27-1292600>)  
  - Open the Settings (by pressing <kbd>Ctrl</kbd> + <kbd>Alt</kbd> + <kbd>S</kbd>; **Mac**: <kbd>command</kbd> + <kbd>,</kbd>)  
  - Go to "Editor > Code Style"  
  - Click on the gear icon (right of "Scheme:")  
  - Click "Import Scheme"  
  - Choose "IntelliJ IDEA code style XML"
  - Navigate to `intellij-idea-code-style.xml`. It is located in `docs/dev/config/IntelliJ IDEA`.  
  - Click "Apply"
  - Click "OK"  
  - Click "Close"  
  
4. Setup code headers to be inserted automatically  
    ![copyright-profile](graphics/copyright-profile-new.png)  
  - Open the Settings (by pressing <kbd>Ctrl</kbd> + <kbd>Alt</kbd> + <kbd>S</kbd>; **Mac**: <kbd>command</kbd> + <kbd>,</kbd>)  
  - Go to "Editor > Copyright > Copyright Profiles"  
  - Click the "+"  
  - Name "Atlas"  
  - Copyright text from [CodeHeaders](CodeHeaders.md)  
  - Click "Apply"
  - Go to "Editor > Copyright > Formatting"
  - Adjust copyright formatting settings
    
     ![checkstyle](graphics/formatting-copyright-new.png)
       - Change to `Use block comments` with `Prefix each line`
       - Set `Relative Location` to `Before other comments`
       - Set `Separator before`to `80` and `Separator after` to `81`
  - Go to "Editor > Copyright"
  - Set "Atlas" as Default project copyright
  - Click "Apply"
  
5. Setup Apache Tomcat
  - Download Tomcat 9.0 from https://tomcat.apache.org/download-90.cgi. Choose "zip" under "Core".
  - Extract it to C:\Apache. Result: C:\Apache\apache-tomcat-9.0.7.
  - Click "Edit Configuration"
    
    ![configuration](graphics/add-config.PNG)
    
  - Click "Tomcat Server > Local"
    
    ![tomcat](graphics/tomcat-first-step.PNG)
    
  - Configure Tomcat Path:

    ![tomcat](graphics/tomcat-second-step.PNG)
    
  - Choose C:\Apache\apache-tomcat-9.0.7 (or wherever you saved and extracted Tomcat) as Tomcat Home and Tomcat base directory. 
    -> Your Tomcat version is displayed, confirm with "OK". 
  - Set no Browser Launch and define url as localhost:8080/atlas
    
    ![tomcat](graphics/tomcat-after-launch.PNG)
    
  - Add deployment artifacts under "Deployment": 
    ![tomcat-deployment](graphics/tomcat-deployment.PNG)

    ![tomcat-artifact1](graphics/click-artifact.png)
    
    ![tomcat-artifact2](graphics/select-artifact-new.png)
    
  - Define deployment context (/atlas): 
    
    ![tomcat-artifact2](graphics/deployment-context.PNG)
    
  - Add artifact under "Server": 
    
    ![build-artifact](graphics/tomcat-build-option-new.png)
    
  - Confirm with "Apply"
    
  - Add new run config: 
    
     ![run-config](graphics/add-run-config.PNG)
     
     ![run-config2](graphics/run-config.png)
     
  - Confirm with "Apply" and "OK"
  
6. Configure Git to handle line endings
  - Insert the following commands in your console:  
  **For Windows**: `git config --global core.autocrlf true`  
  **For Mac/Linux**: `git config --global core.autocrlf input`

     
