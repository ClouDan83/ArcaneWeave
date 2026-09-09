package com.bazaverse.arcanewaves;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.content.SharedPreferences;
import java.util.*;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN);
        setContentView(new GameView());
    }

    final class GameView extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Random rng = new Random();
        final ArrayList<Enemy> enemies = new ArrayList<>();
        final ArrayList<Shot> shots = new ArrayList<>();
        final ArrayList<Shot> enemyShots = new ArrayList<>();
        final ArrayList<Particle> particles = new ArrayList<>();
        final SharedPreferences prefs = getSharedPreferences("arcane_waves", MODE_PRIVATE);

        int mode = 0;
        boolean female = false;
        int wave = 1, coins = 0, kills = 0, total = 0, spawnLeft = 0;
        int bestWave, lifetimeCoins;
        int[] upgradeLv = new int[9];
        int[] offers = new int[]{0,1,2};

        float hp = 120, maxHp = 120;
        float power = 22, fireCd = .48f, fireT = 0;
        float regen = 0, shieldMax = 0, shield = 0;
        float coinBonus = 0, orbSize = 7, projectileSpeed = 500;
        int multicast = 0;
        float spawnT = 0, px, py, targetX, targetY;
        float damageFlash = 0, waveBanner = 0;
        long last = System.nanoTime();

        final String[] upNames = {
                "Potenza Arcana","Cuore Runico","Cadenza Mistica",
                "Multicasting","Rigenerazione","Barriera Astrale",
                "Fortuna Mistica","Orbita Amplificata","Impulso Velocita"
        };
        final String[] upDesc = {
                "+18% danni magici","+30 vita massima e cura","-9% tempo fra gli attacchi",
                "+1 globo per raffica","+1,2 HP al secondo","+24 scudo a ogni ondata",
                "+12% monete dai nemici","+1,5 raggio e +8% danni","+45 velocita proiettili"
        };
        final int[] baseCost = {18,20,22,35,28,26,24,20,18};

        GameView() {
            super(MainActivity.this);
            setFocusable(true);
            bestWave = prefs.getInt("bestWave", 1);
            lifetimeCoins = prefs.getInt("lifetimeCoins", 0);
            p.setTypeface(Typeface.create("sans", Typeface.BOLD));
        }

        @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
            px = targetX = w / 2f;
            py = targetY = h * .78f;
        }

        void newRun() {
            wave = 1; coins = 0; kills = 0;
            hp = maxHp = 120; power = 22; fireCd = .48f; regen = 0;
            shieldMax = shield = 0; coinBonus = 0; orbSize = 7;
            projectileSpeed = 500; multicast = 0;
            Arrays.fill(upgradeLv, 0);
            startWave();
        }

        void startWave() {
            mode = 1;
            enemies.clear(); shots.clear(); enemyShots.clear(); particles.clear();
            kills = 0;
            total = 7 + (int)(wave * 1.8f);
            if (wave % 5 == 0) total += 1;
            spawnLeft = total;
            hp = Math.min(maxHp, hp + maxHp * .12f);
            shield = shieldMax;
            spawnT = .15f;
            fireT = .1f;
            waveBanner = 1.7f;
            targetX = px; targetY = py;
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            long now = System.nanoTime();
            float dt = Math.min(.033f, (now - last) / 1_000_000_000f);
            last = now;

            if (mode == 1) update(dt);
            updateParticles(dt);
            drawBackground(c);

            if (mode == 0) drawSelect(c);
            else {
                drawWorld(c);
                if (mode == 2) drawShop(c);
                else if (mode == 3) drawGameOver(c);
            }
            postInvalidateOnAnimation();
        }

        void update(float dt) {
            damageFlash = Math.max(0, damageFlash - dt * 2.8f);
            waveBanner = Math.max(0, waveBanner - dt);
            fireT -= dt;
            spawnT -= dt;

            float dxMove = targetX - px, dyMove = targetY - py;
            float moveD = (float)Math.hypot(dxMove, dyMove);
            float step = 650 * dt;
            if (moveD > 2) {
                if (moveD <= step) { px = targetX; py = targetY; }
                else { px += dxMove / moveD * step; py += dyMove / moveD * step; }
            }

            if (regen > 0 && hp > 0) hp = Math.min(maxHp, hp + regen * dt);

            if (spawnLeft > 0 && spawnT <= 0) {
                addEnemy();
                spawnLeft--;
                spawnT = Math.max(.16f, .52f - wave * .008f);
            }

            if (fireT <= 0) {
                fireVolley();
                fireT = Math.max(.075f, fireCd);
            }

            for (int i = enemies.size() - 1; i >= 0; i--) {
                Enemy e = enemies.get(i);
                e.t += dt;
                e.contactCd -= dt;
                e.shootCd -= dt;
                float dx = px - e.x, dy = py - e.y;
                float d = Math.max(1, (float)Math.hypot(dx, dy));

                if (e.type == 3 && !e.boss) {
                    if (d > 300) { e.x += dx/d * e.speed * dt; e.y += dy/d * e.speed * dt; }
                    else if (d < 190) { e.x -= dx/d * e.speed * .65f * dt; e.y -= dy/d * e.speed * .65f * dt; }
                    if (e.shootCd <= 0) {
                        enemyBolt(e, false);
                        e.shootCd = Math.max(.7f, 1.8f - wave * .018f);
                    }
                } else {
                    float sway = (float)Math.sin(e.t * 3 + e.seed) * (e.type == 2 ? 34 : 8);
                    float nx = dx/d, ny = dy/d;
                    e.x += (nx * e.speed - ny * sway) * dt;
                    e.y += (ny * e.speed + nx * sway) * dt;
                }

                if (e.boss && e.shootCd <= 0) {
                    int n = 8 + Math.min(8, wave / 10);
                    for (int k = 0; k < n; k++) {
                        float a = (float)(Math.PI * 2 * k / n + e.t * .5);
                        Shot s = new Shot();
                        s.x = e.x; s.y = e.y;
                        s.vx = (float)Math.cos(a) * 170;
                        s.vy = (float)Math.sin(a) * 170;
                        s.r = 8; s.dmg = 8 + wave * .8f; s.enemy = true;
                        enemyShots.add(s);
                    }
                    e.shootCd = Math.max(1.1f, 2.4f - wave * .02f);
                    burst(e.x, e.y, 0xffff6a9e, 14);
                }

                if (d < e.r + 21 && e.contactCd <= 0) {
                    hurt(e.dmg);
                    e.contactCd = e.boss ? .55f : .8f;
                    e.x -= dx/d * 42; e.y -= dy/d * 42;
                    if (mode == 3) return;
                }
            }

            for (int i = shots.size() - 1; i >= 0; i--) {
                Shot s = shots.get(i);
                s.x += s.vx * dt; s.y += s.vy * dt;
                boolean remove = false;
                for (int j = enemies.size() - 1; j >= 0; j--) {
                    Enemy e = enemies.get(j);
                    float dx = s.x - e.x, dy = s.y - e.y;
                    if (dx*dx + dy*dy <= (s.r + e.r) * (s.r + e.r)) {
                        e.hp -= s.dmg;
                        burst(s.x, s.y, 0xffcaa8ff, 4);
                        remove = true;
                        if (e.hp <= 0) {
                            int gain = Math.max(1, Math.round(e.coin * (1f + coinBonus)));
                            coins += gain;
                            lifetimeCoins += gain;
                            kills++;
                            burst(e.x, e.y, e.boss ? 0xffff5d93 : enemyColor(e), e.boss ? 30 : 12);
                            enemies.remove(j);
                            prefs.edit().putInt("lifetimeCoins", lifetimeCoins).apply();
                        }
                        break;
                    }
                }
                if (remove || out(s.x, s.y, 50)) shots.remove(i);
            }

            for (int i = enemyShots.size() - 1; i >= 0; i--) {
                Shot s = enemyShots.get(i);
                s.x += s.vx * dt; s.y += s.vy * dt;
                float dx = s.x - px, dy = s.y - py;
                if (dx*dx + dy*dy < (s.r + 19) * (s.r + 19)) {
                    hurt(s.dmg);
                    enemyShots.remove(i);
                    if (mode == 3) return;
                } else if (out(s.x, s.y, 60)) enemyShots.remove(i);
            }

            if (spawnLeft == 0 && enemies.isEmpty()) {
                enemyShots.clear();
                int reward = 8 + wave * 3;
                coins += reward;
                lifetimeCoins += reward;
                bestWave = Math.max(bestWave, wave);
                prefs.edit().putInt("bestWave", bestWave).putInt("lifetimeCoins", lifetimeCoins).apply();
                rollAllOffers();
                mode = 2;
            }
        }

        boolean out(float x, float y, float m) {
            return x < -m || y < -m || x > getWidth() + m || y > getHeight() + m;
        }

        void hurt(float raw) {
            float dmg = raw;
            if (shield > 0) {
                float used = Math.min(shield, dmg);
                shield -= used; dmg -= used;
            }
            if (dmg > 0) hp -= dmg;
            damageFlash = .34f;
            burst(px, py, 0xffff657d, 10);
            if (hp <= 0) {
                hp = 0; mode = 3;
                bestWave = Math.max(bestWave, wave);
                prefs.edit().putInt("bestWave", bestWave).putInt("lifetimeCoins", lifetimeCoins).apply();
            }
        }

        void addEnemy() {
            Enemy e = new Enemy();
            boolean boss = wave % 5 == 0 && spawnLeft == 1;
            e.boss = boss;
            e.seed = rng.nextFloat() * 10;
            if (boss) e.type = 4;
            else {
                float q = rng.nextFloat();
                e.type = q < .34f ? 0 : q < .57f ? 1 : q < .78f ? 2 : 3;
            }

            int side = rng.nextInt(3);
            if (side == 0) { e.x = 30 + rng.nextFloat() * Math.max(1, getWidth() - 60); e.y = -45; }
            else if (side == 1) { e.x = -45; e.y = 90 + rng.nextFloat() * Math.max(1, getHeight() * .55f); }
            else { e.x = getWidth() + 45; e.y = 90 + rng.nextFloat() * Math.max(1, getHeight() * .55f); }

            float scale = 1f + wave * .105f;
            if (boss) {
                e.r = 42; e.max = e.hp = (320 + wave * 34) * scale;
                e.speed = 52 + wave * .5f; e.dmg = 20 + wave * 1.3f;
                e.coin = 45 + wave * 3; e.shootCd = 1.2f;
            } else if (e.type == 0) {
                e.r = 18; e.max = e.hp = 32 * scale; e.speed = 100 + wave*.55f; e.dmg = 7 + wave*.75f; e.coin = 5 + wave/4;
            } else if (e.type == 1) {
                e.r = 25; e.max = e.hp = 78 * scale; e.speed = 56 + wave*.35f; e.dmg = 12 + wave*.95f; e.coin = 8 + wave/3;
            } else if (e.type == 2) {
                e.r = 15; e.max = e.hp = 24 * scale; e.speed = 145 + wave*.65f; e.dmg = 6 + wave*.6f; e.coin = 6 + wave/4;
            } else {
                e.r = 20; e.max = e.hp = 44 * scale; e.speed = 72 + wave*.4f; e.dmg = 7 + wave*.55f; e.coin = 9 + wave/3; e.shootCd = .5f + rng.nextFloat();
            }
            enemies.add(e);
        }

        void enemyBolt(Enemy e, boolean fast) {
            float a = (float)Math.atan2(py - e.y, px - e.x);
            Shot s = new Shot();
            s.x = e.x; s.y = e.y;
            float sp = fast ? 260 : 205;
            s.vx = (float)Math.cos(a) * sp;
            s.vy = (float)Math.sin(a) * sp;
            s.r = 7; s.dmg = 7 + wave * .65f; s.enemy = true;
            enemyShots.add(s);
        }

        void fireVolley() {
            Enemy best = null;
            float bd = Float.MAX_VALUE;
            for (Enemy e : enemies) {
                float dx = e.x - px, dy = e.y - py, d = dx*dx + dy*dy;
                if (d < bd) { bd = d; best = e; }
            }
            if (best == null) return;

            int visibleShots = Math.min(9, 1 + multicast);
            float overflowBoost = 1f + Math.max(0, multicast - 8) * .12f;
            float base = (float)Math.atan2(best.y - py, best.x - px);
            for (int i = 0; i < visibleShots; i++) {
                float offset = (i - (visibleShots - 1) / 2f) * .095f;
                Shot s = new Shot();
                s.x = px; s.y = py - 6;
                s.vx = (float)Math.cos(base + offset) * projectileSpeed;
                s.vy = (float)Math.sin(base + offset) * projectileSpeed;
                s.r = orbSize;
                s.dmg = power * overflowBoost * (visibleShots > 1 ? .78f : 1f);
                shots.add(s);
            }
            burst(px, py - 10, female ? 0xffff9fe2 : 0xff83c8ff, 3);
        }

        void rollAllOffers() {
            for (int i = 0; i < 3; i++) {
                int id;
                do { id = rng.nextInt(upNames.length); } while ((i > 0 && id == offers[0]) || (i > 1 && id == offers[1]));
                offers[i] = id;
            }
        }

        int upgradeCost(int id) {
            return baseCost[id] + wave * 2 + upgradeLv[id] * Math.max(4, baseCost[id] / 3);
        }

        void buyUpgrade(int slot) {
            int id = offers[slot], cost = upgradeCost(id);
            if (coins < cost) return;
            coins -= cost; upgradeLv[id]++;
            switch (id) {
                case 0: power *= 1.18f; break;
                case 1: maxHp += 30; hp = Math.min(maxHp, hp + 30); break;
                case 2: fireCd = Math.max(.075f, fireCd * .91f); if (fireCd <= .076f) power *= 1.04f; break;
                case 3: multicast++; break;
                case 4: regen += 1.2f; break;
                case 5: shieldMax += 24; shield = shieldMax; break;
                case 6: coinBonus += .12f; break;
                case 7: orbSize = Math.min(22, orbSize + 1.5f); power *= 1.08f; break;
                case 8: projectileSpeed += 45; break;
            }
            int next;
            do { next = rng.nextInt(upNames.length); } while (next == offers[(slot+1)%3] || next == offers[(slot+2)%3]);
            offers[slot] = next;
            burst(getWidth()/2f, 150 + slot*105, 0xffffd76c, 16);
        }

        void updateParticles(float dt) {
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle q = particles.get(i);
                q.life -= dt;
                q.x += q.vx * dt; q.y += q.vy * dt;
                q.vy += 35 * dt;
                if (q.life <= 0) particles.remove(i);
            }
        }

        void burst(float x, float y, int color, int n) {
            for (int i = 0; i < n && particles.size() < 180; i++) {
                float a = rng.nextFloat() * (float)Math.PI * 2;
                float sp = 35 + rng.nextFloat() * 150;
                Particle q = new Particle();
                q.x=x; q.y=y; q.vx=(float)Math.cos(a)*sp; q.vy=(float)Math.sin(a)*sp;
                q.life=q.max=.25f+rng.nextFloat()*.45f; q.r=2+rng.nextFloat()*4; q.color=color;
                particles.add(q);
            }
        }

        void drawBackground(Canvas c) {
            Paint bg = new Paint();
            LinearGradient lg = new LinearGradient(0,0,0,getHeight(),
                    0xff11152f, 0xff351747, Shader.TileMode.CLAMP);
            bg.setShader(lg); c.drawRect(0,0,getWidth(),getHeight(),bg);

            p.setShader(null);
            p.setColor(0x18ffffff);
            for (int i=0;i<18;i++) {
                float x = (i*79 % Math.max(1,getWidth())) + (float)Math.sin(i*2.1)*18;
                float y = (i*137 % Math.max(1,getHeight()));
                c.drawCircle(x,y,1.5f+(i%3),p);
            }
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(0x164a9fff);
            c.drawCircle(getWidth()*.5f,getHeight()*.62f,getWidth()*.42f,p);
            c.drawCircle(getWidth()*.5f,getHeight()*.62f,getWidth()*.28f,p);
            p.setStyle(Paint.Style.FILL);
        }

        void drawWorld(Canvas c) {
            for (Enemy e : enemies) drawEnemy(c,e);
            for (Shot s : shots) {
                p.setColor(0x55d7b8ff); c.drawCircle(s.x,s.y,s.r*2.1f,p);
                p.setColor(female ? 0xffffb1e7 : 0xffa6d8ff); c.drawCircle(s.x,s.y,s.r,p);
                p.setColor(Color.WHITE); c.drawCircle(s.x-s.r*.25f,s.y-s.r*.25f,Math.max(1,s.r*.28f),p);
            }
            for (Shot s : enemyShots) {
                p.setColor(0x44ff315f); c.drawCircle(s.x,s.y,s.r*2.1f,p);
                p.setColor(0xffff5c7e); c.drawCircle(s.x,s.y,s.r,p);
            }
            for (Particle q : particles) {
                int a = (int)(255 * Math.max(0,q.life/q.max));
                p.setColor((q.color & 0x00ffffff) | (a<<24));
                c.drawCircle(q.x,q.y,q.r,p);
            }

            drawHero(c,px,py,1f,female);

            if (shield > 0) {
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3);
                p.setColor(0x995fc8ff); c.drawCircle(px,py-4,29,p);
                p.setStyle(Paint.Style.FILL);
            }

            drawHud(c);
            if (waveBanner > 0) {
                float alpha = Math.min(1, waveBanner*1.5f);
                p.setColor(((int)(150*alpha)<<24) | 0x00101028);
                c.drawRoundRect(35,getHeight()*.38f,getWidth()-35,getHeight()*.52f,26,26,p);
                text(c,"ONDATA "+wave,getWidth()/2f,getHeight()*.445f,30,Color.WHITE,Paint.Align.CENTER);
                if (wave%5==0) text(c,"BOSS IN ARRIVO",getWidth()/2f,getHeight()*.49f,15,0xffff82a9,Paint.Align.CENTER);
            }

            if (damageFlash > 0) {
                int a = (int)(90 * Math.min(1, damageFlash*3));
                c.drawColor((a<<24) | 0x00ff244e);
            }
        }

        void drawHud(Canvas c) {
            p.setColor(0xb20a0d1d); c.drawRect(0,0,getWidth(),86,p);
            text(c,"ONDATA "+wave,14,25,17,Color.WHITE,Paint.Align.LEFT);
            text(c,"✦ "+coins,14,51,17,0xffffdf79,Paint.Align.LEFT);
            text(c,"BEST "+bestWave,14,73,12,0xffbbb4d8,Paint.Align.LEFT);

            text(c,(int)Math.ceil(hp)+" / "+(int)maxHp,getWidth()-14,24,15,Color.WHITE,Paint.Align.RIGHT);
            float barW = Math.min(180,getWidth()*.38f), x2=getWidth()-14, x1=x2-barW;
            p.setColor(0xff2b2238); c.drawRoundRect(x1,34,x2,46,6,6,p);
            p.setColor(0xffff5c7e); c.drawRoundRect(x1,34,x1+barW*Math.max(0,hp/maxHp),46,6,6,p);
            if (shieldMax > 0) {
                p.setColor(0xff2b2238); c.drawRoundRect(x1,50,x2,59,5,5,p);
                p.setColor(0xff61c9ff); c.drawRoundRect(x1,50,x1+barW*Math.max(0,shield/shieldMax),59,5,5,p);
            }
            text(c,"KO "+kills+" / "+total,getWidth()-14,78,13,0xffded8ee,Paint.Align.RIGHT);
        }

        void drawSelect(Canvas c) {
            text(c,"ARCANE WAVES",getWidth()/2f,72,34,Color.WHITE,Paint.Align.CENTER);
            text(c,"Fantasy Survival",getWidth()/2f,101,15,0xffc5b7f4,Paint.Align.CENTER);
            text(c,"Scegli il tuo arcanista",getWidth()/2f,145,18,0xffeee8ff,Paint.Align.CENTER);

            p.setColor(0x554d68ba); c.drawRoundRect(24,185,getWidth()/2f-8,getHeight()*.66f,26,26,p);
            p.setColor(0x556b3e86); c.drawRoundRect(getWidth()/2f+8,185,getWidth()-24,getHeight()*.66f,26,26,p);
            drawHero(c,getWidth()*.27f,getHeight()*.40f,2.0f,false);
            drawHero(c,getWidth()*.73f,getHeight()*.40f,2.0f,true);
            text(c,"MAGO",getWidth()*.27f,getHeight()*.59f,19,Color.WHITE,Paint.Align.CENTER);
            text(c,"MAGA",getWidth()*.73f,getHeight()*.59f,19,Color.WHITE,Paint.Align.CENTER);
            text(c,"Tocca un personaggio",getWidth()/2f,getHeight()*.75f,16,0xffd7cfee,Paint.Align.CENTER);
            text(c,"Record: ondata "+bestWave+"   •   Monete totali: "+lifetimeCoins,getWidth()/2f,getHeight()*.82f,13,0xffffdf79,Paint.Align.CENTER);
        }

        void drawHero(Canvas c,float x,float y,float sc,boolean isFemale) {
            float bob = mode==1 ? (float)Math.sin(System.nanoTime()/180_000_000.0)*1.5f : 0;
            y += bob;

            p.setColor(isFemale ? 0x223ff2d0 : 0x223aa8ff);
            c.drawCircle(x,y,30*sc,p);

            Path robe = new Path();
            robe.moveTo(x-14*sc,y-2*sc); robe.lineTo(x-22*sc,y+28*sc);
            robe.lineTo(x+22*sc,y+28*sc); robe.lineTo(x+14*sc,y-2*sc); robe.close();
            p.setColor(isFemale ? 0xff8f55c8 : 0xff376fc6); c.drawPath(robe,p);

            p.setColor(0xffd8b7ff); c.drawCircle(x-15*sc,y+2*sc,5*sc,p); c.drawCircle(x+15*sc,y+2*sc,5*sc,p);
            p.setColor(0xffffd9c3); c.drawCircle(x,y-18*sc,12*sc,p);

            p.setColor(isFemale ? 0xff4a254f : 0xff273044);
            Path hair = new Path();
            hair.moveTo(x-13*sc,y-21*sc); hair.quadTo(x,y-38*sc,x+14*sc,y-21*sc);
            hair.lineTo(x+10*sc,y-10*sc);
            hair.lineTo(x+5*sc,y-20*sc); hair.lineTo(x,y-10*sc);
            hair.lineTo(x-4*sc,y-21*sc); hair.lineTo(x-10*sc,y-10*sc); hair.close();
            c.drawPath(hair,p);
            if (isFemale) {
                c.drawOval(x-15*sc,y-22*sc,x-8*sc,y+6*sc,p);
                c.drawOval(x+8*sc,y-22*sc,x+15*sc,y+6*sc,p);
            }

            p.setColor(0xff243040);
            c.drawOval(x-6*sc,y-18*sc,x-2*sc,y-14*sc,p);
            c.drawOval(x+2*sc,y-18*sc,x+6*sc,y-14*sc,p);
            p.setColor(Color.WHITE);
            c.drawCircle(x-4.7f*sc,y-16.8f*sc,1.1f*sc,p);
            c.drawCircle(x+3.3f*sc,y-16.8f*sc,1.1f*sc,p);

            p.setStrokeWidth(3*sc); p.setColor(0xffc6a06a);
            c.drawLine(x+15*sc,y+2*sc,x+24*sc,y+24*sc,p);
            p.setColor(isFemale ? 0xffff9ee0 : 0xff8ed5ff);
            c.drawCircle(x+14*sc,y-1*sc,5*sc,p);
            p.setStrokeWidth(1);
        }

        int enemyColor(Enemy e) {
            if (e.boss) return 0xffff4f78;
            if (e.type==0) return 0xffe16f55;
            if (e.type==1) return 0xff7e8b91;
            if (e.type==2) return 0xff7ad6d0;
            return 0xffb26de0;
        }

        void drawEnemy(Canvas c, Enemy e) {
            int col=enemyColor(e);
            if (e.boss) {
                p.setColor(0x33ff355f); c.drawCircle(e.x,e.y,e.r*1.35f,p);
                p.setColor(col); c.drawCircle(e.x,e.y,e.r,p);
                p.setColor(0xff3b1931);
                Path h=new Path(); h.moveTo(e.x-e.r*.65f,e.y-e.r*.55f);h.lineTo(e.x-e.r*.25f,e.y-e.r*1.2f);h.lineTo(e.x-e.r*.05f,e.y-e.r*.55f);h.close();c.drawPath(h,p);
                h=new Path(); h.moveTo(e.x+e.r*.65f,e.y-e.r*.55f);h.lineTo(e.x+e.r*.25f,e.y-e.r*1.2f);h.lineTo(e.x+e.r*.05f,e.y-e.r*.55f);h.close();c.drawPath(h,p);
                p.setColor(0xffffd6e4); c.drawCircle(e.x-13,e.y-6,4,p); c.drawCircle(e.x+13,e.y-6,4,p);
            } else if (e.type==0) {
                p.setColor(col); c.drawCircle(e.x,e.y,e.r,p);
                p.setColor(0xff5a2d28);
                Path h=new Path();h.moveTo(e.x-e.r*.7f,e.y-e.r*.55f);h.lineTo(e.x-e.r*.25f,e.y-e.r*1.05f);h.lineTo(e.x-e.r*.05f,e.y-e.r*.55f);h.close();c.drawPath(h,p);
                p.setColor(Color.WHITE);c.drawCircle(e.x-5,e.y-3,3,p);c.drawCircle(e.x+5,e.y-3,3,p);
            } else if (e.type==1) {
                p.setColor(col); c.drawRoundRect(e.x-e.r,e.y-e.r*.8f,e.x+e.r,e.y+e.r*.8f,8,8,p);
                p.setColor(0xffb7c3c8);c.drawRect(e.x-e.r*.45f,e.y-e.r*.65f,e.x+e.r*.45f,e.y-e.r*.15f,p);
                p.setColor(0xffffd36e);c.drawCircle(e.x-6,e.y-7,3,p);c.drawCircle(e.x+6,e.y-7,3,p);
            } else if (e.type==2) {
                p.setColor(0x557ad6d0);c.drawCircle(e.x,e.y,e.r*1.4f,p);
                p.setColor(col);Path w=new Path();w.moveTo(e.x,e.y-e.r);w.quadTo(e.x+e.r*1.3f,e.y,e.x,e.y+e.r);w.quadTo(e.x-e.r*1.3f,e.y,e.x,e.y-e.r);w.close();c.drawPath(w,p);
                p.setColor(0xff183c46);c.drawCircle(e.x-4,e.y-3,2.5f,p);c.drawCircle(e.x+4,e.y-3,2.5f,p);
            } else {
                p.setColor(0x553f1f60);c.drawCircle(e.x,e.y,e.r*1.25f,p);
                p.setColor(col);Path hood=new Path();hood.moveTo(e.x,e.y-e.r);hood.lineTo(e.x+e.r,e.y+e.r*.8f);hood.lineTo(e.x-e.r,e.y+e.r*.8f);hood.close();c.drawPath(hood,p);
                p.setColor(0xff1d1028);c.drawCircle(e.x,e.y-1,8,p);
                p.setColor(0xffff87e7);c.drawCircle(e.x-3,e.y-2,2,p);c.drawCircle(e.x+3,e.y-2,2,p);
            }

            float bw=e.r*2;
            p.setColor(0xaa16121d);c.drawRoundRect(e.x-e.r,e.y-e.r-13,e.x+e.r,e.y-e.r-7,3,3,p);
            p.setColor(e.boss?0xffff7193:0xffffd36e);
            c.drawRoundRect(e.x-e.r,e.y-e.r-13,e.x-e.r+bw*Math.max(0,e.hp/e.max),e.y-e.r-7,3,3,p);
        }

        void drawShop(Canvas c) {
            p.setColor(0xf20a0918); c.drawRect(0,0,getWidth(),getHeight(),p);
            text(c,"ONDATA "+wave+" COMPLETATA",getWidth()/2f,64,25,Color.WHITE,Paint.Align.CENTER);
            text(c,"✦ "+coins+" monete",getWidth()/2f,94,17,0xffffdf79,Paint.Align.CENTER);
            text(c,"Tocca per acquistare • le offerte si rinnovano",getWidth()/2f,120,12,0xffbfb4d5,Paint.Align.CENTER);

            for(int i=0;i<3;i++) {
                float top=148+i*105;
                int id=offers[i], cost=upgradeCost(id);
                p.setColor(coins>=cost?0xff453376:0xff282239);
                c.drawRoundRect(20,top,getWidth()-20,top+84,19,19,p);
                text(c,upNames[id]+"  Lv."+upgradeLv[id],36,top+29,17,Color.WHITE,Paint.Align.LEFT);
                text(c,upDesc[id],36,top+55,12,0xffd1c8e6,Paint.Align.LEFT);
                text(c,"✦ "+cost,getWidth()-36,top+31,16,coins>=cost?0xffffdf79:0xff80788e,Paint.Align.RIGHT);
            }

            float by=getHeight()-96;
            p.setColor(0xff6b4fd1);c.drawRoundRect(20,by,getWidth()-20,by+60,20,20,p);
            text(c,"INIZIA ONDATA "+(wave+1),getWidth()/2f,by+38,18,Color.WHITE,Paint.Align.CENTER);
        }

        void drawGameOver(Canvas c) {
            p.setColor(0xf20f0717);c.drawRect(0,0,getWidth(),getHeight(),p);
            text(c,"SCONFITTA",getWidth()/2f,getHeight()*.36f,38,0xffff7898,Paint.Align.CENTER);
            text(c,"Hai raggiunto l'ondata "+wave,getWidth()/2f,getHeight()*.44f,19,Color.WHITE,Paint.Align.CENTER);
            text(c,"Record: "+bestWave,getWidth()/2f,getHeight()*.49f,16,0xffffdf79,Paint.Align.CENTER);
            text(c,"Tocca per ricominciare",getWidth()/2f,getHeight()*.60f,17,0xffd8d0e8,Paint.Align.CENTER);
        }

        void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align align) {
            p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextSize(size);
            p.setTextAlign(align);p.setTypeface(Typeface.create("sans",Typeface.BOLD));c.drawText(s,x,y,p);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            float x=e.getX(), y=e.getY();
            if (e.getAction()==MotionEvent.ACTION_DOWN) {
                if (mode==0) {
                    female = x >= getWidth()/2f;
                    newRun();
                    return true;
                }
                if (mode==3) {
                    mode=0;
                    enemies.clear();shots.clear();enemyShots.clear();particles.clear();
                    return true;
                }
                if (mode==2) {
                    for(int i=0;i<3;i++) {
                        float top=148+i*105;
                        if(y>=top && y<=top+84) { buyUpgrade(i); return true; }
                    }
                    if(y>getHeight()-125) {
                        wave++;
                        startWave();
                        return true;
                    }
                }
            }
            if (mode==1 && (e.getAction()==MotionEvent.ACTION_DOWN || e.getAction()==MotionEvent.ACTION_MOVE)) {
                targetX=Math.max(28,Math.min(getWidth()-28,x));
                targetY=Math.max(105,Math.min(getHeight()-35,y));
                return true;
            }
            return true;
        }
    }

    static final class Enemy {
        float x,y,r,hp,max,speed,dmg,shootCd,contactCd,t,seed;
        int coin,type; boolean boss;
    }
    static final class Shot {
        float x,y,vx,vy,r,dmg; boolean enemy;
    }
    static final class Particle {
        float x,y,vx,vy,r,life,max; int color;
    }
}
