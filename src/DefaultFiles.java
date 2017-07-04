import java.io.File;

/**
 * Simple enum containing file names and locations the various
 * components of my adventure game imlpementation should write to
 * and read from.
 *
 * @author Jeffrey Bour
 */
public enum DefaultFiles {

  DAVIS_PUTNAM_INPUT(new File("outputs/davis_putnam_input.txt")),
  DAVIS_PUTNAM_OUTPUT(new File("outputs/davis_putnam_output.txt")),
  SYMBOLIC_CLAUSES(new File("outputs/symbolic_clauses.txt")),
  SOLUTION(new File("outputs/solution.txt"));

  private final File file;

  DefaultFiles(File file) {

    this.file = file;

  }

  File getFile() {

    return file.getAbsoluteFile();

  }

}