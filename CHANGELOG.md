# Changelog

## Unreleased

## 0.7.1

- "Sin límites" solo pregunta una vez al día: si eliges sin límites para una app, el resto del día se abre directamente sin diálogo.

## 0.7.0

- Seguimiento financiero automático: intercepta notificaciones de apps bancarias (BBVA, CaixaBank, Bizum, Fiare, Revolut y más) y genera un CSV diario en Documents/minilauncher-finances/ para sincronizar con Nextcloud.
- Nueva sección en Ajustes para conceder los permisos necesarios (acceso a notificaciones y acceso a archivos) y ver las instrucciones de configuración de Nextcloud.

## 0.6.3

- Fallback por palabra clave en el paquete cuando una app de reloj, teléfono o cámara no declara los intents estándar.
- Esto resuelve casos en los que el dispositivo tiene la app pero el launcher no la encontraba.

## 0.6.2

- Soporte real para anclar accesos directos de apps (chats de Telegram, WhatsApp, etc.) al launcher.
- Nueva actividad que recibe las solicitudes de anclar shortcut y los registra en Minilauncher.

## 0.6.1

- Corrección del bug del reloj que mostraba un selector vacio cuando no habia app de alarmas instalada.
- Mejora de la deteccion de apps de reloj, telefono, camara y navegador con queries explicitas en Android 11+.

## 0.6.0

- Nueva pantalla de etiquetas accesible deslizando a la derecha desde la pantalla principal.
- Filtrado de apps por etiqueta con acciones completas (favorita, ocultar, editar etiquetas).
- Soporte para accesos directos de aplicaciones (shortcuts), como chats de Telegram.
- Los shortcuts aparecen en el listado con su icono real y se abren directamente al pulsar.

## 0.5.0

- Cambio de identificador de aplicacion a `es.sasogu.minilauncher` para desbloquear la publicacion en Google Play.
- Preparacion de firma release con keystore externo mediante `key.properties`.
- Documentacion de release alineada con la nueva version y la pausa temporal del envio a F-Droid.

## 0.4.0

- Selector de zona horaria del sistema integrado en home.
- Botones para limpiar con un clic en campos de búsqueda.
- Alineación mejorada del meridiano (AM/PM) en reloj de 12 horas.
- Toggles en header de home para mostrar/ocultar día de la semana y formato de hora 12h/24h.
- Corrección de orientación de fases lunares (creciente/menguante).
- Opción para resetear el launcher a pantalla principal al tocar intent HOME.
- Porcentaje configurable de iluminación lunar en home.

## 0.3.0

- Etiquetas por aplicacion con edicion directa desde el listado.
- Busqueda por etiquetas tanto en la home como en la pantalla completa de apps.
- Exportacion e importacion de backup local desde ajustes.
- Busqueda en la home ampliada a todas las apps visibles, no solo favoritas.
- Mejor reparto visual del listado, con acciones colocadas debajo del texto para dar mas espacio a nombres largos.
- Mensajes claros cuando faltan apps del sistema como reloj, telefono, camara o navegador.

## 0.2.0

- Pantalla de ajustes funcional para las opciones principales.
- Selector de idioma movido a ajustes.
- Cambio de tema claro y oscuro.
- Busqueda en home y en el listado completo de aplicaciones.
- Reordenacion rapida de favoritas con pulsacion larga.
- Cache de iconos y carga incremental de aplicaciones.
- Permiso de superposicion integrado para mejorar el retorno del launcher al vencer el tiempo.
- Flujo de release y metadata de fastlane/F-Droid actualizados para la nueva version.

## 0.1.0

- Primera version publica del launcher.
- Home minimalista con reloj, fecha y aro de bateria.
- Apertura de alarmas al tocar el reloj.
- Pantalla separada para todas las aplicaciones.
- Favoritas persistentes en la home.
- Accesos fijos a telefono y camara.
- Dialogo de intencion de uso antes de abrir apps.
- Recordatorio local opcional tras el tiempo elegido.
