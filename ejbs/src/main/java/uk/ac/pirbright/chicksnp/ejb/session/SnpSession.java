package uk.ac.pirbright.chicksnp.ejb.session;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.util.List;
import java.util.Set;

import uk.ac.pirbright.chicksnp.ejb.entity.*;


public interface SnpSession
{
  ChickenLine findChickenLine(String chickenLineName);
  void importVcf(String chickenLineName, String filename) throws IOException;
  void importChickenLines(String filename) throws IOException;
  ChickenSnp insertChickenSnp(ChickenSnp chickenSnp);
  void insertChickenSnpsFromLine(ChickenLine chickenLine);
  ChickenLine insertChickenLine(ChickenLine chickenLine);
  List<ChickenLocus> findDifferentialSnpLocusList(Set<String> chickenLineNameSet1, Set<String> chickenLineNameSet2);
}
