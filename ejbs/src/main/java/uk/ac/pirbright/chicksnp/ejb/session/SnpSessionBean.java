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
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import javax.ejb.Stateless;
import javax.ejb.Remote;
import javax.ejb.Local;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import javax.persistence.PersistenceContext;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import static org.javamisc.Util.genericTypecast;

import uk.ac.pirbright.chicksnp.ejb.entity.*;


@Stateless
@Remote({SnpSession.class})
public class SnpSessionBean implements SnpSession
{
  @PersistenceContext
  private EntityManager entityManager;


  public SnpSessionBean()
  {
    super();
  }


  private ChickenLine findChickenLine(String chickenLineName)
  {
    Query query = this.entityManager.createQuery("SELECT l FROM ChickenLine l WHERE l.name = :name");
    query.setParameter("name", chickenLineName);
    List<ChickenLine> entityList = genericTypecast(query.getResultList());
    if (entityList.size() == 0)
    {
      return (null);
    }
    else if (entityList.size() == 1)
    {
      return (entityList.get(0));
    }
    else
    {
      throw new RuntimeException(String.format("%d instances of ChickenLine with name %s", entityList.size(), chickenLineName));
    }
  }


  private Set<ChickenLine> findChickenLineSet(Set<String> chickenLineNameSet)
  {
    Set<ChickenLine> chickenLineSet = new HashSet<ChickenLine>();
    for (String chickenLineName : chickenLineNameSet)
    {
      chickenLineSet.add(this.findChickenLine(chickenLineName));
    }
    return (chickenLineSet);
  }


  private ChickenChromosome findChickenChromosome(String chickenChromosomeName)
  {
    Query query = this.entityManager.createQuery("SELECT c FROM ChickenChromosome c WHERE c.name = :name");
    query.setParameter("name", chickenChromosomeName);
    List<ChickenChromosome> entityList = genericTypecast(query.getResultList());
    if (entityList.size() == 0)
    {
      return (null);
    }
    else if (entityList.size() == 1)
    {
      return (entityList.get(0));
    }
    else
    {
      throw new RuntimeException(String.format("%d instances of ChickenChromosome with name %s", entityList.size(), chickenChromosomeName));
    }
  }


  private ChickenChromosome findOrInsertChickenChromosome(String chickenChromosomeName)
  {
    ChickenChromosome chickenChromosome = this.findChickenChromosome(chickenChromosomeName);
    if (chickenChromosome == null)
    {
      chickenChromosome = new ChickenChromosome(chickenChromosomeName);
      this.entityManager.persist(chickenChromosome);
    }
    return (chickenChromosome);
  }


  private ChickenSnp vcfLineToChickenSnp(ChickenLine chickenLine, String vcfLine)
  {
    String[] w = vcfLine.split("\t");
    String chrom = w[0];
    int pos = Integer.parseInt(w[1].trim());
    String id = w[2];
    String ref = w[3];
    String alt = w[4];
    String qual = w[5];
    String filter = w[6];
    String info = w[7];
    ChickenChromosome chickenChromosome = this.findOrInsertChickenChromosome(chrom);
    ChickenSnp chickenSnp = new ChickenSnp(chickenLine, chickenChromosome, pos, ref, alt);
    return (chickenSnp);
  }


  public void importVcf(String chickenLineName, String filename) throws IOException
  {
    ChickenLine chickenLine = this.findChickenLine(chickenLineName);
    BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
    for (String line = r.readLine(); line != null; line = r.readLine())
    {
      if ((line.length() > 0) && !line.startsWith("#"))
      {
        ChickenSnp chickenSnp = vcfLineToChickenSnp(chickenLine, line);
        this.entityManager.persist(chickenSnp);
      }
    }
  }


  public ChickenSnp insertChickenSnp(ChickenSnp chickenSnp)
  {
    ChickenLine chickenLine = chickenSnp.getChickenLine();
    if (chickenLine != null)
    {
      ChickenLine persistedChickenLine = this.findChickenLine(chickenLine.getName());
      if (persistedChickenLine == null)
      {
        throw new RuntimeException(String.format("no persisted line with name \"%s\"", chickenLine.getName()));
      }
      chickenSnp.unlinkChickenLine();
      chickenSnp.linkChickenLine(persistedChickenLine);
    }
    ChickenChromosome chickenChromosome = chickenSnp.getChickenChromosome();
    if (chickenChromosome != null)
    {
      chickenSnp.unlinkChickenChromosome();
      ChickenChromosome persistedChickenChromosome = this.findOrInsertChickenChromosome(chickenChromosome.getName());
      chickenSnp.linkChickenChromosome(persistedChickenChromosome);
    }
    this.entityManager.persist(chickenSnp);
    return (chickenSnp);
  }


  public void importChickenLines(String filename) throws IOException
  {
    BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
    for (String line = r.readLine(); line != null; line = r.readLine())
    {
      if (line.length() > 0)
      {
        ChickenLine chickenLine = new ChickenLine(line);
        this.entityManager.persist(chickenLine);
      }
    }
  }


  private Set<ChickenLine> findPersistedChickenLineSetByName(Set<String> chickenLineNameSet)
  {
    Set<ChickenLine> chickenLineSet = new HashSet<ChickenLine>();
    for (String chickenLineName : chickenLineNameSet)
    {
      ChickenLine persistedChickenLine = this.findChickenLine(chickenLineName);
      if (persistedChickenLine == null)
      {
        throw new RuntimeException(String.format("no chicken line with name \"%s\" found", chickenLineName));
      }
    }
    return (chickenLineSet);
  }


  private Set<ChickenLine> findPersistedChickenLineSet(Set<ChickenLine> chickenLineSet)
  {
    Set<String> chickenLineNameSet = new HashSet<String>();
    for (ChickenLine chickenLine : chickenLineSet)
    {
      // FIXME: should check for duplicate names and null names
      chickenLineNameSet.add(chickenLine.getName());
    }
    return (this.findPersistedChickenLineSetByName(chickenLineNameSet));
  }


  private static Set<String> findNucleotideSet(ChickenLocus chickenLocus, Set<ChickenLine> chickenLineSet)
  {
    Set<String> nucleotideSet = new HashSet<String>();
    for (ChickenLine chickenLine : chickenLineSet)
    {
      Set<ChickenSnp> chickenSnpSet = chickenLocus.findChickenLineSnpSet(chickenLine);
      for (ChickenSnp chickenSnp : chickenSnpSet)
      {
        nucleotideSet.add(chickenSnp.getAlt());
      }
    }
    return (nucleotideSet);
  }


  public List<ChickenLocus> findDifferentialSnpLocusList(Set<String> chickenLineNameSet1, Set<String> chickenLineNameSet2)
  {
    Set<ChickenLine> chickenLineSet1 = this.findChickenLineSet(chickenLineNameSet1);
    Set<ChickenLine> chickenLineSet2 = this.findChickenLineSet(chickenLineNameSet2);
    Set<String> chickenLineNameSet = new HashSet<String>(chickenLineNameSet1);
    chickenLineNameSet.addAll(chickenLineNameSet2);
    System.err.println(String.format("findDifferentialSnpList: total number of line names is %d", chickenLineNameSet.size()));
    Query query = this.entityManager.createQuery("SELECT s FROM ChickenSnp s WHERE s.chickenLine.name IN ( :nameSet ) ORDER BY s.chickenChromosome.name, s.pos");
    query.setParameter("nameSet", chickenLineNameSet);
    // query.setParameter("nameSet", chickenLineNameSet.iterator().next());
    List<ChickenSnp> chickenSnpList = genericTypecast(query.getResultList());
    System.err.println(String.format("findDifferentialSnpList: got %d SNPs", chickenSnpList.size()));
    for (ChickenSnp chickenSnp : chickenSnpList)
    {
      ChickenLine chickenLine = chickenSnp.getChickenLine();
      ChickenChromosome chickenChromosome = chickenSnp.getChickenChromosome();
      System.err.println(String.format("%10s  %3s  %8d  %s  %s", chickenLine.getName(), chickenChromosome.getName(), chickenSnp.getPos(), chickenSnp.getRef(), chickenSnp.getAlt()));
    }
    List<ChickenLocus> chickenLocusList = new ArrayList<ChickenLocus>();
    int i = 0;
    while (i < chickenSnpList.size())
    {
      ChickenLocus chickenLocus = new ChickenLocus(chickenSnpList.get(i));
      int j = i + 1;
      while ((j < chickenSnpList.size()) && chickenLocus.atLocus(chickenSnpList.get(j)))
      {
        chickenLocus.addChickenSnp(chickenSnpList.get(j++));
      }
      i = j;
      Set<String> nucleotideSet1 = this.findNucleotideSet(chickenLocus, chickenLineSet1);
      Set<String> nucleotideSet2 = this.findNucleotideSet(chickenLocus, chickenLineSet2);
      nucleotideSet1.retainAll(nucleotideSet2);
      if (nucleotideSet1.size() == 0)
      {
        chickenLocusList.add(chickenLocus);
      }
    }
    return (chickenLocusList);
  }
}
