package depsolver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


class Package {
    private String name;
    private String version;
    private Integer size;
    private List<List<String>> depends = new ArrayList<>();
    private List<String> conflictsArray = new ArrayList<>();

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Integer getSize() {
        return size;
    }

    public List<List<String>> getDepends() {
        return depends;
    }

    public List<String> getConflicts() {
        return conflictsArray;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public void setDepends(List<List<String>> depends) {
        this.depends = depends;
    }

    public void setConflicts(List<String> conflictsArray) {
        this.conflictsArray = conflictsArray;
    }

    public String getPackage() {
        String returnPackage = "Name " + getName() + "=" + getVersion() + "Size " + getSize() + "Dep : " +
                getDepends().toString() + "conflictsArray: " + getConflicts();
        return returnPackage;
    }
}

public class Main {

    public static String nameCons;
    public static boolean testConflict;
    public static HashSet<String> dontAddHash;
    public static int advance;
    public static int checks;
    public ArrayList<String> stateCommands;
    public ArrayList<String> conflictsArray;

    public static void main(String[] args) throws IOException {

        TypeReference<List<Package>> repoType = new TypeReference<List<Package>>() {
        };
        List<Package> repo = JSON.parseObject(readFile(args[0]), repoType);
        TypeReference<List<String>> strListType = new TypeReference<List<String>>() {
        };
        List<String> initial = JSON.parseObject(readFile(args[1]), strListType);
        List<String> constraints = JSON.parseObject(readFile(args[2]), strListType);
        List<String> commands = new ArrayList<String>();

        //    1. a repository description, holding a package list with dependencies and conflicts;
//    2. a valid state T of that repository;
//    3. a set of constraints, each in the form of a package reference:
//            a positive constraint requires that at least one of the referred packages is installed
//            a negative constraint requires that all the referred packages are not installed.

//    Your task is twofold:

//    1. find a valid state T that satisfies the given constraints (as well as the repository'T constraints), and
//    2. construct a list of commands that transforms the given state T into your target state T, such that all intermediate
//    states are valid, and you minimize the cost of the transformation.

//      For this assignment, the "vertices" x and y are each a set of packages. There is an "arc" from x to y if x and y
//      are the same set with the exception of one package. A set of packages is VALID if it satisfies all the constraints
//      from repository.json. A set of packages is FINAL if it satisfies all the constraints from constraints.json.

        // CHANGE CODE BELOW:
        // using repo, initial and constraints, compute a solution and print the answer

        //Commands list
        for (String T : constraints) {

            String operator = "" + T.charAt(0);
            String tempCons = T.substring(1);
            ArrayList<String> consArray = splitString(tempCons);
            String nameCons = consArray.get(0);
            String versionCons = consArray.get(1);

            ArrayList<String> initialCheckArray = initialCheck(repo, consArray, initial, constraints);
            ArrayList<String> conflictsArray = new ArrayList<String>();
            ArrayList<String> stateArray = new ArrayList<String>();
            HashSet<String> dontAddHash = new HashSet<String>();

            if (operator.equals("+")) {

                testConflict = false;
                commands.addAll(depBuildArray(repo, consArray, stateArray, dontAddHash, true));

            } else if (operator.equals("-")) {

                commands.add(T);

            }

        }

        if (initial.size() > 0) {

            for (String x : constraints) {

                String operator = "" + x.charAt(0);
                String tempCons = x.substring(1);

                ArrayList<String> consArray = splitString(tempCons);
                String nameCons = consArray.get(0);
                String versionCons = consArray.get(1);

                ArrayList<String> initialCheckArray = initialCheck(repo, consArray, initial, constraints);
                commands.addAll(initialCheckArray);

            }

        }

        Collections.reverse(commands);
        System.out.println(JSON.toJSONString(commands));

    }

    public static ArrayList<String> initialCheck(List<Package> repo, ArrayList<String> consArray, List<String> initial, List<String> constraints) {

        if (initial.size() == 0) {

            ArrayList<String> emptyStateArray = new ArrayList<String>();

            return emptyStateArray;

        }

        HashSet<String> emptyHash = new HashSet<String>();
        ArrayList<String> emptyStateArray = new ArrayList<String>();
        ArrayList<String> newCommandArray = new ArrayList<String>();
        ArrayList<String> commandsChecker = depBuildArray(repo, consArray, emptyStateArray, emptyHash, true);

        for (String initialS : initial) {


            ArrayList<String> addInitialArray = new ArrayList<String>();

            addInitialArray.add(initialS);

            if (validState(commandsChecker, addInitialArray, repo, dontAddHash)) {

                if (initialS.charAt(0) == '-') {
                    if (constraints.contains(initialS)) {

                        String restOf = initialS.substring(1);
                        String newS = "+" + initialS;
                        newCommandArray.add(newS);

                    } else {

                        newCommandArray.add(initialS);

                    }

                } else {

                    String addNewString = "-" + initialS;
                    newCommandArray.add(addNewString);

                }

            }


        }

        return newCommandArray;

    }

    public static boolean compareVersion(String versX, String versY, String operator) {

        if (operator.equals("=")) {

            return versX.equals(versY);

        } else if (operator.equals("<")) {

            if (versX.equals(versY)) {

                return true;

            } else {

                return versX.compareTo(versY) < 0;

            }

        } else if (operator.equals("<=")) {


            return versX.compareTo(versY) < 0;


        } else if (operator.equals(">")) {

            if (versX.equals(versY)) {
                return true;
            } else {

                return versX.compareTo(versY) > 0;

            }
        } else if (operator.equals(">=")) {

            return versX.compareTo(versY) > 0;


        } else return operator.equals("anyOp");


    }

    public static int getChecks() {

        return checks;

    }

    public static void setChecks(int checkVal) {

        checks = checkVal;

    }


    //Build the depedencies

    public static ArrayList<String> depBuildArray(List<Package> repo, List<String> item, ArrayList<String> states, HashSet<String> dontAddHash, boolean cont) {

        String nameCons;
        String versionCons;
        String operator;

        ArrayList<String> originalStateArray = new ArrayList<String>(states);
        ArrayList<String> emptyArray = new ArrayList<String>();

        if (cont == false) {

        }

        ArrayList<String> packageArray = new ArrayList<String>();

        if (item.contains("=")) {

            nameCons = item.get(0);
            versionCons = item.get(1);
            operator = item.get(2);

        } else {

            ArrayList<String> consArray = splitString(item.toString());

            nameCons = consArray.get(0);
            versionCons = consArray.get(1);

            if (versionCons.equals("anyOp")) {

                operator = "anyOp";
                String[] c = nameCons.split(",");
                nameCons = c[0];

            } else {

                operator = consArray.get(2);

            }

        }

        //Starting the process
        for (Package p : repo) {

            if ((p.getName().equals(nameCons) && compareVersion(p.getVersion(), versionCons, operator))) {

                if (dontAddHash.contains("+" + p.getName() + "=" + p.getVersion())) {

                }

                ArrayList<String> stateTestArray = new ArrayList<String>(originalStateArray);
                stateTestArray.add("+" + p.getName() + "=" + p.getVersion());
                ArrayList<String> addTestArray = new ArrayList<String>();

                addTestArray.add("+" + p.getName() + "=" + p.getVersion());

                Set<String> testDupeArray = new HashSet<String>(stateTestArray);

                if (testDupeArray.size() < stateTestArray.size()) {

                    testConflict = true;

                    return emptyArray;

                }


                if (validState(stateTestArray, addTestArray, repo, dontAddHash)) {

                    packageArray.add("+" + p.getName() + "=" + p.getVersion());
                    states.add("+" + p.getName() + "=" + p.getVersion());
                    dontAddHash.add("+" + p.getName() + "=" + p.getVersion());

                    // Size = 0 then return current state within stateArray
                    if (p.getDepends().size() == 0) {

                        cont = false;
                        testConflict = false;

                        return states;

                    }

                    if (p.getDepends().size() >= 1) {


                        for (List<String> dependancyList : p.getDepends()) {

                            if (dependancyList.size() == 1) {

                                ArrayList<String> addedArray = new ArrayList<String>(depBuildArray(repo, dependancyList, states, dontAddHash, true));
                                ArrayList<String> stateTemp = new ArrayList<String>(states);

                                stateTemp.addAll(addedArray);

                                if ((validState(stateTestArray, addedArray, repo, dontAddHash))) {

                                    if (testConflict = false) {

                                        packageArray.addAll(addedArray);
                                        states.addAll(addedArray);

                                    }

                                } else {

                                    dontAddHash.addAll(addedArray);
                                    testConflict = true;

                                }


                            }

                        }


                        for (List<String> depends : p.getDepends()) {


                            int advance = 1;

                           // boolean contin = true;

                            if (depends.size() > 1) {

                                int looper = 0;

                                while (looper < depends.size() && advance == 1) {

                                    String z = depends.get(looper);

                                    //Compares conflictsArray and adds them if valid.
                                    ArrayList<String> x = new ArrayList<String>();

                                    x.add(z);

                                    ArrayList<String> addedArray = new ArrayList<String>(depBuildArray(repo, x, states, dontAddHash, true));
                                    ArrayList<String> stateFTArray = new ArrayList<String>(states);

                                    stateFTArray.addAll(addedArray);

                                    if ((validState(stateTestArray, addedArray, repo, dontAddHash))) {

                                        if (dontAddHash.contains(addedArray)) {

                                        } else if (testConflict = false) {

                                            packageArray.addAll(addedArray);
                                            states.addAll(packageArray);
                                            advance = 2;

                                        }

                                    } else {

                                        dontAddHash.addAll(addedArray);
                                        testConflict = true;

                                        String adding = addedArray.toString().replace("[", "");
                                        adding = adding.replace("]", "");
                                        states.remove(adding);


                                    }

                                    looper++;

                                }

                            }

                        }

                    }

                }

            }

        }

        return states;

    }

    public static ArrayList<String> conflictBuilder(List<Package> repo, ArrayList<String> stateArray) {

        ArrayList<String> tempConArray = new ArrayList<String>();

        for (String T : stateArray) {

            ArrayList<String> stateAsArray = splitString(T);

            String name = stateAsArray.get(0);
            String version = stateAsArray.get(1);
            String symbol;

            name = name.replace("+", "");

            for (Package pack : repo) {

                if ((pack.getName().equals(name) && pack.getVersion().equals(version)) || (pack.getName().equals(name) && compareVersion(pack.getVersion(), version, "="))) {

                    tempConArray.addAll(pack.getConflicts());

                }

            }

        }

        return tempConArray;

    }


//Remove Conflicts

    public static boolean validState(ArrayList<String> stateArray, ArrayList<String> addedArray, List<Package> repo, HashSet<String> dontAddHash) {

        Set<String> testDupeArray = new HashSet<String>(stateArray);

        if (testDupeArray.size() < stateArray.size()) {

            return false;

        }

        ArrayList<String> conflictsArray = conflictBuilder(repo, stateArray);

        ArrayList<Package> addedAlreadyArray = new ArrayList<Package>();
        ArrayList<Package> statePackArray = new ArrayList<Package>();

        for (String T : stateArray) {

            ArrayList<String> stateAsArray = splitString(T);

            String nameCons = stateAsArray.get(0);
            String versionCons = stateAsArray.get(1);
            String operator;


            if (versionCons.equals("anyOp")) {

                operator = "anyOp";
                String[] c = nameCons.split("");
                nameCons = c[1];

            } else {

                operator = stateAsArray.get(2);

            }

            nameCons = nameCons.replace("+", "");

            for (Package p : repo) {

                if ((p.getName().equals(nameCons) && versionCons.equals("anyOp")) || (p.getName().equals(nameCons) && compareVersion(p.getVersion(), versionCons, operator))) {

                    statePackArray.add(p);

                }

            }

        }

        for (Package pack : statePackArray) {

            for (String confl : conflictsArray) {

                ArrayList<String> stateAsArray = splitString(confl);
                String nameCons = stateAsArray.get(0);
                String versionCons = stateAsArray.get(1);
                String operator;

                if (versionCons.equals("anyOp")) {

                    operator = "anyOp";

                } else {

                    operator = stateAsArray.get(2);

                }

                if (pack.getName().equals(nameCons) && compareVersion(pack.getVersion(), versionCons, operator)) {

                    return false;

                }

            }

            addedAlreadyArray.add(pack);

            if (pack.getDepends().size() == 1) {

                if (pack.getDepends().get(0).size() == 1) {

                    String dependant = pack.getDepends().get(0).toString();
                    ArrayList<String> stateAsArray = splitString(dependant);
                    String nameCons = stateAsArray.get(0);
                    String versionCons = stateAsArray.get(1);
                    String operator;

                    if (versionCons.equals("anyOp")) {

                        operator = "anyOp";

                    } else {

                        operator = stateAsArray.get(2);

                    }

                    for (Package pa : addedAlreadyArray) {

                        if (pa.getName().equals(nameCons) && compareVersion(pa.getVersion(), versionCons, operator)) {

                            return false;

                        }


                    }


                }


            }

        }

        return true;

    }


    static String readFile(String filename) throws IOException {

        BufferedReader bf = new BufferedReader(new FileReader(filename));
        StringBuilder sb = new StringBuilder();
        bf.lines().forEach(line -> sb.append(line));

        return sb.toString();

    }

    // The string is then converted to name and operator needed.
    public static ArrayList<String> splitString(String input) {

        ArrayList returnArray = new ArrayList();

        if (input.contains("<=")) {

            input = input.replace("[", "");
            input = input.replace("]", "");

            String[] spli_t = input.split("<=");
            returnArray.add(spli_t[0]);
            returnArray.add(spli_t[1]);
            returnArray.add("<=");

            return returnArray;

        } else if (input.contains("<")) {

            input = input.replace("[", "");
            input = input.replace("]", "");

            String[] spli_t = input.split("<");
            returnArray.add(spli_t[0]);
            returnArray.add(spli_t[1]);
            returnArray.add("<");

            return returnArray;

        } else if (input.contains(">=")) {

            input = input.replace("[", "");
            input = input.replace("]", "");

            String[] spli_t = input.split(">=");

            returnArray.add(spli_t[0]);
            returnArray.add(spli_t[1]);
            returnArray.add(">=");

            return returnArray;

        } else if (input.contains(">")) {

            input = input.replace("[", "");
            input = input.replace("]", "");

            String[] spli_t = input.split(">");
            returnArray.add(spli_t[0]);
            returnArray.add(spli_t[1]);
            returnArray.add(">");

            return returnArray;

        } else if (input.contains("=")) {

            input = input.replace("[", "");
            input = input.replace("]", "");

            String[] spli_t = input.split("=");
            returnArray.add(spli_t[0]);
            returnArray.add(spli_t[1]);
            returnArray.add("=");

            return returnArray;

        } else {

            input = input.replace("[", "");
            input = input.replace("]", "");

            returnArray.add(input);
            returnArray.add("anyOp");

            return returnArray;

        }

    }

}

