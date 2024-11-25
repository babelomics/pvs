package org.babelomics.csvs.lib.io;

import com.mongodb.*;
import com.mongodb.Cursor;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.babelomics.csvs.lib.models.*;
import org.babelomics.csvs.lib.models.prs.PgsGraphic;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.Query;
import org.opencb.biodata.models.feature.Region;

import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;


/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class CSVSQueryManager {

    private static final String EMPTY_VALUE = "-";
    final Datastore datastore;
    static final int DECIMAL_POSITIONS = 3;


    public CSVSQueryManager(String host, String dbName) {
        Morphia morphia = new Morphia();
        morphia.mapPackage("org.babelomics.csvs.lib.models");
        this.datastore = morphia.createDatastore(new MongoClient(host), dbName);
        this.datastore.ensureIndexes();
    }

    public CSVSQueryManager(String dbName) {
        this("localhost", dbName);
    }

    public CSVSQueryManager(Datastore datastore) {
        this.datastore = datastore;
    }

    public DiseaseGroup getDiseaseById(int id) {
        DiseaseGroup dg = datastore.createQuery(DiseaseGroup.class).field("groupId").equal(id).get();
        return dg;
    }

    public Technology getTechnologyById(int id) {
        Technology dg = datastore.createQuery(Technology.class).field("technologyId").equal(id).get();
        return dg;
    }

    public List<DiseaseGroup> getAllDiseaseGroups() {
        List<DiseaseGroup> res = datastore.createQuery(DiseaseGroup.class).order("groupId").asList();
        return res;
    }

    public List<Integer> getAllDiseaseGroupIds() {
        List<Integer> list = new ArrayList<>();
        for (DiseaseGroup dg : this.getAllDiseaseGroups()) {
            list.add(dg.getGroupId());
        }
        return list;
    }

    public List<Integer> getAllTechnologieIds() {
        List<Integer> list = new ArrayList<>();
        for (Technology t : this.getAllTechnologies()) {
            list.add(t.getTechnologyId());
        }
        return list;
    }

    public List<Technology> getAllTechnologies() {
        List<Technology> res = datastore.createQuery(Technology.class).order("technologyId").asList();
        return res;
    }


    public List<DiseaseGroup> getAllDiseaseGroupsOrderedBySample() {
        List<DiseaseGroup> res = datastore.createQuery(DiseaseGroup.class).order("-samples").asList();
        return res;
    }

    public List<Technology> getAllTechnologiesOrderedBySample() {
        List<Technology> res = datastore.createQuery(Technology.class).order("-samples").asList();
        return res;
    }

    public int getMaxDiseaseId() {
        int id = -1;
        DiseaseGroup query = datastore.createQuery(DiseaseGroup.class).order("-groupId").limit(1).get();
        if (query != null) {
            id = query.getGroupId();
        }
        return id;
    }

    public int getMaxTechnologyId() {
        int id = -1;
        Technology query = datastore.createQuery(Technology.class).order("-technologyId").limit(1).get();
        if (query != null) {
            id = query.getTechnologyId();
        }
        return id;
    }

    public List<List<Variant>> getVariantsByRegionList(List<Region> regions) {

        List<List<Variant>> res = new ArrayList<>();

        for (Region r : regions) {

            List<String> chunkIds = getChunkIds(r);
            Query<Variant> auxQuery = this.datastore.createQuery(Variant.class);

            auxQuery.filter("_at.chIds in", chunkIds).
                    filter("chromosome =", r.getChromosome()).
                    filter("position >=", r.getStart()).
                    filter("position <=", r.getEnd());

            List<Variant> variants = auxQuery.asList();

            for (Variant v : variants) {
                v.setStats(null);
                v.setAnnots(null);
            }
            res.add(variants);
        }

        return res;
    }

    public Variant getVariant(String chromosome, int position, String reference, String alternate, List<Integer> diseaseIds, List<Integer> technologyIds) {

        Region r = new Region(chromosome, position, position);

        List<String> chunkIds = getChunkIds(r);

        Query<Variant> query = this.datastore.createQuery(Variant.class);

        query.filter("_at.chIds in", chunkIds);
        query.filter("chromosome = ", chromosome);
        query.filter("position =", position);
        query.filter("reference =", reference.toUpperCase());
        query.filter("alternate =", alternate.toUpperCase());

        boolean disTechCheck = false;

        BasicDBList listDBObjects = new BasicDBList();

        if (diseaseIds != null && !diseaseIds.isEmpty()) {
            listDBObjects.add(new BasicDBObject("dgid", new BasicDBObject("$in", diseaseIds)));
            disTechCheck = true;

        }

        if (technologyIds != null && !technologyIds.isEmpty()) {
            listDBObjects.add(new BasicDBObject("tid", new BasicDBObject("$in", technologyIds)));
            disTechCheck = true;
        }

        if (disTechCheck) {
            query.filter("diseases elem", new BasicDBObject("$and", listDBObjects));
        }

        Variant res = query.get();

        if (res != null) {

            if (diseaseIds == null || diseaseIds.size() == 0) {
                diseaseIds = new ArrayList<>();
                List<DiseaseGroup> dgList = this.getAllDiseaseGroups();
                for (DiseaseGroup dg : dgList) {
                    diseaseIds.add(dg.getGroupId());
                }
            }
            if (technologyIds == null || technologyIds.size() == 0) {
                technologyIds = new ArrayList<>();
                List<Technology> techList = this.getAllTechnologies();
                for (Technology t : techList) {
                    technologyIds.add(t.getTechnologyId());
                }
            }

            // Map with disease-technology-samples
            Map<String, Integer> sampleCountMap = calculateSampleCount(diseaseIds, technologyIds, null);
            Map<String, Integer> sampleCountMap_XX = calculateSampleCount(diseaseIds, technologyIds, "XX");
            Map<String, Integer> sampleCountMap_XY = calculateSampleCount(diseaseIds, technologyIds, "XY");
            int sampleCount = calculateSampleCount(sampleCountMap);
            int sampleCount_XX = calculateSampleCount(sampleCountMap_XX);
            int sampleCount_XY = calculateSampleCount(sampleCountMap_XY);

            DiseaseCount diseaseCount = calculateStats(res, diseaseIds, technologyIds, sampleCount, sampleCountMap, sampleCount_XX, sampleCountMap_XX, sampleCount_XY, sampleCountMap_XY);

            res.setStats(diseaseCount);
            res.setDiseases(null);

        }
        return res;
    }


    public Variant getVariant(Variant variant, List<Integer> diseaseIds, List<Integer> technologyIds) {

        return this.getVariant(variant.getChromosome(), variant.getPosition(), variant.getReference(), variant.getAlternate(), diseaseIds, technologyIds);
    }

    public List<Variant> getVariants(List<Variant> variants, List<Integer> diseaseIds, List<Integer> technologyIds) {
        List<Variant> res = new ArrayList<>();

        for (Variant v : variants) {
            Variant resVariant = this.getVariant(v, diseaseIds, technologyIds);
            res.add(resVariant);
        }

        return res;
    }

    public List<List<IntervalFrequency>> getAllIntervalFrequencies(List<Region> regions, boolean histogramLogarithm, int histogramMax, int interval) {

        List<List<IntervalFrequency>> res = new ArrayList<>();
        for (Region r : regions) {
            res.add(getIntervalFrequencies(r, histogramLogarithm, histogramMax, interval));
        }

        return res;
    }


    public List<IntervalFrequency> getIntervalFrequencies(Region region, boolean histogramLogarithm, int histogramMax, int interval) {

        List<IntervalFrequency> res = new ArrayList<>();

        BasicDBObject start = new BasicDBObject("$gt", region.getStart());
        start.append("$lt", region.getEnd());

        BasicDBList andArr = new BasicDBList();
        andArr.add(new BasicDBObject("c", region.getChromosome()));
        andArr.add(new BasicDBObject("p", start));

        BasicDBObject match = new BasicDBObject("$match", new BasicDBObject("$and", andArr));


        BasicDBList divide1 = new BasicDBList();
        divide1.add("$p");
        divide1.add(interval);

        BasicDBList divide2 = new BasicDBList();
        divide2.add(new BasicDBObject("$mod", divide1));
        divide2.add(interval);

        BasicDBList subtractList = new BasicDBList();
        subtractList.add(new BasicDBObject("$divide", divide1));
        subtractList.add(new BasicDBObject("$divide", divide2));


        BasicDBObject subtract = new BasicDBObject("$subtract", subtractList);

        DBObject totalCount = new BasicDBObject("$sum", 1);

        BasicDBObject g = new BasicDBObject("_id", subtract);
        g.append("features_count", totalCount);
        BasicDBObject group = new BasicDBObject("$group", g);

        BasicDBObject sort = new BasicDBObject("$sort", new BasicDBObject("_id", 1));


        DBCollection collection = datastore.getCollection(Variant.class);

        List<BasicDBObject> aggList = new ArrayList<>();
        aggList.add(match);
        aggList.add(group);
        aggList.add(sort);

        Cursor aggregation = collection.aggregate(aggList, AggregationOptions.builder().outputMode(AggregationOptions.OutputMode.CURSOR).build());

        Map<Long, IntervalFrequency> ids = new HashMap<>();

        while (aggregation.hasNext()) {
            DBObject intervalObj = aggregation.next();

            Long _id = Math.round((Double) intervalObj.get("_id"));//is double

            IntervalFrequency intervalVisited = ids.get(_id);

            if (intervalVisited == null) {
                intervalVisited = new IntervalFrequency();

                intervalVisited.setId(_id);
                intervalVisited.setStart(getChunkStart(_id.intValue(), interval));
                intervalVisited.setEnd(getChunkEnd(_id.intValue(), interval));
                intervalVisited.setChromosome(region.getChromosome());
                intervalVisited.setFeaturesCount(Math.log((int) intervalObj.get("features_count")));
                ids.put(_id, intervalVisited);
            } else {
                double sum = intervalVisited.getFeaturesCount() + Math.log((int) intervalObj.get("features_count"));
                intervalVisited.setFeaturesCount(sum);
            }
        }

        int firstChunkId = getChunkId(region.getStart(), interval);
        int lastChunkId = getChunkId(region.getEnd(), interval);

        IntervalFrequency intervalObj;
        for (int chunkId = firstChunkId; chunkId <= lastChunkId; chunkId++) {
            intervalObj = ids.get((long) chunkId);

            if (intervalObj == null) {
                intervalObj = new IntervalFrequency(chunkId, getChunkStart(chunkId, interval), getChunkEnd(chunkId, interval), region.getChromosome(), 0);
            }
            res.add(intervalObj);
        }

        return res;
    }

    public Iterable<Variant> getVariantsByRegionList(List<Region> regions, List<Integer> diseaseIds, List<Integer> technologyIds, Integer skip, Integer limit, boolean skipCount, MutableLong count) {

        Criteria[] or = new Criteria[regions.size()];

        int i = 0;
        for (Region r : regions) {
            List<String> chunkIds = getChunkIds(r);
            Query<Variant> auxQuery = this.datastore.createQuery(Variant.class);

            List<Criteria> and = new ArrayList<>();
            and.add(auxQuery.criteria("_at.chIds").in(chunkIds));
            and.add(auxQuery.criteria("chromosome").equal(r.getChromosome()));
            and.add(auxQuery.criteria("position").greaterThanOrEq(r.getStart()));
            and.add(auxQuery.criteria("position").lessThanOrEq(r.getEnd()));

            or[i++] = auxQuery.and(and.toArray(new Criteria[and.size()]));
        }

        Query<Variant> query = this.datastore.createQuery(Variant.class);

        query.or(or);


        boolean disTechCheck = false;

        BasicDBList listDBObjects = new BasicDBList();

        if (diseaseIds != null && !diseaseIds.isEmpty()) {
            listDBObjects.add(new BasicDBObject("dgid", new BasicDBObject("$in", diseaseIds)));
            disTechCheck = true;

        }

        if (technologyIds != null && !technologyIds.isEmpty()) {
            listDBObjects.add(new BasicDBObject("tid", new BasicDBObject("$in", technologyIds)));
            disTechCheck = true;
        }

        if (disTechCheck) {
            query.filter("diseases elem", new BasicDBObject("$and", listDBObjects));
        }

        if (skip != null && limit != null) {
            query.offset(skip).limit(limit);
        }


        // System.out.println("query = " + query);

        Iterable<Variant> aux = query.fetch();

        if (!skipCount) {
            count.setValue(query.countAll());
        }

        List<Variant> res = new ArrayList<>();

        if (diseaseIds == null || diseaseIds.isEmpty()) {
            diseaseIds = new ArrayList<>();
            List<DiseaseGroup> dgList = this.getAllDiseaseGroups();
            for (DiseaseGroup dg : dgList) {
                diseaseIds.add(dg.getGroupId());
            }
        }

        if (technologyIds == null || technologyIds.isEmpty()) {
            technologyIds = new ArrayList<>();
            List<Technology> technologyList = this.getAllTechnologies();
            for (Technology t : technologyList) {
                technologyIds.add(t.getTechnologyId());
            }
        }

        // Map with disease-technology-samples
        Map<String, Integer> sampleCountMap = calculateSampleCount(diseaseIds, technologyIds, null);
        Map<String, Integer> sampleCountMap_XX = calculateSampleCount(diseaseIds, technologyIds, "XX");
        Map<String, Integer> sampleCountMap_XY = calculateSampleCount(diseaseIds, technologyIds, "XY");
        int sampleCount = calculateSampleCount(sampleCountMap);
        int sampleCount_XX = calculateSampleCount(sampleCountMap_XX);
        int sampleCount_XY = calculateSampleCount(sampleCountMap_XY);

        for (Variant v : aux) {
            v.setStats(calculateStats(v, diseaseIds, technologyIds, sampleCount, sampleCountMap, sampleCount_XX, sampleCountMap_XX, sampleCount_XY, sampleCountMap_XY));
            v.setDiseases(null);
            res.add(v);
        }

        return res;
    }

    /**
     * Get saturation order by num variants new / num samples disease
     * @param regions
     * @param diseaseIdsOriginal
     * @param technologyIds
     * @return
     */
    public Map<Region, List<SaturationElement>> getSaturationOrderIncrement(List<Region> regions, List<Integer> diseaseIdsOriginal, List<Integer> technologyIds) {

        Map<Region, List<SaturationElement>> map = new LinkedHashMap<>();

        List<DiseaseGroup> diseaseOrder = getAllDiseaseGroupsOrderedBySample();

        for (Region r : regions) {
            List<Integer> diseaseIds = new ArrayList<> (diseaseIdsOriginal);
            List<String> chunkIds = getChunkIds(r);

            List<SaturationElement> list = new ArrayList<>();
            Map<Integer, Integer> diseaseCount = new HashMap<>();

            // Get Variants
            Query<Variant> auxQuery = this.datastore.createQuery(Variant.class);
            List<Criteria> and = new ArrayList<>();
            and.add(auxQuery.criteria("_at.chIds").in(chunkIds));
            and.add(auxQuery.criteria("chromosome").equal(r.getChromosome()));
            and.add(auxQuery.criteria("position").greaterThanOrEq(r.getStart()));
            and.add(auxQuery.criteria("position").lessThanOrEq(r.getEnd()));
            Query<Variant> query = this.datastore.createQuery(Variant.class);
            query.and(and.toArray(new Criteria[and.size()]));

            // Get Panels
            List<Criteria> andPanels = new ArrayList<>();
            Query<org.babelomics.csvs.lib.models.Region> auxQueryPanels = this.datastore.createQuery(org.babelomics.csvs.lib.models.Region.class);
            andPanels.add(auxQueryPanels.criteria("c").equal(r.getChromosome()));
            andPanels.add(auxQueryPanels.criteria("e").greaterThanOrEq(r.getStart()));
            andPanels.add(auxQueryPanels.criteria("s").lessThanOrEq(r.getEnd()));
            Query<org.babelomics.csvs.lib.models.Region> queryRegion = this.datastore.createQuery(org.babelomics.csvs.lib.models.Region.class);
            queryRegion.and(andPanels.toArray(new Criteria[andPanels.size()]));
            List panelsRegions = this.datastore.getCollection(Region.class).distinct("pid", queryRegion.getQueryObject());

            List<Integer> diseaseView = new ArrayList<>();

            while (diseaseIds.size() > 0) {
                // Order disease
                Map<Integer, Long> mapDiseaseIncrement = new HashMap<>();
                Map<Integer, Double> mapDiseaseIncrementSample = new HashMap<>();
                Map<Integer, Integer> mapDiseaseSample = new HashMap<>();
                int sumAcum = list.stream().mapToInt(o -> o.getCount()).sum();

                // Calculate the largest increase ( num variant increase / num samples disease) by disease, and select the largest
                diseaseIds.forEach(dId -> {
                   // if (! diseaseView.contains(dId)) {
                        // Num variant when add a disease
                        Query<Variant> queryDisease = this.datastore.createQuery(Variant.class);
                        queryDisease.disableValidation();
                        queryDisease.and(and.toArray(new Criteria[and.size()]));
                        BasicDBList listDBObjectsDisease = new BasicDBList();
                        listDBObjectsDisease.add(new BasicDBObject("dgid", new BasicDBObject("$in", ListUtils.union(diseaseView, Arrays.asList(dId)))));
                        if (technologyIds != null && !technologyIds.isEmpty()) {
                            listDBObjectsDisease.add(new BasicDBObject("tid", new BasicDBObject("$in", technologyIds)));
                        }
                        queryDisease.filter("diseases elem", new BasicDBObject("$and", listDBObjectsDisease));

                        // Num samples when add a disease (genome+exome+panels)
                        int samplesUnionPanels = 0;
                        Query<File> querySampleDisease = this.datastore.createQuery(File.class);
                        querySampleDisease.disableValidation();
                        querySampleDisease.filter("dgid in ",  Arrays.asList(dId));
                        if (technologyIds != null && !technologyIds.isEmpty()) {
                            querySampleDisease.filter("tid in ", technologyIds);
                        }
                        if (panelsRegions != null && !panelsRegions.isEmpty() && panelsRegions.size() > 0) {
                            Query<File> auxQueryPid = this.datastore.createQuery(org.babelomics.csvs.lib.models.File.class);
                            querySampleDisease.or(auxQueryPid.criteria("pid").in(panelsRegions),auxQueryPid.criteria("pid").doesNotExist() );
                        } else{
                            querySampleDisease.criteria("pid").doesNotExist();
                        }
                        List<File> sampl = querySampleDisease.asList();
                        if (querySampleDisease != null && sampl.size() > 0)
                            samplesUnionPanels = sampl.stream().mapToInt(f -> f.getSamples()).sum();


                        // Increment variantes / num. samples disease
                        mapDiseaseIncrementSample.put(dId, samplesUnionPanels > 0 ? ((double) queryDisease.countAll() - sumAcum)/ samplesUnionPanels : 0);
                        mapDiseaseIncrement.put(dId, (queryDisease.countAll()- sumAcum));
                        mapDiseaseSample.put(dId, samplesUnionPanels);

                    //}
                });

                // Order
                LinkedHashMap<Integer, Double> sortedMap =
                        mapDiseaseIncrementSample.entrySet().stream().
                                filter(line -> !diseaseView.contains(line.getKey())).
                                sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).
                                collect(Collectors.toMap(e -> e.getKey(), (Map.Entry<Integer, Double> e) -> e.getValue(),
                                        (v1, v2) -> v2, LinkedHashMap::new));

                Integer key = sortedMap.keySet().iterator().next();

                // Select first
                Optional<DiseaseGroup> dgFirst = diseaseOrder.stream()
                        .filter(dg -> key.equals(dg.getGroupId()))
                        .findFirst();

                if (dgFirst.isPresent()) {
                    diseaseView.add(key);
                    diseaseIds.remove(key);
                    long increment = Integer.parseInt(String.valueOf(mapDiseaseIncrement.get(key))) ;
                    list.add(new SaturationElement(
                            key,
                            increment > 0 ? Integer.parseInt(String.valueOf(mapDiseaseIncrement.get(key))) : 0,
                            /// gest sample calculate ( genome + beds)
                            mapDiseaseSample.get(key)

                    ));
                }
            }

            map.put(r, list);
        }

        return map;
    }


    public Iterable<Variant> getAllVariants(List<Integer> diseaseIds, List<Integer> technologyIds, Integer skip, Integer limit, MutableLong count) {

        Query<Variant> query = this.datastore.createQuery(Variant.class);

        if (skip != null && limit != null) {
            query.offset(skip).limit(limit);
        }

        if (diseaseIds == null || diseaseIds.size() == 0) {
            diseaseIds = new ArrayList<>();
            List<DiseaseGroup> dgList = this.getAllDiseaseGroups();
            for (DiseaseGroup dg : dgList) {
                diseaseIds.add(dg.getGroupId());
            }

        }

        if (technologyIds == null || technologyIds.isEmpty()) {
            technologyIds = new ArrayList<>();
            List<Technology> technologyList = this.getAllTechnologies();
            for (Technology t : technologyList) {
                technologyIds.add(t.getTechnologyId());
            }
        }
        // Map with disease-technology-samples

        // Map with disease-technology-samples
        Map<String, Integer> sampleCountMap = calculateSampleCount(diseaseIds, technologyIds, null);
        Map<String, Integer> sampleCountMap_XX = calculateSampleCount(diseaseIds, technologyIds, "XX");
        Map<String, Integer> sampleCountMap_XY = calculateSampleCount(diseaseIds, technologyIds, "XY");
        int sampleCount = calculateSampleCount(sampleCountMap);
        int sampleCount_XX = calculateSampleCount(sampleCountMap_XX);
        int sampleCount_XY = calculateSampleCount(sampleCountMap_XY);

        Iterable<Variant> aux = query.fetch();
        Iterable<Variant> customIterable = new AllVariantsIterable(aux, diseaseIds, technologyIds, sampleCount, sampleCountMap, sampleCount_XX, sampleCountMap_XX, sampleCount_XY, sampleCountMap_XY);

        return customIterable;
    }

    /**
     * Calculate sum of all samples.
     * @return
     */
    public int calculateSampleCount() {
        int res = 0;

        BasicDBObject g = new BasicDBObject("_id", "");
        g.put("total", new BasicDBObject("$sum", "$s"));

        BasicDBObject p = new BasicDBObject("_id", 0);
        p.put("total", "$total");

        BasicDBObject group = new BasicDBObject("$group", g);
        BasicDBObject project = new BasicDBObject("$project", p);

        DBCollection collection = datastore.getCollection(File.class);

        List<BasicDBObject> aggList = new ArrayList<>();
        aggList.add(group);
        aggList.add(project);

        Cursor  aggregation = collection.aggregate( aggList, AggregationOptions.builder().outputMode(AggregationOptions.OutputMode.CURSOR).build() );

        while (aggregation.hasNext()) {
            res = (Integer) aggregation.next().get("total");
        }
        return res;
    }

    /**
     * Calculte sum of samples by diseased and technology
     * @param sampleCountMap
     * @return
     */
    private int calculateSampleCount(Map<String, Integer> sampleCountMap) {
        return sampleCountMap.values().stream().mapToInt(i -> i.intValue()).sum();
    }

    /**
     * Return map samples by diseased and technology (without regions)
     * @param diseaseId
     * @param technologyId
     * @return
     */
    public Map<String, Integer> calculateSampleCount(List<Integer> diseaseId, List<Integer> technologyId, String gender) {
        BasicDBList listDBObjects = new BasicDBList();

        listDBObjects.add(new BasicDBObject("dgid", new BasicDBObject("$in", diseaseId)));
        listDBObjects.add(new BasicDBObject("tid", new BasicDBObject("$in", technologyId)));


        listDBObjects.add(new BasicDBObject("pid", new BasicDBObject("$eq", null)));
        if (gender != null && !"".equals(gender))
            listDBObjects.add(new BasicDBObject("gender", new BasicDBObject("$eq", gender)));
        BasicDBObject match = new BasicDBObject("$match", new BasicDBObject("$and", listDBObjects));

        BasicDBList listDBObjectsGroupSub = new BasicDBList();
        listDBObjectsGroupSub.add("$dgid");
        listDBObjectsGroupSub.add(0);
        listDBObjectsGroupSub.add(-1);

        BasicDBList listDBObjectsGroupSubT = new BasicDBList();
        listDBObjectsGroupSubT.add("$tid");
        listDBObjectsGroupSubT.add(0);
        listDBObjectsGroupSubT.add(-1);

        BasicDBList listDBObjectsGroup = new BasicDBList();
        listDBObjectsGroup.add(new BasicDBObject("$substr", listDBObjectsGroupSub));
        listDBObjectsGroup.add("-");
        listDBObjectsGroup.add(new BasicDBObject("$substr", listDBObjectsGroupSubT));

        BasicDBObject g = new BasicDBObject("_id", new BasicDBObject("$concat", listDBObjectsGroup));
        g.append("Sum", new BasicDBObject("$sum", "$s"));
        BasicDBObject group = new BasicDBObject("$group", g);


        DBCollection collection = datastore.getCollection(File.class);

        List<BasicDBObject> aggList = new ArrayList<>();
        aggList.add(match);
        aggList.add(group);

        // System.out.println("QUERY: " + aggList);
        Cursor aggregation = collection.aggregate(aggList, AggregationOptions.builder().outputMode(AggregationOptions.OutputMode.CURSOR).build() );
        //  System.out.println("RESULT: " + aggregation.results());
        Map<String, Integer> res = new HashMap<>();

        while (aggregation.hasNext()) {
            DBObject fileObj = aggregation.next();
            res.put((String) fileObj.get("_id"), (Integer) fileObj.get("Sum"));
        }

        return res;
    }


    /**
     * Calculate sum all examples search in the all files (only with regions).
     * @param v Variant to calc samples
     * @param diseaseId
     * @param technologyId
     * @param datastore
     * @return Sum all examples
     */
    public static int initialCalculateSampleCount(Variant v, int diseaseId, int technologyId, Datastore datastore) {
        int res = 0;

        List objtIdRegion = new ArrayList<>();
        BasicDBObject filter = new BasicDBObject();
        filter.append("c", v.getChromosome());
        filter.append("s", new BasicDBObject("$lte",v.getPosition()));
        filter.append("e", new BasicDBObject("$gte",v.getPosition()));
        objtIdRegion = datastore.getCollection(org.babelomics.csvs.lib.models.Region.class).distinct("pid", filter);

        if (objtIdRegion != null && objtIdRegion.size() > 0) {
            // Replace with calculateSampleRegions
            List<BasicDBObject> aggList = new ArrayList<>();
            BasicDBObject match = new BasicDBObject().append("pid", new BasicDBObject("$in", objtIdRegion)).append("dgid",diseaseId).append("tid", technologyId);
            BasicDBObject group = new BasicDBObject().append("_id", new BasicDBObject().append("dgid", "$dgid").append("tid","$tid")).append("samples",new BasicDBObject("$sum","$s"));
            BasicDBObject project = new BasicDBObject().append("_id", 0).append("samples","1");
            aggList.add(new BasicDBObject("$match", match));
            aggList.add(new BasicDBObject("$group", group));

            Cursor aggregation = datastore.getCollection(File.class).aggregate(aggList, AggregationOptions.builder().outputMode(AggregationOptions.OutputMode.CURSOR).build());

            if (aggregation.hasNext()) {
                BasicDBObject oObj = (BasicDBObject) aggregation.next();
                res = (int) oObj.get("samples");
            }
        }

        return res;
    }






    public static Map calculateSampleRegions(Datastore datastore) {
        Map<String, Map> result = new HashMap<>();

        String[]  listGender = {"","XX","XY"};
        for ( String gender : listGender) {
            List<BasicDBObject> aggList = new ArrayList<>();
            BasicDBObject match = new BasicDBObject().append("pid", new BasicDBObject("$exists", true));
            BasicDBObject group = new BasicDBObject().append("_id", new BasicDBObject().append("dgid", "$dgid").append("tid", "$tid")
                    .append("pid", "$pid")).append("samples", new BasicDBObject("$sum", "$s"));
            if (!"".equals(gender))
                match.append("gender", gender);
            //BasicDBObject project = new BasicDBObject().append("_id", "$_id.pid").append("samples","1");

            aggList.add(new BasicDBObject("$match", match));
            aggList.add(new BasicDBObject("$group", group));

            Cursor aggregation = datastore.getCollection(File.class).aggregate(aggList, AggregationOptions.builder().outputMode(AggregationOptions.OutputMode.CURSOR).build());

            while (aggregation.hasNext()) {
                BasicDBObject oObj = (BasicDBObject) aggregation.next();
                String key = ((Map) oObj.get("_id")).get("dgid") + "_" + ((Map) oObj.get("_id")).get("tid") + (!"".equals(gender) ? "_"+ gender : "");
                if (result.containsKey(key)) {
                    Map value = result.get(key);
                    value.put(((Map) oObj.get("_id")).get("pid"), (int) oObj.get("samples"));
                    result.put(key, value);
                } else {
                    Map value = new HashMap();
                    value.put(((Map) oObj.get("_id")).get("pid"), (int) oObj.get("samples"));
                    result.put(key, value);
                }
            }
        }

        return result;
    }





    private DiseaseCount calculateStats(Variant v, List<Integer> diseaseId, List<Integer> technologyId, int sampleCount, Map<String, Integer> sampleCountMap,
                                        int sampleCount_XX,  Map<String, Integer> sampleCountMap_XX, int sampleCount_XY, Map<String, Integer> sampleCountMap_XY) {
        DiseaseCount dc;

        int gt00 = 0;
        int gt01 = 0;
        int gt11 = 0;
        int gtmissing = 0;
        int sampleCountVariant = 0;
        Map<String, Integer> sampleCountTemp = new HashMap<>();
        boolean existsRegions = false;


        if("X".equals(v.getChromosome())){
            sampleCountTemp =  new HashMap<>(sampleCountMap_XX);
            for(String key : sampleCountMap_XY.keySet()){
                sampleCountTemp.put(key, sampleCountTemp.containsKey(key)? sampleCountTemp.get(key)+sampleCountMap_XY.get(key):sampleCountMap_XY.get(key));
            }

        } else {
            if("Y".equals(v.getChromosome()))
                sampleCountTemp =  new HashMap<>(sampleCountMap_XY);
            else
                sampleCountTemp =  new HashMap<>(sampleCountMap);
        }

        // Variants by regions
        // System.out.println("\nCSVS (calculateStats): Variant= "+ v +  " Samples: "  + sampleCountTemp);
        for (DiseaseCount auxDc : v.getDiseases()) {
            if (diseaseId.contains(auxDc.getDiseaseGroup().getGroupId()) && technologyId.contains(auxDc.getTechnology().getTechnologyId())) {
                gt00 += auxDc.getGt00();
                gt01 += auxDc.getGt01();
                gt11 += auxDc.getGt11();
                gtmissing += auxDc.getGtmissing();
            }
        }

        // exists samples load in the panel
        if (v.getDiseasesSamplePanel() != null) {
            for (DiseaseSum auxDs : v.getDiseasesSamplePanel()) {
                if (diseaseId.contains(auxDs.getDiseaseGroupId()) && technologyId.contains(auxDs.getTechnologyId())) {
                    switch (v.getChromosome()){
                        case "X":
                            // exists samples load in the panel XX + XY
                            if (auxDs.getSumSampleRegionsXX() != 0 || auxDs.getSumSampleRegionsXY() != 0 ) {
                                String key = auxDs.getDiseaseGroupId() + "-" + auxDs.getTechnologyId();
                                int sum = sampleCountMap_XX.containsKey(key) ? sampleCountMap.get(key) : 0;
                                sum = sampleCountMap_XY.containsKey(key) ? sum + sampleCountMap_XY.get(key) : sum;
                                sampleCountTemp.put(key, (auxDs.getSumSampleRegionsXX() != 0 ? auxDs.getSumSampleRegionsXX(): 0) + (auxDs.getSumSampleRegionsXY() != 0 ? auxDs.getSumSampleRegionsXY(): 0) + sum);
                                existsRegions = true;
                            } else{
                                String key = auxDs.getDiseaseGroupId() + "-" + auxDs.getTechnologyId();
                                sampleCountTemp.put(key, 0);
                                existsRegions = true;
                            }
                            break;
                        case "Y":
                            // exists samples load in the panel XY
                            if (auxDs.getSumSampleRegionsXY() != 0) {
                                String key = auxDs.getDiseaseGroupId() + "-" + auxDs.getTechnologyId();
                                int sum = sampleCountMap_XY.containsKey(key) ? sampleCountMap_XY.get(key) : 0;
                                sampleCountTemp.put(key, auxDs.getSumSampleRegionsXY() + sum);
                                existsRegions = true;
                            }
                            break;
                        default:
                            // exists samples load in the panel (All)
                            if (auxDs.getSumSampleRegions() != 0) {
                                String key = auxDs.getDiseaseGroupId() + "-" + auxDs.getTechnologyId();
                                int sum = sampleCountMap.containsKey(key) ? sampleCountMap.get(key) : 0;
                                sampleCountTemp.put(key, auxDs.getSumSampleRegions() + sum);
                                existsRegions = true;
                            }
                    }
                }
            }
        }

        if (existsRegions)
            sampleCountVariant = sampleCountTemp.values().stream().mapToInt(i -> i.intValue()).sum();
        else {
            if("X".equals(v.getChromosome())){
                sampleCountVariant = sampleCount_XX + sampleCount_XY;
            }else{
                if("Y".equals(v.getChromosome()))
                    sampleCountVariant = sampleCount_XY;
                else
                    sampleCountVariant = sampleCount;
            }
        }


        gt00 = sampleCountVariant - gt01 - gt11 - gtmissing;

        float refFreq = 0;
        float altFreq;
        switch (v.getChromosome()) {
            case "X":
                //(mujeres01+mujeres00*2+hombres01+hombres00)/(mujeres totales * 2 + hombres totales)
                //refFreq = (float) (gt01 + gt00*2 + gt01) / ( 2*(gt00+gt01+gt11) + gt00 + gt01);
                altFreq = (float) (gt01 + gt11*2 + gt01) / ( 2*(gt00+gt01+gt11) + gt00 + gt01);
                refFreq = (float) 1 - altFreq;
                break;

            case "Y":
                refFreq = (float) gt00 / (gt00 + gt11);
                altFreq = (float) gt11 / (gt00 + gt11);
                break;

            default:
                int refCount, altCount;
                refCount = gt00 * 2 + gt01;
                altCount = gt11 * 2 + gt01;
                refFreq = (float) refCount / (refCount + altCount);
                altFreq = (float) altCount / (refCount + altCount);
        }

        float maf = Math.min(refFreq, altFreq);

        dc = new DiseaseCount(null, null, gt00, gt01, gt11, gtmissing);

        if (!Float.isNaN(refFreq)) {
            dc.setRefFreq(round(refFreq, DECIMAL_POSITIONS));
        }
        if (!Float.isNaN(altFreq)) {
            dc.setAltFreq(round(altFreq, DECIMAL_POSITIONS));
        }
        if (!Float.isNaN(maf)) {
            dc.setMaf(round(maf, DECIMAL_POSITIONS));
        }

        return dc;
    }



    public Query<Methylation> createQueryMethylation(List<Region> regions ,
                                            List<String> diseases,  List<String> technologies,
                                            List<String> tissues, List<String> genders,
                                                     Integer ageMin, Integer ageMax
                                            ) {
        Query<Methylation> query = this.datastore.createQuery(Methylation.class);

        Criteria[] or = new Criteria[regions.size()];
        int i = 0;
        for (Region r : regions) {
            Query<Methylation> auxQuery = this.datastore.createQuery(Methylation.class);

            List<Criteria> and = new ArrayList<>();
            and.add(auxQuery.criteria("chromosome").equal(r.getChromosome()));
            and.add(auxQuery.criteria("position").equal(r.getStart()));
            or[i++] = auxQuery.and(and.toArray(new Criteria[and.size()]));
        }
        if (or.length > 0) {
            query.and(or);
        }

        if (!diseases.isEmpty()) {
            if (diseases.contains(EMPTY_VALUE) && !diseases.stream().filter(t -> !t.equals(EMPTY_VALUE)).collect(Collectors.toList()).isEmpty()) {
                //orTech.add(auxQuery.criteria("t").doesNotExist());
                query.and(query.or(
                        query.criteria("d").doesNotExist(),
                        query.criteria("d").in(diseases.stream().filter(t -> !t.equals(EMPTY_VALUE)).map(t->Integer.parseInt(t)).collect(Collectors.toList()))
                ));
            } else {
                if (diseases.contains(EMPTY_VALUE)) {
                    query.and(query.criteria("d").doesNotExist());
                } else {
                    query.and(query.criteria("d").in(diseases.stream().filter(t -> !t.equals(EMPTY_VALUE)).map(t->Integer.parseInt(t)).collect(Collectors.toList())));
                }
            }
        }


        if (!technologies.isEmpty()) {
            if (technologies.contains(EMPTY_VALUE) && !technologies.stream().filter(t -> !t.equals(EMPTY_VALUE)).collect(Collectors.toList()).isEmpty()) {
                //orTech.add(auxQuery.criteria("t").doesNotExist());
                query.and(query.or(
                        query.criteria("tech").doesNotExist(),
                        query.criteria("tech").in(technologies.stream().filter(t -> !t.equals(EMPTY_VALUE)).collect(Collectors.toList()))
                ));
            } else {
                if (technologies.contains(EMPTY_VALUE)) {
                    query.and(query.criteria("tech").doesNotExist());
                } else {
                    query.and(query.criteria("tech").in(technologies.stream().filter(t -> !t.equals(EMPTY_VALUE)).collect(Collectors.toList())));
                }
            }
        }

        if (!tissues.isEmpty()) {
            if (tissues.contains(EMPTY_VALUE) && !tissues.stream().filter(t -> !t.equals(EMPTY_VALUE)).collect(Collectors.toList()).isEmpty()) {
                query.and(query.or(
                        query.criteria("tissue").doesNotExist(),
                        query.criteria("tissue").in(tissues.stream().filter(t -> !t.equals(EMPTY_VALUE)).collect(Collectors.toList()))
                ));
            } else {
                if (tissues.contains(EMPTY_VALUE)) {
                    query.and(query.criteria("tissue").doesNotExist());
                } else {
                    query.and(query.criteria("tissue").in(tissues.stream().filter(t -> !t.equals(EMPTY_VALUE)).collect(Collectors.toList())));
                }
            }
        }

        if (!genders.isEmpty()) {
            if (genders.contains(EMPTY_VALUE) && !genders.stream().filter(g -> !g.equals(EMPTY_VALUE)).collect(Collectors.toList()).isEmpty()) {
                query.and(query.or(
                        query.criteria("gender").doesNotExist(),
                        query.criteria("gender").in(genders.stream().filter(g -> !g.equals(EMPTY_VALUE)).collect(Collectors.toList()))
                ));
            } else {
                if (genders.contains(EMPTY_VALUE)) {
                    query.and(query.criteria("gender").doesNotExist());
                } else {
                    query.and(query.criteria("gender").in(genders.stream().filter(g -> !g.equals(EMPTY_VALUE)).collect(Collectors.toList())));
                }
            }
        }

        if (ageMin != null && ageMax == null) {
            query.and(query.criteria("age").greaterThanOrEq(ageMin));
        }
        if (ageMin == null && ageMax != null) {
            query.and(query.criteria("age").lessThanOrEq(ageMax));
        }
        if (ageMin != null && ageMax != null) {
            query.criteria("age").greaterThanOrEq(ageMin).add(query.criteria("age").lessThanOrEq(ageMax));
        }

        return query;
    }


    public List<MethylationFilter> getMethylationFilter(String filter) {
        Query<MethylationFilter> query = datastore.createQuery(MethylationFilter.class);
        if (filter != null && !"".equals(filter)) {
            query.criteria("filter").equal(filter);
            query.order("order");
        }
        return query.asList();
    }

    public List<Methylation> getMethylation(List<Region> regions ,
                List<String> diseases,  List<String> technologies,
                List<String> tissues, List<String> genders, Integer ageMin, Integer ageMax,
                Integer skip, Integer limit, boolean skipCount, MutableLong count) {

        List<Methylation> res = new ArrayList<>();
        Query<Methylation> query = createQueryMethylation(regions, diseases, technologies, tissues, genders, ageMin, ageMax);

        if (skip != null && limit != null & limit != -1) {
            query.offset(skip).limit(limit);
        }

        System.out.println(query);

        Iterable<Methylation> aux = query.fetch();

        if (!skipCount) {
            count.setValue(query.countAll());
        }

        for (Methylation m : aux) {
            res.add(m);
        }

        return res;
    }

    public List<Methylation> getAnnotationMethylation(List<Region> regions,
                                                      List<String> diseases, List<String> technologies,
                                                      List<String> tissues, List<String> genders,
                                                      Integer ageMin, Integer ageMax,
                                                      boolean all,
                                                      Integer skip, Integer limit, boolean skipCount, MutableLong count) {
        List<Methylation> res = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        DBObject match = new BasicDBObject();
        DBObject matchWithoutFilter = new BasicDBObject();
        List<DBObject> matchList = new ArrayList<>();

        // Reg
        List<DBObject> objsRegOr = new ArrayList<>();
        for (Region r : regions) {
            List<BasicDBObject> objAnd = new ArrayList<BasicDBObject>();
            objAnd.add(new BasicDBObject ().append("c", r.getChromosome()));
            objAnd.add(new BasicDBObject ().append("p", new BasicDBObject ("$gte", r.getStart() )));
            objAnd.add(new BasicDBObject ().append("p" ,new BasicDBObject ("$lte", r.getEnd() )));
            objsRegOr.add((new BasicDBObject ("$and", objAnd)));
        }
        matchList.add(new BasicDBObject ("$or", objsRegOr));
        matchWithoutFilter = new BasicDBObject ("$or", objsRegOr);

        List<DBObject> subpopulationsOr = new ArrayList<>();
        if (!diseases.isEmpty()) {
            if (diseases.contains(EMPTY_VALUE) && !diseases.stream().
                    filter(t -> !t.equals(EMPTY_VALUE)).
                    map(t->Integer.parseInt(t)).
                    collect(Collectors.toList()).isEmpty()) {
                subpopulationsOr.add(new BasicDBObject ("$or",
                        new BasicDBObject ().append("d", new BasicDBObject("$exists", false))
                                .append("d", new BasicDBObject("$in", diseases.stream().
                                        filter(t -> !t.equals(EMPTY_VALUE)).collect(Collectors.toList())))
                ));
            } else {
                if (diseases.contains(EMPTY_VALUE)) {
                    subpopulationsOr.add(new BasicDBObject ().append("d", new BasicDBObject("$exists", false)));
                } else {
                    subpopulationsOr.add(new BasicDBObject ().append("d", new BasicDBObject("$in",
                            diseases.stream().
                                    filter(t -> !t.equals(EMPTY_VALUE)).
                                    map(t->Integer.parseInt(t))
                                    .collect(Collectors.toList()))));
                }
            }
        }
        matchList.addAll(subpopulationsOr);

        List<DBObject> technologiesOr = new ArrayList<>();
        if (!technologies.isEmpty()) {
            if (technologies.contains(EMPTY_VALUE) && !technologies.stream().filter(t -> !t.equals(EMPTY_VALUE)).collect(Collectors.toList()).isEmpty()) {

                technologiesOr.add(new BasicDBObject ("$or",
                        Arrays.asList(
                                new BasicDBObject ().append("tech", new BasicDBObject("$exists", false)),
                                new BasicDBObject ().append("tech", new BasicDBObject("$in", technologies.stream().filter(t -> !t.equals(EMPTY_VALUE)).collect(Collectors.toList()))))
                ));
            } else {
                if (technologies.contains(EMPTY_VALUE)) {
                    technologiesOr.add(new BasicDBObject ().append("tech", new BasicDBObject("$exists", false)));
                } else {
                    technologiesOr.add(new BasicDBObject ().append("tech", new BasicDBObject("$in", technologies.stream().filter(t -> !t.equals(EMPTY_VALUE)).collect(Collectors.toList()))));
                }
            }
        }
        matchList.addAll(technologiesOr);

        List<DBObject> tissuesOr = new ArrayList<>();
        if (!tissues.isEmpty()) {
            if (tissues.contains(EMPTY_VALUE) && !tissues.stream().filter(t -> !t.equals(EMPTY_VALUE)).collect(Collectors.toList()).isEmpty()) {
                tissuesOr.add(new BasicDBObject ("$or",
                        Arrays.asList(
                                new BasicDBObject ().append("tissue", new BasicDBObject("$exists", false)),
                                new BasicDBObject ().append("tissue", new BasicDBObject("$in", tissues.stream().filter(t -> !t.equals(EMPTY_VALUE)).collect(Collectors.toList()))))
                ));
            } else {
                if (tissues.contains(EMPTY_VALUE)) {
                    tissuesOr.add(new BasicDBObject ().append("tissue", new BasicDBObject("$exists", false)));
                } else {
                    tissuesOr.add(new BasicDBObject ().append("tissue", new BasicDBObject("$in", tissues.stream().filter(t -> !t.equals(EMPTY_VALUE)).collect(Collectors.toList()))));
                }
            }
        }
        matchList.addAll(tissuesOr);

        List<DBObject> genderOr = new ArrayList<>();
        if (!genders.isEmpty()) {
            if (genders.contains(EMPTY_VALUE) && !tissues.stream().filter(t -> !t.equals(EMPTY_VALUE)).collect(Collectors.toList()).isEmpty()) {

                genderOr.add(new BasicDBObject ("$or",
                        Arrays.asList(
                                new BasicDBObject ().append("gender", new BasicDBObject("$exists", false)),
                                new BasicDBObject ().append("gender", new BasicDBObject("$in", genders.stream().filter(t -> !t.equals(EMPTY_VALUE)).collect(Collectors.toList()))))
                ));
            } else {
                if (genders.contains(EMPTY_VALUE)) {
                    genderOr.add(new BasicDBObject ().append("gender", new BasicDBObject("$exists", false)));
                } else {
                    genderOr.add(new BasicDBObject ().append("gender", new BasicDBObject("$in", genders.stream().filter(t -> !t.equals(EMPTY_VALUE)).collect(Collectors.toList()))));
                }
            }
        }

        if (ageMin != null && ageMax == null) {
            matchList.add(new BasicDBObject ().append("age", new BasicDBObject("$gte", ageMin)));
        }
        if (ageMin == null && ageMax != null) {
            matchList.add(new BasicDBObject ().append("age", new BasicDBObject("$lte", ageMax)));
        }

        if (ageMin != null && ageMax != null) {
            matchList.add(new BasicDBObject ().append("age", new BasicDBObject("$gte", ageMin).append("$lte", ageMax)));
        }

        matchList.addAll(genderOr);
        match.put("$and", matchList);

        List<BasicDBObject> aggList = new ArrayList<>();
        BasicDBObject group = new BasicDBObject().append("_id",  new BasicDBObject().
                        append("chromosome", "$c").
                        append("position", "$p") ).
                append("stdDev", new BasicDBObject("$stdDevSamp", "$value")).
                append("avg", new BasicDBObject("$avg", "$value")).
                append("samples", new BasicDBObject("$sum", 1));

        aggList.add(new BasicDBObject("$match", match));
        aggList.add(new BasicDBObject("$group", group));

        if (skip != null) {
            aggList.add(new BasicDBObject("$skip", skip));
        }

        if (limit != null && limit != -1) {
            aggList.add(new BasicDBObject("$limit", limit));
        }
        System.out.println("List Filter");
        System.out.println(aggList.toString());
        Cursor aggregation = datastore.getCollection(Methylation.class).aggregate(aggList, AggregationOptions.builder().outputMode(AggregationOptions.OutputMode.CURSOR).build());

        while (aggregation.hasNext()) {
            BasicDBObject oObj = (BasicDBObject) aggregation.next();
            Methylation newMethylation = new Methylation(
                    (String) ((BasicDBObject)oObj.get("_id")).get("chromosome"),
                    (Integer) ((BasicDBObject)oObj.get("_id")).get("position")
            );
            Map<String, Object> annots = new HashMap<>();
            annots.put("avg", oObj.get("avg"));
            annots.put("stdDev", oObj.get("stdDev"));
            annots.put("samples", oObj.get("samples"));
            newMethylation.setAnnots(annots);
            res.add(newMethylation);
        }

        // Calculate countAll without filter
        if (!skipCount) {
            List<BasicDBObject> aggListCountNoFilter = new ArrayList<>();
            BasicDBObject groupCountNoFilter = new BasicDBObject().append("_id", new BasicDBObject().
                            append("chromosome", "$c").
                            append("position", "$p")).
                    append("allSamples", new BasicDBObject("$sum", 1));
            aggListCountNoFilter.add(new BasicDBObject("$match", matchWithoutFilter));
            aggListCountNoFilter.add(new BasicDBObject("$group", groupCountNoFilter));
            if (skip != null) {
                aggListCountNoFilter.add(new BasicDBObject("$skip", skip));
            }

            if (limit != null && limit != -1) {
                aggListCountNoFilter.add(new BasicDBObject("$limit", limit));
            }

            Cursor aggregationCountNoFilter = datastore.getCollection(Methylation.class).aggregate(aggListCountNoFilter, AggregationOptions.builder().outputMode(AggregationOptions.OutputMode.CURSOR).build());
            while (aggregationCountNoFilter.hasNext()) {
                BasicDBObject oObj = (BasicDBObject) aggregationCountNoFilter.next();
                String chr = (String) ((BasicDBObject) oObj.get("_id")).get("chromosome");
                Integer pos = (Integer) ((BasicDBObject) oObj.get("_id")).get("position");

                boolean found = false;
                for (Methylation r : res) {
                    if (r.getChromosome().equals(chr) && r.getPosition() == pos) {
                        r.getAnnots().put("allSamples", oObj.get("allSamples"));
                        found = true;
                        continue;
                    }
                }
                // Only return all Samples if all=true
                if (!found && all){
                    Methylation newMethylation = new Methylation(
                            (String) ((BasicDBObject)oObj.get("_id")).get("chromosome"),
                            (Integer) ((BasicDBObject)oObj.get("_id")).get("position")
                    );
                    Map<String, Object> annots = new HashMap<>();
                    annots.put("allSamples", oObj.get("allSamples"));
                    newMethylation.setAnnots(annots);
                    res.add(newMethylation);
                }
            }


            // Count
            List<BasicDBObject> aggListCount = new ArrayList<>();
            BasicDBObject groupCount = new BasicDBObject().append("_id", new BasicDBObject().
                            append("chromosome", "$c").
                            append("position", "$p"));
            BasicDBObject groupSum = new BasicDBObject().append("_id", null).
                            append("count", new BasicDBObject().append("$sum", 1));

            aggListCount.add(new BasicDBObject("$match", match));
            aggListCount.add(new BasicDBObject("$group", groupCount));
            aggListCount.add(new BasicDBObject("$group", groupSum));
            System.out.println("List Count Sum");
            System.out.println(aggListCount.toString());
            Cursor aggregationCount = datastore.getCollection(Methylation.class).aggregate(aggListCount, AggregationOptions.builder().outputMode(AggregationOptions.OutputMode.CURSOR).build());
            while (aggregationCount.hasNext()) {
                BasicDBObject oObj = (BasicDBObject) aggregationCount.next();
                count.setValue( Long.valueOf((Integer) oObj.get("count")));
            }
        }
        return res;
    }

    private Map<String, Object> calculateMethylation(Region r){
        Map<String, Object> result = new HashMap<>();

        List<BasicDBObject> aggList = new ArrayList<>();
        BasicDBObject match = new BasicDBObject().append("c", new BasicDBObject("$eq",  r.getChromosome())).
                append("p", new BasicDBObject("$eq", r.getStart()));
        BasicDBObject group = new BasicDBObject().append("_id", null).
                append("stdDev", new BasicDBObject("$stdDevSamp", "$value")).
                append("avg", new BasicDBObject("$avg", "$value"));

        aggList.add(new BasicDBObject("$match", match));
        aggList.add(new BasicDBObject("$group", group));

        Cursor aggregation = datastore.getCollection(Methylation.class).aggregate(aggList, AggregationOptions.builder().outputMode(AggregationOptions.OutputMode.CURSOR).build());

        while (aggregation.hasNext()) {
            BasicDBObject oObj = (BasicDBObject) aggregation.next();
            //result.put("methylation", oObj);
            result = oObj;
        }

        return result;
    }


    private List<String> getChunkIds(Region region) {
        List<String> chunkIds = new LinkedList<>();

        int chunkSize = (region.getEnd() - region.getStart() > CSVSVariantCountsMongoWriter.CHUNK_SIZE_BIG) ?
                CSVSVariantCountsMongoWriter.CHUNK_SIZE_BIG : CSVSVariantCountsMongoWriter.CHUNK_SIZE_SMALL;
        int ks = chunkSize / 1000;
        int chunkStart = region.getStart() / chunkSize;
        int chunkEnd = region.getEnd() / chunkSize;

        for (int i = chunkStart; i <= chunkEnd; i++) {
            String chunkId = region.getChromosome() + "_" + i + "_" + ks + "k";
            chunkIds.add(chunkId);
        }

        return chunkIds;
    }

    private static float round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    protected int getChunkId(int position, int chunksize) {
        return position / chunksize;
    }

    private int getChunkStart(int id, int chunksize) {
        return (id == 0) ? 1 : id * chunksize;
    }

    private int getChunkEnd(int id, int chunksize) {
        return (id * chunksize) + chunksize - 1;
    }

    /**
     * ContactRequest: Get file.
     * @param v Id variant
     * @return
     */
    public List<File> getInfoFile(String v) {
        // Gets ids variants
        Variant variant =  getVariant(new Variant(v) , null, null);
        return getInfoFile(variant);
    }

    public List<File> getInfoFile(Variant variant) {
        // Get ids file
        DBCollection myCol =  this.datastore.getCollection(FileVariant.class);
        BasicDBObject project = new BasicDBObject();
        project.put("fid", 1);
        project.put("_id", 0);

        List<ObjectId> ids_file = new ArrayList<>();
        DBCursor myCursor = myCol.find(new BasicDBObject("$query", new BasicDBObject("vid",variant.getId())), project);
        while (myCursor.hasNext()) {
            DBObject obj = myCursor.next();
            ids_file.add((ObjectId) obj.get("fid"));
        }

        // Get files
        if(!ids_file.isEmpty()) {
            Query<File> queryFile = this.datastore.createQuery(File.class);
            queryFile.field("_id").in(ids_file);

            // Get info
           // System.out.println(queryFile);

            return queryFile.asList();
        } else
            return null;
    }

    /**
     * Get info metadata
     * @return
     */
    public List<Metadata> getMetadata() {
        List<Metadata> res = datastore.createQuery(Metadata.class).order("-date").asList();
        return res;
    }

    /**
     * Pathopedia: Get info pathopedia from a variant.
     * @param variants
     * @return
     */
    public List<Pathology> getVariantsPathopedia(List<Variant> variants, List<Integer> statesList){

        List<Variant> listVariant = getVariants( variants, null, null);

        List<ObjectId> ids = new ArrayList<>();
        for(Variant v:listVariant)
            if(v != null)
                ids.add(v.getId());

        Map listMatchDBObjects = new HashMap();
        listMatchDBObjects.put("v.$id",  new BasicDBObject("$in",ids));
        listMatchDBObjects.put("s",  new BasicDBObject("$in", statesList));

        BasicDBObject match = new BasicDBObject("$match", listMatchDBObjects);

        Map listDBObjects = new HashMap();
        listDBObjects.put("variant", "$v");
        listDBObjects.put("type", "$t");

        Map listGroupDBObjects = new HashMap();
        listGroupDBObjects.put("_id", listDBObjects);
        listGroupDBObjects.put("count",  new BasicDBObject("$sum", Opinion.PUBLISHED));

        BasicDBObject group = new BasicDBObject("$group", listGroupDBObjects);

        Map listGroup2DBObjects = new HashMap();
        Map push = new HashMap();
        push.put("t", "$_id.type");
        push.put("c", "$count");
        listGroup2DBObjects.put("_id", "$_id.variant");
        listGroup2DBObjects.put("total",  new BasicDBObject("$push", push));

        BasicDBObject group2 = new BasicDBObject("$group", listGroup2DBObjects);

        DBCollection collection = datastore.getCollection(Opinion.class);

        List<BasicDBObject> aggList = new ArrayList<>();
        aggList.add(match);
        aggList.add(group);
        aggList.add(group2);

        Cursor  aggregation = collection.aggregate( aggList, AggregationOptions.builder().outputMode(AggregationOptions.OutputMode.CURSOR).build() );

        List<Pathology> pathologies = new ArrayList<>();

        while (aggregation.hasNext()) {
            DBObject opObject = aggregation.next();
            DBRef aux = (DBRef) opObject.get("_id");
            ObjectId v = (ObjectId) aux.getId();

            Map type = new HashMap();
            BasicDBList total = (BasicDBList) opObject.get("total");
            for(int i=0 ; i < total.size(); i++){
                BasicDBObject o = (BasicDBObject) total.get(i);
                type.put(o.get("t"), o.get("c"));
            }

            Variant variant = this.datastore.createQuery(Variant.class).field("_id").equal(v).get();
            Pathology p = new Pathology( variant, type);

            pathologies.add(p);
        }


        return pathologies;
    }


    /**
     * Pathopedia: Get list all opinion.
     * @param v
     * @return
     */
    public List<Opinion> getAllOpinion(Variant v, List<Integer> statesList, String sort, Integer limit, Integer skip, List<String> clinSignificance) {
        List<Opinion> res = new ArrayList<Opinion>();

        List<Variant> variants=new ArrayList<>();
        variants.add(v);
        List<Pathology> pathology =  getVariantsPathopedia(variants, statesList);
        Pathology pat = (!pathology.isEmpty() && pathology != null) ? pathology.get(0) : null;

        if(pat != null) {
            Query<Opinion> query = this.datastore.createQuery(Opinion.class);
            query.field("v").equal(pat.getVariant());

            if (statesList != null && !statesList.isEmpty()) {
                query.field("s").in(statesList);
            }

            if(clinSignificance != null && !clinSignificance.isEmpty()){
                query.field("t").in(clinSignificance);
            }

            if (sort != null && !"".equals(sort)) {
                query.order(sort);
            }

            if (skip != null && limit != null) {
                query.offset(skip).limit(limit);
            }
            res = query.asList();

        }

        return res;
    }

    /**
     * Pathopedia: Add new opinion or update.
     * @param op
     * @return
     */
    public Opinion saveOpinion(Opinion op, int newState) {
        int oldState = op.getState();
        if (oldState != newState ||  op.getId() == null) {
            op.setState(newState);
            this.datastore.save(op);
         }

        return op;
    }


    /**
     * Pathopedia: Get opinion.
     * @param idOption
     * @return
     */
    public Opinion getOpinion(ObjectId idOption) {
        Opinion res = datastore.createQuery(Opinion.class).field("_id").equal(idOption).get();
        return res;
    }

    /**
     * ContactRequest: Get id file variants with person reference.
     * @param listVariants
     * @return
     */
    public List<String>  getVariantsAddressBook(List<Variant> listVariants) {
        List<String> result = new ArrayList<>();

        // Gets ids variants
        List<Variant> lv = getVariants( listVariants, null, null);

        Map<ObjectId, String> ids = new HashMap();
        for (Variant v:lv)
            if(v != null)
                ids.put(v.getId(), v.getChromosome() + ":"+ v.getPosition()  + ":"+ v.getReference() + ":"+ v.getAlternate());

        // Get variants with file
        DBCollection myCol =  this.datastore.getCollection(FileVariant.class);

        List<DBObject> idVariantsResults = myCol.distinct("vid", new BasicDBObject("vid",new BasicDBObject("$in", ids.keySet())));

        if (idVariantsResults!=null){
            for(int i = 0; i < idVariantsResults.size(); i++){
                result.add( ids.get(idVariantsResults.get(i)));
            }
        }

        return result;
    }

    /***************** Pharma **********************/
 /**
     * Get list pharmacogenomic variants.
     * @param regions
     * @param skip
     * @param limit
     * @param skipCount
     * @param count
     * @return
     */
    public Iterable<AnnotationPharmaAllele> getPharmaVariantsAnnotationByRegionList(List<Region> regions, List<Variant> variants,
                                                                                    List<String> names, List<String> listRs,
                                                                                    Integer skip, Integer limit, boolean skipCount, MutableLong count) {

        List<Criteria> or = new ArrayList<>();

        int i = 0;
        for (Region r : regions) {
            Query<AnnotationPharmaAllele> auxQuery = this.datastore.createQuery(AnnotationPharmaAllele.class);
            List<Criteria> and = new ArrayList<>();

            and.add(auxQuery.criteria("region.chromosome").equal(r.getChromosome()));
            and.add(auxQuery.criteria("region.start").greaterThanOrEq(r.getStart()));
            and.add(auxQuery.criteria("region.end").lessThanOrEq(r.getEnd()));

            or.add(auxQuery.and(and.toArray(new Criteria[and.size()])));
        }

        if ( variants != null && !variants.isEmpty()) {
            Criteria[] orVariant = new Criteria[variants.size()];
            int iVariant = 0;
            for (Variant v : variants) {
                Query<AnnotationPharmaAllele> auxQuery = this.datastore.createQuery(AnnotationPharmaAllele.class);

                List<Criteria> and = new ArrayList<>();
                and.add(auxQuery.criteria("variants.c").equal(v.getChromosome()));
                and.add(auxQuery.criteria("variants.p").equal(v.getPosition()));
                and.add(auxQuery.criteria("variants.r").equal(v.getReference()));
                //and.add(auxQuery.criteria("variants.a").equal(v.getAlternate()));

                orVariant[iVariant++] = auxQuery.and(and.toArray(new Criteria[and.size()]));
            }
            Query<Variant> queryVariant = this.datastore.createQuery(Variant.class);
            or.add(queryVariant.or(orVariant));
        }

        for (String n : names) {
            Query<AnnotationPharmaAllele> auxQuery = this.datastore.createQuery(AnnotationPharmaAllele.class);

            List<Criteria> and = new ArrayList<>();
            and.add(auxQuery.criteria("gene").equal(n.split("_")[0]));
            if (n.split("_").length > 1) {
                and.add(auxQuery.criteria("starAllele").equal(n.split("_")[1]));
            }

            or.add(auxQuery.and(and.toArray(new Criteria[and.size()])));
        }

        if (!listRs.isEmpty()){
            Query<AnnotationPharmaAllele> auxQuery = this.datastore.createQuery(AnnotationPharmaAllele.class);
            or.add(auxQuery.criteria("variants.i").in(listRs));
        }

        Query<AnnotationPharmaAllele> query = this.datastore.createQuery(AnnotationPharmaAllele.class);

        Criteria[] orCriteria = new Criteria[or.size()];
        int indexOrCriteria = 0;
        for ( Criteria c : or){
            orCriteria[indexOrCriteria] = c;
            indexOrCriteria++;
        }

        query.or(orCriteria);

        if (skip != null && limit != null) {
            query.offset(skip).limit(limit);
        }

        Iterable<AnnotationPharmaAllele> aux = query.fetch();

        if (!skipCount) {
            count.setValue(query.countAll());
        }

        List<AnnotationPharmaAllele> res = new ArrayList<>();

        for (AnnotationPharmaAllele v : aux) {
            // Add pharma id

            List<String> namesPharmaId = new ArrayList<>();
            namesPharmaId.add(v.getGene());
            namesPharmaId.add(v.getGene() + (v.getStarAllele().startsWith("*") ? "" : " ") +  v.getStarAllele());
            namesPharmaId.addAll(v.getVariants().stream().map(Variant::getIds).collect(Collectors.toList()));
            Query<AnnotationPharmaIds> queryId = this.datastore.createQuery(AnnotationPharmaIds.class);
            queryId.field("name").in(namesPharmaId);
            List<AnnotationPharmaIds> resPharmaIds = new ArrayList<>();

            for (AnnotationPharmaIds pharmaIds : queryId.fetch()) {
                resPharmaIds.add(pharmaIds);
            }
            v.setPharmaIds(resPharmaIds);

            res.add(v);
        }

        return res;
    }

    /***************** SecFindings **********************/

    /**
     * Get list secondary finding.
     * @param regions
     * @param skip
     * @param limit
     * @param skipCount
     * @param count
     * @return
     */
    public Iterable<AnnotationSecFindings> getSecFindingsVariantsAnnotationByRegionList(List<Region> regions, List<Variant> variants,
                                                                                    List<String> names, List<String> listRs,
                                                                                    Integer skip, Integer limit, boolean skipCount, MutableLong count) {

        List<Criteria> or = new ArrayList<>();

        int i = 0;
        for (Region r : regions) {
            Query<AnnotationSecFindings> auxQuery = this.datastore.createQuery(AnnotationSecFindings.class);
            List<Criteria> and = new ArrayList<>();

            and.add(auxQuery.criteria("region.chromosome").equal(r.getChromosome()));
            and.add(auxQuery.criteria("region.start").greaterThanOrEq(r.getStart()));
            and.add(auxQuery.criteria("region.end").lessThanOrEq(r.getEnd()));

            or.add(auxQuery.and(and.toArray(new Criteria[and.size()])));
        }

        if ( variants != null && !variants.isEmpty()) {
            Criteria[] orVariant = new Criteria[variants.size()];
            int iVariant = 0;
            for (Variant v : variants) {
                Query<AnnotationSecFindings> auxQuery = this.datastore.createQuery(AnnotationSecFindings.class);

                List<Criteria> and = new ArrayList<>();
                and.add(auxQuery.criteria("c").equal(v.getChromosome()));
                and.add(auxQuery.criteria("p").equal(v.getPosition()));
                and.add(auxQuery.criteria("r").equal(v.getReference()));
                and.add(auxQuery.criteria("a").equal(v.getAlternate()));

                orVariant[iVariant++] = auxQuery.and(and.toArray(new Criteria[and.size()]));
            }
            Query<Variant> queryVariant = this.datastore.createQuery(Variant.class);
            or.add(queryVariant.or(orVariant));
        }

        for (String n : names) {
            Query<AnnotationSecFindings> auxQuery = this.datastore.createQuery(AnnotationSecFindings.class);

            List<Criteria> and = new ArrayList<>();
            and.add(auxQuery.criteria("gene").equal(n.split("_")[0]));
            or.add(auxQuery.and(and.toArray(new Criteria[and.size()])));
        }

        if (!listRs.isEmpty()){
            Query<AnnotationSecFindings> auxQuery = this.datastore.createQuery(AnnotationSecFindings.class);
            or.add(auxQuery.criteria("i").in(listRs));
        }

        Query<AnnotationSecFindings> query = this.datastore.createQuery(AnnotationSecFindings.class);

        Criteria[] orCriteria = new Criteria[or.size()];
        int indexOrCriteria = 0;
        for ( Criteria c : or){
            orCriteria[indexOrCriteria] = c;
            indexOrCriteria++;
        }

        query.or(orCriteria);

        if (skip != null && limit != null) {
            query.offset(skip).limit(limit);
        }

        Iterable<AnnotationSecFindings> aux = query.fetch();

        if (!skipCount) {
            count.setValue(query.countAll());
        }

        return aux;
    }

 /***************** RPS **********************/

    public Iterable<Pgs> getPRS(List<String> searchPrsList, List<String> adSourcesList, List<String> adScoresList,
                                List<String> adLisPsgList,
                                int skip, int limit, boolean skipCount, MutableLong count) {
        Query<Pgs> query = this.datastore.createQuery(Pgs.class);

        Criteria[] or = new Criteria[searchPrsList.size() * 3];
        List<Criteria> and = new ArrayList<>();
        Query<Pgs> auxQuery = this.datastore.createQuery(Pgs.class);
        int i = 0;
        List<Criteria> orEfos = new ArrayList<>();
        for (String id : searchPrsList) {
            orEfos.add(auxQuery.criteria("idPgs").equal(id));
            orEfos.add(auxQuery.criteria("efos.id").equal(id));
            orEfos.add(auxQuery.criteria("efos.label").containsIgnoreCase(id));
        }
        if (!orEfos.isEmpty()) {
            and.add(auxQuery.or(orEfos.toArray(new Criteria[orEfos.size()])));
        }

        if (adSourcesList != null && !adSourcesList.isEmpty()) {
            List<Criteria> ancestrySources = new ArrayList<>();
            if (adSourcesList.contains(Pgs.NOT_REPORTED)){
                ancestrySources.add(auxQuery.criteria("sources").doesNotExist());
            }
            ancestrySources.add(auxQuery.criteria("sources.ancestry").in(adSourcesList));
            and.add(auxQuery.or(ancestrySources.toArray(new Criteria[ancestrySources.size()])));
        }

        if (adScoresList != null && !adScoresList.isEmpty()) {
            List<Criteria> ancestryScores = new ArrayList<>();
            if (adScoresList.contains(Pgs.NOT_REPORTED)){
                ancestryScores.add(auxQuery.criteria("scores").doesNotExist());
            }

            ancestryScores.add(auxQuery.criteria("scores.ancestry").in(adScoresList));
            and.add(auxQuery.or(ancestryScores.toArray(new Criteria[ancestryScores.size()])));
        }

        if (adLisPsgList != null && !adLisPsgList.isEmpty()) {
             List<Criteria> ancestryListPgs = new ArrayList<>();
            if (adLisPsgList.contains(Pgs.NOT_REPORTED)){
                ancestryListPgs.add(auxQuery.criteria("listPgs").doesNotExist());
            }
            ancestryListPgs.add(auxQuery.criteria("listPgs.ancestry").in(adLisPsgList));

            and.add(auxQuery.or(ancestryListPgs.toArray(new Criteria[ancestryListPgs.size()])));
        }

        query.and(and.toArray(new Criteria[and.size()]));

        //if (skip != 0 && limit != 0) {
        query.offset(skip).limit(limit);
        //}

        Iterable<Pgs> aux = query.fetch();

        if (!skipCount) {
            count.setValue(query.countAll());
        }

        return aux;
    }

    public List<PgsGraphic> getGraphicPRS(String idPgs, String sequencingType, List diseases, MutableLong count) {
        Query<PgsGraphic> query = this.datastore.createQuery(PgsGraphic.class);
        if (idPgs != null) {
            query.and(query.criteria("idPgs").equal(idPgs));
        }
        if (sequencingType != null) {
            if (PgsGraphic.EXOME.equals(sequencingType)) {
                query.and(query.criteria("exome").exists());
            }
            if (PgsGraphic.GENOME.equals(sequencingType)) {
                query.and(query.criteria("genome").exists());
            }
        }
        if (!diseases.isEmpty()) {
            query.and(query.criteria("gid").in(diseases));
        }

        List<PgsGraphic> aux = query.asList();
        count.setValue(query.countAll());

        return aux;
    }


    /**
     * Get list ancestries distinct and sort.
     * @param ad
     * @param count
     * @return
     */
    public List<String> getAncestries(String ad, MutableLong count) {
        List ancestries = new ArrayList();
        DBCollection dbCollection= this.datastore.getCollection(Pgs.class);

        if (!ad.isEmpty()) {
            ancestries = (List) dbCollection.distinct(ad + ".ancestry").stream()
                    .sorted().collect(Collectors.toList());

            // Add no reported
            if (!ancestries.contains(Pgs.NOT_REPORTED)) {
                Query<Pgs> query = this.datastore.createQuery(Pgs.class);
                query.and(query.criteria(ad + ".ancestry").doesNotExist());
                if (query.fetch().hasNext()) {
                    ancestries.add(Pgs.NOT_REPORTED);
                }
            }
        }
        count.setValue(ancestries.size());
        return ancestries;
    }


    /**
     * Get list diseases with any pgs.
     * @return
     */
    public List<DiseaseGroup> getAllDiseasePRS() {
        List<DiseaseGroup> dgList = this.getAllDiseaseGroups();

        DBCollection dbCollection= this.datastore.getCollection(PgsGraphic.class);

        List<String> diseasesIdPRS = (List) dbCollection.distinct("gid").stream().sorted().collect(Collectors.toList());

        return dgList.stream()
                .filter(d -> diseasesIdPRS.contains(d.getGroupId()))
                .collect(Collectors.toList());
    }


    class AllVariantsIterable implements Iterable<Variant> {

        private Iterable iterable;
        private List<Integer> diseaseIds;
        private List<Integer> technologyIds;
        private int sampleCount;
        private int sampleCount_XX;
        private int sampleCount_XY;
        private Map<String, Integer> sampleCountMap;
        private Map<String, Integer> sampleCountMap_XX;
        private Map<String, Integer> sampleCountMap_XY;

        public AllVariantsIterable(Iterable iterable, List<Integer> diseaseIds, List<Integer> technologyIds, int sampleCount, Map<String, Integer> sampleCountMap,
                                   int sampleCount_XX, Map<String, Integer> sampleCountMap_XX, int sampleCount_XY, Map<String, Integer> sampleCountMap_XY) {
            this.iterable = iterable;
            this.diseaseIds = diseaseIds;
            this.technologyIds = technologyIds;
            this.sampleCount = sampleCount;
            this.sampleCount_XX = sampleCount_XX;
            this.sampleCount_XY = sampleCount_XY;
            this.sampleCountMap = sampleCountMap;
            this.sampleCountMap_XX = sampleCountMap_XX;
            this.sampleCountMap_XY = sampleCountMap_XY;
        }

        @Override
        public Iterator<Variant> iterator() {
            Iterator<Variant> it = new AllVariantsIterator(this.iterable.iterator());
            return it;
        }

        class AllVariantsIterator implements Iterator<Variant> {

            private Iterator<Variant> it;

            public AllVariantsIterator(Iterator<Variant> it) {
                this.it = it;
            }

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Variant next() {
                Variant v = this.it.next();
                v.setStats(calculateStats(v, diseaseIds, technologyIds, sampleCount, sampleCountMap, sampleCount_XX, sampleCountMap_XX, sampleCount_XY, sampleCountMap_XY));
                v.setDiseases(null);
                return v;
            }

            @Override
            public void remove() {

            }
        }
    }

    static class AggregationElem {
        DBRef ref;
        int samples;
        int variants;

        public AggregationElem() {
        }

        public DBRef getRef() {
            return ref;
        }

        public void setRef(DBRef ref) {
            this.ref = ref;
        }


        public int getSamples() {
            return samples;
        }

        public void setSamples(int samples) {
            this.samples = samples;
        }

        public int getVariants() {
            return variants;
        }

        public void setVariants(int variants) {
            this.variants = variants;
        }

        @Override
        public String toString() {
            return "DiseaseElem{" +
                    ", ref=" + ref +
                    ", samples=" + samples +
                    ", variants=" + variants +
                    '}';
        }
    }
}
