package hearsay.idevice_decryption.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for dual logging to both console and file
 */
public class DualLogger {
  private final PrintWriter fileWriter;
  private final boolean logToFile;
  private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public DualLogger(String logFilePath) throws IOException {
    if (logFilePath != null && !logFilePath.trim().isEmpty()) {
      // Validate parent directory exists
      File logFile = new File(logFilePath);
      File parentDir = logFile.getParentFile();
      if (parentDir != null && !parentDir.exists()) {
        throw new IOException("Log file parent directory does not exist: " + parentDir.getAbsolutePath());
      }

      this.fileWriter = new PrintWriter(new FileWriter(logFile, false)); // false = overwrite
      this.logToFile = true;
    } else {
      this.fileWriter = null;
      this.logToFile = false;
    }
  }

  public void info(String message) {
    // Always print to console
    System.out.println(message);

    // Print to file if logging is enabled
    if (logToFile && fileWriter != null) {
      try {
        fileWriter.println(String.format("[%s] INFO: %s", LocalDateTime.now().format(timeFormatter), message));
        fileWriter.flush();
      } catch (Exception e) {
        System.err.println("Warning: Failed to write to log file: " + e.getMessage());
      }
    }
  }

  public void verbose(String message, boolean isVerbose) {
    // Print to console only if verbose mode is enabled
    if (isVerbose) {
      System.out.println(message);
    }

    // Always print to file if logging is enabled (even non-verbose messages)
    if (logToFile && fileWriter != null) {
      try {
        fileWriter.println(String.format("[%s] VERBOSE: %s", LocalDateTime.now().format(timeFormatter), message));
        fileWriter.flush();
      } catch (Exception e) {
        System.err.println("Warning: Failed to write to log file: " + e.getMessage());
      }
    }
  }

  public void error(String message) {
    // Always print to console
    System.out.println("ERROR: " + message);

    // Print to file if logging is enabled
    if (logToFile && fileWriter != null) {
      try {
        fileWriter.println(String.format("[%s] ERROR: %s", LocalDateTime.now().format(timeFormatter), message));
        fileWriter.flush();
      } catch (Exception e) {
        System.err.println("Warning: Failed to write to log file: " + e.getMessage());
      }
    }
  }

  public void close() {
    if (fileWriter != null) {
      try {
        fileWriter.close();
      } catch (Exception e) {
        System.err.println("Warning: Failed to close log file: " + e.getMessage());
      }
    }
  }
}
