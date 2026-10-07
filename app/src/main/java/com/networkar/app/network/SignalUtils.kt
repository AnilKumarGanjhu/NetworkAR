package com.networkar.app.network
object SignalUtils { fun percent(dbm:Int)=((dbm+100).coerceIn(0,60)*100/60); fun quality(dbm:Int)=when{dbm>=-60->"STRONG";dbm>=-75->"MEDIUM";else->"WEAK"} }
