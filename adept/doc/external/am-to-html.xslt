<!--
  ~ Copyright 2016 SRI International
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html"/>
    <xsl:param name="am-title">Action Model</xsl:param>
    <xsl:param name="action-filter">implemented</xsl:param>
    <xsl:param name="file-name"/>
    <xsl:variable name="not-implemented-tag">Not-yet-implemented:</xsl:variable>
    <xsl:key name="action" match="action" use="@id"/>
    <xsl:key name="heir" match="action" use="inherit/@parent"/>
    
    <xsl:template name="top-level" match="/actionModel">
      <html>
        <head>
            <title>
                <xsl:value-of select="$am-title"/>
            </title>
            <style>
              <![CDATA[
    table.param-list, table.param-list tr td, table.param-list tr th{
      border: 1px solid black;
    }
    
    table.param-list tr td{
      padding: 1ex;
    }
    
    table.param-list tr th{
      text-transform: capitalize;
      background: #dddddd;
    }
    
    table.param-list{
      cell-spacing: 0;
    }
    
    .action-header{
      border: 1px solid black;
      background: #eeeeee;
      width: 95%;
      padding: 1ex;
      margin-top: 3em;
      font-size: larger;
      font-weight: bold;
    }
    
    .action-defined-in{
      
    }
    
    div.am-toc{
      border: 2px solid gray;
      float: right;
      margin-left: 1em;
      background: #eeeeee;
      padding: 1ex;
    }
    
    div.am-toc div{
      margin-bottom: .7ex;
    }
    
    div.am-toc-header
    {
      font-size: larger;
      font-weight: bold;
      text-align: center;
    }
    
    div.toc-action-group-header
    {
      font-weight: bold;
      margin-top: 1em;
    }
    
    div.toc-action-group{
      margin-left: 1em;
    }
              ]]>
            </style>
        </head>
        <body>
    
        <div class="am-toc">
          <div class="am-toc-header">Table of Actions</div>
          <xsl:call-template name="am-template">
            <xsl:with-param name="as-toc" select="true()"/>
          </xsl:call-template>
        </div>
    
        <h1><center><xsl:value-of select="$am-title"/></center></h1>
        
    
        
        <xsl:call-template name="am-template"/>
      
      </body>
      </html>
    
    </xsl:template>
    
    <xsl:template name="am-template">
        <xsl:param name="defined-in" select="$file-name"/>
        <xsl:param name="as-toc" select="false()"/>
        
        <xsl:for-each select="require">
          <xsl:variable name="theUrl" select="@url"/>
          <xsl:for-each select="document($theUrl, .)/actionModel">
            <xsl:call-template name="am-template">
              <xsl:with-param name="as-toc" select="$as-toc"/>
              <xsl:with-param name="defined-in" select="$theUrl"/>
            </xsl:call-template>
          </xsl:for-each>
        </xsl:for-each>
        
        <xsl:variable name="implemented" 
             select="action[not(starts-with(normalize-space(string(description)), $not-implemented-tag))]"/>
        <xsl:variable name="unimplemented" 
             select="action[starts-with(normalize-space(string(description)), $not-implemented-tag)]"/>
        <xsl:choose>
          <xsl:when test="$action-filter='all'">
            <xsl:call-template name="actions-template">
              <xsl:with-param name="action-group" select="actions"/>
              <xsl:with-param name="as-toc" select="$as-toc"/>
              <xsl:with-param name="defined-in" select="$defined-in"/>
            </xsl:call-template>
          </xsl:when>
          <xsl:when test="$action-filter='implemented'">
            <xsl:call-template name="actions-template">
              <xsl:with-param name="action-group" select="$implemented"/>
              <xsl:with-param name="as-toc" select="$as-toc"/>
              <xsl:with-param name="defined-in" select="$defined-in"/>
            </xsl:call-template>
          </xsl:when>
          <xsl:when test="$action-filter='unimplemented'">
            <xsl:call-template name="actions-template">
              <xsl:with-param name="action-group" select="actions"/>
              <xsl:with-param name="as-toc" select="$as-toc"/>
              <xsl:with-param name="defined-in" select="$defined-in"/>
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <xsl:message terminate="yes">Unknown action filter supplied: <xsl:value-of select="$action-filter"/></xsl:message>
          </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="actions-template">
      <xsl:param name="action-group"/>
      <xsl:param name="as-toc" select="false()"/>
      <xsl:param name="defined-in"/>
      
      <xsl:if test="$action-group">
        <xsl:choose>
          <xsl:when test="$as-toc">
            <div class="toc-action-group-header">
              From <code><xsl:value-of select="$defined-in"/></code>:
            </div>
            <div class="toc-action-group">
              <xsl:for-each select="$action-group">
                <xsl:call-template name="toc-item-template"/>
              </xsl:for-each>
            </div>
          </xsl:when>
          <xsl:otherwise>
            <xsl:for-each select="$action-group">
              <xsl:call-template name="action-def-template">
                <xsl:with-param name="defined-in" select="$defined-in"/>
              </xsl:call-template>
            </xsl:for-each>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:if>
    </xsl:template>
    
    
    
    <xsl:template name="toc-item-template">
      <div><code><a href="#{generate-id()}"><xsl:value-of select="@parent"/>
          <xsl:value-of select="@id"/>
      </a></code></div>
    </xsl:template>
    
    
    <xsl:template name="action-def-template">
      <xsl:param name="defined-in"/>
      <div class="action-def">
        <div class="action-header"><a name="{generate-id()}" />Action:
          <code><xsl:value-of select="@id"/></code>
        </div>
              
        <p>
          <xsl:variable name="action-description" select="normalize-space(string(description))"/>
          <xsl:choose>
            <xsl:when test="starts-with($action-description, $not-implemented-tag)">
              <xsl:value-of select="substring-after($action-description, $not-implemented-tag)"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="description"/>
            </xsl:otherwise>
          </xsl:choose>
        </p>
        
        <xsl:if test="$defined-in">
          <p class="action-defined-in">
            <b>Defined in: </b> <code><xsl:value-of select="$defined-in"/></code>
          </p>
        </xsl:if>
        
        
        <xsl:variable name="inherits" select="inherit"/>
        <xsl:if test="count($inherits) > 0">
          <p><b>Inherits from:</b></p>
          <ul>
            <xsl:for-each select="$inherits">
              <li>
                <xsl:choose>
                  <xsl:when test="key('action', @parent)">
                    <a href="#{generate-id(key('action', @parent))}"><xsl:value-of select="@parent"/></a>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:value-of select="@parent"/>
                  </xsl:otherwise>
                </xsl:choose>
              </li>
            </xsl:for-each>
          </ul>           
        </xsl:if>
        
        
        <xsl:variable name="heirs" select="key('heir', @id)"/>
        
        <xsl:if test="count($heirs) > 0">
          <p><b>Actions that inherit from this:</b></p>
          <ul>
            <xsl:for-each select="$heirs">
              <li><a href="#{generate-id()}"><xsl:value-of select="@id"/></a></li>
            </xsl:for-each>
          </ul>           
        </xsl:if>
        
        
        <xsl:call-template name="param-list-template">
          <xsl:with-param name="param-type" select="'input'"/>
        </xsl:call-template>
        
        <br/>
        
        <xsl:call-template name="param-list-template">
          <xsl:with-param name="param-type" select="'output'"/>
        </xsl:call-template>
      </div>
    </xsl:template>
    
    
    <xsl:template name="param-list-template">
      <xsl:param name="param-type" select="input"/>
      <xsl:variable name="elt-type" select="concat($param-type, 'Param')"/>
      <xsl:variable name="all-params" 
            select="(.|key('action', ./inherit/@parent))/*[name()=$elt-type]"/>
      
      <table class="param-list">
        <tr>
          <th colspan="4" align="left">
            <xsl:value-of select="$param-type"/>s:
          </th>
        </tr>
        <xsl:choose>
          <!-- Note, if an action has multiple levels of inheritance, 
               and neither it nor it's immediate parent(s) declare parameters,
               this test will incorrectly determine that the action has no
               parameters. -->
          <xsl:when test="count($all-params) = 0">
            <tr>
              <td colspan="4">
                <i>No <xsl:value-of select="$param-type"/>s defined</i>
              </td>
            </tr>
          </xsl:when>
          <xsl:otherwise>
            <xsl:for-each select="*[name()=$elt-type]">
              <xsl:call-template name="single-param-template"/>
            </xsl:for-each>
            <xsl:for-each select="inherit">
              <xsl:for-each select="key('action', @parent)/*[name()=$elt-type]">
                <xsl:call-template name="single-param-template"/>
              </xsl:for-each>
            </xsl:for-each>
          </xsl:otherwise>
        </xsl:choose>
      </table>
      
    </xsl:template>
    
    <xsl:template name="single-param-template">
      <tr>
        <td>
          <xsl:choose>
            <xsl:when test="./ungeneralizable">
              <span style="white-space: pre"> </span>
            </xsl:when>
            <xsl:otherwise>x</xsl:otherwise>
          </xsl:choose>
        </td>
        <td><code><xsl:value-of select="@id"/></code></td>
        <td><code><i><xsl:value-of select="typeRef/@typeId"/></i></code></td>
        <td><xsl:value-of select="description"/></td>
      </tr>
    </xsl:template>
</xsl:stylesheet>