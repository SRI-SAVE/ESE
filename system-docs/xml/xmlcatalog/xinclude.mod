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

<!ELEMENT xi:include (xi:fallback?) >
<!ATTLIST xi:include
xmlns:xi            CDATA       #FIXED       "http://www.w3.org/2001/XInclude"
href                CDATA       #REQUIRED
parse               (xml|text)  "xml"
xpointer            CDATA       #IMPLIED
encoding            CDATA       #IMPLIED
accept              CDATA       #IMPLIED
accept-charset      CDATA       #IMPLIED
accept-language     CDATA       #IMPLIED >
<!ELEMENT xi:fallback ANY >
<!ATTLIST xi:fallback
xmlns:xi            CDATA       #FIXED "http://www.w3.org/2001/XInclude" >
<!ENTITY % local.preface.class    "| xi:include" >
<!ENTITY % local.part.class       "| xi:include" >
<!ENTITY % local.chapter.class    "| xi:include" >
<!ENTITY % local.divcomponent.mix "| xi:include" >
<!ENTITY % local.para.char.mix    "| xi:include" >
<!ENTITY % local.info.class       "| xi:include" >
<!ENTITY % local.common.attrib    "xmlns:xi            CDATA       #FIXED       'http://www.w3.org/2001/XInclude'" >