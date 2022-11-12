/*
 * Copyright (c) 2021 by Damien Pellier <Damien.Pellier@imag.fr>.
 *
 * This file is part of PDDL4J library.
 *
 * PDDL4J is free software: you can redistribute it and/or modify * it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * PDDL4J is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License * along with PDDL4J.  If not,
 * see <http://www.gnu.org/licenses/>
 */

package fr.uga.pddl4j.examples.asp;

import fr.uga.pddl4j.heuristics.state.StateHeuristic;
import fr.uga.pddl4j.parser.DefaultParsedProblem;
import fr.uga.pddl4j.plan.Plan;
import fr.uga.pddl4j.plan.SequentialPlan;
import fr.uga.pddl4j.planners.AbstractPlanner;
import fr.uga.pddl4j.planners.Planner;
import fr.uga.pddl4j.planners.PlannerConfiguration;
import fr.uga.pddl4j.planners.SearchStrategy;
import fr.uga.pddl4j.planners.statespace.HSP;
import fr.uga.pddl4j.planners.statespace.search.StateSpaceSearch;
import fr.uga.pddl4j.problem.*;
import fr.uga.pddl4j.problem.operator.Action;
import fr.uga.pddl4j.problem.operator.Condition;
import fr.uga.pddl4j.problem.operator.ConditionalEffect;
import fr.uga.pddl4j.util.BitSet;
import fr.uga.pddl4j.util.BitVector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sat4j.core.VecInt;
import org.sat4j.maxsat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;
import picocli.CommandLine;

import java.io.*;
import java.util.*;

/**
 * The class is an example. It shows how to create a simple A* search planner
 * able to
 * solve an ADL problem by choosing the heuristic to used and its weight.
 *
 * @author Bastien & Dimitri
 * @version 4.0 - 30.11.2021
 */
@CommandLine.Command(name = "ASP", version = "ASP 1.0", description = "Solves a specified planning problem using A* search strategy.", sortOptions = false, mixinStandardHelpOptions = true, headerHeading = "Usage:%n", synopsisHeading = "%n", descriptionHeading = "%nDescription:%n%n", parameterListHeading = "%nParameters:%n", optionListHeading = "%nOptions:%n")
public class ASP extends AbstractPlanner {

    /**
     * The class logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(ASP.class.getName());

    private int plan_length = 0;

    @Override
    public Problem instantiate(DefaultParsedProblem problem) {
        final Problem pb = new DefaultProblem(problem);
        pb.instantiate();
        return pb;
    }

    /**
     * Search a solution plan to a specified domain and problem using A*.
     *
     * @param problem the problem to solve.
     * @return the plan found or null if no plan was found.
     */
    @Override
    public Plan solve(final Problem problem) {
        TreeMap<Integer, Object> dico = new TreeMap<>();
        int index = 1;

        ISolver solver = SolverFactory.newDefault();

        for (Fluent f : problem.getFluents()) {
            dico.put(index, f);
            index++;
        }
        for (Action a : problem.getActions()) {
            dico.put(index, a);
            index++;
        }

        try {
            for (Action a : problem.getActions()) {

                List<Integer> clause = new ArrayList<>();

                for (int val : a.getPrecondition().getPositiveFluents().stream().toArray()) { // Préconditions positives
                    if (val != 0)
                        clause.add(val);
                }
                for (int val : a.getPrecondition().getNegativeFluents().stream().toArray()) { // Préconditions négatives
                    if (val != 0)
                        clause.add(val);
                }
                List<ConditionalEffect> effets = a.getConditionalEffects();

                for (int i = 0; i < effets.size(); i++) { // Fluents positifs et négatifs des effets des actions
                    int[] posFluents = effets.get(i).getEffect().getPositiveFluents().stream().toArray();
                    int[] negFluents = effets.get(i).getEffect().getNegativeFluents().stream().toArray();
                    int[] posFluentsInv = new int[posFluents.length];
                    int[] negFluentsInv = new int[negFluents.length];

                    for (int j = 0; j < posFluents.length; j++) {
                        if (posFluentsInv[j] != 0) {
                            posFluentsInv[j] = (posFluents[j] * -1);
                            clause.add(posFluentsInv[j]);
                        }
                    }
                    for (int j = 0; j < negFluents.length; j++) {
                        if (negFluentsInv[j] != 0) {
                            negFluentsInv[j] = (negFluentsInv[j] * -1);
                            clause.add(negFluentsInv[j]);
                        }
                    }
                }
                int[] clause_int = clause.stream().mapToInt(i -> i).toArray();
                if (clause_int.length > 0) {
                    solver.addClause(new VecInt(clause_int)); // adapt Array to IVecInt
                }
            }
        } catch (ContradictionException e) {
            throw new RuntimeException(e);
        }

        // IProblem iproblem = solver;
        try {
            if (solver.isSatisfiable()) {
                // System.out.println("le problème est satisfiable");
                this.plan_length = solver.model().length;
                for (int num : solver.model()) {
                    // on récupère les fluents et les actions dans notre dictionnaire
                    Object result = dico.get(Math.abs(num)); // on prend la valeur absolue car notre dictionnaire ne
                                                             // contient que des index positifs
                    if (result instanceof Action) {
                        Action a = (Action) result;
                    } else if (result instanceof Fluent) {
                        Fluent f = (Fluent) result;
                    }
                    // System.out.println(num);
                }
            } else {
                // System.out.println("le problème n'est pas satisfiable");

            }
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public int getPlanLength() {
        return this.plan_length;
    }

    /**
     * The main method of the <code>ASP</code> planner.
     *
     * @param args the arguments of the command line.
     */
    public static void main(String[] args) {
        try {
            // BLOCKS
            File blocks_domain = new File("pddl/domain_blocks.pddl");
            File[] blocks_problems = new File("pddl/problemes_blocks").listFiles();
            Map<File, Double> HSP_blocks_runtimes = new TreeMap<>();
            Map<File, Integer> HSP_blocks_makespans = new TreeMap<>();
            computesBenchmark("HSP", blocks_domain, blocks_problems, HSP_blocks_runtimes, HSP_blocks_makespans);
            Map<File, Double> ASP_blocks_runtimes = new TreeMap<>();
            Map<File, Integer> ASP_blocks_makespans = new TreeMap<>();
            computesBenchmark("ASP", blocks_domain, blocks_problems, ASP_blocks_runtimes, ASP_blocks_makespans);

            // DEPOTS
            File depots_domain = new File("pddl/domain_depots.pddl");
            File[] depots_problems = new File("pddl/problemes_depots").listFiles();
            Map<File, Double> HSP_depots_runtimes = new TreeMap<>();
            Map<File, Integer> HSP_depots_makespans = new TreeMap<>();
            computesBenchmark("HSP", depots_domain, depots_problems, HSP_depots_runtimes, HSP_depots_makespans);
            Map<File, Double> ASP_depots_runtimes = new TreeMap<>();
            Map<File, Integer> ASP_depots_makespans = new TreeMap<>();
            computesBenchmark("ASP", depots_domain, depots_problems, ASP_depots_runtimes, ASP_depots_makespans);

            // GRIPPER
            File gripper_domain = new File("pddl/domain_gripper.pddl");
            File[] gripper_problems = new File("pddl/problemes_gripper").listFiles();
            Map<File, Double> HSP_gripper_runtimes = new TreeMap<>();
            Map<File, Integer> HSP_gripper_makespans = new TreeMap<>();
            computesBenchmark("HSP", gripper_domain, gripper_problems, HSP_gripper_runtimes, HSP_gripper_makespans);
            Map<File, Double> ASP_gripper_runtimes = new TreeMap<>();
            Map<File, Integer> ASP_gripper_makespans = new TreeMap<>();
            computesBenchmark("ASP", gripper_domain, gripper_problems, ASP_gripper_runtimes, ASP_gripper_makespans);

            // LOGISTICS
            File logistics_domain = new File("pddl/domain_logistics.pddl");
            File[] logistics_problems = new File("pddl/problemes_logistics").listFiles();
            Map<File, Double> HSP_logistics_runtimes = new TreeMap<>();
            Map<File, Integer> HSP_logistics_makespans = new TreeMap<>();
            computesBenchmark("HSP", logistics_domain, logistics_problems, HSP_logistics_runtimes,
                    HSP_logistics_makespans);
            Map<File, Double> ASP_logistics_runtimes = new TreeMap<>();
            Map<File, Integer> ASP_logistics_makespans = new TreeMap<>();
            computesBenchmark("ASP", logistics_domain, logistics_problems, ASP_logistics_runtimes,
                    ASP_logistics_makespans);

            // On écrit les données dans un fichier
            BufferedWriter output = new BufferedWriter(new FileWriter("DATA.csv"));
            List<Map<File, ? extends Number>> TOUT = new ArrayList<>();
            TOUT.add(HSP_blocks_runtimes);
            TOUT.add(ASP_blocks_runtimes);
            TOUT.add(HSP_blocks_makespans);
            TOUT.add(ASP_blocks_makespans);
            TOUT.add(HSP_depots_runtimes);;
            TOUT.add(ASP_depots_runtimes);
            TOUT.add(HSP_depots_makespans);
            TOUT.add(ASP_depots_makespans);
            TOUT.add(HSP_gripper_runtimes);
            TOUT.add(ASP_gripper_runtimes);
            TOUT.add(HSP_gripper_makespans);
            TOUT.add(ASP_gripper_makespans);
            TOUT.add(HSP_logistics_runtimes);
            TOUT.add(ASP_logistics_runtimes);
            TOUT.add(HSP_logistics_makespans);
            TOUT.add(ASP_logistics_makespans);

            for (Map<File,? extends Number> map : TOUT) {
                for (Number n : map.values()) {
                    output.write(String.valueOf(n) + ";");
                }
                output.newLine();
            }

            output.close();
        } catch (IllegalArgumentException e) {
            LOGGER.fatal(e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param pddlDomain   Le domain pddl
     * @param pddlProblems l'array des problems pddl
     * @param runtimes     dictionnaire qui associe UN problème à UN temps
     *                     d'exécution
     * @param makespans    dictionnaire qui associé UN problème à UNE longueur de
     *                     plan
     * @return
     */
    private static void computesBenchmark(String algo, File pddlDomain, File[] pddlProblems,
            Map<File, Double> runtimes, Map<File, Integer> makespans) {
        for (int i = 0; i < pddlProblems.length; i++) {
            long start = System.currentTimeMillis();
            if (algo == "HSP") {
                final HSP planner = new HSP();
                CommandLine cmd = new CommandLine(planner);
                cmd.execute(pddlDomain.getAbsolutePath(), pddlProblems[i].getAbsolutePath());
                double totalRuntime = ((System.currentTimeMillis() - start) * 1.0) / pddlProblems.length;
                int planLength = 10;        //On ne savait pas comment récupérer les planlength pour HSP, on a donc mit une valeur par défaut et on l'a remplacée à la main par les valeurs qui se trouvaient dans la console
                runtimes.put(pddlProblems[i], totalRuntime);
                makespans.put(pddlProblems[i], planLength);
            }

            if (algo == "ASP") {
                final ASP planner = new ASP();
                CommandLine cmd = new CommandLine(planner);
                cmd.execute(pddlDomain.getAbsolutePath(), pddlProblems[i].getAbsolutePath());
                double totalRuntime = ((System.currentTimeMillis() - start) * 1.0) / pddlProblems.length;
                int planLength = planner.getPlanLength();
                runtimes.put(pddlProblems[i], totalRuntime);
                makespans.put(pddlProblems[i], planLength);
            }
        }
    }
}
