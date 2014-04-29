package thredds.server.sos.service;

import com.google.common.base.Joiner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import thredds.server.sos.util.XMLDomUtils;

import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.Station;

/**
 * Get Observation Parser
 * @author abird
 */
public class SOSGetObservationRequestHandler extends SOSBaseRequestHandler {

    public final static String TEMPLATE = "templates/sosGetObservation.xml";
    
    private String stationName;
    private String[] variableNames;
    private String[] eventTime;

    private Station station;
    private StationTimeSeriesFeature stationTimeSeriesFeature;
    
    public SOSGetObservationRequestHandler(NetcdfDataset netCDFDataset, String stationName, String[] variableNames, String[] eventTime) throws IOException {
        super(netCDFDataset);
        this.stationName = stationName;
        this.variableNames = variableNames;
        this.eventTime = eventTime;
        
        station = getFeatureCollection().getStation(stationName);
        stationTimeSeriesFeature = getFeatureCollection().getStationFeature(station);
        stationTimeSeriesFeature.calcBounds();
    }

    @Override
    public String getTemplateLocation(){
      return TEMPLATE;
    }

    private void addDatasetResults(String[] obsProperty) {
        //TODO Test the following
        //add Data Block Definition
        document = XMLDomUtils.addNode(document, "om:result", "swe:DataArray");
        //element count
        document = XMLDomUtils.addNode(document, "swe:DataArray", "swe:elementCount");
        document = XMLDomUtils.addNode(document, "swe:elementCount", "swe:Count");
        document = XMLDomUtils.addNodeAndValue(document, "swe:Count", "swe:value", "COUNT!!!!");
        //element Type
        document = XMLDomUtils.addNodeAndAttribute(document, "swe:DataArray", "swe:elementType", "name", "SimpleDataArray");
        //add data record
        document = XMLDomUtils.addNode(document, "swe:elementType", "swe:DataRecord");

        createDataFields(obsProperty);

        //add encoding value
        document = XMLDomUtils.addNode(document, "swe:DataArray", "swe:encoding");
        // text block
        document = XMLDomUtils.addNodeAndAttribute(document, "swe:encoding", "swe:TextBlock", "blockSeparator", " ");
        XMLDomUtils.setAttributeFromNode(document, "swe:encoding", "swe:TextBlock", "decimalSeparator", ".");
        XMLDomUtils.setAttributeFromNode(document, "swe:encoding", "swe:TextBlock", "tokenSeparator", ",");

        try {
            //set the data
            document = XMLDomUtils.addNodeAndValue(document, "swe:DataArray", "swe:values", createObsValuesString());
        } catch (Exception ex) {
            document = XMLDomUtils.addNodeAndValue(document, "swe:DataArray", "swe:values", "ERROR!");
            Logger.getLogger(SOSGetObservationRequestHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void createDataFields(String[] observedProperties) {
        int fieldIndex = 0;
        int quantityIndex = 0;
        
//        //add fields
//        //platform
//        document = XMLDomUtils.addNodeAndAttribute(document, "swe:DataRecord", "swe:field", "name", "PlatformName");
//        document = XMLDomUtils.addNodeAndAttribute(document, "swe:field", "swe:Quantity", fieldIndex++, "definition", "urn:mmisw.org#platform");
//        //time
        document = XMLDomUtils.addNodeAndAttribute(document, "swe:DataRecord", "swe:field", "name", "time");
        document = XMLDomUtils.addNodeAndAttribute(document, "swe:field", "swe:Time", fieldIndex++, "definition", "urn:ogc:phenomenon:time:iso8601");
//        //lat
//        document = XMLDomUtils.addNodeAndAttribute(document, "swe:DataRecord", "swe:field", "name", "lat");
//        document = XMLDomUtils.addNodeAndAttribute(document, "swe:field", "swe:Quantity", fieldIndex++, "definition", "urn:ogc:phenomenon:latitude:wgs84");
//        document = XMLDomUtils.addNodeAndAttribute(document, "swe:Quantity", "swe:uom", quantityIndex++, "code", "deg");
//        //lon
//        document = XMLDomUtils.addNodeAndAttribute(document, "swe:DataRecord", "swe:field", "name", "lon");
//        document = XMLDomUtils.addNodeAndAttribute(document, "swe:field", "swe:Quantity", fieldIndex++, "definition", "urn:ogc:phenomenon:latitude:wgs84");
//        document = XMLDomUtils.addNodeAndAttribute(document, "swe:Quantity", "swe:uom", quantityIndex++, "code", "deg");

//        if (isDepthAvailable == true) {
//            doc = XMLDomUtils.addNodeAndAttribute(doc, "swe:DataRecord", "swe:field", "name", "depth");
//            doc = XMLDomUtils.addNodeAndAttribute(doc, "swe:field", "swe:Quantity", fieldIndex++, "definition", "http://mmisw.org/cf#depth");
//            doc = XMLDomUtils.addNodeAndAttribute(doc, "swe:Quantity", "swe:uom", quantityIndex++, "code", "m");
//        }

        // add observed property
        for (String observedProperty : observedProperties) {
            VariableSimpleIF variable = getFeatureDataset().getDataVariable(observedProperty);
            document = XMLDomUtils.addNodeAndAttribute(document, "swe:DataRecord", "swe:field", "name", observedProperty);
            document = XMLDomUtils.addNodeAndAttribute(document, "swe:field", "swe:Quantity", fieldIndex++, "definition", "urn:ogc:def:phenomenon:mmisw.org:cf:" + observedProperty);
            document = XMLDomUtils.addNodeAndAttribute(document, "swe:Quantity", "swe:uom", quantityIndex++, "code", variable.getUnitsString());
        }


    }

    private String getLatLonString() {
        return (new StringBuilder())
                .append(formatDegree(station.getLatitude())).append(" ")
                .append(formatDegree(station.getLongitude())).append(" ")
                .append("0")
                .toString();
    }

    public String getSystemGMLID() {
        return XMLDomUtils.getObsGMLIDAttributeFromNode(document, "om:ObservationCollection", "gml:id");
    }

    public void setSystemGMLID(String value) {
        XMLDomUtils.setObsGMLIDAttributeFromNode(document, "om:ObservationCollection", "gml:id", value);
    }

    void setSystemGMLID() {
        XMLDomUtils.setObsGMLIDAttributeFromNode(document, "om:ObservationCollection", "gml:id", getGMLID(stationName));
    }

    public void setCollectionDescription() {
        XMLDomUtils.setNodeValue(document, "om:ObservationCollection", "gml:description", getDescription());
    }

    public String getCollectionDescription() {
        return XMLDomUtils.getNodeValue(document, "om:ObservationCollection", "gml:description");
    }

    public void setCollectionName() {
        XMLDomUtils.setNodeValue(document, "om:ObservationCollection", "gml:name", getTitle());
    }

    public String getCollectionName() {
        return XMLDomUtils.getNodeValue(document, "om:ObservationCollection", "gml:name");
    }

    public void setCollectionSourceName() {
        XMLDomUtils.setAttributeFromNode(document, "gml:boundedBy", "gml:Envelope", "srsName", "EPSG:4326");
    }

    public String getCollectionSourceName() {
        return XMLDomUtils.getAttributeFromNode(document, "gml:boundedBy", "gml:Envelope", "srsName");
    }

    public void setCollectionLowerCornerEnvelope() {
        String lonlatStr = getLatLonString();
        XMLDomUtils.setNodeValue(document, "gml:Envelope", "gml:lowerCorner", lonlatStr);
    }

    public String getCollectionLowerCornerEnvelope() {
        return XMLDomUtils.getNodeValue(document, "gml:Envelope", "gml:lowerCorner");
    }

    public void setCollectionUpperCornerEnvelope() {
        XMLDomUtils.setNodeValue(document, "gml:Envelope", "gml:upperCorner", getLatLonString());
    }

    public String getCollectionUpperCornerEnvelope() {
        return XMLDomUtils.getNodeValue(document, "gml:Envelope", "gml:upperCorner");
    }

    /*
     * Create the observation XML data for getObs
     */
    public void parseObservations() {

        setObsCollectionMetaData();

        //add observation
        document = XMLDomUtils.addObservationElement(document);
        //add description
        document = XMLDomUtils.addNodeAndValue(document, "om:Observation", "gml:description", getDescription());
        //add name
        document = XMLDomUtils.addNodeAndValue(document, "om:Observation", "gml:name", getDescription());
        //add bounded by
        document = XMLDomUtils.addNode(document, "om:Observation", "gml:boundedBy");
        //add envelope and attribute
        document = XMLDomUtils.addNodeToNodeAndAttribute(document, "om:Observation", "gml:boundedBy", "gml:Envelope", "srsName", getGMLName(stationName));
        String upper = getLatLonString();
        String lower = getLatLonString();
        document = XMLDomUtils.addNodeToNodeAndValue(document, "gml:Envelope", "gml:lowerCorner", lower);
        //add Upper GPS coors
        document = XMLDomUtils.addNodeToNodeAndValue(document, "gml:Envelope", "gml:upperCorner", upper);
        //add sampling time
        document = XMLDomUtils.addNode(document, "om:Observation", "om:samplingTime");
        //add time instant
        document = XMLDomUtils.addNodeToNodeAndAttribute(document, "om:Observation", "om:samplingTime", "gml:TimePeriod", "gml:id", "DATA_TIME");
        //add time positions (being and end)
        document = XMLDomUtils.addNodeToNodeAndValue(document, "gml:TimePeriod", "gml:beginPosition", stationTimeSeriesFeature.getDateRange().getStart().toDateTimeStringISO());
        document = XMLDomUtils.addNodeToNodeAndValue(document, "gml:TimePeriod", "gml:endPosition", stationTimeSeriesFeature.getDateRange().getEnd().toDateTimeStringISO());
        //add procedure
        document = XMLDomUtils.addNodeAndAttribute(document, "om:Observation", "om:procedure", "xlink:href", getLocation());

        //add observedProperties
        for (int i = 0; i < variableNames.length; i++) {
            String variableName = variableNames[i];
            document = XMLDomUtils.addNodeAndAttribute(document, "om:Observation", "om:observedProperty", "xlink:href", "http://marinemetadata.org/cf#" + variableName);
        }

//        if (isDepthAvailable == true) {
//            doc = XMLDomUtils.addNodeAndAttribute(doc, "om:Observation", "om:observedProperty", "xlink:href", "http://marinemetadata.org/cf#" + "depth");
//        }

        //add feature of interest
        document = XMLDomUtils.addNodeAndAttribute(document, "om:Observation", "om:featureOfInterest", "xlink:href", getFeatureOfInterest(stationName));
        //add results Node
        document = XMLDomUtils.addNode(document, "om:Observation", "om:result");

        addDatasetResults(variableNames);

    }

    public String getObservationDescription() {
        return XMLDomUtils.getNodeValue(document, "om:Observation", "gml:description");
    }

    public String getObservationName() {
        return XMLDomUtils.getNodeValue(document, "om:Observation", "gml:name");
    }

    public String getObservationEnvelope() {
        return XMLDomUtils.getNodeValue(document, "om:Observation", "gml:boundedBy");
    }

    public String getObservationEnvelopeSrsName() {
        return XMLDomUtils.getAttributeFromNode(document, "om:Observation", "gml:Envelope", "srsName");
    }

    String getObservationLowerCorner() {
        return XMLDomUtils.getNodeValue(document, "om:Observation", "gml:lowerCorner");
    }

    String getObservationUpperCorner() {
        return XMLDomUtils.getNodeValue(document, "om:Observation", "gml:upperCorner");
    }

    String getObservationTimeInstant() {
        return XMLDomUtils.getAttributeFromNode(document, "om:Observation", "gml:TimePeriod", "gml:id");
    }

    String getObservationTimePosition() {
        return XMLDomUtils.getNodeValue(document, "om:Observation", "gml:beginPosition");
    }

    String getObservationProcedure() {
        return XMLDomUtils.getAttributeFromNode(document, "om:Observation", "om:procedure", "xlink:href");
    }

    String getObservationObservedProperty() {
        return XMLDomUtils.getAttributeFromNode(document, "om:Observation", "om:observedProperty", "xlink:href");
    }

    public String createObsValuesString() throws Exception {
        
        String latVal = formatDegree(station.getLatitude());
        String lonVal = formatDegree(station.getLongitude());

        CalendarDateRange cdr = makeCalendarDateRange(eventTime);
        StationTimeSeriesFeature subset = stationTimeSeriesFeature.subset(cdr);
        
        PointFeatureIterator iterator = subset.getPointFeatureIterator(-1);
        StringBuilder builder = new StringBuilder();
        DateFormatter dateFormatter = new DateFormatter();
        List<String> valueList = new ArrayList<String>();
        Joiner tokenJoiner = Joiner.on(',');
        while (iterator.hasNext()) {
            PointFeature pointFeature = iterator.next();
            valueList.clear();
//            valueList.add(stationName);
            valueList.add(dateFormatter.toDateTimeStringISO(pointFeature.getObservationTimeAsDate()));
//            valueList.add(latVal);
//            valueList.add(lonVal);
            for (String variableName : variableNames) {
                valueList.add(pointFeature.getData().getScalarObject(variableName).toString());
            }
            builder.append(tokenJoiner.join(valueList));
            // TODO:  conditional inside loop...
            if (stationTimeSeriesFeature.size() > 1) {
                builder.append(" ");
                builder.append("\n");
            }
        }
        setCount(stationTimeSeriesFeature.size());
        return builder.toString();
    }

    private void setObsCollectionMetaData() {
        setSystemGMLID();
        setCollectionDescription();
        setCollectionName();
        setCollectionSourceName();
        setCollectionLowerCornerEnvelope();
        setCollectionUpperCornerEnvelope();
    }

    public String getResultValues() {
        return XMLDomUtils.getNodeValue(document, "om:Observation", "swe:values");
    }

    private void setBeginTime(String time) {
        XMLDomUtils.setNodeValue(document, "om:Observation", "gml:beginPosition", time);
    }

    private void setEndTime(String time) {
        XMLDomUtils.setNodeValue(document, "om:Observation", "gml:EndPosition", time);
    }

    private void setCount(int count) {
        XMLDomUtils.setNodeValue(document, "om:Observation", "swe:value", Integer.toString(count));
    }
    
    private CalendarDateRange makeCalendarDateRange(String[] eventTime) {
        CalendarDateRange cdr = null;
        if (eventTime == null || eventTime.length == 0) {
            throw new IllegalArgumentException("Must be single date or date range");
        } else if (eventTime.length >= 2) {
            cdr = CalendarDateRange.of(CalendarDate.parseISOformat("gregorian", eventTime[0]),
                    CalendarDate.parseISOformat("gregorian", eventTime[1]));
        } else {
            CalendarDate date = CalendarDate.parseISOformat("gregorian", eventTime[0]);
            cdr = CalendarDateRange.of(date, date);
        }
        return cdr;
    }
}
