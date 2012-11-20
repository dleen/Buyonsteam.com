package viewhelpers

object ViewHelp {

def nicePrice(price: Double) = {
	val dot = price.toString.indexOf('.')
	val np  = price.toString + "0000000"

	if (dot == 1) np.slice(0,4)
	else if (dot == 2) np.slice(0,5)
	else if (dot == 3) np.slice(0,6)
	else "0"
	}
	
}