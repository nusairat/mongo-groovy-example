package com.integrallis

import com.integrallis.nosql.*
import com.mongodb.BasicDBObject
import com.mongodb.Mongo

class SearchingController {

  def geoFindService

  def index() { }

  def findLocations(String loc) {
    def coordinates = geoFindService.findLatLongLocation(loc)
    def locations = Location.findByCoordinatesNear(coordinates)

    render view : 'index', model : [locations : locations]
  }

  def findDeals(String locDeal) {
    def mongo = new Mongo('127.0.0.1')
    def db = mongo.getDB('ggxItems')
    // Collection
    def col = db.getCollection("items")

    // Necessary sinces its an embedded set of cooridnates
    def index=new BasicDBObject()
    index.put("locations.coordinates","2d")
    col.ensureIndex(index)
    // STOP

    def coordinates = geoFindService.findLatLongLocation(locDeal)

    def query = col.find(['locations.coordinates' : ['$near' : coordinates] ]  as BasicDBObject)
    def list = query.collect { it } as List
    render view : 'index', model : [deal : list?.get(0)]
  }
}