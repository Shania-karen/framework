@echo off
REM ------------------------------------------------------------------------
REM Script de déploiement Windows pour compiler le framework et préparer le projet de test
REM ------------------------------------------------------------------------

REM Activer delayed expansion pour manipuler les variables dans la boucle
setlocal enabledelayedexpansion

REM ------------------------------------------------------------------------
REM Définition des chemins (à adapter si besoin)
REM ------------------------------------------------------------------------
set "FRAMEWORK_DIR=D:\itu\L3\S5\framework"
set "BUILD_DIR=%FRAMEWORK_DIR%\build"
set "TEST_DIR=D:\apache-tomcat-10.1.28\webapps\testFramework"
set "SERVLET_JAR=%FRAMEWORK_DIR%\jakarta.servlet-api_5.0.0.jar"

REM ------------------------------------------------------------------------
REM Création des dossiers de sortie du framework
REM ------------------------------------------------------------------------
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
if not exist "%BUILD_DIR%\classes" mkdir "%BUILD_DIR%\classes"

REM ------------------------------------------------------------------------
REM Compilation récursive de tous les fichiers Java du framework en une seule commande
REM ------------------------------------------------------------------------
echo Compilation du framework...

REM Créer une variable pour stocker la liste de tous les fichiers Java
set FILE_LIST=

for /R "%FRAMEWORK_DIR%" %%f in (*.java) do (
    set FILE_LIST=!FILE_LIST! "%%f"
)

REM Compiler tous les fichiers ensemble
javac -classpath "%SERVLET_JAR%" -d "%BUILD_DIR%\classes" !FILE_LIST!
if errorlevel 1 (
    echo Erreur de compilation du framework
    exit /b 1
)

REM ------------------------------------------------------------------------
REM Création du JAR du framework
REM ------------------------------------------------------------------------
echo Creation du JAR du framework...
cd /d "%BUILD_DIR%"
if exist "framework.jar" del "framework.jar"
jar cvf "framework.jar" -C "classes" .

REM ------------------------------------------------------------------------
REM Copie du framework.jar dans le projet Test
REM ------------------------------------------------------------------------
echo Copie du framework.jar dans le projet Test...
if not exist "%TEST_DIR%\WEB-INF\lib" mkdir "%TEST_DIR%\WEB-INF\lib"
xcopy "%BUILD_DIR%\framework.jar" "%TEST_DIR%\WEB-INF\lib\" /Y >nul

REM ------------------------------------------------------------------------
REM Démarrage de Tomcat si le chemin est passé en paramètre
REM ------------------------------------------------------------------------
if not "%~1"=="" (
    echo Demarrage de Tomcat...
    call "%~1\bin\startup.bat"
)

endlocal
echo Deploy terminé avec succès !
