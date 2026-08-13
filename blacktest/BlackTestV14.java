package br.com.mineradorlf.duplasfalha;
import java.util.*;
public class BlackTestV14{
 public static void main(String[]a)throws Exception{
  int[][]d={
   {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15},
   {1,2,3,4,5,6,7,8,9,10,16,17,18,19,20},
   {1,2,3,4,5,11,12,13,14,15,21,22,23,24,25},
   {1,2,3,4,5,6,7,8,9,10,21,22,23,24,25},
   {1,3,5,7,9,11,13,15,17,19,21,22,23,24,25},
   {2,4,6,8,10,12,14,16,18,20,21,22,23,24,25},
   {1,2,4,5,7,8,10,11,13,14,16,18,20,22,24},
   {2,3,5,6,8,9,11,12,14,15,17,19,21,23,25},
   {1,3,4,6,7,9,10,12,13,15,16,18,20,22,24},
   {2,4,5,7,8,10,11,13,14,16,17,19,21,23,25},
   {1,2,3,5,6,8,9,12,13,15,17,18,20,22,24},
   {2,3,4,6,7,9,10,11,14,16,18,19,21,23,25},
   {1,2,4,6,8,9,10,12,14,15,17,19,20,22,25},
   {1,3,5,6,7,10,11,13,14,16,18,20,21,23,24},
   {2,4,5,7,8,9,12,13,15,16,17,19,21,22,25}
  };
  ArrayList<int[]>h=new ArrayList<>();for(int[]r:d)h.add(r);
  CoreEngine.Model m=new CoreEngine.Model(h);

  long c8=(long)CoreEngine.combos(m.lastDraw(),8).size()*CoreEngine.combos(m.lastFail(),7).size();
  long c9=(long)CoreEngine.combos(m.lastDraw(),9).size()*CoreEngine.combos(m.lastFail(),6).size();
  long c10=(long)CoreEngine.combos(m.lastDraw(),10).size()*CoreEngine.combos(m.lastFail(),5).size();
  if(c8!=772200L||c9!=1051050L||c10!=756756L)throw new RuntimeException("universos");

  CoreEngine.PerimeterProfile pp=CoreEngine.perimeterProfile(m.lastDraw(),m.draw,m.draw.length);
  if(pp.pts.length!=10)throw new RuntimeException("janela perímetro");
  double score=CoreEngine.perimeterScore(m,m.lastDraw());
  if(Double.isNaN(score)||Double.isInfinite(score))throw new RuntimeException("score perímetro");
  String rep=CoreEngine.perimeterReport(m,m.lastDraw());
  if(!rep.contains("Perímetro últimos 10"))throw new RuntimeException("relatório perímetro");

  System.out.println("BLACKTEST_V14_OK");
  System.out.println("U8="+c8+" U9="+c9+" U10="+c10);
  System.out.println("PERIMETER_LEN="+pp.pts.length);
  System.out.println("PERIMETER_MAX="+pp.max);
  System.out.println("PERIMETER_SCORE="+score);
  System.out.println(rep);
 }
}