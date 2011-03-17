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
          <sld:ColorMapEntry color="#009999" quantity="-5.44"/>
          <sld:ColorMapEntry color="#99FF66" quantity="-2.684"/>
          <sld:ColorMapEntry color="#ffffff" quantity="0.0" opacity="0"/>
          <sld:ColorMapEntry color="#FFFF66" quantity="2.828"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="5.584"/>
          <sld:ColorMapEntry color="#FF9900" quantity="8.34"/>
          <sld:ColorMapEntry color="#FF6600" quantity="11.096"/>
          <sld:ColorMapEntry color="#FF6666" quantity="13.852"/>
          <sld:ColorMapEntry color="#FF3300" quantity="16.608"/>
          <sld:ColorMapEntry color="#CC33FF" quantity="19.364"/>
          <sld:ColorMapEntry color="#FF33FF" quantity="22.120"/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>