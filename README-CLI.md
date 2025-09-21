# iTunes Backup Decryptor - Command Line Tool

A command-line tool to decrypt and extract all files from encrypted iTunes/iPhone backups.

## Features

- Decrypt all files from encrypted iPhone backups
- Extract files maintaining original directory structure (domain/relativePath)
- Support for both encrypted and unencrypted backups
- Progress reporting during extraction
- Resume capability (skip already extracted files)
- Verbose logging option
- Cross-platform support (Windows, macOS, Linux)

## Requirements

- Java 18 or higher
- iTunes backup files (Manifest.plist, Manifest.db, and backup data)

## Building

### JAR with Dependencies
```bash
mvn clean package
```
This creates `target/itunes-backup-explorer-1.7-SNAPSHOT-jar-with-dependencies.jar`

### Native Binary (Recommended)
```bash
# Install GraalVM first
sdk install java 21.0.8-graal
sdk use java 21.0.8-graal

# Build native binary
mvn clean native:compile -Pnative -Dskip.installer=true

# Or use the build script
./build-native.sh
```
This creates a standalone native binary `target/itunes-backup-decryptor` (~62MB)

## Usage

### Basic Usage

**Native Binary (Recommended):**
```bash
./itunes-backup-decryptor \
  --backup /path/to/backup/directory \
  --output /path/to/output/directory \
  --password "your_device_passcode"
```

**JAR Version:**
```bash
java -jar itunes-backup-explorer-1.7-SNAPSHOT-jar-with-dependencies.jar \
  --backup /path/to/backup/directory \
  --output /path/to/output/directory \
  --password "your_device_passcode"
```

### Command Line Options

- `-b, --backup PATH` - Path to iTunes backup directory (required)
- `-o, --output PATH` - Output directory for decrypted files (required)
- `-p, --password PASSWORD` - Backup password (optional, will prompt if needed)
- `-v, --verbose` - Enable verbose output
- `-f, --force` - Overwrite existing files in output directory
- `-h, --help` - Show help message

### Examples

**Decrypt encrypted backup with verbose output:**
```bash
java -jar itunes-backup-decryptor.jar -b ~/Library/Application\ Support/MobileSync/Backup/00008030-001E4C6A3A80802E -o ./extracted_backup -p "1234" -v
```

**Decrypt unencrypted backup:**
```bash
java -jar itunes-backup-decryptor.jar -b /path/to/backup -o ./output
```

**Resume interrupted extraction:**
```bash
java -jar itunes-backup-decryptor.jar -b /path/to/backup -o ./output -p "1234"
# Will skip already extracted files automatically
```

## Output Structure

The tool preserves the original iTunes backup directory structure with decrypted files:

```
output/
├── Manifest.db          # Database containing file mappings (copied/decrypted)
├── Manifest.plist       # Backup metadata (copied)
├── Info.plist           # Device information (copied)
├── ab/
│   ├── ab123456789...   # Decrypted files (original filename preserved)
│   └── ab987654321...   # Each file decrypted in place
├── cd/
│   ├── cd123456789...   # Same structure as original backup
│   └── cd987654321...   # but with decrypted content
├── ef/
│   └── ef123456789...
└── ...                  # All subdirectories (00-ff) as needed
```

**Benefits:**
- **Exact structure preservation** - tools expecting this format will work
- **File relationship maintained** - Manifest.db still references correct paths
- **Easy analysis** - same directory layout as original backup
- **Tool compatibility** - other backup analysis tools can work with this structure

## Finding iTunes Backups

### macOS
```bash
~/Library/Application Support/MobileSync/Backup/
```

### Windows
```bash
%APPDATA%\Apple Computer\MobileSync\Backup\
```

### Linux
Depends on where iTunes/3uTools stores backups.

## Troubleshooting

**"Invalid password provided"**
- Ensure you're using the device passcode, not the iTunes backup password
- Try the device unlock passcode

**"Backup directory does not exist"**
- Verify the path points to a directory containing `Manifest.plist` and `Manifest.db`

**"Output directory is not empty"**
- Use `--force` flag to overwrite existing files
- Or choose an empty output directory

**Java module errors**
- Ensure you're using Java 18 or higher
- Use the full jar-with-dependencies version

## Performance

- Processing speed depends on backup size and encryption
- Typical performance: 50-200 files/second
- Large backups (50GB+) may take 30+ minutes
- SSD storage recommended for better I/O performance

## Security Notes

- Passwords are handled securely and not logged
- Temporary decrypted database files are cleaned up automatically
- Original backup files remain unchanged

## License

Same as original iTunes Backup Explorer project.
