package br.com.mineradorlf.duplasfalha;
import java.util.*;
public class BlackTestV12{
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

  if(CoreEngine.combos(CoreEngine.FULL,2).size()!=300)throw new RuntimeException("C25,2");
  if(CoreEngine.combos(m.lastFail(),2).size()!=45)throw new RuntimeException("C10,2");

  java.util.List<CoreEngine.FailureStat> fs=CoreEngine.failureFrequency(m);
  if(fs.size()!=25)throw new RuntimeException("Tabela falha não tem 25 dezenas");
  for(CoreEngine.FailureStat f:fs){
   if(f.last20Pct<0||f.last20Pct>100||f.totalPct<0||f.totalPct>100)throw new RuntimeException("Percentual inválido");
  }

  CoreEngine.RepeatTrend rt=CoreEngine.repeatTrend(m);
  if(rt.suggestion<8||rt.suggestion>10)throw new RuntimeException("Tendência fora de 8/9/10");

  int keep=CoreEngine.combos(m.lastDraw(),9).get(0);
  int enter=CoreEngine.combos(m.lastFail(),6).get(0);
  int g=keep|enter;
  if(!CoreEngine.validRepeats(m,g))throw new RuntimeException("Filtro 8/9/10");

  String fr=CoreEngine.failureFrequencyReport(m);
  if(!fr.contains("TABELA DE FREQUÊNCIA DE FALHA"))throw new RuntimeException("Relatório falha");
  String tr=CoreEngine.repeatTrendReport(m);
  if(!tr.contains("TENDÊNCIA DA QUANTIDADE DE REPETIDAS"))throw new RuntimeException("Relatório tendência");

  System.out.println("BLACKTEST_V12_OK");
  System.out.println("C25_2=300");
  System.out.println("C10_2=45");
  System.out.println("FAILURE_TABLE=25");
  System.out.println("TREND="+rt.suggestion);
  System.out.println("REPETIDAS_TESTE="+CoreEngine.ov(g,m.lastDraw()));
 }
}