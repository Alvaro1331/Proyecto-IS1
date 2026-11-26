# Proyecto-IS1

## NOMBRE DEL SOFTWARE: MOTORFLOW

### 🌐 Aplicación en Vivo (Despliegue en Microsoft Azure)

[![MotorFlow Live](https://img.shields.io/badge/MotorFlow-Online%20en%20Azure-0078D4?style=for-the-badge&logo=microsoftazure&logoColor=white)](https://motorflow-app-haagege7guh4fva4.swedencentral-01.azurewebsites.net)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge)](LICENSE)

👉 **Acceder a la aplicación web:** **[https://motorflow-app-haagege7guh4fva4.swedencentral-01.azurewebsites.net](https://motorflow-app-haagege7guh4fva4.swedencentral-01.azurewebsites.net)**

> *Desplegada en **Azure App Service (Java 17)** y conectada a **Azure Database for MySQL**, con actualización automática mediante **GitHub Actions** en cada `push` a `main`.*

Bienvenido al README de nuestro proyecto: un software encargado de la gestión integral de un taller mecánico. Con el fin de automatizar los procesos que existen en un taller, hemos desarrollado una aplicación web que permite:
- A los **clientes** pedir cita previa, consultar el estado de su coche en tiempo real, aceptar presupuestos y consultar facturas.
- A los **recepcionistas** registrar clientes y vehículos, gestionar citas, abrir nuevas órdenes de reparación y formalizar la entrega y cobro.
- A los **mecánicos** ver qué coches tienen asignados, gestionar su tiempo con un temporizador, actualizar el avance de la reparación, reportar incidencias con fotos y anotar los materiales usados.
- Al **mecánico jefe / administrador / dueño** supervisarlo todo: crear empleados, asignar reparaciones, controlar el inventario y la caja del taller, supervisar tiempos y dar el visto bueno final antes de entregar los coches.

---

## Índice

- [Equipo de desarrollo](#equipo-de-desarrollo)
- [Información para nuevos desarrolladores](#información-para-nuevos-desarrolladores)
  - [Tecnologías elegidas](#qué-tecnologías-hemos-elegido-y-por-qué)
  - [Cómo arrancar el proyecto en tu ordenador](#cómo-arrancar-el-proyecto-en-tu-ordenador)
  - [Organización del equipo](#organización-del-equipo)
  - [Estándar del entorno (JDK 17)](#estándar-del-entorno-y-versión-de-java-jdk-17)
  - [Suite de pruebas unitarias y seguridad](#suite-completa-de-pruebas-unitarias-y-seguridad-junit-5--mockito)
  - [Módulos avanzados](#módulos-y-arquitectura-avanzada)
  - [Documentación del repositorio](#documentación-del-repositorio)
- [Lógica principal del backend](#lógica-principal-del-backend)
- [Estructura del frontend](#estructura-del-frontend)
- [Información para usuarios](#información-para-usuarios)
  - [Guía rápida de uso según tu perfil](#guía-rápida-de-uso-según-tu-perfil)
  - [Cuentas de demostración](#cuentas-de-demostración-para-pruebas-inmediatas)
- [Feedback y comentarios](#feedback-y-comentarios)
- [Licencia](#licencia)

---

## Equipo de desarrollo

- Marta Barbero Fuentes
- Nicolás Rodríguez Torrevejano
- Marcos Arévalo Garrido
- Marcos Martín Clemente
- Álvaro Sánchez Camacho

---

## Información para nuevos desarrolladores

En la **Wiki** del repositorio está documentado el proceso de desarrollo, los hitos completados y las decisiones tomadas en cada iteración. Además, en el apartado de **Projects** se pueden consultar las historias de usuario relacionadas con el proyecto y su estado actual.

### ¿Qué tecnologías hemos elegido y por qué?

Estas son las herramientas que hemos utilizado para construir la aplicación:

- **Java 17 con Spring Boot 3.2.4 para el backend:** Es el lenguaje que conocemos de clase. Spring Boot nos facilita la creación de la API REST, la conexión con la base de datos mediante JPA/Hibernate, la validación de datos y el envío de correos.
- **Seguridad con BCrypt (`spring-security-crypto`):** Las contraseñas de empleados y clientes se almacenan cifradas mediante hashing BCrypt, con migración automática de credenciales heredadas al arrancar.
- **HTML, CSS y JavaScript nativo para el frontend:** Al no tener experiencia previa con frameworks como React o Angular, optamos por tecnologías estándar sin dependencias complejas, facilitando el desarrollo paralelo del equipo de frontend.
- **MySQL como base de datos definitiva:** Durante los primeros sprints usamos H2 en memoria para prototipar rápido sin instalaciones. Actualmente el sistema utiliza **MySQL** (`tallerdb`) para garantizar la persistencia real de los datos tanto en local como en Azure.
- **Git, GitHub y GitHub Actions:** Control de versiones (usando TortoiseGit como interfaz gráfica) y despliegue continuo (CI/CD) hacia Azure App Service.

### ¿Cómo arrancar el proyecto en tu ordenador?

Si te acabas de clonar el repositorio, sigue estos pasos:

1. **Requisitos previos:** Tener instalados **JDK 17** y **MySQL** en tu equipo.
2. **Base de datos MySQL:** Asegúrate de que el servicio de MySQL está en marcha. Por defecto, `backend/src/main/resources/application.properties` se conecta a `localhost:3306/tallerdb` con usuario `root` y contraseña `admin123`, e incluye `createDatabaseIfNotExist=true` para crear la base de datos automáticamente (aunque también puedes crearla manualmente con `CREATE DATABASE tallerdb;`). Si tus credenciales locales son distintas, puedes sobrescribirlas con las variables de entorno `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`.
3. **Arrancar el backend:**
   - **Desde IntelliJ IDEA:** Abre el proyecto `backend/` y ejecuta `TallerApplication` con el botón verde de Play.
   - **Desde consola (PowerShell, con el Maven incluido en el repo):** Situado en `backend/`, ejecuta:
     ```powershell
     .\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
     ```
4. **Abrir la aplicación:** Una vez arrancado el servidor en el puerto `8080`, accede desde el navegador a `http://localhost:8080` (o abre `frontend/index.html` en desarrollo local).
5. **Correo de recuperación de contraseña (opcional en local):** Si no defines las variables de entorno `SPRING_MAIL_HOST`, `SPRING_MAIL_USERNAME` y `SPRING_MAIL_PASSWORD`, el servidor funciona en modo desarrollo e imprime el código de recuperación de 6 dígitos directamente en la consola.

### Organización del equipo

Dado que somos 5 personas y que el proyecto tiene una parte visual (frontend) y una parte de lógica y datos (backend), decidimos dividir el equipo en dos áreas con dedicación flexible según las necesidades del proyecto:

- **Equipo Frontend:** Se encarga de todo lo que el usuario ve en pantalla (formularios, botones, tablas, diseño visual y selector de tema). Trabaja dentro de la carpeta `frontend/`.
- **Equipo Backend:** Se encarga de la lógica del servidor (recibir peticiones, validar datos, gestionar seguridad, guardar en base de datos y exponer la API REST). Trabaja dentro de la carpeta `backend/`.

### Estándar del Entorno y Versión de Java (JDK 17)

Para garantizar la compatibilidad entre todos los integrantes del equipo y evitar errores de compilación cruzada, el proyecto está estandarizado en **Java 17 (LTS)** (usando distribuciones como *Eclipse Temurin JDK 17* o *Microsoft OpenJDK 17*):

- Tanto `backend/pom.xml` como el flujo de GitHub Actions fijan la versión de Java en **17**.
- Se recomienda que cualquier nuevo colaborador configure su IDE con esta versión de JDK.

### Suite Completa de Pruebas Unitarias y Seguridad (JUnit 5 + Mockito)

El sistema dispone de una suite integral de **51 pruebas unitarias automatizadas** que garantizan el correcto funcionamiento y la seguridad del taller:

- **Ubicación del código de pruebas:** `backend/src/test/java/com/taller/PruebasUnitariasTest.java`
- **Módulos probados:**
  - **Cálculos y Contabilidad:** Comprobación matemática de facturas, desglose de mano de obra, márgenes comerciales del taller, IVA (21%) y presupuestos con líneas de piezas.
  - **Ciberseguridad y Control de Acceso (RBAC):** Bloqueo a mecánicos para crear órdenes de recepción o acceder a supervisión administrativa, bloqueo a recepcionistas para asignaciones técnicas, rechazo de credenciales incorrectas, bloqueo de usuarios inexistentes y protección antifraude en recuperación de contraseñas.
  - **Máquina de Estados de Reparación:** Validación estricta del ciclo de vida (`PENDIENTE` ➔ `EN_PROCESO` ➔ `FINALIZADA` ➔ `CONFIRMADA` ➔ `ENTREGADA` y `CANCELADA`), prohibiendo saltos ilegales, retrocesos o ediciones sobre reparaciones ya concluidas o entregadas.
  - **Almacén y Vehículos:** Validación de stock negativo, stock insuficiente, bloqueo de consumo de piezas sin presupuesto aceptado, matrículas duplicadas o vacías y años de fabricación inválidos.
  - **Gestión de Citas Previas:** Detección de fines de semana, festivos oficiales nacionales y cálculo de días hábiles.
  - **Priorización, Cola Cronológica, Temporizador y Supervisión (HU #7, #8, #19, #20, #21):** Ordenación dinámica de reparaciones por categoría, cola cronológica de espera `PENDIENTE`, ciclo Iniciar/Pausar/Terminar con bloqueo de tareas simultáneas, supervisión RBAC de tiempos por mecánico e historial técnico de solo lectura.
- **Cómo ejecutar las pruebas:**
  - **Desde IntelliJ IDEA:** Clic derecho sobre `PruebasUnitariasTest.java` y seleccionar *Run 'PruebasUnitariasTest'* (o atajo `Mayús+F10`). Se mostrarán los 51 checks verdes.
  - **Desde consola (Maven):** Situarse en la carpeta `backend/` y ejecutar:
    ```powershell
    .\apache-maven-3.9.6\bin\mvn.cmd test -Dtest=PruebasUnitariasTest
    ```
- **Documentación de pruebas entregable:** En la carpeta `Pruebas_unitarias/` están disponibles el informe técnico interactivo (`DOCUMENTO_CAMBIOS_Y_PRUEBAS.html`), su versión lista para imprimir (`DOCUMENTO_CAMBIOS_Y_PRUEBAS.pdf`) y el código íntegro (`CODIGO_PRUEBAS_UNITARIAS.txt`).

### Módulos y Arquitectura Avanzada

A lo largo de las iteraciones se han incorporado módulos clave para cubrir el flujo completo de un taller real:

1. **Módulo de Citas Previas y Calendario de Capacidad (`CitaPreviaService`):** Sistema de reserva con control de aforo (máximo 8 vehículos diarios y 2 por franja horaria), respetando fines de semana y festivos oficiales.
2. **Sistema de Presupuestos Formales y Facturación Real:** El cliente cuenta con un presupuesto detallado que debe aceptar antes de que el mecánico consuma piezas; tras finalizar, se genera automáticamente la factura con mano de obra real, materiales, margen de beneficio e IVA.
3. **Módulo de Incidencias con Fotos:** El mecánico puede reportar incidencias durante la reparación adjuntando explicaciones y fotografías (hasta 10 MB) que el cliente visualiza en su seguimiento web.
4. **Notificaciones Automáticas:** Avisos automáticos al cliente informándole de los cambios de estado de su vehículo (inicio de trabajo, presupuesto disponible, listo para recoger, etc.).
5. **Priorización, Cola Cronológica y Temporizador de Mecánico:** Ordenación de órdenes de trabajo por prioridad/categoría, cola cronológica de vehículos en espera, cronómetro de trabajo (`Iniciar` / `Pausar` / `Terminar`) que impide trabajar en dos reparaciones a la vez, supervisión de tiempos para el administrador e historial técnico de solo lectura.

### Documentación del repositorio

Además del código fuente, el repositorio incluye la documentación de gestión y calidad del proyecto:

- `Historias_de_usuario/`: Documento completo de historias de usuario en HTML y PDF (`DOCUMENTO_HISTORIAS_DE_USUARIO.html` y `DOCUMENTO_HISTORIAS_DE_USUARIO.pdf`).
- `Gestión_de_riesgos/`: Análisis y seguimiento de riesgos relevantes (`GESTION_DE_RIESGOS_RELEVANTES_ACTUALIZADO.docx` y `_ANALISIS DE RIESGOS_ACTUALIZADO.xlsx`).
- `Pruebas_unitarias/`: Informe técnico de cambios y pruebas unitarias (`DOCUMENTO_CAMBIOS_Y_PRUEBAS.html`, `DOCUMENTO_CAMBIOS_Y_PRUEBAS.pdf` y `CODIGO_PRUEBAS_UNITARIAS.txt`).

---

## Lógica principal del backend

El servidor Java con Spring Boot gestiona usuarios, clientes, inventario, citas, presupuestos, facturación, tesorería y el ciclo completo de cada reparación. Actualmente la API REST expone **74 endpoints** repartidos en 9 controladores (`Usuario`, `Cliente`, `Vehiculo`, `Material`, `Reparacion`, `CitaPrevia`, `Factura`, `Notificacion` y `ConfiguracionTaller`).

La información se organiza en las siguientes entidades principales:

- **Usuarios:** Personal del taller con rol `ADMIN`, `RECEPCIONISTA` o `MECANICO` y contraseñas cifradas con BCrypt.
- **Clientes:** Dueños de los vehículos, con soporte para registro rápido sin cuenta (nombre y teléfono) o cuenta personal con correo, contraseña y recuperación mediante código de 6 dígitos.
- **Vehículos:** Coches registrados con matrícula, marca, modelo, año y propietario asociado.
- **Materiales:** Inventario del almacén con piezas, precio y stock disponible (con alertas por bajo stock).
- **Reparaciones (núcleo del taller):** Unen vehículo, recepcionista, mecánico asignado, prioridad, avería, tiempos del temporizador y estado.
- **Reparación-Materiales:** Registra las piezas consumidas en cada coche, descontándolas automáticamente del inventario y del presupuesto asignado al mecánico.
- **Citas Previas:** Reservas de entrada al taller validadas contra calendario laboral y límite de aforo.
- **Presupuestos y Líneas de Presupuesto:** Desglose formal de piezas y mano de obra pendiente de aceptación por el cliente.
- **Facturas:** Documento económico generado automáticamente al finalizar la reparación con desglose de mano de obra, piezas, margen e IVA.
- **Incidencias de Reparación:** Imprevistos documentados por el mecánico con descripción y fotografías adjuntas.
- **Notificaciones a Clientes:** Historial de avisos generados automáticamente ante eventos de la reparación.
- **Configuración del Taller y Gastos de Tesorería:** Parámetros globales (margen comercial por defecto del 15%, mano de obra a 35 €/h y fondo inicial de caja de 2.000 €) junto al registro de gastos del taller.

Reglas de negocio destacadas:

- **Privacidad por Localizador:** En lugar de buscar coches por matrícula, cada reparación genera automáticamente un código alfanumérico único de 8 caracteres (por ejemplo: `Y6NDCVRP`), evitando que un cliente vea vehículos ajenos.
- **Flujo estricto de estados:** Una reparación no puede saltarse pasos ni retroceder. Sigue el orden obligatorio: `PENDIENTE` ➔ `EN_PROCESO` ➔ `FINALIZADA` (el mecánico acaba) ➔ `CONFIRMADA` (el administrador da el visto bueno) ➔ `ENTREGADA` (se cobra y el cliente se lleva el coche), además del estado `CANCELADA`.
- **Control económico y presupuestos:**
  - *Estimación de recepción:* importe orientativo calculado al dar entrada al vehículo.
  - *Presupuesto del mecánico:* tope máximo autorizado por el administrador para gastar en piezas.
  - *Presupuesto cliente y factura final:* cálculo real en función de piezas, mano de obra, margen comercial e impuestos.
  - *Caja del taller:* parte de un fondo inicial de 2.000 € y calcula ingresos, gastos y beneficios en tiempo real.

---

## Estructura del frontend

La interfaz web está desarrollada en **HTML, CSS y JavaScript nativo**, conectada al backend mediante `fetch` con datos reales de MySQL. Cuenta con diseño visual corporativo en naranja y soporte para **modo claro y modo oscuro** (`js/theme.js`). En el despliegue a Azure, GitHub Actions sincroniza automáticamente la carpeta `frontend/` dentro de `backend/src/main/resources/static/` para que Spring Boot sirva la web completa.

Pantallas y archivos principales:

1. **Página de inicio (`index.html`):** Portada de bienvenida con acceso directo a la consulta rápida por localizador, solicitud de cita previa e inicio de sesión.
2. **Cita previa (`pages/cita-previa.html`):** Calendario interactivo para reservar día y franja horaria consultando disponibilidad en tiempo real.
3. **Consulta para clientes (`pages/consulta-estado.html`):** Permite introducir el código localizador de 8 caracteres sin iniciar sesión y ver al instante el estado del vehículo, estimaciones e incidencias con fotos.
4. **Inicio de sesión (`pages/login.html`):** Con dos pestañas:
   - *Área Clientes:* Inicio de sesión, registro y recuperación de contraseña con código de 6 dígitos.
   - *Personal del Taller:* Acceso para administrador, recepcionista y mecánicos con redirección automática a su panel.
5. **Panel privado del Cliente (`pages/dashboard-cliente.html`):** Visualización de vehículos, vinculación de nuevos localizadores, notificaciones, aceptación de presupuestos online y consulta de facturas.
6. **Panel del Recepcionista (`pages/dashboard-recepcionista.html`):** Alta de clientes y vehículos, gestión de citas previas, apertura de órdenes de reparación, listado general y formalización de entrega y cobro.
7. **Panel del Mecánico (`pages/dashboard-mecanico.html`):** Cola de reparaciones asignadas con prioridad, temporizador de trabajo (`Iniciar` / `Pausar` / `Terminar`), consumo de piezas del almacén y registro de incidencias con fotografías.
8. **Panel del Administrador / Jefe (`pages/dashboard-admin.html`):** Gestión de empleados, asignación de averías, supervisión de tiempos por mecánico, historial técnico, control de inventario con alertas de bajo stock, confirmación de calidad, tesorería y configuración de tarifas/márgenes.
9. **Archivos compartidos (`js/app.js`, `js/theme.js` y `css/styles.css`):** `app.js` centraliza la comunicación con la API (detectando automáticamente `http://localhost:8080/api` en local y `/api` en producción) y la gestión de sesión/avisos; `theme.js` gestiona el selector de tema claro/oscuro sin parpadeo; y `styles.css` unifica el diseño en toda la aplicación.

---

## Información para usuarios

### Guía Rápida de Uso según tu Perfil

La aplicación cuenta con interfaces adaptadas a las necesidades de cada tipo de usuario:

#### 1. Si eres Cliente del Taller:
- **Pedir Cita Previa:** Desde la página de inicio o el formulario de citas puedes seleccionar el día y la hora que prefieras. El sistema muestra las plazas disponibles en tiempo real (máximo 2 coches por hora y 8 al día) y bloquea fines de semana y festivos oficiales.
- **Consultar tu coche sin registrarte (Por Localizador):** Si dejaste el coche en el taller y te dieron tu resguardo con un código localizador de 8 caracteres (ej. `Y6NDCVRP`), puedes entrar en *Consulta de Estado*, introducir el código y ver en qué fase está tu vehículo, los días estimados restantes y las fotos de las incidencias que suba el mecánico.
- **Crear una cuenta personal:** En la pestaña *Área Clientes* de la página de inicio de sesión puedes registrarte con tu correo. Desde tu panel personal podrás:
  - Ver todos los coches asociados a tu teléfono y tus notificaciones.
  - Vincular nuevas reparaciones pegando su código localizador.
  - Revisar el presupuesto formal desglosado (piezas y mano de obra) y aceptarlo online para autorizar la reparación.
  - Consultar tu factura detallada una vez el coche esté listo.
- **¿Has olvidado tu contraseña?** Dispones de la opción *Recuperar contraseña*, donde introduces tu correo y el sistema genera un código de verificación de 6 dígitos.

#### 2. Si eres Recepcionista:
- Inicia sesión con tus credenciales de recepción (ej. `laura@taller.com` / `laura123`).
- **Registrar entrada de vehículos:** Da de alta el coche indicando matrícula, marca, modelo y los datos del dueño. Si el cliente ya vino antes, el sistema lo reconoce automáticamente por su teléfono.
- **Gestionar citas y abrir órdenes de reparación:** Consulta las citas del día, describe la avería indicada por el cliente, introduce una estimación orientativa y los días estimados de trabajo. Se generará de inmediato el código localizador único.
- **Entregar y Cobrar:** Cuando la reparación esté confirmada por el administrador, podrás formalizar la entrega al cliente y cobrar la factura.

#### 3. Si eres Mecánico:
- Inicia sesión con tu cuenta técnica (ej. `pedro@taller.com` / `pedro123`).
- **Tus coches asignados y temporizador:** Verás las reparaciones que el jefe de taller te ha asignado ordenadas por prioridad/cola. Puedes iniciar, pausar y terminar el temporizador de trabajo (el sistema evita tener dos reparaciones activas al mismo tiempo).
- **Consumo de piezas del almacén:** Una vez que el cliente haya aceptado el presupuesto, puedes registrar los materiales y piezas que vayas instalando (baterías, aceite, filtros...), descontándose automáticamente del stock.
- **Incidencias y fotos:** Si encuentras una avería oculta o un imprevisto, puedes registrar una incidencia adjuntando una fotografía y explicando el motivo para informar al cliente con total transparencia.
- **Finalizar trabajo:** Registra las horas reales de mano de obra empleadas y marca el estado como *Finalizada* para que el administrador la revise.

#### 4. Si eres Administrador / Dueño del Taller:
- Inicia sesión con la cuenta de administración (`admin@taller.com` / `admin123`).
- **Gestión de Personal:** Alta, consulta y asignación de roles a los empleados del taller.
- **Asignación de averías y supervisión:** Asigna cada reparación abierta al mecánico más adecuado, supervisa los tiempos invertidos por cada mecánico y consulta el historial técnico.
- **Control de Inventario y Almacén:** Añade piezas, modifica precios y consulta alertas visuales automáticas en rojo cuando el stock de algún material baje del umbral mínimo.
- **Confirmación de Calidad:** Revisa las reparaciones finalizadas por los mecánicos y confírmalas para dar luz verde a su entrega y facturación.
- **Caja y Configuración del Taller:** Modifica el margen de beneficio comercial (por defecto 15%) y el precio oficial de la hora de mano de obra (por defecto 35 €/h), supervisando los ingresos y gastos reales del negocio.

### Cuentas de Demostración para Pruebas Inmediatas

Para probar todas las funciones sin necesidad de registrar datos manualmente, puedes utilizar las siguientes cuentas preparadas en el sistema:

| Rol | Correo Electrónico | Contraseña | Uso Principal |
| :--- | :--- | :--- | :--- |
| **Administrador** | `admin@taller.com` | `admin123` | Supervisión total, almacén, caja y confirmación de reparaciones |
| **Recepcionista** | `laura@taller.com` | `laura123` | Recepción de coches, citas previas, apertura de averías y cobro |
| **Mecánico** | `pedro@taller.com` | `pedro123` | Órdenes asignadas, temporizador, consumo de piezas, fotos e incidencias |
| **Cliente** | `juan@email.com` | `juan123` | Seguimiento en tiempo real, aceptación de presupuesto y facturas |

---

## Feedback y comentarios

A todos los que probéis nuestra aplicación, os agradeceríamos que si encontráis algún *bug* o error en el software nos lo reportéis. También nos gustaría que nos dieseis vuestra opinión (siempre desde la crítica constructiva) para mejorar y reforzar las funcionalidades del resultado final.

---

## Licencia

Este proyecto está distribuido bajo la licencia **GNU General Public License v3.0 (GPL-3.0)**. Consulta el archivo [LICENSE](LICENSE) para más detalles.
