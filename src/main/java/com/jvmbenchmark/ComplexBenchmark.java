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

// ============================================
// Complex benchmark simulating web application workload (similar to Spring PetClinic)
// - JSON-like serialization/deserialization
// - Reflection and dynamic invocation
// - Collections and stream operations
// - String manipulation and regex
// - Object creation and garbage collection
// ============================================

public class ComplexBenchmark {
    private static final int WARMUP_ITERATIONS = 20000;
    private static final int BENCHMARK_ITERATIONS = 100000;
    private static final int MEASUREMENT_POINTS = 200;
    
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
                performComplexWork();
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
                performComplexWork();
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
    private static void performComplexWork() {
        OwnerRepository repository = new OwnerRepository();
        
        // 1. Create and populate data (simulates incoming requests)
        for (int i = 0; i < 5; i++) {
            Owner owner = createRandomOwner(i);
            repository.save(owner);
        }
        
        // 2. Perform database-like queries
        List<Owner> allOwners = repository.findAll();
        List<Owner> filtered = repository.findByLastName("Smith");
        
        // 3. JSON serialization (simulates REST API response)
        StringBuilder jsonResponse = new StringBuilder("[");
        for (int i = 0; i < allOwners.size(); i++) {
            jsonResponse.append(allOwners.get(i).toJSON());
            if (i < allOwners.size() - 1) jsonResponse.append(",");
        }
        jsonResponse.append("]");
        
        // 4. String manipulation and regex validation (like form validation)
        String json = jsonResponse.toString();
        validateAndProcessJSON(json);
        
        // 5. Reflection operations (like Spring's dependency injection)
        try {
            performReflectionOps(Owner.class);
        } catch (Exception e) {
            // ignore
        }
        
        // 6. Stream operations and data transformation
        Map<String, List<Owner>> ownersByCity = allOwners.stream()
            .collect(Collectors.groupingBy(o -> o.city));
        
        Map<String, Long> petCountByOwner = allOwners.stream()
            .collect(Collectors.toMap(
                o -> o.firstName + " " + o.lastName,
                o -> (long) o.pets.size()
            ));
        
        // 7. Complex calculations
        double avgPetsPerOwner = allOwners.stream()
            .mapToInt(o -> o.pets.size())
            .average()
            .orElse(0.0);
        
        // 8. Exception handling (common in web apps)
        try {
            if (allOwners.isEmpty()) {
                throw new RuntimeException("No owners found");
            }
        } catch (RuntimeException e) {
            // handled
        }
        
        // Prevent dead code elimination
        if (json.length() < 0 && avgPetsPerOwner < 0) {
            System.out.println("Never happens");
        }
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
