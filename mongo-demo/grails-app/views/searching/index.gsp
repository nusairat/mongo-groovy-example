<!DOCTYPE html>
<html>
  <head>
    <meta name="layout" content="main2"/>
    <title>Searching</title>
  </head>
  <body>
    <div id="status" role="complementary">
      <h1>Search Locations</h1>
      <g:each in="${locations}" var="location">
          Found : ${location.name}<br/>
        </g:each>
      <g:form action="findLocations" method="get">
        Location : <g:textField name="loc" value="${params.loc}"/>
        <g:submitButton name="Search..."/><br/>
      </g:form>

      <h1>Find Deals</h1>
      <g:if test="${deal}">
        Found Deal : ${deal.name}<br/>
      </g:if>
      <g:form action="findDeals" method="get">
        Location : <g:textField name="locDeal" value="${params.locDeal}"/>
        <g:submitButton name="Search..."/><br/>
      </g:form>

      <hr>
    </div>
  </body>
</html>
