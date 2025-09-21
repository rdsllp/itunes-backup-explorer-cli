package hearsay.idevice_decryption;

import hearsay.idevice_decryption.api.*;
import hearsay.idevice_decryption.util.DualLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.InvalidKeyException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Command-line tool to decrypt all files from an iTunes backup.
 *
 * Usage: java -jar itunes-backup-decryptor.jar -b /path/to/backup -o
 * /path/to/output [-p password] [-v]
 */
public class ITunesBackupDecryptor {
  private static final Logger logger = LoggerFactory.getLogger(ITunesBackupDecryptor.class);

  private final boolean verbose;
  private final DualLogger dualLogger;
  private final AtomicInteger processedFiles = new AtomicInteger(0);
  private final AtomicInteger skippedFiles = new AtomicInteger(0);
  private final AtomicInteger errorFiles = new AtomicInteger(0);
  private final AtomicLong totalBytes = new AtomicLong(0);

  public ITunesBackupDecryptor(boolean verbose, String logFilePath) throws IOException {
    this.verbose = verbose;
    this.dualLogger = new DualLogger(logFilePath);
  }

  public static void main(String[] args) {
    try {
      Arguments arguments = parseArguments(args);

      if (arguments.help) {
        printHelp();
        System.exit(0);
      }

      // Validate arguments
      if (arguments.backupPath == null) {
        System.err.println("Error: Backup path is required.");
        printHelp();
        System.exit(1);
      }

      if (arguments.replace && arguments.outputPath != null) {
        System.err.println("Error: Cannot use both --output and --replace options together.");
        printHelp();
        System.exit(1);
      }

      if (!arguments.replace && arguments.outputPath == null) {
        System.err.println("Error: Either --output or --replace option is required.");
        printHelp();
        System.exit(1);
      }

      ITunesBackupDecryptor decryptor = null;
      try {
        decryptor = new ITunesBackupDecryptor(arguments.verbose, arguments.logFilePath);
        decryptor.decryptBackup(arguments.backupPath, arguments.outputPath, arguments.password, arguments.force,
            arguments.replace);
      } finally {
        // Close the log file if decryptor was created
        if (decryptor != null) {
          decryptor.dualLogger.close();
        }
      }

    } catch (IllegalArgumentException e) {
      System.err.println("Error: " + e.getMessage());
      printHelp();
      System.exit(1);
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static class Arguments {
    String backupPath;
    String outputPath;
    String password;
    String logFilePath;
    boolean verbose = false;
    boolean force = false;
    boolean help = false;
    boolean replace = false;
  }

  private static Arguments parseArguments(String[] args) {
    Arguments arguments = new Arguments();

    for (int i = 0; i < args.length; i++) {
      String arg = args[i];

      switch (arg) {
        case "-b":
        case "--backup":
          if (i + 1 >= args.length)
            throw new IllegalArgumentException("Missing value for " + arg);
          arguments.backupPath = args[++i];
          break;
        case "-o":
        case "--output":
          if (i + 1 >= args.length)
            throw new IllegalArgumentException("Missing value for " + arg);
          arguments.outputPath = args[++i];
          break;
        case "-p":
        case "--password":
          if (i + 1 >= args.length)
            throw new IllegalArgumentException("Missing value for " + arg);
          arguments.password = args[++i];
          break;
        case "-l":
        case "--log":
          if (i + 1 >= args.length)
            throw new IllegalArgumentException("Missing value for " + arg);
          arguments.logFilePath = args[++i];
          break;
        case "-v":
        case "--verbose":
          arguments.verbose = true;
          break;
        case "-f":
        case "--force":
          arguments.force = true;
          break;
        case "-r":
        case "--replace":
          arguments.replace = true;
          break;
        case "-h":
        case "--help":
          arguments.help = true;
          break;
        default:
          throw new IllegalArgumentException("Unknown argument: " + arg);
      }
    }

    return arguments;
  }

  private static void printHelp() {
    System.out.println("iTunes Backup Decryptor - Command Line Tool");
    System.out.println("Decrypt all files from an iTunes backup");
    System.out.println();
    System.out.println("Usage: java -jar itunes-backup-decryptor.jar [OPTIONS]");
    System.out.println();
    System.out.println("Options:");
    System.out.println("  -b, --backup PATH      Path to iTunes backup directory (required)");
    System.out.println("  -o, --output PATH      Output directory for decrypted files");
    System.out.println("  -r, --replace          Replace encrypted files in-place with decrypted versions");
    System.out.println("  -p, --password PASS    Backup password (will prompt if not provided)");
    System.out.println("  -l, --log PATH         Write logs to specified file (overwrites if exists)");
    System.out.println("  -v, --verbose          Enable verbose output");
    System.out.println(
        "  -f, --force            Overwrite existing files (in output mode) or skip confirmation (in replace mode)");
    System.out.println("  -h, --help             Show this help message");
    System.out.println();
    System.out.println("Note: Either --output or --replace is required, but not both.");
    System.out.println();
    System.out.println("Examples:");
    System.out.println("  # Extract to separate directory (preserves original backup)");
    System.out.println("  java -jar itunes-backup-decryptor.jar -b /path/to/backup -o /path/to/output");
    System.out
        .println("  java -jar itunes-backup-decryptor.jar -b /path/to/backup -o /path/to/output -p mypassword -v");
    System.out.println();
    System.out.println("  # Replace encrypted files in-place (modifies original backup)");
    System.out.println("  java -jar itunes-backup-decryptor.jar -b /path/to/backup -r");
    System.out.println("  java -jar itunes-backup-decryptor.jar -b /path/to/backup -r -p mypassword -v");
    System.out.println();
    System.out.println("Output mode preserves the original backup directory structure with decrypted files:");
    System.out.println("  output/");
    System.out.println("    ├── Manifest.db       (database with file mappings)");
    System.out.println("    ├── Manifest.plist    (backup metadata)");
    System.out.println("    ├── Info.plist        (device information)");
    System.out.println("    ├── ab/");
    System.out.println("    │   ├── ab123456...    (decrypted files)");
    System.out.println("    ├── cd/");
    System.out.println("    │   └── cd789abc...    (decrypted files)");
    System.out.println("    └── ...");
    System.out.println();
    System.out.println("Replace mode decrypts files directly in the backup directory, preserving the exact structure.");
  }

  public void decryptBackup(String backupPath, String outputPath, String password, boolean force, boolean replace)
      throws Exception {

    log("Starting iTunes backup decryption...");
    log("Backup path: " + backupPath);
    if (replace) {
      log("Mode: In-place replacement");
    } else {
      log("Mode: Extract to output directory: " + outputPath);
    }

    File backupDir = new File(backupPath);
    if (!backupDir.exists() || !backupDir.isDirectory()) {
      throw new IllegalArgumentException("Backup directory does not exist: " + backupPath);
    }

    Path outputDir = null;
    if (!replace) {
      outputDir = Paths.get(outputPath);
      if (!Files.exists(outputDir)) {
        Files.createDirectories(outputDir);
        log("Created output directory: " + outputPath);
      } else if (!force) {
        File[] files = outputDir.toFile().listFiles();
        if (files != null && files.length > 0) {
          throw new IllegalArgumentException("Output directory is not empty. Use --force to overwrite existing files.");
        }
      }
    } else {
      log("Replace mode: Files will be decrypted in-place in the backup directory");
      if (!force) {
        log("WARNING: This will modify the original backup files. Use --force to skip this warning in the future.");
        System.out.print("Continue? (y/N): ");
        String response = System.console() != null ? System.console().readLine() : "n";
        if (!response.toLowerCase().startsWith("y")) {
          log("Operation cancelled by user.");
          return;
        }
      }
    }

    log("Loading iTunes backup from: " + backupPath);
    ITunesBackup backup = new ITunesBackup(backupDir);

    log("Backup Info:");
    log("  Device: " + backup.manifest.deviceName);
    log("  Product: " + backup.manifest.productType + " (" + backup.manifest.productVersion + ")");
    log("  Date: " + backup.manifest.date);
    log("  Encrypted: " + backup.manifest.encrypted);

    // Handle encryption
    if (backup.manifest.encrypted) {
      if (backup.isLocked()) {
        if (password == null) {
          System.out.print("Enter backup password: ");
          password = System.console().readLine();
        }

        log("Unlocking encrypted backup...");
        try {
          backup.manifest.getKeyBag().get().unlock(password);
          backup.decryptDatabase();
          log("Backup unlocked successfully");
        } catch (InvalidKeyException e) {
          throw new IllegalArgumentException("Invalid password provided");
        }
      }
    }

    // Connect to backup database
    backup.connectToDatabase();

    log("Querying all files from backup database...");
    List<BackupFile> allFiles = backup.searchFiles("%", "%");

    log("Found " + allFiles.size() + " files to process");
    log("Starting decryption process...");

    if (replace) {
      log("Replace mode: Decrypting files in-place");
    } else {
      log("Output structure will preserve original backup format with decrypted files");
    }

    long startTime = System.currentTimeMillis();

    if (!replace) {
      // Copy manifest files to preserve backup structure (only in output mode)
      copyManifestFiles(backup, outputDir);
    }

    // Process all files
    for (BackupFile file : allFiles) {
      if (replace) {
        processFileInPlace(file, force);
      } else {
        processFile(file, outputDir, force);
      }

      // Progress reporting every 100 files
      if (processedFiles.get() % 100 == 0) {
        reportProgress(allFiles.size());
      }
    }

    // Final cleanup
    backup.cleanUp();

    // Final report
    long duration = System.currentTimeMillis() - startTime;
    log("\n=== DECRYPTION COMPLETE ===");
    log("Total files: " + allFiles.size());
    log("Successfully processed: " + processedFiles.get());
    log("Skipped (already exist or not encrypted): " + skippedFiles.get());
    log("Errors: " + errorFiles.get());
    log("Total data processed: " + formatBytes(totalBytes.get()));
    log("Time taken: " + formatDuration(duration));

    if (replace) {
      log("Mode: In-place replacement in backup directory");
      log("Location: " + backupPath);
    } else {
      log("Mode: Extract to separate directory");
      log("Output directory: " + outputPath);
      log("Structure: Preserved original backup format with decrypted files");
    }

    if (errorFiles.get() > 0) {
      log("Warning: " + errorFiles.get() + " files had errors during processing");
    }
  }

  private void processFile(BackupFile file, Path outputDir, boolean force) {
    try {
      // Skip directories for now (they'll be created as needed)
      if (file.getFileType() == BackupFile.FileType.DIRECTORY) {
        return;
      }

      // Create output path preserving backup structure: output/ab/ab123456789...
      String fileIdPrefix = file.fileID.substring(0, 2);
      Path filePath = outputDir.resolve(fileIdPrefix).resolve(file.fileID);

      // Skip if file already exists and not forcing
      if (Files.exists(filePath) && !force) {
        skippedFiles.incrementAndGet();
        logVerbose("Skipped (exists): " + fileIdPrefix + "/" + file.fileID + " (" + file.domain + "/"
            + file.relativePath + ")");
        return;
      }

      // Create parent directories (e.g., ab/, cd/, etc.)
      Files.createDirectories(filePath.getParent());

      // Extract the file (decrypt if needed)
      file.extract(filePath.toFile());

      processedFiles.incrementAndGet();
      totalBytes.addAndGet(file.getSize());

      if (file.getSize() == 0) {
        logVerbose("Extracted (0-byte file): " + fileIdPrefix + "/" + file.fileID + " (" + file.domain + "/"
            + file.relativePath +
            (file.isEncrypted() ? ", was encrypted" : "") + ")");
      } else {
        logVerbose("Extracted: " + fileIdPrefix + "/" + file.fileID + " (" + file.domain + "/" + file.relativePath +
            ", " + formatBytes(file.getSize()) +
            (file.isEncrypted() ? ", encrypted" : "") + ")");
      }

    } catch (Exception e) {
      errorFiles.incrementAndGet();
      String errorMsg = "Error processing " + file.fileID + " (" + file.domain + "/" + file.relativePath + "): "
          + e.getMessage();
      dualLogger.error(errorMsg);
      if (verbose) {
        logger.error("Full error details:", e);
      }
    }
  }

  private void processFileInPlace(BackupFile file, boolean force) {
    try {
      // Skip directories
      if (file.getFileType() == BackupFile.FileType.DIRECTORY) {
        return;
      }

      // Skip files that are not encrypted (no need to decrypt)
      if (!file.isEncrypted()) {
        skippedFiles.incrementAndGet();
        logVerbose("Skipped (not encrypted): " + file.fileID + " (" + file.domain + "/" + file.relativePath + ")");
        return;
      }

      // Get the original file path
      File originalFile = file.getContentFile();
      if (originalFile == null || !originalFile.exists()) {
        errorFiles.incrementAndGet();
        dualLogger
            .error("Content file not found for " + file.fileID + " (" + file.domain + "/" + file.relativePath + ")");
        return;
      }

      // Create temporary file in the same directory for atomic replacement
      File tempFile = new File(originalFile.getParent(), originalFile.getName() + ".tmp." + System.currentTimeMillis());

      try {
        // Check if parent directory is writable
        File parentDir = originalFile.getParentFile();
        if (!parentDir.canWrite()) {
          throw new IOException("Cannot write to directory: " + parentDir.getAbsolutePath() + " (permission denied)");
        }

        // Decrypt to temporary file
        logVerbose("Decrypting to temp file: " + tempFile.getAbsolutePath() +
            " (original: " + originalFile.getAbsolutePath() + ", size: " + file.getSize() + " bytes)");

        file.extract(tempFile);

        // Verify the temporary file was created and has content
        if (!tempFile.exists()) {
          throw new IOException("Temporary decrypted file was not created at: " + tempFile.getAbsolutePath());
        }

        if (tempFile.length() == 0 && file.getSize() > 0) {
          throw new IOException("Temporary decrypted file is empty: " + tempFile.getAbsolutePath() +
              " (original size: " + file.getSize() + " bytes, encrypted: " + file.isEncrypted() + ")");
        }

        // Atomic replacement: rename temp file to original file name
        if (!tempFile.renameTo(originalFile)) {
          // If rename fails, try copy and delete (less atomic but still works)
          Files.copy(tempFile.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
          if (!tempFile.delete()) {
            log("Warning: Could not delete temporary file: " + tempFile.getAbsolutePath());
          }
        }

        processedFiles.incrementAndGet();
        totalBytes.addAndGet(file.getSize());

        if (file.getSize() == 0) {
          logVerbose("Replaced in-place (0-byte file): " + file.fileID + " (" + file.domain + "/" + file.relativePath
              + ", was encrypted)");
        } else {
          logVerbose("Replaced in-place: " + file.fileID + " (" + file.domain + "/" + file.relativePath +
              ", " + formatBytes(file.getSize()) + ", was encrypted)");
        }

      } catch (Exception e) {
        // Clean up temp file on error
        if (tempFile.exists()) {
          if (!tempFile.delete()) {
            log("Warning: Could not delete temporary file after error: " + tempFile.getAbsolutePath());
          }
        }

        // If the error was about temp file creation/emptiness, try alternative approach
        if (e.getMessage() != null && e.getMessage().contains("Temporary decrypted file")) {
          logVerbose("Attempting alternative approach: extract to system temp directory first");
          try {
            // Create temp file in system temp directory instead
            File systemTempFile = File.createTempFile("backup_decrypt_", ".tmp");
            try {
              file.extract(systemTempFile);

              if (systemTempFile.exists() && (systemTempFile.length() > 0 || file.getSize() == 0)) {
                // Successfully extracted to system temp, now move to original location
                Files.copy(systemTempFile.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                processedFiles.incrementAndGet();
                totalBytes.addAndGet(file.getSize());

                if (file.getSize() == 0) {
                  logVerbose("Replaced in-place (via system temp, 0-byte file): " + file.fileID + " (" + file.domain
                      + "/" + file.relativePath + ", was encrypted)");
                } else {
                  logVerbose("Replaced in-place (via system temp): " + file.fileID + " (" + file.domain + "/"
                      + file.relativePath +
                      ", " + formatBytes(file.getSize()) + ", was encrypted)");
                }

                return; // Success, exit method
              }
            } finally {
              if (systemTempFile.exists()) {
                systemTempFile.delete();
              }
            }
          } catch (Exception altException) {
            logVerbose("Alternative approach also failed: " + altException.getMessage());
          }
        }

        throw e; // Re-throw original exception if alternative approach didn't work
      }

    } catch (Exception e) {
      errorFiles.incrementAndGet();
      String errorMsg = "Error processing in-place " + file.fileID + " (" + file.domain + "/" + file.relativePath
          + "): "
          + e.getMessage();
      dualLogger.error(errorMsg);
      if (verbose) {
        logger.error("Full error details:", e);
      }
    }
  }

  private void copyManifestFiles(ITunesBackup backup, Path outputDir) {
    try {
      // Copy Manifest.plist
      Path manifestPlistSrc = backup.manifestPListFile.toPath();
      Path manifestPlistDest = outputDir.resolve("Manifest.plist");
      Files.copy(manifestPlistSrc, manifestPlistDest, StandardCopyOption.REPLACE_EXISTING);
      log("Copied: Manifest.plist");

      // Copy Manifest.db (use decrypted version if available, otherwise original)
      File manifestDbSrc = backup.decryptedDatabaseFile != null ? backup.decryptedDatabaseFile : backup.manifestDBFile;
      Path manifestDbDest = outputDir.resolve("Manifest.db");
      Files.copy(manifestDbSrc.toPath(), manifestDbDest, StandardCopyOption.REPLACE_EXISTING);
      log("Copied: Manifest.db" + (backup.decryptedDatabaseFile != null ? " (decrypted)" : ""));

      // Copy Info.plist if it exists
      if (backup.backupInfoFile.exists()) {
        Path infoPlistSrc = backup.backupInfoFile.toPath();
        Path infoPlistDest = outputDir.resolve("Info.plist");
        Files.copy(infoPlistSrc, infoPlistDest, StandardCopyOption.REPLACE_EXISTING);
        log("Copied: Info.plist");
      }

    } catch (IOException e) {
      log("Warning: Failed to copy manifest files: " + e.getMessage());
    }
  }

  private void reportProgress(int totalFiles) {
    int processed = processedFiles.get();
    int skipped = skippedFiles.get();
    int errors = errorFiles.get();
    int completed = processed + skipped + errors;

    double percentage = (double) completed / totalFiles * 100;

    log(String.format("Progress: %d/%d (%.1f%%) - Processed: %d, Skipped: %d, Errors: %d",
        completed, totalFiles, percentage, processed, skipped, errors));
  }

  private void log(String message) {
    dualLogger.info(message);
  }

  private void logVerbose(String message) {
    dualLogger.verbose(message, verbose);
  }

  private static String formatBytes(long bytes) {
    if (bytes < 1024)
      return bytes + " B";
    if (bytes < 1024 * 1024)
      return String.format("%.1f KB", bytes / 1024.0);
    if (bytes < 1024 * 1024 * 1024)
      return String.format("%.1f MB", bytes / (1024.0 * 1024));
    return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
  }

  private static String formatDuration(long millis) {
    long seconds = millis / 1000;
    long minutes = seconds / 60;
    seconds = seconds % 60;

    if (minutes > 0) {
      return String.format("%dm %ds", minutes, seconds);
    } else {
      return String.format("%ds", seconds);
    }
  }
}
