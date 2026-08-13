package br.com.mineradorlf.duplasfalha;
import java.util.*;
public class BlackTestV13{
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
   {2,3,4,6,7,9,10,11,14,16,18,19,21,23,25}
  };
  ArrayList<int[]>h=new ArrayList<>();for(int[]r:d)h.add(r);
  CoreEngine.Model m=new CoreEngine.Model(h);

  long c8=(long)CoreEngine.combos(m.lastDraw(),8).size()*CoreEngine.combos(m.lastFail(),7).size();
  long c9=(long)CoreEngine.combos(m.lastDraw(),9).size()*CoreEngine.combos(m.lastFail(),6).size();
  long c10=(long)CoreEngine.combos(m.lastDraw(),10).size()*CoreEngine.combos(m.lastFail(),5).size();

  if(c8!=772200L)throw new RuntimeException("Universo 8 incorreto: "+c8);
  if(c9!=1051050L)throw new RuntimeException("Universo 9 incorreto: "+c9);
  if(c10!=756756L)throw new RuntimeException("Universo 10 incorreto: "+c10);

  int fail=m.lastFail();
  double sc=CoreEngine.failureSetEvolution(m,fail);
  if(Double.isNaN(sc)||Double.isInfinite(sc))throw new RuntimeException("Score evolução inválido");

  System.out.println("BLACKTEST_V13_OK");
  System.out.println("U8="+c8);
  System.out.println("U9="+c9);
  System.out.println("U10="+c10);
  System.out.println("EVOLUTION_SCORE="+sc);
 }
}