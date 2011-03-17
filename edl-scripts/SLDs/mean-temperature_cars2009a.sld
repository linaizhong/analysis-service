<?xml version="1.0" encoding="UTF-8"?>
<sld:UserStyle xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml">
  <sld:Name>raster</sld:Name>
  <sld:Title>A very simple color map</sld:Title>
  <sld:Abstract>A very basic color map</sld:Abstract>
  <sld:FeatureTypeStyle>
    <sld:Name>name</sld:Name>
    <sld:FeatureTypeName>Feature</sld:FeatureTypeName>
    <sld:Rule>
      <sld:RasterSymbolizer>
        <sld:Geometry>
          <ogc:PropertyName>geom</ogc:PropertyName>
        </sld:Geometry>
        <sld:ChannelSelection>
          <sld:GrayChannel>
            <sld:SourceChannelName>1</sld:SourceChannelName>
          </sld:GrayChannel>
        </sld:ChannelSelection>
        <sld:ColorMap>
          <!--<sld:ColorMapEntry color="#ffffff" opacity="0" quantity="-28068.0"/> -->
          <sld:ColorMapEntry color="#CCCCFF" quantity="-28068.000"/> 
          <sld:ColorMapEntry color="#009999" quantity="-22109.2"/>
          <sld:ColorMapEntry color="#99FF66" quantity="-16150.4"/>
          <sld:ColorMapEntry color="#FFFF66" quantity="-10191.6"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="-4232.8"/>
          <sld:ColorMapEntry color="#ffffff" opacity="0" quantity="0"/>
          <sld:ColorMapEntry color="#FF9900" quantity="1726.0"/>
          <sld:ColorMapEntry color="#FF6600" quantity="7684.8"/>
          <sld:ColorMapEntry color="#FF6666" quantity="13643.6"/>
          <sld:ColorMapEntry color="#FF3300" quantity="19602.4"/>
          <sld:ColorMapEntry color="#CC33FF" quantity="25561.2"/>
          <sld:ColorMapEntry color="#FF33FF" quantity="31520.000"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>