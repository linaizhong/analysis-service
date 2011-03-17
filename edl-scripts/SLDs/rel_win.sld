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
          <sld:ColorMapEntry color="#ffffff" opacity="0" quantity="-9999"/>
          <sld:ColorMapEntry color="#CCCCFF" quantity="0.000"/>
          <sld:ColorMapEntry color="#009999" quantity="9.3"/>
          <sld:ColorMapEntry color="#99FF66" quantity="18.6"/>
          <sld:ColorMapEntry color="#FFFF66" quantity="27.9"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="37.2"/>
          <sld:ColorMapEntry color="#FF9900" quantity="46.5"/>
          <sld:ColorMapEntry color="#FF6600" quantity="55.8"/>
          <sld:ColorMapEntry color="#FF6666" quantity="65.1"/>
          <sld:ColorMapEntry color="#FF3300" quantity="74.4"/>
          <sld:ColorMapEntry color="#CC33FF" quantity="83.7"/>
          <sld:ColorMapEntry color="#FF33FF" quantity="93.000"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>
