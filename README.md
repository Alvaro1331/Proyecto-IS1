# Proyecto-IS1

Bienvenido al ReadMe de nuestro proyecto: un software encargado de la gestión de un taller. Con el fin de automatizar los procesos que existen en un taller buscamos desarrollar un software que permita: al cliente del taller ver el estado de su coche en tiempo real según sus datos; al recepcionista registrar los coches en la base de datos; al mecanico notificar el proceso de reparacion y los materiales usados; y al mecanico jefe/administrador/dueño poder administrar la aplicacion con distintas funcionalidades.
<br>
<br>

## Equipo de desarrollo:
  ·Marta Barbero Fuentes<br>
  ·Nicolás Rodríguez Torrevejano<br>
  ·Marcos Arévalo Garrido<br>
  ·Marcos Martín Clemente<br>
  ·Álvaro Sánchez Camacho<br>
<br>
## Información para nuevos desarrolladores:
Esta sección se tiene y se ira rellenando a medida que se vayan desarrollando el software. 
En la wiki esta documentado el proceso de desarrollo, es muy útil para saber los hitos completados y los objetivos próximos. Además en el apartado de projects hay una sección que permite visionar las historias de usuario relacionadas con el proyecto, y su estado de desarrollo actual.

¿Qué tecnologías hemos elegido y por qué?

Una de las primeras decisiones importantes fue elegir con qué herramientas íbamos a construir la aplicación. Estas son las que hemos decidido usar:

Java con Spring Boot para el backend (la parte del servidor que no se ve). Lo elegimos porque es el lenguaje que conocemos de clase y porque Spring Boot es un framework muy utilizado en la industria que nos facilita muchísimo el trabajo: con muy poco código consigues que el servidor funcione, se conecte a la base de datos y exponga los datos al frontend.

HTML, CSS y JavaScript para el frontend (la parte visual que verá el usuario). Al no tener experiencia con frameworks como React o Angular, optamos por lo más básico y sólido. Así el equipo de frontend puede trabajar sin depender de instalaciones complejas.

H2 como base de datos durante el desarrollo. H2 es una base de datos que vive en la memoria del ordenador mientras el servidor está encendido, y desaparece cuando lo apagas. La ventaja es que no hay que instalar nada: cualquier miembro del equipo puede descargarse el proyecto y arrancarlo directamente. Cuando el proyecto esté más avanzado, cambiaremos a una base de datos real como MySQL.

Git y GitHub para el control de versiones, usando TortoiseGit como interfaz gráfica. Esto nos permite trabajar los cinco de forma paralela sin pisarnos el código.

¿Cómo hemos organizado el equipo?

Dado que somos 5 personas y que el proyecto tiene una parte visual (frontend) y una parte de lógica y datos (backend), decidimos dividir el equipo en dos subgrupos:

Equipo Frontend (2-3 personas): Se encargan de todo lo que el usuario ve en pantalla: los formularios, los botones, las tablas de datos, los colores... Trabajan solo dentro de la carpeta frontend y pueden avanzar de forma totalmente independiente usando datos de prueba inventados mientras el backend no está listo.

Equipo Backend (2-3 personas): Se encargan de la lógica del servidor: recibir peticiones, validar datos, guardarlos en la base de datos y devolverlos al frontend. Trabajan dentro de la carpeta backend.

¿Cómo está organizado el código?

El repositorio tiene la siguiente estructura de carpetas:

Proyecto-IS1/
├── .gitignore          ← Lista de archivos que Git debe ignorar (ej. node_modules)
├── README.md           ← Descripción del proyecto
│
├── frontend/           ← Todo lo visual (HTML, CSS, JS)
│   ├── index.html      ← Página de inicio
│   ├── css/            ← Archivos de estilos
│   ├── js/             ← Archivos JavaScript
│   └── pages/          ← Resto de páginas (login, dashboards...)
│
└── backend/            ← Todo el servidor Java
    ├── pom.xml         ← Lista de librerías que necesita Java (como un "carrito de la compra")
    └── src/main/java/com/taller/
        ├── models/       ← Las "plantillas" de los datos (Cliente, Vehiculo...)
        ├── repositories/ ← Los que hablan directamente con la base de datos
        ├── services/     ← Donde está la lógica real (las reglas del negocio)
        ├── controllers/  ← Los que reciben las peticiones del frontend
        └── dtos/         ← Los "sobres" en los que empaquetamos los datos

<br>
<br>
## Información para usuarios:
Esta sección se tiene y se ira rellenando a medida que se vayan desarrollando el software. 
Si eres una persona que busca probar el software, de momento hay poco que se pueda utilizar, se recomienda leerse la wiki donde esclarecemos el funcionamiento de la aplicación que perseguimos y que tenemos en el momento.
<br>
<br>
### Feedback y comentarios
Para todos aquellos que probeís nuestra aplicación os agradeceriamos que si encontraseis algun "bug" o algun error en el software, nos lo reportaseis. También nos gustaría que nos dieseís vuestra opinicion (siempre que sea una critica constructiva) para mejorar y reforzar las funcionalidades, fortaleciendo el resultado final.
