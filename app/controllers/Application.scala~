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

  val textResults = returnPageN(1, steamStoreHead)

  def index = Action {
    Ok(views.html.index(textResults))
  }

}