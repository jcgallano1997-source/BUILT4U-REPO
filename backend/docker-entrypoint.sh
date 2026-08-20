#!/bin/sh
# Built4U POS — container entrypoint.
#
# The Oracle wallet is half binary (cwallet.sso, *.jks, *.p12), but several
# hosts only accept secrets as pasted text. So the wallet travels as one
# base64'd tarball and is unpacked here, at start, into a private directory.
#
# Set WALLET_B64_FILE (a mounted secret file) or WALLET_B64 (an env var). With
# neither, TNS_ADMIN is left exactly as given — which is how a local run with a
# mounted wallet directory keeps working.
set -e

WALLET_SRC="${WALLET_B64_FILE:-/etc/secrets/wallet.tgz.b64}"
WALLET_DIR=/tmp/wallet

if [ -n "$WALLET_B64" ]; then
  mkdir -p "$WALLET_DIR" && chmod 700 "$WALLET_DIR"
  printf '%s' "$WALLET_B64" | base64 -d | tar xz -C "$WALLET_DIR"
  export TNS_ADMIN="$WALLET_DIR"
  echo "entrypoint: wallet unpacked from WALLET_B64 -> $TNS_ADMIN"
elif [ -f "$WALLET_SRC" ]; then
  mkdir -p "$WALLET_DIR" && chmod 700 "$WALLET_DIR"
  base64 -d < "$WALLET_SRC" | tar xz -C "$WALLET_DIR"
  export TNS_ADMIN="$WALLET_DIR"
  echo "entrypoint: wallet unpacked from $WALLET_SRC -> $TNS_ADMIN"
else
  echo "entrypoint: no base64 wallet found; using TNS_ADMIN=${TNS_ADMIN:-<unset>}"
fi

exec java $JAVA_OPTS -jar /app/app.jar
