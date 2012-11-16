object testing {
  
  val dec = 1000*60*60*24/3                       //> dec  : Int = 28800000
  
  val t = 1 -> 2 :: List()                        //> t  : List[(Int, Int)] = List((1,2))
  2 -> 3 :: t                                     //> res0: List[(Int, Int)] = List((2,3), (1,2))
}