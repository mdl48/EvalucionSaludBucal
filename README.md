# EvalacionSaludBucal
Aplicación para la recolección de datos de salud bucal para estudios de la población

Esta aplicación esta dividida en dos programas:

## Cliente

Un cliente hecho en base a web con HTML/CSS/JS con una aplicación nativa de teléfono hecho para la plataforma Android usando la tecnologia webview de la misma para el desarrollo de la inferfaz unificada para dispositivos moviles y de escritorio.

Este cliente cuenta con un formulario el cual inicialmente es guardado en el dispositivo del usuario en una base de datos SQLite para posteriormente ser subido al servidor local montado en LUZ, posterior al inicio de la misma el usuario debe iniciar sesión con sus credenciales de cedula y contraseña.

## Servidor

Un simple servidor montado localmente en LUZ para la recolección de datos de todos los clientes. Estos seran almacenados en una base de datos de MySQL. Los datos seran posteriormente exportados a CSV o Excel.

## Construcción del programa

### Android

Se debe tener instalado:
- Un entorno de linux (POSIX sh, sed, find, grep, etc)
- El programa de linea de comando zip
- El directorio del SDK de android en una variable de entorno llamada ANDROID_HOME
- Las herramientas de construccion de android (build-tools) en la variable de entorno PATH
- Compilador de java version 11 (OpenJDK)

Posteriormente se ejecuta el script 'android-build' en la carpeta webview para compilar el APK.

### Web

El archivo form.html en webview/assets fue generado a partir del archivo webview/form.jinja.html con el script de python3 webview/gen.py, este requiere de la libreria jinja2

## Duración

El projecto empezo su desarrollo aproximadamente el 8/5/25 con su estimado tiempo de desarrollo siendo 2 semanas.

## Desarrolladores

Facultad de Odontologia de LUZ.
