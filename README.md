# Proyecto-IS1

## NOMBRE DEL SOFTWARE: MOTORFLOW

Bienvenido al ReadMe de nuestro proyecto: un software encargado de la gestión de un taller mecánico. Con el fin de automatizar los procesos que existen en un taller hemos desarrollado un software que permite: a los clientes del taller ver el estado de su coche en tiempo real según sus datos; a los recepcionistas registrar a los clientes, sus coches coches y abrir nuevas órdenes de reparación; a los mecánicos ver qué coches tienen asignados, actualizar el avance de la reparación y anotar los materiales usados; y al mecánico jefe/administrador/dueño poder gestionarlo todo (crear empleados, supervisar el inventario, controlar el dinero del taller y dar el visto bueno final antes de entregar los coches).
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
Esta sección se tiene y se ira rellenando a medida que se vaya desarrollando el software. 
En la wiki está documentado el proceso de desarrollo, es muy útil para saber los hitos completados y los objetivos próximos. Además en el apartado de projects hay una sección que permite visionar las historias de usuario relacionadas con el proyecto, y su estado de desarrollo actual.

### ¿Qué tecnologías hemos elegido y por qué?

Estas son las herramientas que hemos decidido usar para construir la aplicación:

Java con Spring Boot para el backend: es el lenguaje que conocemos de clase. Spring Boot nos facilita muchísimo el trabajo: con muy poco código consigues que el servidor funcione, se conecte a la base de datos y exponga los datos al frontend.

HTML, CSS y JavaScript para el frontend. Al no tener experiencia con frameworks como React o Angular, optamos por lo más básico y sólido. Así el equipo de frontend puede trabajar sin depender de instalaciones complejas.

MySQL como base de datos definitiva. Durante los primeros sprints usamos una base de datos temporal (H2) para probar cosas rápido sin instalar nada. Actualmente, hemos migrado la base de datos a MySQL, un motor más profesional, de forma que ya tenemos una base de datos (tallerdb) persistente.

Git y GitHub para el control de versiones, usando TortoiseGit como interfaz gráfica. Esto nos permite trabajar los cinco de forma paralela sin pisarnos el código.

### ¿Cómo arrancar el proyecto en tu ordenador?
Si te acabas de clonar el repositorio, solo necesitas 3 pasos:
1. Tener **MySQL** instalado en tu equipo.
2. Abrir MySQL Workbench y ejecutar una sola vez esta línea para crear la base: "CREATE DATABASE tallerdb;"
3. Abrir el proyecto en IntelliJ y darle al botón verde de Play (en "application.properties" la contraseña por defecto de MySQL es "admin123").
<br>

### Organización del equipo

Dado que somos 5 personas y que el proyecto tiene una parte visual (frontend) y una parte de lógica y datos (backend), decidimos dividir el equipo en dos partes a las que se dedicará mayor o menor tiempo según las necesidades del poyecto y cómo este avance:

Equipo Frontend: Se encargan de todo lo que el usuario ve en pantalla: los formularios, los botones, las tablas de datos, los colores... Trabajan solo dentro de la carpeta frontend y pueden avanzar de forma totalmente independiente usando datos de prueba inventados mientras el backend no está listo.

Equipo Backend: Se encargan de la lógica del servidor: recibir peticiones, validar datos, guardarlos en la base de datos y devolverlos al frontend. Trabajan dentro de la carpeta backend.


## Lógica principal del backend:

El servidor Java con Spring Boot ya es capaz de gestionar usuarios, el inventario del taller y el flujo completo de una reparación desde que entra el coche hasta que se entrega. Ahora mismo la API ofrece 25 puntos de conexión (endpoints) para que el frontend pueda ir desarrollándose. La información se organiza en estas tablas principales:<br>

Usuarios: Aquí guardamos al personal del taller. Cada uno tiene un rol: ADMIN, RECEPCIONISTA o MECANICO. <br><br>

Clientes: Los dueños de los vehículos. Tienen su propia tabla independiente. Un cliente puede estar registrado sin cuenta (solo con su nombre y teléfono cuando deja el coche), o crearse una cuenta con correo y contraseña para ver su historial. Además, cuenta con un sistema para recuperar la contraseña si se le olvida. <br><br>

Vehículos: Los coches del taller, cada uno con su matrícula, marca, modelo y asociado a su dueño.<br><br>

Materiales: Es nuestro inventario. Guardamos el nombre de las piezas (filtros, aceite, pastillas de freno), su precio y el stock disponible.<br><br>

Reparaciones (El núcleo del taller): Une el coche, el recepcionista que lo atendió, el mecánico que lo arregla y la descripción de la avería.<br><br>

Privacidad por Localizador: En lugar de buscar coches por matrícula, cada reparación genera automáticamente un código alfanumérico único de 8 letras y números (por ejemplo: "Y6NDCVRP"). Así ningún cliente puede ver coches ajenos.<br><br>

Flujo estricto de estados:** Una reparación no puede saltarse pasos. Sigue este orden obligatorio:  
    "PENDIENTE" ➔ "EN_PROCESO" ➔ "FINALIZADA" (el mecánico acaba) ➔ "CONFIRMADA" (el administrador da el visto bueno) ➔ "ENTREGADA" (se cobra y el cliente se lleva el coche).<br><br>
Control del dinero y presupuestos: El taller gestiona números reales:<br>
    -Presupuesto cliente: lo que se le cobra al dueño del coche.<br>
    -Estimación de recepción: lo que calcula la recepcionista que costará arreglarlo.<br>
    -Presupuesto del mecánico: el tope máximo de dinero que el administrador autoriza al mecánico para gastar en piezas.<br>
    -Caja del taller: cuenta con un fondo inicial de 2.000 € y calcula beneficios y gastos en tiempo real.<br>
Reparación-Materiales: Apunta exactamente cuántas piezas ha gastado el mecánico en cada coche. Al usarlas, se descuentan solas del inventario del almacén y del presupuesto del mecánico.
<br><br>

## Estructura del frontend

Para la parte visual hemos decidido usar HTML, CSS y JavaScript nativo. Todas las páginas están conectadas con nuestro servidor de Java y muestran datos reales de la base de datos. 

La web tiene un diseño negro y naranja y está dividida en varias pantallas según quién la use:

1. Página de inicio (index.html): Es la portada de bienvenida de la web. Explica un poco qué hace el taller y tiene un botón grande para que los clientes puedan consultar su coche rápidamente, además de un enlace para que los trabajadores inicien sesión.

2. Consulta para clientes (pages/consulta-estado.html): Pensada para quien no tiene cuenta o tiene prisa. Metes tu código localizador de 8 letras y ves al instante en qué punto está tu coche con etiquetas de colores, sin necesidad de contraseñas.

3. Inicio de sesión (pages/login.html): Tiene dos pestañas inteligentes:
   * Área Clientes: Para iniciar sesión, registrarse como nuevo usuario o solicitar un código de 6 dígitos al correo si has olvidado tu contraseña.
   * Personal del Taller: Para que entren el jefe, el recepcionista o los mecánicos y el sistema los mande a su pantalla correspondiente.
  
4. Panel privado del Cliente (pages/dashboard-cliente.html): Aquí el cliente ve sus datos, la lista de todos sus coches con sus reparaciones activas, y un cajetín donde puede pegar nuevos localizadores para dejarlos guardados en su cuenta para siempre.
  
5. Panel del recepcionista (pages/dashboard-recepcionista.html): Tiene tres pestañas para hacer el trabajo del día a día: una para registrar un coche nuevo junto a su dueño, otra para abrir una nueva avería, y una lista para ver todos los clientes y coches que tenemos guardados.

6. Panel del mecánico (pages/dashboard-mecanico.html): Aquí cada mecánico ve una lista con solo las reparaciones que el jefe le ha asignado a él. Puede darle a un botón para cambiar el estado a "En proceso" o "Finalizada", y además tiene un apartado para marcar qué piezas ha gastado del almacén (por ejemplo, 2 filtros de aceite), lo que resta automáticamente esas piezas del stock para no tener que contarlas a mano.

7. Panel del administrador / jefe (pages/dashboard-admin.html): Es el panel más completo porque el jefe tiene que poder controlarlo todo. Desde aquí puede crear nuevos empleados, ver todas las reparaciones y decidir qué mecánico se encarga de cada una, y gestionar el almacén de piezas (con avisos automáticos en rojo si alguna pieza se está agotando).

8. Archivos compartidos (app.js y styles.css): Hemos creado un archivo de JavaScript común que se encarga de hablar con el servidor Java (haciendo peticiones a http://localhost:8080/api) y de mostrar los avisos y carteles en pantalla, además de una hoja de estilos para que toda la web tenga los mismos colores y botones.
<br>
<br>

## Información para usuarios:
Esta sección se tiene y se ira rellenando a medida que se vayan desarrollando el software. 
Si eres una persona que busca probar el software, de momento hay poco que se pueda utilizar, se recomienda leerse la wiki donde esclarecemos el funcionamiento de la aplicación que perseguimos y que tenemos en el momento.
<br>
<br>

### Feedback y comentarios
Para todos aquellos que probeís nuestra aplicación os agradeceriamos que si encontraseis algun "bug" o algun error en el software, nos lo reportaseis. También nos gustaría que nos dieseís vuestra opinicion (siempre que sea una critica constructiva) para mejorar y reforzar las funcionalidades, fortaleciendo el resultado final.
