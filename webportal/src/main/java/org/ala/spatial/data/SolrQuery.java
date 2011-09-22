/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.data;

import au.com.bytecode.opencsv.CSVReader;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import java.util.zip.GZIPInputStream;
import net.sf.json.JSONArray;
import org.ala.spatial.sampling.SimpleRegion;
import org.ala.spatial.sampling.SimpleShapeFile;
import org.ala.spatial.util.CommonData;

/**
 *
 * @author Adam
 */
public class SolrQuery implements Query, Serializable {

    static final String SAMPLING_SERVICE_CSV_GZIP = "/webportal/occurrences.gz?";
    static final String SAMPLING_SERVICE = "/webportal/occurrences?";
    static final String SPECIES_LIST_SERVICE = "/webportal/species?";
    static final String SPECIES_LIST_SERVICE_CSV = "/webportal/species.csv?";
    static final String DOWNLOAD_URL = "/occurrences/download?";
    static final String DATA_PROVIDERS_SERVICE = "/webportal/dataProviders?";
    static final String LEGEND_SERVICE_CSV = "/webportal/legend?";
    static final String BOUNDING_BOX_CSV = "/webportal/bbox?";
    static final String INDEXED_FIELDS_LIST = "/indexed/fields?";
    static final String POST_SERVICE = "/webportal/params?";
    static final String DEFAULT_ROWS = "pageSize=1000000";
    /** DEFAULT_VALIDATION must not be null */
    static final String DEFAULT_VALIDATION = "geospatial_kosher:true";//%20AND%20longitude:[*%20TO%20*]%20AND%20latitude:[*%20TO%20*]";
    static final String BIE_SPECIES = "/species/";
    static final String WMS_URL = "/webportal/wms/reflect?";
    private static String[] commonTaxonRanks = new String[]{
        "cultivar",
        "superfamily",
        "subgenus",
        "unranked",
        "infrageneric",
        "subfamily",
        "subspecies",
        "section",
        "infraspecific",
        "hybrid",
        "variety",
        "form",
        "series",
        "tribe"
    };
    //query inputs
    String name;
    String rank;
    String lsids;
    ArrayList<Facet> facets;
    String wkt;
    String extraParams;
    String paramId;

    public SolrQuery(String lsids, String wkt, String extraParams, ArrayList<Facet> facets) {
        this.lsids = lsids;
        this.facets = facets;
        this.wkt = (wkt != null && wkt.equals(CommonData.WORLD_WKT)) ? null : wkt;
        this.extraParams = extraParams;

        makeParamId();
    }

    /**
     * Further restrict records by field values.
     *
     * @param field
     * @param values
     */
    @Override
    public SolrQuery newFacet(Facet facet) {
        ArrayList<Facet> newFacets = new ArrayList<Facet>();
        if (facets != null) {
            newFacets.addAll(facets);
        }
        newFacets.add(facet);

        return new SolrQuery(lsids, wkt, extraParams, newFacets);
    }

    /**
     * Restrict to an area.
     *
     * If an area already exists the additional area is applied.
     *
     * @param wkt
     * @return new SolrQuery with the additional wkt area applied.
     */
    @Override
    public SolrQuery newWkt(String wkt) {
        if (wkt == null || wkt.equals(CommonData.WORLD_WKT) || wkt.equals(this.wkt)) {
            return this;
        }

        SolrQuery sq = null;
        try {
            String newWkt = wkt;
            if (this.wkt != null) {
                Geometry newGeom = new WKTReader().read(wkt);
                Geometry thisGeom = new WKTReader().read(this.wkt);
                Geometry intersectionGeom = thisGeom.intersection(newGeom);
                newWkt = (new WKTWriter()).write(intersectionGeom).replace(" (", "(").replace(", ", ",").replace(") ", ")");
            }

            sq = new SolrQuery(lsids, newWkt, extraParams, facets);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sq;
    }

    /**
     * Get records for this query for the provided fields.
     *
     * @param fields QueryFields to return in the sample.
     * @return records as String in CSV format.
     */
    @Override
    public String sample(ArrayList<QueryField> fields) {
        HttpClient client = new HttpClient();
        String url = CommonData.biocacheServer
                + SAMPLING_SERVICE_CSV_GZIP
                + DEFAULT_ROWS
                + "&q=" + getQ()
                + paramQueryFields(fields);
        System.out.println(url);
        GetMethod get = new GetMethod(url.replace("[", "%5B").replace("]", "%5D"));

        String sample = null;

        long start = System.currentTimeMillis();
        try {
            int result = client.executeMethod(get);
            sample = decompressGz(get.getResponseBodyAsStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("get sample in " + (System.currentTimeMillis() - start) + "ms");

        return sample;
    }

    /**
     * Get species list for this query.
     *
     * @return species list as String containing CSV.
     */
    @Override
    public String speciesList() {
        HttpClient client = new HttpClient();
        String url = CommonData.biocacheServer
                + SPECIES_LIST_SERVICE_CSV
                + DEFAULT_ROWS
                + "&q=" + getQ();
        System.out.println(url);
        GetMethod get = new GetMethod(url.replace("[", "%5B").replace("]", "%5D"));

        String speciesList = null;

        try {
            int result = client.executeMethod(get);
            speciesList = get.getResponseBodyAsString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return speciesList;
    }

    /**
     * Get number of occurrences in this query.
     *
     * @return number of occurrences as int or -1 on error.
     */
    @Override
    public int getOccurrenceCount() {
        int occurrenceCount = -1;

        HttpClient client = new HttpClient();
        String url = CommonData.biocacheServer
                + SAMPLING_SERVICE
                + "pageSize=0"
                + "&q=" + getQ();
        System.out.println(url);
        GetMethod get = new GetMethod(url.replace("[", "%5B").replace("]", "%5D"));

        try {
            int result = client.executeMethod(get);
            String response = get.getResponseBodyAsString();

            String start = "\"totalRecords\":";
            String end = ",";
            int startPos = response.indexOf(start) + start.length();

            occurrenceCount = Integer.parseInt(response.substring(startPos, response.indexOf(end, startPos)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return occurrenceCount;
    }

    /**
     * Get number of species in this query.
     *
     * @return number of species as int or -1 on error.
     */
    @Override
    public int getSpeciesCount() {
        int speciesCount = -1;

        HttpClient client = new HttpClient();
        String url = CommonData.biocacheServer
                + SPECIES_LIST_SERVICE
                + DEFAULT_ROWS
                + "&q=" + getQ();
        System.out.println(url);
        GetMethod get = new GetMethod(url.replace("[", "%5B").replace("]", "%5D"));

        try {
            int result = client.executeMethod(get);
            String response = get.getResponseBodyAsString();

            if (result == 200) {
                speciesCount = 0;
                int pos = 0;
                while ((pos = response.indexOf('{', pos + 1)) >= 0) {
                    speciesCount++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return speciesCount;
    }

    /**
     * Get parsed coordinates and optional fields for this query.
     *
     * @param fields QueryFields to return in the sample as ArrayList<QueryField>.
     * If a QueryField isStored() it will be populated with the field data.
     * @return coordinates as double [] like [lng, lat, lng, lat, ...].
     */
    @Override
    public double[] getPoints(ArrayList<QueryField> fields) {
        if (fields == null) {
            fields = new ArrayList<QueryField>();
        }
        fields.add(new QueryField("longitude"));
        fields.add(new QueryField("latitude"));

        String sample = sample(fields);

        long start = System.currentTimeMillis();

        int lineCount = -1; //header count offset
        int pos = -1;
        while ((pos = sample.indexOf('\n', pos + 1)) >= 0) {
            lineCount++;
        }
        System.out.println("sampled records count: " + lineCount);
        CSVReader csv = new CSVReader(new StringReader(sample));

        //process header
        int[] fieldsToCsv = new int[fields.size()];
        for (int i = 0; i < fieldsToCsv.length; i++) {
            fieldsToCsv[i] = -1;
        }
        String[] line = {};
        try {
            line = csv.readNext();
        } catch (IOException ex) {
            Logger.getLogger(SolrQuery.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (int i = 0; i < line.length; i++) {
            if (line[i] != null && i < fieldsToCsv.length) {
                for (int j = 0; j < fields.size(); j++) {
                    if (fields.get(j).getName().equals(line[i])) {
                        fieldsToCsv[j] = i;
                        break;
                    }
                }
            }
        }
        int longitudePos = fieldsToCsv[fieldsToCsv.length - 2];
        int latitudePos = fieldsToCsv[fieldsToCsv.length - 1];

        //process records
        double[] points = new double[lineCount * 2];
        int errCount = 0;
        pos = 0;
        try {
            for (int i = 0; i < fields.size(); i++) {
                if (fields.get(i).isStored()) {
                    fields.get(i).ensureCapacity(lineCount);
                }
            }

            while ((line = csv.readNext()) != null) {
                boolean ok = false;
                try {
                    points[pos] = Double.parseDouble(line[longitudePos]);
                    points[pos + 1] = Double.parseDouble(line[latitudePos]);
                    pos += 2;
                    ok = true;
                } catch (Exception e) {
                    errCount++;
                }
                if (ok) {
                    for (int i = 0; i < fields.size(); i++) {
                        if (fields.get(i).isStored()) {
                            try {
                                fields.get(i).add(line[fieldsToCsv[i]]);
                            } catch (Exception ex) {
                                fields.get(i).add("");
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < fields.size(); i++) {
                if (fields.get(i).isStored()) {
                    long st = System.currentTimeMillis();
                    fields.get(i).store();
                    System.out.println(fields.get(i).getDisplayName() + " stored in " + (System.currentTimeMillis() - st) + "ms");
                }
            }

            csv.close();
        } catch (IOException ex) {
            Logger.getLogger(SolrQuery.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (points.length != pos) {
            double[] pointsCopy = new double[pos];
            System.arraycopy(points, 0, pointsCopy, 0, pos);
            points = pointsCopy;
            System.out.println("pointsCopy, errCount=" + errCount);
        }

        System.out.println("filled getPoints in " + (System.currentTimeMillis() - start) + "ms");

        return points;
    }

    String paramQueryFields(ArrayList<QueryField> fields) {
        StringBuilder sb = new StringBuilder();

        if (fields != null) {
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(fields.get(i).getName());
            }
        }

        return (sb.length() > 0) ? "&fl=" + sb.toString() : "";
    }

    @Override
    public String getQ() {
        if (paramId != null) {
            return "qid:" + paramId;
        }

        return getFullQ();
    }

    @Override
    public String getFullQ() {
        StringBuilder sb = new StringBuilder();

        int queryTerms = 0;
        if (lsids != null) {
            for (String s : lsids.split(",")) {
                if (queryTerms > 0) {
                    sb.append("%20OR%20");
                } else {
                    sb.append("(");
                }
                sb.append("lsid:").append(s);
                queryTerms++;
            }
        }
        if (queryTerms > 0) {
            sb.append(")");
        } else if ((facets == null || facets.isEmpty())
                && DEFAULT_VALIDATION.length() == 0
                && extraParams == null) {
            sb.append("*:*");
            queryTerms++;
        }

        if (facets != null && facets.size() > 0) {
            for (int i = 0; i < facets.size(); i++) {
                if (queryTerms > 0) {
                    sb.append("%20AND%20");
                }
                sb.append(facets.get(i).toString());
                queryTerms++;
            }
        }

        if (DEFAULT_VALIDATION.length() > 0) {
            if (queryTerms > 0) {
                sb.append("%20AND%20");
            }
            queryTerms++;
            sb.append(DEFAULT_VALIDATION);
        }

        //extra parameters
        if (extraParams != null) {
            if (queryTerms > 0) {
                sb.append("%20AND%20");
            }
            queryTerms++;
            sb.append(extraParams);
        }

        //wkt term
        if (wkt != null) {
            sb.append("&wkt=" + wkt.replace(" ", ":"));
        }

        try {
            //String q = URLEncoder.encode(sb.toString(), "UTF-8");
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * Get a query string under a registered id.
     *
     * @return a short query term as String, or null on error.
     */
    final void makeParamId() {
        paramId = null;

        HttpClient client = new HttpClient();
        String url = CommonData.biocacheServer
                + POST_SERVICE;
        PostMethod post = new PostMethod(url.replace("[", "%5B").replace("]", "%5D"));
        try {
            String[] qs = getQ().replace("%20", " ").split("&");
            for (int i = 0; i < qs.length; i++) {
                String q = qs[i];
                int p = q.indexOf('=');
                if (p < 0) {
                    post.addParameter("q", q.substring(p + 1));
                    System.out.println("param: " + "q" + " : " + q.substring(p + 1));
                } else {
                    post.addParameter(q.substring(0, p), q.substring(p + 1));
                    System.out.println("param: " + q.substring(0, p) + " : " + q.substring(p + 1));
                }

            }
            int result = client.executeMethod(post);
            String response = post.getResponseBodyAsString();

            if (result == 200) {
                paramId = response;

                System.out.println(url + " > " + paramId);
            } else {
                System.out.println("error with url:" + url + " posting q: " + getQ() + " > response_code:" + result + " response:" + response);
            }
        } catch (Exception e) {
            System.out.println("error with url:" + url + " posting q: " + getQ());
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        if (name != null) {
            return name;
        }

        if (lsids == null) {
            name = "All species";
        } else if (lsids.split(",").length == 1) {
            name = getScientificNameRank(lsids).split(",")[0];
        } else {
            name = "Selected species";
        }

        return name;
    }

    @Override
    public String getRank() {
        if (rank != null) {
            return rank;
        }

        if (lsids.split(",").length == 1) {
            rank = getScientificNameRank(lsids).split(",")[1];

            if (rank.equalsIgnoreCase("scientific name") || rank.equalsIgnoreCase("scientific")) {
                rank = "taxon";
            }
        } else {
            rank = "scientific name";
        }

        return rank;
    }

//    /**
//     * test
//     *
//     * @param args
//     */
//    static public void main(String[] args) {
//        SolrQuery sq = new SolrQuery();
//
//        //count all occurrences and species
//        System.out.println("total number of occurrences: " + sq.getOccurrenceCount());
//        System.out.println("total number of species: " + sq.getSpeciesCount());
//
//        //Repeat in a polygon
//        String wkt = "POLYGON((140:-37,151:-37,151:-26,140.1310:-26,140:-37))";
//        sq.addWkt(wkt);
//        System.out.println("wkt: " + wkt);
//        System.out.println("number of occurrences in wkt: " + sq.getOccurrenceCount());
//        System.out.println("number of species in wkt: " + sq.getSpeciesCount());
//
//        //Sample some points
//        ArrayList<QueryField> fields = new ArrayList<QueryField>();
//        fields.add(new QueryField("id", "uuid"));
//        String sample = sq.sample(fields);
//        System.out.println("sample:");
//        System.out.println(sample.substring(0, 500) + ((sample.length() <= 500) ? "" : "..."));
//
//        //Sample some coordinates
//        double[] coordinates = sq.getPoints(null);
//        System.out.println("coordinates:");
//        for (int i = 0; coordinates != null && i < coordinates.length && i < 100; i += 2) {
//            System.out.print(coordinates[0] + "," + coordinates[1] + " ");
//        }
//
//    }
//    public String getSingleLsid() {
//        return (lsids.size() == 1) ? lsids.get(0) : "";
//    }
    static public String getScientificNameRank(String lsid) {

        String snUrl = CommonData.bieServer + BIE_SPECIES + lsid + ".json";
        System.out.println(snUrl);

        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(snUrl);
            get.addRequestHeader("Content-type", "application/json");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONObject jo = JSONObject.fromObject(slist);
            String scientficName = jo.getJSONObject("taxonConcept").getString("nameString");
            String rank = jo.getJSONObject("taxonConcept").getString("rankString");

            System.out.println("Arrays.binarySearch(commonTaxonRanks, rank): " + Arrays.binarySearch(commonTaxonRanks, rank));
            if (Arrays.binarySearch(commonTaxonRanks, rank) > -1) {
                rank = "taxon";
            }

            return scientficName + "," + rank;
        } catch (Exception e) {
            System.out.println("Error getting scientific name");
            e.printStackTrace(System.out);
        }

        return null;
    }

    private String decompressGz(InputStream gziped) throws IOException {
        GZIPInputStream gzip = new GZIPInputStream(gziped);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1048576);
        byte[] buffer = new byte[1048576];
        int size;
        while ((size = gzip.read(buffer)) >= 0) {
            baos.write(buffer, 0, size);
        }

        return new String(baos.toByteArray());
    }

    @Override
    public String getWMSpath() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    ArrayList<QueryField> facetFieldList = null;

    @Override
    public ArrayList<QueryField> getFacetFieldList() {
        if (facetFieldList == null) {
            ArrayList<QueryField> fields = new ArrayList<QueryField>();

            fields.add(new QueryField("species_group", "Lifeform", QueryField.FieldType.STRING));
            fields.add(new QueryField("taxon_name", "Scientific Name", QueryField.FieldType.STRING));
            fields.add(new QueryField("species", "Species", QueryField.FieldType.STRING));
            fields.add(new QueryField("genus", "Genus", QueryField.FieldType.STRING));
            fields.add(new QueryField("family", "Family", QueryField.FieldType.STRING));
            fields.add(new QueryField("order", "Order", QueryField.FieldType.STRING));
            fields.add(new QueryField("class", "Class", QueryField.FieldType.STRING));
            fields.add(new QueryField("phylum", "Phylum", QueryField.FieldType.STRING));
            fields.add(new QueryField("kingdom", "Kingdom", QueryField.FieldType.STRING));

            fields.add(new QueryField("coordinate_uncertainty", "Uncertainty", QueryField.FieldType.INT));
            fields.add(new QueryField("data_provider", "Data Provider", QueryField.FieldType.STRING));
            fields.add(new QueryField("institution_name", "Institution", QueryField.FieldType.STRING));
            fields.add(new QueryField("year", "Year", QueryField.FieldType.INT));
            fields.add(new QueryField("collection_name", "Collection", QueryField.FieldType.STRING));
            fields.add(new QueryField("basis_of_record", "Basis of Record", QueryField.FieldType.STRING));

            for (int i = 0; i < fields.size(); i++) {
                fields.get(i).setStored(true);
            }

            facetFieldList = fields;
        }

        return facetFieldList;
    }

    @Override
    public String getSpeciesIdFieldName() {
        return "taxon_concept_lsid";
    }

    @Override
    public String getRecordIdFieldName() {
        return "id";
    }
    HashMap<String, LegendObject> legends = new HashMap<String, LegendObject>();

    /**
     * Get legend for a facet field.
     *
     * @param colourmode
     * @return
     */
    @Override
    public LegendObject getLegend(String colourmode) {
        LegendObject lo = legends.get(colourmode);
        if (lo == null) {
            HttpClient client = new HttpClient();
            String url = CommonData.biocacheServer
                    + LEGEND_SERVICE_CSV
                    + DEFAULT_ROWS
                    + "&q=" + getQ()
                    + "&cm=" + colourmode;
            System.out.println(url);
            GetMethod get = new GetMethod(url.replace("[", "%5B").replace("]", "%5D"));

            String legend = null;

            try {
                int result = client.executeMethod(get);

                lo = new SolrLegendObject(colourmode, get.getResponseBodyAsString());
                legends.put(colourmode, lo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return lo;
    }

    @Override
    public Query newFacets(List<Facet> facets) {
        ArrayList<Facet> newFacets = new ArrayList<Facet>();
        if (this.facets != null) {
            newFacets.addAll(this.facets);
        }
        newFacets.addAll(facets);

        return new SolrQuery(lsids, wkt, extraParams, newFacets);
    }

    @Override
    public String getUrl() {
        return CommonData.biocacheServer + WMS_URL;
    }
    List<Double> bbox = null;

    @Override
    public List<Double> getBBox() {
        if (bbox != null) {
            return bbox;
        }

        bbox = new ArrayList<Double>();

        HttpClient client = new HttpClient();
        String url = CommonData.biocacheServer
                + BOUNDING_BOX_CSV
                + DEFAULT_ROWS
                + "&q=" + getQ();

        System.out.println(url);

        GetMethod get = new GetMethod(url.replace("[", "%5B").replace("]", "%5D"));
        try {
            int result = client.executeMethod(get);
            String[] s = get.getResponseBodyAsString().split(",");
            for (int i = 0; i < 4; i++) {
                bbox.add(Double.parseDouble(s[i]));
            }
        } catch (Exception e) {
            //default to 'world' bb
            SimpleRegion sr = SimpleShapeFile.parseWKT(CommonData.WORLD_WKT);
            bbox.clear();
            bbox.add(sr.getBoundingBox()[0][0]);
            bbox.add(sr.getBoundingBox()[0][1]);
            bbox.add(sr.getBoundingBox()[1][0]);
            bbox.add(sr.getBoundingBox()[1][1]);

            e.printStackTrace();
        }

        return bbox;
    }

    @Override
    public String getMetadataHtml() {
        //first line is the 'caption'
//        return "biocache data\n"
//                + "number of species=" + getSpeciesCount()
//                + "<br>number of occurrences=" + getOccurrenceCount()
//                + "<br>classification=" + lsids
//                + "<br>data providers=" + getDataProviders();

        Map<String,String> classification = getSpeciesClassification(lsids);
        String spname = getName();

        String html = "Species information for " + spname + "\n";
        //html += "<h2 class='md_heading'>Species information for " + spname + "</h2>";
        html += "<table class='md_table'>";
        html += "<tr class='md_grey-bg'><td class='md_th'>Number of species: </td><td class='md_spacer'/><td class='md_value'>"+getSpeciesCount()+"</td></tr>";
        html += "<tr><td class='md_th'>Number of occurrences: </td><td class='md_spacer'/><td class='md_value'>"+getOccurrenceCount()+"</td></tr>";
        html += "<tr class='md_grey-bg'><td class='md_th'>Classification: </td><td class='md_spacer'/><td class='md_value'>";

        Iterator<String> it = classification.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            String value = classification.get(key);
            html += "<a href='" + CommonData.bieServer + BIE_SPECIES + value + "'>" + key + "</a> ";
            if (it.hasNext()) html += " > "; 
        }

        html += "</td></tr>";
        html += "<tr><td class='md_th'>Data providers: </td><td class='md_spacer'/><td class='md_value'>"+getDataProviders()+"</td></tr>";
        if(lsids != null && lsids.length() > 0) {
            html += "<tr class='md_grey-bg'><td class='md_value' colspan='3'>More information for <a href='" + CommonData.bieServer + BIE_SPECIES + lsids + "' target='_blank'>"+ spname +"</a></td></tr>";
        }
        html += "</table>";

        return html;
    }

    private Map<String,String> getSpeciesClassification(String lsid) {

        String[] classificationList = {"kingdom", "phylum", "class", "order", "family", "genus", "species", "subspecies"};
        Map<String,String> classification = new LinkedHashMap<String, String>();

        String snUrl = CommonData.bieServer + BIE_SPECIES + lsid + ".json";
        System.out.println(snUrl);

        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(snUrl);
            get.addRequestHeader("Content-type", "application/json");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONObject jo = JSONObject.fromObject(slist);
            //String scientficName = jo.getJSONObject("taxonConcept").getString("nameString");
            String rank = jo.getJSONObject("taxonConcept").getString("rankString");

            JSONObject joOcc = jo.getJSONObject("classification");
            for (String c : classificationList) {
                if (c.equals(rank)) {
                    break;
                }
                classification.put(joOcc.getString(c.replace("ss", "zz")), joOcc.getString(c.replace("ss", "zz")+"Guid"));
            }

        } catch (Exception e) {
            System.out.println("Error getting scientific name");
            e.printStackTrace(System.out);
        }

        return classification;
    }

    @Override
    public String getDownloadUrl(String[] extraFields) {
        //Some cl fields are downloaded by default
        //TODO: add the other cl fields downloaded by default
        String[] fieldsToRemove = {"cl22"};

        StringBuilder sb = new StringBuilder();
        if (extraFields != null && extraFields.length > 0) {
            for (int i = 0; i < extraFields.length; i++) {
                int j = 0;
                for (j = 0; j < fieldsToRemove.length; j++) {
                    if (fieldsToRemove[j].equals(extraFields[i])) {
                        break;
                    }
                }
                //append if field is not in 'removed' list
                if (j == fieldsToRemove.length) {
                    if (sb.length() == 0) {
                        sb.append("&extra=").append(extraFields[i]);
                    } else {
                        sb.append(",").append(extraFields[i]);
                    }
                }
            }
        }
        return CommonData.biocacheServer + DOWNLOAD_URL + "q=" + getQ() + sb.toString();
    }

    @Override
    public byte[] getDownloadBytes(String[] extraFields) {
        return null;
    }

    private String getDataProviders() {
        HttpClient client = new HttpClient();
        String url = CommonData.biocacheServer
                + DATA_PROVIDERS_SERVICE
                + DEFAULT_ROWS
                + "&q=" + getQ();
        System.out.println(url);
        GetMethod get = new GetMethod(url.replace("[", "%5B").replace("]", "%5D"));

        try {
            int result = client.executeMethod(get);
            String response = get.getResponseBodyAsString();

            if (result == 200) {

                String html = "";

                JSONArray ja = JSONArray.fromObject(response);
                for (int i=0; i<ja.size(); i++) {
                    JSONObject jo = ja.getJSONObject(i);
                    html += "<a href='http://collections.ala.org.au/public/showDataProvider/" + jo.getString("id") + "'>" + jo.getString("name") + "</a>: " + jo.getString("count") + " records <br />";
                }
                //return response;

                return html; 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}