<p:declare-step version="3.0" xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="#all"
                name="main">
  <p:input port="source"/>
  <p:output port="result" 
            serialization="map{'method':'xml', 'indent':true()}"/>
  <p:option name="buildhash" as="xs:string" required="true"/>

  <p:xslt>
    <p:with-input port="stylesheet" href="include.xsl"/>
  </p:xslt>

  <p:xslt>
    <p:with-input port="stylesheet" href="augment.xsl"/>
  </p:xslt>

  <p:xslt>
    <p:with-input port="stylesheet" href="cleanup.xsl"/>
  </p:xslt>

  <p:xslt parameters="map{'buildhash': $buildhash}">
    <p:with-input port="stylesheet" href="rngdocxml.xsl"/>
  </p:xslt>

</p:declare-step>
