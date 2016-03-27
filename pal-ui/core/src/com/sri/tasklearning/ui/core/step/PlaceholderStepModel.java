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

package com.sri.tasklearning.ui.core.step;

/**
 * A special and somewhat hacky extension of StepModel that is used as a
 * placeholder during a user gesture that involves moving/reordering steps
 * through drag/drop. 
 */
public class PlaceholderStepModel extends StepModel {
    public PlaceholderStepModel() {
        super("placeholder");
        
        stepType = StepType.PLACEHOLDER;
    }
}
