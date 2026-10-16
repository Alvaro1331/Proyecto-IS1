# Proyecto-IS1

##NOMBRE DEL SOFTWARE: MOTORFLOW

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

Organización del equipo

Dado que somos 5 personas y que el proyecto tiene una parte visual (frontend) y una parte de lógica y datos (backend), decidimos dividir el equipo en dos partes a las que se dedicará mayor o menor tiempo según las necesidades del poyecto y cómo este avance:

Equipo Frontend: Se encargan de todo lo que el usuario ve en pantalla: los formularios, los botones, las tablas de datos, los colores... Trabajan solo dentro de la carpeta frontend y pueden avanzar de forma totalmente independiente usando datos de prueba inventados mientras el backend no está listo.

Equipo Backend: Se encargan de la lógica del servidor: recibir peticiones, validar datos, guardarlos en la base de datos y devolverlos al frontend. Trabajan dentro de la carpeta backend.


Lógica principal del backend:

El servidor Java con Spring Boot ya es capaz de gestionar usuarios, el inventario del taller y el flujo completo de una reparación desde que entra el coche hasta que se entrega. Ahora mismo la API ofrece 25 puntos de conexión (endpoints) para que el frontend pueda ir desarrollándose. Ahora la base de datos funciona así:<br>

Usuarios: Aquí guardamos al personal del taller. Cada uno tiene un rol: ADMIN, RECEPCIONISTA o MECANICO. (Nota: Los clientes no van en esta tabla porque de momento no necesitan iniciar sesión, van en su propia tabla Clientes, hay que cambiarlo).<br><br>

Materiales: Es nuestro inventario. Guardamos el nombre de las piezas (filtros, aceite, pastillas de freno), su precio y el stock disponible.<br><br>
Reparaciones: Una reparación une todo:<br>
Sabe qué coche se está arreglando.<br>
Sabe qué recepcionista lo registró.<br>
Sabe qué mecánico se encarga (cuando el jefe se lo asigna).<br><br>
Reparación-Materiales (Tabla intermedia): Como en una reparación se pueden usar muchas piezas distintas, esta tabla apunta qué piezas y cuántas unidades se han gastado en cada reparación concreta. Además hemos hecho que una reparación no pueda estar en cualquier estado.  Sigue un camino estricto: empieza como PENDIENTE, pasa a EN_PROCESO (cuando se le asigna un mecánico), después pasa a FINALIZADA (el coche está arreglado pero sigue en el taller) y termina en ENTREGADA (el cliente se lo lleva). Hemos programado la siguiente regla: el sistema da un error si intentas saltarte pasos hacia atrás (por ejemplo, pasar un coche "Entregado" a "Pendiente").
<br><br>

Estructura del frontend

Para la parte visual hemos decidido usar HTML, CSS y JavaScript nativo. Todas las páginas están conectadas con nuestro servidor de Java y muestran datos reales de la base de datos. 

La web tiene un diseño negro y naranja(a modificar) y está dividida en varias pantallas según quién la use:

1. Página de inicio (index.html): Es la portada de bienvenida de la web. Explica un poco qué hace el taller y tiene un botón grande para que los clientes puedan consultar su coche rápidamente, además de un enlace para que los trabajadores inicien sesión.

2. Consulta para clientes (pages/consulta-estado.html): Por ahora los clientes no necesitan crearse una cuenta (a modificar), en esta pantalla cualquiera puede escribir la matrícula de su coche o su número de teléfono. Al darle a buscar, la página le enseña una tarjeta con los datos de su coche y unas etiquetas de colores que le dicen si la reparación está Pendiente, En proceso, Finalizada o si ya se ha Entregado.

3. Inicio de sesión (pages/login.html): Aquí entra el personal del taller con su correo y contraseña. Lo hemos programado para que sea inteligente: cuando inicias sesión, el sistema mira si eres el jefe, la recepcionista o un mecánico, y te manda automáticamente a tu panel correspondiente.

4. Panel de la recepcionista (pages/dashboard-recepcionista.html): Tiene tres pestañas para hacer el trabajo del día a día: una para registrar un coche nuevo junto a su dueño, otra para abrir una nueva avería, y una lista para ver todos los clientes y coches que tenemos guardados.

5. Panel del mecánico (pages/dashboard-mecanico.html): Aquí cada mecánico ve una lista con solo las reparaciones que el jefe le ha asignado a él. Puede darle a un botón para cambiar el estado a "En proceso" o "Finalizada", y además tiene un apartado para marcar qué piezas ha gastado del almacén (por ejemplo, 2 filtros de aceite), lo que resta automáticamente esas piezas del stock para no tener que contarlas a mano.

6. Panel del administrador / jefe (pages/dashboard-admin.html): Es el panel más completo porque el jefe tiene que poder controlarlo todo. Desde aquí puede crear nuevos empleados, ver todas las reparaciones y decidir qué mecánico se encarga de cada una, y gestionar el almacén de piezas (con avisos automáticos en rojo si alguna pieza se está agotando).

7. Archivos compartidos (app.js y styles.css): Hemos creado un archivo de JavaScript común que se encarga de hablar con el servidor Java (haciendo peticiones a http://localhost:8080/api) y de mostrar los avisos y carteles en pantalla, además de una hoja de estilos para que toda la web tenga los mismos colores y botones.
<br>
<br>

## Información para usuarios:
Esta sección se tiene y se ira rellenando a medida que se vayan desarrollando el software. 
Si eres una persona que busca probar el software, de momento hay poco que se pueda utilizar, se recomienda leerse la wiki donde esclarecemos el funcionamiento de la aplicación que perseguimos y que tenemos en el momento.
<br>
<br>

### Feedback y comentarios
Para todos aquellos que probeís nuestra aplicación os agradeceriamos que si encontraseis algun "bug" o algun error en el software, nos lo reportaseis. También nos gustaría que nos dieseís vuestra opinicion (siempre que sea una critica constructiva) para mejorar y reforzar las funcionalidades, fortaleciendo el resultado final.
