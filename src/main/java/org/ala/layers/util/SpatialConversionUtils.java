package org.ala.layers.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.kml.KMLConfiguration;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.xml.Parser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.google.common.io.Files;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;

public class SpatialConversionUtils {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(SpatialConversionUtils.class);

    // This code was adapted from the webportal class
    // org.ala.spatial.util.ShapefileUtils, method loadShapeFile
    public static String shapefileToWKT(File shpfile) {
        try {

            FileDataStore store = FileDataStoreFinder.getDataStore(shpfile);

            System.out.println("Loading shapefile. Reading content:");
            System.out.println(store.getTypeNames()[0]);

            SimpleFeatureSource featureSource = store.getFeatureSource(store.getTypeNames()[0]);

            SimpleFeatureCollection featureCollection = featureSource.getFeatures();
            SimpleFeatureIterator it = featureCollection.features();

            List<String> wktStrings = new ArrayList<String>();

            while (it.hasNext()) {
                SimpleFeature feature = (SimpleFeature) it.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();
                List<Object> attributes = feature.getAttributes();
                WKTWriter wkt = new WKTWriter();

                String wktString = wkt.write(geom);

                wktStrings.add(wktString);
            }

            featureCollection.close(it);

            if (wktStrings.size() > 1) {
                return "GEOMETRYCOLLECTION(" + StringUtils.join(wktStrings, ",") + ")";
            } else {
                return wktStrings.get(0);
            }

        } catch (Exception e) {
            System.out.println("Unable to load shapefile: ");
            e.printStackTrace(System.out);
        }

        return null;
    }

    public static File buildZippedShapeFile(String wktString, String filenamePrefix, String name, String description) throws IOException {

        File tempDir = Files.createTempDir();

        File shpFile = new File(tempDir, filenamePrefix + ".shp");

        saveShapefile(shpFile, wktString, name, description);

        File zipFile = new File(tempDir, filenamePrefix + ".zip");
        ZipOutputStream zipOS = new ZipOutputStream(new FileOutputStream(zipFile));

        List<File> excludedFiles = new ArrayList<File>();
        excludedFiles.add(zipFile);

        Iterator<File> iterFile = FileUtils.iterateFiles(shpFile.getParentFile(), new BaseFileNameInDirectoryFilter(filenamePrefix, tempDir, excludedFiles), null);

        while (iterFile.hasNext()) {
            File nextFile = iterFile.next();
            ZipEntry zipEntry = new ZipEntry(nextFile.getName());
            zipOS.putNextEntry(zipEntry);
            zipOS.write(FileUtils.readFileToByteArray(nextFile));
            zipOS.closeEntry();
        }

        zipOS.close();

        System.out.println(zipFile.getAbsolutePath());
        return zipFile;
    }

    protected static class BaseFileNameInDirectoryFilter implements IOFileFilter {

        private String baseFileName;
        private File parentDir;
        private List<File> excludedFiles;

        public BaseFileNameInDirectoryFilter(String baseFileName, File parentDir, List<File> excludedFiles) {
            this.baseFileName = baseFileName;
            this.parentDir = parentDir;
            this.excludedFiles = new ArrayList<File>(excludedFiles);
        }

        @Override
        public boolean accept(File file) {
            if (excludedFiles.contains(file)) {
                return false;
            }
            return file.getParentFile().equals(parentDir) && file.getName().startsWith(baseFileName);
        }

        @Override
        public boolean accept(File dir, String name) {
            if (excludedFiles.contains(new File(dir, name))) {
                return false;
            }
            return dir.equals(parentDir) && name.startsWith(baseFileName);
        }

    }

    public static File saveShapefile(File shpfile, String wktString, String name, String description) {
        try {
            String wkttype = "POLYGON";
            if (wktString.contains("GEOMETRYCOLLECTION") || wktString.contains("MULTIPOLYGON")) {
                wkttype = "GEOMETRYCOLLECTION";
            }
            final SimpleFeatureType TYPE = createFeatureType(wkttype);

            FeatureCollection collection = FeatureCollections.newCollection();
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);

            WKTReader wkt = new WKTReader();
            Geometry geom = wkt.read(wktString);
            featureBuilder.add(geom);

            if (name != null) {
                featureBuilder.set("name", name);
            }

            if (description != null) {
                featureBuilder.set("desc", description);
            }

            SimpleFeature feature = featureBuilder.buildFeature(null);
            collection.add(feature);

            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

            Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.put("url", shpfile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);

            ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            newDataStore.createSchema(TYPE);

            newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

            Transaction transaction = new DefaultTransaction("create");

            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

                featureStore.setTransaction(transaction);
                try {
                    featureStore.addFeatures(collection);
                    transaction.commit();

                } catch (Exception problem) {
                    problem.printStackTrace();
                    transaction.rollback();

                } finally {
                    transaction.close();
                }
            }

            return shpfile;
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return null;
        }
    }

    private static SimpleFeatureType createFeatureType(String type) {

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("ActiveArea");
        builder.setCRS(DefaultGeographicCRS.WGS84); // <- Coordinate reference
                                                    // system

        // add attributes in order
        if ("GEOMETRYCOLLECTION".equalsIgnoreCase(type)) {
            builder.add("area", MultiPolygon.class);
        } else {
            builder.add("area", Polygon.class);
        }
        builder.length(50).add("name", String.class); // <- 50 chars width for
                                                      // name field
        builder.length(100).add("desc", String.class); // 100 chars width
                                                              // for description
                                                              // field

        // build the type
        final SimpleFeatureType ActiveArea = builder.buildFeatureType();

        return ActiveArea;
    }
}