<p:declare-step version="3.0" xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                name="main">
  <p:import href="https://xmlcalabash.com/ext/library/trang.xpl"/>
  <p:import href="https://xmlcalabash.com/ext/library/pebble.xpl"/>
<!--
  <p:output port="result" sequence="true"
            serialization="map{'method':'xml','indent':true()}"/>
-->
  <p:option name="path" as="xs:string" required="true"/>
  <p:option name="schema" as="xs:string" required="true"/>
  <p:option name="release" as="xs:string" required="true"/>
  
  <!-- Cleanup any previous run -->
  <p:file-delete name="del" href="{$path}/build/tmp/src/{$schema}" recursive="true"/>
  <p:file-mkdir name="mkdir" depends="del" href="{$path}/build/tmp/src/{$schema}"/>

  <cx:trang-files name="rnc-to-rng" p:depends="mkdir"
                  source-schema="{$path}/src/main/{$schema}/{$schema}.rnc"
                  result-schema="{$path}/build/tmp/src/{$schema}/{$schema}.rng"/>
  <p:sink/>

  <p:xslt depends="rnc-to-rng">
    <p:with-input port="source" href="{$path}/build/tmp/src/{$schema}/{$schema}.rng"/>
    <p:with-input port="stylesheet" href="include.xsl"/>
  </p:xslt>

  <p:xslt>
    <p:with-input port="stylesheet" href="augment.xsl"/>
  </p:xslt>

  <p:xslt>
    <p:with-input port="stylesheet" href="cleanup.xsl"/>
  </p:xslt>

  <p:xslt name="nodoc"
          parameters="map { 'remove-schematron': 0 }">
    <p:with-input port="stylesheet" href="removedoc.xsl"/>
  </p:xslt>

  <p:xslt>
    <p:with-input port="stylesheet" href="schematron.xsl"/>
  </p:xslt>

  <p:store href="{$path}/build/tmp/{$schema}/sch/{$schema}.sch"
           serialization="map{'method':'xml', 'indent':false()}"/>

  <cx:pebble name="copyright" 
             context="map { 'release': $release }">
    <p:with-input href="{$path}/src/main/xml/{$schema}.xml"/>
  </cx:pebble>

  <p:variable name="copyright" as="document-node()" select="/"/>

  <p:xslt name="rng-schema">
    <p:with-input pipe="@nodoc"/>
    <p:with-input port="stylesheet" href="attach-copyright.xsl"/>
    <p:with-option name="parameters" select="map {
      'copyright': $copyright
      }"/>
  </p:xslt>

  <cx:trang name="rng-to-rnc" result-format="rnc"
            output-base-uri="file:/{$path}/build/tmp/{$schema}/rng/{$schema}/"/>

  <p:store href="{$path}/build/tmp/{$schema}/rng/{$schema}.rnc"
           serialization="map{'method':'text'}"/>

  <cx:trang result-format="rng"
            output-base-uri="file:/{$path}/build/tmp/{$schema}/rng/{$schema}/">
    <p:with-input pipe="@rng-to-rnc"/>
  </cx:trang>

  <p:store href="{$path}/build/tmp/{$schema}/rng/{$schema}.rng"
           serialization="map{'method':'xml', 'indent':false()}"/>

  <p:sink/>

</p:declare-step>
