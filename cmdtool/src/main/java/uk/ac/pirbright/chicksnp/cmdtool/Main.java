package uk.ac.pirbright.chicksnp.cmdtool;

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
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.NameClassPair;

import uk.ac.pirbright.chicksnp.util.*;

import uk.ac.pirbright.chicksnp.ejb.entity.*;

import uk.ac.pirbright.chicksnp.ejb.session.SnpSession;
import uk.ac.pirbright.chicksnp.ejb.session.SnpSessionBean;


public class Main
{
  private static SnpSession snpSession;


  private static void vcfImport(String chickenLineName, String filename) throws Exception
  {
    snpSession.importVcf(chickenLineName, filename);
  }


  private static void chickenLineImport(String filename) throws Exception
  {
    snpSession.importChickenLines(filename);
  }


  private static List<String> csvToList(String csv)
  {
    List<String> csvList = new ArrayList<String>();
    for (String w : csv.split(","))
    {
      csvList.add(w.trim());
    }
    return (csvList);
  }


  private static Set<String> csvToSet(String csv)
  {
    return (new HashSet<String>(csvToList(csv)));
  }


  private static void testSplit(String splitArg) throws Exception
  {
    String[] w = splitArg.split("\\|");
    if (w.length != 2)
    {
      throw new IllegalArgumentException(String.format("malformed split arg \"%s\"", splitArg));
    }
    Set<String> chickenLineNameSet1 = csvToSet(w[0]);
    Set<String> chickenLineNameSet2 = csvToSet(w[1]);
    List<ChickenLocus> chickenLocusList = snpSession.findDifferentialSnpLocusList(chickenLineNameSet1, chickenLineNameSet2);
    for (ChickenLocus chickenLocus : chickenLocusList)
    {
      System.err.println(chickenLocus);
    }
  }


  public static void main(String[] args) throws Exception
  {
    String sessionJndiName = EjbSupport.sessionJndiName(SnpSessionBean.class, SnpSession.class);
    Context context = new InitialContext();
    snpSession = (SnpSession) context.lookup(sessionJndiName);
    if (args.length == 0)
    {
      System.err.println("usage: cmdtool <cmd> [args...]");
      System.exit(1);
    }
    String cmd = args[0];
    if ("vcfimport".equals(cmd))
    {
      vcfImport(args[1], args[2]);
    }
    else if ("chickimport".equals(cmd))
    {
      chickenLineImport(args[1]);
    }
    else if ("testsplit".equals(cmd))
    {
      testSplit(args[1]);
    }
    else
    {
      System.err.println(String.format("unrecognised command: %s", cmd));
      System.exit(1);
    }
  }
}
