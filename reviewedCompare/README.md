# Cytomine Review script

Compute stats (count, gis,...) for reviewed layer and 1 job layer.
It build a confusion matrix where

 > > > > >                 prédittumeur                préditpastumeur
 > > > > > tumeur                x                        y
 > > > > > pastumeur        w                        z
 > > > > >
 > > > > >
 > > > > > où x c'est le pourcentage de tumeur de la couche review bien prédite
 > > > > > comme tumeur dans la couche algo;
 > > > > > y c'est le pourcentage de tumeur de la couche review mal classée comme
 > > > > > pastumeur dans la couche algo;
 > > > > > w c'est le pourcentage de pastumeur de la couche review mal classée
 > > > > > comme tumeur dans la couche algo;
 > > > > > z c'est le pourcentage de pastumeur de la couche review bien classée
 > > > > > come pastumeur dans la couche algo

Create a txt file with 1 line for each image to analyze. Eeach line muste be

    filename ; idimage ; iduserjob

To create this file, you can use the SQL request

    SELECT ai.original_filename as filename, ii.id as ident, u.id, count(*) as annotations
    FROM abstract_image ai, image_instance ii, reviewed_annotation ra,algo_annotation aa, algo_annotation_term aat, sec_user u
    WHERE ii.base_image_id = ai.id
    AND aa.image_id = ii.id
    AND aa.user_id = u.id
    AND ra.parent_ident = aa.id
    AND aat.annotation_ident = aa.id
    AND aat.term_id = TERM_ID
    AND ii.project_id = PROJECT_ID
    GROUP by ai.original_filename, ii.id,u.id
    ORDER BY annotations asc;
    
where PROJECT_ID  is the image project and TERM_ID is the term use for the user job
PGP = 7873585
Adéno = 20202

The result looks like this:

    PGP POUMON PGP10 1 - 2012-08-07 12.44.21.jp2  | 7888418 | 8730358 |         279
    PGP POUMON PGP10 11 - 2012-08-07 12.47.03.jp2 | 7888428 | 8723520 |         170
    PGP POUMON PGP10 31 - 2012-08-07 12.52.42.jp2 | 7888436 | 8718255 |         165
    PGP POUMON PGP9 11 - 2012-08-07 12.33.04.jp2  | 7888452 | 8712431 |         193
    
where | should be replaced by ;


To run this script use:

    groovy -cp "Cytomine-Java-Client.jar:JTS.jar" reviewedCompare.groovy inputFile.txt http://...cytomine.be PUBLIC_KEY PRIVATE_KEY

E.g. 

    groovy -cp "/home/lrollus/git/Core/algo/computeAnnotationStats/Cytomine-Java-Client.jar:/home/lrollus/git/Core/algo/computeAnnotationStats/jts-1.13.jar" reviewedCompare.groovy /home/lrollus/imageGIS.txt

