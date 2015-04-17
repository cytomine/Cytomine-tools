import com.vividsolutions.jts.geom.*
import com.vividsolutions.jts.io.WKTReader
import be.cytomine.client.models.*;
import be.cytomine.client.collections.*;
import be.cytomine.client.*;
import com.vividsolutions.jts.precision.SimpleGeometryPrecisionReducer;
import com.vividsolutions.jts.geom.PrecisionModel;


//map image id and image filename
def imageNameMap = [:]
//map image id and user job id
def userMap = [:]
//contain all result lines
def result = []

println args
new File(args[0]).text.split("\n").each {
    def row = it.split(";")
    println row
    String filename = row[0].trim()
    Long imageId = Long.parseLong(row[1].trim())
    Long userId = Long.parseLong(row[2].trim())

    imageNameMap.put(imageId, filename)
    userMap.put(imageId, userId)     //if two useralgo for 1 image, delete the previous one on the map (request is order by annotations number asc)

}





String cytomineProdPATH = args[1];
String publickey = args[2];
String privatekey = args[3];

Cytomine cytomine = new Cytomine(cytomineProdPATH, publickey, privatekey, "./");

result << "filename;x;y;w;z;allReviewed;contentReviewed;allPredicted;predictedNotReviewed;notPredictedReviewed;reviewedNotModified;reviewedModified"
userMap.each {
    def image = it.key
    def user = it.value
    Long predicted = 20202l
    Long content = 5735l
    try {
        result << image + ";" + imageNameMap.get(image) + ";" + compute(image, predicted, content, user, cytomine)
    } catch (Exception e) {
		Thread.sleep(10000);


		try {
		    result << image + ";" + imageNameMap.get(image) + ";" + compute(image, predicted, content, user, cytomine)
		} catch (Exception exx) {
		    //e.print
			println exx
		    result << image + ";" + imageNameMap.get(image) + "; " + exx
		}
    }
    println result.join("\n")
}

println result.join("\n")





String compute(def image, def predicted, def content, def predictor, def cytomine) {
    println "compute $image"

    //reduce geometry precision => avoid topology exception
    SimpleGeometryPrecisionReducer spr = new SimpleGeometryPrecisionReducer(new PrecisionModel(1024))

    //get all reviewed annotation for the predicted term
    def allReviewed = retrieveAllReviewed(image, predicted, cytomine)
    println "allReviewed ${allReviewed.size()}"

    //get all predicted user or algo annotation for the predicted term by the predicteor
    def allPredicted = retrieveAllPredicted(image, predicted, predictor, cytomine)
    println "allPredicted ${allPredicted.size()}"
    //get all annotation not predicted by reviewed
    def notPredictedReviewed = retrieveNotPredictedReviewed(allReviewed)
    println "notPredictedReviewed ${notPredictedReviewed.size()}"
    //get all annotation predicted by not reviewed
    def predictedNotReviewed = retrievePredictedNotReviewed(allReviewed, allPredicted)
    println "predictedNotReviewed ${predictedNotReviewed.size()}"
    //get all annotation reviewed and modifier / or not modified
    def splitReviewed = splitReviewedModifiedAndNotModified(allReviewed, predictor, cytomine)
    def reviewedNotModified = splitReviewed.reviewedNotModified
    def reviewedModified = splitReviewed.reviewedModified
    println "predictedNotReviewed ${reviewedNotModified.size()}"
    println "reviewedModified ${reviewedModified.size()}"
    //get all annotation reviewed "content" (like a hung)
    def contentReviewed = retrieveAllReviewed(image, content, cytomine)
    println "contentReviewed ${contentReviewed.size()}"


    //get content geometry (example: hung)
    def contentReviewedGeometry = contentReviewed.collect { spr.reduce(new WKTReader().read(it['location'])) }

    //get predicted geometries reviewed (example: tumor)
    def reviewedGeometriesTmp = allReviewed.collect {
        spr.reduce(new WKTReader().read(it['location']))
    }
    reviewedGeometries = []
    reviewedGeometriesTmp.each { geom ->
        if (geom.isValid()) {
            reviewedGeometries << geom
        } else {
            def geombuf = geom.buffer(0)
            for (int i = 0; i < geombuf.getNumGeometries(); i++) {
                def geomSimple = geombuf.getGeometryN(i)
                 if (!(geomSimple instanceof LineString)) {
                    reviewedGeometries << geomSimple
                    
                }
            }
        }
    }

    //filter annotation, only get annotation that intersect a content annotation
    reviewedGeometries = reviewedGeometries.findAll { rev ->
        boolean isInContent = false
        contentReviewedGeometry.each { cont ->
            isInContent = isInContent ? isInContent : cont.intersects(rev)
        }
        return isInContent
    }



    def predictedGeometries = allPredicted.collect {
        def geom = spr.reduce(new WKTReader().read(it['location']))
        geom.isValid() ? geom : geom.buffer(0)
    }
    //filter annotation, only get annotation that intersect a content annotation
    predictedGeometries = predictedGeometries.findAll { rev ->
        boolean isInContent = false
        contentReviewedGeometry.each { cont ->
            isInContent = isInContent ? isInContent : cont.intersects(rev)
        }
        return isInContent
    }

    def reviewedGeometries2 = reviewedGeometries.collect { it.clone() }
    def predictedGeometries2 = predictedGeometries.collect { it.clone() }


    //x = x c'est le pourcentage de tumeur de la couche review bien prédite comme tumeur dans la couche algo;
    def instersectGeometries = []
    reviewedGeometries.each { revgeom ->
        predictedGeometries.each { predictgeom ->
            instersectGeometries << revgeom.intersection(predictgeom)
        }
    }
    instersectGeometries = instersectGeometries.findAll { !it.isEmpty() }
    def x = computeGeometriesArea(instersectGeometries) / computeGeometriesArea(reviewedGeometries)

    //y = c'est le pourcentage de tumeur de la couche review mal classée comme pastumeur dans la couche algo;
    def differenceGeometries = []

    reviewedGeometries.each { revgeom ->
        //for each reveiwed, get the intersection with a predicted (=good prediction) and make difference between the reviewed and good prediction (= bad prediction)
        boolean intersect = false
        def badPrediction = revgeom.clone()
        predictedGeometries.each { predictgeom ->
            def goodPrediction = revgeom.intersection(predictgeom)

            if (!goodPrediction.isEmpty()) {
                intersect = true


                for (int i = 0; i < goodPrediction.getNumGeometries(); i++) {
                    def geomSimple = goodPrediction.getGeometryN(i)
                     if (!(geomSimple instanceof LineString)) {
                        badPrediction = badPrediction.difference(geomSimple)                        
                    }
                }


            }
        }
        differenceGeometries << badPrediction
    }
    differenceGeometries = differenceGeometries.findAll { !it.isEmpty() }
    differenceGeometries = differenceGeometries.unique()

    def y = computeGeometriesArea(differenceGeometries) / computeGeometriesArea(reviewedGeometries)

    //w = c'est le pourcentage de pastumeur de la couche review mal classée comme tumeur dans la couche algo;



    def differenceGeometriesW = []
    predictedGeometries2.each { predictgeom ->
        boolean intersect = false
        def badPrediction = predictgeom.clone()
        reviewedGeometries2.each { revgeom ->
            def goodPrediction
            goodPrediction = predictgeom.intersection(revgeom)
            if (goodPrediction && !goodPrediction.isEmpty()) {
                intersect = true

                for (int i = 0; i < goodPrediction.getNumGeometries(); i++) {
                    def geomSimple = goodPrediction.getGeometryN(i)
                     if (!(geomSimple instanceof LineString)) {
                        badPrediction = badPrediction.difference(geomSimple)                       
                    }
                }
            }
        }
        differenceGeometriesW << badPrediction
    }

    differenceGeometriesW = differenceGeometriesW.findAll { !it.isEmpty() }
    differenceGeometriesW = differenceGeometriesW.unique()

    //compute the content annotation without the reviewed annotation (= not tumor)
    def contentWithoutReviewedPrediction = []
    contentReviewedGeometry.each { contentRev ->
        reviewedGeometries2.each { wellPredicted ->
            contentRev = contentRev.difference(wellPredicted)
        }
        contentWithoutReviewedPrediction << contentRev
    }



    def w = computeGeometriesArea(differenceGeometriesW) / computeGeometriesArea(contentWithoutReviewedPrediction)

    //z = c'est le pourcentage de pastumeur de la couche review bien classée come pastumeur dans la couche algo
    def contentWithoutReviewedPredictionAndPredicted = []
    //compute the content without the reviewed and without the predicted (not tumor well predicted)
    contentWithoutReviewedPrediction.each { contentRev ->
        predictedGeometries2.each { predict ->
            contentRev = contentRev.difference(predict)
        }
        contentWithoutReviewedPredictionAndPredicted << contentRev
    }

    def z = computeGeometriesArea(contentWithoutReviewedPredictionAndPredicted) / computeGeometriesArea(contentWithoutReviewedPrediction)

    println "x=$x"
    println "y=$y"
    println "w=$w"
    println "z=$z"
    println "x+y=" + (x + y)
    println "w+z=" + (w + z)
    return "$x;$y;$w;$z;${allReviewed.size()};${contentReviewed.size()};${allPredicted.size()};${predictedNotReviewed.size()};${notPredictedReviewed.size()};${reviewedNotModified.size()};${reviewedModified.size()}"

}

//compute total area for all geometries
private def computeGeometriesArea(def geometries) {
    def intersectArea = 0
    geometries.each {
        intersectArea = intersectArea + it.area
    }
    return intersectArea
}

//take all reviewed annotation and make 2 list: reviewed modified and reviewed not modified
private def splitReviewedModifiedAndNotModified(def allReviewed, def predictor, def cytomine) {
    //Nbre d'annotations acceptées et non modifiées
    def reviewedNotModified = []
    //Nbre d'annotations acceptées et modifiées
    def reviewedModified = []
    allReviewed.each { reviewed ->
        def parent
        try {
            parent = cytomine.getAnnotation(reviewed.parentIdent)
        } catch (CytomineException e) {
            println e
        }

        if (parent && parent.get('user') == predictor && new WKTReader().read(parent.get('location')).equals(new WKTReader().read(reviewed['location']))) {
            reviewedNotModified << reviewed
        } else if (parent && parent.getLong('user') == predictor) {
            reviewedModified << reviewed
        }
    }
    return [reviewedNotModified: reviewedNotModified, reviewedModified: reviewedModified]
}

//get all predicted annotation that are not reviewed
private def retrievePredictedNotReviewed(def allReviewed, def allPredicted) {
    def reviewedParentsIds = allReviewed.collect { it.getLong('parentIdent') }

    def predictedNotReviewed = []
    allPredicted.each {
        if (!reviewedParentsIds.contains(it['id'])) {
            predictedNotReviewed << it
        }

    }
    predictedNotReviewed
}

//get all reviewed that are not based on a predicted annotation
private def retrieveNotPredictedReviewed(def allReviewed) {
    def notPredictedReviewed = []
    allReviewed.each { reviewed ->
        if (reviewed.getLong('user') == reviewed.getLong('reviewUser')) {
            notPredictedReviewed << reviewed
        }
    }
    notPredictedReviewed

}

//get all reviewed annotation for the image and the term
private def retrieveAllReviewed(Long image, Long term, def cytomine) {
    Map<String, String> filter = new HashMap<String, String>();
    filter.put("image", image + "");
    filter.put("term", term + "");
    filter.put("showBasic", "true");
    filter.put("showMeta", "true");
    filter.put("showWKT", "true");
    filter.put("showTerm", "true");
    filter.put("reviewed", "true");

    AnnotationCollection annotationCollection = cytomine.getAnnotations(filter)
    List list = []
    for (int i = 0; i < annotationCollection.size(); i++) {
        list << annotationCollection.get(i);
    }
    return list
}

//get all algo or user annotation for the image the term and the user
private def retrieveAllPredicted(Long image, Long term, Long user, def cytomine) {
    Map<String, String> filter = new HashMap<String, String>();
    filter.put("image", image + "");
    filter.put("term", term + "");
    filter.put("user", user + "");

    filter.put("showBasic", "true");
    filter.put("showMeta", "true");
    filter.put("showWKT", "true");
    filter.put("showTerm", "true");



    AnnotationCollection annotationCollection = cytomine.getAnnotations(filter)
    List list = []
    for (int i = 0; i < annotationCollection.size(); i++) {
        list << annotationCollection.get(i);
    }
    return list
}
