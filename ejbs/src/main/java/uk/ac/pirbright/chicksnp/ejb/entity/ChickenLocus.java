package uk.ac.pirbright.chicksnp.ejb.entity;

import java.io.Serializable;

import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Version;
import javax.persistence.OneToOne;
import javax.persistence.OneToMany;
import javax.persistence.ManyToOne;
import javax.persistence.ManyToMany;
import javax.persistence.Column;


// FIXME: not an entity but in entity package -- because this could become an entity after further normalisation
public class ChickenLocus implements Serializable
{
  private static final long serialVersionUID = 1;

  private ChickenChromosome chickenChromosome;
  private int pos;
  private String ref;
  private Map<String, Set<ChickenSnp>> chickenSnpMap;


  public ChickenLocus()
  {
    super();
    this.chickenSnpMap = new HashMap<String, Set<ChickenSnp>>();
  }


  public ChickenLocus(ChickenChromosome chickenChromosome, int pos, String ref)
  {
    this();
    this.chickenChromosome = chickenChromosome;
    this.pos = pos;
    this.ref = ref;
  }


  public ChickenLocus(ChickenSnp chickenSnp)
  {
    this(chickenSnp.getChickenChromosome(), chickenSnp.getPos(), chickenSnp.getRef());
    this.addChickenSnp(chickenSnp);
  }


  public ChickenChromosome getChickenChromosome()
  {
    return (this.chickenChromosome);
  }


  public void setChickenChromosome(ChickenChromosome chickenChromosome)
  {
    this.chickenChromosome = chickenChromosome;
  }


  public int getPos()
  {
    return (this.pos);
  }


  public void setPos(int pos)
  {
    this.pos = pos;
  }


  public Map<String, Set<ChickenSnp>> getChickenSnpMap()
  {
    return (this.chickenSnpMap);
  }


  public void setChickenSnpMap(Map<String, Set<ChickenSnp>> chickenSnpMap)
  {
    this.chickenSnpMap = chickenSnpMap;
  }


  public Set<ChickenSnp> getChickenSnpSet()
  {
    Set<ChickenSnp> chickenSnpSet = new HashSet<ChickenSnp>();
    for (Set<ChickenSnp> chickenLineSnpSet : this.chickenSnpMap.values())
    {
      chickenSnpSet.addAll(chickenLineSnpSet);
    }
    return (chickenSnpSet);
  }


  public void addChickenSnp(ChickenSnp chickenSnp)
  {
    // FIXME: could check that added snp is indeed at same locus
    String chickenLineName = chickenSnp.getChickenLine().getName();
    if (!this.chickenSnpMap.containsKey(chickenLineName))
    {
      this.chickenSnpMap.put(chickenLineName, new HashSet<ChickenSnp>());
    }
    this.chickenSnpMap.get(chickenLineName).add(chickenSnp);
  }


  public void addNonSnpLines(Set<ChickenLine> chickenLineSet)
  {
    for (ChickenLine chickenLine : chickenLineSet)
    {
      Set<ChickenSnp> chickenLineSnpSet = this.chickenSnpMap.get(chickenLine.getName());
      if (chickenLineSnpSet == null)
      {
        chickenLineSnpSet = new HashSet<ChickenSnp>();
        chickenLineSnpSet.add(new ChickenSnp(chickenLine, this.chickenChromosome, this.pos, this.ref, this.ref));
        this.chickenSnpMap.put(chickenLine.getName(), chickenLineSnpSet);
      }
    }
  }


  public Set<ChickenSnp> findChickenLineSnpSet(ChickenLine chickenLine)
  {
    Set<ChickenSnp> chickenLineSnpSet = this.chickenSnpMap.get(chickenLine.getName());
    if (chickenLineSnpSet == null)
    {
      throw new IllegalArgumentException(String.format("no chicken line with name \"%s\"", chickenLine.getName()));
    }
    return (chickenLineSnpSet);
  }


  public boolean atLocus(ChickenSnp chickenSnp)
  {
    return (this.chickenChromosome.equals(chickenSnp.getChickenChromosome()) && (this.pos == chickenSnp.getPos()));
  }


  private static String nucleotideSetString(Set<ChickenSnp> chickenSnpSet)
  {
    List<String> nucleotideList = new ArrayList<String>();
    for (ChickenSnp chickenSnp : chickenSnpSet)
    {
      nucleotideList.add(chickenSnp.getAlt());
    }
    Collections.sort(nucleotideList);
    StringBuilder sb = new StringBuilder();
    String glue = "";
    for (String nucleotide : nucleotideList)
    {
      sb.append(glue).append(nucleotide);
      glue = ", ";
    }
    return (sb.toString());
  }


  public String toString(List<String> lineNameList)
  {
    StringBuilder sb = new StringBuilder(String.format("%s:%d (%s):", this.chickenChromosome.getName(), this.pos, this.ref));
    for (String lineName : lineNameList)
    {
      Set<ChickenSnp> chickenSnpSet = this.chickenSnpMap.get(lineName);
      if (chickenSnpSet == null)
      {
        sb.append(String.format(" %s: NA", lineName));
      }
      else
      {
        sb.append(String.format(" %s: %s;", lineName, nucleotideSetString(chickenSnpSet)));
      }
    }
    return (sb.toString());
  }


  @Override
  public String toString()
  {
    List<String> lineNameList = new ArrayList<String>(this.chickenSnpMap.keySet());
    Collections.sort(lineNameList);
    return (this.toString(lineNameList));
  }
}
