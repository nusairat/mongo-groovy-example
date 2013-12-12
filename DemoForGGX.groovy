import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DB
import com.mongodb.Mongo
import com.mongodb.gridfs.GridFS

@Grab(group='org.mongodb', module='mongo-java-driver', version='2.11.3')

//db.foo.drop()

def mongo = new Mongo('127.0.0.1')
def db = mongo.getDB('phxjug')

// Collection
def col = db.getCollection("accounts")

// remove
col.remove([] as BasicDBObject)

// now inserts for database
// db.people.insert({ firstName : 'joseph', lastName : 'nusairat', address : { street : '123 Smith St', state : 'AZ' } })
// db.people.insert({ firstName : 'bryan', lastName : 'williams', address : { street : '123 Smith St', state : 'AZ' } })
def record1 = [ firstName : 'joseph', lastName : 'nusairat', amount : 50, active : true, type : 'checking', address : [ street : '2447 East Bell Road', city : 'Phoenix' ] ]
def record2 = [ firstName : 'bryan', lastName : 'williams', amount : 70, active : false, type : 'checking', address : [ street : '1205 South Rural Road', city : 'Tempe' ] ]
def record3 = [ firstName : 'brian', lastName : 'sam-bodden', amount : 30, type : 'checking', address : [ street : '15768 North Pima Road', city : 'Scottsdale' ] ]

col.insert(record1 as BasicDBObject)
col.insert(record2 as BasicDBObject)
col.insert(record3 as BasicDBObject)

// Now lets go ahead and just add a city to the sub collection
col.update([] as BasicDBObject, ['$set' : ['address.state' : 'AZ']] as BasicDBObject, true, true)

// Do an or query
def query = col.find([type : 'checking', '$or' : [ [firstName : 'bryan'], [firstName : 'brian'] ] ] as BasicDBObject)
println query.size()
println "Should retrieve brian and bryan: " + query.collect { "${it.firstName} / ${it.lastName}" }

// Do an exists
query = col.find(['active' : ['$exists' : true]] as BasicDBObject)
println "Should retrieve joseph and bryan: " + query.collect { "${it.firstName} / ${it.lastName}" }
query = col.find(['active' : ['$exists' : false]] as BasicDBObject)
println "Should retrieve brian: " + query.collect { "${it.firstName} / ${it.lastName}" }

// Do an "in" query
query = col.find([firstName : ['$in' : [ 'bryan', 'brian']]] as BasicDBObject)
println "Should retrieve bryan and brian: " + query.collect { "${it.firstName} / ${it.lastName}" }

// Do an "gte" query
query = col.find([amount : ['$gte' : 70]] as BasicDBObject)
println "Should retrieve bryan: " + query.collect { "${it.firstName} / ${it.lastName}" }

// Do an "lte" query
query = col.find([amount : ['$lte' : 69]] as BasicDBObject)
println "Should retrieve joseph and brian: " + query.collect { "${it.firstName} / ${it.lastName}" }

// Do a Count / counting
query = col.getCount([type : 'checking'] as BasicDBObject)
println "The count should be 3 : $query "

// Use skip / limit to do pagination
query = col.find([type : 'checking'] as BasicDBObject).skip(1).limit(2)
println "Skip / Limit test, only 2 " + query.collect { "${it.firstName} / ${it.lastName}" }

// Incrememt test
col.update([firstName : 'joseph'] as BasicDBObject, ['$inc' : ['amount' : 30] ] as BasicDBObject)

// You can limit the amount of items coming back as well
query = col.find([firstName : 'joseph'] as BasicDBObject, [amount : 1] as BasicDBObject)
println "Incremebt 50 + 30 to 80 " + query.collect { it }


// Map / Reduce

// Geo Location
// Lets first apply Encoding to the database
def index=new BasicDBObject()
index.put("loc","2d")
col.ensureIndex(index)
// via db db.accounts.ensureIndex( { loc : "2d" , category : 1 } )

// Now update locations in our database
col.find([] as BasicDBObject, [address : 1] as BasicDBObject).each {
    def ll = findFromAddress(it.address['address'], it.address['city'], it.address['state'], null)
    col.update(['_id' : it['_id']] as BasicDBObject, ['$set' : ['loc' : ll]] as BasicDBObject, true, true)
}


// 2625 West Baseline Road, Tempe AZ
http://maps.googleapis.com/maps/api/geocode/json?address=${address.toGoogleMapString()}&sensor=false
// 1.7 added some more sphereical accuracy
def address = '2625 West Baseline Road'
def city = 'Tempe'
def state = 'AZ'
def zipCode = null
def phxjugAdd = findFromAddress(address,city,state,zipCode)
println "before"
// you can add maxDistance as well
query = col.find(['loc' : ['$near' : phxjugAdd]  ] as BasicDBObject).limit(1)
println "Should find Bryan " + query.collect { "${it.firstName} / ${it.lastName}" }


// using the GEO Near Command Instead
query = db.command([geoNear : 'accounts', near : phxjugAdd ] as BasicDBObject)
//println "Query $query"
query['results'].each {
 println "Distance from ${it['obj']['firstName']} is ${it['dis']}"
}
// radius of the earth (about 6371 km or 3959 miles)


// Now to save the file out - Grid FS
def gridfs = new GridFS(db)
def inputFile = gridfs.createFile(new File('in/struts.png'))
inputFile.save()
println "Out > " + inputFile

// u can also find by fileName as well
def fileOut = gridfs.find(inputFile.getId())
println "File Out :: $fileOut"
// write out the file
fileOut.writeTo(new File("out/${fileOut.getFilename()}"))

// Also can retrieve and get via input/outputstreams


// MAP REDUCE
def mapReduce = col.mapReduce("""
    function map() {
        // emit is a necssary call to basically call the next result
        // first value is the differentiator ... second is the aggregator
        emit(1, {count : this.amount});
    }
    """,
    """
    function reduce(key, values) {
        var count = 0
        for (var i = 0; i < values.length; i++)
            count += values[i].count
        return {count: count}
    }
    """,
    "result",
    [type : 'checking'] as BasicDBObject)
println "mapReduce  > $mapReduce "
// Can only have one or the other
//mapReduce.results().each { println it}
println "mapReduce2 > " + mapReduce.results()[0]['value']['count']

// ************************************************************************************
// ************************************************************************************
// ************************************************************************************
// ************************************************************************************
def findFromAddress(address, city, state, zipCode) {
    def fullAddress = [address, city, state + ' ' + (zipCode ?: '')].findAll { it != null || it?.trim()?.length() > 0}.join(',')
    def encodedAddress = java.net.URLEncoder.encode(fullAddress, 'utf8')
    // get the coordinates
    def latlong = findLatLongLocation(encodedAddress)
//    println "Lat long : $latlong"
    latlong
}

def findLatLongLocation(def address) {
  try {
      def url = new URL("http://maps.googleapis.com/maps/api/geocode/xml?address=${address}&sensor=false").text
      def GeocodeResponse = new XmlParser().parseText(url)
      def latlongs = GeocodeResponse.result.geometry.location
      return [latlongs[0]?.lat?.text()?.toDouble(), latlongs[0].lng?.text()?.toDouble()]
      /*
      def location = JSON.parse(url)

      // now lets go the GEOTag off of it
      def latlongs = location['results']['geometry']['location']
      if (latlongs) {
        def latlong = latlongs[0]
        return [latlong['lat'],latlong['lng']]
      }
      */
  } catch(java.net.UnknownHostException e) {
      log.info("Unknown host : ${e.getMessage()}")
    }
    []
}