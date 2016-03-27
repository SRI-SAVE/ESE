/*
 * Copyright 2016 SRI International
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sri.tasklearning.novo;

import com.sri.tasklearning.novo.thing.Assembly;
import com.sri.tasklearning.novo.thing.AssemblyConfigurationEnum;
import com.sri.tasklearning.novo.thing.ColorEnum;
import com.sri.tasklearning.novo.thing.Piece;
import com.sri.tasklearning.novo.thing.ShapeEnum;
import com.sri.tasklearning.novo.thing.SizeEnum;
import com.sri.tasklearning.novo.thing.Thing;

public final class Puzzle {
    private static final Puzzle[] puzzles = new Puzzle[] {
        new Puzzle("Snowman", PuzzleDifficulty.EASY, 
                new Thing[] {
                    new Piece(ShapeEnum.TRIANGLE), new Piece(ShapeEnum.CIRCLE)}, 
                new Thing[] {
                    new Assembly(AssemblyConfigurationEnum.VERTICAL, new Thing[] {         
                        new Piece(ShapeEnum.CIRCLE, ColorEnum.WHITE, SizeEnum.SMALL), 
                        new Piece(ShapeEnum.CIRCLE, ColorEnum.WHITE, SizeEnum.MEDIUM),
                        new Piece(ShapeEnum.CIRCLE, ColorEnum.WHITE, SizeEnum.LARGE)})}),
        new Puzzle("Patriotic Snowman", PuzzleDifficulty.MEDIUM, 
                new Thing[] {
                    new Piece(ShapeEnum.TRIANGLE), new Piece(ShapeEnum.CIRCLE)}, 
                new Thing[] {
                    new Assembly(AssemblyConfigurationEnum.VERTICAL, new Thing[] {         
                        new Piece(ShapeEnum.CIRCLE, ColorEnum.BLUE, SizeEnum.SMALL), 
                        new Piece(ShapeEnum.CIRCLE, ColorEnum.WHITE, SizeEnum.MEDIUM),
                        new Piece(ShapeEnum.CIRCLE, ColorEnum.RED, SizeEnum.LARGE)})}),
        new Puzzle("Tree", PuzzleDifficulty.EASY, 
                new Thing[] {
                    new Piece(ShapeEnum.TRIANGLE), new Piece(ShapeEnum.CIRCLE)}, 
                new Thing[] {
                    new Assembly(AssemblyConfigurationEnum.VERTICAL, new Thing[] {         
                        new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.SMALL), 
                        new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.MEDIUM),
                        new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.LARGE)})}),
        new Puzzle("Mountains", PuzzleDifficulty.EASY, 
                new Thing[] {
                    new Piece(ShapeEnum.TRIANGLE), new Piece(ShapeEnum.CIRCLE)}, 
                new Thing[] {
                    new Assembly(AssemblyConfigurationEnum.HORIZONTAL, new Thing[] {         
                        new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.MEDIUM), 
                        new Piece(ShapeEnum.TRIANGLE, ColorEnum.BLACK, SizeEnum.SMALL),
                        new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.LARGE)})}),
        new Puzzle("Forest (3 trees)", PuzzleDifficulty.HARD, 
                new Thing[] {
                    new Piece(ShapeEnum.TRIANGLE), new Piece(ShapeEnum.CIRCLE)}, 
                new Thing[] {
                    new Assembly(AssemblyConfigurationEnum.VERTICAL, new Thing[] {         
                        new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.SMALL, "1"), 
                        new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.MEDIUM, "1"),
                        new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.LARGE, "1")}),
                    new Assembly(AssemblyConfigurationEnum.VERTICAL, new Thing[] {         
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.SMALL, "2"), 
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.MEDIUM, "2"),
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.LARGE, "2")}),
                    new Assembly(AssemblyConfigurationEnum.VERTICAL, new Thing[] {         
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.SMALL, "3"), 
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.MEDIUM, "3"),
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.LARGE, "3")})}),
        new Puzzle("Forest (7 trees)", PuzzleDifficulty.HARD, 
                new Thing[] {
                    new Piece(ShapeEnum.TRIANGLE), new Piece(ShapeEnum.CIRCLE)}, 
                new Thing[] {
                    new Assembly(AssemblyConfigurationEnum.VERTICAL, new Thing[] {         
                        new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.SMALL, "1"), 
                        new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.MEDIUM, "1"),
                        new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.LARGE, "1")}),
                    new Assembly(AssemblyConfigurationEnum.VERTICAL, new Thing[] {         
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.SMALL, "2"), 
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.MEDIUM, "2"),
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.LARGE, "2")}),
                    new Assembly(AssemblyConfigurationEnum.VERTICAL, new Thing[] {         
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.SMALL, "3"), 
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.MEDIUM, "3"),
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.LARGE, "3")}), 
                    new Assembly(AssemblyConfigurationEnum.VERTICAL, new Thing[] {         
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.SMALL, "4"), 
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.MEDIUM, "4"),
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.LARGE, "4")}), 
                    new Assembly(AssemblyConfigurationEnum.VERTICAL, new Thing[] {         
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.SMALL, "5"), 
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.MEDIUM, "5"),
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.LARGE, "5")}), 
                    new Assembly(AssemblyConfigurationEnum.VERTICAL, new Thing[] {         
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.SMALL, "6"), 
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.MEDIUM, "6"),
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.LARGE, "6")}), 
                    new Assembly(AssemblyConfigurationEnum.VERTICAL, new Thing[] {         
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.SMALL, "7"), 
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.MEDIUM, "7"),
                            new Piece(ShapeEnum.TRIANGLE, ColorEnum.GREEN, SizeEnum.LARGE, "7")})})        
        };                            
    
    private final String name;
    private final PuzzleDifficulty difficulty;
    private final Thing[] startingConfiguration;
    private final Thing[] solution; 
    
    private Puzzle(String name, PuzzleDifficulty difficulty, Thing[] startingConfiguration, Thing[] solution) {
        this.name = name;
        this.difficulty = difficulty;
        this.startingConfiguration = startingConfiguration;
        this.solution = solution;
    }
    
    
    public static Puzzle[] getPuzzles() {
        return puzzles;
    }

    public String getName() {
        return name;
    }

    public PuzzleDifficulty getDifficulty() {
        return difficulty;
    }

    public Thing[] getStartingConfiguration() {
        return startingConfiguration;
    }

    public Thing[] getSolution() {
        return solution;
    }

    
    enum PuzzleDifficulty {
        EASY,
        MEDIUM,
        HARD
    }
}
