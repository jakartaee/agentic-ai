/*****************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package ee.jakarta.examples.ai.agent.courseplanner;

import ee.jakarta.examples.ai.agent.courseplanner.model.Standard;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

/**
 * The curriculum standards the catalogue tags against, by subject. A small
 * built-in set is enough for the example; a real deployment would read a
 * school's own standards from configuration or a database.
 */
@ApplicationScoped
public class StandardsReference {

    private final Map<String, List<Standard>> bySubject = Map.of(
            "Physics", List.of(
                    new Standard("HS-PS2-1",
                            "Analyze data to support the claim that Newton's Second Law describes the "
                                    + "mathematical relationship among the net force on an object, its mass "
                                    + "and its acceleration."),
                    new Standard("HS-PS2-2",
                            "Use mathematical representations to support the claim that the total momentum "
                                    + "of a system is conserved when there is no net force on the system."),
                    new Standard("HS-PS2-3",
                            "Design, evaluate and refine a device that minimises the force on an object "
                                    + "during a collision."),
                    new Standard("HS-PS2-4",
                            "Use mathematical representations of Newton's Law of Gravitation and Coulomb's "
                                    + "Law to describe and predict gravitational and electrostatic forces."),
                    new Standard("HS-PS3-1",
                            "Create a computational model to calculate the change in energy of one "
                                    + "component in a system when the change in the others and energy flows "
                                    + "in and out are known."),
                    new Standard("HS-PS3-2",
                            "Illustrate that macroscopic energy is a combination of the motion of particles "
                                    + "and the relative position of particles."),
                    new Standard("HS-PS4-1",
                            "Use mathematical representations to support a claim about relationships among "
                                    + "the frequency, wavelength and speed of waves travelling in a medium.")),
            "Mathematics", List.of(
                    new Standard("HSA-SSE.A.1",
                            "Interpret expressions that represent a quantity in terms of its context."),
                    new Standard("HSA-SSE.B.3",
                            "Choose and produce an equivalent form of an expression to reveal and explain "
                                    + "properties of the quantity represented."),
                    new Standard("HSA-CED.A.1",
                            "Create equations and inequalities in one variable and use them to solve problems."),
                    new Standard("HSA-REI.B.3",
                            "Solve linear equations and inequalities in one variable, including equations "
                                    + "with coefficients represented by letters."),
                    new Standard("HSF-IF.A.1",
                            "Understand that a function assigns to each element of the domain exactly one "
                                    + "element of the range."),
                    new Standard("HSG-SRT.A.1",
                            "Verify experimentally the properties of dilations given by a centre and a "
                                    + "scale factor.")),
            "English", List.of(
                    new Standard("RL.9-10.1",
                            "Cite strong and thorough textual evidence to support analysis of what the text "
                                    + "says explicitly as well as inferences drawn from it."),
                    new Standard("RL.9-10.2",
                            "Determine a theme or central idea of a text and analyse in detail its "
                                    + "development over the course of the text."),
                    new Standard("W.9-10.1",
                            "Write arguments to support claims in an analysis of substantive topics or texts."),
                    new Standard("W.9-10.2",
                            "Write informative or explanatory texts to examine and convey complex ideas, "
                                    + "concepts and information clearly and accurately."),
                    new Standard("SL.9-10.1",
                            "Initiate and participate effectively in a range of collaborative discussions.")));

    public List<Standard> forSubject(String subject) {
        return bySubject.getOrDefault(subject, List.of());
    }
}
