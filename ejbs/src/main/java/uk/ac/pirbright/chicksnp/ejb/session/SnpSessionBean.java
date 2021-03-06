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

import java.util.NoSuchElementException;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import javax.ejb.Stateless;
import javax.ejb.Remote;
import javax.ejb.Local;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import javax.persistence.PersistenceContext;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import static org.javamisc.Util.genericTypecast;

import uk.ac.pirbright.chicksnp.util.GenomicRange;

import uk.ac.pirbright.chicksnp.ejb.entity.*;


interface QueryProvider<GenericEntity>
{
  Query getQuery();
}


class ChickenSnpQueryProvider implements QueryProvider<ChickenSnp>
{
  private Set<String> chickenLineNameSet;
  private EntityManager entityManager;


  public ChickenSnpQueryProvider(EntityManager entityManager, Set<String> chickenLineNameSet)
  {
    this.entityManager = entityManager;
    this.chickenLineNameSet = chickenLineNameSet;
  }


  @Override
  public Query getQuery()
  {
    Query query = this.entityManager.createQuery("SELECT s FROM ChickenSnp s WHERE s.chickenLine.name IN ( :nameSet ) ORDER BY s.chickenChromosome.name, s.pos");
    query.setParameter("nameSet", this.chickenLineNameSet);
    return (query);
  }
}


class BufferedResultListIterator<GenericEntity> implements Iterator<GenericEntity>
{
  private GenericEntity nextEntity;
  private Query query;
  private int offset;
  private Iterator<GenericEntity> resultListIterator;


  public BufferedResultListIterator(Query query, int bufsize)
  {
    this.query = query;
    this.query.setMaxResults(bufsize);
    this.offset = 0;
    this.fetchNextChunk();
    this.fetchNextEntity();
  }


  private void fetchNextChunk()
  {
    // System.err.println(String.format("fetching chunk at offset %d", this.offset));
    query.setFirstResult(this.offset);
    List<GenericEntity> entityList = genericTypecast(this.query.getResultList());
    this.offset += entityList.size();
    System.err.println(String.format("fetched %d entities, new offset %d", entityList.size(), this.offset));
    this.resultListIterator = entityList.iterator();
  }


  private void fetchNextEntity()
  {
    if (this.resultListIterator.hasNext())
    {
      this.nextEntity = this.resultListIterator.next();
    }
    else
    {
      this.fetchNextChunk();
      if (this.resultListIterator.hasNext())
      {
        this.nextEntity = this.resultListIterator.next();
      }
      else
      {
        this.nextEntity = null;
      }
    }
  }


  public boolean hasNext()
  {
    // System.err.println(String.format("hasNext: %s", this.nextEntity != null));
    return (this.nextEntity != null);
  }


  public GenericEntity next()
  {
    // System.err.println("next");
    GenericEntity entity = this.nextEntity;
    if (entity == null)
    {
      throw new NoSuchElementException("no next entity");
    }
    this.fetchNextEntity();
    return (entity);
  }


  public void remove()
  {
    throw new UnsupportedOperationException("BufferedResultListIterator does not support remove()");
  }
}


class BufferedResultList<GenericEntity> implements Iterable<GenericEntity>
{
  private static final int DEFAULT_BUFSIZE = 10000;

  private Query query;
  private int bufsize;


  public BufferedResultList(Query query, int bufsize)
  {
    this.bufsize = bufsize;
    this.query = query;
  }


  public BufferedResultList(Query query)
  {
    this(query, DEFAULT_BUFSIZE);
  }


  public Iterator<GenericEntity> iterator()
  {
    return (new BufferedResultListIterator(this.query, this.bufsize));
  }
}


class ChickenLocusScanner
{
  private Iterator<ChickenSnp> chickenSnpIterator;
  private ChickenSnp nextChickenSnp;


  public ChickenLocusScanner(Iterator<ChickenSnp> chickenSnpIterator)
  {
    this.chickenSnpIterator = chickenSnpIterator;
    this.fetchNext();
  }


  public ChickenLocusScanner(Iterable<ChickenSnp> chickenSnpIterable)
  {
    this(chickenSnpIterable.iterator());
  }


  private void fetchNext()
  {
    if (this.chickenSnpIterator.hasNext())
    {
      this.nextChickenSnp = this.chickenSnpIterator.next();
    }
    else
    {
      this.nextChickenSnp = null;
    }
  }

  public ChickenLocus nextChickenLocus()
  {
    if (this.nextChickenSnp == null)
    {
      return (null);
    }
    ChickenLocus chickenLocus = new ChickenLocus(this.nextChickenSnp);
    this.fetchNext();
    while ((this.nextChickenSnp != null) && chickenLocus.atLocus(this.nextChickenSnp))
    {
      chickenLocus.addChickenSnp(this.nextChickenSnp);
      this.fetchNext();
    }
    return (chickenLocus);
  }
}


@Stateless
@Remote({SnpSession.class})
@Local({SnpSessionLocal.class})
public class SnpSessionBean implements SnpSession
{
  @PersistenceContext
  private EntityManager entityManager;


  public SnpSessionBean()
  {
    super();
  }


  public ChickenLine findChickenLine(String chickenLineName)
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


  public void importVcf(String chickenLineName, String filename) throws IOException
  {
    ChickenLine chickenLine = this.findChickenLine(chickenLineName);
    if (chickenLine == null)
    {
      throw new RuntimeException(String.format("no chicken line with name \"%s\"", chickenLineName));
    }
    BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
    for (String vcfLine = r.readLine(); vcfLine != null; vcfLine = r.readLine())
    {
      for (ChickenSnp chickenSnp : ChickenSnp.vcfLineToChickenSnpSet(chickenLine, vcfLine))
      {
        ChickenChromosome chickenChromosome = chickenSnp.getChickenChromosome();
        if (chickenChromosome != null)
        {
          chickenSnp.unlinkChickenChromosome();
          ChickenChromosome persistedChickenChromosome = this.findOrInsertChickenChromosome(chickenChromosome.getName());
          chickenSnp.linkChickenChromosome(persistedChickenChromosome);
        }
        this.entityManager.persist(chickenSnp);
      }
      this.entityManager.flush();
      // System.err.println("flushed");
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


  public void insertChickenSnps(String chickenLineName, Collection<ChickenSnp> chickenSnpCollection)
  {
    ChickenLine chickenLine = this.findChickenLine(chickenLineName);
    if (chickenLine == null)
    {
      throw new RuntimeException(String.format("no chicken line with name \"%s\"", chickenLineName));
    }
    for (ChickenSnp chickenSnp : chickenSnpCollection)
    {
      chickenSnp.linkChickenLine(chickenLine);
      ChickenChromosome chickenChromosome = chickenSnp.getChickenChromosome();
      if (chickenChromosome != null)
      {
        chickenSnp.unlinkChickenChromosome();
        ChickenChromosome persistedChickenChromosome = this.findOrInsertChickenChromosome(chickenChromosome.getName());
        chickenSnp.linkChickenChromosome(persistedChickenChromosome);
      }
      this.entityManager.persist(chickenSnp);
    }
  }


  public ChickenLine insertChickenLine(ChickenLine chickenLine)
  {
    this.entityManager.persist(chickenLine);
    return (chickenLine);
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


  /*
  private void rattleThroughBufferedResultList(Query query)
  {
    BufferedResultList<ChickenSnp> bufferedQuery = new BufferedResultList(query, 7);
    for (ChickenSnp chickenSnp : bufferedQuery)
    {
      ChickenLine chickenLine = chickenSnp.getChickenLine();
      ChickenChromosome chickenChromosome = chickenSnp.getChickenChromosome();
      System.err.println(String.format("from bufferedQuery: %10s  %3s  %8d  %s  %s", chickenLine.getName(), chickenChromosome.getName(), chickenSnp.getPos(), chickenSnp.getRef(), chickenSnp.getAlt()));
    }
  }


  private void rattleThroughQuery(Query query)
  {
    // QueryProvider<ChickenSnp> chickenSnpQueryProvider = new ChickenSnpQueryProvider(chickenLineNameSet);
    int offset = 0;
    // Query query = chickenSnpQueryProvider.getQuery();
    query.setMaxResults(9);
    query.setFirstResult(offset);
    List<ChickenSnp> chickenSnpList = query.getResultList();
    while (chickenSnpList.size() > 0)
    {
      System.err.println(String.format("offset %d", offset));
      for (ChickenSnp chickenSnp : chickenSnpList)
      {
        ChickenLine chickenLine = chickenSnp.getChickenLine();
        ChickenChromosome chickenChromosome = chickenSnp.getChickenChromosome();
        System.err.println(String.format("%10s  %3s  %8d  %s  %s", chickenLine.getName(), chickenChromosome.getName(), chickenSnp.getPos(), chickenSnp.getRef(), chickenSnp.getAlt()));
      }
      System.err.println();
      offset += chickenSnpList.size();
      query.setFirstResult(offset);
      chickenSnpList = query.getResultList();
    }
  }
  */


  public List<ChickenLocus> findDifferentialSnpLocusList(Set<String> chickenLineNameSet1, Set<String> chickenLineNameSet2, GenomicRange genomicRange)
  {
    Set<ChickenLine> chickenLineSet1 = this.findChickenLineSet(chickenLineNameSet1);
    Set<ChickenLine> chickenLineSet2 = this.findChickenLineSet(chickenLineNameSet2);
    Set<ChickenLine> allChickenLineSet = new HashSet<ChickenLine>(chickenLineSet1);
    allChickenLineSet.addAll(chickenLineSet2);
    Set<String> allChickenLineNameSet = new HashSet<String>(chickenLineNameSet1);
    allChickenLineNameSet.addAll(chickenLineNameSet2);
    // System.err.println(String.format("findDifferentialSnpList: total number of line names is %d", allChickenLineNameSet.size()));
    Query query = null;
    if (genomicRange == null)
    {
      query = this.entityManager.createQuery("SELECT chickenSnp FROM ChickenSnp chickenSnp WHERE chickenSnp.chickenLine.name IN ( :nameSet ) ORDER BY chickenSnp.chickenChromosome.name, chickenSnp.pos");
    }
    else
    {
      query = this.entityManager.createQuery("SELECT chickenSnp FROM ChickenSnp chickenSnp WHERE chickenSnp.chickenLine.name IN ( :nameSet ) AND chickenSnp.chickenChromosome.name = :chrom and chickenSnp.pos >= :rangeStart AND chickenSnp.pos <= :rangeEnd ORDER BY chickenSnp.chickenChromosome.name, chickenSnp.pos");
      query.setParameter("chrom", genomicRange.getChrom());
      query.setParameter("rangeStart", genomicRange.getStart());
      query.setParameter("rangeEnd", genomicRange.getEnd());
    }
    query.setParameter("nameSet", allChickenLineNameSet);
    // query.setParameter("nameSet", allChickenLineNameSet.iterator().next());
    // List<ChickenSnp> chickenSnpList = genericTypecast(query.getResultList());
    // System.err.println(String.format("findDifferentialSnpList: got %d SNPs", chickenSnpList.size()));
    BufferedResultList<ChickenSnp> chickenSnpList = new BufferedResultList<ChickenSnp>(query);
    ChickenLocusScanner chickenLocusScanner = new ChickenLocusScanner(chickenSnpList);
    List<ChickenLocus> chickenLocusList = new ArrayList<ChickenLocus>();
    for (ChickenLocus chickenLocus = chickenLocusScanner.nextChickenLocus(); chickenLocus != null; chickenLocus = chickenLocusScanner.nextChickenLocus())
    {
      // System.err.println(String.format("got locus %s:%d", chickenLocus.getChickenChromosome().getName(), chickenLocus.getPos()));
      chickenLocus.addNonSnpLines(allChickenLineSet);
      // System.err.println("added non-SNP lines");
      Set<String> nucleotideSet1 = this.findNucleotideSet(chickenLocus, chickenLineSet1);
      Set<String> nucleotideSet2 = this.findNucleotideSet(chickenLocus, chickenLineSet2);
      // System.err.println("got nucleotide sets");
      nucleotideSet1.retainAll(nucleotideSet2);
      if (nucleotideSet1.size() == 0)
      {
        chickenLocusList.add(chickenLocus);
        // System.err.println(String.format("found locus #%d at %s:%d", chickenLocusList.size(), chickenLocus.getChickenChromosome().getName(), chickenLocus.getPos()));
      }

      else
      {
        // System.err.println(String.format("found non-supporting locus: %s:%d", chickenLocus.getChickenChromosome().getName(), chickenLocus.getPos()));
      }

    }
    return (chickenLocusList);
  }


  public List<ChickenLocus> findDifferentialSnpLocusList(Set<String> chickenLineNameSet1, Set<String> chickenLineNameSet2)
  {
    return (this.findDifferentialSnpLocusList(chickenLineNameSet1, chickenLineNameSet2, null));
  }
}
