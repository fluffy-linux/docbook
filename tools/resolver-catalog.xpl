<p:declare-step version="3.0" xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="#all"
                name="main">
  <p:input port="source"/>
  <p:output port="result"/>
  <p:option name="schema" as="xs:string" required="true"/>
  <p:option name="version" as="xs:string" required="true"/>

  <p:xslt parameters="map { 'schema': $schema, 'version': $version }">
    <p:with-input port="stylesheet" href="resolver-catalog.xsl"/>
  </p:xslt>

</p:declare-step>
