import java.io.File;

/**
 * Driver class for my adventure game implementation.
 *
 * @author Jeffrey Bour
 */
public class Main {

  public static void main(String[] args) {

    File file = null;

    boolean writeSymbolicClauses = false;

    if (args.length >= 1) {
      file = new File(args[0]);
    } else {
      System.out.println("usage: Main file [-s]");
      System.exit(1);
    }

    if (args.length > 1) {
      if (args.length == 2 && args[1].equals("-s")) {
        writeSymbolicClauses = true;
      } else {
        System.out.println("usage: Main file [-s]");
        System.exit(1);
      }
    }

    FrontEnd fe = new FrontEnd(file, writeSymbolicClauses);
    fe.go();

    DavisPutnam dp = new DavisPutnam();
    dp.go();

    BackEnd be = new BackEnd();
    be.go();

  }

}