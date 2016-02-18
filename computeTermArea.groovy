/*

groovy -cp "/tmp/cytomine-java-client-1.1.1.jar" computeTermArea.groovy 8049106 7483 http://demo.cytomine.be yyyy xxx 528038,528044 528460,528132

 */

import be.cytomine.client.models.*;
import be.cytomine.client.collections.*;
import be.cytomine.client.*;
import java.nio.file.Files;


println "computeTermArea : start"

println args

Long idJob = Long.parseLong(args[0])
String userjob = args[1]
String host= args[2]
String publickey = args[3]
String privatekey= args[4]
String[] idsTerm = args[5].split(",")
String[] idsImages= args[6].split(",")
def terms = []
def images = []
def datas = []
def file = new File('/tmp/output.txt')
file.write ''

println "Job=$idJob"

Cytomine cytomine = new Cytomine(host, publickey, privatekey);

cytomine.changeStatus(idJob,Cytomine.JobStatus.RUNNING,0)

println "Get terms"

idsTerm.each {
    Term term = cytomine.getTerm(Long.parseLong(it))
    terms << term
}

Thread.sleep(10)
cytomine.changeStatus(idJob,Cytomine.JobStatus.RUNNING,10)

println "Get images"

idsImages.each {
    ImageInstance image = cytomine.getImageInstance(Long.parseLong(it))
    images << image
}

Thread.sleep(10)
cytomine.changeStatus(idJob,Cytomine.JobStatus.RUNNING,20)

terms.each { term ->
    images.each { image ->

        def filters = [:]
        filters.put("reviewed","true");
        filters.put("term",term.getId()+"");
        filters.put("image",image.getId()+"");
        filters.put("showGIS","true");
        filters.put("showMeta","true");
        filters.put("showTerm","true");
        AnnotationCollection annotations = cytomine.getAnnotations(filters);

        
        for(int i=0;i<annotations.size();i++) {
            datas << [image:image, term:term, created:new Date (annotations.get(i).get('created').longValue()), area:annotations.get(i).getDbl('area')]
        }


    }
}

Thread.sleep(10)
cytomine.changeStatus(idJob,Cytomine.JobStatus.RUNNING,50)



file << ";;Summarize Area;\n"
file << ";;Image;"
for(int i=0; i<terms.size(); i++){
    file << terms[i].getStr('name')+";"
}
file << "Total;\n"

for(int i=0; i<images.size(); i++){
    file << ";;"+images[i].getStr('instanceFilename')+";"
    def datasByImage = datas.findAll{it.image == images[i]}
    for(int j=0; j<terms.size(); j++){
        def totArea = datasByImage.findAll{it.term == terms[j]}.collect{it.area}.sum()?:0
        file << totArea +";"
    }
    def totArea = datasByImage.collect{it.area}.sum()?:0
    file << totArea+";\n"
}

file << ";;Total;"
for(int i=0; i<terms.size(); i++){
    def totArea = datas.findAll{it.term == terms[i]}.collect{it.area}.sum()?:0
    file << totArea +";"
}
def totArea = datas.collect{it.area}.sum()?:0
file << totArea+";\n"


for(int i=0; i<4; i++){
    file << ";;;\n"
}


file << ";;Summarize Number;\n"
file << ";;Image;"
for(int i=0; i<terms.size(); i++){
    file << terms[i].getStr('name')+";"
}
file << ";;Total;\n"

def totNumber;

for(int i=0; i<images.size(); i++){
    file << ";;"+images[i].getStr('instanceFilename')+";"
    def datasByImage = datas.findAll{it.image == images[i]}
    for(int j=0; j<terms.size(); j++){
        totNumber = datasByImage.findAll{it.term == terms[j]}.size()
        file << totNumber +";"
    }
    totNumber = datasByImage.size()
    file << totNumber+";\n"
}

file << "Total;"
for(int i=0; i<terms.size(); i++){
    totNumber = datas.findAll{it.term == terms[i]}.size()
    file << totNumber +";"
}
totNumber = datas.size()
file << totNumber+";\n"


for(int i=0; i<4; i++){
    file << ";;;\n"
}


file << ";;Ratio Data;\n"
file << ";;Image;"
for(int i=0; i<terms.size(); i++){
    file << terms[i].getStr('name')+";"
}
file << "Total;\n"

for(int i=0; i<images.size(); i++){
    file << ";;"+images[i].getStr('instanceFilename')+";"
    def datasByImage = datas.findAll{it.image == images[i]}
    def totAreaByImage = datasByImage.collect{it.area}.sum()?:0
    for(int j=0; j<terms.size(); j++){
        def termArea = datasByImage.findAll{it.term == terms[j]}.collect{it.area}.sum() ?:0
        def ratio = totAreaByImage == 0.0 ? 0 : (termArea / totAreaByImage)
        file << ratio +";"
    }
    def ratio = 1
    file << ratio+";\n"
}


for(int i=0; i<4; i++){
    file << ";;;\n"
}


file << ";;Details;\n"

for(int i=0; i<images.size(); i++){

    file << ";;************************************************************************;\n"
    file << ";;Image "+(i+1)+";\n"
    file << ";;"+images[i].getStr('instanceFilename')+";\n"
    file << ";;************************************************************************;\n"
    for(int k=0; k<4; k++){ file << ";;;\n"}

    for(int j=0; j<terms.size(); j++){
        for(int k=0; k<4; k++){ file << ";;;\n"}
        file << ";;#######################;\n"
        file << ";;"+terms[j].getStr('name')+";\n"
        file << ";;Created;Area;\n"
        datas.findAll{it.term == terms[j] && it.image == images[i]}.each {
            file << ";;"+it.created+";"+it.area+";\n"
        }
        file << ";;;\n"	

        def numberAnnot = datas.findAll{it.term == terms[j] && it.image == images[i]}.size()
        totArea = datas.findAll{it.term == terms[j] && it.image == images[i]}.collect{it.area}.sum()?:0
        def meanArea = numberAnnot == 0.0 ? 0 : (totArea / numberAnnot)

        file << ";;Annotations number;"+numberAnnot+";\n"
        file << ";;Average Area;"+meanArea+";\n"
        file << ";;Total Area;"+totArea+";\n"
    }
    
    for(int k=0; k<4; k++){ file << ";;;\n"}


}


println "Upload job data"
JobData jobdata = cytomine.addJobData(idJob+"", idJob, "report.csv")
cytomine.uploadJobData(jobdata.getId(), Files.readAllBytes(file.toPath()))

Thread.sleep(10)
cytomine.changeStatus(idJob,Cytomine.JobStatus.SUCCESS,100)

println "END"

