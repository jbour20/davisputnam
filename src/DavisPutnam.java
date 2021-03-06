import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Davis-Putnam algorithm for my adventure game implementation.
 * Reads in file generated by my frontend program at location specified in
 * DefaultFiles.java. The algorithm itself follows the outline provided by
 * profesor Davis, and does not require any explanation. If a solution is
 * found, a mapping from atom numbers to T/F values is written to file,
 * followed by the frontend-provided key: otherwise just the key is written.
 *
 * @author Jeffrey Bour
 */
public class DavisPutnam {

  // Total number of atoms.
  private int numAtoms;

  DavisPutnam() {

    numAtoms = Integer.MIN_VALUE;

  }

  // Start the DavisPutnam program.
  void go() {

    List<List<Integer>> clauses = getClauses(DefaultFiles.DAVIS_PUTNAM_INPUT);
    String encoding = getEncoding(DefaultFiles.DAVIS_PUTNAM_INPUT);

    // Valuations array is 1-based, hence the size numAtoms + 1.
    Boolean[] result = dp1(clauses, new Boolean[numAtoms + 1]);

    if (result != null) {
      assignUnbound(result);
    }

    String content = buildOutput(result, encoding);

    writeToFile(DefaultFiles.DAVIS_PUTNAM_OUTPUT, content);

  }

  // First pass: read in propositional clauses and return as a list of lists.
  private List<List<Integer>> getClauses(DefaultFiles inFile) {

    List<List<Integer>> clauses = new ArrayList<List<Integer>>();

    try {

      BufferedReader br = new BufferedReader(new FileReader(inFile.getFile()));
      String line = br.readLine();

      while (!line.startsWith("0")) {

        List<Integer> clause = new ArrayList<Integer>();
        String[] literals = line.trim().split("\\s+");

        for (String literal : literals) {
          try {
            Integer val = Integer.parseInt(literal);
            clause.add(val);
            numAtoms = (Math.abs(val) > numAtoms) ? Math.abs(val) : numAtoms;
          } catch (NumberFormatException e) {
            // assume input is well-formed...
          }
        }

        clauses.add(clause);
        line = br.readLine();

      }

      br.close();

    } catch (IOException e) {
      // ignore...
    }

    return clauses;

  }

  // Second pass: get frontend-provided key that will be appended to the results
  // and passed to the backend.
  private String getEncoding(DefaultFiles inFile) {

    StringBuilder s = new StringBuilder();

    try {

      BufferedReader br = new BufferedReader(new FileReader(inFile.getFile()));
      String line = br.readLine();

      // skip lines read previously.
      while (!line.startsWith("0")) {
        line = br.readLine();
      }

      while (line != null) {
        s.append(line + "\n");
        line = br.readLine();
      }

      br.close();

    } catch (IOException e) {
      // ignore...
    }

    return s.toString();

  }

  // Main DavisPutnam recursive method.
  private Boolean[] dp1(List<List<Integer>> clauses, Boolean[] valuations) {

    int atom;

    while (true) {

      if (clauses.isEmpty()) {
        return valuations;
      } else if (checkFailure(clauses)) {
        return null;
      } else if ((atom = checkPureLiteral(clauses)) != 0) {
        valuations = obviousAssign(valuations, atom);
        deleteClauses(clauses, atom);
      } else if ((atom = checkSingleLiteral(clauses)) != 0) {
        valuations = obviousAssign(valuations, atom);
        clauses = propagate(clauses, atom);
      } else {
        break;
      }

    }

    atom = findFirstUnbound(valuations);
    valuations[atom] = true;
    List<List<Integer>> clausesNew = propagate(clauses, atom);
    Boolean[] vNew = dp1(clausesNew, valuations);

    if (vNew != null) {
      return vNew;
    }

    valuations[atom] = false;
    clausesNew = propagate(clauses, atom * -1);

    return dp1(clausesNew, valuations);

  }

  // Check to see if some clause is empty.
  private boolean checkFailure(List<List<Integer>> clauses) {

    for (List<Integer> clause : clauses) {
      if (clause.isEmpty()) {
        return true;
      }
    }

    return false;

  }

  // Check to see if some atom exists in pure literal form.
  // Return that literal if it exists, otherwise return 0.
  private int checkPureLiteral(List<List<Integer>> clauses) {

    boolean[] literals = new boolean[numAtoms + 1];
    boolean[] negation = new boolean[numAtoms + 1];

    // Store the existence of each atom as pure or otherwise.
    for (List<Integer> clause : clauses) {
      for (Integer literal : clause) {
        int val = Math.abs(literal);
        if (literal < 0) {
          negation[val] = true;
        } else {
          literals[val] = true;
        }
      }
    }

    // If literals[i] != negation[i] we have a pure literal.
    for (int i = 1; i < literals.length; i++) {
      if (literals[i] != negation[i]) {
        return negation[i] ? -1 * i : i;
      }
    }

    return 0;

  }

  // Check to see if some clause contains a single literal.
  // If so then return that literal, otherwise return 0.
  private int checkSingleLiteral(List<List<Integer>> clauses) {

    for (List<Integer> clause : clauses) {
      if (clause.size() == 1) {
        return clause.get(0);
      }
    }

    return 0;

  }

  // Return a copy of valuations with the assignment given by atom.
  private Boolean[] obviousAssign(Boolean[] valuations, int atom) {

    Boolean[] copy = new Boolean[valuations.length];

    for (int i = 1; i < valuations.length; i++) {
      if (valuations[i] != null) {
        copy[i] = new Boolean(valuations[i].booleanValue());
      }
    }

    copy[Math.abs(atom)] = atom > 0 ? true : false;

    return copy;

  }

  // Delete clauses containing literal atom.
  private void deleteClauses(List<List<Integer>> clauses, int atom) {

    // Need to use an iterator since we're potentially altering our list.
    Iterator<List<Integer>> iter = clauses.iterator();

    while (iter.hasNext()) {

      List<Integer> clause = iter.next();
      boolean remove = false;

      for (Integer literal : clause) {
        if (literal == atom) {
          remove = true;
          break;
        }
      }

      if (remove) {
        iter.remove();
      }

    }

  }

  // Remove clauses containing literal atom, and remove literals from clauses
  // that are the negation of atom.
  private List<List<Integer>> propagate(List<List<Integer>> clauses, int atom) {

    List<List<Integer>> result = new ArrayList<List<Integer>>();

    for (List<Integer> clause : clauses) {

      List<Integer> resultInner = new ArrayList<Integer>();
      boolean remove = false;

      for (Integer literal : clause) {
        if (literal == atom) {
          remove = true;
          break;
        } else if (Math.abs(atom) != Math.abs(literal)) {
          resultInner.add(literal);
        }
      }

      if (!remove) {
        result.add(resultInner);
      }

    }

    return result;

  }

  // Return the index of the first unbound atom.
  private int findFirstUnbound(Boolean[] valuations) {

    for (int i = 1; i < valuations.length; i++) {
      if (valuations[i] == null) {
        return i;
      }
    }

    return 0;

  }

  // Assign all unbound variables to true.
  private void assignUnbound(Boolean[] valuations) {

    for (int i = 1; i < valuations.length; i++) {
      if (valuations[i] == null) {
        valuations[i] = true;
      }
    }

  }

  // Build the string that will be written to our output file.
  // If no solution is found then simply return the key.
  private String buildOutput(Boolean[] result, String encoding) {

    StringBuilder s = new StringBuilder();

    if (result != null) {

      int len = String.valueOf(result.length).length();
      String format = "%" + len + "d %s\n";

      for (int i = 1; i < result.length; i++) {
        String c = result[i] ? "T" : "F";
        s.append(String.format(format, i, c));
      }

    }

    s.append(encoding);

    return s.toString();

  }

  // Write content to specified file.
  private void writeToFile(DefaultFiles outFile, String content) {

    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(outFile.getFile()));
      bw.write(content);
      bw.close();
    } catch (IOException e) {
      // ignore...
    }

  }

}
