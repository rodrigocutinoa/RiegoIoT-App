#!/usr/bin/env bash
# Deja listos los dos "teléfonos" de la demostración (Hito 8):
#   emulator-5554  Medium_Phone_API_37.0  -> Panel
#   emulator-5556  Nodo_Riego             -> Nodo de riego
# Crea el AVD Nodo_Riego si falta (copia la configuración del primero), arranca ambos
# SIN -read-only (así se conservan las cuentas y el historial), instala la app y concede
# los permisos de notificaciones y de red local.
#
# Uso: scripts/demo-emuladores.sh            (desde la carpeta RiegoIoT-App)
#      SIN_INSTALAR=1 scripts/demo-emuladores.sh
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"
EMU="$SDK/emulator/emulator"
AVD_DIR="$HOME/.android/avd"
BASE=Medium_Phone_API_37.0
NODO=Nodo_Riego
APP=cl.inacap.riegoiot
cd "$(dirname "$0")/.."

# 1. AVD del nodo (una copia limpia de la configuración del emulador base)
if ! "$EMU" -list-avds | grep -qx "$NODO"; then
  echo "== Creando el AVD $NODO"
  base_dir=$(sed -n 's/^path=//p' "$AVD_DIR/$BASE.ini")
  mkdir -p "$AVD_DIR/$NODO.avd"
  cp "$base_dir/config.ini" "$AVD_DIR/$NODO.avd/"
  sed -i '' "s/^AvdId=.*/AvdId=$NODO/; s/^avd.ini.displayname=.*/avd.ini.displayname=Nodo Riego/" "$AVD_DIR/$NODO.avd/config.ini"
  printf 'avd.ini.encoding=UTF-8\npath=%s\npath.rel=avd/%s.avd\ntarget=android-37.0\n' \
    "$AVD_DIR/$NODO.avd" "$NODO" > "$AVD_DIR/$NODO.ini"
fi

# 2. Arrancar los emuladores que falten
arrancar() { # <avd> <puerto>
  if "$ADB" devices | grep -q "emulator-$2"; then echo "== emulator-$2 ya está encendido"; return; fi
  echo "== Arrancando $1 en emulator-$2"
  nohup "$EMU" -avd "$1" -port "$2" -no-boot-anim >/dev/null 2>&1 &
}
arrancar "$BASE" 5554
sleep 15
arrancar "$NODO" 5556
for s in emulator-5554 emulator-5556; do
  "$ADB" -s "$s" wait-for-device
  until [ "$("$ADB" -s "$s" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do sleep 3; done
  echo "== $s listo"
done

# 3. Instalar la app y conceder los permisos
if [ "${SIN_INSTALAR:-0}" != 1 ]; then
  export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
  ./gradlew -q assembleDebug
  for s in emulator-5554 emulator-5556; do
    "$ADB" -s "$s" install -r -g app/build/outputs/apk/debug/app-debug.apk >/dev/null
    for p in POST_NOTIFICATIONS ACCESS_LOCAL_NETWORK; do
      "$ADB" -s "$s" shell pm grant "$APP" "android.permission.$p" 2>/dev/null || true
    done
    echo "== App instalada en $s"
  done
fi

echo
echo "Listo. Panel: emulator-5554 · Nodo: emulator-5556 · broker 10.0.2.2:1883 · código 123456."
echo "Falta levantar hertzbench/riego-broker.yaml (desde Claude Code o con hertzbench run)."
