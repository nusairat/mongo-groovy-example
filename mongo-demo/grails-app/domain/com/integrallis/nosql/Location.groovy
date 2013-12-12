package com.integrallis.nosql

import org.bson.types.ObjectId

class Location {
  ObjectId id
  String name
  List coordinates

  def geoFindService

  static mapWith = "mongo"

  static mapping = {
    //database "mydb"
    collection "locations"
    version false
    coordinates geoIndex:true
  }

  static constraints = {
    name()
    coordinates nullable : true
  }

  def beforeInsert() {
println "here !! $name"
    coordinates = geoFindService.findLatLongLocation(name)
println "coordinates :: $coordinates"
  }

  def beforeUpdate() {
    coordinates = geoFindService.findLatLongLocation(name)
  }

  String toString() { name }
}