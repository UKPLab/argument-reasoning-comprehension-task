/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Checks whether all stance-related radio groups are checked and whether
 * there is either a selection of segments done or a checkbox saying that there
 * are no explicit segments
 */
function validateFormAndUpdateCollectedResults() {
    // make sure all arguments have selected stance from the radio group
    var allOk = true;

    // check where all have stance to 0 or 1, and if so, whether the
    // sentences were highlighted or checked implicitness
    $('div.argument').each(function () {
        allOk &= validateSingleAnnotatedArgument($(this).attr("id"));
    });

    var educationSelected = $("input[name=education]:checked").val();
    var trainingSelected = $("input[name=training]:checked").val();

    allOk &= educationSelected != null;
    allOk &= trainingSelected != null;

    // update the submit button
    $("#submitButton").prop("disabled", !allOk);

}

/**
 * Validates if an argument is fully annotated, updates the global container with annotations
 * @param reasonId argument
 * @returns {boolean} true if we have all required annotations, false otherwise
 */
function validateSingleAnnotatedArgument(reasonId) {
    // check whether the some groups have unfilled buttons
    var optionSelected = $("input[name=" + reasonId + "_warrant_group]:checked").val();

    // check whether the some groups have unfilled buttons
    var familiarSelected = $("input[name=" + reasonId + "_familiarGroup]:checked").val();
    // console.log("Familiar selected: " + familiarSelected);

    return optionSelected != null && familiarSelected != null;
}


$(document).ready(function () {
    // wait for any radio button to change
    // wait for any radio button to change
    $("input:radio").change(function () {
        var reasonId = $(this).data("reasonid");

        validateFormAndUpdateCollectedResults();
    });

    // disable hitting enter to submit the form
    $('form input:text').bind('input', function () {
        validateFormAndUpdateCollectedResults();
        return e.which !== 13;
    });

});
