# AppGrabarAudio

Aplicación Android para grabar audio con el micrófono del teléfono, guardar un historial de grabaciones y reproducirlas.

## Funciones

- Grabar y detener una grabación de audio.
- Guardar cada grabación en el almacenamiento interno de la app (formato `.m4a`).
- Registrar el nombre, la ruta y la fecha de cada audio en una base de datos local.
- Ver el historial de todos los audios y reproducir cualquiera con el botón **Play**.
- Mostrar en la pantalla principal el **último audio escuchado**, que se conserva aunque se cierre la app.

## Tecnologías

| Componente | Detalle |
|---|---|
| Lenguaje | Kotlin 2.2.10 |
| Interfaz | Jetpack Compose con Material 3 |
| Base de datos | Room (con KSP) |
| Grabación | `MediaRecorder` |
| Reproducción | `MediaPlayer` |
| Preferencias | `SharedPreferences` (guarda el último audio escuchado) |
| Gradle | Android Gradle Plugin 9.4.1 |
| SDK | minSdk 24, targetSdk 37 |
| Java | 11 |

## Requisitos

- Android Studio reciente (compatible con Android Gradle Plugin 9.4.1).
- Conexión a internet la primera vez, para descargar las dependencias de Gradle.
- Un celular Android con Android 7.0 (API 24) o superior.

## Cómo ejecutar el proyecto

1. Clona el repositorio:
   ```
   https://github.com/RauuLCLOUD/AppGrabarAudio.git
   ```
   En Android Studio: *File > New > Project from Version Control*.
2. Espera a que Gradle termine de sincronizar.
3. En el celular, activa las **Opciones de desarrollador**: en *Ajustes > Acerca del teléfono*, toca 7 veces el *Número de compilación*.
4. Dentro de las opciones de desarrollador, activa la **Depuración USB**.
5. Conecta el celular a la PC con un cable USB y acepta la autorización en el teléfono.
6. Selecciona el celular en la lista de dispositivos de Android Studio y pulsa **Run**.
7. Al abrir la app, acepta el permiso de **micrófono**.

> Se recomienda probar en un celular real. El micrófono del emulador es poco confiable y puede producir audios en silencio.

## Permisos

| Permiso | Uso |
|---|---|
| `RECORD_AUDIO` | Grabar audio con el micrófono. Se solicita al abrir la app y de nuevo al pulsar Grabar si fue denegado. |

## Estructura del proyecto

```
AppGrabarAudio/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/example/appgrabaraudio/
│           ├── MainActivity.kt      Pantallas, grabación y reproducción
│           ├── Database.kt          Entidad, DAO y base de datos (Room)
│           └── ui/theme/            Colores, tipografía y tema
├── gradle/libs.versions.toml        Versiones de dependencias
├── build.gradle.kts
└── settings.gradle.kts
```

## Descripción de los archivos principales

### `MainActivity.kt`
- **`MainActivity`**: controla el permiso de micrófono, inicia y detiene la grabación con `MediaRecorder`, reproduce audios con `MediaPlayer` y guarda en `SharedPreferences` el id del último audio escuchado.
- **`MainScreen`**: pantalla principal con los botones Grabar, Parar, Reproducir e Historial, y el nombre del último audio escuchado.
- **`HistoryScreen`**: lista de todos los audios grabados, cada uno con su botón Play.

### `Database.kt`
- **`AudioRecord`**: entidad con `id`, `filePath`, `fileName` y `timestamp`.
- **`RecordingDao`**: inserta grabaciones, obtiene todas ordenadas de la más reciente a la más antigua y obtiene la última.
- **`AppDatabase`**: base de datos Room (`audio_database`), con una única instancia compartida.

## Flujo de la app

1. El usuario pulsa **Grabar**. La app verifica el permiso de micrófono; si falta, lo solicita.
2. `MediaRecorder` guarda el audio en la carpeta interna de la app como `audio_<fecha>.m4a`.
3. Al pulsar **Parar**, si la grabación fue válida se inserta un registro en Room. Si fue demasiado corta, el archivo se elimina.
4. En **Historial** se listan los audios. Al pulsar **Play**, se reproduce el audio y queda guardado como el último escuchado.
5. La pantalla principal muestra ese último audio y permite reproducirlo de nuevo.

## Notas

- Los audios se guardan en el almacenamiento interno de la app, por lo que se eliminan si se desinstala.
- La base de datos guarda solo la información del audio, no el audio en sí.
