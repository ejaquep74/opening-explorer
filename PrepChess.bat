@echo off
:: Enable local environment variables
SETLOCAL

:: Define the name of the JAR file
SET JAR_FILE=YourApp.jar

:: Define the name/location of the external configuration file
SET CONFIG_FILE=application.yml

:: Run the Java application with the specified JAR and configuration file
java -jar %JAR_FILE% --spring.config.location=%CONFIG_FILE%

:: End local environment variables scope
ENDLOCAL
