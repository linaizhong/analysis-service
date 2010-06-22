/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.wms.WMSStyle;
import org.ala.spatial.util.LayersUtil;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Checkbox;

/**
 *
 * @author ajay
 */
public class ALOCWCController extends UtilityComposer {

    private static final String SAT_URL = "sat_url";
    private Listbox lbenvlayers;
    private Combobox cbEnvLayers;
    private Textbox groupCount;
    Checkbox useArea;
    private List<String> selectedLayers;
    private MapComposer mc;
    private String satServer = "";
    private SettingsSupplementary settingsSupplementary = null;
    String user_polygon = "";
    Textbox selectionGeomALOC;
    int generation_count = 1;
    String pid;
    String layerLabel;
    String legendPath;
    LayersUtil layersUtil;
    String previousArea = "";

    @Override
    public void afterCompose() {
        super.afterCompose();

        mc = getThisMapComposer();
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(SAT_URL);
        }

        layersUtil = new LayersUtil(mc, satServer);

        setupEnvironmentalLayers();

        selectedLayers = new Vector<String>();
    }

    /**
     * Iterate thru' the layer list setup in the @doAfterCompose method
     * and setup the listbox
     */
    private void setupEnvironmentalLayers() {
        try {
            String[] aslist = layersUtil.getEnvironmentalLayers();

            if (aslist.length > 0) {

                lbenvlayers.setItemRenderer(new ListitemRenderer() {

                    @Override
                    public void render(Listitem li, Object data) {
                        li.setWidth(null);
                        new Listcell((String) data).setParent(li);
                    }
                });

                lbenvlayers.setModel(new SimpleListModel(aslist));
            }

        } catch (Exception e) {
            System.out.println("error setting up env list");
            e.printStackTrace(System.out);
        }
    }

    public void onChange$cbEnvLayers(Event event) {
        String new_value = "";

        new_value = cbEnvLayers.getValue();
        if (new_value.equals("")) {
            return;
        }
        new_value = (new_value.equals("")) ? "" : new_value.substring(0, new_value.indexOf("(")).trim();
        System.out.println("new value: " + new_value);

        if (selectedLayers.contains(new_value)) {
            System.out.println("is not new");
            return;
        } else {
            System.out.println("is new");
            selectedLayers.add(new_value);
        }
    }

    public void onDoInit(Event event) throws Exception {
        runclassification();
        Clients.showBusy("", false);
    }

    public void onClick$btnGenerate(Event event) {
        Clients.showBusy("Classification running...", true);
        Events.echoEvent("onDoInit", this, event.toString());
    }

    public void runclassification() {
        try {
            StringBuffer sbenvsel = new StringBuffer();

            if (lbenvlayers.getSelectedCount() > 1) {
                Iterator it = lbenvlayers.getSelectedItems().iterator();
                int i = 0;
                while (it.hasNext()) {
                    Listitem li = (Listitem) it.next();

                    sbenvsel.append(li.getLabel());
                    if (it.hasNext()) {
                        sbenvsel.append(":");
                    }
                }
            } else {
                Messagebox.show("Please select two or more environmental layers", "ALA Spatial Toolkit", Messagebox.OK, Messagebox.EXCLAMATION);
                return;
            }

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(satServer + "/alaspatial/ws/aloc/processgeo?");
            sbProcessUrl.append("gc=" + URLEncoder.encode(groupCount.getValue(), "UTF-8"));
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));
            if (useArea.isChecked()) {
                user_polygon = mc.getSelectionArea();
            } else {
                user_polygon = "";
            }
            System.out.println("user_polygon: " + user_polygon);
            String area;
            if (user_polygon.length() > 0) {
                //sbProcessUrl.append("&area=" + URLEncoder.encode(user_polygon, "UTF-8"));
                area = user_polygon;
            } else {
                //sbProcessUrl.append("&area=" + URLEncoder.encode("none", "UTF-8"));
                area = "none";
            }

            HttpClient client = new HttpClient();
            //GetMethod get = new GetMethod(sbProcessUrl.toString()); // testurl
            PostMethod get = new PostMethod(sbProcessUrl.toString());
            get.addParameter("area",area);

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            layerLabel = "Classification #" + generation_count + " - " + groupCount.getValue() + " groups";

            generation_count++;
            pid = slist;

            legendPath = "/WEB-INF/zul/AnalysisClassificationLegend.zul?pid=" + pid + "&layer=" + URLEncoder.encode(layerLabel, "UTF-8");

            loadMap();

        } catch (Exception ex) {
            System.out.println("Opps!: ");
            ex.printStackTrace(System.out);
        }
    }

    /**
     * Gets the main pages controller so we can add a
     * layer to the map
     * @return MapComposer = map controller class
     */
    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        Page page = getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }

    /**
     * populate sampling screen with values from active layers and areas tab
     */
    public void callPullFromActiveLayers() {
        //get list of env/ctx layers
        String[] layers = layersUtil.getActiveEnvCtxLayers();

        /* set as selected each envctx layer found */
        if (layers != null) {
            List<Listitem> lis = lbenvlayers.getItems();
            for (int i = 0; i < lis.size(); i++) {
                for (int j = 0; j < layers.length; j++) {
                    if (lis.get(i).getLabel().equalsIgnoreCase(layers[j])) {
                        lbenvlayers.addItemToSelection(lis.get(i));
                        break;
                    }
                }
            }
        }

        /* validate the area box presence, check if area updated */
        String currentArea = mc.getSelectionArea();
        if (currentArea.length() > 0) {
            useArea.setDisabled(false);
            if (!currentArea.equalsIgnoreCase(previousArea)) {
                useArea.setChecked(true);
            }
        } else {
            useArea.setDisabled(true);
            useArea.setChecked(false);
        }
        previousArea = currentArea;
    }

    private void loadMap() {
        String uri = satServer + "/alaspatial/output/layers/" + pid + "/img.png";
        float opacity = Float.parseFloat("0.75");

        List<Double> bbox = new ArrayList<Double>();
        bbox.add(112.0);
        bbox.add(-44.0000000007);
        bbox.add(154.00000000084);
        bbox.add(-9.0);

        mc.addImageLayer(pid, layerLabel, uri, opacity, bbox);
        MapLayer mapLayer = mc.getMapLayer(layerLabel);
        if (mapLayer != null) {
            WMSStyle style = new WMSStyle();
            style.setName("Default");
            style.setDescription("Default style");
            style.setTitle("Default");
            style.setLegendUri(legendPath);

            System.out.println("legend:" + legendPath);
            mapLayer.addStyle(style);
            mapLayer.setSelectedStyleIndex(1);
        }
    }
}
