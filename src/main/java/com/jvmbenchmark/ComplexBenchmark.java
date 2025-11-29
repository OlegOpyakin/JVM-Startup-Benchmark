package com.jvmbenchmark;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.function.*;

import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.random.MersenneTwister;

// ============================================
// Complex benchmark simulating web application workload (similar to Spring PetClinic)
// Extended with Renaissance-inspired computational patterns:
// - Complex nested JSON parsing and validation
// - 100+ fragmented methods for extensive JIT compilation
// - Graph algorithms and tree traversals
// - Statistical computations and data transformations
// - Polymorphic call sites and interface-based design
// ============================================

public class ComplexBenchmark {
    private static final int WARMUP_ITERATIONS = 20000;
    private static final int BENCHMARK_ITERATIONS = 100000;
    private static final int MEASUREMENT_POINTS = 200;
    
    // ============================================
    // Complex JSON Document Classes
    // ============================================
    
    static class JsonDocument {
        Map<String, Object> root = new LinkedHashMap<>();
        
        void put(String key, Object value) {
            root.put(key, value);
        }
        
        Object get(String key) {
            return root.get(key);
        }
        
        String serialize() {
            return serializeObject(root);
        }
        
        private String serializeObject(Object obj) {
            if (obj == null) return "null";
            if (obj instanceof String) return "\"" + escape((String)obj) + "\"";
            if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
            if (obj instanceof Map) return serializeMap((Map<?, ?>)obj);
            if (obj instanceof List) return serializeList((List<?>)obj);
            return "\"" + obj.toString() + "\"";
        }
        
        private String serializeMap(Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                sb.append(serializeObject(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        
        private String serializeList(List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(serializeObject(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        
        private String escape(String str) {
            return str.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
    
    static class JsonParser {
        private String json;
        private int pos;
        
        JsonParser(String json) {
            this.json = json;
            this.pos = 0;
        }
        
        Object parse() {
            skipWhitespace();
            return parseValue();
        }
        
        private Object parseValue() {
            skipWhitespace();
            if (pos >= json.length()) return null;
            
            char c = json.charAt(pos);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            if (c == '-' || Character.isDigit(c)) return parseNumber();
            
            throw new RuntimeException("Unexpected character: " + c);
        }
        
        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // skip {
            skipWhitespace();
            
            while (pos < json.length() && json.charAt(pos) != '}') {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                if (json.charAt(pos) != ':') throw new RuntimeException("Expected :");
                pos++;
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (json.charAt(pos) == ',') {
                    pos++;
                } else if (json.charAt(pos) != '}') {
                    throw new RuntimeException("Expected , or }");
                }
            }
            pos++; // skip }
            return map;
        }
        
        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // skip [
            skipWhitespace();
            
            while (pos < json.length() && json.charAt(pos) != ']') {
                list.add(parseValue());
                skipWhitespace();
                if (json.charAt(pos) == ',') {
                    pos++;
                } else if (json.charAt(pos) != ']') {
                    throw new RuntimeException("Expected , or ]");
                }
            }
            pos++; // skip ]
            return list;
        }
        
        private String parseString() {
            pos++; // skip "
            StringBuilder sb = new StringBuilder();
            while (pos < json.length() && json.charAt(pos) != '"') {
                if (json.charAt(pos) == '\\') {
                    pos++;
                    if (pos < json.length()) {
                        sb.append(json.charAt(pos));
                    }
                } else {
                    sb.append(json.charAt(pos));
                }
                pos++;
            }
            pos++; // skip "
            return sb.toString();
        }
        
        private Number parseNumber() {
            int start = pos;
            if (json.charAt(pos) == '-') pos++;
            while (pos < json.length() && (Character.isDigit(json.charAt(pos)) || json.charAt(pos) == '.')) {
                pos++;
            }
            String numStr = json.substring(start, pos);
            return numStr.contains(".") ? Double.parseDouble(numStr) : Long.parseLong(numStr);
        }
        
        private Boolean parseBoolean() {
            if (json.startsWith("true", pos)) {
                pos += 4;
                return true;
            } else if (json.startsWith("false", pos)) {
                pos += 5;
                return false;
            }
            throw new RuntimeException("Invalid boolean");
        }
        
        private Object parseNull() {
            if (json.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new RuntimeException("Invalid null");
        }
        
        private void skipWhitespace() {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
                pos++;
            }
        }
    }
    
    // ============================================
    // Graph and Tree Data Structures
    // ============================================
    
    static class GraphNode {
        int id;
        List<GraphNode> neighbors = new ArrayList<>();
        Map<String, Object> metadata = new HashMap<>();
        
        GraphNode(int id) {
            this.id = id;
        }
        
        void addNeighbor(GraphNode node) {
            neighbors.add(node);
        }
    }
    
    static class TreeNode {
        int value;
        TreeNode left;
        TreeNode right;
        int height;
        
        TreeNode(int value) {
            this.value = value;
            this.height = 1;
        }
    }
    
    // ============================================
    // Business Logic Interfaces
    // ============================================
    
    interface DataProcessor<T, R> {
        R process(T data);
    }
    
    interface Validator<T> {
        boolean validate(T data);
    }
    
    interface Transformer<T> {
        T transform(T data);
    }
    
    interface Aggregator<T, R> {
        R aggregate(Collection<T> items);
    }
    
    // Simulated data models
    static class Owner {
        String firstName;
        String lastName;
        String address;
        String city;
        String telephone;
        List<Pet> pets = new ArrayList<>();
        
        Owner(String firstName, String lastName, String address, String city, String telephone) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.address = address;
            this.city = city;
            this.telephone = telephone;
        }
        
        public String toJSON() {
            StringBuilder json = new StringBuilder("{");
            json.append("\"firstName\":\"").append(firstName).append("\",");
            json.append("\"lastName\":\"").append(lastName).append("\",");
            json.append("\"address\":\"").append(address).append("\",");
            json.append("\"city\":\"").append(city).append("\",");
            json.append("\"telephone\":\"").append(telephone).append("\",");
            json.append("\"pets\":[");
            for (int i = 0; i < pets.size(); i++) {
                json.append(pets.get(i).toJSON());
                if (i < pets.size() - 1) json.append(",");
            }
            json.append("]}");
            return json.toString();
        }
    }
    
    static class Pet {
        String name;
        String birthDate;
        String type;
        List<Visit> visits = new ArrayList<>();
        
        Pet(String name, String birthDate, String type) {
            this.name = name;
            this.birthDate = birthDate;
            this.type = type;
        }
        
        public String toJSON() {
            return "{\"name\":\"" + name + "\",\"birthDate\":\"" + birthDate + 
                   "\",\"type\":\"" + type + "\",\"visits\":" + visits.size() + "}";
        }
    }
    
    static class Visit {
        String date;
        String description;
        
        Visit(String date, String description) {
            this.date = date;
            this.description = description;
        }
    }
    
    // Repository simulation
    static class OwnerRepository {
        private Map<Integer, Owner> owners = new ConcurrentHashMap<>();
        private int nextId = 1;
        
        public void save(Owner owner) {
            owners.put(nextId++, owner);
        }
        
        public Owner findById(int id) {
            return owners.get(id);
        }
        
        public List<Owner> findByLastName(String lastName) {
            return owners.values().stream()
                .filter(o -> o.lastName.toLowerCase().contains(lastName.toLowerCase()))
                .collect(Collectors.toList());
        }
        
        public List<Owner> findAll() {
            return new ArrayList<>(owners.values());
        }
    }
    
    public static void main(String[] args) {
        long jvmStartTime = System.currentTimeMillis();
        String outputFile = args.length > 0 ? args[0] : "benchmark_results.csv";
        int runNumber = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        
        // Check if file exists to determine if we need to write header
        boolean fileExists = new java.io.File(outputFile).exists();
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile, true))) {
            // Only write header if file doesn't exist
            if (!fileExists) {
                writer.println("run,timestamp_ms,iteration,latency_ns,phase");
            }
            
            System.out.println("Complex JVM Benchmark Starting...");
            System.out.println("Simulating web application workload (like Spring PetClinic)");
            System.out.println("JVM Start Time: " + jvmStartTime);
            
            // Measure startup phase
            List<LatencyMeasurement> startupMeasurements = new ArrayList<>();
            System.out.println("Phase 1: Startup and Cold Run");
            
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                long start = System.nanoTime();
                performComplexWork(i);
                long latency = System.nanoTime() - start;
                
                if (i % (WARMUP_ITERATIONS / MEASUREMENT_POINTS) == 0) {
                    long timestamp = System.currentTimeMillis() - jvmStartTime;
                    startupMeasurements.add(new LatencyMeasurement(timestamp, i, latency, "warmup"));
                    writer.println(runNumber + "," + timestamp + "," + i + "," + latency + ",warmup");
                }
            }
            
            System.out.println("Phase 2: Warmed Up Performance");
            
            // Measure peak performance after warmup
            List<LatencyMeasurement> peakMeasurements = new ArrayList<>();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                long start = System.nanoTime();
                performComplexWork(WARMUP_ITERATIONS + i);
                long latency = System.nanoTime() - start;
                
                if (i % (BENCHMARK_ITERATIONS / MEASUREMENT_POINTS) == 0) {
                    long timestamp = System.currentTimeMillis() - jvmStartTime;
                    peakMeasurements.add(new LatencyMeasurement(timestamp, i + WARMUP_ITERATIONS, latency, "peak"));
                    writer.println(runNumber + "," + timestamp + "," + (i + WARMUP_ITERATIONS) + "," + latency + ",peak");
                }
            }
            
            long totalTime = System.currentTimeMillis() - jvmStartTime;

            // Calculate statistics
            double startupAvg = calculateAverage(startupMeasurements);
            double peakAvg = calculateAverage(peakMeasurements);
            double startupP99 = calculatePercentile(startupMeasurements, 99);
            double peakP99 = calculatePercentile(peakMeasurements, 99);
            
            // Print summary
            System.out.println("\n=== Benchmark Results (Run #" + runNumber + ") ===");
            System.out.println("Total Execution Time: " + totalTime + " ms");
            System.out.println("Warmup Iterations: " + WARMUP_ITERATIONS);
            System.out.println("Benchmark Iterations: " + BENCHMARK_ITERATIONS);
            System.out.println("\nStartup Phase:");
            System.out.println("  Average Latency: " + String.format("%.2f", startupAvg) + " ns");
            System.out.println("  P99 Latency: " + String.format("%.2f", startupP99) + " ns");
            System.out.println("\nPeak Performance Phase:");
            System.out.println("  Average Latency: " + String.format("%.2f", peakAvg) + " ns");
            System.out.println("  P99 Latency: " + String.format("%.2f", peakP99) + " ns");
            System.out.println("\nImprovement: " + String.format("%.2f", (startupAvg - peakAvg) / startupAvg * 100) + "%");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    // Complex workload simulating web application operations
    private static void performComplexWork(int globalIteration) {
        OwnerRepository repository = buildRepositoryWithRandomData();

        List<Owner> allOwners = performRepositoryQueries(repository);
        String basicJson = serializeOwnersToJson(allOwners);

        validateAndProcessJSON(basicJson);
        runReflectionWorkload(Owner.class);

        Map<String, List<Owner>> ownersByCity = groupOwnersByCity(allOwners);
        Map<String, Long> petCountByOwner = computePetCountByOwner(allOwners);
        double avgPetsPerOwner = calculateAveragePetsPerOwner(allOwners);

        String complexJson = buildComplexJson(allOwners, ownersByCity, petCountByOwner, avgPetsPerOwner);
        deepJsonProcessing(complexJson);

        runGraphAndTreeWorkloads(allOwners, petCountByOwner);
        runExceptionSimulation(allOwners);

        // New: heavy numerical workloads that become active gradually over iterations
        runCommonsMathWorkloads(globalIteration, allOwners, ownersByCity, petCountByOwner, avgPetsPerOwner);

        // Prevent dead code elimination
        if (basicJson.length() < 0 && avgPetsPerOwner < 0) {
            System.out.println("Never happens");
        }
    }
    
    private static OwnerRepository buildRepositoryWithRandomData() {
        OwnerRepository repository = new OwnerRepository();
        populateRepository(repository, 5);
        return repository;
    }

    private static void populateRepository(OwnerRepository repository, int ownerCount) {
        for (int i = 0; i < ownerCount; i++) {
            populateSingleOwner(repository, i);
        }
    }

    private static void populateSingleOwner(OwnerRepository repository, int seed) {
        Owner owner = createRandomOwner(seed);
        repository.save(owner);
    }

    private static List<Owner> performRepositoryQueries(OwnerRepository repository) {
        List<Owner> allOwners = repository.findAll();
        List<Owner> filtered = repository.findByLastName("Smith");
        if (filtered.size() > allOwners.size() + 1) {
            throw new IllegalStateException("Unexpected repository state");
        }
        return allOwners;
    }

    private static String serializeOwnersToJson(List<Owner> allOwners) {
        StringBuilder jsonResponse = new StringBuilder("[");
        for (int i = 0; i < allOwners.size(); i++) {
            jsonResponse.append(allOwners.get(i).toJSON());
            if (i < allOwners.size() - 1) jsonResponse.append(",");
        }
        jsonResponse.append("]");
        return jsonResponse.toString();
    }

    private static void runReflectionWorkload(Class<?> clazz) {
        try {
            performReflectionOps(clazz);
        } catch (Exception e) {
            // ignore
        }
    }

    private static Map<String, List<Owner>> groupOwnersByCity(List<Owner> allOwners) {
        return allOwners.stream().collect(Collectors.groupingBy(o -> o.city));
    }

    private static Map<String, Long> computePetCountByOwner(List<Owner> allOwners) {
        return allOwners.stream()
                .collect(Collectors.toMap(
                        o -> o.firstName + " " + o.lastName,
                        o -> (long) o.pets.size()
                ));
    }

    private static double calculateAveragePetsPerOwner(List<Owner> allOwners) {
        return allOwners.stream()
                .mapToInt(o -> o.pets.size())
                .average()
                .orElse(0.0);
    }

    private static void runExceptionSimulation(List<Owner> allOwners) {
        try {
            if (allOwners.isEmpty()) {
                throw new RuntimeException("No owners found");
            }
        } catch (RuntimeException e) {
            // handled
        }
    }

    private static String buildComplexJson(List<Owner> allOwners,
                                           Map<String, List<Owner>> ownersByCity,
                                           Map<String, Long> petCountByOwner,
                                           double avgPetsPerOwner) {
        JsonDocument doc = new JsonDocument();
        doc.put("meta", buildMetaSection(allOwners, avgPetsPerOwner));
        doc.put("owners", buildOwnersSection(allOwners));
        doc.put("cities", buildCityStatisticsSection(ownersByCity));
        doc.put("petCounts", buildPetCountSection(petCountByOwner));
        doc.put("deepNested", buildDeepNestedSection(allOwners));
        return doc.serialize();
    }

    private static Map<String, Object> buildMetaSection(List<Owner> owners, double avgPetsPerOwner) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("ownerCount", owners.size());
        meta.put("avgPetsPerOwner", avgPetsPerOwner);
        meta.put("timestamp", System.nanoTime());
        meta.put("tags", Arrays.asList("benchmark", "startup", "json", "renaissance-inspired"));
        return meta;
    }

    private static List<Map<String, Object>> buildOwnersSection(List<Owner> owners) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Owner owner : owners) {
            list.add(buildOwnerMap(owner));
        }
        return list;
    }

    private static Map<String, Object> buildOwnerMap(Owner owner) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("firstName", owner.firstName);
        map.put("lastName", owner.lastName);
        map.put("address", owner.address);
        map.put("city", owner.city);
        map.put("telephone", owner.telephone);
        map.put("pets", buildPetsSection(owner));
        return map;
    }

    private static List<Map<String, Object>> buildPetsSection(Owner owner) {
        List<Map<String, Object>> petsList = new ArrayList<>();
        for (Pet pet : owner.pets) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("name", pet.name);
            p.put("birthDate", pet.birthDate);
            p.put("type", pet.type);
            p.put("visitCount", pet.visits.size());
            petsList.add(p);
        }
        return petsList;
    }

    private static Map<String, Object> buildCityStatisticsSection(Map<String, List<Owner>> ownersByCity) {
        Map<String, Object> cities = new LinkedHashMap<>();
        for (Map.Entry<String, List<Owner>> entry : ownersByCity.entrySet()) {
            Map<String, Object> cityInfo = new LinkedHashMap<>();
            cityInfo.put("ownerCount", entry.getValue().size());
            cityInfo.put("nameLength", entry.getKey().length());
            cityInfo.put("hash", entry.getKey().hashCode());
            cities.put(entry.getKey(), cityInfo);
        }
        return cities;
    }

    private static Map<String, Object> buildPetCountSection(Map<String, Long> petCountByOwner) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : petCountByOwner.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    private static List<Object> buildDeepNestedSection(List<Owner> owners) {
        List<Object> current = new ArrayList<>();
        current.add("root");
        current.add(owners.size());
        current.add(System.nanoTime());
        for (int depth = 0; depth < 3; depth++) {
            current = wrapInAnotherLevel(current, depth);
        }
        return current;
    }

    private static List<Object> wrapInAnotherLevel(List<Object> previous, int depth) {
        List<Object> level = new ArrayList<>();
        level.add("depth-" + depth);
        level.add(previous);
        level.add(Collections.singletonMap("size", previous.size()));
        return level;
    }

    private static void deepJsonProcessing(String complexJson) {
        JsonParser parser = new JsonParser(complexJson);
        Object root = parser.parse();
        if (!(root instanceof Map)) {
            throw new IllegalStateException("Expected root object");
        }
        Map<String, Object> rootMap = castToMap(root);

        JsonTreeStats stats = new JsonTreeStats();
        walkJsonTree(rootMap, 0, stats);

        List<Number> numbers = new ArrayList<>();
        collectNumericValues(rootMap, numbers);
        double numericAggregate = numbers.isEmpty() ? 0.0 : computeNumericAggregate(numbers);

        Validator<Map<String, Object>> validator = new DefaultJsonValidator();
        Transformer<Map<String, Object>> transformer = new DefaultJsonTransformer();
        Aggregator<Number, Double> aggregator = new DefaultJsonAggregator();

        if (validator.validate(rootMap)) {
            Map<String, Object> transformed = transformer.transform(rootMap);
            double aggregated = aggregator.aggregate(numbers);

            if (stats.maxDepth < 0 && numericAggregate == aggregated && transformed.size() == 123456) {
                System.out.println("Impossible JSON condition");
            }
        }
    }

    static class JsonTreeStats {
        int maxDepth;
        int objectCount;
        int arrayCount;
        int stringCount;
        int numberCount;
        int booleanCount;
        int nullCount;
    }

    private static Map<String, Object> castToMap(Object root) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) root;
        return map;
    }

    private static void walkJsonTree(Object node, int depth, JsonTreeStats stats) {
        stats.maxDepth = Math.max(stats.maxDepth, depth);
        if (node instanceof Map) {
            stats.objectCount++;
            for (Object value : ((Map<?, ?>) node).values()) {
                walkJsonTree(value, depth + 1, stats);
            }
        } else if (node instanceof List) {
            stats.arrayCount++;
            for (Object value : (List<?>) node) {
                walkJsonTree(value, depth + 1, stats);
            }
        } else if (node instanceof String) {
            stats.stringCount++;
        } else if (node instanceof Number) {
            stats.numberCount++;
        } else if (node instanceof Boolean) {
            stats.booleanCount++;
        } else if (node == null) {
            stats.nullCount++;
        }
    }

    private static void collectNumericValues(Object node, List<Number> out) {
        if (node instanceof Number) {
            out.add((Number) node);
        } else if (node instanceof Map) {
            for (Object value : ((Map<?, ?>) node).values()) {
                collectNumericValues(value, out);
            }
        } else if (node instanceof List) {
            for (Object value : (List<?>) node) {
                collectNumericValues(value, out);
            }
        }
    }

    private static double computeNumericAggregate(List<Number> numbers) {
        double sum = 0.0;
        double sumSq = 0.0;
        for (Number n : numbers) {
            double v = n.doubleValue();
            sum += v;
            sumSq += v * v;
        }
        if (numbers.isEmpty()) {
            return 0.0;
        }
        double mean = sum / numbers.size();
        double variance = (sumSq / numbers.size()) - mean * mean;
        return mean + variance;
    }

    private static void runGraphAndTreeWorkloads(List<Owner> allOwners, Map<String, Long> petCountByOwner) {
        List<GraphNode> graph = buildOwnerGraph(allOwners, petCountByOwner);
        int graphChecksum = traverseGraph(graph);

        TreeNode root = buildBalancedTree(petCountByOwner.values());
        int treeChecksum = computeTreeChecksum(root);

        if (graphChecksum == treeChecksum && graphChecksum == -1) {
            System.out.println("Unreachable equality");
        }
    }

    private static List<GraphNode> buildOwnerGraph(List<Owner> owners, Map<String, Long> petCountByOwner) {
        List<GraphNode> nodes = new ArrayList<>();
        Map<String, GraphNode> byName = new HashMap<>();
        int id = 0;
        for (Owner owner : owners) {
            String name = owner.firstName + " " + owner.lastName;
            GraphNode node = new GraphNode(id++);
            node.metadata.put("name", name);
            node.metadata.put("city", owner.city);
            node.metadata.put("pets", petCountByOwner.getOrDefault(name, 0L));
            nodes.add(node);
            byName.put(name, node);
        }
        for (int i = 0; i < nodes.size(); i++) {
            GraphNode current = nodes.get(i);
            GraphNode next = nodes.get((i + 1) % nodes.size());
            current.addNeighbor(next);
        }
        return nodes;
    }

    private static int traverseGraph(List<GraphNode> graph) {
        if (graph.isEmpty()) {
            return 0;
        }
        Set<GraphNode> visited = new HashSet<>();
        Deque<GraphNode> stack = new ArrayDeque<>();
        stack.push(graph.get(0));
        int checksum = 0;
        while (!stack.isEmpty()) {
            GraphNode node = stack.pop();
            if (!visited.add(node)) {
                continue;
            }
            checksum += node.id;
            Object pets = node.metadata.get("pets");
            if (pets instanceof Number) {
                checksum += ((Number) pets).intValue();
            }
            for (GraphNode neighbor : node.neighbors) {
                if (!visited.contains(neighbor)) {
                    stack.push(neighbor);
                }
            }
        }
        return checksum;
    }

    private static TreeNode buildBalancedTree(Collection<Long> values) {
        List<Integer> ints = new ArrayList<>();
        for (Long v : values) {
            ints.add(v.intValue());
        }
        Collections.sort(ints);
        return buildBalancedTreeFromList(ints, 0, ints.size());
    }

    private static TreeNode buildBalancedTreeFromList(List<Integer> values, int start, int end) {
        if (start >= end) {
            return null;
        }
        int mid = (start + end) / 2;
        TreeNode node = new TreeNode(values.get(mid));
        node.left = buildBalancedTreeFromList(values, start, mid);
        node.right = buildBalancedTreeFromList(values, mid + 1, end);
        node.height = 1 + Math.max(height(node.left), height(node.right));
        return node;
    }

    private static int height(TreeNode node) {
        return node == null ? 0 : node.height;
    }

    private static int computeTreeChecksum(TreeNode node) {
        if (node == null) {
            return 0;
        }
        return node.value + computeTreeChecksum(node.left) + computeTreeChecksum(node.right);
    }

    static class DefaultJsonValidator implements Validator<Map<String, Object>> {
        @Override
        public boolean validate(Map<String, Object> data) {
            return hasKey(data, "meta") && hasKey(data, "owners") && hasKey(data, "cities");
        }

        private boolean hasKey(Map<String, Object> data, String key) {
            return data.containsKey(key) && data.get(key) != null;
        }
    }

    static class DefaultJsonTransformer implements Transformer<Map<String, Object>> {
        @Override
        public Map<String, Object> transform(Map<String, Object> data) {
            Map<String, Object> copy = new LinkedHashMap<>(data);
            Object meta = copy.get("meta");
            if (meta instanceof Map) {
                Map<String, Object> metaMap = new LinkedHashMap<>((Map<String, Object>) meta);
                metaMap.put("transformed", Boolean.TRUE);
                copy.put("meta", metaMap);
            }
            return copy;
        }
    }

    static class DefaultJsonAggregator implements Aggregator<Number, Double> {
        @Override
        public Double aggregate(Collection<Number> items) {
            double sum = 0.0;
            for (Number n : items) {
                sum += n.doubleValue();
            }
            return sum;
        }
    }

    // ============================================
    // Apache Commons Math Workloads
    // ============================================

    private static void runCommonsMathWorkloads(int globalIteration,
                                                List<Owner> allOwners,
                                                Map<String, List<Owner>> ownersByCity,
                                                Map<String, Long> petCountByOwner,
                                                double avgPetsPerOwner) {
        // Phase 0: always-on lightweight stats
        DescriptiveStatistics stats = buildBasicStatistics(allOwners, petCountByOwner, avgPetsPerOwner);

        // IMPORTANT: run the same numerical workload in both warmup and peak phases.
        // We still vary sizes/values based on globalIteration for diversity, but
        // all phases are active from iteration 0 so that differences between
        // warmup and peak are due to JIT/GC, not different code paths.
        runMatrixPhaseOne(globalIteration, stats);
        runMatrixPhaseTwo(globalIteration, ownersByCity.size());
        runMatrixPhaseThree(globalIteration, petCountByOwner.size());
    }

    private static DescriptiveStatistics buildBasicStatistics(List<Owner> owners,
                                                              Map<String, Long> petCountByOwner,
                                                              double avgPetsPerOwner) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        stats.addValue(avgPetsPerOwner);
        for (Owner owner : owners) {
            String name = owner.firstName + " " + owner.lastName;
            long petCount = petCountByOwner.getOrDefault(name, 0L);
            stats.addValue(petCount);
            stats.addValue(owner.city.length());
            stats.addValue(owner.telephone.length());
        }
        // Use some stats to keep them live
        double mean = stats.getMean();
        double std = stats.getStandardDeviation();
        if (mean < 0 && std < 0) {
            System.out.println("Impossible stats");
        }
        return stats;
    }

    private static void runMatrixPhaseOne(int globalIteration, DescriptiveStatistics stats) {
        int dimension = 8 + (globalIteration % 8); // 8..15
        RealMatrix a = buildRandomMatrix(dimension, dimension, globalIteration);
        RealMatrix b = buildRandomMatrix(dimension, dimension, globalIteration * 31L + 7);
        RealMatrix c = a.multiply(b);
        RealMatrix d = c.add(a.scalarMultiply(stats.getMean() + 1.0));
        double trace = d.getTrace();
        if (trace == Double.MIN_VALUE) {
            System.out.println("Unreachable trace");
        }
    }

    private static void runMatrixPhaseTwo(int globalIteration, int cityCount) {
        int dimension = 10 + (cityCount % 10); // depends on data
        RealMatrix m = buildRandomSymmetricPositiveDefiniteMatrix(dimension, globalIteration * 13L + 17);
        new LUDecomposition(m).getDeterminant();
        new QRDecomposition(m).getR();
    }

    private static void runMatrixPhaseThree(int globalIteration, int ownerCount) {
        int size = Math.max(5, Math.min(25, ownerCount));
        RealMatrix m = buildRandomMatrix(size, size, globalIteration * 101L + 3);
        RealVector v = buildRandomVector(size, globalIteration * 53L + 11);
        DecompositionSolver solver = new QRDecomposition(m.transpose().multiply(m).add(
                MatrixUtils.createRealIdentityMatrix(size).scalarMultiply(0.01))).getSolver();
        RealVector solution = solver.solve(v);
        double norm = solution.getNorm();
        if (norm < 0) {
            System.out.println("Unreachable norm");
        }
    }

    private static RealMatrix buildRandomMatrix(int rows, int cols, long seed) {
        MersenneTwister rng = new MersenneTwister(seed);
        double[][] data = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] = rng.nextDouble();
            }
        }
        return MatrixUtils.createRealMatrix(data);
    }

    private static RealMatrix buildRandomSymmetricPositiveDefiniteMatrix(int dimension, long seed) {
        RealMatrix m = buildRandomMatrix(dimension, dimension, seed);
        RealMatrix mt = m.transpose();
        RealMatrix sym = mt.multiply(m); // A^T A is SPD
        RealMatrix identity = MatrixUtils.createRealIdentityMatrix(dimension).scalarMultiply(0.1);
        return sym.add(identity);
    }

    private static RealVector buildRandomVector(int size, long seed) {
        MersenneTwister rng = new MersenneTwister(seed);
        double[] data = new double[size];
        for (int i = 0; i < size; i++) {
            data[i] = rng.nextGaussian();
        }
        return new ArrayRealVector(data, false);
    }

    private static Owner createRandomOwner(int seed) {
        Random random = new Random(seed);
        String[] firstNames = {"George", "Betty", "Eduardo", "Harold", "Peter", "Jean", "Jeff", "Maria"};
        String[] lastNames = {"Franklin", "Davis", "Rodriquez", "Davis", "McTavish", "Coleman", "Black", "Escobito"};
        String[] cities = {"Madison", "Sun Prairie", "Monona", "Windsor", "McFarland"};
        String[] petTypes = {"cat", "dog", "lizard", "snake", "bird", "hamster"};
        
        Owner owner = new Owner(
            firstNames[random.nextInt(firstNames.length)],
            lastNames[random.nextInt(lastNames.length)],
            random.nextInt(9999) + " " + (random.nextBoolean() ? "Oak" : "Main") + " Street",
            cities[random.nextInt(cities.length)],
            String.format("%03d%03d%04d", random.nextInt(1000), random.nextInt(1000), random.nextInt(10000))
        );
        
        // Add pets
        int numPets = random.nextInt(3) + 1;
        for (int i = 0; i < numPets; i++) {
            Pet pet = new Pet(
                "Pet" + i,
                "2020-0" + (random.nextInt(9) + 1) + "-" + (random.nextInt(28) + 1),
                petTypes[random.nextInt(petTypes.length)]
            );
            
            // Add visits
            int numVisits = random.nextInt(5);
            for (int j = 0; j < numVisits; j++) {
                pet.visits.add(new Visit(
                    "2023-" + (random.nextInt(12) + 1) + "-" + (random.nextInt(28) + 1),
                    "Checkup " + j
                ));
            }
            
            owner.pets.add(pet);
        }
        
        return owner;
    }
    
    private static void validateAndProcessJSON(String json) {
        // Regex patterns (common in web validation)
        Pattern phonePattern = Pattern.compile("\\d{10}");
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        
        Matcher phoneMatcher = phonePattern.matcher(json);
        int phoneCount = 0;
        while (phoneMatcher.find()) {
            phoneCount++;
        }
        
        // String operations
        String processed = json.replace("\"", "'")
                              .toLowerCase()
                              .substring(0, Math.min(json.length(), 100));
    }
    
    private static void performReflectionOps(Class<?> clazz) throws Exception {
        // Simulate dependency injection / reflection-based frameworks
        Method[] methods = clazz.getDeclaredMethods();
        
        for (Method method : methods) {
            String name = method.getName();
            Class<?>[] params = method.getParameterTypes();
            
            // Simulate annotation processing
            if (name.startsWith("get") || name.startsWith("set")) {
                // getter/setter processing
            }
        }
        
        // Instance creation via reflection
        if (clazz.equals(Owner.class)) {
            // Can't easily construct without parameters, so just inspect
            var fields = clazz.getDeclaredFields();
            for (var field : fields) {
                field.getName(); // Access field metadata
            }
        }
    }
    
    private static double calculateAverage(List<LatencyMeasurement> measurements) {
        return measurements.stream()
            .mapToLong(m -> m.latency)
            .average()
            .orElse(0.0);
    }
    
    private static double calculatePercentile(List<LatencyMeasurement> measurements, double percentile) {
        List<Long> latencies = new ArrayList<>();
        for (LatencyMeasurement m : measurements) {
            latencies.add(m.latency);
        }
        latencies.sort(Long::compareTo);
        
        int index = (int) Math.ceil(percentile / 100.0 * latencies.size()) - 1;
        return latencies.get(Math.max(0, index));
    }
    
    static class LatencyMeasurement {
        long timestamp;
        int iteration;
        long latency;
        String phase;
        
        LatencyMeasurement(long timestamp, int iteration, long latency, String phase) {
            this.timestamp = timestamp;
            this.iteration = iteration;
            this.latency = latency;
            this.phase = phase;
        }
    }
}
