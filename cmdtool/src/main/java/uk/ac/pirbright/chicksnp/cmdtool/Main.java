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
import java.util.Map;
import java.util.HashMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.NameClassPair;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.DriverManager;

import uk.ac.pirbright.chicksnp.util.*;

import uk.ac.pirbright.chicksnp.ejb.entity.*;

import uk.ac.pirbright.chicksnp.ejb.session.SnpSession;
import uk.ac.pirbright.chicksnp.ejb.session.SnpSessionLocal;
import uk.ac.pirbright.chicksnp.ejb.session.SnpSessionBean;


public class Main
{
  private final static int CHICKENSNP_SQL_BATCH_SIZE = 10000;
  private static SnpSession snpSession;


  private static Map<String, ChickenChromosome> makeChickenChromosomeMap(Connection connection) throws SQLException
  {
    Map<String, ChickenChromosome> chickenChromosomeMap = new HashMap<String, ChickenChromosome>();
    Statement chromosomeSelectStatement = connection.createStatement();
    ResultSet resultSet = chromosomeSelectStatement.executeQuery("SELECT id, name FROM ChickenChromosome");
    while (resultSet.next())
    {
      ChickenChromosome chickenChromosome = new ChickenChromosome(resultSet.getString("name"));
      chickenChromosome.setId(new Integer(resultSet.getInt("id")));
      chickenChromosomeMap.put(chickenChromosome.getName(), chickenChromosome);
      System.err.println(String.format("added to map: %s, %d", chickenChromosome.getName(), chickenChromosome.getId().intValue()));
    }
    resultSet.close();
    chromosomeSelectStatement.close();
    return (chickenChromosomeMap);
  }


  private static void vcfImportJdbc(String chickenLineName, String filename, String dbName, String dbUsername, String dbPassword) throws Exception
  {
    Class.forName("org.postgresql.Driver");
    BufferedReader vcfReader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
    String connectionUrl = String.format("jdbc:postgresql://localhost/%s?user=%s&password=%s", dbName, dbUsername, dbPassword);
    // System.err.println(connectionUrl);
    Connection connection = DriverManager.getConnection(connectionUrl);
    PreparedStatement chickenLineSelectStatement = connection.prepareStatement("SELECT id FROM ChickenLine WHERE name = ?");
    chickenLineSelectStatement.setString(1, chickenLineName);
    ResultSet resultSet = chickenLineSelectStatement.executeQuery();
    if (!resultSet.next())
    {
      resultSet.close();
      chickenLineSelectStatement.close();
      connection.close();
      throw new RuntimeException(String.format("no chicken line with name \"%s\" found", chickenLineName));
    }
    int chickenLineId = resultSet.getInt("id");
    // System.err.println(String.format("vcfImportJdbc: found id %d for chicken line \"%s\"", chickenLineId, chickenLineName));
    resultSet.close();
    chickenLineSelectStatement.close();
    Map<String, ChickenChromosome> chickenChromosomeMap = makeChickenChromosomeMap(connection);
    PreparedStatement chromosomeInsertStatement = connection.prepareStatement("INSERT INTO ChickenChromosome (id, name) VALUES (nextval('hibernate_sequence'), ?)");
    PreparedStatement snpInsertStatement = connection.prepareStatement("INSERT INTO ChickenSnp (id, chickenline_id, chickenchromosome_id, pos, dbSnpId, ref, alt) VALUES (nextval('hibernate_sequence'), ?, ?, ?, ?, ?, ?)");
    int batchSize = 0;
    for (String vcfLine = vcfReader.readLine(); vcfLine != null; vcfLine = vcfReader.readLine())
    {
      ChickenLine chickenLine = new ChickenLine(chickenLineName);
      for (ChickenSnp chickenSnp : ChickenSnp.vcfLineToChickenSnpSet(chickenLine, vcfLine))
      {
        String chromosomeName = chickenSnp.getChickenChromosome().getName();
        ChickenChromosome chickenChromosome = chickenChromosomeMap.get(chromosomeName);
        if (chickenChromosome == null)
        {
          chromosomeInsertStatement.setString(1, chromosomeName);
          chromosomeInsertStatement.executeUpdate();
          // FIXME: clumsy / inefficient to construct a whole new map
          // rather than adding just one element -- but as there are
          // not that many chromosomes I leave that to future
          // optimisation
          chickenChromosomeMap = makeChickenChromosomeMap(connection);
          chickenChromosome = chickenChromosomeMap.get(chromosomeName);
          if (chickenChromosome == null)
          {
            throw new RuntimeException(String.format("chromosome \"%s\" not found and insertion failed", chromosomeName));
          }
        }
        snpInsertStatement.clearParameters();
        snpInsertStatement.setInt(1, chickenLineId);
        snpInsertStatement.setInt(2, chickenChromosome.getId().intValue());
        snpInsertStatement.setInt(3, chickenSnp.getPos());
        snpInsertStatement.setString(4, chickenSnp.getDbSnpId());
        snpInsertStatement.setString(5, chickenSnp.getRef());
        snpInsertStatement.setString(6, chickenSnp.getAlt());
        snpInsertStatement.addBatch();
        batchSize++;
        if (batchSize >= CHICKENSNP_SQL_BATCH_SIZE)
        {
          int[] batchResult = snpInsertStatement.executeBatch();
          batchSize = 0;
          System.err.println(String.format("executed batch, %d results", batchResult.length));
       }
      }
    }
    if (batchSize >= 0)
    {
      int[] batchResult = snpInsertStatement.executeBatch();
      System.err.println(String.format("executed batch, %d results", batchResult.length));
    }
    chromosomeInsertStatement.close();
    snpInsertStatement.close();
    connection.close();
  }


  private static void vcfImport(String chickenLineName, String filename) throws Exception
  {
    // server side parsing

    String fullPath = String.format("%s%s%s", System.getProperty("user.dir"), System.getProperty("file.separator"), filename);
    System.err.println(String.format("importing from %s", fullPath));
    snpSession.importVcf(chickenLineName, fullPath);

    // client side parsing, transfer in blocks
    /*
    ChickenLine chickenLine = snpSession.findChickenLine(chickenLineName);
    if (chickenLine == null)
    {
      throw new RuntimeException(String.format("no chicken line with name \"%s\" exists", chickenLineName));
    }
    BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
    chickenLine = new ChickenLine(chickenLineName);
    for (String line = r.readLine(); line != null; line = r.readLine())
    {
      if ((line.length() > 0) && !line.startsWith("#"))
      {
        ChickenSnp.vcfLineToChickenSnpSet(chickenLine, line);
      }
      if (chickenLine.getChickenSnpSet().size() >= SNP_BATCH_SIZE)
      {
        snpSession.insertChickenSnpsFromLine(chickenLine);
        chickenLine = new ChickenLine(chickenLineName);
      }
    }
    // System.err.println(String.format("%d snps read", chickenLine.getChickenSnpSet().size()));
    if (chickenLine.getChickenSnpSet().size() > 0)
    {
      snpSession.insertChickenSnpsFromLine(chickenLine);
    }
    */
    // client side parsing, transfer of individual SNPs
    /*
    for (String line = r.readLine(); line != null; line = r.readLine())
    {
      if ((line.length() > 0) && !line.startsWith("#"))
      {
        chickenLine = new ChickenLine(chickenLineName);
        for (ChickenSnp chickenSnp : ChickenSnp.vcfLineToChickenSnpSet(chickenLine, line))
        {
          snpSession.insertChickenSnp(chickenSnp);
        }
      }
    }
    */
  }


  private static void chickenLineImport(String filename) throws Exception
  {
    // snpSession.importChickenLines(filename);
    BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
    for (String line = r.readLine(); line != null; line = r.readLine())
    {
      ChickenLine chickenLine = new ChickenLine(line);
      snpSession.insertChickenLine(chickenLine);
    }
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


  private static void testSplit(String splitArg, String rangeArg) throws Exception
  {
    GenomicRange genomicRange = null;
    if (rangeArg != null)
    {
      genomicRange = new GenomicRange(rangeArg);
      System.err.println(String.format("genomic range: %s", genomicRange.toString()));
    }
    String[] w = splitArg.split("\\|");
    if (w.length != 2)
    {
      throw new IllegalArgumentException(String.format("malformed split arg \"%s\"", splitArg));
    }
    Set<String> chickenLineNameSet1 = csvToSet(w[0]);
    Set<String> chickenLineNameSet2 = csvToSet(w[1]);
    List<ChickenLocus> chickenLocusList = snpSession.findDifferentialSnpLocusList(chickenLineNameSet1, chickenLineNameSet2, genomicRange);
    for (ChickenLocus chickenLocus : chickenLocusList)
    {
      System.out.println(chickenLocus);
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
    else if ("vcfjdbcimport".equals(cmd))
    {
      String chickenLineName = args[1];
      String filename = args[2];
      String dbName = args[3];
      String dbUsername = args[4];
      String dbPassword = args[5];
      vcfImportJdbc(chickenLineName, filename, dbName, dbUsername, dbPassword);
    }
    else if ("chickimport".equals(cmd))
    {
      chickenLineImport(args[1]);
    }
    else if ("testsplit".equals(cmd))
    {
      if (args.length > 2)
      {
        testSplit(args[1], args[2]);
      }
      else
      {
        testSplit(args[1], null);
      }
    }
    else
    {
      System.err.println(String.format("unrecognised command: %s", cmd));
      System.exit(1);
    }
  }
}
