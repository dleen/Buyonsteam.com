package controllers

import java.util.Date


object forTestPurposes {
  val ttt = "name"                                //> ttt  : java.lang.String = name
  "%"+ttt+"%"                                     //> res0: java.lang.String = %name%
  ttt.isEmpty                                     //> res1: Boolean = false
  
  new Date()                                      //> res2: java.util.Date = Sat Oct 27 16:52:35 PDT 2012
  
}