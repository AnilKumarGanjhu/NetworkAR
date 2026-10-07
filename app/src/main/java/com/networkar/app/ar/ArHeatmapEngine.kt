package com.networkar.app.ar
import kotlin.math.hypot
data class HeatPoint(val x:Float,val z:Float,val dbm:Int,val quality:String)
class ArHeatmapEngine{fun best(p:List<HeatPoint>)=p.maxByOrNull{it.dbm};fun interpolate(p:List<HeatPoint>,x:Float,z:Float):Float{if(p.isEmpty())return -100f;var n=0.0;var d=0.0;for(q in p){val dist=hypot((q.x-x).toDouble(),(q.z-z).toDouble()).coerceAtLeast(.05);val w=1/(dist*dist);n+=w*q.dbm;d+=w};return(n/d).toFloat()}}
