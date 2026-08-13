package br.com.mineradorlf.duplasfalha;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public final class CoreEngine {
    private CoreEngine(){}

    public static final int FULL=(1<<25)-1;
    public static final int PRIME=maskOf(new int[]{2,3,5,7,11,13,17,19,23});
    public static final int FIB=maskOf(new int[]{1,2,3,5,8,13,21});
    public static final int MIOLO=maskOf(new int[]{7,8,9,12,13,14,17,18,19});
    public static final int CRUZ=maskOf(new int[]{3,8,11,12,13,14,15,18,23});
    public static final int BORDA=FULL^MIOLO;

    public interface Progress { void update(double pct,String msg); }

    public static int maskOf(int[]a){
        int m=0;for(int n:a)if(n>=1&&n<=25)m|=1<<(n-1);return m;
    }
    public static int[] numsOf(int m){
        int[]a=new int[Integer.bitCount(m)];int k=0;
        for(int n=1;n<=25;n++)if((m&(1<<(n-1)))!=0)a[k++]=n;
        return a;
    }
    public static String fmt(int m){
        StringBuilder s=new StringBuilder();
        for(int n:numsOf(m)){if(s.length()>0)s.append(" ");s.append(String.format(Locale.US,"%02d",n));}
        return s.toString();
    }
    public static int ov(int a,int b){return Integer.bitCount(a&b);}

    public static List<int[]> parseHistory(InputStream in)throws IOException{
        ArrayList<int[]>out=new ArrayList<>();
        BufferedReader br=new BufferedReader(new InputStreamReader(in,"UTF-8"));
        String line;
        while((line=br.readLine())!=null){
            Matcher mt=Pattern.compile("\\d+").matcher(line.replace("\uFEFF",""));
            ArrayList<Integer>v=new ArrayList<>();
            while(mt.find())try{v.add(Integer.parseInt(mt.group()));}catch(Exception ignored){}
            int[]chosen=null;
            for(int st=v.size()-15;st>=0;st--){
                if(st+15>v.size())continue;
                TreeSet<Integer>s=new TreeSet<>();boolean ok=true;
                for(int j=0;j<15;j++){
                    int n=v.get(st+j);
                    if(n<1||n>25||!s.add(n)){ok=false;break;}
                }
                if(ok){
                    chosen=new int[15];int k=0;for(int n:s)chosen[k++]=n;
                    break;
                }
            }
            if(chosen!=null)out.add(chosen);
        }
        if(out.size()<10)throw new IOException("TXT inválido ou com menos de 10 concursos válidos.");
        return out;
    }

    public static final class Model{
        public final int[]draw,fail;
        public final HashSet<Integer>winners=new HashSet<>();
        public Model(List<int[]>h){
            draw=new int[h.size()];fail=new int[h.size()];
            for(int i=0;i<h.size();i++){
                draw[i]=maskOf(h.get(i));
                fail[i]=FULL^draw[i];
                winners.add(draw[i]);
            }
        }
        public int lastDraw(){return draw[draw.length-1];}
        public int lastFail(){return fail[fail.length-1];}
    }

    static void rec(int[]v,int k,int at,int ch,int m,List<Integer>o){
        if(ch==k){o.add(m);return;}
        for(int i=at;i<=v.length-(k-ch);i++)
            rec(v,k,i+1,ch+1,m|(1<<(v[i]-1)),o);
    }
    public static List<Integer> combos(int source,int k){
        ArrayList<Integer>o=new ArrayList<>();
        if(k==0){o.add(0);return o;}
        if(k<0||k>Integer.bitCount(source))return o;
        rec(numsOf(source),k,0,0,0,o);
        return o;
    }

    // =========================================================
    // ESTUDO 1 — TENDÊNCIA DA QUANTIDADE DE REPETIDAS
    // =========================================================
    public static final class RepeatTrend{
        public final int lastRepeat,suggestion;
        public final int[] fullCounts=new int[16];
        public final int[] last100=new int[16];
        public final int[] last50=new int[16];
        public final int[] last20=new int[16];
        public final double confidence;
        RepeatTrend(int l,int s,double c){lastRepeat=l;suggestion=s;confidence=c;}
    }

    public static RepeatTrend repeatTrend(Model m){
        int n=m.draw.length;
        if(n<3)return new RepeatTrend(9,9,0);
        int[] reps=new int[n-1];
        for(int i=1;i<n;i++)reps[i-1]=ov(m.draw[i-1],m.draw[i]);
        int last=reps[reps.length-1];

        RepeatTrend rt=new RepeatTrend(last,9,0);
        int matches=0,m100=0,m50=0,m20=0;
        for(int i=0;i<reps.length-1;i++){
            if(reps[i]!=last)continue;
            int next=reps[i+1];
            if(next>=0&&next<rt.fullCounts.length)rt.fullCounts[next]++;
            matches++;
            int dist=(reps.length-2)-i;
            if(dist<100){rt.last100[next]++;m100++;}
            if(dist<50){rt.last50[next]++;m50++;}
            if(dist<20){rt.last20[next]++;m20++;}
        }

        int best=9;double bestScore=-1;
        for(int r=8;r<=10;r++){
            double pAll=matches==0?0:(double)rt.fullCounts[r]/matches;
            double p100=m100==0?0:(double)rt.last100[r]/m100;
            double p50=m50==0?0:(double)rt.last50[r]/m50;
            double p20=m20==0?0:(double)rt.last20[r]/m20;
            double score=pAll*.20+p100*.20+p50*.25+p20*.35;
            if(score>bestScore){bestScore=score;best=r;}
        }
        RepeatTrend out=new RepeatTrend(last,best,Math.max(0,bestScore));
        System.arraycopy(rt.fullCounts,0,out.fullCounts,0,rt.fullCounts.length);
        System.arraycopy(rt.last100,0,out.last100,0,rt.last100.length);
        System.arraycopy(rt.last50,0,out.last50,0,rt.last50.length);
        System.arraycopy(rt.last20,0,out.last20,0,rt.last20.length);
        return out;
    }

    public static String repeatTrendReport(Model m){
        RepeatTrend r=repeatTrend(m);
        StringBuilder s=new StringBuilder();
        s.append("TENDÊNCIA DA QUANTIDADE DE REPETIDAS\n");
        s.append("Último movimento: ").append(r.lastRepeat).append(" repetidas\n");
        s.append("Sugestão do motor: ").append(r.suggestion).append(" repetidas\n");
        s.append("Confiança estatística relativa: ").append(String.format(Locale.US,"%.1f%%",r.confidence*100)).append("\n\n");
        for(int from:new int[]{8,9,10}){} // mantém leitura simples
        s.append("Quando o movimento anterior foi ").append(r.lastRepeat).append(":\n");
        for(int x=8;x<=10;x++){
            s.append("  ").append(r.lastRepeat).append(" → ").append(x)
             .append(" | histórico=").append(r.fullCounts[x])
             .append(" | ult100=").append(r.last100[x])
             .append(" | ult50=").append(r.last50[x])
             .append(" | ult20=").append(r.last20[x]).append("\n");
        }
        return s.toString();
    }

    // =========================================================
    // ESTUDO 2 — TABELA DE FREQUÊNCIA DE FALHA DAS 25 DEZENAS
    // =========================================================
    public static final class FailureStat{
        public final int dez;
        public final int currentStreak,last20Count,last20Max,totalCount,totalMax;
        public final double last20Pct,last20Mean,totalPct,totalMean,score;
        FailureStat(int d,int cs,int c20,double p20,int m20,double av20,int tc,double tp,int tm,double tav,double sc){
            dez=d;currentStreak=cs;last20Count=c20;last20Pct=p20;last20Max=m20;last20Mean=av20;
            totalCount=tc;totalPct=tp;totalMax=tm;totalMean=tav;score=sc;
        }
    }

    static double meanFailureRuns(Model m,int dez,int start){
        int bit=1<<(dez-1),run=0,runs=0,sum=0;
        for(int i=start;i<m.fail.length;i++){
            if((m.fail[i]&bit)!=0)run++;
            else if(run>0){sum+=run;runs++;run=0;}
        }
        if(run>0){sum+=run;runs++;}
        return runs==0?0:(double)sum/runs;
    }
    static int maxFailureRun(Model m,int dez,int start){
        int bit=1<<(dez-1),run=0,best=0;
        for(int i=start;i<m.fail.length;i++){
            if((m.fail[i]&bit)!=0){run++;best=Math.max(best,run);}else run=0;
        }
        return best;
    }
    static int currentFailureRun(Model m,int dez){
        int bit=1<<(dez-1),run=0;
        for(int i=m.fail.length-1;i>=0;i--){
            if((m.fail[i]&bit)!=0)run++;else break;
        }
        return run;
    }
    public static List<FailureStat> failureFrequency(Model m){
        ArrayList<FailureStat>out=new ArrayList<>();
        int n=m.fail.length,start20=Math.max(0,n-20);
        for(int d=1;d<=25;d++){
            int bit=1<<(d-1),c20=0,total=0;
            for(int i=0;i<n;i++)if((m.fail[i]&bit)!=0){total++;if(i>=start20)c20++;}
            int cur=currentFailureRun(m,d);
            int mx20=maxFailureRun(m,d,start20),mx=maxFailureRun(m,d,0);
            double av20=meanFailureRuns(m,d,start20),av=meanFailureRuns(m,d,0);
            double p20=100.0*c20/Math.max(1,n-start20),pt=100.0*total/n;
            // score focado em FALHA: frequência histórica + recente + persistência atual,
            // sem usar frequência de presença como fator principal.
            double sc=total + c20*14.0 + cur*35.0 + mx20*10.0 + av20*8.0 + pt*2.0;
            out.add(new FailureStat(d,cur,c20,p20,mx20,av20,total,pt,mx,av,sc));
        }
        out.sort((a,b)->Double.compare(b.score,a.score));
        return out;
    }
    public static String failureFrequencyReport(Model m){
        StringBuilder s=new StringBuilder("TABELA DE FREQUÊNCIA DE FALHA — 01 A 25\n\n");
        s.append("Dez | Falha atual | Falhas ult20 | % ult20 | Máx ult20 | Média seq falha ult20 | Total falhas | % total | Máx hist | Média hist\n");
        for(FailureStat f:failureFrequency(m)){
            s.append(String.format(Locale.US,
                "%02d | %d | %d | %.1f%% | %d | %.2f | %d | %.1f%% | %d | %.2f\n",
                f.dez,f.currentStreak,f.last20Count,f.last20Pct,f.last20Max,f.last20Mean,
                f.totalCount,f.totalPct,f.totalMax,f.totalMean));
        }
        return s.toString();
    }

    static double failureNumScore(Model m,int dez){
        for(FailureStat f:failureFrequency(m))if(f.dez==dez)return f.score;
        return 0;
    }


    // =========================================================
    // ESTUDO 3 — PERÍMETRO DE PONTUAÇÃO EM JANELAS DE 10 CONCURSOS
    // =========================================================
    public static final class PerimeterProfile{
        public final int[] pts;
        public final double mean,sd;
        public final int min,max,count12plus,count13plus,count14plus,rises,falls;
        PerimeterProfile(int[]p,double m,double s,int mn,int mx,int c12,int c13,int c14,int r,int f){
            pts=p;mean=m;sd=s;min=mn;max=mx;count12plus=c12;count13plus=c13;count14plus=c14;rises=r;falls=f;
        }
    }

    public static PerimeterProfile perimeterProfile(int game,int[]draw,int endExclusive){
        int start=Math.max(0,endExclusive-10),len=endExclusive-start;
        int[]p=new int[len];double sm=0;int mn=99,mx=-1,c12=0,c13=0,c14=0,r=0,f=0;
        for(int i=0;i<len;i++){
            int x=ov(game,draw[start+i]);p[i]=x;sm+=x;mn=Math.min(mn,x);mx=Math.max(mx,x);
            if(x>=12)c12++;if(x>=13)c13++;if(x>=14)c14++;
            if(i>0){if(p[i]>p[i-1])r++;else if(p[i]<p[i-1])f++;}
        }
        double av=len==0?0:sm/len,ss=0;for(int x:p){double d=x-av;ss+=d*d;}
        return new PerimeterProfile(p,av,len<2?0:Math.sqrt(ss/len),len==0?0:mn,len==0?0:mx,c12,c13,c14,r,f);
    }

    public static final class PerimeterStats{
        public double meanMean,sdMean,meanSd,sdSd,meanMin,sdMin,meanMax,sdMax;
        public double mean12,sd12,mean13,sd13,mean14,sd14,meanRises,sdRises,meanFalls,sdFalls;
    }

    public static PerimeterStats learnPerimeter(Model m){
        int n=m.draw.length,count=Math.max(0,n-10);
        PerimeterStats s=new PerimeterStats();if(count==0)return s;
        double[]aMean=new double[count],aSd=new double[count],aMin=new double[count],aMax=new double[count];
        double[]a12=new double[count],a13=new double[count],a14=new double[count],aR=new double[count],aF=new double[count];
        int k=0;
        for(int target=10;target<n;target++){
            PerimeterProfile p=perimeterProfile(m.draw[target],m.draw,target);
            aMean[k]=p.mean;aSd[k]=p.sd;aMin[k]=p.min;aMax[k]=p.max;a12[k]=p.count12plus;a13[k]=p.count13plus;a14[k]=p.count14plus;aR[k]=p.rises;aF[k]=p.falls;k++;
        }
        s.meanMean=mean(aMean);s.sdMean=sd(aMean,s.meanMean);s.meanSd=mean(aSd);s.sdSd=sd(aSd,s.meanSd);
        s.meanMin=mean(aMin);s.sdMin=sd(aMin,s.meanMin);s.meanMax=mean(aMax);s.sdMax=sd(aMax,s.meanMax);
        s.mean12=mean(a12);s.sd12=sd(a12,s.mean12);s.mean13=mean(a13);s.sd13=sd(a13,s.mean13);
        s.mean14=mean(a14);s.sd14=sd(a14,s.mean14);s.meanRises=mean(aR);s.sdRises=sd(aR,s.meanRises);
        s.meanFalls=mean(aF);s.sdFalls=sd(aF,s.meanFalls);
        return s;
    }

    public static double perimeterScore(Model m,int game){
        if(m.draw.length<11)return 0;
        PerimeterStats s=learnPerimeter(m);PerimeterProfile p=perimeterProfile(game,m.draw,m.draw.length);
        double q=0;
        q+=z(p.mean,s.meanMean,s.sdMean)*1.5+z(p.sd,s.meanSd,s.sdSd)*0.7+z(p.min,s.meanMin,s.sdMin)*0.6+z(p.max,s.meanMax,s.sdMax)*1.1;
        q+=z(p.count12plus,s.mean12,s.sd12)*1.5+z(p.count13plus,s.mean13,s.sd13)*2.0+z(p.count14plus,s.mean14,s.sd14)*3.5;
        q+=z(p.rises,s.meanRises,s.sdRises)*0.4+z(p.falls,s.meanFalls,s.sdFalls)*0.4;
        return 1000.0-70.0*q;
    }

    public static boolean perimeterAccept(Model m,int game){
        if(m.draw.length<11)return true;
        PerimeterStats s=learnPerimeter(m);PerimeterProfile p=perimeterProfile(game,m.draw,m.draw.length);
        if(z(p.mean,s.meanMean,s.sdMean)>1.75)return false;
        if(z(p.max,s.meanMax,s.sdMax)>2.00)return false;
        if(z(p.count12plus,s.mean12,s.sd12)>2.10)return false;
        if(z(p.count13plus,s.mean13,s.sd13)>2.10)return false;
        if(z(p.count14plus,s.mean14,s.sd14)>1.85)return false;
        return true;
    }

    public static String perimeterReport(Model m,int game){
        PerimeterProfile p=perimeterProfile(game,m.draw,m.draw.length);PerimeterStats s=learnPerimeter(m);
        StringBuilder seq=new StringBuilder();for(int x:p.pts){if(seq.length()>0)seq.append(" ");seq.append(x);}
        return "Perímetro últimos 10: "+seq+" | Média="+String.format(Locale.US,"%.2f",p.mean)+
            " | Mín="+p.min+" | Máx="+p.max+" | >=12="+p.count12plus+" | >=13="+p.count13plus+
            " | >=14="+p.count14plus+" | média histórica="+String.format(Locale.US,"%.2f",s.meanMean);
    }

    // =========================================================
    // 300 DUPLAS DO UNIVERSO — MÓDULO 1
    // =========================================================
    public static final class PairStat{
        public final int pair;
        public final double historical,recent,evolution,movement,score;
        PairStat(int p,double h,double r,double e,double m,double s){
            pair=p;historical=h;recent=r;evolution=e;movement=m;score=s;
        }
    }
    static double pairContestScore(int pair,int failMask){
        int x=ov(pair,failMask);
        return x==2?100.0:(x==1?50.0:0.0);
    }
    public static List<PairStat> rankUniversePairs(Model m,Progress p){
        List<Integer>pairs=combos(FULL,2);ArrayList<PairStat>out=new ArrayList<>();
        int n=m.draw.length,done=0;
        for(int pair:pairs){
            double hist=0,r10=0,r30=0;
            for(int i=0;i<n;i++){
                double sc=pairContestScore(pair,m.fail[i]);
                hist+=sc;
                if(i>=n-10)r10+=sc;
                if(i>=n-30)r30+=sc;
            }
            double evo=(r10/10.0)*.60+(r30/30.0)*.40;
            double movement=0;
            if(n>=2)movement=pairContestScore(pair,m.fail[n-1])-pairContestScore(pair,m.fail[n-2]);
            double numLayer=0;
            for(int d:numsOf(pair))numLayer+=failureNumScore(m,d);
            double score=hist+r30*2+r10*5+evo*15+Math.max(0,movement)*8+numLayer*6;
            out.add(new PairStat(pair,hist,r10,evo,movement,score));
            done++;
            if(p!=null&&(done==1||done==pairs.size()||done%10==0))
                p.update(4+done*28.0/pairs.size(),"M1 — ranking das 300 duplas: "+done+"/"+pairs.size());
        }
        out.sort((a,b)->Double.compare(b.score,a.score));
        return out;
    }

    // =========================================================
    // DUPLAS REAIS DAS 10 FALHAS — MÓDULO 2
    // =========================================================
    public static final class RealPairStat{
        public final int pair,fullOccurrences,last20,last6;
        public final double score;
        RealPairStat(int p,int f,int l20,int l6,double s){pair=p;fullOccurrences=f;last20=l20;last6=l6;score=s;}
    }
    public static List<RealPairStat> rankRealFailurePairs(Model m,Progress p){
        HashMap<Integer,int[]>map=new HashMap<>();int n=m.fail.length;
        for(int i=0;i<n;i++){
            for(int pair:combos(m.fail[i],2)){
                int[]a=map.computeIfAbsent(pair,k->new int[3]);
                a[0]++;if(i>=n-20)a[1]++;if(i>=n-6)a[2]++;
            }
            if(p!=null&&(i==0||i==n-1||i%25==0))
                p.update(4+(i+1)*28.0/n,"M2 — concurso "+(i+1)+"/"+n+" | 45 duplas da falha");
        }
        ArrayList<RealPairStat>out=new ArrayList<>();
        for(Map.Entry<Integer,int[]>e:map.entrySet()){
            int[]a=e.getValue();
            double numLayer=0;for(int d:numsOf(e.getKey()))numLayer+=failureNumScore(m,d);
            double sc=a[0]*100+a[1]*250+a[2]*700+numLayer*8;
            out.add(new RealPairStat(e.getKey(),a[0],a[1],a[2],sc));
        }
        out.sort((a,b)->Double.compare(b.score,a.score));
        return out;
    }

    // =========================================================
    // PADRÕES HISTÓRICOS DO JOGO FINAL
    // =========================================================
    static int sumMask(int g){int s=0;for(int n:numsOf(g))s+=n;return s;}
    static int evenCount(int g){int c=0;for(int n:numsOf(g))if(n%2==0)c++;return c;}
    static int rowCount(int g,int row){int c=0;for(int n=1+row*5;n<=5+row*5;n++)if((g&(1<<(n-1)))!=0)c++;return c;}
    static int colCount(int g,int col){int c=0;for(int n=col+1;n<=25;n+=5)if((g&(1<<(n-1)))!=0)c++;return c;}
    static double mean(double[]a){double s=0;for(double x:a)s+=x;return a.length==0?0:s/a.length;}
    static double sd(double[]a,double m){
        if(a.length<2)return 1;
        double s=0;for(double x:a){double d=x-m;s+=d*d;}
        return Math.max(.75,Math.sqrt(s/a.length));
    }
    static double z(double x,double m,double sd){return Math.abs(x-m)/Math.max(.75,sd);}

    public static final class PatternStats{
        public double ms,ss,me,se,mp,sp,mf,sf,mm,sm,mc,sc,mb,sb;
        public final double[]mr=new double[5],sr=new double[5],mco=new double[5],sco=new double[5];
    }
    public static PatternStats learnPatterns(Model m){
        int n=m.draw.length;
        double[]su=new double[n],ev=new double[n],pr=new double[n],fi=new double[n],mi=new double[n],cr=new double[n],bo=new double[n];
        double[][]rs=new double[5][n],cs=new double[5][n];
        for(int i=0;i<n;i++){
            int g=m.draw[i];
            su[i]=sumMask(g);ev[i]=evenCount(g);pr[i]=ov(g,PRIME);fi[i]=ov(g,FIB);
            mi[i]=ov(g,MIOLO);cr[i]=ov(g,CRUZ);bo[i]=ov(g,BORDA);
            for(int r=0;r<5;r++)rs[r][i]=rowCount(g,r);
            for(int c=0;c<5;c++)cs[c][i]=colCount(g,c);
        }
        PatternStats p=new PatternStats();
        p.ms=mean(su);p.ss=sd(su,p.ms);p.me=mean(ev);p.se=sd(ev,p.me);p.mp=mean(pr);p.sp=sd(pr,p.mp);
        p.mf=mean(fi);p.sf=sd(fi,p.mf);p.mm=mean(mi);p.sm=sd(mi,p.mm);p.mc=mean(cr);p.sc=sd(cr,p.mc);p.mb=mean(bo);p.sb=sd(bo,p.mb);
        for(int r=0;r<5;r++){p.mr[r]=mean(rs[r]);p.sr[r]=sd(rs[r],p.mr[r]);}
        for(int c=0;c<5;c++){p.mco[c]=mean(cs[c]);p.sco[c]=sd(cs[c],p.mco[c]);}
        return p;
    }
    public static boolean validRepeats(Model m,int game){
        int r=ov(game,m.lastDraw());
        return r==8||r==9||r==10;
    }
    static boolean patternAccept(Model m,PatternStats p,int g){
        if(!validRepeats(m,g)||m.winners.contains(g))return false;
        if(z(sumMask(g),p.ms,p.ss)>1.45||z(evenCount(g),p.me,p.se)>1.70||
           z(ov(g,PRIME),p.mp,p.sp)>1.80||z(ov(g,FIB),p.mf,p.sf)>1.80||
           z(ov(g,MIOLO),p.mm,p.sm)>1.80||z(ov(g,CRUZ),p.mc,p.sc)>1.80||
           z(ov(g,BORDA),p.mb,p.sb)>1.80)return false;
        for(int r=0;r<5;r++)if(z(rowCount(g,r),p.mr[r],p.sr[r])>2.10)return false;
        for(int c=0;c<5;c++)if(z(colCount(g,c),p.mco[c],p.sco[c])>2.10)return false;
        return true;
    }
    static double patternFit(PatternStats p,int g){
        double q=z(sumMask(g),p.ms,p.ss)+z(evenCount(g),p.me,p.se)+z(ov(g,PRIME),p.mp,p.sp)+
                 z(ov(g,FIB),p.mf,p.sf)+z(ov(g,MIOLO),p.mm,p.sm)+z(ov(g,CRUZ),p.mc,p.sc)+z(ov(g,BORDA),p.mb,p.sb);
        for(int r=0;r<5;r++)q+=.6*z(rowCount(g,r),p.mr[r],p.sr[r]);
        for(int c=0;c<5;c++)q+=.6*z(colCount(g,c),p.mco[c],p.sco[c]);
        return 1000-60*q;
    }
    public static String patternReport(Model m,int g){
        PatternStats p=learnPatterns(m);
        return "Repetidas="+ov(g,m.lastDraw())+
            " | Soma="+sumMask(g)+" (média "+String.format(Locale.US,"%.1f",p.ms)+")"+
            " | Pares="+evenCount(g)+" (média "+String.format(Locale.US,"%.1f",p.me)+")"+
            " | Primos="+ov(g,PRIME)+" (média "+String.format(Locale.US,"%.1f",p.mp)+")"+
            " | Fibonacci="+ov(g,FIB)+" (média "+String.format(Locale.US,"%.1f",p.mf)+")"+
            " | Miolo="+ov(g,MIOLO)+" (média "+String.format(Locale.US,"%.1f",p.mm)+")"+
            " | Cruz="+ov(g,CRUZ)+" (média "+String.format(Locale.US,"%.1f",p.mc)+")"+
            " | Borda="+ov(g,BORDA)+" (média "+String.format(Locale.US,"%.1f",p.mb)+")";
    }

    static double pairSupportUniverse(List<PairStat>rank,int f){
        double s=0;int u=0;
        for(PairStat x:rank)if((x.pair&f)==x.pair){s+=x.score;if(++u>=14)break;}
        return u==0?0:s/u;
    }
    static double pairSupportReal(List<RealPairStat>rank,int f){
        double s=0;int u=0;
        for(RealPairStat x:rank)if((x.pair&f)==x.pair){s+=x.score;if(++u>=14)break;}
        return u==0?0:s/u;
    }
    static int sourceTopFailure(Model m,int topN){
        List<FailureStat>fs=failureFrequency(m);int s=0;
        for(int i=0;i<Math.min(topN,fs.size());i++)s|=1<<(fs.get(i).dez-1);
        return s;
    }
    static double failureSetScore(Model m,int f){
        double s=0;for(int d:numsOf(f))s+=failureNumScore(m,d);return s;
    }

    static int buildU(Model m,List<PairStat>rank,Progress p){
        PatternStats ps=learnPatterns(m);RepeatTrend rt=repeatTrend(m);
        double best=-1e99;int bm=0;int[]sizes={18,20,22};
        for(int size:sizes){
            List<Integer>cs=combos(sourceTopFailure(m,size),10);int d=0;
            for(int f:cs){
                int g=FULL^f;
                if(patternAccept(m,ps,g) && perimeterAccept(m,g)){
                    double bonus=(ov(g,m.lastDraw())==rt.suggestion)?250000.0:0;
                    double sc=pairSupportUniverse(rank,f)+failureSetScore(m,f)*20+patternFit(ps,g)*20+perimeterScore(m,g)*18+bonus;
                    if(sc>best){best=sc;bm=f;}
                }
                d++;
                if(p!=null&&(d==1||d==cs.size()||d%300==0))
                    p.update(34+d*60.0/Math.max(1,cs.size()),"M1 — peneira "+size+" dezenas: "+d+"/"+cs.size());
            }
            if(bm!=0)break;
        }
        if(bm==0)throw new IllegalStateException("Nenhum candidato encontrou filtro 8/9/10 + padrão histórico.");
        return bm;
    }

    static int buildR(Model m,List<RealPairStat>rank,List<PairStat>univ,Progress p){
        PatternStats ps=learnPatterns(m);RepeatTrend rt=repeatTrend(m);
        double best=-1e99;int bm=0;int[]sizes={18,20,22};
        for(int size:sizes){
            List<Integer>cs=combos(sourceTopFailure(m,size),10);int d=0;
            for(int f:cs){
                int g=FULL^f;
                if(patternAccept(m,ps,g) && perimeterAccept(m,g)){
                    double bonus=(ov(g,m.lastDraw())==rt.suggestion)?250000.0:0;
                    double sc=pairSupportReal(rank,f)+pairSupportUniverse(univ,f)*.25+failureSetScore(m,f)*20+patternFit(ps,g)*20+perimeterScore(m,g)*18+bonus;
                    if(sc>best){best=sc;bm=f;}
                }
                d++;
                if(p!=null&&(d==1||d==cs.size()||d%300==0))
                    p.update(34+d*60.0/Math.max(1,cs.size()),"M2 — peneira "+size+" dezenas: "+d+"/"+cs.size());
            }
            if(bm!=0)break;
        }
        if(bm==0)throw new IllegalStateException("Nenhum candidato encontrou filtro 8/9/10 + padrão histórico.");
        return bm;
    }

    public static final class ModuleResult{
        public final int failure10,game15;
        public final String report;
        ModuleResult(int f,String r){failure10=f;game15=FULL^f;report=r;}
    }

    public static ModuleResult module1(Model m,Progress p){
        List<PairStat>rank=rankUniversePairs(m,p);
        int f=buildU(m,rank,p),g=FULL^f;
        StringBuilder s=new StringBuilder();
        s.append("MÓDULO 1 — 300 DUPLAS DO UNIVERSO\n\n");
        s.append("FALHAS PROJETADAS (10): ").append(fmt(f)).append("\n");
        s.append("JOGO (15): ").append(fmt(g)).append("\n\n");
        s.append(repeatTrendReport(m)).append("\n");
        s.append("PADRÕES DO JOGO\n").append(patternReport(m,g)).append("\n");
        s.append(perimeterReport(m,g)).append("\n\n");
        s.append("JUSTIFICATIVA\n");
        s.append("O ranking principal foi construído pelo lado da FALHA: score das 300 duplas, frequência de falha das dezenas, evolução recente e padrão histórico. ");
        s.append("O jogo só foi aceito com 8, 9 ou 10 repetidas e dentro do envelope histórico dos padrões.\n\n");
        s.append("TOP 15 DUPLAS UNIVERSAIS\n");
        for(int i=0;i<Math.min(15,rank.size());i++){
            PairStat x=rank.get(i);
            s.append(i+1).append(") ").append(fmt(x.pair))
             .append(" | hist=").append((int)x.historical)
             .append(" | ult10=").append((int)x.recent)
             .append(" | movimento=").append((int)x.movement)
             .append(" | score=").append(String.format(Locale.US,"%.1f",x.score)).append("\n");
        }
        if(p!=null)p.update(100,"Módulo 1 concluído");
        return new ModuleResult(f,s.toString());
    }

    public static ModuleResult module2(Model m,Progress p){
        List<RealPairStat>rank=rankRealFailurePairs(m,p);
        List<PairStat>univ=rankUniversePairs(m,null);
        int f=buildR(m,rank,univ,p),g=FULL^f;
        StringBuilder s=new StringBuilder();
        s.append("MÓDULO 2 — DUPLAS REAIS DAS 10 FALHAS DE CADA CONCURSO\n\n");
        s.append("FALHAS PROJETADAS (10): ").append(fmt(f)).append("\n");
        s.append("JOGO (15): ").append(fmt(g)).append("\n\n");
        s.append(repeatTrendReport(m)).append("\n");
        s.append("PADRÕES DO JOGO\n").append(patternReport(m,g)).append("\n");
        s.append(perimeterReport(m,g)).append("\n\n");
        s.append("JUSTIFICATIVA\n");
        s.append("Este módulo usa somente as 45 duplas que realmente existiram dentro das 10 falhas de cada concurso, acumulando frequência e evolução. ");
        s.append("Depois cruza a força das dezenas em falha e só aceita 8, 9 ou 10 repetidas dentro do padrão histórico.\n\n");
        s.append("TOP 15 DUPLAS REAIS DE FALHA\n");
        for(int i=0;i<Math.min(15,rank.size());i++){
            RealPairStat x=rank.get(i);
            s.append(i+1).append(") ").append(fmt(x.pair))
             .append(" | falhou junta=").append(x.fullOccurrences)
             .append(" | ult20=").append(x.last20)
             .append(" | ult6=").append(x.last6)
             .append(" | score=").append(String.format(Locale.US,"%.1f",x.score)).append("\n");
        }
        if(p!=null)p.update(100,"Módulo 2 concluído");
        return new ModuleResult(f,s.toString());
    }

    // =========================================================
    // MÓDULO 3 — UNIVERSO 8 / 9 / 10 REPETIDAS
    // Varre cada universo bruto e escolhe a falha de 10 mais evoluída
    // entre candidatos dentro do padrão histórico.
    // =========================================================
    static double failureSetEvolution(Model m,int failure10){
        int n=m.fail.length;
        double score=0;
        int full=0,full20=0,full6=0,seq=0,maxSeq=0;
        for(int i=0;i<n;i++){
            int hit=ov(failure10,m.fail[i]);
            if(hit==10){
                full++;seq++;maxSeq=Math.max(maxSeq,seq);
                if(i>=n-20)full20++;
                if(i>=n-6)full6++;
                score+=1200;
            }else{
                seq=0;
                score+=hit*18;
            }
            if(i>=2){
                int a=ov(failure10,m.fail[i-2]);
                int b=ov(failure10,m.fail[i-1]);
                int c=ov(failure10,m.fail[i]);
                if(c>b && b>=a)score+=80*(c-b);
            }
        }
        score+=full*250+full20*900+full6*1800+maxSeq*600;
        for(int d:numsOf(failure10))score+=failureNumScore(m,d)*12;
        return score;
    }

    public static final class UniverseResult{
        public final int repeats,failure10,game15;
        public final long totalUniverse,insidePattern;
        public final double score;
        public final String report;
        UniverseResult(int r,int f,long t,long in,double s,String rep){
            repeats=r;failure10=f;game15=FULL^f;totalUniverse=t;insidePattern=in;score=s;report=rep;
        }
    }

    public static UniverseResult module3ForRepeat(Model m,int repeats,Progress p,double pctStart,double pctEnd){
        if(repeats<8||repeats>10)throw new IllegalArgumentException("Repetidas deve ser 8, 9 ou 10.");
        PatternStats ps=learnPatterns(m);

        List<Integer>keep=combos(m.lastDraw(),repeats);
        List<Integer>enter=combos(m.lastFail(),15-repeats);
        long total=(long)keep.size()*(long)enter.size();

        double best=-1e100;int bestGame=0,bestFail=0;
        long done=0,inside=0,lastEmit=0;

        for(int a:keep){
            for(int b:enter){
                int game=a|b;
                if(Integer.bitCount(game)==15 && ov(game,m.lastDraw())==repeats && patternAccept(m,ps,game) && perimeterAccept(m,game)){
                    inside++;
                    int fail=FULL^game;
                    double sc=failureSetEvolution(m,fail)+patternFit(ps,game)*20.0+perimeterScore(m,game)*22.0;
                    if(sc>best){best=sc;bestGame=game;bestFail=fail;}
                }
                done++;
                if(p!=null && (done==1 || done==total || done-lastEmit>=5000)){
                    double pct=pctStart+((double)done*(pctEnd-pctStart)/Math.max(1L,total));
                    p.update(pct,"M3 — "+repeats+" repetidas — jogo "+done+"/"+total+" | dentro padrão="+inside);
                    lastEmit=done;
                }
            }
        }

        if(bestGame==0)throw new IllegalStateException("Nenhum candidato válido em "+repeats+" repetidas.");

        String report=
            "MÓDULO 3 — UNIVERSO "+repeats+" REPETIDAS\n\n"+
            "UNIVERSO BRUTO: "+total+" jogos\n"+
            "DENTRO DO PADRÃO HISTÓRICO: "+inside+" jogos\n\n"+
            "FALHA MAIS EVOLUÍDA (10):\n"+fmt(bestFail)+"\n\n"+
            "JOGO PADRONIZADO (15):\n"+fmt(bestGame)+"\n\n"+
            "PADRÕES DO JOGO:\n"+patternReport(m,bestGame)+"\n"+
            perimeterReport(m,bestGame)+"\n\n"+
            "SCORE DE EVOLUÇÃO DA FALHA: "+String.format(Locale.US,"%.2f",best)+"\n\n"+
            "JUSTIFICATIVA:\n"+
            "O motor percorreu todo o universo de jogos com exatamente "+repeats+" repetidas do último resultado, "+
            "descartou candidatos fora do padrão histórico e fora do perímetro histórico dos últimos 10 concursos, "+
            "e escolheu a falha de 10 com maior evolução histórica/recente.";

        return new UniverseResult(repeats,bestFail,total,inside,best,report);
    }

    public static List<UniverseResult> module3(Model m,Progress p){
        ArrayList<UniverseResult>out=new ArrayList<>();
        out.add(module3ForRepeat(m,8,p,2.0,34.0));
        out.add(module3ForRepeat(m,9,p,34.0,67.0));
        out.add(module3ForRepeat(m,10,p,67.0,99.0));
        if(p!=null)p.update(100.0,"Módulo 3 concluído — 3 universos analisados");
        return out;
    }

}
