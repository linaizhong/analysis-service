/**
 * ************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * *************************************************************************
 */
package au.org.ala.spatial.web.services;

import au.org.ala.layers.intersect.SimpleRegion;
import au.org.ala.layers.intersect.SimpleShapeFile;
import au.org.ala.layers.util.LayerFilter;
import au.org.ala.spatial.util.AlaspatialProperties;
import au.org.ala.spatial.util.AnalysisJobSitesBySpecies;
import au.org.ala.spatial.util.AnalysisQueue;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.net.URLDecoder;

/**
 * Sites by species webservices.
 *
 * @author ajay
 */
@Controller
public class SitesBySpeciesWSController {

    @RequestMapping(value = "/ws/sitesbyspecies", method = RequestMethod.POST)
    public
    @ResponseBody
    String processgeoq(HttpServletRequest req) {

        try {

            long currTime = System.currentTimeMillis();

            String currentPath = AlaspatialProperties.getBaseOutputDir() + "output" + File.separator + "sitesbyspecies";
            String qname = URLDecoder.decode(req.getParameter("qname"), "UTF-8").replace("__", ".");
            String speciesq = URLDecoder.decode(req.getParameter("speciesq"), "UTF-8").replace("__", ".");
            String area = req.getParameter("area");
            String biocacheurl = URLDecoder.decode(req.getParameter("bs"), "UTF-8");
            double gridsize = Double.parseDouble(req.getParameter("gridsize"));
            int movingAverageSize = Integer.parseInt(req.getParameter("movingaveragesize"));
            boolean occurrencedensity = req.getParameter("occurrencedensity") != null;
            boolean speciesdensity = req.getParameter("speciesdensity") != null;
            boolean sitesbyspecies = req.getParameter("sitesbyspecies") != null;
            String areasqkm = req.getParameter("areasqkm");

            LayerFilter[] filter = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                filter = LayerFilter.parseLayerFilters(area);
            } else {
                region = SimpleShapeFile.parseWKT(area);
            }

            String pid = Long.toString(currTime);

            AnalysisJobSitesBySpecies sbs = new AnalysisJobSitesBySpecies(pid, currentPath, qname, speciesq, gridsize, region, filter, sitesbyspecies, occurrencedensity, speciesdensity, movingAverageSize, biocacheurl, areasqkm);

            StringBuffer inputs = new StringBuffer();
            inputs.append("pid:").append(pid);
            inputs.append(";qname:").append(qname);
            inputs.append(";speciesq:").append(speciesq);
            inputs.append(";gridsize:").append(gridsize);
            inputs.append(";area:").append(area);
            if (occurrencedensity) {
                inputs.append(";occurrencedensity:true").append(area);
            }
            if (speciesdensity) {
                inputs.append(";speciesdensity:true").append(area);
            }
            if (sitesbyspecies) {
                inputs.append(";sitesbyspecies:true").append(area);
            }
            sbs.setInputs(inputs.toString());
            AnalysisQueue.addJob(sbs);

            return pid;

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        return "";
    }

    @RequestMapping(value = "/ws/sitesbyspecies/estimate", method = RequestMethod.POST)
    public
    @ResponseBody
    String processgeoqEstimate(HttpServletRequest req) {

        try {

            long currTime = System.currentTimeMillis();

            String currentPath = AlaspatialProperties.getBaseOutputDir() + "output" + File.separator + "sitesbyspecies";
            String qname = URLDecoder.decode(req.getParameter("qname"), "UTF-8").replace("__", ".");
            String speciesq = URLDecoder.decode(req.getParameter("speciesq"), "UTF-8").replace("__", ".");
            String area = req.getParameter("area");
            String biocacheurl = URLDecoder.decode(req.getParameter("bs"), "UTF-8");
            double gridsize = Double.parseDouble(req.getParameter("gridsize"));
            int movingAverageSize = Integer.parseInt(req.getParameter("movingaveragesize"));
            boolean occurrencedensity = req.getParameter("occurrencedensity") != null;
            boolean speciesdensity = req.getParameter("speciesdensity") != null;
            boolean sitesbyspecies = req.getParameter("sitesbyspecies") != null;
            String areasqkm = req.getParameter("areasqkm");

            LayerFilter[] filter = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                filter = LayerFilter.parseLayerFilters(area);
            } else {
                region = SimpleShapeFile.parseWKT(area);
            }

            String pid = Long.toString(currTime);

            AnalysisJobSitesBySpecies sbs = new AnalysisJobSitesBySpecies(pid, currentPath, qname, speciesq, gridsize, region, filter, sitesbyspecies, occurrencedensity, speciesdensity, movingAverageSize, biocacheurl, areasqkm);

            StringBuffer inputs = new StringBuffer();
            inputs.append("pid:").append(pid);
            inputs.append(";qname:").append(qname);
            inputs.append(";speciesq:").append(speciesq);
            inputs.append(";gridsize:").append(gridsize);
            inputs.append(";area:").append(area);
            if (occurrencedensity) {
                inputs.append(";occurrencedensity:true").append(area);
            }
            if (speciesdensity) {
                inputs.append(";speciesdensity:true").append(area);
            }
            if (sitesbyspecies) {
                inputs.append(";sitesbyspecies:true").append(area);
            }
            sbs.setInputs(inputs.toString());
            //AnalysisQueue.addJob(sbs);

            return String.valueOf(sbs.getEstimate());

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        return "";
    }
}
