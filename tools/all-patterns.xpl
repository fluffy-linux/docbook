<p:declare-step version="3.0" xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="#all"
                name="main">
  <p:input port="source"/>
  <p:output port="result" 
            serialization="map{'method':'xml','indent':true()}"/>

  <p:xslt>
    <p:with-input port="stylesheet" href="all-patterns.xsl"/>
  </p:xslt>

</p:declare-step>
