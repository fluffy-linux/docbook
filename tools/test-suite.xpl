<p:declare-step version="3.0" xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:t="http://docbook.org/ns/tests"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:xvrl="http://www.xproc.org/ns/xvrl"
                exclude-inline-prefixes="#all"
                name="main">
<!--
  <p:output port="result" sequence="true"
            serialization="map{'method':'xml','indent':true()}"/>
-->
  <p:option name="path" as="xs:string" required="true"/>
  <p:option name="rngschema" as="xs:string" required="true"/>
  <p:option name="schschema" as="xs:string" required="true"/>
  <p:option name="report" as="xs:string" required="true"/>

  <p:load message="RELAX NG validation with {$rngschema}"
          name="relaxng" href="{$rngschema}"/>

  <p:load message="Schematron validation with {$schschema}"
          name="schematron" href="{$schschema}"/>

  <p:directory-list path="{$path}" include-filter=".*.xml$"/>
  <p:add-xml-base relative="false"/>

  <p:for-each name="loop">
    <p:with-input select="//c:file"/>
    <p:variable name="filename" select="tokenize(/c:file/@xml:base, '/')[last()]"/>
    <p:load href="{/c:file/@xml:base}"/>
    <p:if name="test-case" test="/t:test-case/t:document">
      <p:unwrap match="/t:document">
        <p:with-input select="/t:test-case/t:document"/>
      </p:unwrap>

      <p:choose name="rngtest">
        <p:when test="/t:test-case/@relaxng">
          <p:with-input pipe="@test-case"/>
          <p:variable name="expected" select="/t:test-case/@relaxng/string()">
            <p:pipe step="test-case"/>
          </p:variable>
          <p:validate-with-relax-ng name="vrng" assert-valid="false">
            <p:with-input port="schema" pipe="@relaxng"/>
          </p:validate-with-relax-ng>
          <p:variable name="valid"
                      select="if (/xvrl:report/xvrl:digest/@valid = 'true')
                              then 'pass' else 'fail'">
            <p:pipe port="report"/>
          </p:variable>
          <p:choose>
            <p:when test="$expected = $valid">
              <p:identity>
                <p:with-input>
                  <pass test="relaxng" actual="{$valid}" expected="{$expected}"/>
                </p:with-input>
              </p:identity>
            </p:when>
            <p:otherwise>
              <p:insert position="first-child">
                <p:with-input>
                  <fail test="relaxng" actual="{$valid}" expected="{$expected}"/>
                </p:with-input>
                <p:with-input port="insertion" pipe="report@vrng"/>
              </p:insert>
            </p:otherwise>
          </p:choose>
        </p:when>
        <p:otherwise>
          <p:identity>
            <p:with-input>
              <skip test="relaxng"/>
            </p:with-input>
          </p:identity>
        </p:otherwise>
      </p:choose>

      <p:choose name="schtest">
        <p:when test="/t:test-case/@schematron">
          <p:with-input pipe="@test-case"/>
          <p:variable name="expected" select="/t:test-case/@schematron/string()">
            <p:pipe step="test-case"/>
          </p:variable>
          <p:validate-with-schematron name="vsch" 
                                      assert-valid="false"
                                      report-format="xvrl">
            <p:with-input port="source" pipe="@test-case"/>
            <p:with-input port="schema" pipe="@schematron"/>
          </p:validate-with-schematron>
          <p:variable name="valid"
                      select="if (/xvrl:report/xvrl:digest/@valid = 'true')
                              then 'pass' else 'fail'">
            <p:pipe port="report"/>
          </p:variable>
          <p:choose>
            <p:when test="$expected = $valid">
              <p:identity>
                <p:with-input>
                  <pass test="schematron" actual="{$valid}" expected="{$expected}"/>
                </p:with-input>
              </p:identity>
            </p:when>
            <p:otherwise>
              <p:insert position="first-child">
                <p:with-input>
                  <fail test="schematron" actual="{$valid}" expected="{$expected}"/>
                </p:with-input>
                <p:with-input port="insertion" pipe="report@vsch"/>
              </p:insert>
            </p:otherwise>
          </p:choose>
        </p:when>
        <p:otherwise>
          <p:identity>
            <p:with-input>
              <skip test="schematron"/>
            </p:with-input>
          </p:identity>
        </p:otherwise>
      </p:choose>

      <p:wrap-sequence wrapper="test-case">
        <p:with-input pipe="@rngtest @schtest"/>
      </p:wrap-sequence>
      <p:add-attribute attribute-name="href" attribute-value="{$filename}"/>
      
      <p:choose>
        <p:when test="//fail">
          <p:identity message="FAIL {$filename}"/>
        </p:when>
        <p:otherwise>
          <p:identity/>
        </p:otherwise>
      </p:choose>
    </p:if>
  </p:for-each>

  <p:wrap-sequence wrapper="test-report"/>

  <p:store href="{$report}" serialization="map{'method':'xml','indent':true()}"/>

  <p:if test="//fail">
    <p:error code="failing-tests"/>
  </p:if>

  <p:sink/>

</p:declare-step>
