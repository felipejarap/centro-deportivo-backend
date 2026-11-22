# Centro Deportivo — Backend

Backend Spring Boot para el frontend `centro-deportivo-react`. Este repositorio contiene solo los servicios que usa esa aplicación:

- Eureka Server y API Gateway
- Auth, Users, Classes, Plans, Reservations, Assistance y Subscription
- MySQL y migraciones de base de datos

Notification, Coaches, Medical Record y Exercise Routine no forman parte de este despliegue.

## Requisitos

- Docker Desktop con Docker Compose
- Puertos locales disponibles: 3307, 8090 y 8761

## Iniciar localmente

1. Copia `.env.example` a `.env`.
2. Cambia `MYSQL_ROOT_PASSWORD` y `JWT_SECRET` por valores locales propios. El secreto JWT debe tener al menos 32 caracteres.
3. Ejecuta:

   ```powershell
   docker compose up --build -d
   docker compose ps
   ```

El Gateway queda en `http://localhost:8090` y Eureka en `http://localhost:8761`.
Los microservicios solo son accesibles dentro de la red de Docker; MySQL y Eureka se enlazan a loopback para desarrollo local. Si hay conflictos de puertos, puedes configurar `MYSQL_PORT`, `EUREKA_PORT` y `GATEWAY_PORT` en el entorno antes de iniciar Compose.
En Vite, configura el proxy `/api` hacia `http://localhost:8090`.
Swagger está disponible a través del Gateway en `/docs/{servicio}/api-docs`; por ejemplo, `http://localhost:8090/docs/classes/api-docs`. La especificación OpenAPI JSON correspondiente está en `/docs/{servicio}/v3/api-docs`.

## Autenticación y permisos

El Gateway valida JWT y aplica permisos por rol. Solo el login, registro de alumnos, Swagger de solo lectura y catálogos de lectura son públicos; las demás rutas requieren token. El acceso dura 15 minutos y se renueva con un refresh token rotatorio de siete días, revocado al cambiar la contraseña. Los alumnos acceden a su perfil, reservas y suscripciones mediante rutas asociadas a su propio ID; profesores solo pueden consultar y gestionar asistencia de clases que tienen asignadas; las operaciones administrativas quedan restringidas al rol administrador.

El microservicio de autenticación acepta refresh tokens únicamente en `POST /api/v1/auth/refresh`; el resto de sus rutas requiere un access token. La renovación de una suscripción usa la duración vigente del plan asociado y rechaza el proceso si el plan ya no existe o su duración no es válida.

El registro web envía los datos del alumno y su contraseña juntos a `POST /api/v1/auth/register`. Auth crea el perfil con rol Alumno fijo en Users y luego las credenciales; si guardar las credenciales falla, intenta eliminar el perfil recién creado. El endpoint de registro de Users no se expone como ruta pública del Gateway, y la solicitud no acepta un ID de usuario existente que pueda ser reclamado. Users también rechaza correos que ya están asociados a perfiles. El flujo no verifica la titularidad del buzón: para exigirla, configura un proveedor de correo y un proceso de verificación con token de un solo uso antes de activar la cuenta.

Para consultar logs: `docker compose logs -f`.
Para detener los contenedores sin borrar la base: `docker compose down`.
Los datos de MySQL persisten en el volumen `mysql_data`; para borrarlos explícitamente: `docker compose down -v`.

## Cuentas

El perfil `dev` incluye una cuenta administrativa de demostración creada por migraciones separadas de las de producción:

- Usuario: `admin.demo@kronofit.local`
- Contraseña: `KronoFit-Demo-2026!`

Úsala solo en el entorno local para probar el panel de administración. No reutilices esa contraseña ni actives las migraciones de desarrollo en producción. En `prod` no se crea una cuenta administrativa predeterminada.

## Configuración

No subas `.env` ni claves privadas. Los valores de `.env.example` son marcadores solo para desarrollo local; reemplázalos antes de iniciar la aplicación. Para otros entornos, inyecta las variables de configuración desde un gestor seguro de secretos.
