<p:declare-step version="3.0" xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                name="main">
  <p:output port="result" sequence="true"
            serialization="map{'method':'xml','indent':true()}"/>
  <p:option name="path" as="xs:string" required="true"/>
  <p:option name="schema" as="xs:string" required="true"/>
  <p:option name="title" as="xs:string" required="true"/>
  <p:option name="version" as="xs:string" required="true"/>
  <p:option name="prefix" as="xs:string" select="'xml'"/>

  <p:directory-list path="{$path}/build/{$schema}-{$version}" max-depth="unbounded">
    <p:with-option name="exclude-filter" select="('catalog.xml', 'release-notes/.*')"/>
  </p:directory-list>

  <p:add-xml-base relative="false"/>

  <p:xslt parameters="map { 'path': 'file:' || $path || '/build/'
                                    || $schema || '-' || $version || '/',
                            'version': $version,
                            'title': $title,
                            'prefix': $prefix }">
    <p:with-input port="stylesheet" href="catalog.xsl"/>
  </p:xslt>
</p:declare-step>
