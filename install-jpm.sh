#!/usr/bin/env bash
#
# JPM (Java Package Manager) installer
# Usage: curl -sSL https://raw.githubusercontent.com/your-org/jpm/main/install-jpm.sh | bash -s -- [VERSION]
# Example: bash install-jpm.sh v1.0.0
#

set -euo pipefail

VERSION="${1:-latest}"
REPO="${2:-israelglory/jpm}"
INSTALL_DIR="${INSTALL_DIR:-/usr/local/lib/jpm}"
BIN_DIR="${BIN_DIR:-/usr/local/bin}"
USING_SUDO=false

# Resolve "latest" to actual version via GitHub API
if [ "$VERSION" = "latest" ]; then
    VERSION=$(curl -sSL "https://api.github.com/repos/$REPO/releases/latest" | grep -o '"tag_name": "[^"]*' | cut -d'"' -f4)
    if [ -z "$VERSION" ]; then
        echo "✘ Error: Could not resolve latest version from GitHub"
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
        echo "✘ Error: Unsupported platform ${OS}:${ARCH}"
        exit 1
        ;;
esac

BASE_URL="https://github.com/$REPO/releases/download/$RELEASE_TAG"
JAR_URL="$BASE_URL/$JAR_NAME"

# Create temporary directory for downloads
TMPDIR=$(mktemp -d)
trap 'rm -rf "$TMPDIR"' EXIT

# Download JAR to temp first
echo "Downloading $JAR_NAME..."
if ! curl -sSL -o "$TMPDIR/$JAR_NAME" "$JAR_URL"; then
    echo "✘ Error: Failed to download JPM from $JAR_URL"
    exit 1
fi

# Verify checksum if available
SHA256_URL="$BASE_URL/$JAR_NAME.sha256"
if curl -sSL -o "$TMPDIR/jpm.sha256" "$SHA256_URL" 2>/dev/null; then
    echo "Verifying checksum..."
    # Extract just the hash (first field) from the checksum file
    EXPECTED_HASH=$(awk '{print $1}' "$TMPDIR/jpm.sha256" | head -1)
    if [ -n "$EXPECTED_HASH" ]; then
        # Calculate actual hash
        ACTUAL_HASH=$(sha256sum "$TMPDIR/$JAR_NAME" | awk '{print $1}')
        if [ "$EXPECTED_HASH" = "$ACTUAL_HASH" ]; then
            echo "✓ Checksum verified"
        else
            echo "✘ Error: Checksum mismatch"
            echo "  Expected: $EXPECTED_HASH"
            echo "  Actual:   $ACTUAL_HASH"
            exit 1
        fi
    else
        echo "✘ Error: Could not parse checksum file"
        exit 1
    fi
fi

# Determine if we need sudo for system directories
if [ "$INSTALL_DIR" = "/usr/local/lib/jpm" ] || [ "$INSTALL_DIR" = "/usr/local/lib/jpm/" ]; then
    if [ ! -w /usr/local/lib ] 2>/dev/null; then
        USING_SUDO=true
    fi
fi

# Create install directory with appropriate permissions
if [ ! -d "$INSTALL_DIR" ]; then
    echo "Creating $INSTALL_DIR..."
    if [ "$USING_SUDO" = true ]; then
        if ! sudo mkdir -p "$INSTALL_DIR"; then
            echo "✘ Error: Failed to create $INSTALL_DIR (permission denied)"
            echo "Try installing to a user directory:"
            echo "  INSTALL_DIR=~/.local/lib/jpm bash install-jpm.sh $RELEASE_TAG"
            exit 1
        fi
    else
        mkdir -p "$INSTALL_DIR"
    fi
fi

# Move JAR to final location
echo "Installing JAR to $INSTALL_DIR..."
if [ "$USING_SUDO" = true ]; then
    sudo mv "$TMPDIR/$JAR_NAME" "$INSTALL_DIR/$JAR_NAME"
    if [ -f "$TMPDIR/jpm.sha256" ]; then
        sudo mv "$TMPDIR/jpm.sha256" "$INSTALL_DIR/$JAR_NAME.sha256"
    fi
else
    mv "$TMPDIR/$JAR_NAME" "$INSTALL_DIR/$JAR_NAME"
    if [ -f "$TMPDIR/jpm.sha256" ]; then
        mv "$TMPDIR/jpm.sha256" "$INSTALL_DIR/$JAR_NAME.sha256"
    fi
fi

# Determine if we need sudo for bin directory
if [ "$BIN_DIR" = "/usr/local/bin" ]; then
    if [ ! -w "$BIN_DIR" ] 2>/dev/null && [ "$USING_SUDO" = false ]; then
        USING_SUDO=true
    fi
fi

# Create bin directory if needed
if [ ! -d "$BIN_DIR" ]; then
    echo "Creating $BIN_DIR..."
    if [ "$USING_SUDO" = true ]; then
        sudo mkdir -p "$BIN_DIR"
    else
        mkdir -p "$BIN_DIR"
    fi
fi

# Create wrapper script
echo "Installing launcher to $BIN_DIR/jpm..."
cat > "$TMPDIR/jpm-launcher" <<BINSCRIPT
#!/usr/bin/env bash
exec java -jar "$INSTALL_DIR/$JAR_NAME" "\$@"
BINSCRIPT
chmod +x "$TMPDIR/jpm-launcher"

# Move launcher to bin directory
if [ "$USING_SUDO" = true ]; then
    sudo mv "$TMPDIR/jpm-launcher" "$BIN_DIR/jpm"
else
    mv "$TMPDIR/jpm-launcher" "$BIN_DIR/jpm"
fi


# Verify installation
echo ""
if command -v jpm > /dev/null 2>&1; then
    echo "✓ JPM ${VERSION} installed successfully!"
    echo "  Binary: $BIN_DIR/jpm"
    echo "  JAR: $INSTALL_DIR/$JAR_NAME"
    echo ""
    echo "Try it: jpm --version"
    jpm --version
else
    echo "✓ JPM ${VERSION} installed to $INSTALL_DIR"
    echo "  Binary: $BIN_DIR/jpm"
    echo ""
    echo "Make sure $BIN_DIR is in your PATH, then:"
    echo "  jpm --version"
fi
