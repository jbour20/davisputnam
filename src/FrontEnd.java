import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Frontend for my adventure game implementation.
 * After game specifications are read from given input file,
 * propositions for each of the thirteen categories defining gameplay
 * are generated. These human-readable propositions can be written
 * to a file, if desired (see README). These propositions are then
 * encoded for my Davis-Putnam implementation, and written to a file
 * along with a key for backend interpretation.
 *
 * @author Jeffrey Bour
 */
public class FrontEnd {

  // File to read maze specifications from.
  private final File inFile;

  // Symbolic clause to integer representation mapping.
  private final Map<String, Integer> atomMap;

  // Maze data stored here.
  private final Map<String, Set<String>> graph;
  private final Map<String, Set<String>> treasures;
  private final Map<String, Set<String>> tolls;

  // Write symbolic clauses to file?
  private boolean writeSymbolicClauses;

  // Specified number of moves allowed.
  private int numMoves;

  // Simple enum for atom types.
  // Used in conjunction with inner class Atom
  // to generate string representations of
  // symbolic clauses which can be easily
  // read and hashed.
  private enum Type {

    AT("At"),
    AVAILABLE("Available"),
    HAS("Has");

    private final String type;

    Type(String type) {

      this.type = type;

    }

    @Override
    public String toString() {

      return type;

    }

  }

  // Simple inner class for atoms.
  // Used in conjunction with enum Type
  // to generate string representations of
  // symbolic clauses which can be easily
  // read and hashed.
  private class Atom {

    Type type;
    String node;
    int time;

    Atom(Type type, String node, int time) {

      this.type = type;
      this.node = node;
      this.time = time;

    }

    @Override
    public String toString() {

      return type.toString() + "(" + node + "," + time + ")";

    }

  }

  // FrontEnd constructor. Input file along with a boolean
  // value specifying whether symbolic clauses should be
  // written to a file must be provided.
  FrontEnd(File inFile, boolean writeSymbolicClauses) {

    this.inFile = inFile;
    this.writeSymbolicClauses = writeSymbolicClauses;

    // LinkedHashMaps are used here mainly for debugging purposes.
    atomMap = new LinkedHashMap<String, Integer>();

    graph = new LinkedHashMap<String, Set<String>>();
    treasures = new LinkedHashMap<String, Set<String>>();
    tolls = new LinkedHashMap<String, Set<String>>();

  }

  // Start the FrontEnd program.
  void go() {

    getAtoms();
    getMaze();

    String symbolicClauses = buildOutput();

    if (writeSymbolicClauses) {
      writeToFile(DefaultFiles.SYMBOLIC_CLAUSES, symbolicClauses);
    }

    String content = encodeForDavisPutnam(symbolicClauses);

    writeToFile(DefaultFiles.DAVIS_PUTNAM_INPUT, content);

  }

  // First pass: read node names, treasures, and number of allowed moves.
  private void getAtoms() {

    try {

      BufferedReader br = new BufferedReader(new FileReader(inFile));

      String line = br.readLine().trim();
      String[] nodes = line.split("\\s+");

      line = br.readLine().trim();
      String[] prizes = line.split("\\s+");

      line = br.readLine().trim();
      try {
        numMoves = Integer.parseInt(line.trim());
        mapAtoms(nodes, prizes);
      } catch (NumberFormatException e) {
        // assume input is well-formed...
      }

      br.close();

    } catch (IOException e) {
      // ignore...
    }

  }

  // Create mapping for all possible atoms.
  private void mapAtoms(String[] nodes, String[] prizes) {

    int mapNum = 1;
    Atom atom;
    int i;

    for (String node : nodes) {
      for (i = 0; i <= numMoves; i++) {
        atom = new Atom(Type.AT, node, i);
        atomMap.put(atom.toString(), mapNum);
        mapNum++;
      }
    }

    for (String prize : prizes) {
      for (i = 0; i <= numMoves; i++) {
        atom = new Atom(Type.AVAILABLE, prize, i);
        atomMap.put(atom.toString(), mapNum);
        mapNum++;
      }
    }

    for (String prize : prizes) {
      for (i = 0; i <= numMoves; i++) {
        atom = new Atom(Type.HAS, prize, i);
        atomMap.put(atom.toString(), mapNum);
        mapNum++;
      }
    }

  }

  // Second pass: read in game board specifications.
  private void getMaze() {

    try {

      BufferedReader br = new BufferedReader(new FileReader(inFile));

      String line = br.readLine();
      // skip lines read previously.
      for (int i = 0; i < 3; i++) {
        line = br.readLine();
      }

      while (line != null) {
        if (!line.trim().equals("")) {
          parseLine(line);
        }
        line = br.readLine();
      }

      br.close();

    } catch (IOException e) {
      // ignore...
    }

  }

  // Interpret data for each node.
  private void parseLine(String line) {

    String[] tokens = line.trim().split("\\s+");
    int indx = 1;

    indx = getTreasures(tokens, indx + 1);

    indx = getTolls(tokens, indx + 1);

    getNext(tokens, indx + 1);

  }

  // Get treasures at this node.
  private int getTreasures(String[] tokens, int indx) {

    // LinkedHashSets are used here mainly for debugging purposes.
    Set<String> prizes = new LinkedHashSet<String>();
    String node = tokens[0];

    while (!tokens[indx].equals("TOLLS")) {
      prizes.add(tokens[indx]);
      indx++;
    }

    treasures.put(node, prizes);

    return indx;

  }

  // Get tolls at this node.
  private int getTolls(String[] tokens, int indx) {

    // LinkedHashSets are used here mainly for debugging purposes.
    Set<String> fees = new LinkedHashSet<String>();
    String node = tokens[0];

    while (!tokens[indx].equals("NEXT")) {
      fees.add(tokens[indx]);
      indx++;
    }

    tolls.put(node, fees);

    return indx;

  }

  // Get neighbors of this node.
  private void getNext(String[] tokens, int indx) {

    // LinkedHashSets are used here mainly for debugging purposes.
    Set<String> neighbors = new LinkedHashSet<String>();
    String node = tokens[0];

    // Need to make sure there is a self-loop from goal to itself.
    boolean haveGoal = false;

    while (indx < tokens.length) {
      haveGoal = tokens[indx].equals("GOAL") ? true : haveGoal;
      neighbors.add(tokens[indx]);
      indx++;
    }

    if (node.equals("GOAL") && !haveGoal) {
      neighbors.add("GOAL");
    }

    graph.put(node, neighbors);

  }

  // Build a string of all propositions in symbolic form
  // defining gameplay.
  private String buildOutput() {

    StringBuilder s = new StringBuilder();

    s.append(categoryOne());
    s.append(categoryTwo());
    s.append(categoryThree());
    s.append(categoryFour());
    s.append(categoryFive());
    s.append(categorySix());
    s.append(categorySeven());
    s.append(categoryEight());
    s.append(categoryNine());
    s.append(categoryTen());
    s.append(categoryEleven());
    s.append(categoryTwelve());
    s.append(categoryThirteen());

    return s.toString();

  }

  // Generate propositions specifying that the player
  // can be at just one place at any given time.
  private String categoryOne() {

    StringBuilder s = new StringBuilder();

    String[] nodes = new String[graph.size()];
    Atom atom;
    int i, j, k;

    // Put nodes in an array so they can be accessed via indices.
    i = 0;
    for (String node : graph.keySet()) {
      nodes[i] = node;
      i++;
    }

    for (i = 0; i <= numMoves; i++) {
      for (j = 0; j < nodes.length - 1; j++) {
        for (k = j + 1; k < nodes.length; k++) {
          atom = new Atom(Type.AT, nodes[j], i);
          s.append("-" + atom.toString());
          atom = new Atom(Type.AT, nodes[k], i);
          s.append(" -" + atom.toString() + "\n");
        }
      }
    }

    return s.toString();

  }

  // Generate propositions specifying that the player
  // cannot have a treasure that is also available.
  private String categoryTwo() {

    StringBuilder s = new StringBuilder();
    Atom atom;

    for (Map.Entry<String, Set<String>> entry : treasures.entrySet()) {
      for (String treasure : entry.getValue()) {
        for (int i = 0; i <= numMoves; i++) {
          atom = new Atom(Type.HAS, treasure, i);
          s.append("-" + atom.toString());
          atom = new Atom(Type.AVAILABLE, treasure, i);
          s.append(" -" + atom.toString() + "\n");
        }
      }
    }

    return s.toString();

  }

  // Generate propositions specifying that the player
  // must move along the maze edges.
  private String categoryThree() {

    StringBuilder s = new StringBuilder();
    Atom atom;

    for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
      for (int i = 0; i < numMoves; i++) {
        atom = new Atom(Type.AT, entry.getKey(), i);
        s.append("-" + atom.toString());
        for (String neighbor : entry.getValue()) {
          atom = new Atom(Type.AT, neighbor, i + 1);
          s.append(" " + atom.toString());
        }
        s.append("\n");
      }
    }

    return s.toString();

  }

  // Generate propositions specifying that the player
  // can only visit nodes with tolls if the player has
  // those treasures corresponding to the tolls.
  private String categoryFour() {

    StringBuilder s = new StringBuilder();
    Atom atom;

    for (Map.Entry<String, Set<String>> entry : tolls.entrySet()) {
      for (String toll : entry.getValue()) {
        for (int i = 1; i <= numMoves; i++) {
          atom = new Atom(Type.AT, entry.getKey(), i);
          s.append("-" + atom.toString());
          atom = new Atom(Type.HAS, toll, i - 1);
          s.append(" " + atom.toString() + "\n");
        }
      }
    }

    return s.toString();

  }

  // Generate propositions specifying that the player
  // has a treasure if the treasure is available when
  // that treasure's home is visited by the player.
  private String categoryFive() {

    StringBuilder s = new StringBuilder();
    Atom atom;

    for (Map.Entry<String, Set<String>> entry : treasures.entrySet()) {
      for (String treasure : entry.getValue()) {
        for (int i = 0; i < numMoves; i++) {
          atom = new Atom(Type.AVAILABLE, treasure, i);
          s.append("-" + atom.toString());
          atom = new Atom(Type.AT, entry.getKey(), i + 1);
          s.append(" -" + atom.toString());
          atom = new Atom(Type.HAS, treasure, i + 1);
          s.append(" " + atom.toString() + "\n");
        }
      }
    }

    return s.toString();

  }

  // Generate propositions specifying that the player
  // no longer has a treasure if that player visits a
  // node requiring the treasure as a toll.
  private String categorySix() {

    StringBuilder s = new StringBuilder();
    Atom atom;

    for (Map.Entry<String, Set<String>> entry : tolls.entrySet()) {
      for (String toll : entry.getValue()) {
        for (int i = 0; i <= numMoves; i++) {
          atom = new Atom(Type.AT, entry.getKey(), i);
          s.append("-" + atom.toString());
          atom = new Atom(Type.HAS, toll, i);
          s.append(" -" + atom.toString() + "\n");
        }
      }
    }

    return s.toString();

  }

  // Generate propositions specifying that a treasure is
  // still available if it is currently available when the
  // player visits a node that is not that treasure's home.
  private String categorySeven() {

    StringBuilder s = new StringBuilder();
    Atom atom;

    for (Map.Entry<String, Set<String>> entry : treasures.entrySet()) {
      for (String treasure : entry.getValue()) {
        for (String node : graph.keySet()) {
          if (!node.equals(entry.getKey())) {
            for (int i = 0; i < numMoves; i++) {
              atom = new Atom(Type.AVAILABLE, treasure, i);
              s.append("-" + atom.toString());
              atom = new Atom(Type.AT, node, i + 1);
              s.append(" -" + atom.toString());
              atom = new Atom(Type.AVAILABLE, treasure, i + 1);
              s.append(" " + atom.toString() + "\n");
            }
          }
        }
      }
    }

    return s.toString();

  }

  // Generate propositions specifying that a treasure is
  // not available at any time after it becomes unavailable.
  private String categoryEight() {

    StringBuilder s = new StringBuilder();
    Atom atom;

    for (Map.Entry<String, Set<String>> entry : treasures.entrySet()) {
      for (String treasure : entry.getValue()) {
        for (int i = 0; i < numMoves; i++) {
          atom = new Atom(Type.AVAILABLE, treasure, i);
          s.append(atom.toString());
          atom = new Atom(Type.AVAILABLE, treasure, i + 1);
          s.append(" -" + atom.toString() + "\n");
        }
      }
    }

    return s.toString();

  }

  // Generate propositions specifying that the player
  // does not have a treasure after visitng a node requiring
  // that treasure as a toll.
  private String categoryNine() {

    StringBuilder s = new StringBuilder();
    Atom atom;

    for (Map.Entry<String, Set<String>> entry : treasures.entrySet()) {
      for (String treasure : entry.getValue()) {
        for (int i = 0; i < numMoves; i++) {
          atom = new Atom(Type.AVAILABLE, treasure, i);
          s.append(atom.toString());
          atom = new Atom(Type.HAS, treasure, i);
          s.append(" " + atom.toString());
          atom = new Atom(Type.HAS, treasure, i + 1);
          s.append(" -" + atom.toString() + "\n");
        }
      }
    }

    return s.toString();

  }

  // Generate propositions specifying that the player
  // still has a treasure after visiting a node that does
  // not require that treasure as a toll.
  private String categoryTen() {

    StringBuilder s = new StringBuilder();
    Atom atom;

    for (Map.Entry<String, Set<String>> treasureEntry : treasures.entrySet()) {
      for (String treasure : treasureEntry.getValue()) {
        for (Map.Entry<String, Set<String>> tollEntry : tolls.entrySet()) {
          if (!tollEntry.getValue().contains(treasure)) {
            for (int i = 0; i < numMoves; i++) {
              atom = new Atom(Type.HAS, treasure, i);
              s.append("-" + atom.toString());
              atom = new Atom(Type.AT, tollEntry.getKey(), i + 1);
              s.append(" -" + atom.toString());
              atom = new Atom(Type.HAS, treasure, i + 1);
              s.append(" " + atom.toString() + "\n");
            }
          }
        }
      }
    }

    return s.toString();

  }

  // Generate the single proposition specifying that the player
  // must be at the node 'START' when the game begins.
  private String categoryEleven() {

    Atom atom = new Atom(Type.AT, "START", 0);
    return atom.toString() + "\n";

  }

  // Generate propositions specifying that all treasures
  // are avilable when the game begins.
  private String categoryTwelve() {

    StringBuilder s = new StringBuilder();
    Atom atom;

    for (Map.Entry<String, Set<String>> entry : treasures.entrySet()) {
      for (String treasure : entry.getValue()) {
        atom = new Atom(Type.AVAILABLE, treasure, 0);
        s.append(atom.toString() + "\n");
      }
    }

    return s.toString();

  }

  // Generate the single proposition specifying that the player
  // must be at the node 'GOAL' after the specifie number of moves.
  private String categoryThirteen() {

    Atom atom = new Atom(Type.AT, "GOAL", numMoves);
    return atom.toString() + "\n";

  }

  // Generate a string representation of the integer to symbolic
  // clause mapping. This acts as a key for the backend to interpret
  // the results generated by the Davis-Putnam algorithm.
  private String mapToString() {

    StringBuilder s = new StringBuilder();

    int len = String.valueOf(atomMap.size()).length();

    String format = "%" + len + "d %s\n";

    for (Map.Entry<String, Integer> entry : atomMap.entrySet()) {
      s.append(String.format(format, entry.getValue(), entry.getKey()));
    }

    return s.toString();

  }

  // Replace clauses in symbolic form with integer representations
  // for Davis-Putnam.
  private String encodeForDavisPutnam(String symbolicClauses) {

    StringBuilder s = new StringBuilder();

    try {

      BufferedReader br = new BufferedReader(new StringReader(symbolicClauses));

      String line = br.readLine();

      while (line != null) {

        String[] clauses = line.trim().split("\\s+");

        for (String clause : clauses) {

          int val;

          if (clause.startsWith("-")) {
            val = atomMap.get(clause.substring(1));
            s.append("-" + val + " ");
          } else {
            val = atomMap.get(clause);
            s.append(val + " ");
          }

        }

        s.append("\n");
        line = br.readLine();

      }
    } catch (IOException e) {
      // ignore...
    }

    return s.toString();

  }

  // Write content to specified file.
  private void writeToFile(DefaultFiles outFile, String content) {

    content += ("0\n" + mapToString());

    try {

      BufferedWriter bw = new BufferedWriter(new FileWriter(outFile.getFile()));
      bw.write(content);
      bw.close();

    } catch (IOException e) {
      // ignore...
    }

  }

}