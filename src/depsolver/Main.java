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
	private List<List<String>> dependsArray = new ArrayList<>();
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
		return dependsArray;
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

	public void setDepends(List<List<String>> dependsArray) {
		this.dependsArray = dependsArray;
	}

	public void setConflicts(List<String> conflictsArray) {
		this.conflictsArray = conflictsArray;
	}

	public String getPackage() {
		String returnPackage = "Name " + getName() + "=" + getVersion() + "Size " + getSize() + "Dep : "
				+ getDepends().toString() + "conflictsArray: " + getConflicts();
		return returnPackage;
	}
}

public class Main {
	private static boolean satisfies = false;
	private static List<String> commandsArray = new ArrayList<>();
	private static HashSet<List<String>> visitedSetHash = new HashSet<>();
	private static HashMap<String, Integer> solutionHash = new HashMap<>();
	private static HashMap<String, Integer> maxConflictHash = new HashMap<>();

	public static void main(String[] args) throws IOException {

		TypeReference<List<Package>> repoType = new TypeReference<List<Package>>() {
		};
		List<Package> repo = JSON.parseObject(readFile(args[0]), repoType);
		TypeReference<List<String>> strListType = new TypeReference<List<String>>() {
		};
		List<String> initial = JSON.parseObject(readFile(args[1]), strListType);
		List<String> constraints = JSON.parseObject(readFile(args[2]), strListType);

		// 1. a repository description, holding a package list with dependencies and
		// conflictsArray;
		// 2. a valid state T of that repository;
		// 3. a set of constraints, each in the form of a package reference:
		// a positive constraint requires that at least one of the referred packages is
		// installed
		// a negative constraint requires that all the referred packages are not
		// installed.

		// My task is twofold:

		// 1. find a valid state T that satisfies the given constraints (as well as the
		// repository'T constraints), and
		// 2. construct a list of commandsArray that transforms the given state T into
		// your
		// target state T, such that all intermediate
		// states are valid, and you minimise the cost of the transformation.

		// For this assignment, the "vertices" x and y are each a set of packages. There
		// is an "arc" from x to y if x and y
		// are the same set with the exception of one package. A set of packages is
		// VALID if set_Iterator satisfies all the constraints
		// from repository.json. A set of packages is FINAL if set_Iterator satisfies
		// all the
		// constraints from constraints.json.

		// CHANGE CODE BELOW:

		while (packetValid(initial, repo) == false) {
			initial = throwBadPackages(initial, repo);
		}

		find(initial, repo, initial, constraints);
		getFinalSolution();

	}

	private static void find(List<String> set, List<Package> repo, List<String> initial, List<String> constraints) {

		int costOfSolution;
		

		if (packetValid(set, repo) == false) {
			return;
		}

		if (visitedSetHash.contains(set)) {
			return;
		}

		if (lastState(set, constraints)) {
			satisfies = true;

			// Then solutionHash Found
			costOfSolution = costOfSolution(repo);
			
			String cTemp;
			// loop through commandsArray adding T
			for (String T : commandsArray) {
				cTemp = cTemp + T;
				cTemp = cTemp + ",";
			}

			solutionHash.put(cTemp, (Integer) costOfSolution);

			return;
		}

		// Continue solution
		visitedSetHash.add(set);

		for (Package pack : repo) {

			if (initial.contains(pack.getName() + "=" + pack.getVersion())) {

				// Un-install from the initial set.
				set.remove(pack.getName() + "=" + pack.getVersion());
				commandsArray.add("-" + pack.getName() + "=" + pack.getVersion());

				find(set, repo, initial, constraints);

				set.add(pack.getName() + "=" + pack.getVersion());
				commandsArray.remove("-" + pack.getName() + "=" + pack.getVersion());

			} else if (commandsArray.contains("-" + pack.getName() + "=" + pack.getVersion()) == false
					&& set.contains(pack.getName() + "=" + pack.getVersion()) == false) {

				set.add(pack.getName() + "=" + pack.getVersion());
				commandsArray.add("+" + pack.getName() + "=" + pack.getVersion());

				find(set, repo, initial, constraints);

				set.remove(pack.getName() + "=" + pack.getVersion());
				commandsArray.remove("+" + pack.getName() + "=" + pack.getVersion());
			}
		}

	}

	// Removes from the maxConflict if there are more than 1 conflict
	private static List<String> removeMaxConflict(List<String> set) {
		if (maxConflictHash.size() >= 2) {
			String limit = Collections.max(maxConflictHash.entrySet(), (e1, e2) -> e1.getValue() - e2.getValue())
					.getKey();

			set.remove(limit);
		}
		return set;
	}

	// Removes from repo then calls a search.
	private static void findInverse(List<String> set, List<Package> repo, List<String> constraints) {

		int costOfSolution;

		if (packetValid(set, repo) == false) {
			return;
		}
		if (visitedSetHash.contains(set)) {
			return;
		}
		visitedSetHash.add(set);
		if (lastState(set, constraints)) {

			satisfies = true;
			// solution is found
			costOfSolution = costOfSolution(repo);
			String cTemp;
			for (String T : commandsArray) {
				cTemp = cTemp + T;
				cTemp = cTemp + ",";
			}

			solutionHash.put(cTemp, (Integer) costOfSolution);

			return;
		}

		Iterator<String> set_Iterator;

		set_Iterator = set.iterator();

		while (set_Iterator.hasNext()) {
			String cTemp;
			cTemp = set_Iterator.next();
			set_Iterator.remove();

			commandsArray.add("-" + cTemp);
			for (Package pack : repo) {
				String p;
				p = pack.getName() + "=" + pack.getVersion();
				if (p.equals(cTemp) == false) {
					set.add(pack.getName() + "=" + pack.getVersion());
					commandsArray.add("+" + pack.getName() + "=" + pack.getVersion());
				}
			}

			set.add(cTemp);
			commandsArray.remove("-" + cTemp);
		}
	}

	private static boolean packetValid(List<String> set, List<Package> repo) {

		for (Package pack : repo) {
			// Calls for dependsArray()
			if (set.contains(pack.getName() + "=" + pack.getVersion())) {

				for (List<String> subsectionArray : pack.getDepends()) {
					boolean discovered;
					discovered = false;

					for (String p : subsectionArray) {
						String[] spli_t;
						spli_t = packageSpli_t(p);

						String compareOperator;
						compareOperator = spli_t[2];

						if (discovered == false) {
							for (String T : set) {

								String[] spl_i_t;
								spl_i_t = packageSpli_t(T);

								if (spli_t[0].equals(spl_i_t[0])) {
									switch (compareOperator) {

									case "=":
										if (p.equals(T)) {
											discovered = true;
										}

										break;

									case "<":

										if (spli_t[1].equals(spl_i_t[1]) == false) {
											if (versionCompare(spli_t[1], spl_i_t[1])) {
												discovered = true;
											}
										}

										break;

									case "<=":
										if (versionCompare(spli_t[1], spl_i_t[1])) {
											discovered = true;
										}

										break;

									case ">":
										if (spli_t[1].equals(spl_i_t[1]) == false) {
											if (versionCompare(spl_i_t[1], spli_t[1])) {
												discovered = true;
											}
										}

										break;

									case ">=":
										if (versionCompare(spl_i_t[1], spli_t[1])) {
											discovered = true;
										}

										break;
									default:

										// No operator found
										discovered = true;

										break;
									}
								}
							}
						}
					}
					if (discovered == false) {
						// Missing dep
						return false;
					}
				}

				boolean foundConflict;
				foundConflict = false;

				// Checking conflictsArray
				for (String T : pack.getConflicts()) {

					String[] spli_tCons;
					spli_tCons = packageSpli_t(T);

					String compareOperator;
					compareOperator = spli_tCons[2];

					if (foundConflict == false) {
						for (String S : set) {

							String[] split_S;
							split_S = packageSpli_t(S);

							if (spli_tCons[0].equals(split_S[0])) {

								// Compare the operator
								switch (compareOperator) {

								case "=":
									if (T.equals(S)) {
										foundConflict = true;

										Integer value;
										value = maxConflictHash.get(S);

										if (value == null) {
											maxConflictHash.put(S, 1);
										} else {
											maxConflictHash.put(S, value++);
										}
									}

									break;

								case "<":
									if (spli_tCons[1].equals(split_S[1]) == false) {
										if (versionCompare(spli_tCons[1], split_S[1])) {
											foundConflict = true;

											Integer value;
											value = maxConflictHash.get(S);

											if (value == null) {
												maxConflictHash.put(S, 1);
											} else {
												maxConflictHash.put(S, value++);
											}
										}
									}

									break;

								case "<=":
									if (versionCompare(spli_tCons[1], split_S[1])) {
										foundConflict = true;

										Integer value;
										value = maxConflictHash.get(S);

										if (value == null) {
											maxConflictHash.put(S, 1);
										} else {
											maxConflictHash.put(S, value++);
										}
									}

									break;

								case ">":
									if (spli_tCons[1].equals(split_S[1]) == false) {
										if (versionCompare(split_S[1], spli_tCons[1])) {
											foundConflict = true;

											Integer value;
											value = maxConflictHash.get(S);

											if (value == null) {
												maxConflictHash.put(S, 1);
											} else {
												maxConflictHash.put(S, value++);
											}
										}
									}

									break;

								case ">=":
									if (versionCompare(split_S[1], spli_tCons[1])) {
										foundConflict = true;

										Integer value;
										value = maxConflictHash.get(S);

										if (value == null) {
											maxConflictHash.put(S, 1);
										} else {
											maxConflictHash.put(S, value++);
										}
									}

									break;

								default:
									foundConflict = true;

									Integer value;
									value = maxConflictHash.get(S);

									if (value == null) {
										maxConflictHash.put(S, 1);
									} else {
										maxConflictHash.put(S, value++);
									}

									break;

								}
							}
						}
					}
				}
				if (foundConflict) {

					// Conflict found
					return false;
				}
			}
		}
		return true;
	}

	private static boolean lastState(List<String> set, List<String> constraints) {

		for (String T : constraints) {

			String operator;
			operator = Character.toString(T.charAt(0));

			T = T.substring(1);
			String[] versionConsName;
			versionConsName = packageSpli_t(T);

			String nameCons;
			nameCons = versionConsName[0];

			String versionCons;
			versionCons = versionConsName[1];

			String compareOperator;
			compareOperator = versionConsName[2];

			boolean metConstraints;
			metConstraints = false;

			if (operator.equals("+")) {
				for (String pack : set) {

					String[] split_P;
					split_P = packageSpli_t(pack);
					if (nameCons.equals(split_P[0])) {

						// Compare operator
						switch (compareOperator) {
						case "=":
							if (versionCons.equals(split_P[1])) {
								metConstraints = true;
							}

							break;

						case "<":

							break;

						case "<=":

							break;

						case ">":

							break;

						case ">=":

							break;

						default:

							metConstraints = true;

							break;

						}
					}
				}
			}

			if (metConstraints == false) {
				return false;
			}

			if (operator.equals("-")) {

				int i;
				i = 0;

				for (String pack : set) {

					String[] split_P;
					split_P = packageSpli_t(pack);

					switch (compareOperator) {
					case "=":
						if (versionCons.equals(split_P[1]) && nameCons.equals(split_P[0])) {
							i++;
						}

						break;

					case "<":

						break;

					case "<=":

						break;

					case ">":

						break;

					case ">=":

						break;

					default:

						break;

					}
				}
				if (i > 0) {
					return false;
				}
			}
		}
		return true;
	}

	private static List<String> throwBadPackages(List<String> set, List<Package> repo) {

		List<String> removeArray = new ArrayList<>();

		int increment = 0;

		int limit;
		limit = set.size();

		for (Package pack : repo) {
			if (limit <= increment) {

				break;

			}

			boolean packageThrown;
			packageThrown = false;

			// collect dep and cons if pack is in the set
			if (set.contains(pack.getName() + "=" + pack.getVersion())) {

				increment++;

				for (List<String> subsectionArray : pack.getDepends()) {
					boolean discovered = false;

					// checking deps
					for (String p : subsectionArray) {
						String[] spli_t;
						spli_t = packageSpli_t(p);

						String compareOperator;
						compareOperator = spli_t[2];

						if (p.contains("=") == false) {

							if (discovered == false) {
								for (String T : set) {

									String[] spl_i_t;
									spl_i_t = packageSpli_t(T);

									if (spli_t[0].equals(spl_i_t[0])) {

										// Compare operator
										switch (compareOperator) {

										case "=":
											if (p.equals(T)) {
												discovered = true;
											}

											break;

										case "<":

											if (spli_t[1].equals(spl_i_t[1]) == false) {
												if (versionCompare(spli_t[1], spl_i_t[1])) {
													discovered = true;
												}
											}

											break;

										case "<=":
											if (versionCompare(spli_t[1], spl_i_t[1])) {
												discovered = true;
											}

											break;

										case ">":
											if (spli_t[1].equals(spl_i_t[1]) == false) {
												if (versionCompare(spl_i_t[1], spli_t[1])) {
													discovered = true;
												}
											}

											break;

										case ">=":
											if (versionCompare(spl_i_t[1], spli_t[1])) {
												discovered = true;
											}

											break;

										default:
											// found the dep
											discovered = true;

											break;

										}
									}
								}
							}
						} else {
							if (set.contains(p)) {
								discovered = true;
							}
						}
					}
					if (discovered == false) {

						packageThrown = true;

						set.remove(pack.getName() + "=" + pack.getVersion());
						commandsArray.add("-" + pack.getName() + "=" + pack.getVersion());
					}
				}
				if (packageThrown == false) {
					boolean foundConflict = false;

					for (String T : pack.getConflicts()) {
						String[] spli_tCons = packageSpli_t(T);
						String compareOperator = spli_tCons[2];
						if (foundConflict == false) {
							for (String S : set) {

								String[] split_S = packageSpli_t(S);
								if (spli_tCons[0].equals(split_S[0])) {

									switch (compareOperator) {
									case "=":
										if (T.equals(S)) {
											foundConflict = true;

										}

										break;

									case "<":
										if (spli_tCons[1].equals(split_S[1]) == false) {
											if (versionCompare(spli_tCons[1], split_S[1])) {
												foundConflict = true;

											}
										}

										break;

									case "<=":
										if (versionCompare(spli_tCons[1], split_S[1])) {
											foundConflict = true;

										}

										break;

									case ">":
										if (spli_tCons[1].equals(split_S[1]) == false) {
											if (versionCompare(split_S[1], spli_tCons[1])) {
												foundConflict = true;

											}
										}

										break;

									case ">=":
										if (versionCompare(split_S[1], spli_tCons[1])) {
											foundConflict = true;

										}

										break;

									default:
										foundConflict = true;

										break;
									}
								}
							}
						}
					}
					if (foundConflict) {

						removeArray.add(pack.getName() + "=" + pack.getVersion());
						set.remove(pack.getName() + "=" + pack.getVersion());
						commandsArray.add("-" + pack.getName() + "=" + pack.getVersion());
					}
				}
			}
		}

		return set;
	}

	// compare to see if they are the same version

	private static boolean versionCompare(String versX, String versY) {

		String versXTemp;
		String versYTemp;

		versXTemp = versX.replace("0", "");
		versYTemp = versY.replace("0", "");

		if (versXTemp.isEmpty() == false) {
			versX = versXTemp;
		}
		if (versYTemp.isEmpty() == false) {
			versY = versYTemp;
		}

		List<String> versXArray = new ArrayList(Arrays.asList(versX.split("\\.")));
		List<String> versYArray = new ArrayList(Arrays.asList(versY.split("\\.")));

		while (versXArray.size() != versYArray.size()) {
			if (versXArray.size() > versYArray.size()) {
				versYArray.add("0");
			} else {
				versXArray.add("0");
			}
		}

		for (int i = 0; i < versXArray.size(); i++) {
			if (Integer.parseInt(versXArray.get(i)) > Integer.parseInt(versYArray.get(i))) {
				return true;
			}
			if (Integer.parseInt(versYArray.get(i)) > Integer.parseInt(versXArray.get(i))) {
				return false;
			}
		}
		return true;
	}

	private static String[] packageSpli_t(String T) {

		String[] versionConsName;
		versionConsName = new String[2];

		String nameCons = "";
		String versionCons = "";
		String compareOperator = "";

		// split the packages
		if (T.contains("<=")) {
			T = T.replace("<", "");
			versionConsName = T.split("=");
			nameCons = versionConsName[0];
			versionCons = versionConsName[1];
			compareOperator = "<=";
		} else if (T.contains("<")) {
			versionConsName = T.split("<");
			nameCons = versionConsName[0];
			versionCons = versionConsName[1];
			compareOperator = "<";
		} else if (T.contains(">=")) {
			T = T.replace(">", "");
			versionConsName = T.split("=");
			nameCons = versionConsName[0];
			versionCons = versionConsName[1];
			compareOperator = ">=";
		} else if (T.contains(">")) {
			versionConsName = T.split(">");
			nameCons = versionConsName[0];
			versionCons = versionConsName[1];
			compareOperator = ">";
		} else if (T.contains("=")) {
			versionConsName = T.split("=");
			nameCons = versionConsName[0];
			versionCons = versionConsName[1];
			compareOperator = "=";
		} else {

			nameCons = T;
		}
		String[] solutionArray = { nameCons, versionCons, compareOperator };
		return solutionArray;
	}

	private static int costOfSolution(List<Package> repo) {
		int solutionArray = 0;

		for (String T : commandsArray) {

			if (T.contains("-")) {
				solutionArray = solutionArray + 1000000;
			} else {
				for (Package pack : repo) {
					String versionName;
					versionName = pack.getName() + "=" + pack.getVersion();

					String p;
					p = T.substring(1);

					if (p.equals(versionName)) {
						solutionArray = solutionArray + pack.getSize();
					}
				}
			}
		}

		return solutionArray;
	}

	private static void getFinalSolution() {

		List<String> solutionArray = new ArrayList<>();
		int resultCost = 0;

		for (Map.Entry<String, Integer> entry : solutionHash.entrySet()) {

			if (solutionArray.size() == 0 || entry.getValue() < resultCost) {
				solutionArray = Arrays.asList(entry.getKey().split(","));
				resultCost = entry.getValue();
			}
		}

		System.out.println(JSON.toJSON(solutionArray));

	}

	static String readFile(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename));
		StringBuilder sb = new StringBuilder();
		br.lines().forEach(line -> sb.append(line));
		return sb.toString();
	}
}
