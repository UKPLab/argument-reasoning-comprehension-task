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
 * Checks whether all stance-related radio groups are checked
 */
function validateFormAndUpdateCollectedResults() {
    // make sure all arguments have selected stance from the radio group
    var allOk = true;

    // check where all have stance to 0 or 1, and if so, whether the
    // sentences were highlighted or checked implicitness
    $('div.argument').each(function () {
        allOk &= validateSingleAnnotatedArgument($(this).attr("id"));
    });

    // update the submit button
    $("#submitButton").prop("disabled", !allOk);

}

/**
 * Validates if an argument is annotated with stance
 * @param reasonID argument
 * @returns {boolean} true if we have all required annotations, false otherwise
 */
function validateSingleAnnotatedArgument(reasonID) {
    // check whether the some groups have unfilled buttons
    var selectedReasonValue = $("input[name=" + reasonID + "_original_reason_group]:checked").val();

    // console.log(reasonID + ", " + selectedReasonValue);

    // console.log("Stance value for " + reasonID + ": " + stanceValue + ", stanceValue != null: " + (stanceValue != null));
    var toughnessFilled = true;
    var logicFilled = true;
    if (selectedReasonValue == 0 || selectedReasonValue == 1) {
        var selectedToughnessValue = $("input[name=" + reasonID + "_familiarGroup]:checked").val();
        // console.log(reasonID + ", " + selectedToughnessValue);
        toughnessFilled = selectedToughnessValue != null;

        var selectedLogicValue = $("input[name=" + reasonID + "_logicalGroup]:checked").val();
        // console.log(reasonID + ", " + selectedToughnessValue);
        logicFilled = selectedLogicValue != null;
    }

    return selectedReasonValue != null && toughnessFilled && logicFilled;
}

function updateDisplayedReason(reasonId, clickedReasonId) {
    var span = $("#" + reasonId + "_selected_reason_text");
    var selectedReason = $("#" + clickedReasonId + "_label");
    var rephrasedText = selectedReason.text();

    var selectedReasonValue = $("input[name=" + reasonId + "_original_reason_group]:checked").val();

    // get also the value
    // console.log(rephrasedText);
    // console.log(span.attr("id"));
    // console.log(span.text());

    if (selectedReasonValue != 2) {
        span.text(rephrasedText);

        // show the extended dialog box
        $("#" + reasonId + "_additionalQuestions").show(400);

        // add the underline to the explanation
        $("#" + reasonId + "_warrant").addClass("underlined-text")

    } else {
        span.text("--------");

        $("#" + reasonId + "_additionalQuestions").hide(400);
        // remove the underline to the explanation
        $("#" + reasonId + "_warrant").removeClass("underlined-text")
    }
}

$(document).ready(function () {
    // wait for any radio button to change
    $(".stanceGroup input:radio").change(function () {
        var reasonId = $(this).data("reasonid");

        validateFormAndUpdateCollectedResults();

        var clickedReasonId = $(this).attr("id");
        updateDisplayedReason(reasonId, clickedReasonId);
    });

    // wait for any radio button to change
    $(".additional-questions input:radio").change(function () {
        var reasonId = $(this).data("reasonid");

        validateFormAndUpdateCollectedResults();
    });

});
