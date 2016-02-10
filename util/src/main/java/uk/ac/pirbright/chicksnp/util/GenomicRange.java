package uk.ac.pirbright.chicksnp.util;

import java.io.Serializable;


public class GenomicRange implements Serializable
{
  private static final long serialVersionUID = 1;

  private String chrom;
  private int start;
  private int end;


  public GenomicRange(String chrom, int start, int end)
  {
    this.chrom = chrom;
    this.start = start;
    this.end = end;
  }


  public GenomicRange(String s)
  {
    String[] chromRange = s.split(":");
    if (chromRange.length != 2)
    {
      throw new IllegalArgumentException(String.format("malformed genomic range (no chrom:): %s", s));
    }
    String[] startEnd = chromRange[1].split("-");
    if (startEnd.length != 2)
    {
      throw new IllegalArgumentException(String.format("malformed genomic range (no start-end): %s", s));
    }
    this.chrom = chromRange[0].trim();
    this.start = Integer.parseInt(startEnd[0].replaceAll(",", "").trim());
    this.end = Integer.parseInt(startEnd[1].replaceAll(",", "").trim());
  }


  public String getChrom()
  {
    return (this.chrom);
  }


  public void setChrom(String chrom)
  {
    this.chrom = chrom;
  }


  public int getStart()
  {
    return (this.start);
  }


  public void setStart(int start)
  {
    this.start = start;
  }


  public int getEnd()
  {
    return (this.end);
  }


  public void setEnd(int end)
  {
    this.end = end;
  }


  @Override
  public String toString()
  {
    return (String.format("%s:%d-%d", this.chrom, this.start, this.end));
  }
}
