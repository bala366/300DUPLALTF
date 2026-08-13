package br.com.mineradorlf.duplasfalha;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity{
    final ExecutorService ex=Executors.newSingleThreadExecutor();
    final Handler handler=new Handler(Looper.getMainLooper());
    CoreEngine.Model model;
    CoreEngine.ModuleResult lastResult;
    TextView status,clock,trendView,out;
    ProgressBar bar;
    Button m1,m2,m3,failTableBtn;
    LinearLayout actions;
    long startedAt=0;double pct=0;boolean running=false;
    int purple=0xFF8E44AD;

    int dp(int x){return(int)(x*getResources().getDisplayMetrics().density+.5f);}
    TextView tv(String s,int z,boolean b){
        TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setPadding(dp(10),dp(8),dp(10),dp(8));
        if(b)v.setTypeface(Typeface.DEFAULT_BOLD);return v;
    }
    Button bt(String s){
        Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackgroundColor(purple);return b;
    }

    public void onCreate(Bundle x){
        super.onCreate(x);
        ScrollView sv=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14),dp(12),dp(14),dp(40));sv.addView(root);

        TextView hd=tv("MINERADOR LOTOFÁCIL — DUPLAS DE FALHA",23,true);
        hd.setTextColor(Color.WHITE);hd.setBackgroundColor(purple);root.addView(hd);
        root.addView(tv("3 módulos • falha • tendência 8/9/10 • padrões • perímetro últimos 10 • PDF visual",13,true));

        Button load=bt("CARREGAR NOVO TXT — RESETAR E ANALISAR");root.addView(load,new LinearLayout.LayoutParams(-1,dp(62)));
        load.setOnClickListener(v->pick());

        status=tv("Aguardando TXT.",14,true);root.addView(status);
        bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000);root.addView(bar,new LinearLayout.LayoutParams(-1,dp(18)));
        clock=tv("Progresso: 0,0% | Tempo: 00:00 | ETA: --:--",13,true);root.addView(clock);

        trendView=tv("Tendência de repetidas: carregue o TXT.",14,true);root.addView(trendView);

        failTableBtn=bt("VER TABELA DE FREQUÊNCIA DE FALHA");failTableBtn.setEnabled(false);
        root.addView(failTableBtn,new LinearLayout.LayoutParams(-1,dp(60)));
        failTableBtn.setOnClickListener(v->{if(model!=null){out.setText(CoreEngine.failureFrequencyReport(model));clearActions();}});

        m1=bt("MÓDULO 1 — 300 DUPLAS DO UNIVERSO");m1.setEnabled(false);root.addView(m1,new LinearLayout.LayoutParams(-1,dp(66)));
        root.addView(tv("300 duplas de 01–25. Score 100/50/0, evolução, movimento e frequência individual de falha.",13,false));

        m2=bt("MÓDULO 2 — DUPLAS REAIS DAS 10 FALHAS");m2.setEnabled(false);root.addView(m2,new LinearLayout.LayoutParams(-1,dp(66)));
        root.addView(tv("Em cada concurso, gera somente as 45 duplas das 10 falhas e acumula o banco real da falha.",13,false));

        m3=bt("MÓDULO 3 — UNIVERSO 8 / 9 / 10 REPETIDAS");m3.setEnabled(false);root.addView(m3,new LinearLayout.LayoutParams(-1,dp(66)));
        root.addView(tv("Varre 8, 9 e 10 repetidas, aplica padrão + perímetro dos últimos 10 e encontra a falha de 10 mais evoluída.",13,false));

        out=tv("",14,false);out.setTextIsSelectable(true);root.addView(out);
        actions=new LinearLayout(this);actions.setOrientation(LinearLayout.VERTICAL);root.addView(actions);

        m1.setOnClickListener(v->runModule(1));
        m2.setOnClickListener(v->runModule(2));
        m3.setOnClickListener(v->runModule3());
        setContentView(sv);
    }

    void pick(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/*");startActivityForResult(i,10);
    }

    void hardReset(){
        running=false;pct=0;startedAt=0;bar.setProgress(0);
        status.setText("Carregando novo TXT...");
        clock.setText("Progresso: 0,0% | Tempo: 00:00 | ETA: --:--");
        trendView.setText("Tendência de repetidas: recalculando...");
        out.setText("");
        lastResult=null;
        clearActions();
        m1.setEnabled(false);m2.setEnabled(false);m3.setEnabled(false);failTableBtn.setEnabled(false);
    }
    void clearActions(){actions.removeAllViews();}

    protected void onActivityResult(int r,int c,Intent d){
        super.onActivityResult(r,c,d);if(r!=10||c!=RESULT_OK||d==null)return;
        hardReset();
        try(InputStream in=getContentResolver().openInputStream(d.getData())){
            List<int[]>x=CoreEngine.parseHistory(in);model=new CoreEngine.Model(x);
            CoreEngine.RepeatTrend rt=CoreEngine.repeatTrend(model);
            status.setText("TXT carregado: "+x.size()+" concursos. Base anterior zerada.");
            trendView.setText("Último movimento: "+rt.lastRepeat+" repetidas | Tendência indicada: "+rt.suggestion+
                " | confiança relativa: "+String.format(Locale.US,"%.1f%%",rt.confidence*100));
            m1.setEnabled(true);m2.setEnabled(true);m3.setEnabled(true);failTableBtn.setEnabled(true);
        }catch(Exception e){model=null;status.setText("Erro: "+e.getMessage());}
    }

    void runModule(int mod){
        if(model==null)return;
        m1.setEnabled(false);m2.setEnabled(false);m3.setEnabled(false);failTableBtn.setEnabled(false);
        out.setText("");clearActions();startProgress();
        ex.submit(()->{try{
            CoreEngine.Progress p=(a,b)->runOnUiThread(()->update(a,b));
            CoreEngine.ModuleResult rr=mod==1?CoreEngine.module1(model,p):CoreEngine.module2(model,p);
            runOnUiThread(()->{
                running=false;lastResult=rr;out.setText(rr.report);addPdfButton(rr);
                m1.setEnabled(true);m2.setEnabled(true);m3.setEnabled(true);failTableBtn.setEnabled(true);
            });
        }catch(Exception e){runOnUiThread(()->{
            running=false;status.setText("Erro: "+e.getMessage());
            m1.setEnabled(true);m2.setEnabled(true);m3.setEnabled(true);failTableBtn.setEnabled(true);
        });}});
    }


    void runModule3(){
        if(model==null)return;
        m1.setEnabled(false);m2.setEnabled(false);m3.setEnabled(false);failTableBtn.setEnabled(false);
        out.setText("");clearActions();startProgress();
        ex.submit(()->{try{
            CoreEngine.Progress p=(a,b)->runOnUiThread(()->update(a,b));
            List<CoreEngine.UniverseResult> rs=CoreEngine.module3(model,p);
            runOnUiThread(()->{
                running=false;
                StringBuilder sb=new StringBuilder();
                for(CoreEngine.UniverseResult r:rs){
                    if(sb.length()>0)sb.append("\n\n============================\n\n");
                    sb.append(r.report);
                }
                out.setText(sb.toString());
                for(CoreEngine.UniverseResult r:rs)addUniversePdfButton(r);
                m1.setEnabled(true);m2.setEnabled(true);m3.setEnabled(true);failTableBtn.setEnabled(true);
            });
        }catch(Exception e){runOnUiThread(()->{
            running=false;status.setText("Erro: "+e.getMessage());
            m1.setEnabled(true);m2.setEnabled(true);m3.setEnabled(true);failTableBtn.setEnabled(true);
        });}});
    }

    void addUniversePdfButton(CoreEngine.UniverseResult rr){
        Button b=bt("GERAR PDF — "+rr.repeats+" REPETIDAS");
        actions.addView(b,new LinearLayout.LayoutParams(-1,dp(58)));
        b.setOnClickListener(v->pdfUniverse(rr));
    }

    void pdfUniverse(CoreEngine.UniverseResult rr){
        try{
            PdfDocument doc=new PdfDocument();
            PdfDocument.Page pg=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());
            Canvas c=pg.getCanvas();Paint p=new Paint(1);
            p.setColor(purple);c.drawRect(0,0,595,82,p);p.setColor(Color.WHITE);p.setTextSize(17);p.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText("MÓDULO 3 — "+rr.repeats+" REPETIDAS",22,34,p);
            p.setTextSize(11);c.drawText("10 FALHAS EM VERMELHO | 15 DO JOGO EM BRANCO",22,58,p);
            drawVolante(c,rr.failure10,110);doc.finishPage(pg);

            ArrayList<String>ls=wrap(rr.report,80);int at=0,pn=2;
            while(at<ls.size()){
                PdfDocument.Page q=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,pn++).create());
                Canvas ca=q.getCanvas();Paint pa=new Paint(1);
                pa.setColor(purple);ca.drawRect(0,0,595,72,pa);pa.setColor(Color.WHITE);pa.setTypeface(Typeface.DEFAULT_BOLD);pa.setTextSize(16);
                ca.drawText("ANÁLISE DO UNIVERSO "+rr.repeats+" REPETIDAS",22,38,pa);
                pa.setColor(Color.DKGRAY);pa.setTypeface(Typeface.DEFAULT);pa.setTextSize(9);int y=96;
                while(at<ls.size()&&y<810){ca.drawText(ls.get(at++),22,y,pa);y+=13;}
                doc.finishPage(q);
            }

            ContentValues v=new ContentValues();
            v.put(MediaStore.Downloads.DISPLAY_NAME,"M3_"+rr.repeats+"_REPETIDAS_"+System.currentTimeMillis()+".pdf");
            v.put(MediaStore.Downloads.MIME_TYPE,"application/pdf");
            v.put(MediaStore.Downloads.RELATIVE_PATH,"Download/DUPLAS_FALHA");
            Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);
            OutputStream os=getContentResolver().openOutputStream(u);doc.writeTo(os);os.close();doc.close();
            Toast.makeText(this,"PDF "+rr.repeats+" repetidas salvo.",Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(this,"Erro PDF: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }

    void startProgress(){startedAt=SystemClock.elapsedRealtime();pct=0;running=true;bar.setProgress(0);heartbeat();}
    void update(double p,String s){
        pct=Math.max(0,Math.min(100,p));bar.setProgress((int)Math.round(pct*10));
        status.setText(s+" — "+String.format(Locale.US,"%.1f",pct).replace('.',',')+"%");updateClock();
    }
    void heartbeat(){handler.post(new Runnable(){public void run(){if(!running)return;updateClock();handler.postDelayed(this,1000);}});}
    void updateClock(){
        long e=startedAt==0?0:SystemClock.elapsedRealtime()-startedAt;String eta="--:--";
        if(pct>.1&&pct<100){long total=(long)(e*(100.0/pct));eta=fmtTime(Math.max(0,total-e));}
        else if(pct>=100)eta="00:00";
        clock.setText("Progresso: "+String.format(Locale.US,"%.1f",pct).replace('.',',')+"% | Tempo: "+fmtTime(e)+" | ETA: "+eta);
    }
    String fmtTime(long ms){long s=Math.max(0,ms/1000),m=s/60;s%=60;return String.format(Locale.US,"%02d:%02d",m,s);}

    void addPdfButton(CoreEngine.ModuleResult rr){
        Button b=bt("GERAR PDF — 10 FALHAS VERMELHAS + 15 EM BRANCO");
        actions.addView(b,new LinearLayout.LayoutParams(-1,dp(60)));
        b.setOnClickListener(v->pdf(rr));
    }

    void drawVolante(Canvas c,int fail,int top){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);int cell=92,left=66;p.setTextAlign(Paint.Align.CENTER);
        for(int n=1;n<=25;n++){
            int ro=(n-1)/5,co=(n-1)%5;float x=left+co*cell,y=top+ro*cell;boolean f=(fail&(1<<(n-1)))!=0;
            p.setStyle(Paint.Style.FILL);p.setColor(f?Color.rgb(220,40,40):Color.WHITE);c.drawRect(x,y,x+70,y+70,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.DKGRAY);c.drawRect(x,y,x+70,y+70,p);
            p.setStyle(Paint.Style.FILL);p.setColor(f?Color.WHITE:Color.BLACK);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(20);
            c.drawText(String.format(Locale.US,"%02d",n),x+35,y+43,p);
        }
        p.setTextAlign(Paint.Align.LEFT);p.setTextSize(11);p.setColor(Color.DKGRAY);
        c.drawText("VERMELHO = FALHA PROJETADA | BRANCO = JOGO",left,top+5*cell+22,p);
    }

    void pdf(CoreEngine.ModuleResult rr){
        try{
            PdfDocument doc=new PdfDocument();
            PdfDocument.Page pg=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());
            Canvas c=pg.getCanvas();Paint p=new Paint(1);
            p.setColor(purple);c.drawRect(0,0,595,82,p);p.setColor(Color.WHITE);p.setTextSize(17);p.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText("MINERADOR LOTOFÁCIL — DUPLAS DE FALHA",22,34,p);
            p.setTextSize(11);c.drawText("10 FALHAS EM VERMELHO | 15 DO JOGO EM BRANCO",22,58,p);
            drawVolante(c,rr.failure10,110);doc.finishPage(pg);

            ArrayList<String>ls=wrap(rr.report,80);int at=0,pn=2;
            while(at<ls.size()){
                PdfDocument.Page q=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,pn++).create());
                Canvas ca=q.getCanvas();Paint pa=new Paint(1);
                pa.setColor(purple);ca.drawRect(0,0,595,72,pa);pa.setColor(Color.WHITE);pa.setTypeface(Typeface.DEFAULT_BOLD);pa.setTextSize(16);
                ca.drawText("PADRÕES, TENDÊNCIA E JUSTIFICATIVA",22,38,pa);
                pa.setColor(Color.DKGRAY);pa.setTypeface(Typeface.DEFAULT);pa.setTextSize(9);int y=96;
                while(at<ls.size()&&y<810){ca.drawText(ls.get(at++),22,y,pa);y+=13;}
                doc.finishPage(q);
            }
            ContentValues v=new ContentValues();
            v.put(MediaStore.Downloads.DISPLAY_NAME,"DUPLAS_FALHA_V12_"+System.currentTimeMillis()+".pdf");
            v.put(MediaStore.Downloads.MIME_TYPE,"application/pdf");
            v.put(MediaStore.Downloads.RELATIVE_PATH,"Download/DUPLAS_FALHA");
            Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);
            OutputStream os=getContentResolver().openOutputStream(u);doc.writeTo(os);os.close();doc.close();
            Toast.makeText(this,"PDF salvo em Downloads/DUPLAS_FALHA",Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(this,"Erro PDF: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }

    ArrayList<String>wrap(String x,int n){
        ArrayList<String>o=new ArrayList<>();
        for(String l:x.split("\\n",-1)){
            String s=l;if(s.isEmpty()){o.add("");continue;}
            while(s.length()>n){int q=s.lastIndexOf(' ',n);if(q<10)q=n;o.add(s.substring(0,q));s=s.substring(q).trim();}
            o.add(s);
        }
        return o;
    }
}
