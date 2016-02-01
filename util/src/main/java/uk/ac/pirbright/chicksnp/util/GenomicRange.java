package uk.ac.pirbright.chicksnp.util;


public class GenomicRange
{
  private String chrom;
  private int start;
  private int end;


  public GenomicRange(String chrom, int start, int end)
  {
    this.chrom = chrom;
    this.start = start;
    this.end = end;
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
}
