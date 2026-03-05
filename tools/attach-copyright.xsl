<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="3.0">
  
<xsl:output method="xml" encoding="utf-8" indent="no"/>
<xsl:preserve-space elements="*"/>

<xsl:param name="copyright" as="document-node()"/>

<xsl:mode on-no-match="shallow-copy"/>

<xsl:template match="/*">
  <xsl:copy>
    <xsl:copy-of select="@*"/>
    <xsl:text>&#10;&#10;</xsl:text>
    <xsl:comment><xsl:value-of select="$copyright"/></xsl:comment>
    <xsl:text>&#10;&#10;</xsl:text>
    <xsl:apply-templates/>
  </xsl:copy>
</xsl:template>

</xsl:stylesheet>
