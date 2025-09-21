# iDevice Decryption Tool

A command-line tool for decrypting iTunes/Finder backup files from iPhone and iPad devices. Supports both encrypted and non-encrypted backups from iOS 10.2 onwards.

## Features

- **Decrypt encrypted iTunes backups** using your backup password
- **Extract files** to a separate directory preserving backup structure
- **In-place decryption** to replace encrypted files directly in the backup directory
- **File logging** with timestamped entries for automation and debugging
- **Cross-platform** support for macOS, Windows, and Linux
- **Native executables** for optimal performance

## Usage

### Extract to separate directory (preserves original backup):
```bash
java -jar idevice-decryption.jar -b /path/to/backup -o /path/to/output
java -jar idevice-decryption.jar -b /path/to/backup -o /path/to/output -p password -v
java -jar idevice-decryption.jar -b /path/to/backup -o /path/to/output -l /path/to/logfile.log
```

### Replace encrypted files in-place (modifies original backup):
```bash
java -jar idevice-decryption.jar -b /path/to/backup -r
java -jar idevice-decryption.jar -b /path/to/backup -r -p password -v
java -jar idevice-decryption.jar -b /path/to/backup -r -l /path/to/logfile.log -v
```

### File logging (for automation and debugging):
```bash
# Console + file logging
java -jar idevice-decryption.jar -b /path/to/backup -o /path/to/output -l /path/to/logfile.log

# Verbose console + complete file logging
java -jar idevice-decryption.jar -b /path/to/backup -r -l /path/to/logfile.log -v

# Silent console with file logging only (for automation)
java -jar idevice-decryption.jar -b /path/to/backup -o /path/to/output -l /path/to/logfile.log 2>/dev/null
```

### Command Options:
- `-b, --backup PATH` - Path to iTunes backup directory (required)
- `-o, --output PATH` - Output directory for decrypted files
- `-r, --replace` - Replace encrypted files in-place with decrypted versions
- `-p, --password PASS` - Backup password (will prompt if not provided)
- `-l, --log PATH` - Write logs to specified file (overwrites if exists)
- `-v, --verbose` - Enable verbose output
- `-f, --force` - Overwrite existing files or skip confirmation
- `-h, --help` - Show help message

**Note:** Either `--output` or `--replace` is required, but not both.

### Log File Format

When using the `-l/--log` option, log entries are written with timestamps in the following format:

```
[2025-09-21 14:13:11] INFO: Starting iTunes backup decryption...
[2025-09-21 14:13:11] INFO: Backup path: /path/to/backup
[2025-09-21 14:13:11] INFO: Mode: Extract to output directory
[2025-09-21 14:13:12] VERBOSE: Decrypting to temp file: /tmp/abc123.tmp
[2025-09-21 14:13:12] ERROR: Content file not found for file123
```

**Log Levels:**
- `INFO` - General operation messages (always logged)
- `VERBOSE` - Detailed progress information (always logged to file, console only with `-v`)
- `ERROR` - Error messages and warnings

### Node.js Integration

Perfect for automation with Node.js `spawn()`:

```javascript
const { spawn } = require('child_process');

const process = spawn('java', [
  '-jar', 'idevice-decryption.jar',
  '-b', '/path/to/backup',
  '-r',
  '-l', '/path/to/logfile.log',
  '-p', 'backup_password'
]);

// Real-time console output
process.stdout.on('data', (data) => {
  console.log(`stdout: ${data}`);
});

// Complete logs available in /path/to/logfile.log
```

## Building

### Prerequisites
- **Java 21+** (JDK)
- **Maven 3.6+**
- **GraalVM** (for native executables)

### Build JAR with Dependencies
```bash
mvn clean package
```
The JAR file will be available at `target/idevice-decryption-1.7-SNAPSHOT.jar`

### Build Native Executables

#### macOS
```bash
# Install GraalVM (if not already installed)
sdk install java 21.0.1-graal  # Using SDKMAN
# OR download from https://github.com/graalvm/graalvm-ce-builds/releases

# Build native executable
mvn clean package -Pnative

# Build macOS installer (.dmg)
mvn clean package  # Builds app image and DMG installer
```

#### Windows
```bash
# Install GraalVM Native Image
# Download GraalVM from https://github.com/graalvm/graalvm-ce-builds/releases
# Add to PATH and install native-image component

# Build native executable
mvn clean package -Pnative

# Build Windows installer (.msi)
mvn clean package  # Builds app image and MSI installer
```

#### Linux
```bash
# Install GraalVM
# Download from https://github.com/graalvm/graalvm-ce-builds/releases
# OR use package manager

# Build native executable
mvn clean package -Pnative

# Build Linux installer (.deb)
mvn clean package  # Builds app image and DEB package
```

### Build Output Locations
- **Native executable**: `target/itunes-backup-decryptor`
- **App image**: `target/app-image/`
- **Installers**: `target/installer/`

### Platform-Specific Notes

#### macOS
- Use `packaging/package-macos.sh` for custom builds
- Supports both Intel (x64) and Apple Silicon (arm64)
- May require code signing for distribution

#### Windows
- Use `packaging/package-win.bat` for custom builds
- Requires Windows SDK for jpackage
- Creates MSI installer by default

#### Linux
- Use `packaging/package-linux.sh` for custom builds
- Creates DEB package for Debian/Ubuntu
- For other distributions, use the native executable directly

## Development

### Run directly with Maven:
```bash
mvn exec:exec
```

### Run with custom arguments:
```bash
mvn exec:exec -Dexec.args="-b /path/to/backup -o /path/to/output -v"
mvn exec:exec -Dexec.args="-b /path/to/backup -r -l /tmp/debug.log -v"
```

### Clean build:
```bash
mvn clean compile
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Credits

Based on the excellent work and research on iOS backup encryption by:
- [andrewdotn's StackOverflow answer](https://stackoverflow.com/a/13793043/8868841)
- iPhone Data Protection in Depth research
- iOS Hacker's Handbook
- Apple iOS Security documentation
