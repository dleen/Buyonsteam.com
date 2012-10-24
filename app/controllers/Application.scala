package controllers

import play.api._
import play.api.mvc._

// jsoup html parser
import org.jsoup._

/* Allows us to treat Java objects as Scala objects.
   The main use is that jsoup.select.Elements is an instantiation
   of java.util.List[Element]. So this collection allows us
   to use the Scala map function on a Java list. */
import scala.collection.JavaConversions._

object Application extends Controller {

  class Game(Name: String , Price: String, Date: String) 
    {
      
    }

  val steamStoreHead =
    "http://store.steampowered.com/search/?snr=1_4_4__12&term=#sort_by=&sort_order=ASC&page="

  val doc = Jsoup.connect(steamStoreHead + "1").get()
  val listedPageNumbers = doc.getElementsByClass("search_pagination_right").select("a").text

  val pageNums = listedPageNumbers.split(' ')

  val finalPage = pageNums(2).toInt

  def returnPageN(pageN: Int, urlHead: String): List[String] = {
    val doc = Jsoup.connect(urlHead + pageN.toString).get()
    val searchResults = doc.getElementsByClass("search_result_row")

    searchResults.map(_.select("a[href]").text).toList
  }

/* Data is returned as a List of Lists. 
 * A List contains either names, prices, release dates or image data.
 * */
  def returnPageNList(pageN: Int, urlHead: String): List[List[String]] = { 
    val doc = Jsoup.connect(urlHead + pageN.toString).get()
    val searchResults = doc.getElementsByClass("search_result_row")

    val searchPrices = searchResults.map(_.select("div.search_price").text).toList
    val searchReleases = searchResults.map(_.select("div.search_released").text).toList
    val searchNames = searchResults.map(_.select("div.search_name").text).toList
// TODO - Store information for holding image data - width, height and src    
//val searchPrices = searchResults.map(_.select("div.search_").text).toList                                                                                                          
    
    List(searchNames, searchPrices, searchReleases)
   /* List(searchNames, searchPrices, searchReleases, searchPrices)*/
  }


  def returnPageNGame(pageN: Int, urlHead: String): List[Game] = { 
    val doc = Jsoup.connect(urlHead + pageN.toString).get()
    val searchResults = doc.getElementsByClass("search_result_row")

    val games = List()

    for( price <- searchResults.map(_.select("div.search_price").text);
	 date <- searchResults.map(_.select("div.search_released").text);
	 name <- searchResults.map(_.select("div.search_name").text)
      ) { games  ++ List(new Game(name,date,price))  }
 
   games

  }


  val textResults = returnPageN(1, steamStoreHead)

  def index = Action {
    Ok(views.html.index(textResults))
  }

}
