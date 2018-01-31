<?xml version="1.0" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:param name="lang" select="'en'"/>
<xsl:output method="html" indent="yes"/>

<xsl:template match="/" >
<html>
 <head><title>
  <xsl:choose>
   <xsl:when test='$lang="fr"'>Historique Jmol</xsl:when>
   <xsl:when test='$lang="nl"'>Jmol Changes</xsl:when>
   <xsl:otherwise>Jmol History</xsl:otherwise>
  </xsl:choose>
 </title></head>
 <body bgcolor="#ffffff">
  <center><img src="../app/images/Jmol_logo.jpg" alt="Jmol logo" /></center>

  <h2><xsl:choose>
   <xsl:when test='$lang="fr"'>Liste des modifications de Jmol:</xsl:when>
   <xsl:when test='$lang="nl"'>Veranderingen:</xsl:when>
   <xsl:otherwise>List of changes to Jmol:</xsl:otherwise>
  </xsl:choose></h2>

  <p><xsl:choose>
   <xsl:when test='$lang="fr"'>Participants:</xsl:when>
   <xsl:when test='$lang="nl"'>Medewerkers:</xsl:when>
   <xsl:otherwise>Contributors:</xsl:otherwise>
  </xsl:choose></p>
  <blockquote>
<xsl:text>
</xsl:text>
   <xsl:for-each select="history/contributors/contributor" >
    <xsl:value-of select="@id" /> = <xsl:value-of select="name" />
    (<a href="mailto:{email}"><xsl:value-of select="email" /></a>)<br/>
<xsl:text>
</xsl:text>
  </xsl:for-each></blockquote>

  <xsl:for-each select="history/changes" >
   <h3><xsl:value-of select="@version" /></h3>
   <ul><xsl:for-each select="change" ><li><xsl:value-of select="." />
    <xsl:if test="@contributor">(<xsl:value-of select="@contributor" />)</xsl:if>
   </li></xsl:for-each></ul>
  </xsl:for-each>

 </body>
</html>

</xsl:template>

</xsl:stylesheet>
