import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.*

def location = new WKTReader().read(new File("a.txt").text)

println location

println location.isValid()
