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
          <sld:ColorMapEntry color="#ffffff" opacity="0" quantity="-999"/>
          <sld:ColorMapEntry color="#009999" quantity="2.0629"/>
          <sld:ColorMapEntry color="#99FF66" quantity="4.1088"/>
          <sld:ColorMapEntry color="#FFFF66" quantity="6.1547"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="8.2006"/>
          <sld:ColorMapEntry color="#FF9900" quantity="10.2465"/>
          <sld:ColorMapEntry color="#FF6600" quantity="12.2924"/>
          <sld:ColorMapEntry color="#FF6666" quantity="14.3383"/>
          <sld:ColorMapEntry color="#FF3300" quantity="16.3842"/>
          <sld:ColorMapEntry color="#CC33FF" quantity="18.4301"/>
          <sld:ColorMapEntry color="#FF33FF" quantity="20.476"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>
