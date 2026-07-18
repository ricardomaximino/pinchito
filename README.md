mvn clean install && mvn -Pnative native:compile && docker build -f docker/Dockerfile --tag ricardomaximino/pinchito . && docker push ricardomaximino/pinchito:latest

# Pinchito - Gestor de Deudas Amorosas 💜

**Pinchito** es una aplicación web gamificada para la gestión de "deudas amorosas" (favores y juegos picantes o románticos) diseñada para parejas. Permite registrar promesas, reclamar su cumplimiento, y validar o rechazar los pagos en tiempo real con una interfaz moderna y atractiva.

---

## Propósito

El propósito de la aplicación es dinamizar la comunicación y el juego en la relación mediante un panel de control interactivo donde los miembros pueden:
*   **Declarar deudas**: Asignar favores pendientes que tu pareja debe cumplir (con categorías predefinidas y divertidas localizadas para España).
*   **Reclamar pagos**: Declarar cuándo y cómo se ha cumplido una deuda.
*   **Aceptar u oponerse**: Validar el pago para archivarlo en el historial o rechazarlo añadiendo una objeción si no se considera cumplido.

---

## Estado del Proyecto y Características

La plataforma está completamente desarrollada y optimizada para despliegues modernos (incluyendo entornos serverless y contenedores como Google Cloud Run):

1.  **Alineación y Estilo Premium**: Interfaz en español con temática oscura y diseño *glassmorphism* (efectos de desenfoque, degradados violetas y bordes brillantes).
2.  **Sincronización en Tiempo Real**: Integración con **Spring WebSockets (STOMP + SockJS)**. Los cambios realizados por un usuario (deudas nuevas, declaraciones, confirmaciones) muestran notificaciones flotantes (*toasts*) instantáneas al otro miembro y refrescan la pantalla de forma automática sin alterar la pestaña activa del usuario.
3.  **Persistencia Asíncrona (Write-Through)**:
    *   **Lecturas en memoria**: Los datos se precargan en una caché activa al arrancar para garantizar tiempos de respuesta inmediatos y evitar límites de tasa (*rate limits*) de la API de GitHub.
    *   **Escrituras en segundo plano**: Cualquier cambio se guarda instantáneamente en memoria y se encola en un hilo secundario asíncrono para guardarse inmediatamente en el almacenamiento de respaldo (GitHub o disco local). Esto evita pérdidas de datos ante apagados abruptos en la nube.
4.  **Soporte de Compilación Nativa (GraalVM Native Image)**:
    *   Incluye un registrador de pistas de ejecución AOT (`PinchitoRuntimeHints`) para registrar la reflexión de Jackson 2 y Thymeleaf.
    *   Configura manejadores de señales y salida (`--install-exit-handlers`) en el plugin nativo.
    *   Implementa `DisposableBean` en el almacenamiento en caché para garantizar que los callbacks de cierre de contexto se ejecuten de manera determinista en ejecutables nativos.
5.  **Nombres de Usuario Insensibles a Mayúsculas**: Inicio de sesión flexible (puedes entrar como `alice`, `ALICE` o `Alice`) manteniendo la capitalización de registro en la sesión, con validación de unicidad de nombres case-insensitive al crear la cuenta.
6.  **Simulación de Apagado (SIGTERM)**: Expone un endpoint de desarrollo `GET /shutdown` para forzar un cierre limpio del contexto Spring y comprobar la sincronización del almacenamiento.

---

## Configuración y Variables de Entorno

La aplicación soporta almacenamiento local en formato JSON o persistencia remota mediante la API de GitHub:

| Variable de Entorno | Descripción | Valor por Defecto |
| :--- | :--- | :--- |
| `GITHUB_TOKEN` | Token de acceso personal de GitHub (si se define, activa almacenamiento en la nube). | *(Vacío - activa almacenamiento local)* |
| `GITHUB_OWNER` | Usuario o nombre de la organización del repositorio. | `ricardomaximino` |
| `GITHUB_REPO` | Nombre del repositorio de datos. | `pichito-data` |
| `GITHUB_BRANCH` | Rama donde guardar los archivos JSON. | `master` |
| `pinchito.local.storage-path` | Directorio local alternativo si no hay token de GitHub. | `${user.home}/.pinchito/accounts` |

---

## Cómo Ejecutar el Proyecto

### 1. Ejecución Local en Desarrollo (JVM)

Asegúrate de configurar tu token si deseas probar la persistencia en GitHub:

```bash
# En Windows (PowerShell)
$env:GITHUB_TOKEN="tu_token_de_github"
$env:GITHUB_REPO="pinchito"
./mvnw spring-boot:run

# En Linux / macOS
export GITHUB_TOKEN="tu_token_de_github"
export GITHUB_REPO="pinchito"
./mvnw spring-boot:run
```

La aplicación estará disponible en `http://localhost:8080`.

### 2. Compilar Imagen Nativa con GraalVM

Para generar el ejecutable nativo optimizado:
```bash
mvn clean install
mvn -Pnative native:compile
```
El binario resultante se generará en `target/pinchito`.

### 3. Construcción y Despliegue con Docker

Para empaquetar el binario nativo en un contenedor liviano de Alpine y publicarlo:
```bash
docker build -f docker/Dockerfile --tag ricardomaximino/pinchito .
docker push ricardomaximino/pinchito:latest
```