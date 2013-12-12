package com.integrallis.nosql

import org.bson.types.ObjectId

class Item {
  ObjectId id
  String name
  BigDecimal price
  String description

  static mapWith = "mongo"

  List locations
  static hasMany = [locations : Location]

  static embedded = ['locations']

  static mapping = {
    //database "mydb"
    version false
    collection "items"
  }

  static constraints = {
    name(blank : false)
    price()
    description(nullable : true, maxSize: 300)
  }
}