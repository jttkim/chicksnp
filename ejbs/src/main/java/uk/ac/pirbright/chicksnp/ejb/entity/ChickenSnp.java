package uk.ac.pirbright.chicksnp.ejb.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Version;
import javax.persistence.OneToOne;
import javax.persistence.OneToMany;
import javax.persistence.ManyToOne;
import javax.persistence.ManyToMany;
import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;


@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"chickenLine_id", "chickenchromosome_id", "pos",  "alt"}))
public class ChickenSnp implements Serializable
{
  private static final long serialVersionUID = 1;

  private Integer id;
  private int pos;
  private String ref;
  private String alt;
  private String dbSnpId;
  private ChickenLine chickenLine;
  private ChickenChromosome chickenChromosome;


  public ChickenSnp()
  {
    super();
  }


  public ChickenSnp(ChickenLine chickenLine, ChickenChromosome chickenChromosome, int pos, String ref, String alt, String dbSnpId)
  {
    this();
    this.linkChickenLine(chickenLine);
    this.linkChickenChromosome(chickenChromosome);
    this.pos = pos;
    this.ref = ref;
    this.alt = alt;
    this.dbSnpId = dbSnpId;
  }


  public ChickenSnp(ChickenLine chickenLine, ChickenChromosome chickenChromosome, int pos, String ref, String alt)
  {
    this(chickenLine, chickenChromosome, pos, ref, alt, null);
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


  @Column(nullable = false)
  public int getPos()
  {
    return (this.pos);
  }


  public void setPos(int pos)
  {
    this.pos = pos;
  }


  @Column(length = 1, nullable = false)
  public String getRef()
  {
    return (this.ref);
  }


  public void setRef(String ref)
  {
    this.ref = ref;
  }


  @Column(length = 1, nullable = false)
  public String getAlt()
  {
    return (this.alt);
  }


  public void setAlt(String alt)
  {
    this.alt = alt;
  }


  @Column(length = 16)
  public String getDbSnpId()
  {
    return (this.dbSnpId);
  }


  public void setDbSnpId(String dbSnpId)
  {
    this.dbSnpId = dbSnpId;
  }


  @ManyToOne(optional = false)
  public ChickenLine getChickenLine()
  {
    return (this.chickenLine);
  }


  public void setChickenLine(ChickenLine chickenLine)
  {
    this.chickenLine = chickenLine;
  }


  public void linkChickenLine(ChickenLine chickenLine)
  {
    this.chickenLine = chickenLine;
    chickenLine.getChickenSnpSet().add(this);
  }


  public boolean unlinkChickenLine()
  {
    if (this.chickenLine == null)
    {
      return (false);
    }
    if (!this.chickenLine.getChickenSnpSet().remove(this))
    {
      return (false);
    }
    this.chickenLine = null;
    return (true);
  }


  @ManyToOne(optional = false)
  public ChickenChromosome getChickenChromosome()
  {
    return (this.chickenChromosome);
  }


  public void setChickenChromosome(ChickenChromosome chickenChromosome)
  {
    this.chickenChromosome = chickenChromosome;
  }


  public void linkChickenChromosome(ChickenChromosome chickenChromosome)
  {
    this.chickenChromosome = chickenChromosome;
    chickenChromosome.getChickenSnpSet().add(this);
  }


  public boolean unlinkChickenChromosome()
  {
    if (this.chickenChromosome == null)
    {
      return (false);
    }
    if (!this.chickenChromosome.getChickenSnpSet().remove(this))
    {
      return (false);
    }
    this.chickenChromosome = null;
    return (true);
  }


  public boolean atSameLocus(ChickenSnp other)
  {
    return (this.chickenChromosome.equals(other.chickenChromosome) && (this.pos == other.pos));
  }


  @Override
  public String toString()
  {
    String s = String.format("%s:%d, %s: %s -> %s", this.chickenChromosome.getName(), this.pos, this.chickenLine.getName(), this.ref, this.alt);
    return (s);
  }
}
