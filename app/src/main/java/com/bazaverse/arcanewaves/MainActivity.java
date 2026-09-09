package com.bazaverse.arcanewaves;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b){ super.onCreate(b); setContentView(new GameView()); }

    final class GameView extends View {
        final Paint p=new Paint(3); final Random r=new Random();
        final ArrayList<Enemy> es=new ArrayList<>(); final ArrayList<Bullet> bs=new ArrayList<>();
        int mode=0; // 0 select, 1 battle, 2 shop, 3 game over
        boolean female=false; int wave=1,coins=0,kills=0,total=0,spawn=0;
        float hp=100,maxHp=100,power=18,fireCd=.55f,fireT=0,spawnT=0,px,py; long last=System.nanoTime();
        final String[] upNames={"Potenza Arcana","Cuore Runico","Cadenza Mistica"};
        GameView(){super(MainActivity.this); setBackgroundColor(Color.rgb(12,14,30)); p.setTypeface(Typeface.create("sans",Typeface.NORMAL));}
        void start(){mode=1;es.clear();bs.clear();kills=0;total=6+(int)(wave*1.6f);spawn=total;hp=Math.min(maxHp,hp+15);spawnT=.15f;fireT=.1f;invalidate();}
        void reset(){wave=1;coins=0;hp=maxHp=100;power=18;fireCd=.55f;start();}
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){px=w/2f;py=h*.78f;}
        @Override protected void onDraw(Canvas c){super.onDraw(c); long n=System.nanoTime(); float dt=Math.min(.033f,(n-last)/1_000_000_000f);last=n;if(mode==1)update(dt);drawBg(c); if(mode==0)drawSelect(c); else {drawBattle(c); if(mode==2)drawShop(c); if(mode==3)drawOver(c);} postInvalidateOnAnimation();}
        void update(float dt){fireT-=dt;spawnT-=dt;if(spawn>0&&spawnT<=0){addEnemy();spawn--;spawnT=.45f;}if(fireT<=0){fire();fireT=fireCd;}
            for(int i=es.size()-1;i>=0;i--){Enemy e=es.get(i);float dx=px-e.x,dy=py-e.y,d=(float)Math.hypot(dx,dy);if(d<1)d=1;e.x+=dx/d*e.s*dt;e.y+=dy/d*e.s*dt;if(d<e.rad+18){hp-=e.dmg;es.remove(i);kills++;if(hp<=0){hp=0;mode=3;return;}}}
            for(int i=bs.size()-1;i>=0;i--){Bullet b=bs.get(i);b.x+=b.vx*dt;b.y+=b.vy*dt;boolean hit=false;for(int j=es.size()-1;j>=0;j--){Enemy e=es.get(j);float dx=b.x-e.x,dy=b.y-e.y;if(dx*dx+dy*dy<(b.rad+e.rad)*(b.rad+e.rad)){e.hp-=b.dmg;hit=true;if(e.hp<=0){coins+=e.coin;kills++;es.remove(j);}break;}}if(hit||b.x<-30||b.x>getWidth()+30||b.y<-30||b.y>getHeight()+30)bs.remove(i);}
            if(spawn==0&&es.isEmpty()&&kills>=total){coins+=5+wave*2;mode=2;}
        }
        void addEnemy(){boolean boss=wave%5==0&&spawn==1;Enemy e=new Enemy();e.x=25+r.nextFloat()*Math.max(1,getWidth()-50);e.y=-30;e.rad=boss?32:18;e.hp=e.max=boss?220+wave*20:35+wave*8;e.s=(boss?.55f:1.1f)*60;e.dmg=boss?24:8+wave*1.5f;e.coin=boss?35:5+wave/2;es.add(e);}
        void fire(){Enemy best=null;float bd=Float.MAX_VALUE;for(Enemy e:es){float dx=e.x-px,dy=e.y-py,d=dx*dx+dy*dy;if(d<bd){bd=d;best=e;}}if(best==null)return;float a=(float)Math.atan2(best.y-py,best.x-px);Bullet b=new Bullet();b.x=px;b.y=py;b.vx=(float)Math.cos(a)*430;b.vy=(float)Math.sin(a)*430;b.rad=7;b.dmg=power;bs.add(b);}
        void drawBg(Canvas c){Paint g=new Paint();LinearGradient lg=new LinearGradient(0,0,0,getHeight(),Color.rgb(21,25,54),Color.rgb(45,19,61),Shader.TileMode.CLAMP);g.setShader(lg);c.drawRect(0,0,getWidth(),getHeight(),g);}
        void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align align){p.setShader(null);p.setColor(color);p.setTextSize(size);p.setTextAlign(align);p.setTypeface(Typeface.create("sans",Typeface.BOLD));c.drawText(s,x,y,p);}
        void drawHero(Canvas c,float x,float y,float sc){p.setColor(female?Color.rgb(220,85,180):Color.rgb(75,130,235));c.drawCircle(x,y,20*sc,p);p.setColor(Color.rgb(245,210,185));c.drawCircle(x,y-17*sc,11*sc,p);p.setColor(Color.rgb(35,25,48));c.drawArc(x-13*sc,y-30*sc,x+13*sc,y-8*sc,180,180,true,p);text(c,"✦",x,y+7*sc,18*sc,Color.WHITE,Paint.Align.CENTER);}
        void drawSelect(Canvas c){text(c,"ARCANE WAVES",getWidth()/2f,80,34,Color.WHITE,Paint.Align.CENTER);text(c,"Scegli il personaggio",getWidth()/2f,125,18,0xffd8ccff,Paint.Align.CENTER);female=false;drawHero(c,getWidth()*.32f,getHeight()*.42f,2);text(c,"MAGO",getWidth()*.32f,getHeight()*.58f,18,Color.WHITE,Paint.Align.CENTER);female=true;drawHero(c,getWidth()*.68f,getHeight()*.42f,2);text(c,"MAGA",getWidth()*.68f,getHeight()*.58f,18,Color.WHITE,Paint.Align.CENTER);text(c,"Tocca un personaggio per iniziare",getWidth()/2f,getHeight()*.72f,14,0xffc8bde9,Paint.Align.CENTER);}
        void drawBattle(Canvas c){for(Enemy e:es){p.setColor(e.rad>25?0xffb63d74:0xff66c7a4);c.drawCircle(e.x,e.y,e.rad,p);p.setColor(0xff222222);c.drawRect(e.x-e.rad,e.y-e.rad-10,e.x+e.rad,e.y-e.rad-5,p);p.setColor(0xffffda72);c.drawRect(e.x-e.rad,e.y-e.rad-10,e.x-e.rad+2*e.rad*Math.max(0,e.hp/e.max),e.y-e.rad-5,p);}for(Bullet b:bs){p.setColor(0xffd3b3ff);c.drawCircle(b.x,b.y,b.rad,p);}drawHero(c,px,py,1);p.setColor(0xaa000000);c.drawRect(0,0,getWidth(),72,p);text(c,"Ondata "+wave,14,28,18,Color.WHITE,Paint.Align.LEFT);text(c,"✦ "+coins,14,55,18,0xffffe186,Paint.Align.LEFT);text(c,(int)Math.ceil(hp)+" / "+(int)maxHp,getWidth()-14,28,18,Color.WHITE,Paint.Align.RIGHT);text(c,"KO "+kills+" / "+total,getWidth()-14,55,16,Color.WHITE,Paint.Align.RIGHT);}
        void drawShop(Canvas c){p.setColor(0xee080814);c.drawRect(0,0,getWidth(),getHeight(),p);text(c,"ONDATA COMPLETATA",getWidth()/2f,75,28,Color.WHITE,Paint.Align.CENTER);text(c,"Monete: "+coins,getWidth()/2f,110,18,0xffffe186,Paint.Align.CENTER);for(int i=0;i<3;i++){float y=155+i*94;p.setColor(0xff503d87);c.drawRoundRect(24,y,getWidth()-24,y+68,18,18,p);text(c,upNames[i],38,y+29,18,Color.WHITE,Paint.Align.LEFT);int cost=15+wave*3+i*4;text(c,"✦ "+cost,getWidth()-38,y+29,18,0xffffe186,Paint.Align.RIGHT);}p.setColor(0xff744fd0);c.drawRoundRect(24,getHeight()-92,getWidth()-24,getHeight()-34,18,18,p);text(c,"INIZIA ONDATA "+(wave+1),getWidth()/2f,getHeight()-55,18,Color.WHITE,Paint.Align.CENTER);}
        void drawOver(Canvas c){p.setColor(0xee100818);c.drawRect(0,0,getWidth(),getHeight(),p);text(c,"SCONFITTA",getWidth()/2f,getHeight()*.42f,34,0xffff8ca7,Paint.Align.CENTER);text(c,"Hai raggiunto l’ondata "+wave,getWidth()/2f,getHeight()*.5f,18,Color.WHITE,Paint.Align.CENTER);text(c,"Tocca per ricominciare",getWidth()/2f,getHeight()*.60f,18,Color.WHITE,Paint.Align.CENTER);}
        @Override public boolean onTouchEvent(android.view.MotionEvent e){float x=e.getX(),y=e.getY();if(e.getAction()==MotionEvent.ACTION_DOWN){if(mode==0){female=x>=getWidth()/2f;reset();return true;}if(mode==3){reset();return true;}if(mode==2){for(int i=0;i<3;i++){float yy=155+i*94;if(y>=yy&&y<=yy+68){int cost=15+wave*3+i*4;if(coins>=cost){coins-=cost;if(i==0)power+=6;else if(i==1){maxHp+=20;hp+=20;}else fireCd=Math.max(.1f,fireCd*.9f);}return true;}}if(y>getHeight()-120){wave++;start();return true;}}}if(mode==1&&(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE)){px=Math.max(20,Math.min(getWidth()-20,x));py=Math.max(95,Math.min(getHeight()-30,y));return true;}return true;}
    }
    static final class Enemy{float x,y,rad,hp,max,s,dmg;int coin;} static final class Bullet{float x,y,vx,vy,rad,dmg;}
}
