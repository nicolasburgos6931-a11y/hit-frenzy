# Hit Frenzy

Mod de Fabric para Minecraft 26.3. Hecho por **ElBatzyMC**, con la ayuda de **Claude (Anthropic)**.

## Que hace
- Numeros flotantes con el daño real de cada golpe.
- Efectos: ¡CRÍTICO!, Meh... y ¡KILL!!
- Contador de combo (x2, x3, x4...) que crece y cambia de color como loco.
- Mensajes especiales a mitad del combo (x5, x10, x20, x30, x50).
- Al terminar el combo, un cartel segun lo bien que te fue: Meh..., ¡Bien!, ¡GENIAL!, ¡ASOMBROSO!!
- Sonidos y particulas. Todo se puede apagar o cambiar.

## Compilar sin instalar nada (GitHub Actions)
1. Crea un repositorio nuevo en github.com.
2. Sube TODO el contenido de esta carpeta, incluida la carpeta oculta `.github`.
   Si la web no te deja subirla: "Add file > Create new file", escribe
   `.github/workflows/build.yml` como nombre y pega el contenido de ese archivo.
3. Pestaña **Actions**: espera a que termine "build" (3-5 minutos).
4. Entra a la ejecucion terminada y baja **Artifacts**. Dentro esta `hit-frenzy-1.0.0.jar`.
   (El archivo `-sources.jar` no lo necesitas.)

## Instalar
En la carpeta `mods` de tu Fabric 26.3: `hit-frenzy-1.0.0.jar` y Fabric API para 26.3.
En un servidor, el mod va en el servidor (los jugadores no necesitan instalarlo).

## Configuracion
La primera vez que abres el juego se crea `config/hitfrenzy.properties`.
Ahi puedes cambiar el idioma (`language=es`), cuanto dura el combo, cuantos golpes
hacen falta para cada cartel, colores, mensajes, sonidos y mas. Reinicia el juego tras cambiarlo.

## Licencia
MIT (ver LICENSE).
