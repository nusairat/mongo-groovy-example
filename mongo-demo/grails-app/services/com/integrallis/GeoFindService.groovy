package com.integrallis

/**
 * Finds the geo coordinates for a location.
 */
class GeoFindService {

  def findFromAddress(address) {
    def encodedAddress = java.net.URLEncoder.encode(address, 'utf8')
    // get the coordinates
    def latlong = findLatLongLocation(encodedAddress)
    log.info "Lat long : $latlong"
    latlong
  }

  def findFromAddress(address, city, state, zipCode) {
    def fullAddress = [address, city, state + ' ' + (zipCode ?: '')].findAll { it != null || it?.trim()?.length() > 0}.join(',')
    def encodedAddress = java.net.URLEncoder.encode(fullAddress, 'utf8')
    // get the coordinates
    def latlong = findLatLongLocation(encodedAddress)
    log.info "Lat long : $latlong"
    latlong
  }

  def findLatLongLocation(def address) {
    try {
        def url = new URL("http://maps.googleapis.com/maps/api/geocode/xml?address=${address}&sensor=false").text
        def GeocodeResponse = new XmlParser().parseText(url)
        def latlongs = GeocodeResponse.result.geometry.location
        return [latlongs[0]?.lat?.text()?.toDouble(), latlongs[0].lng?.text()?.toDouble()]
    } catch(java.net.UnknownHostException e) {
        log.error "Unknown host : ${e.getMessage()}", e
    }
    []
  }
}