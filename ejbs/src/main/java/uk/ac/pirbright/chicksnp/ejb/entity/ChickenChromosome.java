package uk.ac.pirbright.chicksnp.ejb.entity;

import java.io.Serializable;

import java.util.Set;
import java.util.HashSet;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Version;
import javax.persistence.OneToOne;
import javax.persistence.OneToMany;
import javax.persistence.ManyToOne;
import javax.persistence.ManyToMany;
import javax.persistence.Column;


@Entity
public class ChickenChromosome implements Serializable
{
  private static final long serialVersionUID = 1;

  private Integer id;
  private String name;
  private Set<ChickenSnp> chickenSnpSet;


  public ChickenChromosome()
  {
    this.chickenSnpSet = new HashSet<ChickenSnp>();
  }


  public ChickenChromosome(String name)
  {
    this();
    this.name = name;
  }


  @Override
  public boolean equals(Object otherObject)
  {
    if (!(otherObject instanceof ChickenChromosome))
    {
      return (false);
    }
    ChickenChromosome other = (ChickenChromosome) otherObject;
    if ((this.id == null) || (other.id == null))
    {
      System.err.println("equals called for ChickenChromosome instance(s) with null ID");
      return (false);
    }
    return (this.id.equals(other.id));
  }


  @Override
  public int hashCode()
  {
    if (this.id == null)
    {
      return (0);
    }
    else
    {
      return (this.id.intValue());
    }
  }


  @Id
  @GeneratedValue
  public Integer getId()
  {
    return (this.id);
  }


  public void setId(Integer id)
  {
    this.id = id;
  }


  @Column(unique = true)
  public String getName()
  {
    return (this.name);
  }


  public void setName(String name)
  {
    this.name = name;
  }


  @OneToMany(mappedBy = "chickenChromosome")
  public Set<ChickenSnp> getChickenSnpSet()
  {
    return (this.chickenSnpSet);
  }


  public void setChickenSnpSet(Set<ChickenSnp> chickenSnpSet)
  {
    this.chickenSnpSet = chickenSnpSet;
  }


  public void linkChickenSnp(ChickenSnp chickenSnp)
  {
    this.chickenSnpSet.add(chickenSnp);
    chickenSnp.setChickenChromosome(this);
  }


  public boolean unlinkChickenSnp(ChickenSnp chickenSnp)
  {
    if (!this.chickenSnpSet.remove(chickenSnp))
    {
      return (false);
    }
    chickenSnp.setChickenChromosome(null);
    return (true);
  }
}
