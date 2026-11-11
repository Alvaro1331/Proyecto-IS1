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

### Estándar del Entorno y Versión de Java (JDK 17)
Para garantizar la compatibilidad entre todos los integrantes del equipo y evitar errores de compilación cruzada, el proyecto está completamente estandarizado en **Java 17 (LTS)** (usando distribuciones como *Eclipse Temurin JDK 17* o *Microsoft OpenJDK 17*).
* Tanto el archivo `pom.xml` de Maven como la configuración de IntelliJ IDEA (`.idea/misc.xml`) tienen fijado el nivel de lenguaje en Java 17.
* Se recomienda que cualquier nuevo colaborador configure su IDE con esta versión de JDK.

### Suite Completa de Pruebas Unitarias y Seguridad (JUnit 5 + Mockito)
El sistema dispone de una suite integral de **33 pruebas unitarias automatizadas** que garantizan el correcto funcionamiento y la seguridad del taller:
* **Ubicación del código de pruebas:** `backend/src/test/java/com/taller/PruebasUnitariasTest.java`
* **Módulos probados:**
  * **Cálculos y Contabilidad:** Comprobación matemática de facturas, desglose de mano de obra, márgenes comerciales del taller, IVA (21%) y presupuestos con líneas de piezas.
  * **Ciberseguridad y Control de Acceso (RBAC):** Bloqueo a mecánicos para crear órdenes de recepción, bloqueo a recepcionistas para asignaciones técnicas, rechazo de credenciales incorrectas, bloqueo de usuarios inexistentes y protección antifraude en recuperación de contraseñas.
  * **Máquina de Estados de Reparación:** Validación estricta del ciclo de vida (`PENDIENTE` ➔ `EN_PROCESO` ➔ `FINALIZADA` ➔ `CONFIRMADA` ➔ `ENTREGADA`), prohibiendo saltos ilegales, retrocesos o ediciones sobre reparaciones ya entregadas.
  * **Almacén y Vehículos:** Validación de stock negativo, stock insuficiente, bloqueo de consumo de piezas sin presupuesto aceptado, matrículas duplicadas o vacías y años de fabricación inválidos.
  * **Gestión de Citas Previas:** Detección de fines de semana, festivos oficiales nacionales y cálculo de días hábiles.
* **Cómo ejecutar las pruebas:**
  * **Desde IntelliJ IDEA:** Clic derecho sobre `PruebasUnitariasTest.java` y seleccionar *Run 'PruebasUnitariasTest'* (o atajo `Mayús+F10`). Se mostrarán los 33 checks verdes.
  * **Desde consola (Maven):** Situarse en la carpeta `backend/` y ejecutar:
    ```powershell
    .\apache-maven-3.9.6\bin\mvn.cmd test -Dtest=PruebasUnitariasTest
    ```
* **Documentación de pruebas entregable:** En la carpeta `Pruebas_unitarias/` se encuentran disponibles el informe técnico interactivo (`DOCUMENTO_CAMBIOS_Y_PRUEBAS.html`), su versión lista para imprimir (`DOCUMENTO_CAMBIOS_Y_PRUEBAS.pdf`) y el código íntegro (`CODIGO_PRUEBAS_UNITARIAS.txt`).

### Nuevos Módulos y Arquitectura Avanzada
En las últimas iteraciones se han incorporado los siguientes módulos para completar los requisitos del taller:
1. **Módulo de Citas Previas y Calendario de Capacidad (`CitaPreviaService`):** Sistema de reserva con control de aforo (máximo 8 vehículos diarios y 2 por franja horaria), respetando fines de semana y festivos oficiales.
2. **Sistema de Presupuestos Formales y Facturación Real:** El cliente cuenta con un presupuesto detallado que debe aceptar antes de que el mecánico consuma piezas; tras finalizar, se genera automáticamente la factura con mano de obra real, materiales, margen de beneficio e impuestos.
3. **Módulo de Incidencias con Fotos:** El mecánico puede reportar incidencias durante la reparación adjuntando explicaciones y fotografías que el cliente puede visualizar en su seguimiento web.
4. **Notificaciones Automáticas:** Avisos automáticos al cliente informándole de los cambios de estado de su vehículo (inicio de trabajo, listo para recoger, etc.).

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
<br>
<br>

### Guía Rápida de Uso según tu Perfil

La aplicación cuenta con diferentes interfaces adaptadas a las necesidades de cada tipo de usuario:

#### 1. Si eres Cliente del Taller:
* **Pedir Cita Previa:** Desde la página de inicio o el formulario de citas puedes seleccionar el día y la hora que prefieras. El sistema te mostrará qué tramos tienen plazas disponibles en tiempo real (máximo 2 coches por hora y 8 al día) y no te permitirá elegir fines de semana o festivos oficiales.
* **Consultar tu coche sin registrarte (Por Localizador):** Si dejaste el coche en el taller y te dieron tu resguardo con un código localizador de 8 caracteres (ej. `Y6NDCVRP`), puedes entrar en *Consulta de Estado*, introducir el código y ver exactamente en qué estado se encuentra tu vehículo, los días estimados que le quedan y las fotos de las incidencias que suba el mecánico.
* **Crear una cuenta personal:** En la pestaña *Área Clientes* de la página de inicio de sesión puedes registrarte con tu correo. Una vez dentro de tu panel personal podrás:
  * Ver todos los coches asociados a tu teléfono.
  * Vincular nuevas reparaciones pegando su código localizador.
  * Ver el presupuesto formal desglosado (piezas y mano de obra) y aceptarlo online para autorizar la reparación.
  * Descargar o consultar tu factura detallada una vez el coche esté listo.
* **¿Has olvidado tu contraseña?** Dispones de la opción *Recuperar contraseña*, donde introduces tu correo y el sistema te genera un código de verificación seguro de 6 dígitos.

#### 2. Si eres Recepcionista:
* Inicia sesión con tus credenciales de recepción (ej. `laura@taller.com` / `laura123`).
* **Registrar entrada de vehículos:** Da de alta el coche indicando matrícula, marca, modelo y los datos del dueño. Si el cliente ya vino antes, el sistema lo reconoce automáticamente por su teléfono.
* **Abrir orden de reparación:** Describe la avería indicada por el cliente, introduce una estimación orientativa de precio y los días estimados de trabajo. Se generará de inmediato el código localizador único.
* **Entregar y Cobrar:** Cuando la reparación esté confirmada por el administrador, podrás formalizar la entrega al cliente y cobrar la factura.

#### 3. Si eres Mecánico:
* Inicia sesión con tu cuenta técnica (ej. `pedro@taller.com` / `pedro123`).
* **Tus coches asignados:** Verás únicamente la lista de los vehículos que el jefe de taller te ha encomendado.
* **Consumo de piezas del almacén:** Una vez que el cliente haya aceptado el presupuesto, puedes ir registrando los materiales y piezas que vayas instalando (baterías, aceite, filtros...), descontándose automáticamente del stock.
* **Incidencias y fotos:** Si encuentras una avería oculta o un imprevisto, puedes registrar una incidencia adjuntando una fotografía y explicando el motivo, permitiendo que el cliente esté siempre informado con total transparencia.
* **Finalizar trabajo:** Registra las horas reales de mano de obra empleadas y marca el estado como *Finalizada* para que el administrador la revise.

#### 4. Si eres Administrador / Dueño del Taller:
* Inicia sesión con la cuenta de administración (`admin@taller.com` / `admin123`).
* **Gestión de Personal:** Alta, consulta y asignación de roles a los empleados del taller.
* **Asignación de averías:** Asigna cada reparación abierta al mecánico más adecuado.
* **Control de Inventario y Almacén:** Añade piezas, modifica precios y consulta alertas visuales automáticas en rojo cuando el stock de algún material baje de un umbral mínimo.
* **Confirmación de Calidad:** Revisa las reparaciones finalizadas por los mecánicos y confírmalas para dar luz verde a su entrega y facturación.
* **Caja y Configuración del Taller:** Modifica el margen de beneficio comercial (por defecto 15%) y el precio oficial de la hora de mano de obra (por defecto 35 €/h), supervisando los ingresos y gastos reales del negocio.

### Cuentas de Demostración para Pruebas Inmediatas
Para probar todas las funciones sin necesidad de registrar datos manualmente, puedes utilizar las siguientes cuentas preparadas en el sistema:

| Rol | Correo Electrónico | Contraseña | Uso Principal |
| :--- | :--- | :--- | :--- |
| **Administrador** | `admin@taller.com` | `admin123` | Supervisión total, almacén, caja y confirmación de reparaciones |
| **Recepcionista** | `laura@taller.com` | `laura123` | Recepción de coches, citas previas, apertura de averías y cobro |
| **Mecánico** | `pedro@taller.com` | `pedro123` | Órdenes asignadas, consumo de piezas, subida de fotos e incidencias |
| **Cliente** | `juan@email.com` | `juan123` | Seguimiento en tiempo real, aceptación de presupuesto y facturas |

### Feedback y comentarios
Para todos aquellos que probeís nuestra aplicación os agradeceriamos que si encontraseis algun "bug" o algun error en el software, nos lo reportaseis. También nos gustaría que nos dieseís vuestra opinicion (siempre que sea una critica constructiva) para mejorar y reforzar las funcionalidades, fortaleciendo el resultado final.
