-sprint2-bis
retourne la liste des class qui a les annotations par rapport aux controllers . 
exemple 

com.package.controller
@Controller
Test1.Java

com.package.controller
Test2.java

La fonction va retourner Test1.java , pcq y a que Test1 qui a l'annotation 
@controller dans le package controller


-sprint3
retourne les methodes dans un @controller donne a partir de son url 
exemple "monApp/login"
methodes : getUser()


-sprint4
execute la fonction dans l'url , exemeple dans "\login" on a 
String getUser(){
    return "user est ici ";
}
quand on accede a \login , on obtient : user est ici 


-sprint4-bis:
retourne la page jsp d'un controller , exemple /login va afficher login.jsp  