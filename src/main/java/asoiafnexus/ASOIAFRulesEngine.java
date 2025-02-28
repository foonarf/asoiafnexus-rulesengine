package asoiafnexus;

import org.jeasy.rules.api.*;
import org.jeasy.rules.core.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

@JsonIgnoreProperties(ignoreUnknown = true)
class Unit {
    private String id;
    private String name;
    private Statistics statistics;
    private String faction;

    public Unit() {}

    public Unit(String id, String name, Statistics statistics, String faction) {
        this.id = id;
        this.name = name;
        this.statistics = statistics;
        this.faction = faction;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public String getFaction() {
        return faction;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Statistics {
        private String version;
        private String faction;
        private String type;
        private int cost;
        private int speed;
        private int defense;
        private int morale;
        private List<Attack> attacks;
        private List<String> abilities;
        private String tray;
        private int wounds;

        public Statistics() {}

        public Statistics(String version, String faction, String type, int cost, int speed, int defense, int morale, List<Attack> attacks, List<String> abilities, String tray) {
            this.version = version;
            this.faction = faction;
            this.type = type;
            this.cost = cost;
            this.speed = speed;
            this.defense = defense;
            this.morale = morale;
            this.attacks = attacks;
            this.abilities = abilities;
            this.tray = tray;
        }

        public int getCost() {
            return cost;
        }

        public String getVersion() {
            return version;
        }

        public String getFaction() {
            return faction;
        }

        public String getType() {
            return type;
        }

        public int getSpeed() {
            return speed;
        }

        public int getDefense() {
            return defense;
        }

        public int getMorale() {
            return morale;
        }

        public List<Attack> getAttacks() {
            return attacks;
        }

        public List<String> getAbilities() {
            return abilities;
        }

        public String getTray() {
            return tray;
        }

        public int getWounds() {
            return wounds;
        }
    }

    static class Attack {
        private String name;
        private String type;
        private int hit;
        private List<Integer> dice;

        public Attack() {}

        public Attack(String name, String type, int hit, List<Integer> dice) {
            this.name = name;
            this.type = type;
            this.hit = hit;
            this.dice = dice;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public int getHit() {
            return hit;
        }

        public List<Integer> getDice() {
            return dice;
        }
    }
}

public class ASOIAFRulesEngine {
    public static void main(String[] args) {
        // Create a rules engine
        RulesEngine rulesEngine = new DefaultRulesEngine();

        // Load units from all faction files
        ObjectMapper objectMapper = new ObjectMapper();
        List<Unit> allUnits = new ArrayList<>();
        String[] factions = {"baratheon", "bolton", "brotherhood", "freefolk", "greyjoy", "lannister", "martell", "neutral", "nightswatch", "stark", "targaryen"};
        for (String faction : factions) {
            try {
                JsonNode rootNode = objectMapper.readTree(new File("assets/" + faction + ".json"));
                JsonNode unitsNode = rootNode.get("units");
                if (unitsNode.isArray()) {
                    for (JsonNode unitNode : unitsNode) {
                        Unit unit = objectMapper.readValue(unitNode.toString(), Unit.class);
                        unit = new Unit(unit.getId(), unit.getName(), unit.getStatistics(), faction);
                        allUnits.add(unit);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Select a random faction
        Random random = new Random();
        String randomFaction = factions[random.nextInt(factions.length)];

        // Create a list of 5 random units from the selected faction
        List<Unit> randomUnits = new ArrayList<>();
        List<Unit> factionUnits = new ArrayList<>();
        for (Unit unit : allUnits) {
            if (unit.getFaction().equals(randomFaction)) {
                factionUnits.add(unit);
            }
        }

        if (!factionUnits.isEmpty()) {
            for (int i = 0; i < Math.min(5, factionUnits.size()); i++) {
                int randomIndex = random.nextInt(factionUnits.size());
                if (i == 4 && random.nextDouble() < 0.5) {
                    // Add a unit from allUnits
                    randomUnits.add(allUnits.get(random.nextInt(allUnits.size())));
                } else {
                    randomUnits.add(factionUnits.get(randomIndex));
                    factionUnits.remove(randomIndex); // Prevent duplicates
                }
                
            }
        }

        // Convert List<Unit> to Unit[]
        Unit[] unitsArray = randomUnits.toArray(new Unit[0]);

        // Print the name and cost of each unit
        System.out.println("Units in the list (Faction: " + randomFaction + "):");
        for (Unit unit : unitsArray) {
            System.out.println("Name: " + unit.getName() + ", Cost: " + unit.getStatistics().getCost() + ", Faction: " + unit.getFaction());
        }

        // Load rules from JSON file
        File jsonFile = new File("rules.json");

        // Define facts
        Facts facts = new Facts();

        facts.put("unit_list", unitsArray);

        try {
            List<Map<String, Object>> ruleList = objectMapper.readValue(jsonFile, List.class);

            // Create rules
            Rules rules = new Rules();

            // Iterate through the list of rules
            for (Map<String, Object> ruleMap : ruleList) {
                String name = (String) ruleMap.get("name");
                String description = (String) ruleMap.get("description");
                int priority = (int) ruleMap.get("priority");
                String condition = (String) ruleMap.get("condition");
                List<String> onTrueActions = (List<String>) ruleMap.get("on_true");
                List<String> onFalseActions = (List<String>) ruleMap.get("on_false");
                List<Map<String, Object>> inputs = (List<Map<String, Object>>) ruleMap.get("inputs");

                // Prompt user for inputs
                Scanner scanner = new Scanner(System.in);
                if (inputs != null) {
                    for (Map<String, Object> input : inputs) {
                        String inputName = (String) input.get("name");
                        String inputDescription = (String) input.get("description");
                        String inputType = (String) input.get("type");

                        System.out.println("Enter " + inputDescription + ":");
                        if (inputType.equals("integer")) {
                            int value = scanner.nextInt();
                            facts.put(inputName, value);
                        } else {
                            String value = scanner.nextLine();
                            facts.put(inputName, value);
                        }
                    }
                }
                scanner.close();

                // Create rule
                Rule exampleRule = new RuleBuilder()
                    .name(name)
                    .description(description)
                    .priority(priority)
                    .when(f -> {
                        try (Context context = Context.create("js")) {
                            Value bindings = context.getBindings("js");
                            if (inputs != null) {
                                for (Map<String, Object> input : inputs) {
                                    String inputName = (String) input.get("name");
                                    bindings.putMember(inputName, f.get(inputName));
                                }
                            }
                            bindings.putMember("unit_list", context.eval("js", objectMapper.writeValueAsString(f.get("unit_list"))));
                            boolean result = context.eval("js", condition).asBoolean();
                            if (result) {
                                for (String action : onTrueActions) {
                                    if (action.startsWith("log")) {
                                        String message = action.substring(action.indexOf('(') + 1, action.lastIndexOf(')'));
                                        logAction(message);
                                    }
                                }
                            } else {
                                for (String action : onFalseActions) {
                                    if (action.startsWith("log")) {
                                        String message = action.substring(action.indexOf('(') + 1, action.lastIndexOf(')'));
                                        logAction(message);
                                    }
                                }
                            }
                            return result;
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                            return false;
                        }
                    })
                    .build();

                // Register the rule
                rules.register(exampleRule);
            }

            // Fire rules
            rulesEngine.fire(rules, facts);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void logAction(String message) {
        System.out.println(message.replace("\"", ""));
    }
}

