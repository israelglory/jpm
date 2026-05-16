#!/usr/bin/env bash
#
# JPM (Java Package Manager) installer
# Usage: curl -sSL https://raw.githubusercontent.com/your-org/jpm/main/install-jpm.sh | bash -s -- [VERSION]
# Example: bash install-jpm.sh v1.0.0
#

set -euo pipefail

VERSION="${1:-latest}"
REPO="your-org/jpm"
INSTALL_DIR="${INSTALL_DIR:-/usr/local/lib/jpm}"
BIN_DIR="${BIN_DIR:-/usr/local/bin}"

# Resolve "latest" to actual version via GitHub API
if [ "$VERSION" = "latest" ]; then
    VERSION=$(curl -sSL "https://api.github.com/repos/$REPO/releases/latest" | grep -o '"tag_name": "[^"]*' | cut -d'"' -f4)
    if [ -z "$VERSION" ]; then
        echo "Error: Could not resolve latest version from GitHub"
        exit 1
    fi
fi

# Normalize version (remove leading 'v' if present)
VERSION="${VERSION#v}"
RELEASE_TAG="v${VERSION}"

echo "Installing JPM ${VERSION}..."

# Detect platform
OS=$(uname -s)
ARCH=$(uname -m)

case "$OS:$ARCH" in
    Linux:x86_64|Darwin:x86_64|Darwin:arm64)
        # JAR is platform-independent; use same name
        JAR_NAME="jpm-${VERSION}.jar"
        ;;
    *)
        echo "Error: Unsupported platform ${OS}:${ARCH}"
        exit 1
        ;;
esac

BASE_URL="https://github.com/$REPO/releases/download/$RELEASE_TAG"
JAR_URL="$BASE_URL/$JAR_NAME"

# Create install directory
if [ ! -d "$INSTALL_DIR" ]; then
    echo "Creating $INSTALL_DIR..."
    mkdir -p "$INSTALL_DIR"
fi

# Download JAR
echo "Downloading $JAR_NAME..."
if ! curl -sSL -o "$INSTALL_DIR/$JAR_NAME" "$JAR_URL"; then
    echo "Error: Failed to download JPM from $JAR_URL"
    exit 1
fi

# Verify checksum if available
SHA256_URL="$BASE_URL/$JAR_NAME.sha256"
if curl -sSL -o /tmp/jpm.sha256 "$SHA256_URL" 2>/dev/null; then
    echo "Verifying checksum..."
    cd "$INSTALL_DIR"
    if ! sha256sum -c /tmp/jpm.sha256 > /dev/null 2>&1; then
        echo "Error: Checksum verification failed"
        rm -f "$INSTALL_DIR/$JAR_NAME"
        exit 1
    fi
fi

# Create wrapper script
echo "Creating wrapper script..."
cat > /tmp/jpm-wrapper <<'WRAPPER'
#!/usr/bin/env bash
INSTALL_DIR="$1"
JAR_NAME="$2"
exec java -jar "$INSTALL_DIR/$JAR_NAME" "$@"
WRAPPER

chmod +x /tmp/jpm-wrapper

# Install wrapper to bin directory
if [ ! -d "$BIN_DIR" ]; then
    mkdir -p "$BIN_DIR"
fi

cat > "$BIN_DIR/jpm" <<BINSCRIPT
#!/usr/bin/env bash
exec java -jar "$INSTALL_DIR/$JAR_NAME" "\$@"
BINSCRIPT
chmod +x "$BIN_DIR/jpm"

echo "✓ JPM ${VERSION} installed successfully!"
echo "  Binary: $BIN_DIR/jpm"
echo "  JAR: $INSTALL_DIR/$JAR_NAME"
echo ""
echo "You can now use: jpm add postgres"

